package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.InMoov2HeadConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * InMoovHead - This is the inmoov head service. This service controls the
 * servos for the following: jaw, eyeX, eyeY, rothead and neck.
 * 
 */
public class InMoov2Head extends Service {

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
  public void broadcastState() {
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

  public long getLastActivityTime() {

    long lastActivityTime = Math.max(rothead.getLastActivityTime(), neck.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, eyeX.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, eyeY.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, jaw.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, rollNeck.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, eyelidLeft.getLastActivityTime());
    lastActivityTime = Math.max(lastActivityTime, eyelidRight.getLastActivityTime());
    return lastActivityTime;
  }

  @Deprecated /* use LangUtils */
  public String getScript(String inMoovServiceName) {
    return String.format(Locale.ENGLISH, "%s.moveHead(%.2f,%.2f,%.2f,%.2f,%.2f,%.2f)\n", inMoovServiceName, neck.getCurrentInputPos(), rothead.getCurrentInputPos(),
        eyeX.getCurrentInputPos(), eyeY.getCurrentInputPos(), jaw.getCurrentInputPos(), rollNeck.getCurrentInputPos());
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
    if (rothead != null && rotheadPos != null) {
      rothead.moveTo(rotheadPos);
    }
    if (neck != null && neckPos != null) {
      neck.moveTo(neckPos);
    }
    if (eyeX != null && eyeXPos != null) {
      eyeX.moveTo(eyeXPos);
    }
    if (eyeY != null && eyeYPos != null) {
      eyeY.moveTo(eyeYPos);
    }
    if (jaw != null && jawPos != null) {
      jaw.moveTo(jawPos);
    }
    if (rollNeck != null && rollNeckPos != null) {
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

      VirtualArduino vleft = (VirtualArduino) Runtime.start("vleft", "VirtualArduino");
      vleft.connect("COM3");
      Runtime.start("gui", "SwingGui");

      InMoov2Head head = (InMoov2Head) Runtime.start("head", "InMoovHead");

      log.info(head.getScript("i01"));

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

  static public LinkedHashMap<String, ServiceConfig> getDefault(String name) {

    LinkedHashMap<String, ServiceConfig> config = new LinkedHashMap<>();

    InMoov2HeadConfig headConfig = new InMoov2HeadConfig();

    // RuntimeConfig runtime = new RuntimeConfig();
    // runtime.registry = new String[] { controllerName, cvName, tiltName,
    // panName, pidName, trackingName };

    // set local names and config
    headConfig.jaw = name + ".jaw";
    headConfig.eyeX = name + ".eyeX";
    headConfig.eyeY = name + ".eyeY";
    headConfig.rothead = name + ".rothead";
    headConfig.neck = name + ".neck";
    headConfig.rollNeck = name + ".rollNeck";
    headConfig.eyelidLeft = name + ".eyelidLeft";
    headConfig.eyelidRight = name + ".eyelidRight";

    // build a config with all peer defaults
    config.putAll(ServiceInterface.getDefault(headConfig.jaw, "Servo"));
    config.putAll(ServiceInterface.getDefault(headConfig.eyeX, "Servo"));
    config.putAll(ServiceInterface.getDefault(headConfig.eyeY, "Servo"));
    config.putAll(ServiceInterface.getDefault(headConfig.rothead, "Servo"));
    config.putAll(ServiceInterface.getDefault(headConfig.neck, "Servo"));
    config.putAll(ServiceInterface.getDefault(headConfig.rollNeck, "Servo"));
    config.putAll(ServiceInterface.getDefault(headConfig.eyelidLeft, "Servo"));
    config.putAll(ServiceInterface.getDefault(headConfig.eyelidRight, "Servo"));

    ServoConfig jaw = (ServoConfig) config.get(headConfig.jaw);
    jaw.autoDisable = true;
    jaw.clip = true;
    jaw.controller = "i01.left";
    jaw.idleTimeout = 3000;
    jaw.inverted = false;
    jaw.maxIn = 180.0;
    jaw.maxOut = 25.0;
    jaw.minIn = 0.0;
    jaw.minOut = 10.0;
    jaw.pin = "26";
    jaw.rest = 10.0;
    jaw.speed = 500.0;
    jaw.sweepMax = null;
    jaw.sweepMin = null;

    ServoConfig eyeX = (ServoConfig) config.get(headConfig.eyeX);
    eyeX.autoDisable = true;
    eyeX.clip = true;
    eyeX.controller = "i01.left";
    eyeX.idleTimeout = 3000;
    eyeX.inverted = false;
    eyeX.maxIn = 180.0;
    eyeX.maxOut = 120.0;
    eyeX.minIn = 0.0;
    eyeX.minOut = 60.0;
    eyeX.pin = "22";
    eyeX.rest = 90.0;
    eyeX.speed = null;
    eyeX.sweepMax = null;
    eyeX.sweepMin = null;

    ServoConfig eyeY = (ServoConfig) config.get(headConfig.eyeY);
    eyeY.autoDisable = true;
    eyeY.clip = true;
    eyeY.controller = "i01.left";
    eyeY.idleTimeout = 3000;
    eyeY.inverted = false;
    eyeY.maxIn = 180.0;
    eyeY.maxOut = 120.0;
    eyeY.minIn = 0.0;
    eyeY.minOut = 60.0;
    eyeY.pin = "24";
    eyeY.rest = 90.0;
    eyeY.speed = null;
    eyeY.sweepMax = null;
    eyeY.sweepMin = null;

    ServoConfig rothead = (ServoConfig) config.get(headConfig.rothead);
    rothead.autoDisable = true;
    rothead.clip = true;
    rothead.controller = "i01.left";
    rothead.enabled = false;
    rothead.idleTimeout = 3000;
    rothead.inverted = false;
    rothead.maxIn = 180.0;
    rothead.maxOut = 150.0;
    rothead.minIn = 0.0;
    rothead.minOut = 30.0;
    rothead.pin = "13";
    rothead.rest = 90.0;
    rothead.speed = 45.0;
    rothead.sweepMax = null;
    rothead.sweepMin = null;
    
    ServoConfig neck = (ServoConfig) config.get(headConfig.neck);
    neck.autoDisable = true;
    neck.clip = true;
    neck.controller = "i01.left";
    neck.enabled = false;
    neck.idleTimeout = 3000;
    neck.inverted = false;
    neck.maxIn = 180.0;
    neck.maxOut = 160.0;
    neck.minIn = 0.0;
    neck.minOut = 20.0;
    neck.pin = "12";
    neck.rest = 90.0;
    neck.speed = 45.0;
    neck.sweepMax = null;
    neck.sweepMin = null;

    ServoConfig rollNeck = (ServoConfig) config.get(headConfig.rollNeck);
    rollNeck.autoDisable = true;
    rollNeck.clip = true;
    rollNeck.controller = "i01.right";
    rollNeck.enabled = false;
    rollNeck.idleTimeout = 3000;
    rollNeck.inverted = false;
    rollNeck.maxIn = 180.0;
    rollNeck.maxOut = 160.0;
    rollNeck.minIn = 0.0;
    rollNeck.minOut = 20.0;
    rollNeck.pin = "12";
    rollNeck.rest = 90.0;
    rollNeck.speed = 45.0;
    rollNeck.sweepMax = null;
    rollNeck.sweepMin = null;

    ServoConfig eyelidLeft = (ServoConfig) config.get(headConfig.eyelidLeft);
    eyelidLeft.autoDisable = true;
    eyelidLeft.clip = true;
    eyelidLeft.controller = "i01.right";
    eyelidLeft.enabled = false;
    eyelidLeft.idleTimeout = 3000;
    eyelidLeft.inverted = false;
    eyelidLeft.maxIn = 180.0;
    eyelidLeft.maxOut = 180.0;
    eyelidLeft.minIn = 0.0;
    eyelidLeft.minOut = 0.0;
    eyelidLeft.pin = "24";
    eyelidLeft.rest = 0.0;
    eyelidLeft.speed = 50.0;
    eyelidLeft.sweepMax = null;
    eyelidLeft.sweepMin = null;
    
    ServoConfig eyelidRight = (ServoConfig) config.get(headConfig.eyelidRight);
    eyelidRight.autoDisable = true;
    eyelidRight.clip = true;
    eyelidRight.controller = "i01.right";
    eyelidRight.enabled = false;
    eyelidRight.idleTimeout = 3000;
    eyelidRight.inverted = false;
    eyelidRight.maxIn = 180.0;
    eyelidRight.maxOut = 180.0;
    eyelidRight.minIn = 0.0;
    eyelidRight.minOut = 0.0;
    eyelidRight.pin = "22";
    eyelidRight.rest = 0.0;
    eyelidRight.speed = 50.0;
    eyelidRight.sweepMax = null;
    eyelidRight.sweepMin = null;

    return config;

  }

}
