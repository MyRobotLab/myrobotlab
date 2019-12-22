package org.myrobotlab.service;

import java.util.Locale;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
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
public class InMoovTorso extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoovTorso.class);

  
  private static final Integer DEFAULT_TOPSTOM_PIN = 27;
  private static final Integer DEFAULT_MIDSTOM_PIN = 28;
  private static final Integer DEFAULT_LOWSTOM_PIN = 29;
  
  transient public ServoControl topStom;
  transient public ServoControl midStom;
  transient public ServoControl lowStom;
  transient public ServoController controller;

  static public void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      VirtualArduino v = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");

      v.connect("COM4");
      InMoovTorso torso = (InMoovTorso) Runtime.start("i01.torso", "InMoovTorso");
      torso.connect("COM4");
      Runtime.start("webgui", "WebGui");
      torso.test();
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public InMoovTorso(String n, String id) {
    super(n, id);
    // TODO: just call startPeers here.
    //    // createReserves(n); // Ok this might work but IT CANNOT BE IN SERVICE
    //    // FRAMEWORK !!!!!
    //    topStom = (ServoControl) createPeer("topStom");
    //    midStom = (ServoControl) createPeer("midStom");
    //    lowStom = (ServoControl) createPeer("lowStom");
    //    // controller = (ServoController) createPeer("arduino");
  }
  
  public void startService() {
    if (topStom == null) {
      topStom = (ServoControl) createPeer("topStom");
    }
    if (midStom == null) {
      midStom = (ServoControl) createPeer("midStom");
    }
    if (lowStom == null) {
      lowStom = (ServoControl) createPeer("lowStom");
    }
    /*
    if (controller == null) {
      controller = (ServoController) createPeer("arduino");
    }
    */
  }

  private void initServoDefaults() {
    
    if (topStom.getPin() == null) 
      topStom.setPin(DEFAULT_TOPSTOM_PIN);
    if (midStom.getPin() == null)
        midStom.setPin(DEFAULT_MIDSTOM_PIN);
    if (lowStom.getPin() == null)
      lowStom.setPin(DEFAULT_LOWSTOM_PIN);
    
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

  /*
   * attach all the servos - this must be re-entrant and accomplish the
   * re-attachment when servos are detached
   */
  @Deprecated
  public boolean attach() {
    log.warn("attach deprecated please use enable");
    return enable();
  }

  public boolean enable() {

    sleep(InMoov.attachPauseMs);
    topStom.enable();
    sleep(InMoov.attachPauseMs);
    midStom.enable();
    sleep(InMoov.attachPauseMs);
    lowStom.enable();
    sleep(InMoov.attachPauseMs);
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
    topStom.setAutoDisable(param);
    midStom.setAutoDisable(param);
    lowStom.setAutoDisable(param);
  }

  @Override
  public void broadcastState() {
    // notify the gui
    topStom.broadcastState();
    midStom.broadcastState();
    lowStom.broadcastState();
  }
  
  public void setController(ServoController controller) {
    this.controller = controller;
  }

  public boolean connect(String port) throws Exception {
    controller = (ServoController)startPeer("arduino");
    
    if (controller == null) {
      error("arduino is invalid");
      return false;
    }

    if (controller instanceof PortConnector) {
      PortConnector arduino = (PortConnector) controller;
      arduino.connect(port);
      if (!arduino.isConnected()) {
        error("torso arduino on port %s not connected", port);
        return false;
      }
    }

    // incase the peers haven't been started, or the peers don't have their defaults set
    startPeers();
    initServoDefaults();
    
    enableAutoEnable(true);

    broadcastState();
    return true;
  }

  @Deprecated
  public void detach() {
    log.warn("detach deprecated please use disable");
    if (topStom != null) {
      topStom.detach();
      sleep(InMoov.attachPauseMs);
    }
    if (midStom != null) {
      midStom.detach();
      sleep(InMoov.attachPauseMs);
    }
    if (lowStom != null) {
      lowStom.detach();
      sleep(InMoov.attachPauseMs);
    }
  }

  public void disable() {
    if (topStom != null) {
      topStom.disable();
      sleep(InMoov.attachPauseMs);
    }
    if (midStom != null) {
      midStom.disable();
      sleep(InMoov.attachPauseMs);
    }
    if (lowStom != null) {
      lowStom.disable();
      sleep(InMoov.attachPauseMs);
    }
  }

  public long getLastActivityTime() {
    long minLastActivity = Math.max(topStom.getLastActivityTime(), midStom.getLastActivityTime());
    minLastActivity = Math.max(minLastActivity, lowStom.getLastActivityTime());
    return minLastActivity;
  }

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

  @Deprecated
  public void setSpeed(Double topStom, Double midStom, Double lowStom) {
    log.warn("setspeed deprecated please use setvelocity");
    this.topStom.setSpeed(topStom);
    this.midStom.setSpeed(midStom);
    this.lowStom.setSpeed(lowStom);
  }

  public void test() {

    if (controller == null) {
      error("arduino is null");
    }

    /*
     * FIXME - connections need to be outside .. this must be a ServoController
     * if (!arduino.isConnected()) { error("arduino not connected"); }
     */

    topStom.moveTo(topStom.getPos() + 2);
    midStom.moveTo(midStom.getPos() + 2);
    lowStom.moveTo(lowStom.getPos() + 2);

    moveTo(35.0, 45.0, 55.0);
    String move = getScript("i01");
    log.info(move);
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

    ServiceType meta = new ServiceType(InMoovTorso.class.getCanonicalName());
    meta.addDescription("InMoov Torso");
    meta.addCategory("robot");

    meta.addPeer("topStom", "Servo", "Top Stomach servo");
    meta.addPeer("midStom", "Servo", "Mid Stomach servo");
    meta.addPeer("lowStom", "Servo", "Low Stomach servo");
    meta.addPeer("arduino", "Arduino", "Arduino controller for torso");

    return meta;
  }

  public void setVelocity(Double topStom, Double midStom, Double lowStom) {
    this.topStom.setSpeed(topStom);
    this.midStom.setSpeed(midStom);
    this.lowStom.setSpeed(lowStom);
  }
}
