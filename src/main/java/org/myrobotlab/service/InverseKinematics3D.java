package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.kinematics.JointFrame;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.config.InverseKinematics3DConfig;
import org.myrobotlab.service.data.JoystickData;
import org.myrobotlab.service.interfaces.IKJointAngleListener;
import org.myrobotlab.service.interfaces.IKJointAnglePublisher;
import org.myrobotlab.service.interfaces.PointsListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * InverseKinematics3D - 3D inverse kinematics from DH parameters using a
 * pseudo-inverse Jacobian gradient descent to move the end effector to a
 * desired x,y,z.
 *
 * <p>
 * <b>Frames:</b> the solver and public API are <em>meters</em>, Y-up — the same
 * as JMonkeyEngine. {@link #calibrateFromSimulator} measures each joint's real
 * rotation axis and origin off the rig (see {@link JointFrame}) and rebuilds the
 * chain from them, so forward kinematics reproduce the simulated arm at every
 * pose rather than only at the one that was sampled.
 * </p>
 *
 * <p>
 * <b>Joint space:</b> {@code servoDeg = servoSlope * thetaDeg + offset}, with
 * both terms taken from the simulator's own node mapper. Sharing that map is
 * what keeps the solver, the mesh and the physical servo from disagreeing about
 * the direction or scale of a joint.
 * </p>
 *
 * <p>
 * Rotation and orientation of the end effector are not currently solved.
 * </p>
 *
 * @author kwatters
 */
public class InverseKinematics3D extends Service<InverseKinematics3DConfig> implements IKJointAnglePublisher,PointsListener
{

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InverseKinematics3D.class);

  // private DHRobotArm currentArm = null;
  String currentArm = null;
  private final Map<String, DHRobotArm> arms = new TreeMap<String, DHRobotArm>();

  // we will track the joystick input to specify our velocity.
  private Point joystickLinearVelocity = new Point(0, 0, 0, 0, 0, 0);

  /**
   * Legacy mm-per-meter factor. The solver is native meters; keep this for
   * callers that still convert old millimeter samples.
   */
  public static final double IK_MM_PER_JME_METER = 1000.0;

  /** InMoov link lengths are tens of cm; reject scene-graph mistakes. */
  public static final double MIN_DH_LINK_M = 0.015;

  public static final double MAX_DH_LINK_M = 0.450;

  /**
   * Largest acceptable disagreement between the model and the rig during
   * {@link #verifyAgainstSimulator}, meters.
   */
  public static final double VERIFY_TOLERANCE_M = 0.005;

  private Matrix inputMatrix = null;

  /** World-frame origin of the base (first joint of the calibrated chain). */
  private Point worldOrigin = null;

  /**
   * Last computed palm position in world coordinates. Updated by
   * {@link #publishTelemetry(String)} so WebGui can display it.
   */
  public Point worldPosition = null;

  /**
   * Active IK Cartesian goal (world meters). The simulator green marker tracks
   * this, not the in-progress FK palm.
   */
  public Point ikGoal = null;

  /** World-meter step along a straight line for {@link #moveTo}. */
  public static final double CARTESIAN_STEP_M = 0.02;

  /**
   * Cap on {@link #moveTo} waypoints. Each one publishes a servo command, so a
   * long move should not turn into a hundred of them.
   */
  public static final int MAX_CARTESIAN_STEPS = 25;

  private boolean worldCalibrated = false;

  // check - http://myrobotlab.org/content/inverse-kinematics-update
  transient InputTrackingThread trackingThread = null;

  public InverseKinematics3D(String n, String id) {
    super(n, id);
  }

  @Override
  public InverseKinematics3DConfig apply(InverseKinematics3DConfig c) {
    super.apply(c);
    if (c != null) {
      installCalibrationFromConfig();
    }
    return c;
  }

  public void startTracking() {
    log.info("startTracking - starting new joystick input tracking thread {}_tracking", getName());
    if (trackingThread != null) {
      stopTracking();
    }
    trackingThread = new InputTrackingThread(String.format("%s_tracking", getName()));
    trackingThread.start();
  }

  public void stopTracking() {
    if (trackingThread != null) {
      trackingThread.setTracking(false);
    }
  }

  public class InputTrackingThread extends Thread {

    private boolean isTracking = false;

    public InputTrackingThread(String name) {
      super(name);
    }

    @Override
    public void run() {

      // Ok, here we are. if we're running..
      // we should be updating the move to based on the velocities
      // that are being tracked with the joystick.

      // how many ms to wait between movements.
      long pollInterval = 250;

      String arm = "myArm";
      isTracking = true;
      long now = System.currentTimeMillis();
      while (isTracking) {
        long pause = now + pollInterval - System.currentTimeMillis();
        try {
          // the number of milliseconds until we update the position
          Thread.sleep(pause);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          log.info("Interrupted tracking thread.");
          e.printStackTrace();
          isTracking = false;
        }
        // lets get the current position
        // current position + velocity * time
        Point current = currentPosition(arm);
        Point targetPoint = current.add(joystickLinearVelocity.multiplyXYZ(pollInterval / 1000.0));
        if (!targetPoint.equals(current)) {
          log.info("Velocity: {} Old: {} New: {}", joystickLinearVelocity, current, targetPoint);
        }

        invoke("publishTracking", targetPoint);
        moveTo(arm, targetPoint);
        // update current timestamp to determine how long we should wait
        // before the next moveTo is called.
        now = System.currentTimeMillis();
      }

    }

    public boolean isTracking() {
      return isTracking;
    }

    public void setTracking(boolean isTracking) {
      this.isTracking = isTracking;
    }
  }

  public Point currentPosition(String name) {
    return arms.get(name).getPalmPosition();
  }

  public String getCurrentArmName() {
    return currentArm;
  }

  /**
   * Move the current arm's palm to a point in the same frame as
   * {@link #worldPosition} / {@link #currentPositionWorld}: JME world meters
   * after calibration, or DH-local meters if the origin is still (0,0,0).
   * Used by the WebGui MoveTo form — do not convert through
   * {@link #toIkFrame}; the DH chain already applies {@code baseTransform}.
   */
  public Point moveTo(double x, double y, double z) {
    String name = requireCurrentArm();
    if (name == null) {
      return null;
    }
    return moveTo(name, x, y, z);
  }

  public Point moveTo(String arm, double x, double y, double z) {
    return moveTo(arm, new Point(x, y, z, 0, 0, 0));
  }

  /**
   * Legacy world→DH matrix kept for {@link #toIkFrame} when no arm is loaded.
   * {@link #moveTo} does <em>not</em> apply this; the arm {@code baseTransform}
   * already maps DH into world meters.
   */
  public Matrix createInputMatrix(double dx, double dy, double dz, double roll, double pitch, double yaw) {
    roll = MathUtils.degToRad(roll);
    pitch = MathUtils.degToRad(pitch);
    yaw = MathUtils.degToRad(yaw);
    Matrix trMatrix = Matrix.translation(dx, dy, dz);
    Matrix rotMatrix = Matrix.zRotation(roll).multiply(Matrix.yRotation(yaw).multiply(Matrix.xRotation(pitch)));
    inputMatrix = trMatrix.multiply(rotMatrix);
    return inputMatrix;
  }

  /**
   * Place the base at a world translation, leaving the measured joint geometry
   * alone. Rarely needed — {@link #calibrateFromSimulator} sets the base from the
   * first joint it measures.
   */
  public void calibrateToWorld(double originX, double originY, double originZ) {
    calibrateToWorld(originX, originY, originZ, 0, 0, 0);
  }

  /**
   * Install the transform that maps chain-local meters onto the world / JME
   * frame: {@code p_world = T(origin) R(roll,pitch,yaw) p_local}.
   */
  public void calibrateToWorld(double originX, double originY, double originZ, double rollDeg, double pitchDeg, double yawDeg) {
    worldOrigin = new Point(originX, originY, originZ);
    createInputMatrix(-originX, -originY, -originZ, rollDeg, pitchDeg, yawDeg);
    if (config != null) {
      config.worldFrame = true;
      config.originX = originX;
      config.originY = originY;
      config.originZ = originZ;
      config.originRoll = rollDeg;
      config.originPitch = pitchDeg;
      config.originYaw = yawDeg;
    }
    DHRobotArm arm = currentArmModel();
    if (arm != null) {
      arm.setBaseTransform(originX, originY, originZ, rollDeg, pitchDeg, yawDeg);
    }
    log.info("World calibration origin=({}, {}, {}) rpy=({}, {}, {})", originX, originY, originZ, rollDeg, pitchDeg, yawDeg);
    if (currentArm != null && arms.containsKey(currentArm)) {
      worldPosition = currentPositionWorld(currentArm);
    }
  }

  /**
   * Measure the arm off the first running JMonkeyEngine.
   */
  public Point calibrateFromSimulator() {
    for (org.myrobotlab.framework.interfaces.ServiceInterface si : Runtime.getServices()) {
      if (si instanceof JMonkeyEngine) {
        return calibrateFromSimulator((JMonkeyEngine) si);
      }
    }
    error("No JMonkeyEngine running — start the simulator first");
    return null;
  }

  /**
   * Rebuild the current arm from joints measured on the simulator's rig.
   *
   * <p>
   * Each of the arm's nodes contributes its real rotation axis, a point on that
   * axis, its current angle and the servo&rarr;mesh map it was configured with.
   * The end effector is the wrist node's world position, which depends only on
   * the four arm joints. Nothing here guesses axis signs or fits DH parameters —
   * the previous approach matched one pose by translation, which cannot detect a
   * chain whose joint axes or rotation directions are wrong, so the error grew
   * with every degree the arm moved.
   * </p>
   *
   * @return the model's end effector in world meters, or null on failure
   */
  public Point calibrateFromSimulator(JMonkeyEngine jme) {
    if (jme == null) {
      return null;
    }
    String name = requireCurrentArm();
    if (name == null) {
      return null;
    }
    DHRobotArm arm = arms.get(name);
    String[] parsed = parseArmLinkName(arm.getLink(0) != null ? arm.getLink(0).getName() : null);
    if (parsed == null) {
      error("Cannot parse robot/side from link name %s", arm.getLink(0) != null ? arm.getLink(0).getName() : null);
      return null;
    }
    String robot = parsed[0];
    String side = parsed[1];

    List<JointFrame> frames = jme.getArmJointFrames(robot, side);
    if (frames.size() != arm.getNumLinks()) {
      error("Measured %d of %d %s arm joints — check VinMoov node names and that simulator node mappers were applied", frames.size(), arm.getNumLinks(), side);
      return null;
    }
    Point wrist = toPoint3(jme.getHandWorldTranslation(robot, side));
    if (wrist == null) {
      error("No wrist node for %s %s — cannot place the end effector", robot, side);
      return null;
    }
    return calibrateFromJointFrames(name, frames, wrist);
  }

  /**
   * Rebuild an arm from measured joints. See {@link #calibrateFromSimulator}.
   */
  public Point calibrateFromJointFrames(String name, List<JointFrame> frames, Point endEffectorWorld) {
    DHRobotArm arm = arms.get(name);
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
    worldOrigin = arm.getBaseOrigin();
    persistCalibration(arm, frames, endEffectorWorld);
    worldCalibrated = true;
    Point world = currentPositionWorld(name);
    worldPosition = world;
    invoke("publishWorldPosition", world);
    invoke("publishIkGoal", world);
    double residual = endEffectorWorld == null ? 0 : world.distanceTo(endEffectorWorld);
    log.info("Calibrated {} from {} measured joints: origin {} end effector {} residual {} m, segments {}", name, frames.size(), worldOrigin, world, residual,
        measuredSegmentLengths(frames, endEffectorWorld));
    if (residual > 1e-6) {
      warn("calibration residual %.4f m — the tool offset should reproduce the sampled pose exactly", residual);
    }
    return world;
  }

  /**
   * Straight-line distances between consecutive measured joints, plus the last
   * joint to the end effector. These are the arm's real link lengths, replacing
   * the hand-entered defaults in {@code InMoov2Arm}.
   */
  static List<Double> measuredSegmentLengths(List<JointFrame> frames, Point endEffectorWorld) {
    List<Double> lengths = new ArrayList<>();
    for (int i = 1; i < frames.size(); i++) {
      lengths.add(round4(frames.get(i - 1).origin.distanceTo(frames.get(i).origin)));
    }
    if (endEffectorWorld != null && !frames.isEmpty()) {
      lengths.add(round4(frames.get(frames.size() - 1).origin.distanceTo(endEffectorWorld)));
    }
    return lengths;
  }

  private static double round4(double v) {
    return Math.round(v * 10000.0) / 10000.0;
  }

  /**
   * {@link #verifyAgainstSimulator(JMonkeyEngine, int)} against the first running
   * simulator. Exposed for the WebGui "Verify against simulator" button.
   *
   * @return the worst disagreement in meters, or {@code NaN}
   */
  public double verifyAgainstSimulator() {
    for (org.myrobotlab.framework.interfaces.ServiceInterface si : Runtime.getServices()) {
      if (si instanceof JMonkeyEngine) {
        return verifyAgainstSimulator((JMonkeyEngine) si, 5);
      }
    }
    error("No JMonkeyEngine running — start the simulator first");
    return Double.NaN;
  }

  /**
   * Sweep each joint across its range, command the rig directly and compare the
   * simulated wrist to the model's end effector.
   *
   * <p>
   * This is the check a single-pose fit cannot do. A model with the wrong joint
   * axes or a mirrored rotation direction matches at rest and drifts everywhere
   * else, so agreement has to be demonstrated across the workspace.
   * </p>
   *
   * @param jme
   *          the running simulator
   * @param stepsPerJoint
   *          how many angles to test per joint (minimum 2)
   * @return the largest disagreement in meters, or {@code NaN} if it could not
   *         run
   */
  public double verifyAgainstSimulator(JMonkeyEngine jme, int stepsPerJoint) {
    String name = requireCurrentArm();
    if (jme == null || name == null) {
      return Double.NaN;
    }
    DHRobotArm arm = arms.get(name);
    String[] parsed = parseArmLinkName(arm.getLink(0).getName());
    if (parsed == null) {
      return Double.NaN;
    }
    String robot = parsed[0];
    String side = parsed[1];
    int steps = Math.max(2, stepsPerJoint);

    double[] restoreServo = new double[arm.getNumLinks()];
    for (int i = 0; i < arm.getNumLinks(); i++) {
      restoreServo[i] = arm.getLink(i).toServoDegrees();
    }

    double worst = 0;
    String worstLabel = "none";
    try {
      for (int j = 0; j < arm.getNumLinks(); j++) {
        DHLink link = arm.getLink(j);
        double servoLo = Math.min(link.servoMin, link.servoMax);
        double servoHi = Math.max(link.servoMin, link.servoMax);
        for (int s = 0; s < steps; s++) {
          double servo = servoLo + (servoHi - servoLo) * s / (double) (steps - 1);
          jme.rotateOnAxis(link.getName(), null, servo);
          link.setFromServoDegrees(servo);
          sleep(150);
          Point simWrist = toPoint3(jme.getHandWorldTranslation(robot, side));
          if (simWrist == null) {
            continue;
          }
          double err = arm.getPalmPosition().distanceTo(simWrist);
          log.info("verify {} servo {} model {} sim {} err {} m", link.getName(), servo, arm.getPalmPosition(), simWrist, err);
          if (err > worst) {
            worst = err;
            worstLabel = String.format("%s @ %.1f", link.getName(), servo);
          }
        }
        // put this joint back before sweeping the next one
        jme.rotateOnAxis(link.getName(), null, restoreServo[j]);
        link.setFromServoDegrees(restoreServo[j]);
        sleep(150);
      }
    } finally {
      for (int i = 0; i < arm.getNumLinks(); i++) {
        jme.rotateOnAxis(arm.getLink(i).getName(), null, restoreServo[i]);
        arm.getLink(i).setFromServoDegrees(restoreServo[i]);
      }
      publishTelemetry(name);
    }

    if (worst > VERIFY_TOLERANCE_M) {
      error("model disagrees with the simulator by %.4f m (worst at %s) — calibration is not valid across the workspace", worst, worstLabel);
    } else {
      log.info("model matches the simulator within {} m across {} joints (worst {})", round4(worst), arm.getNumLinks(), worstLabel);
    }
    return worst;
  }

  public Point onSceneReady(String jmeName) {
    if (worldCalibrated) {
      log.info("Simulator scene ready {} — keeping existing world calibration", jmeName);
      String name = requireCurrentArm();
      return name != null ? currentPositionWorld(name) : null;
    }
    log.info("Simulator scene ready {} — calibrating IK world frame", jmeName);
    JMonkeyEngine jme = (JMonkeyEngine) Runtime.getService(jmeName);
    return calibrateFromSimulator(jme);
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

  private static Point toPoint3(org.myrobotlab.math.geometry.Point3df p) {
    if (p == null) {
      return null;
    }
    return new Point(p.x, p.y, p.z);
  }

  /**
   * Fit a last-frame tool offset so the IK palm matches a world-frame wrist at
   * the current pose. Requires a base transform first. The offset rotates with
   * the arm (unlike origin-fitting).
   */
  public Point fitToolOffsetFromWorld(String name, Point endEffectorWorld) {
    DHRobotArm arm = arms.get(name);
    if (arm == null || endEffectorWorld == null) {
      error("Cannot fit tool offset — arm or end effector missing");
      return null;
    }
    Point offset = arm.fitToolOffset(endEffectorWorld);
    persistToolOffset(arm);
    worldPosition = currentPositionWorld(name);
    log.info("Tool offset {} — IK world palm now {}", offset, worldPosition);
    return offset;
  }

  public Point fitToolOffsetFromWorld(Point endEffectorWorld) {
    String name = requireCurrentArm();
    if (name == null) {
      return null;
    }
    return fitToolOffsetFromWorld(name, endEffectorWorld);
  }

  static double distM(Point a, Point b) {
    if (a == null || b == null) {
      return 0;
    }
    return a.distanceTo(b);
  }

  /**
   * Map a world point into the chain-local frame (inverse of the arm base
   * transform).
   */
  public Point toIkFrame(Point world) {
    DHRobotArm arm = currentArmModel();
    if (arm != null) {
      return arm.toLocalFrame(world);
    }
    return inputMatrix != null ? rotateAndTranslate(world) : world;
  }

  /**
   * Map a chain-local point into world / JME coordinates.
   */
  public Point toWorldFrame(Point ik) {
    DHRobotArm arm = currentArmModel();
    if (arm != null) {
      return arm.toWorldFrame(ik);
    }
    return inputMatrix != null ? inverseRotateAndTranslate(ik) : ik;
  }

  public Point currentPositionWorld(String name) {
    return currentPosition(name);
  }

  public Point getWorldOrigin() {
    DHRobotArm arm = currentArmModel();
    if (arm != null) {
      return arm.getBaseOrigin();
    }
    return worldOrigin;
  }

  public boolean isWorldFrameEnabled() {
    return config == null || config.worldFrame;
  }

  public Point rotateAndTranslate(Point pIn) {

    Matrix m = new Matrix(4, 1);
    m.elements[0][0] = pIn.getX();
    m.elements[1][0] = pIn.getY();
    m.elements[2][0] = pIn.getZ();
    m.elements[3][0] = 1;
    Matrix pOM = inputMatrix.multiply(m);

    // TODO: compute the roll pitch yaw
    double roll = 0;
    double pitch = 0;
    double yaw = 0;

    Point pOut = new Point(pOM.elements[0][0], pOM.elements[1][0], pOM.elements[2][0], roll, pitch, yaw);
    return pOut;
  }

  /**
   * Inverse of {@link #rotateAndTranslate}: {@code p = R^T (p' − t)} for
   * {@code inputMatrix = T R}.
   */
  public Point inverseRotateAndTranslate(Point pIn) {
    if (inputMatrix == null) {
      return pIn;
    }
    double dx = pIn.getX() - inputMatrix.elements[0][3];
    double dy = pIn.getY() - inputMatrix.elements[1][3];
    double dz = pIn.getZ() - inputMatrix.elements[2][3];
    double x = inputMatrix.elements[0][0] * dx + inputMatrix.elements[1][0] * dy + inputMatrix.elements[2][0] * dz;
    double y = inputMatrix.elements[0][1] * dx + inputMatrix.elements[1][1] * dy + inputMatrix.elements[2][1] * dz;
    double z = inputMatrix.elements[0][2] * dx + inputMatrix.elements[1][2] * dy + inputMatrix.elements[2][2] * dz;
    return new Point(x, y, z, pIn.getRoll(), pIn.getPitch(), pIn.getYaw());
  }

  public void centerAllJoints() {
    String name = requireCurrentArm();
    if (name == null) {
      return;
    }
    centerAllJoints(name);
  }

  public void centerAllJoints(String name) {
    DHRobotArm arm = arms.get(name);
    arm.centerAllJoints();
    invoke("publishIkGoal", arm.getPalmPosition());
    publishTelemetry(name);
  }

  /**
   * Read current servo input positions into the DH model (inverse of the
   * {@code theta → servo} mapping used in {@link #publishTelemetry}) and
   * publish the resulting forward-kinematics palm in the world frame.
   *
   * @return world-frame palm, or null if the current arm or servos are missing
   */
  public Point computePositionFromServos() {
    String name = requireCurrentArm();
    if (name == null) {
      return null;
    }
    return computePositionFromServos(name);
  }

  public Point computePositionFromServos(String name) {
    DHRobotArm arm = arms.get(name);
    if (arm == null) {
      error("No arm named %s", name);
      return null;
    }
    int found = 0;
    for (DHLink link : arm.getLinks()) {
      String servoName = link.getName();
      if (servoName == null) {
        continue;
      }
      org.myrobotlab.framework.interfaces.ServiceInterface si = Runtime.getService(servoName);
      if (!(si instanceof ServoControl)) {
        warn("No servo named {} — skip DH link", servoName);
        continue;
      }
      double servoPos = ((ServoControl) si).getCurrentInputPos();
      link.setFromServoDegrees(servoPos);
      found++;
      log.info("{} from servo {}° → theta {}°", servoName, servoPos, link.getThetaDegrees());
    }
    if (found == 0) {
      error("No servos found for arm %s — cannot compute position", name);
      return null;
    }
    publishTelemetry(name);
    Point world = currentPositionWorld(name);
    invoke("publishIkGoal", world);
    log.info("Forward kinematics from servos world {}", world);
    return world;
  }

  /**
   * Solve IK so the palm reaches {@code p}. {@code p} is already in the palm /
   * world frame ({@link DHRobotArm#getPalmPosition()}); the old
   * {@link #createInputMatrix} scale/translate path is not applied here.
   * <p>
   * Publishes {@link #publishIkGoal} immediately (simulator green marker), then
   * walks a straight Cartesian line in {@link #CARTESIAN_STEP_M} steps so the
   * arm iterates toward the goal instead of jumping.
   *
   * @return reached world palm (same as {@link #worldPosition} after publish)
   */
  public Point moveTo(String name, Point p) {
    DHRobotArm arm = arms.get(name);
    if (arm == null) {
      error("No arm named %s", name);
      return null;
    }
    Point current = arm.getPalmPosition();
    Point origin = arm.getBaseOrigin();
    double travel = current.distanceTo(p);
    log.info("moveTo {} world goal {} current {} origin {} travel {} m", name, p, current, origin, travel);
    if (travel > 0.80) {
      warn("moveTo goal is {} m from the palm — likely a DH-local point sent as world, or an unreachable pose. Origin {}", String.format("%.3f", travel), origin);
    }
    invoke("publishIkGoal", p);
    int steps = Math.max(1, Math.min(MAX_CARTESIAN_STEPS, (int) Math.ceil(travel / CARTESIAN_STEP_M)));
    int missed = 0;
    for (int i = 1; i <= steps; i++) {
      Point waypoint = lerp(current, p, (double) i / steps);
      if (!arm.moveToGoal(waypoint)) {
        // keep walking rather than aborting - the solver leaves the arm at its
        // closest reachable pose, and a later waypoint may be reachable again
        missed++;
        log.debug("missed waypoint {}/{} {} by {} m", i, steps, waypoint, arm.distanceToGoal(waypoint));
      }
      publishTelemetry(name);
    }
    Point reached = currentPositionWorld(name);
    double err = reached.distanceTo(p);
    if (missed > 0) {
      warn("moveTo %s got within %.4f m of %s (%d of %d waypoints unreachable)", name, err, p, missed, steps);
    } else {
      log.info("moveTo {} reached {} goal {} err {} m", name, reached, p, err);
    }
    return reached;
  }

  static Point lerp(Point a, Point b, double t) {
    return new Point(a.getX() + t * (b.getX() - a.getX()), a.getY() + t * (b.getY() - a.getY()), a.getZ() + t * (b.getZ() - a.getZ()));
  }

  public Point publishIkGoal(Point goal) {
    ikGoal = goal;
    return goal;
  }

  // public void publishTelemetry()

  // TODO - publishTelemetry() which iterates through all parts
  public void publishTelemetry(String name) {
    Map<String, Double> angleMap = new HashMap<String, Double>();
    for (DHLink l : arms.get(name).getLinks()) {
      String jointName = l.getName();
      if (jointName == null) {
        continue;
      }
      // servoSlope * thetaDeg + offset, clamped to the joint's servo range.
      // No modulo-360 wrap: it is not invertible, so computePositionFromServos
      // could not read back what was published, and it mangles negative slopes.
      angleMap.put(jointName, l.toServoDegrees());
      log.debug("Servo : {}  Angle : {}", jointName, angleMap.get(jointName));
    }
    // Synchronous delivery so InMoov2/arm listeners move before the next IK step
    broadcast("publishJointAngles", angleMap);
    // we want to publish the joint positions
    // this way we can render on the web gui..
    double[][] jointPositionMap = createJointPositionMap(name);
    // TODO: pass a better datastructure?
    invoke("publishJointPositions", (Object) jointPositionMap);
    invoke("publishWorldPosition", currentPositionWorld(name));
  }

  public double[][] createJointPositionMap(String name) {

    DHRobotArm arm = arms.get(name);
    double[][] jointPositionMap = new double[arm.getNumLinks() + 1][3];

    Point origin = arm.getBaseOrigin();
    jointPositionMap[0][0] = origin.getX();
    jointPositionMap[0][1] = origin.getY();
    jointPositionMap[0][2] = origin.getZ();

    for (int i = 1; i <= arm.getNumLinks(); i++) {
      Point jp = arm.getJointPosition(i - 1);
      jointPositionMap[i][0] = jp.getX();
      jointPositionMap[i][1] = jp.getY();
      jointPositionMap[i][2] = jp.getZ();
    }
    return jointPositionMap;
  }

  public DHRobotArm getCurrentArm(String name) {
    return arms.get(name);
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

  public void setCurrentArm(String name, DHRobotArm arm) {
    arm.setIk3D(this);
    this.arms.put(name, arm);
    this.currentArm = name;
    installCalibrationFromConfig();
    worldPosition = currentPositionWorld(name);
  }

  private DHRobotArm currentArmModel() {
    if (currentArm != null && arms.containsKey(currentArm)) {
      return arms.get(currentArm);
    }
    return null;
  }

  /**
   * Rebuild the current arm from a saved calibration so a restart does not need
   * the simulator running. Falls back to the arm's built-in default geometry when
   * nothing was saved.
   */
  private void installCalibrationFromConfig() {
    DHRobotArm arm = currentArmModel();
    if (arm == null || config == null) {
      return;
    }
    List<JointFrame> frames = config.toJointFrames();
    if (frames.isEmpty()) {
      return;
    }
    if (frames.size() != arm.getNumLinks()) {
      warn("saved calibration has %d joints but the arm has %d — ignoring it", frames.size(), arm.getNumLinks());
      return;
    }
    Point endEffector = config.endEffectorSet ? new Point(config.endEffectorX, config.endEffectorY, config.endEffectorZ) : null;
    if (arm.applyJointFrames(frames, endEffector)) {
      worldOrigin = arm.getBaseOrigin();
      worldCalibrated = true;
      log.info("restored measured calibration for {} — origin {} palm {}", currentArm, worldOrigin, arm.getPalmPosition());
    }
  }

  /**
   * Save the measured joints so {@link #installCalibrationFromConfig} can restore
   * them, plus the resulting link lengths for human reference.
   */
  private void persistCalibration(DHRobotArm arm, List<JointFrame> frames, Point endEffectorWorld) {
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
    config.originRoll = 0;
    config.originPitch = 0;
    config.originYaw = 0;
    persistToolOffset(arm);

    List<Double> segments = measuredSegmentLengths(frames, endEffectorWorld);
    if (segments.size() >= 4) {
      config.omoplateA = segments.get(0);
      config.shoulderD = segments.get(1);
      config.rotateD = segments.get(2);
      config.bicepA = segments.get(3);
    }
  }

  private void persistToolOffset(DHRobotArm arm) {
    if (config == null || arm == null) {
      return;
    }
    Point t = arm.getToolOffset();
    if (t == null) {
      config.toolOffsetSet = false;
      return;
    }
    config.toolOffsetSet = true;
    config.toolOffsetX = t.getX();
    config.toolOffsetY = t.getY();
    config.toolOffsetZ = t.getZ();
  }

  @Override
  public void attach(Attachable attachable) {
    if (attachable instanceof JMonkeyEngine) {
      addListener("publishWorldPosition", attachable.getName(), "onWorldPosition");
      addListener("publishIkGoal", attachable.getName(), "onIkGoal");
      subscribe(attachable.getName(), "publishSceneReady", getName(), "onSceneReady");
      return;
    }
    if (attachable instanceof InMoov2Arm) {
      warn("Do not attach IK joint angles to InMoov2Arm — its deprecated onJointAngles applies a second gain/phase map. Attach InMoov2 instead.");
      return;
    }
    if (attachable instanceof IKJointAngleListener) {
      addListener("publishJointAngles", attachable.getName(), "onJointAngles");
    }
  }

  public static void main(String[] args) throws Exception {
    LoggingFactory.init("info");

    String arm = "myArm";
    Runtime.createAndStart("python", "Python");
    Runtime.createAndStart("gui", "SwingGui");

    InverseKinematics3D inversekinematics = (InverseKinematics3D) Runtime.start("ik3d", "InverseKinematics3D");
    // InverseKinematics3D inversekinematics = new InverseKinematics3D("iksvc");
    inversekinematics.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    //
    // inversekinematics.getCurrentArm(arm).setIk3D(inversekinematics);
    // Create a new DH Arm.. simpler for initial testing.
    // d , r, theta , alpha
    // DHRobotArm testArm = new DHRobotArm();
    // testArm.addLink(new DHLink("one" ,400,0,0,90));
    // testArm.addLink(new DHLink("two" ,300,0,0,90));
    // testArm.addLink(new DHLink("three",200,0,0,0));
    // testArm.addLink(new DHLink("two", 0,0,0,0));
    // inversekinematics.setCurrentArm(testArm);
    // set up our input translation/rotation
    //
    // if (false) {
    // double dx = 400.0;
    // double dy = -600.0;
    // double dz = -350.0;
    // double roll = 0.0;
    // double pitch = 0.0;
    // double yaw = 0.0;
    // inversekinematics.createInputMatrix(dx, dy, dz, roll, pitch, yaw);
    // }

    // Rest position...
    // Point rest = new Point(100,-300,0,0,0,0);
    // rest.
    // inversekinematics.moveTo(rest);

    // LeapMotion lm = (LeapMotion)Runtime.start("leap", "LeapMotion");
    // lm.addPointsListener(inversekinematics);

    boolean attached = true;
    if (attached) {
      // set up the left inmoov arm
      InMoov2Arm leftArm = (InMoov2Arm) Runtime.start("leftArm", "InMoov2Arm");
      // leftArm.connect("COM21");
      // leftArm.omoplate.setMinMax(0, 180);
      // attach the publish joint angles to the on JointAngles for the inmoov
      // arm.
      inversekinematics.addListener("publishJointAngles", leftArm.getName(), "onJointAngles");
    }

    // Runtime.createAndStart("gui", "SwingGui");
    // OpenCV cv1 = (OpenCV)Runtime.createAndStart("cv1", "OpenCV");
    // OpenCVFilterAffine aff1 = new OpenCVFilterAffine("aff1");
    // aff1.setAngle(270);
    // aff1.setDx(-80);
    // aff1.setDy(-80);
    // cv1.addFilter(aff1);
    //
    // cv1.setCameraIndex(0);
    // cv1.capture();
    // cv1.undockDisplay(true);

    /*
     * SwingGui gui = new SwingGui("gui"); gui.startService();
     */

    Joystick joystick = (Joystick) Runtime.start("joystick", "Joystick");
    joystick.setController(2);

    // joystick.startPolling();

    // attach the joystick input to the ik3d service.
    joystick.addInputListener(inversekinematics);

    Runtime.start("webgui", "WebGui");
    Runtime.start("log", "Log");
  }

  @Override
  public Map<String, Double> publishJointAngles(Map<String, Double> angleData) {
    return angleData;
  }

  public double[][] publishJointPositions(double[][] jointPositionMap) {
    return jointPositionMap;
  }

  public Point publishTracking(Point tracking) {
    return tracking;
  }

  public Point publishWorldPosition(Point position) {
    worldPosition = position;
    return position;
  }

  // input data from point publisher
  public void onPoint(Point point) {
    // TODO : move input matrix translation to here? or somewhere?
    // TODO: also don't like that i'm going to just say take the first point
    // now.
    // TODO: points should probably be a map, each point should have a name ?
    log.info("Attempting to move to {}", point);

    // TODO: scale / translate & rotate...
    moveTo(currentArm, point);
  }

  @Override
  public void onPoints(List<Point> points) {
    // TODO : move input matrix translation to here? or somewhere?
    // TODO: also don't like that i'm going to just say take the first point
    // now.
    // TODO: points should probably be a map, each point should have a name ?
    moveTo(currentArm, points.get(0));
  }

  public void onJoystickInput(JoystickData input) {

    // a few control button pushes
    // Ok, lets say the the "a" button starts tracking
    if ("0".equals(input.id)) {
      log.info("Start Tracking button pushed.");
      startTracking();
    } else if ("1".equals(input.id)) {
      stopTracking();
    }
    // and the "b" button stops tracking
    // TODO: use the joystick input to drive the "moveTo" command.
    // TODO: joystick listener interface?
    // input.id
    // input.value
    // depending on input we want to get the current position and move in some
    // direction.
    // or potentially stay in the same place..
    // we start at the origin
    // initially at rest.
    // we can set the velocities to be equal to the joystick inputs
    // with some gain/amplification.
    // Ok, so this will track the y,rx,ry inputs from the joystick as x,y,z
    // velocities

    // we want to have a minimum threshold o/w we set the value to zero
    // quantize
    float threshold = 0.1F;
    if (Math.abs(input.value) < threshold) {
      input.value = 0.0F;
    }

    double totalGain = 0.1;
    double xGain = totalGain;
    // invert y control.
    double yGain = -1.0 * totalGain;
    double zGain = totalGain;
    if ("x".equals(input.id)) {
      // x axis control (left/right)
      joystickLinearVelocity.setX(input.value * xGain);
    } else if ("y".equals(input.id)) {
      // y axis control (up/down)
      joystickLinearVelocity.setY(input.value * yGain);
    }
    if ("ry".equals(input.id)) {
      // z axis control (forward / backwards)
      joystickLinearVelocity.setZ(input.value * zGain);
    }
    // log.info("Linear Velocity : {}", joystickLinearVelocity);
    // on a loop I want to sample the current joystickLinearVelocity
    // at some interval and move the current position by the new dx,dy,dz
    // computed based
    // off the input from the joystick.
    // relying on the current position is probably bad.
    // TODO: track the desired position independently of the current position.
    // we will allow translation, x,y,z
    // for the input point.
  }

}