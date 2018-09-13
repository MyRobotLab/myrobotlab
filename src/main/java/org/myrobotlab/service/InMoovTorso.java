package org.myrobotlab.service;

import java.util.Locale;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * InMoovTorso - The inmoov torso. This will allow control of the topStom,
 * midStom, and lowStom servos.
 *
 */
public class InMoovTorso extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoovTorso.class);

  transient public Servo topStom;
  transient public Servo midStom;
  transient public Servo lowStom;
  transient public Arduino arduino;

  static public void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      VirtualArduino v = (VirtualArduino)Runtime.start("virtual", "VirtualArduino");
      
      v.connect("COM4");
      InMoovTorso torso = (InMoovTorso) Runtime.start("i01.torso", "InMoovTorso");
      torso.connect("COM4");
      Runtime.start("webgui", "WebGui");
      torso.test();
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public InMoovTorso(String n) {
    super(n);
    // createReserves(n); // Ok this might work but IT CANNOT BE IN SERVICE
    // FRAMEWORK !!!!!
    topStom = (Servo) createPeer("topStom");
    midStom = (Servo) createPeer("midStom");
    lowStom = (Servo) createPeer("lowStom");
    arduino = (Arduino) createPeer("arduino");

    topStom.setMinMax(60, 120);
    midStom.setMinMax(0, 180);
    lowStom.setMinMax(0, 180);

    topStom.setRest(90);
    midStom.setRest(90);
    lowStom.setRest(90);

    setVelocity(5.0,5.0,5.0);
    
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
  
  public void setOverrideAutoDisable(Boolean param) {
    topStom.setOverrideAutoDisable(param);
    midStom.setOverrideAutoDisable(param);
    lowStom.setOverrideAutoDisable(param);
    }  
  
  @Override
  public void broadcastState() {
    // notify the gui
    topStom.broadcastState();
    midStom.broadcastState();
    lowStom.broadcastState();
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

    topStom.attach(arduino, 27, topStom.getRest(), topStom.getVelocity());
    midStom.attach(arduino, 28, midStom.getRest(), midStom.getVelocity());
    lowStom.attach(arduino, 29, lowStom.getRest(), lowStom.getVelocity());
    
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
    return String.format(Locale.ENGLISH,"%s.moveTorso(%.2f,%.2f,%.2f)\n", inMoovServiceName, topStom.getPos(), midStom.getPos(), lowStom.getPos());
  }

  public boolean isAttached() {
    boolean attached = false;

    attached |= topStom.isAttached();
    attached |= midStom.isAttached();
    attached |= lowStom.isAttached();

    return attached;
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

  
  // FIXME - releasePeers()
  public void release() {
    disable();
    if (topStom != null) {
      topStom.releaseService();
      topStom = null;
    }
    if (midStom != null) {
      midStom.releaseService();
      midStom = null;
    }
    if (lowStom != null) {
      lowStom.releaseService();
      lowStom = null;
    }
  }

  public void rest() {

    //setSpeed(1.0, 1.0, 1.0);

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

  public void setLimits(int topStomMin, int topStomMax, int midStomMin, int midStomMax, int lowStomMin, int lowStomMax) {
    topStom.setMinMax(topStomMin, topStomMax);
    midStom.setMinMax(midStomMin, midStomMax);
    lowStom.setMinMax(lowStomMin, lowStomMax);
  }

  // ------------- added set pins
  public void setpins(Integer topStomPin, Integer midStomPin, Integer lowStomPin) {
    // createPeers();
	  /*
    this.topStom.setPin(topStom);
    this.midStom.setPin(midStom);
    this.lowStom.setPin(lowStom);
    */
	  

	    arduino.servoAttachPin(topStom, topStomPin);
	    arduino.servoAttachPin(topStom, midStomPin);
	    arduino.servoAttachPin(topStom, lowStomPin);
  }

  @Deprecated
  public void setSpeed(Double topStom, Double midStom, Double lowStom) {
	log.warn("setspeed deprecated please use setvelocity");
    this.topStom.setSpeed(topStom);
    this.midStom.setSpeed(midStom);
    this.lowStom.setSpeed(lowStom);
  }

  /*
   * public boolean load() { super.load(); topStom.load(); midStom.load();
   * lowStom.load(); return true; }
   */

  @Override
  public void startService() {
    super.startService();
    topStom.startService();
    midStom.startService();
    lowStom.startService();
    arduino.startService();
  }

  public void test() {

    if (arduino == null) {
      error("arduino is null");
    }

    if (!arduino.isConnected()) {
      error("arduino not connected");
    }

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
    this.topStom.setVelocity(topStom);
    this.midStom.setVelocity(midStom);
    this.lowStom.setVelocity(lowStom);
   }
}
