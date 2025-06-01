package org.myrobotlab.service;

import java.util.Set;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.sensor.TimeEncoder;
import org.myrobotlab.service.abstracts.AbstractServo;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.data.ServoMove;
import org.slf4j.Logger;

/**
 * Servos have both input and output. Input is usually of the range of integers
 * between 0.0 - 180.0, and output can relay those values directly to the
 * servo's firmware (Arduino ServoLib, I2C controller, etc)
 * 
 * However there can be the occasion that the input comes from a system which
 * does not have the same range. Such that input can vary from 0.0 to 1.0. For
 * example, OpenCV coordinates are often returned in this range. When a mapping
 * is needed Servo.map can be used. For this mapping Servo.map(0.0, 1.0, 0, 180)
 * might be desired. Reversing input would be done with Servo.map(180, 0, 0,
 * 180)
 * 
 * outputY - is the values sent to the firmware, and should not necessarily be
 * confused with the inputX which is the input values sent to the servo
 * 
 * FIXME - inherit from AbstractMotor ..
 * 
 * @author GroG
 */
public class Servo extends AbstractServo<ServoConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Servo.class);

  public Servo(String n, String id) {
    super(n, id);
  }

  /**
   * max complexity moveTo
   * 
   * FIXME - move is more general and could be the "max" complexity method with
   * positional information supplied
   * 
   * @param newPos
   * @param blocking
   * @param timeoutMs
   */
  @Override
  protected boolean processMove(Double newPos, boolean blocking, Long timeoutMs) {
    if (newPos == null) {
      log.info("servo processMove(null) not valid position");
      return false;
    }

    double minLimit = Math.min(mapper.minX, mapper.maxX);
    double maxLimit = Math.max(mapper.minX, mapper.maxX);
    newPos = (newPos < minLimit) ? minLimit : newPos;
    newPos = (newPos > maxLimit) ? maxLimit : newPos;

    log.debug("{} processMove {}", getName(), newPos);

    // This is to allow attaching disabled
    // then delay enabling until the first moveTo command
    // is used
    if (firstMove && !enabled) {
      enable();
      firstMove = false;
    }

    if (config.autoDisable && !enabled) {
      // if the servo was disable with a timer - re-enable it
      enable();
    }
    // purge any timers currently in process
    // if currently configured to autoDisable - the timer starts now
    // we cancel any pre-existing timer if it exists
    purgeTask("disable");
    // blocking move will be idleTime out enabled later.

    if (!enabled) {
      log.info("cannot moveTo {} not enabled", getName());
      return false;
    }
    targetPos = newPos;
    log.debug("pos {} output {}", targetPos, getTargetOutput());

    /**
     * <pre>
     * 
     * BLOCKING 
     *   
     *   if isBlocking already, and incoming request is not blocking - we cancel it 
     *   if isBlocking already, and incoming request is a blocking one - we block it
     *   if not currently blocking, and incoming request is blocking - we start blocking 
     *               with default encoder until it - unblocks or max-timeout is reached
     * 
     * </pre>
     *
     */
    // FIXME - poor implementation - should addListener(sync)
    // and use pub/sub :(
    for (String syncServo : syncedServos) {
      send(syncServo, "moveTo", newPos);
    }
    // TODO: this block isn't tested by ServoTest
    if (isBlocking && !blocking) {
      // if isBlocking already, and incoming request is not blocking - we cancel
      log.info("{} is currently blocking - ignoring request to moveTo({})", getName(), newPos);
      return false;
    }

    // broadcast("publishServoMoveTo", new ServoMove(getName(), newPos,
    // mapper.calcOutput(newPos))); apparently we want input here
    // THIS IS CONSUMED BY ARDUINO CONTROLLER - IT USES ServoMove.outputPos !!!!
    broadcast("publishServoMoveTo", new ServoMove(getName(), newPos, mapper.calcOutput(newPos)));

    // TODO: this block isn't tested by ServoTest
    if (isBlocking && blocking) {
      // if isBlocking already, and incoming request is a blocking one - we
      // block it
      log.info("{} is currently blocking - request to moveToBlocking({}) will need to wait", getName(), newPos);
      synchronized (this) {
        try {
          this.wait();
        } catch (InterruptedException e) {
          /* don't care */
        }
      }
      return false;
    }
    if (!isBlocking && blocking) {
      // if not currently blocking, and incoming request is blocking - we start
      // blocking with default encoder until it - unblocks or max-timeout is
      // reached - if timeout not specified - we block until an encoder unblocks
      // us
      log.info("{} is currently blocking - request to moveToBlocking({}) will need to wait", getName(), newPos);
      isBlocking = true;
    }

    lastActivityTimeTs = System.currentTimeMillis();
    isMoving = true;
    // "real" encoders are electrically hooked up to the servo and get their
    // events through
    // data lines - faux encoders need to be told in software when servos begin
    // movement
    // usually knowing about encoder type is "bad" but the timer encoder is the
    // default native encoder
    long blockingTimeMs = 0;
    if (encoder != null && encoder instanceof TimeEncoder) {
      TimeEncoder timeEncoder = (TimeEncoder) encoder;
      // calculate trajectory calculates and processes this move
      blockingTimeMs = timeEncoder.calculateTrajectory(getCurrentInputPos(), getTargetPos(), getSpeed());
    }

    if (isBlocking) {
      // our thread did a blocking call - we will wait until encoder notifies us
      // to continue or timeout (if supplied) has been reached - "cheesy" need
      // to
      // re-work for real monitor callbacks from real encoders
      sleep(blockingTimeMs);
      isBlocking = false;
      isMoving = false;
      if (config.autoDisable) {
        // and start our countdown
        addTaskOneShot(idleTimeout, "disable");
      }
    }
    return true;
  }

  @Deprecated
  public void enableAutoDisable(boolean value) {
    setAutoDisable(value);
  }

  @Override
  public ServiceConfig getFilteredConfig() {
    ServoConfig sc = (ServoConfig) super.getFilteredConfig();
    Set<String> removeList = Set.of("onServoEnable", "onServoDisable", "onEncoderData", "onServoSetSpeed", "onServoWriteMicroseconds", "onServoMoveTo", "onServoStop");
    if (sc.listeners != null) {
      sc.listeners.removeIf(listener -> removeList.contains(listener.callback));
    }
    return sc;
  }

  public static void main(String[] args) throws InterruptedException {
    try {

      // log.info("{}","blah$Blah".contains("$"));

      LoggingFactory.init(Level.INFO);
      // Platform.setVirtual(true);

      // Runtime.start("python", "Python");
      // Runtime runtime = Runtime.getInstance();

      Runtime.start("clock", "Servo");
      Runtime runtime = Runtime.getInstance();
      // runtime.connect("http://localhost:8888");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      Servo tilt = (Servo) Runtime.start("tilt", "Servo");
      Servo pan = (Servo) Runtime.start("pan", "Servo");

      Arduino mega = (Arduino) Runtime.start("mega", "Arduino");

      tilt.setPin(4);
      pan.setPin(5);
      tilt.setMinMax(10, 100);
      pan.setMinMax(5, 105);
      tilt.setInverted(true);

      mega.connect("/dev/ttyACM0");

      mega.attach(tilt);
      mega.attach(pan);

      boolean done = true;
      if (done) {
        return;
      }

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
