package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.kinematics.JointFrame;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.service.config.InMoov2ArmConfig;
import org.myrobotlab.service.interfaces.IKJointAngleListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * InMoovArm - This is the Arm sub-service for the InMoov Robot. It consists of
 * 4 Servos: bicep, rotate,shoulder,omoplate It uses Arduino to control the
 * servos.
 * 
 * TODO - make this service responsible for setting up pub subs - and on any new
 * registration look for attach capablities
 * 
 * Null checking is not necessary for this "group" of servos - its assumed the
 * user would want the entire group initialized on creation and that is what
 * startPeers() does
 *
 */
public class InMoov2Arm extends Service<InMoov2ArmConfig> implements IKJointAngleListener {

  public final static Logger log = LoggerFactory.getLogger(InMoov2Arm.class);

  private static final long serialVersionUID = 1L;

  /**
   * Default DH lengths (meters) for a printed InMoov / VinMoov stick figure.
   * The IK demo overwrites these from measured VinMoov node distances when the
   * simulator is running.
   */
  public static final double DH_OMOPLATE_A = 0.040;
  public static final double DH_SHOULDER_D = 0.080;
  public static final double DH_ROTATE_D = 0.280;
  public static final double DH_BICEP_A = 0.280;

  /** Servo rest / limits matching {@link org.myrobotlab.service.config.InMoov2ArmConfig} (degrees). */
  public static final double OMOPLATE_SERVO_MIN = 10.0;
  public static final double OMOPLATE_SERVO_MAX = 80.0;
  public static final double OMOPLATE_SERVO_REST = 10.0;

  public static final double SHOULDER_SERVO_MIN = 0.0;
  public static final double SHOULDER_SERVO_MAX = 180.0;
  public static final double SHOULDER_SERVO_REST = 30.0;

  public static final double ROTATE_SERVO_MIN = 40.0;
  public static final double ROTATE_SERVO_MAX = 180.0;
  public static final double ROTATE_SERVO_REST = 90.0;

  public static final double BICEP_SERVO_MIN = 0.0;
  public static final double BICEP_SERVO_MAX = 90.0;
  public static final double BICEP_SERVO_REST = 0.0;

  /** Mesh rotation direction of each arm node, matching {@code InMoov2Config}. */
  public static final double OMOPLATE_MESH_SLOPE_LEFT = 1;

  public static final double OMOPLATE_MESH_SLOPE_RIGHT = -1;

  public static final double SHOULDER_MESH_SLOPE = -1;

  public static final double ROTATE_MESH_SLOPE_LEFT = 1;

  public static final double ROTATE_MESH_SLOPE_RIGHT = -1;

  public static final double BICEP_MESH_SLOPE = -1;

  /**
   * VinMoov node mapper so 1° of InMoov servo is 1° of mesh rotation and rest
   * maps to 0° (bind pose). {@code slope} is +1 or −1 for the mesh axis
   * direction used in {@code InMoov2Config} simulator nodes.
   */
  public static MapperLinear vinMoovArmMapper(double restServo, double slope) {
    return new MapperLinear(0.0, 180.0, -restServo * slope, slope * (180.0 - restServo), true, false);
  }

