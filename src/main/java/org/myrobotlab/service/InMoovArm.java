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

  /**
   * peer services
   */
  transient public Servo bicep;
  transient public Servo rotate;
  transient public Servo shoulder;
  transient public Servo omoplate;
  transient public Arduino arduino;
  String side;

  public InMoovArm(String n) throws Exception {
    super(n);
    // createReserves(n); // Ok this might work but IT CANNOT BE IN SERVICE
    // FRAMEWORK !!!!!
    bicep = (Servo) createPeer("bicep");
    rotate = (Servo) createPeer("rotate");
    shoulder = (Servo) createPeer("shoulder");
    omoplate = (Servo) createPeer("omoplate");
    arduino = (Arduino) createPeer("arduino");

    bicep.setMinMax(5, 90);
    rotate.setMinMax(40, 180);
    shoulder.setMinMax(0, 180);
    omoplate.setMinMax(10, 80);

    bicep.setRest(5);
    rotate.setRest(90);
    shoulder.setRest(30);
    omoplate.setRest(10);
    
    setVelocity(20.0, 20.0, 20.0, 20.0);
    
    }

  /*
   * attach all the servos - this must be re-entrant and accomplish the
   * re-attachment when servos are detached
   */
  @Deprecated
  public boolean attach() {
    return enable();
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
    bicep.setAutoDisable(param);
    rotate.setAutoDisable(param);
    shoulder.setAutoDisable(param);
    omoplate.setAutoDisable(param);
    }
  
  public void setOverrideAutoDisable(Boolean param) {
    bicep.setOverrideAutoDisable(param);
    rotate.setOverrideAutoDisable(param);
    shoulder.setOverrideAutoDisable(param);
    omoplate.setOverrideAutoDisable(param);
    }

  @Override
  public void broadcastState() {
    // notify the gui
    bicep.broadcastState();
    rotate.broadcastState();
    shoulder.broadcastState();
    omoplate.broadcastState();
  }

  public boolean connect(String port) throws Exception {
    startService(); // NEEDED? I DONT THINK SO....

    if (arduino == null) {
      error("arduino is invalid");
      return false;
    }

    arduino.connect(port);

    if (!arduino.isConnected()) {
      error("arduino %s not connected", arduino.getName());
      return false;
    }
    bicep.attach(arduino, 8, bicep.getRest(), bicep.getVelocity());
    rotate.attach(arduino, 9, rotate.getRest(), rotate.getVelocity());
    shoulder.attach(arduino, 10, shoulder.getRest(), shoulder.getVelocity());
    omoplate.attach(arduino, 11, omoplate.getRest(), omoplate.getVelocity());
    
    enableAutoEnable(true);

    broadcastState();
    return true;
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

  public Arduino getArduino() {
    return arduino;
  }

  public Servo getBicep() {
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

  public Servo getOmoplate() {
    return omoplate;
  }

  public Servo getRotate() {
    return rotate;
  }

  public String getScript(String inMoovServiceName) {
    return String.format(Locale.ENGLISH,"%s.moveArm(\"%s\",%.2f,%.2f,%.2f,%.2f)\n", inMoovServiceName, side, bicep.getPos(), rotate.getPos(), shoulder.getPos(), omoplate.getPos());
  }

  public Servo getShoulder() {
    return shoulder;
  }

  public String getSide() {
    return side;
  }

  public boolean isAttached() {
    boolean attached = false;

    attached |= bicep.isAttached();
    attached |= rotate.isAttached();
    attached |= shoulder.isAttached();
    attached |= omoplate.isAttached();

    return attached;
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
  
  // FIXME - releasePeers()
  public void release() {
    disable();
    if (bicep != null) {
      bicep.releaseService();
      bicep = null;
    }
    if (rotate != null) {
      rotate.releaseService();
      rotate = null;
    }
    if (shoulder != null) {
      shoulder.releaseService();
      shoulder = null;
    }
    if (omoplate != null) {
      omoplate.releaseService();
      omoplate = null;
    }
  }

  public void rest() {

    //setSpeed(1.0, 1.0, 1.0, 1.0);

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

  public void setArduino(Arduino arduino) {
    this.arduino = arduino;
  }

  public void setBicep(Servo bicep) {
    this.bicep = bicep;
  }

  public void setLimits(int bicepMin, int bicepMax, int rotateMin, int rotateMax, int shoulderMin, int shoulderMax, int omoplateMin, int omoplateMax) {
    bicep.setMinMax(bicepMin, bicepMax);
    rotate.setMinMax(rotateMin, rotateMax);
    shoulder.setMinMax(shoulderMin, shoulderMax);
    omoplate.setMinMax(omoplateMin, omoplateMax);
  }

  public void setOmoplate(Servo omoplate) {
    this.omoplate = omoplate;
  }

  // ------------- added set pins
  /* OLD WAY
  public void setpins(Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {

    log.info(String.format("setPins %d %d %d %d %d %d", bicep, rotate, shoulder, omoplate));
    // createPeers();
    this.bicep.setPin(bicep);
    this.rotate.setPin(rotate);
    this.shoulder.setPin(shoulder);
    this.omoplate.setPin(omoplate);
  }
 	*/
 
  public void setRotate(Servo rotate) {
    this.rotate = rotate;
  }

  public void setShoulder(Servo shoulder) {
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
    bicep.startService();
    rotate.startService();
    shoulder.startService();
    omoplate.startService();
    arduino.startService();
  }

  public void test() {
    if (arduino == null) {
      error("arduino is null");
    }
    if (!arduino.isConnected()) {
      error("arduino not connected");
    }
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
          omoplate.moveTo(angle.intValue());
        }
        if ("shoulder".equals(s)) {
          Double angle = (gainMap.get(s) * angleMap.get(s) + phaseShiftMap.get(s)) % 360.0;
          if (angle < 0) {
            angle += 360;
          }
          shoulder.moveTo(angle.intValue());
        }
        if ("rotate".equals(s)) {
          Double angle = (gainMap.get(s) * angleMap.get(s) + phaseShiftMap.get(s)) % 360.0;
          if (angle < 0) {
            angle += 360;
          }
          rotate.moveTo(angle.intValue());
        }
        if ("bicep".equals(s)) {
          Double angle = (gainMap.get(s) * angleMap.get(s) + phaseShiftMap.get(s)) % 360.0;
          bicep.moveTo(angle.intValue());
          if (angle < 0) {
            angle += 360;
          }
        }
      }
    }
  }

  public static DHRobotArm getDHRobotArm() {

    // TODO: specify this correctly and document the reference frames!
    DHRobotArm arm = new DHRobotArm();
    // d , r, theta , alpha

    // TODO: the DH links should take into account the encoder offsets and
    // calibration maps
    DHLink link1 = new DHLink("omoplate", 0, 40, 0, MathUtils.degToRad(-90));
    link1.setMin(MathUtils.degToRad(-80));
    link1.setMax(MathUtils.degToRad(0));

    DHLink link2 = new DHLink("shoulder", 80, 0, 0, MathUtils.degToRad(90));
    // TODO: this is actually 90 to -90 ? validate if inverted.
    link2.setMin(MathUtils.degToRad(-90));
    link2.setMax(MathUtils.degToRad(90));

    DHLink link3 = new DHLink("rotate", 280, 0, 0, MathUtils.degToRad(90));
    link3.setMin(MathUtils.degToRad(90));
    link3.setMax(MathUtils.degToRad(270));

    DHLink link4 = new DHLink("bicep", 0, 280, 0, MathUtils.degToRad(0));
    // TODO: this is probably inverted? should be 90 to 0...
    link4.setMin(MathUtils.degToRad(0));
    link4.setMax(MathUtils.degToRad(90));

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
    this.bicep.setVelocity(bicep);
    this.rotate.setVelocity(rotate);
    this.shoulder.setVelocity(shoulder);
    this.omoplate.setVelocity(omoplate);
  }

}
