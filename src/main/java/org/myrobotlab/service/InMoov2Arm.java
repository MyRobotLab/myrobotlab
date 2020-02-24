package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MathUtils;
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
public class InMoov2Arm extends Service implements IKJointAngleListener {

  public final static Logger log = LoggerFactory.getLogger(InMoov2Arm.class);

  private static final long serialVersionUID = 1L;

  public static DHRobotArm getDHRobotArm(String name, String side) {

    // TODO: specify this correctly and document the reference frames!
    DHRobotArm arm = new DHRobotArm();
    // d , r, theta , alpha

    // HashMap<String, Double> calibMap = new HashMap<String, Double>();
    // calibMap.put("i01.leftArm.omoplate", 90.0);
    // calibMap.put("i01.leftArm.shoulder", -90.0+45);
    // calibMap.put("i01.leftArm.rotate", 0.0);
    // calibMap.put("i01.leftArm.bicep", -90.0);

    // TODO: the DH links should take into account the encoder offsets and
    // calibration maps
    DHLink link1 = new DHLink(String.format("%s.%sArm.omoplate", name, side), 0, 40, MathUtils.degToRad(-90), MathUtils.degToRad(-90));
    // dh model + 90 degrees = real
    link1.setMin(MathUtils.degToRad(-90));
    link1.setMax(MathUtils.degToRad(0));
    link1.setOffset(90);

    // -80 vs +80 difference between left/right arm.
    double shoulderWidth = 80;
    if (side.equalsIgnoreCase("right")) {
      // TODO: there are probably other differnces between the 2 arms.
      shoulderWidth = -80;
    }
    DHLink link2 = new DHLink(String.format("%s.%sArm.shoulder", name, side), shoulderWidth, 0, MathUtils.degToRad(90), MathUtils.degToRad(90));
    // TODO: this is actually 90 to -90 ? validate if inverted.
    // this link is inverted :-/
    link2.setMin(MathUtils.degToRad(-90));
    link2.setMax(MathUtils.degToRad(90));
    link2.setOffset(-45);

    DHLink link3 = new DHLink(String.format("%s.%sArm.rotate", name, side), 280, 0, MathUtils.degToRad(0), MathUtils.degToRad(90));
    // TODO: check if this is inverted. i think it is.
    link3.setMin(MathUtils.degToRad(-90));
    link3.setMax(MathUtils.degToRad(90));
    link3.setOffset(0);

    DHLink link4 = new DHLink(String.format("%s.%sArm.bicep", name, side), 0, 280, MathUtils.degToRad(90), MathUtils.degToRad(0));
    // TODO: this is probably inverted? should be 90 to 0...
    link4.setMin(MathUtils.degToRad(90));
    link4.setMax(MathUtils.degToRad(180));
    link4.setOffset(-90);

    arm.addLink(link1);
    arm.addLink(link2);
    arm.addLink(link3);
    arm.addLink(link4);

    return arm;
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(InMoov2Arm.class.getCanonicalName());
    meta.addDescription("the InMoov Arm Service");
    meta.addCategory("robot");

    meta.addPeer("bicep", "Servo", "Bicep servo");
    meta.addPeer("rotate", "Servo", "Rotate servo");
    meta.addPeer("shoulder", "Servo", "Shoulder servo");
    meta.addPeer("omoplate", "Servo", "Omoplate servo");
    meta.addPeer("arduino", "Arduino", "Arduino controller for this arm");

    return meta;
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

    // FIXME - future will just be pub/sub attach/detach subscriptions
    // and there will be no need this service.
    // Config will be managed by LangUtils

    startPeers();

    bicep.setPin(8);
    rotate.setPin(9);
    shoulder.setPin(10);
    omoplate.setPin(11);

    bicep.setMinMax(5.0, 90.0);
    rotate.setMinMax(40.0, 180.0);
    shoulder.setMinMax(0.0, 180.0);
    omoplate.setMinMax(10.0, 80.0);

    bicep.setRest(5.0);
    rotate.setRest(90.0);
    shoulder.setRest(30.0);
    omoplate.setRest(10.0);

    bicep.setPosition(5.0);
    shoulder.setPosition(30.0);
    rotate.setPosition(90.0);
    omoplate.setPosition(10.0);

    setSpeed(20.0, 20.0, 20.0, 20.0);
  }

  @Override
  public void broadcastState() {
    super.broadcastState();
    bicep.broadcastState();
    rotate.broadcastState();
    shoulder.broadcastState();
    omoplate.broadcastState();
  }

