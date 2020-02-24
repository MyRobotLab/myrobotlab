package org.myrobotlab.service;

import java.util.Locale;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.PortConnector;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * InMoovTorso - The inmoov torso. This will allow control of the topStom,
 * midStom, and lowStom servos.
 *
 */
public class InMoov2Torso extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoov2Torso.class);

  transient public ServoControl topStom;
  transient public ServoControl midStom;
  transient public ServoControl lowStom;

  public InMoov2Torso(String n, String id) {
    super(n, id);
    // TODO: just call startPeers here.
    // // createReserves(n); // Ok this might work but IT CANNOT BE IN SERVICE
    // // FRAMEWORK !!!!!
    // topStom = (ServoControl) createPeer("topStom");
    // midStom = (ServoControl) createPeer("midStom");
    // lowStom = (ServoControl) createPeer("lowStom");
    // // controller = (ServoController) createPeer("arduino");

    // FIXME - createPeers ?
    startPeers();
    topStom.setPin(27);
    midStom.setPin(28);
    lowStom.setPin(29);

    topStom.setMinMax(60.0, 120.0);
    midStom.setMinMax(0.0, 180.0);
    lowStom.setMinMax(0.0, 180.0);
    topStom.setRest(90.0);
    topStom.setPosition(90.0);
    midStom.setRest(90.0);
    midStom.setPosition(90.0);
    lowStom.setRest(90.0);
    lowStom.setPosition(90.0);

    setVelocity(5.0, 5.0, 5.0);

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


  public void enable() {
    topStom.enable();
    midStom.enable();
    lowStom.enable();
  }

  public void setAutoDisable(Boolean param) {
    topStom.setAutoDisable(param);
    midStom.setAutoDisable(param);
    lowStom.setAutoDisable(param);
  }

  @Override
  public void broadcastState() {
    topStom.broadcastState();
    midStom.broadcastState();
    lowStom.broadcastState();
  }

  public void disable() {
    topStom.disable();
    midStom.disable();
    lowStom.disable();
  }

  public long getLastActivityTime() {
    long minLastActivity = Math.max(topStom.getLastActivityTime(), midStom.getLastActivityTime());
    minLastActivity = Math.max(minLastActivity, lowStom.getLastActivityTime());
    return minLastActivity;
  }

  @Deprecated /* use LangUtils */
  public String getScript(String inMoovServiceName) {
    return String.format(Locale.ENGLISH, "%s.moveTorso(%.2f,%.2f,%.2f)\n", inMoovServiceName, topStom.getPos(), midStom.getPos(), lowStom.getPos());
  }

  public void moveTo(double topStom, double midStom, double lowStom) {
    if (log.isDebugEnabled()) {
      log.debug("{} moveTo {} {} {}", getName(), topStom, midStom, lowStom);
    }
    this.topStom.moveTo(topStom);
    this.midStom.moveTo(midStom);
    this.lowStom.moveTo(lowStom);

  }

  public void moveToBlocking(Double topStom, Double midStom, Double lowStom) {
    log.info("init {} moveToBlocking ", getName());
    moveTo(topStom, midStom, lowStom);
    waitTargetPos();
    log.info("end {} moveToBlocking", getName());
  }

  public void waitTargetPos() {
    topStom.waitTargetPos();
    midStom.waitTargetPos();
    lowStom.waitTargetPos();
  }

  public void rest() {
    topStom.rest();
    midStom.rest();
    lowStom.rest();
  }

  @Override
  public boolean save() {
    super.save();
    topStom.save();
    midStom.save();
    lowStom.save();
    return true;
  }

  public void setLimits(double topStomMin, double topStomMax, double midStomMin, double midStomMax, double lowStomMin, double lowStomMax) {
    topStom.setMinMax(topStomMin, topStomMax);
    midStom.setMinMax(midStomMin, midStomMax);
    lowStom.setMinMax(lowStomMin, lowStomMax);
  }

  // ------------- added set pins
  public void setpins(Integer topStomPin, Integer midStomPin, Integer lowStomPin) {
    // createPeers();
    /*
     * this.topStom.setPin(topStom); this.midStom.setPin(midStom);
     * this.lowStom.setPin(lowStom);
     */

    /**
     * FIXME - has to be done outside of
     * 
     * arduino.servoAttachPin(topStom, topStomPin);
     * arduino.servoAttachPin(topStom, midStomPin);
     * arduino.servoAttachPin(topStom, lowStomPin);
     */
  }

  public void setSpeed(Double topStom, Double midStom, Double lowStom) {
    log.warn("setspeed deprecated please use setvelocity");
    this.topStom.setSpeed(topStom);
    this.midStom.setSpeed(midStom);
    this.lowStom.setSpeed(lowStom);
  }

  public void test() {

    topStom.moveTo(topStom.getPos() + 2);
    midStom.moveTo(midStom.getPos() + 2);
    lowStom.moveTo(lowStom.getPos() + 2);

    moveTo(35.0, 45.0, 55.0);
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

    ServiceType meta = new ServiceType(InMoov2Torso.class.getCanonicalName());
    meta.addDescription("InMoov Torso");
    meta.addCategory("robot");

    meta.addPeer("topStom", "Servo", "Top Stomach servo");
    meta.addPeer("midStom", "Servo", "Mid Stomach servo");
    meta.addPeer("lowStom", "Servo", "Low Stomach servo");
    meta.addPeer("arduino", "Arduino", "Arduino controller for torso");

    return meta;
  }

  @Deprecated /* use setSpeed */
  public void setVelocity(Double topStom, Double midStom, Double lowStom) {
    this.topStom.setSpeed(topStom);
    this.midStom.setSpeed(midStom);
    this.lowStom.setSpeed(lowStom);
  }

  static public void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      VirtualArduino v = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");

      v.connect("COM4");
      InMoov2Torso torso = (InMoov2Torso) Runtime.start("i01.torso", "InMoovTorso");
      Runtime.start("webgui", "WebGui");
      torso.test();
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public void fullSpeed() {
    topStom.fullSpeed();
    midStom.fullSpeed();
    lowStom.fullSpeed();
  }

  public void stop() {
    topStom.stop();
    midStom.stop();
    lowStom.stop();
  }

}
