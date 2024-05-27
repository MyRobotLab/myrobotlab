package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.InMoov2TorsoConfig;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * InMoovTorso - The inmoov torso. This will allow control of the topStom,
 * midStom, and lowStom servos.
 *
 */
public class InMoov2Torso extends Service<InMoov2TorsoConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoov2Torso.class);

  transient public ServoControl topStom;
  transient public ServoControl midStom;
  transient public ServoControl lowStom;

  public InMoov2Torso(String n, String id) {
    super(n, id);
  }

  @Override
  public void startService() {
    super.startService();
    
    topStom = (ServoControl) getPeer("topStom");
    midStom = (ServoControl) getPeer("midStom");
    lowStom = (ServoControl) getPeer("lowStom");
  }

  @Override
  public void releaseService() {
    try {
      disable();

      topStom = null;
      midStom = null;
      lowStom = null;

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
  public Service broadcastState() {
    if (topStom != null)
      topStom.broadcastState();
    if (midStom != null)
      midStom.broadcastState();
    if (lowStom != null)
      lowStom.broadcastState();
    return this;
  }

  public void disable() {
    if (topStom != null)
      topStom.disable();
    if (midStom != null)
      midStom.disable();
    if (lowStom != null)
      lowStom.disable();
  }
  
  @Deprecated /* use onMove(map) */
  public void onMoveTorso(HashMap<String, Double> map) {
    onMove(map);
  }

  public void onMove(Map<String, Double> map) {
    moveTo(map.get("topStom"), map.get("midStom"), map.get("lowStom"));
  }


  public long getLastActivityTime() {
    long minLastActivity = Math.max(topStom.getLastActivityTime(), midStom.getLastActivityTime());
    minLastActivity = Math.max(minLastActivity, lowStom.getLastActivityTime());
    return minLastActivity;
  }

  public String getScript(String inMoovServiceName) {
    return String.format("%s.moveTorso(%.0f,%.0f,%.0f)\n", inMoovServiceName, topStom.getCurrentInputPos(), midStom.getCurrentInputPos(),
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

}
