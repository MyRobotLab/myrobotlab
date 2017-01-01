package org.myrobotlab.service;

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
      InMoovTorso torso = (InMoovTorso) Runtime.createAndStart("torso", "InMoovTorso");
      torso.connect("COM4");
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

  /**
   * attach all the servos - this must be re-entrant and accomplish the
   * re-attachment when servos are detached
   * 
   * @return
   */
  public boolean attach() {
 
    sleep(InMoov.attachPauseMs);
    topStom.attach();
    sleep(InMoov.attachPauseMs);
    midStom.attach();
    sleep(InMoov.attachPauseMs);
    lowStom.attach();
    sleep(InMoov.attachPauseMs);
    return true;
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

    broadcastState();
    return true;
  }

  public void detach() {
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

  public long getLastActivityTime() {
    long minLastActivity = Math.max(topStom.getLastActivityTime(), midStom.getLastActivityTime());
    minLastActivity = Math.max(minLastActivity, lowStom.getLastActivityTime());
    return minLastActivity;
  }

  public String getScript(String inMoovServiceName) {
    return String.format("%s.moveTorso(%d,%d,%d)\n", inMoovServiceName, topStom.getPos(), midStom.getPos(), lowStom.getPos());
  }

  public boolean isAttached() {
    boolean attached = false;

    attached |= topStom.isAttached();
    attached |= midStom.isAttached();
    attached |= lowStom.isAttached();

    return attached;
  }

  public void moveTo(Integer topStom, Integer midStom, Integer lowStom) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("%s moveTo %d %d %d", getName(), topStom, midStom, lowStom));
    }
    this.topStom.moveTo(topStom);
    this.midStom.moveTo(midStom);
    this.lowStom.moveTo(lowStom);

  }

  // FIXME - releasePeers()
  public void release() {
    detach();
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

    setSpeed(1.0, 1.0, 1.0);

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

  public void setLimits(int bicepMin, int bicepMax, int rotateMin, int rotateMax, int shoulderMin, int shoulderMax) {
    topStom.setMinMax(bicepMin, bicepMax);
    midStom.setMinMax(rotateMin, rotateMax);
    lowStom.setMinMax(shoulderMin, shoulderMax);
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

  public void setSpeed(Double topStom, Double midStom, Double lowStom) {
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

    moveTo(35, 45, 55);
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
    meta.addPeer("arduino", "Arduino", "Arduino controller for this arm");

    return meta;
  }

  public void setVelocity(Double topStom, Double midStom, Double lowStom) {
    this.topStom.setVelocity(topStom);
    this.midStom.setVelocity(midStom);
    this.lowStom.setVelocity(lowStom);
   }
}
