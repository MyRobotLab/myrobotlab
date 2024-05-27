package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.InMoov2HeadConfig;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * InMoovHead - This is the inmoov head service. This service controls the
 * servos for the following: jaw, eyeX, eyeY, rothead and neck.
 * 
 */
public class InMoov2Head extends Service<InMoov2HeadConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoov2Head.class);

  // peers
  transient public ServoControl jaw;
  transient public ServoControl eyeX;
  transient public ServoControl eyeY;
  transient public ServoControl rothead;
  transient public ServoControl neck;
  transient public ServoControl rollNeck;
  transient public ServoControl eyelidLeft;
  transient public ServoControl eyelidRight;

  public InMoov2Head(String n, String id) {
    super(n, id);
  }

  @Override
  public void startService() {
    super.startService();

    jaw = (ServoControl) getPeer("jaw");
    eyeX = (ServoControl) getPeer("eyeX");
    eyeY = (ServoControl) getPeer("eyeY");
    rothead = (ServoControl) getPeer("rothead");
    neck = (ServoControl) getPeer("neck");
    rollNeck = (ServoControl) getPeer("rollNeck");
    eyelidLeft = (ServoControl) getPeer("eyelidLeft");
    eyelidRight = (ServoControl) getPeer("eyelidRight");
  }

  public void blink() {

    // TODO: clean stop autoblink if tracking ...
    double tmpVelo = ThreadLocalRandom.current().nextInt(40, 150 + 1);
    if (eyelidLeft != null)
      eyelidLeft.setSpeed(tmpVelo);
    if (eyelidRight != null)
      eyelidRight.setSpeed(tmpVelo);
    moveToBlocking(180, 180);
    moveToBlocking(0, 0);
  }

  public void enable() {
    if (eyeX != null)
      eyeX.enable();
    if (eyeY != null)
      eyeY.enable();
    if (jaw != null)
      jaw.enable();
    if (rothead != null)
      rothead.enable();
    if (neck != null)
      neck.enable();
    if (rollNeck != null)
      rollNeck.enable();
    if (eyelidLeft != null)
      eyelidLeft.enable();
    if (eyelidRight != null)
      eyelidRight.enable();

  }

  @Override
  public Service broadcastState() {
    if (rothead != null)
      rothead.broadcastState();
    if (rollNeck != null)
      rollNeck.broadcastState();
    if (neck != null)
      neck.broadcastState();
    if (eyeX != null)
      eyeX.broadcastState();
    if (eyeY != null)
      eyeY.broadcastState();
    if (jaw != null)
      jaw.broadcastState();
    if (eyelidLeft != null)
      eyelidLeft.broadcastState();
    if (eyelidRight != null)
      eyelidRight.broadcastState();
    return this;
  }

  public void stop() {
    if (rothead != null)
      rothead.stop();
    if (neck != null)
      neck.stop();
    if (eyeX != null)
      eyeX.stop();
    if (eyeY != null)
      eyeY.stop();
    if (jaw != null)
      jaw.stop();
    if (rollNeck != null)
      rollNeck.stop();
    if (eyelidLeft != null)
      eyelidLeft.stop();
    if (eyelidRight != null)
      eyelidRight.stop();
  }

  public void disable() {
    stop();
    if (rothead != null)
      rothead.disable();
    if (neck != null)
      neck.disable();
    if (eyeX != null)
      eyeX.disable();
    if (eyeY != null)
      eyeY.disable();
    if (jaw != null)
      jaw.disable();
    if (rollNeck != null)
      rollNeck.disable();
    if (eyelidLeft != null)
      eyelidLeft.disable();
    if (eyelidRight != null)
      eyelidRight.disable();
  }

  public Long getLastActivityTime() {

    Long lastActivityTime = Math.max(rothead.getLastActivityTime(), neck.getLastActivityTime());
    if (getPeer("eyeX") != null) {
      lastActivityTime = Math.max(lastActivityTime, eyeX.getLastActivityTime());
    }
    if (getPeer("eyeY") != null) {
      lastActivityTime = Math.max(lastActivityTime, eyeY.getLastActivityTime());
    }
    if (getPeer("jaw") != null) {
      lastActivityTime = Math.max(lastActivityTime, jaw.getLastActivityTime());
    }
    if (getPeer("rollNeck") != null) {
      lastActivityTime = Math.max(lastActivityTime, rollNeck.getLastActivityTime());
    }
    if (getPeer("rollNeck") != null) {
      lastActivityTime = Math.max(lastActivityTime, rothead.getLastActivityTime());
    }
    if (getPeer("rollNeck") != null) {
      lastActivityTime = Math.max(lastActivityTime, neck.getLastActivityTime());
    }

    if (getPeer("eyelidLeft") != null) {
      lastActivityTime = Math.max(lastActivityTime, eyelidLeft.getLastActivityTime());
    }
    if (getPeer("eyelidRight") != null) {
      lastActivityTime = Math.max(lastActivityTime, eyelidRight.getLastActivityTime());
    }
    return lastActivityTime;
  }

  public String getScript(String inmoovName) {

    Double jaw = (Servo) getPeer("jaw") == null ? null : ((Servo) getPeer("jaw")).getCurrentInputPos();
    Double eyeX = (Servo) getPeer("eyeX") == null ? null : ((Servo) getPeer("eyeX")).getCurrentInputPos();
    Double eyeY = (Servo) getPeer("eyeY") == null ? null : ((Servo) getPeer("eyeY")).getCurrentInputPos();
    Double rothead = (Servo) getPeer("rothead") == null ? null : ((Servo) getPeer("rothead")).getCurrentInputPos();
    Double neck = (Servo) getPeer("neck") == null ? null : ((Servo) getPeer("neck")).getCurrentInputPos();
    Double rollNeck = (Servo) getPeer("rollNeck") == null ? null : ((Servo) getPeer("rollNeck")).getCurrentInputPos();
    Double eyelidLeft = (Servo) getPeer("eyelidLeft") == null ? null : ((Servo) getPeer("eyelidLeft")).getCurrentInputPos();
    Double eyelidRight = (Servo) getPeer("eyelidRight") == null ? null : ((Servo) getPeer("eyelidRight")).getCurrentInputPos();

    StringBuilder head = new StringBuilder(String.format("%s.moveHead(%.0f,%.0f,%.0f,%.0f,%.0f,%.0f)\n", inmoovName, neck, rothead, eyeX, eyeY, jaw, rollNeck));
    if (eyelidLeft != null && eyelidRight != null) {
      head.append(String.format("%s.moveEyelids(%.0f,%.0f)\n", inmoovName, eyelidLeft, eyelidRight));
    }

    return head.toString();
  }

  public boolean isValid() {
    if (rothead != null)
      rothead.moveTo(rothead.getRest() + 2);
    if (neck != null)
      neck.moveTo(neck.getRest() + 2);
    if (eyeX != null)
      eyeX.moveTo(eyeX.getRest() + 2);
    if (eyeY != null)
      eyeY.moveTo(eyeY.getRest() + 2);
    if (jaw != null)
      jaw.moveTo(jaw.getRest() + 2);
    if (rollNeck != null)
      rollNeck.moveTo(rollNeck.getRest() + 2);
    return true;
  }

  public void lookAt(Double x, Double y, Double z) {
    Double distance = Math.sqrt(Math.pow(x, 2.0) + Math.pow(y, 2.0) + Math.pow(z, 2.0));
    Double rotation = Math.toDegrees(Math.atan(y / x));
    Double colatitude = Math.toDegrees(Math.acos(z / distance));
    log.info("distance: " + distance);
    log.info("rotation: " + rotation);
    log.info("colatitude: " + colatitude);
    log.info("object distance is {},rothead servo {},neck servo {} ", distance, rotation, colatitude);
  }

  @Deprecated /* use onMove(map) */
  public void onMoveHead(HashMap<String, Double> map) {
    onMove(map);
  }

  public void onMove(Map<String, Double> map) {
    moveTo(map.get("neck"), map.get("rothead"), map.get("eyeX"), map.get("eyeY"), map.get("jaw"), map.get("rollNeck"));
  }

  // FIXME !!! - this is a mess ... some Double some double ...
  public void moveTo(double neck, double rothead) {
    moveTo(neck, rothead, null, null, null, null);
  }

  // TODO IK head moveTo ( neck + rollneck + rothead ? )
  public void moveTo(Double neck, Double rothead, Double rollNeck) {
    moveTo(neck, rothead, null, null, null, rollNeck);
  }

  public void moveTo(double neck, double rothead, double eyeX, double eyeY) {
    moveTo(neck, rothead, eyeX, eyeY, null, null);
  }

  public void moveTo(double neck, double rothead, double eyeX, double eyeY, double jaw) {
    moveTo(neck, rothead, eyeX, eyeY, jaw, null);
  }

  /**
   * Move servos of the head - null is a none move
   * 
   * @param neckPos
   *          p
   * @param rotheadPos
   *          p
   * @param eyeXPos
   *          p
   * @param eyeYPos
   *          p
   * @param jawPos
   *          p
   * @param rollNeckPos
   *          p
   * 
   */
  public void moveTo(Double neckPos, Double rotheadPos, Double eyeXPos, Double eyeYPos, Double jawPos, Double rollNeckPos) {
    if (log.isDebugEnabled()) {
      log.debug("head.moveTo {} {} {} {} {} {}", neckPos, rotheadPos, eyeXPos, eyeYPos, jawPos, rollNeckPos);
    }
    // In theory this could use mrl standard pub/sub by mapping different output
    // topics to ServoControl.onServoMoveTo
    // but I'm tired ... :)
    ServoControl neck = (ServoControl) Runtime.getService(getPeerName("neck"));
    if (neck != null) {
      neck.moveTo(neckPos);
    }

    ServoControl rothead = (ServoControl) Runtime.getService(getPeerName("rothead"));
    if (rothead != null) {
      rothead.moveTo(rotheadPos);
    }

    ServoControl eyeX = (ServoControl) Runtime.getService(getPeerName("eyeX"));
    if (eyeX != null) {
      eyeX.moveTo(eyeXPos);
    }

    ServoControl eyeY = (ServoControl) Runtime.getService(getPeerName("eyeY"));
    if (eyeY != null) {
      eyeY.moveTo(eyeYPos);
    }

    ServoControl jaw = (ServoControl) Runtime.getService(getPeerName("jaw"));
    if (jaw != null) {
      jaw.moveTo(jawPos);
    }

    ServoControl rollNeck = (ServoControl) Runtime.getService(getPeerName("rollNeck"));
    if (rollNeck != null) {
      rollNeck.moveTo(rollNeckPos);
    }
  }

  public void moveEyelidsTo(double eyelidleftPos, double eyelidrightPos) {
    if (eyelidLeft != null) {
      eyelidLeft.moveTo(eyelidleftPos);
    }
    if (eyelidRight != null) {
      eyelidRight.moveTo(eyelidrightPos);
    }
  }

  public void moveToBlocking(double neck, double rothead) {
    moveToBlocking(neck, rothead, null, null, null, null);
  }

  public void moveToBlocking(double neck, double rothead, Double rollNeck) {
    moveToBlocking(neck, rothead, null, null, null, rollNeck);
  }

  public void moveToBlocking(double neck, double rothead, double eyeX, double eyeY) {
    moveToBlocking(neck, rothead, eyeX, eyeY, null, null);
  }

  public void moveToBlocking(double neck, double rothead, double eyeX, double eyeY, double jaw) {
    moveToBlocking(neck, rothead, eyeX, eyeY, jaw, null);
  }

  public void moveToBlocking(Double neck, Double rothead, Double eyeX, Double eyeY, Double jaw, Double rollNeck) {
    log.info("init {} moveToBlocking ", getName());
    moveTo(neck, rothead, eyeX, eyeY, jaw, rollNeck);
    waitTargetPos();
    log.info("end {} moveToBlocking ", getName());
  }

  public void waitTargetPos() {
    if (neck != null) {
      neck.waitTargetPos();
    }
    if (rothead != null) {
      rothead.waitTargetPos();
    }
    if (eyeX != null) {
      eyeX.waitTargetPos();
    }
    if (eyeY != null) {
      eyeY.waitTargetPos();
    }
    if (jaw != null) {
      jaw.waitTargetPos();
    }
    if (rollNeck != null) {
      rollNeck.waitTargetPos();
    }
  }

  public void release() {
    disable();
  }

  @Override
  public void releaseService() {
    disable();
    super.releaseService();
  }

  public void rest() {
    // initial positions
    // setSpeed(1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
    if (rothead != null) {
      rothead.rest();
    }
    if (neck != null) {
      neck.rest();
    }
    if (eyeX != null) {
      eyeX.rest();
    }
    if (eyeY != null) {
      eyeY.rest();
    }
    if (jaw != null) {
      jaw.rest();
    }
    if (rollNeck != null) {
      rollNeck.rest();
    }
    if (eyelidLeft != null) {
      eyelidLeft.rest();
    }
    if (eyelidRight != null) {
      eyelidRight.rest();
    }
  }

  @Override
  public boolean save() {
    super.save();
    if (rothead != null)
      rothead.save();
    if (neck != null)
      neck.save();
    if (eyeX != null)
      eyeX.save();
    if (eyeY != null)
      eyeY.save();
    if (jaw != null)
      jaw.save();
    if (rollNeck != null)
      rollNeck.save();
    if (eyelidLeft != null)
      eyelidLeft.save();
    if (eyelidRight != null)
      eyelidRight.save();
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

  public void setAutoDisable(Boolean rotheadParam, Boolean neckParam, Boolean rollNeckParam) {
    if (rothead != null)
      rothead.setAutoDisable(rotheadParam);
    if (rollNeck != null)
      rollNeck.setAutoDisable(rollNeckParam);
    if (neck != null)
      neck.setAutoDisable(neckParam);
  }

  public void setAutoDisable(Boolean param) {
    if (rothead != null)
      rothead.setAutoDisable(param);
    if (neck != null)
      neck.setAutoDisable(param);
    if (eyeX != null)
      eyeX.setAutoDisable(param);
    if (eyeY != null)
      eyeY.setAutoDisable(param);
    if (jaw != null)
      jaw.setAutoDisable(param);
    if (rollNeck != null)
      rollNeck.setAutoDisable(param);
    if (eyelidLeft != null)
      eyelidLeft.setAutoDisable(param);
    if (eyelidRight != null)
      eyelidRight.setAutoDisable(param);
  }

  public void setLimits(double headXMin, double headXMax, double headYMin, double headYMax, double eyeXMin, double eyeXMax, double eyeYMin, double eyeYMax, double jawMin,
      double jawMax, double rollNeckMin, double rollNeckMax) {

    if (rothead != null)
      rothead.setMinMaxOutput(headXMin, headXMax);
    if (neck != null)
      neck.setMinMaxOutput(headYMin, headYMax);
    if (eyeX != null)
      eyeX.setMinMaxOutput(eyeXMin, eyeXMax);
    if (eyeY != null)
      eyeY.setMinMaxOutput(eyeYMin, eyeYMax);
    if (jaw != null)
      jaw.setMinMaxOutput(jawMin, jawMax);
    if (rollNeck != null)
      rollNeck.setMinMaxOutput(rollNeckMin, rollNeckMax);
  }

  public void setSpeed(Double headXSpeed, Double headYSpeed, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {
    setSpeed(headXSpeed, headYSpeed, eyeXSpeed, eyeYSpeed, jawSpeed, null);

  }

  public void setSpeed(Double headXSpeed, Double headYSpeed, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed, Double rollNeckSpeed) {
    log.debug(String.format("%s setSpeed %.2f %.2f %.2f %.2f %.2f %.2f", getName(), headXSpeed, headYSpeed, eyeXSpeed, eyeYSpeed, jawSpeed, rollNeckSpeed));
    if (rothead != null)
      rothead.setSpeed(headXSpeed);
    if (neck != null)
      neck.setSpeed(headYSpeed);
    if (eyeX != null)
      eyeX.setSpeed(eyeXSpeed);
    if (eyeY != null)
      eyeY.setSpeed(eyeYSpeed);
    if (jaw != null)
      jaw.setSpeed(jawSpeed);
    if (rollNeck != null)
      rollNeck.setSpeed(rollNeckSpeed);
  }

  public void fullSpeed() {
    if (rothead != null)
      rothead.fullSpeed();
    if (neck != null)
      neck.fullSpeed();
    if (eyeX != null)
      eyeX.fullSpeed();
    if (eyeY != null)
      eyeY.fullSpeed();
    if (jaw != null)
      jaw.fullSpeed();
  }

  public void test() {
    if (rothead != null)
      rothead.moveTo(rothead.getCurrentInputPos() + 2);
    if (neck != null)
      neck.moveTo(neck.getCurrentInputPos() + 2);
    if (eyeX != null)
      eyeX.moveTo(eyeX.getCurrentInputPos() + 2);
    if (eyeY != null)
      eyeY.moveTo(eyeY.getCurrentInputPos() + 2);
    if (jaw != null)
      jaw.moveTo(jaw.getCurrentInputPos() + 2);
    if (rollNeck != null)
      rollNeck.moveTo(rollNeck.getCurrentInputPos() + 2);
    if (eyelidLeft != null)
      eyelidLeft.moveTo(179.0);
    sleep(300);
    if (eyelidRight != null)
      eyelidRight.moveToBlocking(1.0);
  }

  /**
   * FIXME - implement
   * 
   * @param b
   */
  public void autoBlink(boolean b) {

  }

  @Deprecated /* use setSpeed */
  public void setVelocity(Double headXSpeed, Double headYSpeed, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {

    setVelocity(headXSpeed, headYSpeed, eyeXSpeed, eyeYSpeed, jawSpeed, null);

  }

  @Deprecated /* use setSpeed */
  public void setVelocity(Double headXSpeed, Double headYSpeed, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed, Double rollNeckSpeed) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("%s setVelocity %.2f %.2f %.2f %.2f %.2f %.2f", getName(), headXSpeed, headYSpeed, eyeXSpeed, eyeYSpeed, jawSpeed, rollNeckSpeed));
    }
    if (rothead != null)
      rothead.setSpeed(headXSpeed);
    if (neck != null)
      neck.setSpeed(headYSpeed);
    if (eyeX != null)
      eyeX.setSpeed(eyeXSpeed);
    if (eyeY != null)
      eyeY.setSpeed(eyeYSpeed);
    if (jaw != null)
      jaw.setSpeed(jawSpeed);
    if (rollNeck != null)
      rollNeck.setSpeed(rollNeckSpeed);
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);

      String leftPort = "COM3";

      // VirtualArduino vleft = (VirtualArduino) Runtime.start("vleft",
      // "VirtualArduino");
      // vleft.connect("COM3");
      // Runtime.start("gui", "SwingGui");

      InMoov2Head head = (InMoov2Head) Runtime.start("head", "InMoov2Head");

      // log.info(head.getScript("i01"));

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public void setPins(Integer neckPin, Integer rotheadPin, Integer eyeXPin, Integer eyeYPin, Integer jawPin, Integer rollNeckPin) {
    if (neck != null)
      neck.setPin(neckPin);
    if (rothead != null)
      rothead.setPin(rotheadPin);
    if (eyeX != null)
      eyeX.setPin(eyeXPin);
    if (eyeY != null)
      eyeY.setPin(eyeYPin);
    if (jaw != null)
      jaw.setPin(jawPin);
    if (rollNeck != null)
      rollNeck.setPin(rollNeckPin);
  }

}
