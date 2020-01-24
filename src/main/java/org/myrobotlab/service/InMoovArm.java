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
import org.myrobotlab.service.interfaces.PortConnector;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * InMoovArm - This is the Arm sub-service for the InMoov Robot. It consists of
 * 4 Servos: bicep, rotate,shoulder,omoplate It uses Arduino to control the
 * servos.
 *
 */
public class InMoovArm extends Service implements IKJointAngleListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoovArm.class);

  // default pins if not specified.
  private static final Integer DEFAULT_BICEP_PIN = 8;
  private static final Integer DEFAULT_ROTATE_PIN = 9;
  private static final Integer DEFAULT_SHOULDER_PIN = 10;
  private static final Integer DEFAULT_OMOPLATE_PIN = 11;

  /**
   * peer services
   */
  transient public ServoControl bicep;
  transient public ServoControl rotate;
  transient public ServoControl shoulder;
  transient public ServoControl omoplate;
  transient public ServoController controller;

  String side;

  public InMoovArm(String n, String id) throws Exception {
    super(n, id);
  }

  @Deprecated
  public boolean attach() {
    return enable();
  }
  
  public void releaseService() {
    try {
      disable();
      releasePeers();
      super.releaseService(); 
    } catch (Exception e) {
      error(e);
    }
  }


  public boolean enable() {
    sleep(InMoov.attachPauseMs);
    bicep.enable();
    sleep(InMoov.attachPauseMs);
    rotate.enable();
    sleep(InMoov.attachPauseMs);
    shoulder.enable();
    sleep(InMoov.attachPauseMs);
    omoplate.enable();
    return true;
  }

  @Deprecated
  public void enableAutoEnable(Boolean param) {
  }

  @Deprecated
  public void enableAutoDisable(Boolean param) {
    setAutoDisable(param);
  }

  public void setAutoDisable(Boolean param) {
    if (bicep != null) {
      bicep.setAutoDisable(param);
    }

    if (rotate != null) {
      rotate.setAutoDisable(param);
    }
    if (shoulder != null) {
      shoulder.setAutoDisable(param);
    }
    if (omoplate != null) {
      omoplate.setAutoDisable(param);
    }
  }

  @Override
  public void broadcastState() {
    super.broadcastState();
    if (bicep != null) {
      bicep.broadcastState();
    }

    if (rotate != null) {
      rotate.broadcastState();
    }

    if (shoulder != null) {
      shoulder.broadcastState();
    }

    if (omoplate != null) {
      omoplate.broadcastState();
    }
  }

  public boolean connect(String port) throws Exception {

    // if the servos haven't been started already.. fire them up!
    startPeers();
    controller = (ServoController) startPeer("arduino");
    // set defaults for the servos
    initServoDefaults();
    // we need a default controller

    if (controller == null) {
      error("controller is invalid");
      return false;
    }

    if (controller instanceof PortConnector) {
      PortConnector arduino = (PortConnector) controller;
      arduino.connect(port);

      if (!arduino.isConnected()) {
        error("arm %s could not connect on port %s", getName(), port);
      }
    }

    bicep.attach(controller);
    rotate.attach(controller);
    shoulder.attach(controller);
    omoplate.attach(controller);

    enableAutoEnable(true);

    broadcastState();
    return true;
  }

  private void initServoDefaults() {
    if (bicep.getPin() == null)
      bicep.setPin(DEFAULT_BICEP_PIN);
    if (rotate.getPin() == null)
      rotate.setPin(DEFAULT_ROTATE_PIN);
    if (shoulder.getPin() == null)
      shoulder.setPin(DEFAULT_SHOULDER_PIN);
    if (omoplate.getPin() == null)
      omoplate.setPin(DEFAULT_OMOPLATE_PIN);

    bicep.setMinMax(5.0, 90.0);
    rotate.setMinMax(40.0, 180.0);
    shoulder.setMinMax(0.0, 180.0);
    omoplate.setMinMax(10.0, 80.0);

    bicep.setRest(5.0);
    bicep.setPosition(5.0);
    rotate.setRest(90.0);
    rotate.setPosition(90.0);
    shoulder.setRest(30.0);
    shoulder.setPosition(30.0);
    omoplate.setRest(10.0);
    omoplate.setPosition(10.0);

    setVelocity(20.0, 20.0, 20.0, 20.0);
  }

  @Deprecated
  public void detach() {
    if (bicep != null) {
      bicep.detach();
      sleep(InMoov.attachPauseMs);
    }
    if (rotate != null) {
      rotate.detach();
      sleep(InMoov.attachPauseMs);
    }
    if (shoulder != null) {
      shoulder.detach();
      sleep(InMoov.attachPauseMs);
    }
    if (omoplate != null) {
      omoplate.detach();
    }
  }

  public void disable() {
    if (bicep != null) {
      bicep.disable();
      sleep(InMoov.attachPauseMs);
    }
    if (rotate != null) {
      rotate.disable();
      sleep(InMoov.attachPauseMs);
    }
    if (shoulder != null) {
      shoulder.disable();
      sleep(InMoov.attachPauseMs);
    }
    if (omoplate != null) {
      omoplate.disable();
    }
  }

  public ServoController getArduino() {
    return controller;
  }

  public ServoControl getBicep() {
    return bicep;
  }

  /*
   * public boolean load(){ super.load(); bicep.load(); rotate.load();
   * shoulder.load(); omoplate.load(); return true; }
   */

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

  public String getScript(String inMoovServiceName) {
    return String.format(Locale.ENGLISH, "%s.moveArm(\"%s\",%.2f,%.2f,%.2f,%.2f)\n", inMoovServiceName, side, bicep.getPos(), rotate.getPos(), shoulder.getPos(),
        omoplate.getPos());
  }

  public ServoControl getShoulder() {
    return shoulder;
  }

  public String getSide() {
    return side;
  }

  public void moveTo(double bicep, double rotate, double shoulder, double omoplate) {
    if (log.isDebugEnabled()) {
      log.debug("{} moveTo {} {} {} {}", getName(), bicep, rotate, shoulder, omoplate);
    }
    this.bicep.moveTo(bicep);
    this.rotate.moveTo(rotate);
    this.shoulder.moveTo(shoulder);
    this.omoplate.moveTo(omoplate);
  }

  public void moveToBlocking(double bicep, double rotate, double shoulder, double omoplate) {
    log.info("init {} moveToBlocking", getName());
    moveTo(bicep, rotate, shoulder, omoplate);
    waitTargetPos();
    log.info("end {} moveToBlocking", getName());
  }

  public void waitTargetPos() {
    bicep.waitTargetPos();
    rotate.waitTargetPos();
    shoulder.waitTargetPos();
    omoplate.waitTargetPos();
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

  public void setArduino(ServoController arduino) {
    this.controller = arduino;
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

  // ------------- added set pins
  /*
   * OLD WAY public void setpins(doubleeger bicep, Integer rotate, Integer
   * shoulder, Integer omoplate) {
   * 
   * log.info(String.format("setPins %d %d %d %d %d %d", bicep, rotate,
   * shoulder, omoplate)); // createPeers(); this.bicep.setPin(bicep);
   * this.rotate.setPin(rotate); this.shoulder.setPin(shoulder);
   * this.omoplate.setPin(omoplate); }
   */

  public void setRotate(ServoControl rotate) {
    this.rotate = rotate;
  }

  public void setShoulder(ServoControl shoulder) {
    this.shoulder = shoulder;
  }

  public void setSide(String side) {
    this.side = side;
  }

  @Deprecated
  public void setSpeed(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    log.warn("setspeed deprecated please use setvelocity");
    this.bicep.setSpeed(bicep);
    this.rotate.setSpeed(rotate);
    this.shoulder.setSpeed(shoulder);
    this.omoplate.setSpeed(omoplate);
  }

  @Override
  public void startService() {
    super.startService();
    if (bicep == null) {
      bicep = (ServoControl) startPeer("bicep");
    }
    if (rotate == null) {
      rotate = (ServoControl) startPeer("rotate");
    }
    if (shoulder == null) {
      shoulder = (ServoControl) startPeer("shoulder");
    }
    if (omoplate == null) {
      omoplate = (ServoControl) startPeer("omoplate");
    }
  }

  public void test() {
    if (controller == null) {
      error("arduino is null");
    }
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

    ServiceType meta = new ServiceType(InMoovArm.class.getCanonicalName());
    meta.addDescription("the InMoov Arm Service");
    meta.addCategory("robot");

    meta.addPeer("bicep", "Servo", "Bicep servo");
    meta.addPeer("rotate", "Servo", "Rotate servo");
    meta.addPeer("shoulder", "Servo", "Shoulder servo");
    meta.addPeer("omoplate", "Servo", "Omoplate servo");
    meta.addPeer("arduino", "Arduino", "Arduino controller for this arm");

    return meta;
  }

  public void setVelocity(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    this.bicep.setSpeed(bicep);
    this.rotate.setSpeed(rotate);
    this.shoulder.setSpeed(shoulder);
    this.omoplate.setSpeed(omoplate);
  }

  public void setController(ServoController controller) {
    this.controller = controller;
  }

}
