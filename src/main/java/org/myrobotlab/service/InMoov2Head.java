package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.FilenameUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

/**
 * InMoovHead - This is the inmoov head service. This service controls the
 * servos for the following: jaw, eyeX, eyeY, rothead and neck.
 * 
 */
public class InMoov2Head extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoov2Head.class);

  transient public ServoControl jaw;
  transient public ServoControl eyeX;
  transient public ServoControl eyeY;
  transient public ServoControl rothead;
  transient public ServoControl neck;
  transient public ServoControl rollNeck;

  transient public ServoControl eyelidLeft;
  transient public ServoControl eyelidRight;

  transient Timer blinkEyesTimer = new Timer();

  public InMoov2Head(String n, String id) {
    super(n, id);

    // FIXME - future will just be pub/sub attach/detach subscriptions
    // and there will be no need this service.
    // Config will be managed by LangUtils
    startPeers();

    jaw.map(10.0, 25.0, 10.0, 25.0);
    jaw.setRest(10.0);

    eyeX.map(60.0, 120.0, 60.0, 120.0);
    eyeX.setRest(90.0);

    eyeY.map(60.0, 120.0, 60.0, 120.0);
    eyeY.setRest(90.0);

    rollNeck.map(20.0, 160.0, 20.0, 160.0);
    rollNeck.setRest(90.0);

    neck.map(20.0, 160.0, 20.0, 160.0);
    neck.setRest(90.0);

    rothead.map(20.0, 160.0, 20.0, 160.0);
    rothead.setRest(90.0);

    neck.setPin(12);
    rothead.setPin(13);
    eyeX.setPin(22);
    eyeY.setPin(24);
    jaw.setPin(26);
    //FIXME rollNeck and eyelids must be connected to right controller
    //rollNeck.setPin(12);
    //eyelidLeft.setPin(22);
    //eyelidRight.setPin(24);

    neck.map(20.0, 160.0, 20.0, 160.0);
    rollNeck.map(20.0, 160.0, 20.0, 160.0);
    rothead.map(30.0, 150.0, 30.0, 150.0);
    // reset by mouth control
    jaw.map(10.0, 25.0, 10.0, 25.0);
    eyeX.map(60.0, 120.0, 60.0, 120.0);
    eyeY.map(60.0, 120.0, 60.0, 120.0);
    neck.setRest(90.0);
    neck.setPosition(90.0);
    rollNeck.setRest(90.0);
    rollNeck.setPosition(90.0);
    rothead.setRest(90.0);
    rothead.setPosition(90.0);
    jaw.setRest(10.0);
    jaw.setPosition(10.0);
    eyeX.setRest(90.0);
    eyeX.setPosition(90.0);
    eyeY.setRest(90.0);
    eyeY.setPosition(90.0);
    eyelidLeft.setRest(0.0);
    eyelidRight.setRest(0.0);

    eyelidLeft.setSpeed(50.0);
    eyelidRight.setSpeed(50.0);
    setSpeed(45.0, 45.0, null, null, null, 45.0);
  }

  public void blink() {

    // TODO: clean stop autoblink if tracking ...
    double tmpVelo = ThreadLocalRandom.current().nextInt(40, 150 + 1);
    eyelidLeft.setSpeed(tmpVelo);
    eyelidRight.setSpeed(tmpVelo);
    moveToBlocking(180, 180);
    moveToBlocking(0, 0);

  }

  class blinkEyesTimertask extends TimerTask {
    @Override
    public void run() {
      int delay = ThreadLocalRandom.current().nextInt(10, 40 + 1);
      blinkEyesTimer.schedule(new blinkEyesTimertask(), delay * 1000);

      blink();
      // random double blink
      if (ThreadLocalRandom.current().nextInt(0, 1 + 1) == 1) {
        sleep(ThreadLocalRandom.current().nextInt(1000, 2000 + 1));
        blink();
      }
    }
  }

  public void enable() {
    eyeX.enable();
    eyeY.enable();
    jaw.enable();
    rothead.enable();
    neck.enable();
    rollNeck.enable();
    eyelidLeft.enable();
    eyelidRight.enable();

  }

  @Override
  public void broadcastState() {
    rothead.broadcastState();
    rollNeck.broadcastState();
    neck.broadcastState();
    eyeX.broadcastState();
    eyeY.broadcastState();
    jaw.broadcastState();
    eyelidLeft.broadcastState();
    eyelidRight.broadcastState();
  }

  public void stop() {
    rothead.stop();
    neck.stop();
    eyeX.stop();
    eyeY.stop();
    jaw.stop();
    rollNeck.stop();
    eyelidLeft.stop();
    eyelidRight.stop();
  }

  public void disable() {
    stop();
    rothead.disable();
    neck.disable();
    eyeX.disable();
    eyeY.disable();
    jaw.disable();
    rollNeck.disable();
    eyelidLeft.enable();
    eyelidRight.enable();
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
    return String.format(Locale.ENGLISH, "%s.moveHead(%.2f,%.2f,%.2f,%.2f,%.2f,%.2f)\n", inMoovServiceName, neck.getCurrentInputPos(), rothead.getCurrentInputPos(), eyeX.getCurrentInputPos(), eyeY.getCurrentInputPos(),
        jaw.getCurrentInputPos(), rollNeck.getCurrentInputPos());
  }

  public boolean isValid() {
    rothead.moveTo(rothead.getRest() + 2);
    neck.moveTo(neck.getRest() + 2);
    eyeX.moveTo(eyeX.getRest() + 2);
    eyeY.moveTo(eyeY.getRest() + 2);
    jaw.moveTo(jaw.getRest() + 2);
    rollNeck.moveTo(rollNeck.getRest() + 2);
    return true;
  }

  public void lookAt(Double x, Double y, Double z) {
    Double distance = Math.sqrt(Math.pow(x, 2.0) + Math.pow(y, 2.0) + Math.pow(z, 2.0));
    Double rotation = Math.toDegrees(Math.atan(y / x));
    Double colatitude = Math.toDegrees(Math.acos(z / distance));
    System.out.println(distance);
    System.out.println(rotation);
    System.out.println(colatitude);
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
   * @param rotheadPos
   * @param eyeXPos
   * @param eyeYPos
   * @param jawPos
   * @param rollNeckPos
   */
  public void moveTo(Double neckPos, Double rotheadPos, Double eyeXPos, Double eyeYPos, Double jawPos, Double rollNeckPos) {
    if (log.isDebugEnabled()) {
      log.debug("head.moveTo {} {} {} {} {} {}", neckPos, rotheadPos, eyeXPos, eyeYPos, jawPos, rollNeckPos);
    }
    this.rothead.moveTo(rotheadPos);
    this.neck.moveTo(neckPos);
    this.eyeX.moveTo(eyeXPos);
    this.eyeY.moveTo(eyeYPos);
    this.jaw.moveTo(jawPos);
    this.rollNeck.moveTo(rollNeckPos);
  }

  public void moveEyelidsTo(double eyelidleftPos, double eyelidrightPos) {
    eyelidLeft.moveTo(eyelidleftPos);
    eyelidRight.moveTo(eyelidrightPos);
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
    neck.waitTargetPos();
    rothead.waitTargetPos();
    eyeX.waitTargetPos();
    eyeY.waitTargetPos();
    jaw.waitTargetPos();
    rollNeck.waitTargetPos();
  }

  public void release() {
    disable();
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

  public void rest() {
    // initial positions
    // setSpeed(1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
    rothead.rest();
    neck.rest();
    eyeX.rest();
    eyeY.rest();
    jaw.rest();
    rollNeck.rest();
    eyelidLeft.rest();
    eyelidRight.rest();
  }

  @Override
  public boolean save() {
    super.save();
    rothead.save();
    neck.save();
    eyeX.save();
    eyeY.save();
    jaw.save();
    rollNeck.save();
    eyelidLeft.save();
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
    rothead.setAutoDisable(rotheadParam);
    rollNeck.setAutoDisable(rollNeckParam);
    neck.setAutoDisable(neckParam);
  }

  public void setAutoDisable(Boolean param) {
    rothead.setAutoDisable(param);
    neck.setAutoDisable(param);
    eyeX.setAutoDisable(param);
    eyeY.setAutoDisable(param);
    jaw.setAutoDisable(param);
    rollNeck.setAutoDisable(param);
    eyelidLeft.setAutoDisable(param);
    eyelidRight.setAutoDisable(param);
  }

  /**
   * Set the put min and max values for all servoes in the head.  input limits are not modified.
   * 
   * @param headXMin
   * @param headXMax
   * @param headYMin
   * @param headYMax
   * @param eyeXMin
   * @param eyeXMax
   * @param eyeYMin
   * @param eyeYMax
   * @param jawMin
   * @param jawMax
   * @param rollNeckMin
   * @param rollNeckMax
   */
  public void setLimits(double headXMin, double headXMax, double headYMin, double headYMax, double eyeXMin, double eyeXMax, double eyeYMin, double eyeYMax, double jawMin,
      double jawMax, double rollNeckMin, double rollNeckMax) {
    rothead.setMinMaxOutput(headXMin, headXMax);
    neck.setMinMaxOutput(headYMin, headYMax);
    eyeX.setMinMaxOutput(eyeXMin, eyeXMax);
    eyeY.setMinMaxOutput(eyeYMin, eyeYMax);
    jaw.setMinMaxOutput(jawMin, jawMax);
    rollNeck.setMinMaxOutput(rollNeckMin, rollNeckMax);
  }

  public void setSpeed(Double headXSpeed, Double headYSpeed, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {
    setSpeed(headXSpeed, headYSpeed, eyeXSpeed, eyeYSpeed, jawSpeed, null);

  }

  public void setSpeed(Double headXSpeed, Double headYSpeed, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed, Double rollNeckSpeed) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("%s setSpeed %.2f %.2f %.2f %.2f %.2f %.2f", getName(), headXSpeed, headYSpeed, eyeXSpeed, eyeYSpeed, jawSpeed, rollNeckSpeed));
    }
    rothead.setSpeed(headXSpeed);
    neck.setSpeed(headYSpeed);
    eyeX.setSpeed(eyeXSpeed);
    eyeY.setSpeed(eyeYSpeed);
    jaw.setSpeed(jawSpeed);
    jaw.setSpeed(rollNeckSpeed);

  }

  public void fullSpeed() {
    rothead.fullSpeed();
    neck.fullSpeed();
    eyeX.fullSpeed();
    eyeY.fullSpeed();
    jaw.fullSpeed();
    jaw.fullSpeed();
  }

  public void test() {
    rothead.moveTo(rothead.getCurrentInputPos() + 2);
    neck.moveTo(neck.getCurrentInputPos() + 2);
    eyeX.moveTo(eyeX.getCurrentInputPos() + 2);
    eyeY.moveTo(eyeY.getCurrentInputPos() + 2);
    jaw.moveTo(jaw.getCurrentInputPos() + 2);
    rollNeck.moveTo(rollNeck.getCurrentInputPos() + 2);
    eyelidLeft.moveTo(179.0);
    sleep(300);
    eyelidRight.moveToBlocking(1.0);
  }

  public void autoBlink(boolean param) {
    if (blinkEyesTimer != null) {
      blinkEyesTimer.cancel();
      blinkEyesTimer = null;
    }
    if (param) {
      blinkEyesTimer = new Timer();
      new blinkEyesTimertask().run();
    }
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
    rothead.setSpeed(headXSpeed);
    neck.setSpeed(headYSpeed);
    eyeX.setSpeed(eyeXSpeed);
    eyeY.setSpeed(eyeYSpeed);
    jaw.setSpeed(jawSpeed);
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
    neck.setPin(neckPin);
    rothead.setPin(rotheadPin);
    eyeX.setPin(eyeXPin);
    eyeY.setPin(eyeYPin);
    jaw.setPin(jawPin);
    rollNeck.setPin(rollNeckPin);
  }

}