  /**
   * The four arm joints as measured on a nominal printed InMoov, in an arm-root
   * frame whose origin is the omoplate: {@code +X} out to the robot's left,
   * {@code +Y} up, {@code +Z} forward (the JMonkeyEngine convention).
   *
   * <p>
   * Each joint is an origin plus a real rotation axis, not a set of
   * Denavit-Hartenberg parameters. The rotation <em>direction</em> is carried by
   * the simulator's node mapper slope, so the axes are identical for both arms
   * and only the origins mirror in X — trying to mirror the axes as well would
   * double-count the reversal.
   * </p>
   *
   * <p>
   * These are a fallback. Call
   * {@link org.myrobotlab.service.InverseKinematics3D#calibrateFromSimulator} to
   * replace them with values measured from VinMoov.
   * </p>
   */
  public static List<JointFrame> getDefaultJointFrames(String name, String side) {
    boolean right = "right".equalsIgnoreCase(side);
    double mirror = right ? -1.0 : 1.0;
    double omoplateSlope = right ? OMOPLATE_MESH_SLOPE_RIGHT : OMOPLATE_MESH_SLOPE_LEFT;
    double rotateSlope = right ? ROTATE_MESH_SLOPE_RIGHT : ROTATE_MESH_SLOPE_LEFT;

    double shoulderX = mirror * DH_OMOPLATE_A;
    double rotateX = mirror * (DH_OMOPLATE_A + DH_SHOULDER_D);
    double elbowY = -DH_ROTATE_D;

    List<JointFrame> frames = new ArrayList<>();
    frames.add(jointFrame(String.format("%s.%sArm.omoplate", name, side), 0, 0, 0, 0, 0, 1, "z", OMOPLATE_SERVO_REST, omoplateSlope, OMOPLATE_SERVO_MIN, OMOPLATE_SERVO_MAX));
    frames.add(jointFrame(String.format("%s.%sArm.shoulder", name, side), shoulderX, 0, 0, 1, 0, 0, "x", SHOULDER_SERVO_REST, SHOULDER_MESH_SLOPE, SHOULDER_SERVO_MIN,
        SHOULDER_SERVO_MAX));
    frames.add(jointFrame(String.format("%s.%sArm.rotate", name, side), rotateX, 0, 0, 0, -1, 0, "y", ROTATE_SERVO_REST, rotateSlope, ROTATE_SERVO_MIN, ROTATE_SERVO_MAX));
    frames.add(jointFrame(String.format("%s.%sArm.bicep", name, side), rotateX, elbowY, 0, 1, 0, 0, "x", BICEP_SERVO_REST, BICEP_MESH_SLOPE, BICEP_SERVO_MIN, BICEP_SERVO_MAX));
    return frames;
  }

  /** Nominal wrist position for {@link #getDefaultJointFrames}, arm-root frame. */
  public static Point getDefaultEndEffector(String side) {
    double mirror = "right".equalsIgnoreCase(side) ? -1.0 : 1.0;
    return new Point(mirror * (DH_OMOPLATE_A + DH_SHOULDER_D), -(DH_ROTATE_D + DH_BICEP_A), 0);
  }

  /**
   * A joint frame whose servo map is taken from the same
   * {@link #vinMoovArmMapper} the simulator uses, sampled at the servo limits.
   * Because the map is affine, two points fix it exactly, and reusing the
   * simulator's mapper means the solver cannot disagree with the mesh about
   * direction or scale.
   */
  private static JointFrame jointFrame(String name, double ox, double oy, double oz, double ax, double ay, double az, String mask, double servoRest, double meshSlope,
      double servoMin, double servoMax) {
    MapperLinear mapper = vinMoovArmMapper(servoRest, meshSlope);
    JointFrame frame = new JointFrame(name, new Point(ox, oy, oz), new Point(ax, ay, az));
    frame.rotationMask = mask;
    frame.angleDeg = mapper.calcOutput(servoRest);
    frame.withServoMap(servoMin, servoMax, mapper.calcOutput(servoMin), mapper.calcOutput(servoMax));
    return frame;
  }

  /**
   * InMoov left/right arm kinematic chain.
   *
   * <p>
   * Built from {@link #getDefaultJointFrames} so the link geometry is real joint
   * axes and origins, and the servo mapping ({@code servo = servoSlope * thetaDeg
   * + offset}) comes from the simulator's node mapper. That makes {@code theta}
   * identical to the mesh rotation angle: zero at servo rest, one degree per
   * servo degree, same sign.
   * </p>
   */
  public static DHRobotArm getDHRobotArm(String name, String side) {
    return DHRobotArm.fromJointFrames(String.format("%s.%sArm", name, side), getDefaultJointFrames(name, side), getDefaultEndEffector(side));
  }

