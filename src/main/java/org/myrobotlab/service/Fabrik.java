package org.myrobotlab.service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.kinematics.FabrikArm;
import org.myrobotlab.kinematics.JointFrame;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.kinematics.ReachCloud;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.geometry.Point3df;
import org.myrobotlab.math.geometry.PointCloud;
import org.myrobotlab.service.config.FabrikConfig;
import org.myrobotlab.service.interfaces.IKJointAngleListener;
import org.myrobotlab.service.interfaces.IKJointAnglePublisher;
import org.myrobotlab.service.interfaces.PointListener;
import org.myrobotlab.service.interfaces.PointsListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * FABRIK inverse kinematics service. Cartesian goals are meters, Y-up — the same
 * frame as JMonkeyEngine and {@link InverseKinematics3D}.
 *
 * <p>
 * The linked <a href="https://github.com/Fabrik/fabrik">Fabrik/fabrik</a> GitHub
 * project is a Joomla application builder, not an IK solver. This service
 * implements the FABRIK algorithm (Aristidou &amp; Lasenby) as published in the
 * Java Caliko library (<a href="https://github.com/feduni/caliko">FedUni/caliko</a>,
 * MIT). After a geometric FABRIK pass, joint angles are recovered around the
 * measured hinge axes, then cyclic coordinate descent and damped least squares
 * tighten the palm to within a few millimeters.
 * </p>
 *
 * <p>
 * Chain:
 * {@code jme.publishClickPoint → fabrik.onPoint → moveTo};
 * {@code fabrik.publishJointAngles → i01.onJointAngles → Servo.moveTo →
 * JMonkeyEngine.rotateTo}. The green simulator marker tracks
 * {@link #publishIkGoal}.
 * </p>
 */
public class Fabrik extends Service<FabrikConfig> implements IKJointAnglePublisher, PointListener, PointsListener {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Fabrik.class);

  public static final double CARTESIAN_STEP_M = 0.02;

  public static final int MAX_CARTESIAN_STEPS = 25;

  String currentArm = null;

  private final Map<String, FabrikArm> arms = new TreeMap<>();

  public Point worldPosition = null;

  public Point ikGoal = null;

  public double lastSolveError = Double.NaN;

  public int lastSolveIterations = 0;

  public int reachCloudPointCount = 0;

  private boolean worldCalibrated = false;

  public Fabrik(String n, String id) {
    super(n, id);
  }

  @Override
  public void startService() {
    super.startService();
    subscribeToSimulatorClicks();
  }

  /**
   * Simulator OAK-D mesh click (world meters, Y-up). Same path as the WebGui
   * MoveTo boxes.
   */
  @Override
  public void onPoint(Point point) {
    if (point == null) {
      return;
    }
    log.info("FABRIK onPoint {}", point);
    invoke("publishPickedPoint", point);
    moveTo(point.getX(), point.getY(), point.getZ());
  }

  /**
   * World point picked on the OAK-D overlay (before {@link #moveTo}).
   */
  public Point publishPickedPoint(Point point) {
    return point;
  }

  @Override
  public void onPoints(List<Point> points) {
    if (points == null || points.isEmpty()) {
      return;
    }
    onPoint(points.get(0));
  }

  @Override
  public FabrikConfig apply(FabrikConfig c) {
    super.apply(c);
    if (c != null) {
      installCalibrationFromConfig();
    }
    return c;
  }

  public Point currentPosition(String name) {
    FabrikArm arm = arms.get(name);
    return arm == null ? null : arm.getPalmPosition();
  }

  public Point currentPositionWorld(String name) {
    return currentPosition(name);
  }

  public String getCurrentArmName() {
    return currentArm;
  }

  public FabrikArm getCurrentArm(String name) {
    return arms.get(name);
  }

  public Point getWorldOrigin() {
    FabrikArm arm = currentArmModel();
    return arm == null ? null : arm.getBaseOrigin();
  }

  public Point moveTo(double x, double y, double z) {
    String name = requireCurrentArm();
    if (name == null) {
      return null;
    }
    return moveTo(name, x, y, z);
  }

  public Point moveTo(String arm, double x, double y, double z) {
    return moveTo(arm, new Point(x, y, z));
  }

  /**
   * Solve so the palm reaches {@code p}. Publishes the green goal marker
   * immediately, then walks a straight line in {@link #CARTESIAN_STEP_M} steps.
   */
  public Point moveTo(String name, Point p) {
    FabrikArm arm = arms.get(name);
    if (arm == null) {
      error("No arm named %s", name);
      return null;
    }
    Point current = arm.getPalmPosition();
    double travel = current.distanceTo(p);
    log.info("FABRIK moveTo {} goal {} current {} travel {} m", name, p, current, travel);
    invoke("publishIkGoal", p);
    int steps = Math.max(1, Math.min(MAX_CARTESIAN_STEPS, (int) Math.ceil(travel / CARTESIAN_STEP_M)));
    int missed = 0;
    for (int i = 1; i <= steps; i++) {
      Point waypoint = lerp(current, p, (double) i / steps);
      boolean last = i == steps;
      // Intermediate waypoints stay cheap (no reseeding). The last step uses
      // extra seeds and DLS so the palm finishes within a few millimeters.
      if (!arm.moveToGoal(waypoint, last ? FabrikArm.DEFAULT_SEED_ATTEMPTS : 0)) {
        missed++;
        log.debug("missed waypoint {}/{} {} by {} m", i, steps, waypoint, arm.distanceToGoal(waypoint));
      }
      lastSolveError = arm.getLastError();
      lastSolveIterations = arm.getLastIterations();
      publishTelemetry(name);
    }
    Point reached = currentPositionWorld(name);
    double err = reached.distanceTo(p);
    if (err > arm.getErrorThreshold()) {
      arm.moveToGoal(p, FabrikArm.DEFAULT_SEED_ATTEMPTS);
      lastSolveError = arm.getLastError();
      lastSolveIterations = arm.getLastIterations();
      publishTelemetry(name);
      reached = currentPositionWorld(name);
      err = reached.distanceTo(p);
    }
    if (missed > 0) {
      warn("FABRIK moveTo %s got within %.4f m of %s (%d of %d waypoints unreachable)", name, err, p, missed, steps);
    } else {
      log.info("FABRIK moveTo {} reached {} goal {} err {} m", name, reached, p, err);
    }
    broadcastState();
    return reached;
  }

  public Point calibrateFromSimulator() {
    JMonkeyEngine jme = findSimulator();
    if (jme == null) {
      error("No JMonkeyEngine running — start the simulator first");
      return null;
    }
    return calibrateFromSimulator(jme);
  }

  public Point calibrateFromSimulator(JMonkeyEngine jme) {
    if (jme == null) {
      return null;
    }
    String name = requireCurrentArm();
    if (name == null) {
      return null;
    }
    FabrikArm arm = arms.get(name);
    if (arm.getNumJoints() == 0) {
      error("No joints on arm %s", name);
      return null;
    }
    String[] parsed = parseArmLinkName(arm.getFrame(0).name);
    if (parsed == null) {
      error("Cannot parse robot/side from link name %s", arm.getFrame(0).name);
      return null;
    }
    List<JointFrame> measured = jme.getArmJointFrames(parsed[0], parsed[1]);
    if (measured.size() != arm.getNumJoints()) {
      error("Measured %d of %d %s arm joints — check VinMoov node names", measured.size(), arm.getNumJoints(), parsed[1]);
      return null;
    }
    Point wrist = toPoint3(jme.getHandWorldTranslation(parsed[0], parsed[1]));
    if (wrist == null) {
      error("No wrist node for %s %s", parsed[0], parsed[1]);
      return null;
    }
    return calibrateFromJointFrames(name, measured, wrist);
  }

  public Point calibrateFromJointFrames(String name, List<JointFrame> frames, Point endEffectorWorld) {
    FabrikArm arm = arms.get(name);
    if (arm == null) {
      error("No arm named %s", name);
      return null;
    }
    for (JointFrame frame : frames) {
      log.info("measured {}", frame);
    }
    if (!arm.applyJointFrames(frames, endEffectorWorld)) {
      error("Could not build %s from measured joints", name);
      return null;
    }
    persistCalibration(arm, frames, endEffectorWorld);
    worldCalibrated = true;
    Point world = currentPositionWorld(name);
    worldPosition = world;
    invoke("publishWorldPosition", world);
    invoke("publishIkGoal", world);
    double residual = endEffectorWorld == null ? 0 : world.distanceTo(endEffectorWorld);
    log.info("FABRIK calibrated {} from {} joints: origin {} palm {} residual {} m", name, frames.size(), arm.getBaseOrigin(), world, residual);
    if (config != null && config.reachCloud) {
      setReachCloud(true);
    }
    return world;
  }

  public Point onSceneReady(String jmeName) {
    if (worldCalibrated) {
      log.info("Simulator scene ready {} — keeping existing FABRIK calibration", jmeName);
      String name = requireCurrentArm();
      return name != null ? currentPositionWorld(name) : null;
    }
    log.info("Simulator scene ready {} — calibrating FABRIK world frame", jmeName);
    JMonkeyEngine jme = (JMonkeyEngine) Runtime.getService(jmeName);
    return calibrateFromSimulator(jme);
  }

  public Point centerAllJoints() {
    String name = requireCurrentArm();
    if (name == null) {
      return null;
    }
    return centerAllJoints(name);
  }

  public Point centerAllJoints(String name) {
    FabrikArm arm = arms.get(name);
    if (arm == null) {
      return null;
    }
    arm.centerAllJoints();
    Point palm = arm.getPalmPosition();
    invoke("publishIkGoal", palm);
    publishTelemetry(name);
    return palm;
  }

  public Point computePositionFromServos() {
    String name = requireCurrentArm();
    if (name == null) {
      return null;
    }
    return computePositionFromServos(name);
  }

  public Point computePositionFromServos(String name) {
    FabrikArm arm = arms.get(name);
    if (arm == null) {
      error("No arm named %s", name);
      return null;
    }
    int found = 0;
    for (int i = 0; i < arm.getNumJoints(); i++) {
      String servoName = arm.getFrame(i).name;
      if (servoName == null) {
        continue;
      }
      org.myrobotlab.framework.interfaces.ServiceInterface si = Runtime.getService(servoName);
      if (!(si instanceof ServoControl)) {
        warn("No servo named {} — skip FABRIK joint", servoName);
        continue;
      }
      double servoPos = ((ServoControl) si).getCurrentInputPos();
      arm.setFromServoDegrees(i, servoPos);
      found++;
      log.info("{} from servo {}° → mesh {}°", servoName, servoPos, arm.getThetaDegrees(i));
    }
    if (found == 0) {
      error("No servos found for arm %s — cannot compute position", name);
      return null;
    }
    publishTelemetry(name);
    Point world = currentPositionWorld(name);
    invoke("publishIkGoal", world);
    log.info("FABRIK forward kinematics from servos world {}", world);
    return world;
  }

  public Point publishIkGoal(Point goal) {
    ikGoal = goal;
    return goal;
  }

  public Point publishWorldPosition(Point position) {
    worldPosition = position;
    return position;
  }

  /**
   * Cyan voxel cloud of palm positions this arm can reach. Shown in the
   * simulator when attached to JMonkeyEngine.
   */
  public PointCloud publishReachCloud(PointCloud cloud) {
    reachCloudPointCount = cloud == null ? 0 : cloud.size();
    return cloud;
  }

  public boolean getReachCloud() {
    return config != null && config.reachCloud;
  }

  /**
   * Sample the current arm's joint ranges and show (or hide) the reach cloud
   * in the simulator.
   */
  public boolean setReachCloud(boolean show) {
    if (config == null) {
      return false;
    }
    if (!show) {
      config.reachCloud = false;
      reachCloudPointCount = 0;
      JMonkeyEngine jme = findSimulator();
      if (jme != null) {
        jme.setReachCloud(false);
      }
      broadcastState();
      return false;
    }
    PointCloud cloud = sampleReachCloud();
    if (cloud == null || cloud.size() == 0) {
      error("No left-arm chain to sample — setCurrentArm or calibrate from the simulator first");
      config.reachCloud = false;
      broadcastState();
      return false;
    }
    config.reachCloud = true;
    invoke("publishReachCloud", cloud);
    JMonkeyEngine jme = findSimulator();
    if (jme != null) {
      jme.onReachCloud(cloud);
    }
    log.info("FABRIK reach cloud {} voxels", cloud.size());
    broadcastState();
    return true;
  }

  public PointCloud sampleReachCloud() {
    FabrikArm arm = currentArmModel();
    if (arm == null || arm.getNumJoints() == 0) {
      return null;
    }
    int steps = config != null && config.reachCloudSteps > 1 ? config.reachCloudSteps : ReachCloud.DEFAULT_STEPS;
    float voxel = config != null && config.reachCloudVoxelM > 0f ? config.reachCloudVoxelM : ReachCloud.DEFAULT_VOXEL_M;
    PointCloud cloud = ReachCloud.sample(arm, steps, voxel);
    reachCloudPointCount = cloud.size();
    return cloud;
  }

  @Override
  public Map<String, Double> publishJointAngles(Map<String, Double> angleMap) {
    return angleMap;
  }

  public double[][] publishJointPositions(double[][] positions) {
    return positions;
  }

  public void publishTelemetry(String name) {
    FabrikArm arm = arms.get(name);
    if (arm == null) {
      return;
    }
    Map<String, Double> angleMap = arm.toServoMap();
    lastSolveError = arm.getLastError();
    lastSolveIterations = arm.getLastIterations();
    broadcast("publishJointAngles", angleMap);
    invoke("publishJointPositions", (Object) arm.getSkeleton());
    invoke("publishWorldPosition", arm.getPalmPosition());
  }

  public void setCurrentArm(String name, FabrikArm arm) {
    this.arms.put(name, arm);
    this.currentArm = name;
    installCalibrationFromConfig();
    worldPosition = currentPositionWorld(name);
  }

  /**
   * Build a FABRIK chain from the same default InMoov joint frames the Jacobian
   * solver uses.
   */
  public void setCurrentArm(String name, String robot, String side) {
    FabrikArm arm = new FabrikArm(String.format("%s.%sArm", robot, side));
    arm.applyJointFrames(InMoov2Arm.getDefaultJointFrames(robot, side), InMoov2Arm.getDefaultEndEffector(side));
    setCurrentArm(name, arm);
  }

  private FabrikArm currentArmModel() {
    if (currentArm != null && arms.containsKey(currentArm)) {
      return arms.get(currentArm);
    }
    return null;
  }

  private String requireCurrentArm() {
    if (currentArm != null && arms.containsKey(currentArm)) {
      return currentArm;
    }
    if (arms.size() == 1) {
      currentArm = arms.keySet().iterator().next();
      return currentArm;
    }
    error("No current arm set — call setCurrentArm first");
    return null;
  }

  private void installCalibrationFromConfig() {
    FabrikArm arm = currentArmModel();
    if (arm == null || config == null) {
      return;
    }
    List<JointFrame> frames = config.toJointFrames();
    if (frames.isEmpty()) {
      return;
    }
    if (arm.getNumJoints() != 0 && frames.size() != arm.getNumJoints()) {
      warn("saved FABRIK calibration has %d joints but the arm has %d — ignoring it", frames.size(), arm.getNumJoints());
      return;
    }
    Point endEffector = config.endEffectorSet ? new Point(config.endEffectorX, config.endEffectorY, config.endEffectorZ) : null;
    if (endEffector != null && arm.applyJointFrames(frames, endEffector)) {
      worldCalibrated = true;
      log.info("restored FABRIK calibration for {} — origin {} palm {}", currentArm, arm.getBaseOrigin(), arm.getPalmPosition());
    }
  }

  private void persistCalibration(FabrikArm arm, List<JointFrame> frames, Point endEffectorWorld) {
    if (config == null) {
      return;
    }
    config.worldFrame = true;
    config.fromJointFrames(frames);
    if (endEffectorWorld != null) {
      config.endEffectorSet = true;
      config.endEffectorX = endEffectorWorld.getX();
      config.endEffectorY = endEffectorWorld.getY();
      config.endEffectorZ = endEffectorWorld.getZ();
    } else {
      config.endEffectorSet = false;
    }
    Point origin = arm.getBaseOrigin();
    config.originX = origin.getX();
    config.originY = origin.getY();
    config.originZ = origin.getZ();
  }

  private void subscribeToSimulatorClicks() {
    for (ServiceInterface si : Runtime.getServices()) {
      if (si instanceof JMonkeyEngine) {
        subscribe(si.getName(), "publishClickPoint", getName(), "onPoint");
      }
    }
  }

  @Override
  public void attach(Attachable attachable) {
    if (attachable instanceof JMonkeyEngine) {
      addListener("publishWorldPosition", attachable.getName(), "onWorldPosition");
      addListener("publishIkGoal", attachable.getName(), "onIkGoal");
      addListener("publishReachCloud", attachable.getName(), "onReachCloud");
      subscribe(attachable.getName(), "publishSceneReady", getName(), "onSceneReady");
      subscribe(attachable.getName(), "publishClickPoint", getName(), "onPoint");
      return;
    }
    if (attachable instanceof InMoov2Arm) {
      warn("Do not attach IK joint angles to InMoov2Arm — attach InMoov2 instead.");
      return;
    }
    if (attachable instanceof IKJointAngleListener) {
      addListener("publishJointAngles", attachable.getName(), "onJointAngles");
    }
  }

  private JMonkeyEngine findSimulator() {
    for (ServiceInterface si : Runtime.getServices()) {
      if (si instanceof JMonkeyEngine) {
        return (JMonkeyEngine) si;
      }
    }
    return null;
  }

  static Point lerp(Point a, Point b, double t) {
    return new Point(a.getX() + t * (b.getX() - a.getX()), a.getY() + t * (b.getY() - a.getY()), a.getZ() + t * (b.getZ() - a.getZ()));
  }

  static String[] parseArmLinkName(String linkName) {
    if (linkName == null) {
      return null;
    }
    int armAt = linkName.indexOf("Arm.");
    if (armAt <= 0) {
      return null;
    }
    String prefix = linkName.substring(0, armAt);
    int dot = prefix.lastIndexOf('.');
    if (dot <= 0 || dot == prefix.length() - 1) {
      return null;
    }
    return new String[] { prefix.substring(0, dot), prefix.substring(dot + 1) };
  }

  private static Point toPoint3(Point3df p) {
    if (p == null) {
      return null;
    }
    return new Point(p.x, p.y, p.z);
  }

  public static void main(String[] args) {
    LoggingFactory.init("info");
    Runtime.start("webgui", "WebGui");
    Fabrik fabrik = (Fabrik) Runtime.start("fabrik", "Fabrik");
    fabrik.setCurrentArm("left", "i01", "left");
    fabrik.centerAllJoints("left");
    log.info("FABRIK rest palm {}", fabrik.currentPosition("left"));
  }
}