  public void disable() {
    bicep.disable();
    rotate.disable();
    shoulder.disable();
    omoplate.disable();
  }

  public void enable() {
    sleep(InMoov.attachPauseMs);
    bicep.enable();
    sleep(InMoov.attachPauseMs);
    rotate.enable();
    sleep(InMoov.attachPauseMs);
    shoulder.enable();
    sleep(InMoov.attachPauseMs);
    omoplate.enable();
  }

  public void fullSpeed() {
    bicep.fullSpeed();
    rotate.fullSpeed();
    shoulder.fullSpeed();
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

  @Deprecated /* use UtilLang classes */
  public String getScript(String inMoovServiceName) {
    // FIXME - this is cheesy
    String side = inMoovServiceName.contains("left") ? "left" : "right";
    return String.format(Locale.ENGLISH, "%s.moveArm(\"%s\",%.2f,%.2f,%.2f,%.2f)\n", inMoovServiceName, side, bicep.getPos(), rotate.getPos(), shoulder.getPos(),
        omoplate.getPos());
  }

  public ServoControl getShoulder() {
    return shoulder;
  }

  public void moveTo(double bicepPos, double rotatePos, double shoulderPos, double omoplatePos) {
    if (log.isDebugEnabled()) {
      log.debug("{} moveTo {} {} {} {}", getName(), bicepPos, rotatePos, shoulderPos, omoplatePos);
    }
    bicep.moveTo(bicepPos);
    rotate.moveTo(rotatePos);
    shoulder.moveTo(shoulderPos);
    omoplate.moveTo(omoplatePos);
  }

  public void moveToBlocking(double bicep, double rotate, double shoulder, double omoplate) {
    log.info("init {} moveToBlocking", getName());
    moveTo(bicep, rotate, shoulder, omoplate);
    waitTargetPos();
    log.info("end {} moveToBlocking", getName());
  }

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
  public void releaseService() {
    try {
      disable();
      releasePeers();
      super.releaseService(); 
    } catch (Exception e) {
      error(e);
    }
  }

  public void rest() {
    bicep.rest();
    rotate.rest();
    shoulder.rest();
    omoplate.rest();
  }

  @Override
  public boolean save() {
    super.save();
    bicep.save();
    rotate.save();
    shoulder.save();
    omoplate.save();
    return true;
  }

  public void setAutoDisable(Boolean idleTimeoutMs) {
    bicep.setAutoDisable(idleTimeoutMs);
    rotate.setAutoDisable(idleTimeoutMs);
    shoulder.setAutoDisable(idleTimeoutMs);
    omoplate.setAutoDisable(idleTimeoutMs);
  }

  public void setBicep(ServoControl bicep) {
    this.bicep = bicep;
  }

  public void setLimits(double bicepMin, double bicepMax, double rotateMin, double rotateMax, double shoulderMin, double shoulderMax, double omoplateMin, double omoplateMax) {
    bicep.setMinMax(bicepMin, bicepMax);
    rotate.setMinMax(rotateMin, rotateMax);
    shoulder.setMinMax(shoulderMin, shoulderMax);
    omoplate.setMinMax(omoplateMin, omoplateMax);
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

  public void setSpeed(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    this.bicep.setSpeed(bicep);
    this.rotate.setSpeed(rotate);
    this.shoulder.setSpeed(shoulder);
    this.omoplate.setSpeed(omoplate);
  }

  @Deprecated
  public void setVelocity(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    setSpeed(bicep, rotate, shoulder, omoplate);
  }

  public void stop() {
    bicep.stop();
    rotate.stop();
    shoulder.stop();
    omoplate.stop();
  }

  public void test() {
    /*
     * FIXME - non ServoController methods must go - or I2C needs a
     * connect(baseAddress, address) with overloaded connect("0x48- if
     * (!arduino.isConnected()) { error("arduino not connected"); }
     */
    bicep.moveTo(bicep.getPos() + 2);
    rotate.moveTo(rotate.getPos() + 2);
    shoulder.moveTo(shoulder.getPos() + 2);
    omoplate.moveTo(omoplate.getPos() + 2);
    sleep(300);
  }

  public void waitTargetPos() {
    bicep.waitTargetPos();
    rotate.waitTargetPos();
    shoulder.waitTargetPos();
    omoplate.waitTargetPos();
  }

}