  @Deprecated /* use onMove(map) */
  public void onMoveArm(HashMap<String, Double> map) {
    onMove(map);
  }

  public void onMove(Map<String, Double> map) {
    moveTo(map.get("bicep"), map.get("rotate"), map.get("shoulder"), map.get("omoplate"));
  }


  /**
   * peer services FIXME - framework should always - startPeers() unless
   * configured not to
   */
  transient public ServoControl bicep;

  transient public ServoControl omoplate;

  transient public ServoControl rotate;

  transient public ServoControl shoulder;

  public InMoov2Arm(String n, String id) throws Exception {
    super(n, id);
  }

  @Override
  public void startService() {
    super.startService();
    bicep = (ServoControl) startPeer("bicep");
    rotate = (ServoControl) startPeer("rotate");
    shoulder = (ServoControl) startPeer("shoulder");
    omoplate = (ServoControl) startPeer("omoplate");
  }
  
  @Override
  public void stopService() {
    super.stopService();
    releasePeer("bicep");
    releasePeer("rotate");
    releasePeer("shoulder");
    releasePeer("omoplate");
  }

  @Override
  public Service broadcastState() {
    super.broadcastState();
    if (bicep != null)
      bicep.broadcastState();
    if (rotate != null)
      rotate.broadcastState();
    if (shoulder != null)
      shoulder.broadcastState();
    if (omoplate != null)
      omoplate.broadcastState();
    return this;
  }

  public void disable() {
    if (bicep != null)
      bicep.disable();
    if (rotate != null)
      rotate.disable();
    if (shoulder != null)
      shoulder.disable();
    if (omoplate != null)
      omoplate.disable();
  }

  public void enable() {
    if (bicep != null)
      bicep.enable();
    if (rotate != null)
      rotate.enable();
    if (shoulder != null)
      shoulder.enable();
    if (omoplate != null)
      omoplate.enable();
  }

  public void fullSpeed() {
    if (bicep != null)
      bicep.fullSpeed();
    if (rotate != null)
      rotate.fullSpeed();
    if (shoulder != null)
      shoulder.fullSpeed();
    if (omoplate != null)
      omoplate.fullSpeed();
  }

  public ServoControl getBicep() {
    return bicep;
  }

  public long getLastActivityTime() {
    long lastActivityTime = Math.max(bicep.getLastActivityTime(), rotate.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, shoulder.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, omoplate.getLastActivityTime());
    return lastActivityTime;
  }

  public ServoControl getOmoplate() {
    return omoplate;
  }

  public ServoControl getRotate() {
    return rotate;
  }

  public String getScript(String service) {
    String side = getName().contains("left") ? "left" : "right";
    return String.format("%s.moveArm(\"%s\",%.0f,%.0f,%.0f,%.0f)\n", service, side, bicep.getCurrentInputPos(), rotate.getCurrentInputPos(),
        shoulder.getCurrentInputPos(), omoplate.getCurrentInputPos());
  }

  public ServoControl getShoulder() {
    return shoulder;
  }

  public void moveTo(Double bicepPos, Double rotatePos, Double shoulderPos, Double omoplatePos) {
    log.debug("{} moveTo {} {} {} {}", getName(), bicepPos, rotatePos, shoulderPos, omoplatePos);
    if (bicep != null)
      bicep.moveTo(bicepPos);
    if (rotate != null)
      rotate.moveTo(rotatePos);
    if (shoulder != null)
      shoulder.moveTo(shoulderPos);
    if (omoplate != null)
      omoplate.moveTo(omoplatePos);
  }

  public void moveToBlocking(double bicep, double rotate, double shoulder, double omoplate) {
    log.info("init {} moveToBlocking", getName());
    moveTo(bicep, rotate, shoulder, omoplate);
    waitTargetPos();
    log.info("end {} moveToBlocking", getName());
  }

