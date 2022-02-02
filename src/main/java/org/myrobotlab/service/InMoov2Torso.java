package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.InMoov2TorsoConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.interfaces.ServoControl;
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
  }

  public void startService() {
    super.startService();
  }

  public void releaseService() {
    try {
      disable();
      super.releaseService();
    } catch (Exception e) {
      error(e);
    }
  }

  public void enable() {
    if (topStom != null)
      topStom.enable();
    if (midStom != null)
      midStom.enable();
    if (lowStom != null)
      lowStom.enable();
  }

  public void setAutoDisable(Boolean param) {
    if (topStom != null)
      topStom.setAutoDisable(param);
    if (midStom != null)
      midStom.setAutoDisable(param);
    if (lowStom != null)
      lowStom.setAutoDisable(param);
  }

  @Override
  public void broadcastState() {
    if (topStom != null)
      topStom.broadcastState();
    if (midStom != null)
      midStom.broadcastState();
    if (lowStom != null)
      lowStom.broadcastState();
  }

  public void disable() {
    if (topStom != null)
      topStom.disable();
    if (midStom != null)
      midStom.disable();
    if (lowStom != null)
      lowStom.disable();
  }

  public long getLastActivityTime() {
    long minLastActivity = Math.max(topStom.getLastActivityTime(), midStom.getLastActivityTime());
    minLastActivity = Math.max(minLastActivity, lowStom.getLastActivityTime());
    return minLastActivity;
  }

  @Deprecated /* use LangUtils */
  public String getScript(String inMoovServiceName) {
    return String.format(Locale.ENGLISH, "%s.moveTorso(%.2f,%.2f,%.2f)\n", inMoovServiceName, topStom.getCurrentInputPos(), midStom.getCurrentInputPos(),
        lowStom.getCurrentInputPos());
  }

  public void moveTo(Double topStomPos, Double midStomPos, Double lowStomPos) {
    if (log.isDebugEnabled()) {
      log.debug("{} moveTo {} {} {}", getName(), topStomPos, midStomPos, lowStomPos);
    }
    if (topStom != null && topStomPos != null) {
      this.topStom.moveTo(topStomPos);
    }
    if (midStom != null && midStomPos != null) {
      this.midStom.moveTo(midStomPos);
    }
    if (lowStom != null && lowStomPos != null) {
      this.lowStom.moveTo(lowStomPos);
    }
  }

  public void moveToBlocking(Double topStomPos, Double midStomPos, Double lowStomPos) {
    log.info("init {} moveToBlocking ", getName());
    moveTo(topStomPos, midStomPos, lowStomPos);
    waitTargetPos();
    log.info("end {} moveToBlocking", getName());
  }

  public void waitTargetPos() {
    if (topStom != null)
      topStom.waitTargetPos();
    if (midStom != null)
      midStom.waitTargetPos();
    if (lowStom != null)
      lowStom.waitTargetPos();
  }

  public void rest() {
    if (topStom != null)
      topStom.rest();
    if (midStom != null)
      midStom.rest();
    if (lowStom != null)
      lowStom.rest();
  }

  @Override
  public boolean save() {
    super.save();
    if (topStom != null)
      topStom.save();
    if (midStom != null)
      midStom.save();
    if (lowStom != null)
      lowStom.save();
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

  /**
   * Sets the output min/max values for all servos in the torso. input limits on
   * servos are not modified in this method.
   * 
   * @param topStomMin
   *          a
   * @param topStomMax
   *          a
   * @param midStomMin
   *          a
   * @param midStomMax
   *          a
   * @param lowStomMin
   *          a
   * @param lowStomMax
   *          a
   * 
   */
  public void setLimits(double topStomMin, double topStomMax, double midStomMin, double midStomMax, double lowStomMin, double lowStomMax) {
    if (topStom != null)
      topStom.setMinMaxOutput(topStomMin, topStomMax);
    if (midStom != null)
      midStom.setMinMaxOutput(midStomMin, midStomMax);
    if (lowStom != null)
      lowStom.setMinMaxOutput(lowStomMin, lowStomMax);
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

  public void setSpeed(Double topStomSpeed, Double midStomSpeed, Double lowStomSpeed) {
    if (topStom != null)
      topStom.setSpeed(topStomSpeed);
    if (midStom != null)
      midStom.setSpeed(midStomSpeed);
    if (lowStom != null)
      lowStom.setSpeed(lowStomSpeed);
  }

  public void test() {

    if (topStom != null)
      topStom.moveTo(topStom.getCurrentInputPos() + 2);
    if (midStom != null)
      midStom.moveTo(midStom.getCurrentInputPos() + 2);
    if (lowStom != null)
      lowStom.moveTo(lowStom.getCurrentInputPos() + 2);

    moveTo(35.0, 45.0, 55.0);
  }

  @Deprecated /* use setSpeed */
  public void setVelocity(Double topStomSpeed, Double midStomSpeed, Double lowStomSpeed) {
    if (topStom != null)
      topStom.setSpeed(topStomSpeed);
    if (midStom != null)
      midStom.setSpeed(midStomSpeed);
    if (lowStom != null)
      lowStom.setSpeed(lowStomSpeed);
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
    if (topStom != null)
      topStom.fullSpeed();
    if (midStom != null)
      midStom.fullSpeed();
    if (lowStom != null)
      lowStom.fullSpeed();
  }

  public void stop() {
    if (topStom != null)
      topStom.stop();
    if (midStom != null)
      midStom.stop();
    if (lowStom != null)
      lowStom.stop();
  }

  static public LinkedHashMap<String, ServiceConfig> getDefault(String name) {

    LinkedHashMap<String, ServiceConfig> config = new LinkedHashMap<>();

    InMoov2TorsoConfig torsoConfig = new InMoov2TorsoConfig();

    // RuntimeConfig runtime = new RuntimeConfig();
    // runtime.registry = new String[] { controllerName, cvName, tiltName,
    // panName, pidName, trackingName };

    // set local names and config
    torsoConfig.topStom = name + ".topStom";
    torsoConfig.midStom = name + ".midStom";
    torsoConfig.lowStom = name + ".lowStom";

    // build a config with all peer defaults
    config.putAll(ServiceInterface.getDefault(torsoConfig.topStom, "Servo"));
    config.putAll(ServiceInterface.getDefault(torsoConfig.midStom, "Servo"));
    config.putAll(ServiceInterface.getDefault(torsoConfig.lowStom, "Servo"));

    ServoConfig topStom = (ServoConfig) config.get(torsoConfig.topStom);
    topStom.autoDisable = true;
    topStom.clip = true;
    topStom.controller = "i01.left";
    topStom.idleTimeout = 3000;
    topStom.inverted = false;
    topStom.maxIn = 180.0;
    topStom.maxOut = 120.0;
    topStom.minIn = 0.0;
    topStom.minOut = 60.0;
    topStom.pin = "27";
    topStom.rest = 90.0;
    topStom.speed = 20.0;
    topStom.sweepMax = null;
    topStom.sweepMin = null;

    ServoConfig midStom = (ServoConfig) config.get(torsoConfig.midStom);
    midStom.autoDisable = true;
    midStom.clip = true;
    midStom.controller = "i01.left";
    midStom.idleTimeout = 3000;
    midStom.inverted = false;
    midStom.maxIn = 180.0;
    midStom.maxOut = 120.0;
    midStom.minIn = 0.0;
    midStom.minOut = 60.0;
    midStom.pin = "28";
    midStom.rest = 90.0;
    midStom.speed = 20.0;
    midStom.sweepMax = null;
    midStom.sweepMin = null;

    ServoConfig lowStom = (ServoConfig) config.get(torsoConfig.lowStom);
    lowStom.autoDisable = true;
    lowStom.clip = true;
    lowStom.controller = "i01.left";
    lowStom.idleTimeout = 3000;
    lowStom.inverted = false;
    lowStom.maxIn = 180.0;
    lowStom.maxOut = 180.0;
    lowStom.minIn = 0.0;
    lowStom.minOut = 0.0;
    lowStom.pin = "29";
    lowStom.rest = 90.0;
    lowStom.speed = 20.0;
    lowStom.sweepMax = null;
    lowStom.sweepMin = null;

    return config;

  }  
  
}
