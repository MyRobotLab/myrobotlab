package org.myrobotlab.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHRobotArm;
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
 * desired x,y,z in the base frame.
 *
 * <p>
 * <b>Frames:</b> the DH solver works in <em>millimeters</em> with the origin at
 * the first joint (InMoov omoplate) and Y-up. Constructor thetas are InMoov
 * servo rest. The VinMoov / JMonkeyEngine simulator is <em>meters</em>, Y-up,
 * origin at the model root. {@link #calibrateToWorld} installs scale+translation
 * (omoplate origin, typically 1000 mm/m). A last-frame
 * {@link DHRobotArm#fitToolOffset} maps the DH palm onto the simulated wrist so
 * a bind-pose forward offset rotates with the arm.
 * </p>
 *
 * Rotation and orientation of the end effector are not currently solved.
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
   * JME / VinMoov world units are meters; DH / IK are millimeters.
   */
  public static final double IK_MM_PER_JME_METER = 1000.0;

  /** InMoov link lengths are tens of cm; reject scene-graph mistakes. */
  public static final double MIN_DH_LINK_MM = 15.0;

  public static final double MAX_DH_LINK_MM = 450.0;

  private Matrix inputMatrix = null;
  private Point scale = null;

  /** World-frame origin of the DH base (set by {@link #calibrateToWorld}). */
  private Point worldOrigin = null;

  /**
   * Last computed palm position in world coordinates. Updated by
   * {@link #publishTelemetry(String)} so WebGui can display it.
   */
  public Point worldPosition = null;

  // check - http://myrobotlab.org/content/inverse-kinematics-update
  transient InputTrackingThread trackingThread = null;

  public InverseKinematics3D(String n, String id) {
    super(n, id);
  }

  @Override
  public InverseKinematics3DConfig apply(InverseKinematics3DConfig c) {
    super.apply(c);
    if (c != null && c.worldFrame) {
      calibrateToWorld(c.originX, c.originY, c.originZ, c.scaleX, c.scaleY, c.scaleZ);
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
   * Move the current arm to a world-frame (or DH, if uncalibrated) point.
   * Used by the WebGui MoveTo form.
   */
  public void moveTo(double x, double y, double z) {
    String name = requireCurrentArm();
    if (name == null) {
      return;
    }
    moveTo(name, x, y, z);
  }

  public void moveTo(String arm, double x, double y, double z) {
    // TODO: allow passing roll pitch and yaw
    moveTo(arm, new Point(x, y, z, 0, 0, 0));
  }

  /**
   * This create a rotation and translation matrix that will be applied on the
   * "moveTo" call.
   * 
   * @param dx
   *              - x axis translation
   * @param dy
   *              - y axis translation
   * @param dz
   *              - z axis translation
   * @param roll
   *              - rotation about z (in degrees)
   * @param pitch
   *              - rotation about x (in degrees)
   * @param yaw
   *              - rotation about y (in degrees)
   * @return a matric that represents the rotation/translation matrix
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

  public Point createInputScale(double x, double y, double z) {
    scale = new Point(x, y, z, 0, 0, 0);
    return scale;
  }

  /**
   * Install the transform that maps simulator / world coordinates onto the DH
   * frame: {@code p_ik = scale ⊙ (p_world − origin)}.
   *
   * <p>
   * For VinMoov / JME the typical values are origin = omoplate world
   * translation (meters) and scale = (1000, 1000, 1000) (or a negated axis if
   * that axis is flipped relative to DH).
   * </p>
   *
   * @param originX
   *          world X of the DH base (omoplate)
   * @param originY
   *          world Y of the DH base
   * @param originZ
   *          world Z of the DH base
   * @param scaleX
   *          world-to-IK scale on X (mm per world unit)
   * @param scaleY
   *          world-to-IK scale on Y
   * @param scaleZ
   *          world-to-IK scale on Z
   */
  public void calibrateToWorld(double originX, double originY, double originZ, double scaleX, double scaleY, double scaleZ) {
    worldOrigin = new Point(originX, originY, originZ);
    createInputScale(scaleX, scaleY, scaleZ);
    createInputMatrix(-scaleX * originX, -scaleY * originY, -scaleZ * originZ, 0, 0, 0);
    if (config != null) {
      config.worldFrame = true;
      config.originX = originX;
      config.originY = originY;
      config.originZ = originZ;
      config.scaleX = scaleX;
      config.scaleY = scaleY;
      config.scaleZ = scaleZ;
    }
    log.info("World calibration origin=({}, {}, {}) scale=({}, {}, {})", originX, originY, originZ, scaleX, scaleY, scaleZ);
    if (currentArm != null && arms.containsKey(currentArm)) {
      worldPosition = currentPositionWorld(currentArm);
    }
  }

  /**
   * Default InMoov / JME calibration: millimeters vs meters, same Y-up axes,
   * origin at the given DH-base world position.
   */
  public void calibrateToWorld(double originX, double originY, double originZ) {
    calibrateToWorld(originX, originY, originZ, IK_MM_PER_JME_METER, IK_MM_PER_JME_METER, IK_MM_PER_JME_METER);
  }

  /**
   * Infer per-axis world→IK scale from a paired sample (IK palm in mm vs
   * end-effector minus origin in world units). Only the sign is inferred;
   * magnitude is always {@link #IK_MM_PER_JME_METER}. Degenerate axes (near-zero
   * IK or world component — typical for DH Z at the hanging pose) default to
   * +1000 so a leftover millimeter value is never treated as meters.
   *
   * @param ikPalm
   *          palm in the DH / IK frame (mm)
   * @param worldRelative
   *          {@code endEffectorWorld − originWorld}
   * @return scale vector to pass to {@link #calibrateToWorld}
   */
  public Point inferWorldScale(Point ikPalm, Point worldRelative) {
    double sx = inferAxisScale(ikPalm.getX(), worldRelative.getX());
    double sy = inferAxisScale(ikPalm.getY(), worldRelative.getY());
    double sz = inferAxisScale(ikPalm.getZ(), worldRelative.getZ());
    Point inferred = new Point(sx, sy, sz);
    log.info("Inferred world scale {} from IK {} vs world-relative {}", inferred, ikPalm, worldRelative);
    return inferred;
  }

  static double inferAxisScale(double ikMm, double world) {
    if (Math.abs(world) < 1e-4 || Math.abs(ikMm) < 1e-3) {
      return IK_MM_PER_JME_METER;
    }
    return Math.signum(ikMm / world) * IK_MM_PER_JME_METER;
  }

  /**
   * Sample the simulator and install the world-frame calibration so the IK palm
   * maps onto the simulated hand at this pose.
   *
   * <p>
   * Axis signs are inferred from {@code endEffector − originNode}. The DH origin
   * is then <em>fitted</em> to the end effector (not taken as the omoplate node
   * translation). The hanging DH palm sits at Z=0 while the VinMoov wrist is
   * forward of the shoulder; using the omoplate node as origin left a Z bias.
   * </p>
   *
   * @param originWorld
   *          DH-base node in world coordinates (omoplate), used for axis signs
   * @param ikPalm
   *          current IK palm (mm, DH origin)
   * @param endEffectorWorld
   *          hand / wrist in world coordinates
   * @return the inferred scale
   */
  public Point calibrateToSimulator(Point originWorld, Point ikPalm, Point endEffectorWorld) {
    Point worldRelative = endEffectorWorld.subtract(originWorld);
    Point inferred = inferWorldScale(ikPalm, worldRelative);
    Point fittedOrigin = new Point(endEffectorWorld.getX() - ikPalm.getX() / inferred.getX(), endEffectorWorld.getY() - ikPalm.getY() / inferred.getY(),
        endEffectorWorld.getZ() - ikPalm.getZ() / inferred.getZ());
    log.info("Fitted DH origin {} from end effector {} and IK {}", fittedOrigin, endEffectorWorld, ikPalm);
    calibrateToWorld(fittedOrigin.getX(), fittedOrigin.getY(), fittedOrigin.getZ(), inferred.getX(), inferred.getY(), inferred.getZ());
    return inferred;
  }

  /**
   * Map VinMoov bone lengths onto the 4 DH slots. Distances are world meters;
   * DH d/a are millimeters. Left/right sign on shoulder {@code d} is preserved.
   *
   * @return true if at least one length was updated
   */
  public boolean fitVinMoovLinkLengths(String name, Point omoplateWorld, Point shoulderWorld, Point rotateWorld, Point bicepWorld, Point wristWorld) {
    DHRobotArm arm = arms.get(name);
    if (arm == null || arm.getNumLinks() < 4) {
      error("Cannot fit VinMoov lengths — arm %s missing or short", name);
      return false;
    }
    boolean updated = false;
    updated |= setLinkLength(arm.getLink(0), true, distMm(omoplateWorld, shoulderWorld), "omoplate a");
    double shoulderD = distMm(shoulderWorld, rotateWorld);
    if (arm.getLink(1).getD() < 0) {
      shoulderD = -shoulderD;
    }
    updated |= setLinkLength(arm.getLink(1), false, shoulderD, "shoulder d");
    updated |= setLinkLength(arm.getLink(2), false, distMm(rotateWorld, bicepWorld), "rotate d");
    updated |= setLinkLength(arm.getLink(3), true, distMm(bicepWorld, wristWorld), "bicep a");
    return updated;
  }

  /**
   * Map DH millimeters onto a simulator pose: origin at the omoplate node,
   * per-axis scale (including a Y flip if the mesh rest is not hanging down),
   * and a last-frame wrist offset. Call after DH thetas match the servos.
   */
  public Point calibrateFromWorldSamples(Point omoplateWorld, Point wristWorld) {
    String name = requireCurrentArm();
    if (name == null || omoplateWorld == null || wristWorld == null) {
      error("calibrateFromWorldSamples needs a current arm, omoplate, and wrist");
      return null;
    }
    DHRobotArm arm = arms.get(name);
    arm.setToolOffset(null);
    Point ikPalm = currentPosition(name);
    Point worldRelative = wristWorld.subtract(omoplateWorld);
    Point inferred = inferWorldScale(ikPalm, worldRelative);
    calibrateToWorld(omoplateWorld.getX(), omoplateWorld.getY(), omoplateWorld.getZ(), inferred.getX(), inferred.getY(), inferred.getZ());
    fitToolOffsetFromWorld(name, wristWorld);
    Point world = currentPositionWorld(name);
    log.info("Calibrated from world samples origin={} scale={} IK world {} sim wrist {} err={} m", omoplateWorld, inferred, world, wristWorld, world.distanceTo(wristWorld));
    return world;
  }

  /**
   * Sample VinMoov omoplate + wrist and {@link #calibrateFromWorldSamples}.
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
      warn("Cannot parse robot/side from DH link name {}", arm.getLink(0) != null ? arm.getLink(0).getName() : null);
      return null;
    }
    String robot = parsed[0];
    String side = parsed[1];
    Point omoplate = toPoint3(jme.getWorldTranslation(robot + "." + side + "Arm.omoplate"));
    if (omoplate == null) {
      omoplate = toPoint3(jme.getWorldTranslation(robot + "." + side + "Arm.shoulder"));
    }
    Point wrist = toPoint3(jme.getHandWorldTranslation(robot, side));
    if (omoplate == null || wrist == null) {
      warn("Simulator samples missing omoplate={} wrist={} — world overlay will not match VinMoov", omoplate, wrist);
      return null;
    }
    Map<String, org.myrobotlab.math.geometry.Point3df> chain = jme.getArmChainWorldTranslations(robot, side);
    Point shoulder = toPoint3(chain.get(robot + "." + side + "Arm.shoulder"));
    Point rotate = toPoint3(chain.get(robot + "." + side + "Arm.rotate"));
    Point bicep = toPoint3(chain.get(robot + "." + side + "Arm.bicep"));
    fitVinMoovLinkLengths(name, omoplate, shoulder, rotate, bicep, wrist);
    return calibrateFromWorldSamples(omoplate, wrist);
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

  private JMonkeyEngine findSimulator() {
    for (org.myrobotlab.framework.interfaces.ServiceInterface si : Runtime.getServices()) {
      if (si instanceof JMonkeyEngine) {
        JMonkeyEngine jme = (JMonkeyEngine) si;
        if (jme.isRunning()) {
          return jme;
        }
      }
    }
    return null;
  }

  /**
   * Fit a last-frame tool offset so the IK palm matches a world-frame wrist at
   * the current pose. Requires {@link #calibrateToWorld} first. The offset
   * rotates with the arm (unlike origin-fitting).
   */
  public Point fitToolOffsetFromWorld(String name, Point endEffectorWorld) {
    DHRobotArm arm = arms.get(name);
    if (arm == null || endEffectorWorld == null) {
      error("Cannot fit tool offset — arm or end effector missing");
      return null;
    }
    Point targetIk = toIkFrame(endEffectorWorld);
    Point offset = arm.fitToolOffset(targetIk);
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

  static double distMm(Point a, Point b) {
    if (a == null || b == null) {
      return 0;
    }
    return a.distanceTo(b) * IK_MM_PER_JME_METER;
  }

  static boolean setLinkLength(DHLink link, boolean isA, double mm, String label) {
    if (link == null || Math.abs(mm) < MIN_DH_LINK_MM || Math.abs(mm) > MAX_DH_LINK_MM) {
      if (link != null && Math.abs(mm) >= 1.0) {
        log.warn("Ignoring DH {} {} mm (allowed {}–{})", label, mm, MIN_DH_LINK_MM, MAX_DH_LINK_MM);
      }
      return false;
    }
    if (isA) {
      log.info("DH {} {} -> {}", label, link.getA(), mm);
      link.setA(mm);
    } else {
      log.info("DH {} {} -> {}", label, link.getD(), mm);
      link.setD(mm);
    }
    return true;
  }

  /**
   * Map a simulator / world point into the DH / IK frame (same transform
   * {@link #moveTo} applies before solving).
   */
  public Point toIkFrame(Point world) {
    Point p = world;
    if (scale != null) {
      p = new Point(scale.getX() * p.getX(), scale.getY() * p.getY(), scale.getZ() * p.getZ(), p.getRoll(), p.getPitch(), p.getYaw());
    }
    if (inputMatrix != null) {
      p = rotateAndTranslate(p);
    }
    return p;
  }

  /**
   * Map a DH / IK point into simulator / world coordinates (inverse of
   * {@link #toIkFrame}).
   */
  public Point toWorldFrame(Point ik) {
    Point p = ik;
    if (inputMatrix != null) {
      p = inverseRotateAndTranslate(p);
    }
    if (scale != null) {
      double sx = scale.getX() != 0.0 ? scale.getX() : 1.0;
      double sy = scale.getY() != 0.0 ? scale.getY() : 1.0;
      double sz = scale.getZ() != 0.0 ? scale.getZ() : 1.0;
      p = new Point(p.getX() / sx, p.getY() / sy, p.getZ() / sz, p.getRoll(), p.getPitch(), p.getYaw());
    }
    return p;
  }

  public Point currentPositionWorld(String name) {
    return toWorldFrame(currentPosition(name));
  }

  public Point getWorldOrigin() {
    return worldOrigin;
  }

  public Point getScale() {
    return scale;
  }

  public boolean isWorldFrameEnabled() {
    return config != null && config.worldFrame && (scale != null || inputMatrix != null);
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
    arms.get(name).centerAllJoints();
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
      // publishTelemetry: servo = deg(theta) + offset  (then % 360)
      double thetaDeg = servoPos - link.getOffset();
      link.setTheta(MathUtils.degToRad(thetaDeg));
      found++;
      log.info("DH {} from servo {}° → theta {}°", servoName, servoPos, thetaDeg);
    }
    if (found == 0) {
      error("No servos found for arm %s — cannot compute position", name);
      return null;
    }
    JMonkeyEngine jme = findSimulator();
    if (jme != null) {
      calibrateFromSimulator(jme);
    } else if (scale == null) {
      // Overlay and HUD are meters; never publish raw DH millimeters as world.
      calibrateToWorld(0.0, 0.0, 0.0);
    }
    publishTelemetry(name);
    Point world = currentPositionWorld(name);
    log.info("Forward kinematics from servos world {}", world);
    return world;
  }

  /**
   * Compute the inverse kinematics to move the robot hand to the destination
   * first scale the input point, then apply
   * 
   * @param name
   *             n
   * @param p
   *             p
   * 
   */
  public void moveTo(String name, Point p) {

    log.info("Raw Input : {} - {}", name, p);
    if (scale != null) {
      // scale the x,y,z by the factors stored in the scale point. (really
      // vector i guess?)
      double x = scale.getX() * p.getX();
      double y = scale.getY() * p.getY();
      double z = scale.getZ() * p.getZ();
      p = new Point(x, y, z, p.getRoll(), p.getPitch(), p.getYaw());
      log.info("Scaled Input {}", p);
    }
    if (inputMatrix != null) {
      p = rotateAndTranslate(p);
      log.info("Rot/Translated input {}", p);
    }
    boolean success = arms.get(name).moveToGoal(p);
    if (!success) {
      log.warn("IK did not reach goal {} — publishing FK of last thetas (miss {} mm)", p, arms.get(name).getPalmPosition().distanceTo(p));
    }
    publishTelemetry(name);
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
      double theta = l.getTheta();
      // angles between 0 - 360 degrees.. not sure what people will really want?
      // - 180 to + 180 ?
      double angle = MathUtils.radToDeg(theta) + l.getOffset();
      angleMap.put(jointName, angle % 360.0F);
      log.info("Servo : {}  Angle : {}", jointName, angleMap.get(jointName));
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

    double[][] jointPositionMap = new double[arms.get(name).getNumLinks() + 1][3];

    // first position is the origin... second is the end of the first link
    jointPositionMap[0][0] = 0;
    jointPositionMap[0][1] = 0;
    jointPositionMap[0][2] = 0;

    for (int i = 1; i <= arms.get(name).getNumLinks(); i++) {
      Point jp = arms.get(name).getJointPosition(i - 1);
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
    worldPosition = currentPositionWorld(name);
  }

  @Override
  public void attach(Attachable attachable) {
    if (attachable instanceof JMonkeyEngine) {
      // Overlay only — JME.onJointAngles would move servos a second time.
      addListener("publishWorldPosition", attachable.getName(), "onWorldPosition");
      JMonkeyEngine jme = (JMonkeyEngine) attachable;
      if (jme.isRunning() && currentArm != null) {
        calibrateFromSimulator(jme);
      }
      return;
    }
    if (attachable instanceof IKJointAngleListener) {
      // Matches IKJointAnglePublisher / IKJointAngleListener (plural)
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

    double totalGain = 100.0;
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