  /**
   * Legacy short-name IK mapping (gain + phaseShift). InverseKinematics3D now
   * publishes ready-to-use servo degrees under full names
   * ({@code i01.leftArm.omoplate}) and must attach to
   * {@link InMoov2#onJointAngles}, which just forwards them. Do not attach IK to
   * this arm service or both maps will fight.
   */
  @Deprecated
  @Override
  public void onJointAngles(Map<String, Double> angleMap) {
    // We should walk though our list of servos and see if
    // the map has it.. if so .. move to it!
    // Peers p = InMoovArm.getPeers(getName()).getPeers("Servo");
    // TODO: look up the mapping for all the servos in the arm.

    // we map the servo 90 degrees to be 0 degrees.
    HashMap<String, Double> phaseShiftMap = new HashMap<String, Double>();
    // phaseShiftMap.put("omoplate", 90);
    // Harry's omoplate is +90 degrees from Gaels InMoov..
    // These are for the encoder offsets.
    // these map between the reference frames of the dh model & the actual arm.
    // (calibration)
    phaseShiftMap.put("omoplate", 90.0);
    phaseShiftMap.put("shoulder", 90.0);
    phaseShiftMap.put("rotate", -450.0);
    phaseShiftMap.put("bicep", 90.0);

    HashMap<String, Double> gainMap = new HashMap<String, Double>();
    gainMap.put("omoplate", 1.0);
    gainMap.put("shoulder", -1.0);
    gainMap.put("rotate", -1.0);
    gainMap.put("bicep", -1.0);

    ArrayList<String> servos = new ArrayList<String>();
    servos.add("omoplate");
    servos.add("shoulder");
    servos.add("rotate");
    servos.add("bicep");
    for (String s : servos) {
      if (angleMap.containsKey(s)) {
        if ("omoplate".equals(s)) {
          Double angle = (gainMap.get(s) * angleMap.get(s) + phaseShiftMap.get(s)) % 360.0;
          if (angle < 0) {
            angle += 360;
          }
          omoplate.moveTo(angle);
        }
        if ("shoulder".equals(s)) {
          Double angle = (gainMap.get(s) * angleMap.get(s) + phaseShiftMap.get(s)) % 360.0;
          if (angle < 0) {
            angle += 360;
          }
          shoulder.moveTo(angle);
        }
        if ("rotate".equals(s)) {
          Double angle = (gainMap.get(s) * angleMap.get(s) + phaseShiftMap.get(s)) % 360.0;
          if (angle < 0) {
            angle += 360;
          }
          rotate.moveTo(angle);
        }
        if ("bicep".equals(s)) {
          Double angle = (gainMap.get(s) * angleMap.get(s) + phaseShiftMap.get(s)) % 360.0;
          bicep.moveTo(angle);
          if (angle < 0) {
            angle += 360;
          }
        }
      }
    }
  }

  // FIXME - framework should auto-release - unless configured not to
  @Override
  public void releaseService() {
    try {
      disable();
      super.releaseService();
    } catch (Exception e) {
      error(e);
    }
  }

  public void rest() {
    if (bicep != null)
      bicep.rest();
    if (rotate != null)
      rotate.rest();
    if (shoulder != null)
      shoulder.rest();
    if (omoplate != null)
      omoplate.rest();
  }

  @Override
  public boolean save() {
    super.save();
    if (bicep != null)
      bicep.save();
    if (rotate != null)
      rotate.save();
    if (shoulder != null)
      shoulder.save();
    if (omoplate != null)
      omoplate.save();
    return true;
  }

  @Deprecated
  public boolean loadFile(String file) {
    File f = new File(file);
    Python p = (Python) Runtime.getService("python");
    log.info("Loading  Python file {}", f.getAbsolutePath());
    if (p == null) {
      log.error("Python instance not found");
      return false;
    }
    String script = null;
    try {
      script = FileIO.toString(f.getAbsolutePath());
    } catch (IOException e) {
      log.error("IO Error loading file : ", e);
      return false;
    }
    // evaluate the scripts in a blocking way.
    boolean result = p.exec(script, true);
    if (!result) {
      log.error("Error while loading file {}", f.getAbsolutePath());
      return false;
    } else {
      log.debug("Successfully loaded {}", f.getAbsolutePath());
    }
    return true;
  }

  public void setAutoDisable(Boolean idleTimeoutMs) {
    if (bicep != null)
      bicep.setAutoDisable(idleTimeoutMs);
    if (rotate != null)
      rotate.setAutoDisable(idleTimeoutMs);
    if (shoulder != null)
      shoulder.setAutoDisable(idleTimeoutMs);
    if (omoplate != null)
      omoplate.setAutoDisable(idleTimeoutMs);
  }

  public void setBicep(ServoControl bicep) {
    this.bicep = bicep;
  }

  /**
   * This method sets the output min/max limits for all of the servos in the
   * arm. Input limits are unchanged.
   * 
   * @param bicepMin
   *          m
   * @param bicepMax
   *          m
   * @param rotateMin
   *          m
   * @param rotateMax
   *          m
   * @param shoulderMin
   *          m
   * @param shoulderMax
   *          m
   * @param omoplateMin
   *          m
   * @param omoplateMax
   *          m
   * 
   */
  public void setLimits(double bicepMin, double bicepMax, double rotateMin, double rotateMax, double shoulderMin, double shoulderMax, double omoplateMin, double omoplateMax) {
    if (bicep != null)
      bicep.setMinMaxOutput(bicepMin, bicepMax);
    if (rotate != null)
      rotate.setMinMaxOutput(rotateMin, rotateMax);
    if (shoulder != null)
      shoulder.setMinMaxOutput(shoulderMin, shoulderMax);
    if (omoplate != null)
      omoplate.setMinMaxOutput(omoplateMin, omoplateMax);
  }

  public void setOmoplate(ServoControl omoplate) {
    this.omoplate = omoplate;
  }

  public void setRotate(ServoControl rotate) {
    this.rotate = rotate;
  }

  public void setShoulder(ServoControl shoulder) {
    this.shoulder = shoulder;
  }

  public void setSpeed(Double bicepSpeed, Double rotateSpeed, Double shoulderSpeed, Double omoplateSpeed) {
    if (bicep != null)
      bicep.setSpeed(bicepSpeed);
    if (rotate != null)
      rotate.setSpeed(rotateSpeed);
    if (shoulder != null)
      shoulder.setSpeed(shoulderSpeed);
    if (omoplate != null)
      omoplate.setSpeed(omoplateSpeed);
  }

  @Deprecated
  public void setVelocity(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    setSpeed(bicep, rotate, shoulder, omoplate);
  }

  public void stop() {
    if (bicep != null)
      bicep.stop();
    if (rotate != null)
      rotate.stop();
    if (shoulder != null)
      shoulder.stop();
    if (omoplate != null)
      omoplate.stop();
  }

  public void test() {
    /*
     * FIXME - non ServoController methods must go - or I2C needs a
     * connect(baseAddress, address) with overloaded connect("0x48- if
     * (!arduino.isConnected()) { error("arduino not connected"); }
     */
    if (bicep != null)
      bicep.moveTo(bicep.getCurrentInputPos() + 2);
    if (rotate != null)
      rotate.moveTo(rotate.getCurrentInputPos() + 2);
    if (shoulder != null)
      shoulder.moveTo(shoulder.getCurrentInputPos() + 2);
    if (omoplate != null)
      omoplate.moveTo(omoplate.getCurrentInputPos() + 2);
    sleep(300);
  }

  public void waitTargetPos() {
    if (bicep != null)
      bicep.waitTargetPos();
    if (rotate != null)
      rotate.waitTargetPos();
    if (shoulder != null)
      shoulder.waitTargetPos();
    if (omoplate != null)
      omoplate.waitTargetPos();
  }

}
