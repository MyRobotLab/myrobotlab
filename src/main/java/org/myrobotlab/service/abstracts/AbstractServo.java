package org.myrobotlab.service.abstracts;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.framework.Config;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.sensor.EncoderPublisher;
import org.myrobotlab.sensor.TimeEncoder;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.ServoData;
import org.myrobotlab.service.interfaces.ServoData.ServoStatus;
import org.slf4j.Logger;

/**
 * 
 * @author GroG
 * 
 *         There was (in the past) great confusion about position of the servo.
 *         To make things clear - you must think of 2 different sets of data.
 * 
 *         The set of "what I want" versus the set of "what is".
 * 
 *         The set of "What I want" is all control related data. Target position
 *         would be part of this set. All data related to telling the servo what
 *         you want it to do.
 * 
 *         "What is" is all the data for which the servo reports. For example
 *         its "current position". You tell it what position you want it to be,
 *         and in turn it tells you what position its currently in. "Control"
 *         versus "Status". Do not mix these concepts, keep seperate variables.
 * 
 *         The mapper accepts inputs, the controller needs mapper outputs.
 *         Nothing outside of the servo controller should need the mapper
 *         outputs.
 * 
 *
 */
public abstract class AbstractServo extends Service implements ServoControl, EncoderPublisher {

  public final static Logger log = LoggerFactory.getLogger(AbstractServo.class);

  private static final long serialVersionUID = 1L;

  public static void main(String[] args) throws InterruptedException {
    try {

      LoggingFactory.init(Level.INFO);
      Platform.setVirtual(true);

      Runtime.start("gui", "SwingGui");
      // Runtime.start("python", "Python");

      Arduino mega = (Arduino) Runtime.start("mega", "Arduino");
      mega.connect("COM7");
      // mega.setBoardMega();

      Servo servo03 = (Servo) Runtime.start("servo03", "Servo");

      double pos = 78;
      servo03.setPosition(pos);

      double min = 3;
      double max = 170;
      double speed = 5; // degree/s

      servo03.attach(mega, 8, 38.0);

      // servo03.sweep(min, max, speed);

      /*
       * Servo servo04 = (Servo) Runtime.start("servo04", "Servo"); Servo
       * servo05 = (Servo) Runtime.start("servo05", "Servo"); Servo servo06 =
       * (Servo) Runtime.start("servo06", "Servo"); Servo servo07 = (Servo)
       * Runtime.start("servo07", "Servo"); Servo servo08 = (Servo)
       * Runtime.start("servo08", "Servo"); Servo servo09 = (Servo)
       * Runtime.start("servo09", "Servo"); Servo servo10 = (Servo)
       * Runtime.start("servo10", "Servo"); Servo servo11 = (Servo)
       * Runtime.start("servo11", "Servo"); Servo servo12 = (Servo)
       * Runtime.start("servo12", "Servo");
       */
      // Servo servo13 = (Servo) Runtime.start("servo13", "Servo");

      /*
       * servo04.attach(mega, 4, 38.0); servo05.attach(mega, 5, 38.0);
       * servo06.attach(mega, 6, 38.0); servo07.attach(mega, 7, 38.0);
       * servo08.attach(mega, 8, 38.0); servo09.attach(mega, 9, 38.0);
       * servo10.attach(mega, 10, 38.0); servo11.attach(mega, 11, 38.0);
       * servo12.attach(mega, 12, 38.0);
       */

      // TestCatcher catcher = (TestCatcher)Runtime.start("catcher",
      // "TestCatcher");
      // servo03.attach((ServoDataListener)catcher);

      // servo.setPin(12);

      /*
       * servo.attach(mega, 7, 38.0); servo.attach(mega, 7, 38.0);
       * servo.attach(mega, 7, 38.0); servo.attach(mega, 7, 38.0);
       * servo.attach(mega, 7, 38.0); servo.attach(mega, 7, 38.0);
       * servo.attach(mega, 7, 38.0); servo.attach(mega, 7, 38.0);
       * servo.attach(mega, 7, 38.0); servo.attach(mega, 7, 38.0);
       * servo.attach(mega, 7, 38.0); servo.attach(mega, 7, 38.0);
       */

      // servo.sweepDelay = 3;
      // servo.save();
      // servo.load();
      // servo.save();
      // log.info("sweepDely {}", servo.sweepDelay);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
  
  // FIXME - should be renamed - autoDisableDefault
  // FIXME - setAutoDisableDefault should be used and this should be protected
  static public boolean autoDisableDefault = false;
  
  @Deprecated /* use setAutoDisableDefault */
  static public boolean enableAutoDisable(boolean b) {
    autoDisableDefault = b;
    return b;
  }
  
  static public boolean setAutoDisableDefault(boolean b) {
    autoDisableDefault = b;
    return b;
  }

  /**
   * The automatic disabling of the servo in idleTimeout ms This de-energizes
   * the servo
   * 
   * FIXME - poorly named enableAutoDisable
   */
  protected Boolean autoDisable = autoDisableDefault;

  /**
   * set of currently subscribed servo controllers
   */
  protected Set<String> controllers = new TreeSet<>();

  /**
   * the "current" position of the servo - this never gets updated from
   * "command" methods such as moveTo - its always status information, and its
   * typically updated from an encoder of some form
   */
  protected Double currentPos;

  /**
   * set by encoders
   */
  protected Double currentPosInput = null;

  /**
   * if enabled then a pwm pulse is keeping the servo at the current position,
   * and movements are possible
   */
  protected boolean enabled = false;

  /**
   * The servos encoder - by "default" this will be a TimerEncoder - where a
   * timer calculates the expected time the servo will make and complete
   * movements, and some configured division of time is configured to send
   * position update events.
   * 
   * Unlike previous servo events which dependend on feedback coming from
   * Arduino, TimerEncoder events do not need an Arduino, in fact they don't
   * even need anything as computers are pretty good at counting time.
   * 
   * Arduino MrlEncoders (legacy) could potentially be substituted for a
   * TimerEncoder, however there is
   * 
   */
  protected transient EncoderControl encoder; // this does not need to be
                                              // transient in the future

  /**
   * If the servo was disabled through an idle-timeout. If the servo is disabled
   * through an idle-timeout, it can be re-enabled on next move. If the servo
   * was disabled through a human or event which "manually" disabled the servo,
   * the servo SHOULD NOT be enabled next move - this is an internal field
   */
  protected boolean idleDisabled = false;

  /**
   * if autoDisable is true - then after any move a timer is set to disable the
   * servo. if the servo is idle for any length of time after
   */
  int idleTimeout = 3000;

  /**
   * if the servo is doing a blocking call - it will block other blocking calls
   * until the move it complete or a timeout has been reached. A "moveTo"
   * command will be canceled (not blocked) when isBlocking == true
   */
  protected boolean isBlocking = false;

  /**
   * status only field - updated by encoder
   */
  boolean isMoving = false;

  /**
   * controls if the servo is sweeping
   */
  protected boolean isSweeping = false;

  /**
   * last time the servo has moved
   */
  protected long lastActivityTimeTs = 0;

  /**
   * input mapper
   */
  protected Mapper mapper = new MapperLinear(0, 180, 0, 180);

  /**
   * maximum speed default is 500 degrees per second or operating speed of 60
   * degrees in 0.12 seconds
   */
  protected Double maxSpeed = 500.0;

  /**
   * the 'pin' for this Servo - it is Integer because it can be in a state of
   * 'not set' or null.
   * 
   * pin is the ONLY value which cannot and will not be 'defaulted'
   */
  protected String pin;

  /**
   * if the servo uses a internal timer encoder - this will allow encoder data
   * to be published
   */
  protected boolean publishEncoderData;

  /**
   * default rest is 90 default target position will be 90 if not specified
   */
  protected Double rest = 90.0;

  /**
   * speed of the servo - null is no speed control
   */
  protected Double speed = null;

  /**
   * direction of sweep
   */
  protected boolean sweepingToMax = true;

  /**
   * max sweep value
   */
  protected Double sweepMax = null;

  /**
   * min sweep value
   */
  protected Double sweepMin = null;

  /**
   * synchronized servos - when this one moves, it sends move commands to these
   * servos
   */
  protected Set<String> syncedServos = new LinkedHashSet<>();

  /**
   * the calculated output for the servo
   */
  protected Double targetOutput;

  /**
   * the requested INPUT position of the servo
   */
  protected Double targetPos;

  /**
   * if true - a single moveTo command will be published for servo controllers
   * or other services which implement their own speed contrl
   * 
   * if false - many moveTo commands will be published by TimeEncoder to
   * provided speed control using incremental moves at appropriate times to
   * approximate appropriate speed
   * 
   * defaulted to true - here is a list of controllers which provide their own
   * speed control
   * 
   * * Arduino/MrlComm * Adafruit16CServoController * JMonkeyEngine /
   * Interpolator
   * 
   */
  boolean useServoControllerSpeedControl = true;

  /**
   * weather a move request was successful. The cases it would be false is no
   * controller or calling moveTo when blocking is in process
   */
  boolean validMoveRequest = false;

  public AbstractServo(String n, String id) {
    super(n, id);

    // this servo is interested in new services which support either
    // ServoControllers or EncoderControl interfaces
    // we subscribe to runtime here for new services
    subscribeToRuntime("registered");

    /*
     * // new feature - // extracting the currentPos from serialized servo
     * Double lastCurrentPos = null; try { lastCurrentPos = (Double)
     * loadField("currentPos"); } catch (IOException e) {
     * log.info("current pos cannot be found in saved file"); }
     */

    // if no position could be loaded - set to rest
    // we have no "historical" info - assume we are @ rest
    currentPos = targetPos = rest;
    
    mapper.setMinMax(0, 180);

    // create our default TimeEncoder
    if (encoder == null) {
      encoder = new TimeEncoder(this);
      // if the encoder has a current value - we initialize the
      // servo with that value
      Double savedPos = encoder.getPos();
      if (savedPos != null) {
        log.info("found previous values for {} setting initial position to {}", getName(), savedPos);
        currentPos = targetPos = savedPos;
      }
    }
  }

  /**
   * overloaded routing attach
   */
  public void attach(Attachable service) throws Exception {
    if (ServoController.class.isAssignableFrom(service.getClass())) {
      attach((ServoController) service, null, null, null);
    } else if (EncoderControl.class.isAssignableFrom(service.getClass())) {
      attach((EncoderControl) service);
    } else {
      warn(String.format("%s.attach does not know how to attach to a %s", this.getClass().getSimpleName(), service.getClass().getSimpleName()));
    }
  }

  /**
   * max complexity - minimal parameter EncoderControl attach
   * 
   * @param enc
   * @throws Exception
   */
  public void attach(EncoderControl enc) throws Exception {
    if (enc == null) {
      log.warn("encoder is null");
      return;
    }
    if (enc.equals(encoder)) {
      log.info("encoder {} already attached", enc.getName());
      return;
    }
    encoder = enc;
    enc.attach(this);
    broadcastState();
  }

  public void attach(ServoController sc) {
    attach(sc, null, null, null);
  }

  public void attach(ServoController sc, Integer pin) {
    attachServoController(sc.getName(), pin, null, null);
  }

  public void attach(ServoController sc, Integer pin, Double pos) {
    attachServoController(sc.getName(), pin, pos, null);
  }

  public void attach(ServoController sc, Integer pin, Double pos, Double speed) {
    attachServoController(sc.getName(), pin, pos, speed);
  }

  public void attach(String sc) throws Exception {
    attachServoController(sc, null, null, null);
  }

  // @Override
  public void attach(String controllerName, Integer pin) {
    attach(controllerName, pin, null);
  }

  // @Override
  public void attach(String controllerName, Integer pin, Double pos) {
    attach(controllerName, pin, pos, null);
  }

  // @Override
  // FIXME - decide how attach will work or wont with extra parameters
  public void attach(String controllerName, Integer pin, Double pos, Double speed) {
    try {
      setPin(pin);
      setPosition(pos);
      setSpeed(speed);
      attach(controllerName);
    } catch (Exception e) {
      error(e);
    }
  }

  /**
   * maximum complexity attach with reference to controller FIXME - max
   * complexity service should use NAME not a direct reference to
   * ServoController !!!!
   */
  public void attachServoController(String sc, Integer pin, Double pos, Double speed) {

    if (controllers.contains(sc)) {
      log.info("{} already attached", sc);
      return;
    }
    
    // update pin if non-null value supplied
    if (pin != null) {
      setPin(pin);
    }

    // update pos if non-null value supplied
    if (pos != null) {
      targetPos = pos;
    }

    // update speed if non-null value supplied
    if (speed != null) {
      setSpeed(speed);
    }

    // the subscribes .... or addListeners in this case ...
    addListener("publishServoMoveTo", sc);
    addListener("publishServoStop", sc);
    // addListener("publishServoStopped", sc);
    addListener("publishServoWriteMicroseconds", sc);
    addListener("publishServoSetSpeed", sc);
    addListener("publishServoEnable", sc);
    addListener("publishServoDisable", sc);

    controllers.add(sc);
    enabled = true; // <-- how to deal with this ? "real" controllers usually
                    // need an enable/energize command
    
    // FIXME sc NEEDS TO BE FULL NAME !!!
    send(sc, "attachServoControl", this);

    broadcastState();
  }

  /**
   * disables and detaches from all controllers
   */
  public void detach() {
    disable();
    Set<String> copy = new HashSet<>(controllers);
    for (String sc : copy) {
      detach(sc);
    }
  }

  @Override
  public void detach(Attachable service) {
    detach(service.getName());
  }

  public void detach(ServoController sc) {
    detach(sc.getName());
    broadcastState();
  }

  public void detach(String sc) {
    
    if (!controllers.contains(sc)) {
      log.info("{} already detached from {}", getName(), sc);
      return;
    }

    // the subscribes .... or addListeners in this case ...
    removeListener("publishServoMoveTo", sc);
    removeListener("publishServoStop", sc);
    // removeListener("publishServoStopped", sc);
    removeListener("publishServoWriteMicroseconds", sc);
    removeListener("publishServoSetSpeed", sc);
    removeListener("publishServoEnable", sc);
    removeListener("publishServoDisable", sc);

    controllers.remove(sc);
    
    disable();
   
    send(sc, "detach", getName());
    sleep(500);
    
    broadcastState();
  }

  @Override
  public void disable() {
    enabled = false;
    invoke("publishServoDisable", this);
    broadcastState();
  }

  @Override
  public void enable() {
    enabled = true;
    invoke("publishServoEnable", this);
    broadcastState();
  }

  public void fullSpeed() {
    log.info("disabling speed control");
    speed = null;
    invoke("publishServoSetSpeed", this);
    broadcastState();
  }

  @Override
  public boolean getAutoDisable() {
    return autoDisable;
  }

  @Override
  public Set<String> getControllers() {
    return controllers;
  }

  @Override
  public EncoderControl getEncoder() {
    return encoder;
  }

  @Override
  public long getLastActivityTime() {
    return lastActivityTimeTs;
  }

  @Override
  public Mapper getMapper() {
    return mapper;
  }

  @Override
  public Double getMax() {
    return mapper.getMax();
  }

  @Override
  public Double getMaxSpeed() {
    return maxSpeed;
  }

  @Override
  public Double getMin() {
    return mapper.getMin();
  }

  @Override
  public String getPin() {
    return pin;
  }

  /**
   * this value is always only set by an encoder - if available
   */
  @Override
  public Double getPos() {
    return currentPos;
  }

  @Override
  public Double getRest() {
    return rest;
  }

  @Override
  public Double getSpeed() {
    return speed;
  }

  @Override
  public Double getTargetOutput() {
    if (targetPos == null) {
      targetPos = rest;
    }
    targetOutput = mapper.calcOutput(targetPos);
    return targetOutput;
  }

  @Override
  public Double getTargetPos() {
    return targetPos;
  }

  @Deprecated /* this is really speed - velocity is a vector */
  @Override
  public Double getVelocity() {
    return speed;
  }

  /**
   * a method called by the idle timer - we will know that this disable is
   * allowed to re-enable
   */
  public void idleDisable() {
    idleDisabled = true;
    disable();
  }

  public boolean isAttached(Attachable attachable) {
    return controllers.contains(attachable.getName());
  }

  public boolean isAttached(String name) {
    return controllers.contains(name);
  }

  @Override
  public boolean isBlocking() {
    return isBlocking;
  }

  @Override
  public Boolean isEnabled() {
    return enabled;
  }

  @Override
  public Boolean isInverted() {
    return mapper.isInverted();
  }

  @Override
  public boolean isMoving() {
    return isMoving;
  }

  public boolean isSweeping() {
    return isSweeping;
  }

  @Override
  public void map(Double minX, Double maxX, Double minY, Double maxY) {
    mapper = new MapperLinear(minX, maxX, minY, maxY);
    broadcastState();
  }

  /**
   * formula for calculating the position from microseconds to degrees
   * 
   * @param microseconds
   * @return
   */
  public double microsecondsToDegree(double microseconds) {
    if (microseconds <= 180)
      return microseconds;
    return (double) (microseconds - 544) * 180 / (2400 - 544);
  }

  @Override
  public boolean moveTo(Double newPos) {
    processMove(newPos, false, null);
    return validMoveRequest;
  }

  @Override
  public Double moveToBlocking(Double pos) {
    processMove(pos, true, null);
    return currentPos; // should be requested pos - unless timeout occured
  }

  @Override
  public Double moveToBlocking(Double newPos, Long timeoutMs) {
    processMove(newPos, true, timeoutMs);
    return currentPos; // should be requested pos - unless timeout occured
  }

  @Override
  public void onEncoderData(EncoderData data) {
    // log.info("onEncoderData - {}", data.value); - helpful to debug
    currentPos = data.value;
    // TODO test on type of encoder to handle differently if necessary
    // TODO - where does resolution or accuracy managed ? (in the encoder or in
    // the motor ?)
    // FIXME - configurable accuracy difference ? ie - when your in the range of
    // 0.02 - then they are considered equal ?
    int t = targetPos.intValue();
    int c = currentPos.intValue();

    boolean equal = Math.abs(targetPos - currentPos) < 0.1;
    // if (targetPos.equals(currentPos)) {
    if (equal) {
      synchronized (this) {
        this.notifyAll();
      }
      isMoving = false;

      if (publishEncoderData) {
        invoke("publishEncoderData", data);
      }

      // FIXME - these should be Deprecated - and publishEncoderData used if
      // necessary
      invoke("publishServoData", ServoStatus.SERVO_STOPPED, currentPos);
      invoke("publishServoStopped", this);

      // new feature - saving the last position
      // save(); <-- BAD !!

      // servo has stopped its move ... (or best guess estimate it has)
      // if currently configured to autoDisable - the timer starts now
      if (autoDisable) {
        // we cancel any pre-existing timer if it exists
        purgeTask("idleDisable");
        // and start our countdown
        addTaskOneShot(idleTimeout, "idleDisable");
      }
      // log.info("encoder data says -> stopped");
    } else {
      isMoving = true;
      if (useServoControllerSpeedControl) {
        invoke("publishMoveTo", this);
      }
      // FIXME - is this necessary ?
      invoke("publishServoData", ServoStatus.SERVO_POSITION_UPDATE, currentPos);
      // log.info("encoder data says -> moving {} {}", currentPos, targetPos);
    }
  }

  public void onRegistered(Registration s) {
    refreshControllers();
    broadcastState();
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
  protected void processMove(Double newPos, boolean blocking, Long timeoutMs) {
    // FIXME - implement encoder blocking ...
    // FIXME - when and what should a servo publish and when ?
    // FIXME FIXME FIXME !!!! @*@*!!! - currentPos is the reported position of
    // the servo, targetPos is
    // the desired position of the servo - currentPos should NEVER be set in
    // this function
    // even with no hardware encoder a servo can have a TimeEncoder from which
    // position would be guessed - but
    // it would not be "set" here !

    // breakMoveToBlocking=true;
    // synchronized (moveToBlocked) {
    // moveToBlocked.notify(); // Will wake up MoveToBlocked.wait()
    // }

    // enableAutoEnable is "always" on - if you want to stop a motor from
    // working use .lock()
    // which is part of the motor command set ... once you lock a motor you
    // can't do anything until you unlock it

    if (newPos == null) {
      log.info("{} processing a null move - will not move", getName());
      return;
    }

    if (idleDisabled && !enabled) {
      // if the servo was disable with a timer - re-enable it
      enable();
    }

    // purge any timers currently in process
    purgeTask("idleDisable");

    if (!enabled) {
      log.info("cannot moveTo {} not enabled", getName());
      validMoveRequest = false;
      return;
    }

    targetPos = mapper.calcOutput(newPos);

    // work on confused info below
    if (!isEnabled() && autoDisable) { // FIXME - still not right - need to know
                                       // if this servo was disabled through
                                       // timer or not
      // if (newPos != lastPos || !getAutoDisable()) {
      if (targetPos != currentPos || !isEnabled()) {
        enable();
      }
    }

    /*
     * if (currentPos != targetPos) {
     * log.info("{} command to move {} but already there", getName(),
     * targetPos); return; }
     */

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

    if (isBlocking && !blocking) {
      // if isBlocking already, and incoming request is not blocking - we cancel
      log.info("{} is currently blocking - ignoring request to moveTo({})", getName(), newPos);
      validMoveRequest = false;
      return;
    }

    if (isBlocking && blocking) {
      // if isBlocking already, and incoming request is a blocking one - we
      // block it
      log.info("{} is currently blocking - request to moveToBlocking({}) will need to wait", getName(), newPos);
      synchronized (this) {
        try {
          this.wait();
        } catch (InterruptedException e) {
          /* don't care */}
      }
      return;
    }

    if (!isBlocking && blocking) {
      // if not currently blocking, and incoming request is blocking - we start
      // blocking with default encoder until it - unblocks or max-timeout is
      // reached - if timeout not specified - we block until an encoder unblocks
      // us
      log.info("{} is currently blocking - request to moveToBlocking({}) will need to wait", getName(), newPos);
      isBlocking = true;
    }

    targetOutput = getTargetOutput();
    lastActivityTimeTs = System.currentTimeMillis();

    isMoving = true;
    validMoveRequest = true;

    // "real" encoders are electrically hooked up to the servo and get their
    // events through
    // data lines - faux encoders need to be told in software when servos begin
    // movement
    // usually knowing about encoder type is "bad" but the timer encoder is the
    // default native encoder
    Long blockingTimeMs = null;
    if (encoder != null && encoder instanceof TimeEncoder) {
      TimeEncoder timeEncoder = (TimeEncoder) encoder;
      // calculate trajectory calculates and processes this move
      blockingTimeMs = timeEncoder.calculateTrajectory(getPos(), getTargetPos(), getSpeed());
    }

    invoke("publishServoMoveTo", this);
    broadcastState();

    if (isBlocking) {
      // our thread did a blocking call - we will wait until encoder notifies us
      // to continue or timeout (if supplied) has been reached
      sleep(blockingTimeMs);
      isBlocking = false;
    }
    return;
  }

  /**
   * Servo has the ability to act as an encoder if it is using TimeEncoder.
   * TimeEncoder will use Servo to publish a series of encoder events with
   * estimated trajectory
   */
  @Override
  public EncoderData publishEncoderData(EncoderData data) {
    return data;
  }

  /**
   * moveTo requests are published through this publishing point
   */
  public ServoControl publishMoveTo(ServoControl sc) {
    return sc;
  }

  @Override
  @Deprecated /*
               * not used nor wanted - subscribers should be either
               * EncoderListener or ServoController
               */
  public ServoData publishServoData(ServoStatus status, Double currentPosUs) {
    double pos = microsecondsToDegree(currentPosUs);
    ServoData sd = new ServoData(status, getName(), pos);
    lastActivityTimeTs = System.currentTimeMillis();
    // log.debug("status {} pos {}", status, pos);

    if (isSweeping) {
      if (sweepingToMax && pos >= sweepMax - 1) { // handle overshoot ?
        sweepingToMax = false;
        moveTo(sweepMin);
      }
      if (!sweepingToMax && pos <= sweepMin + 1) { // handle overshoot ?
        sweepingToMax = true;
        moveTo(sweepMax);
      }
    }

    return sd;
  }

  @Override
  public ServoControl publishServoDisable(ServoControl sc) {
    return sc;
  }

  @Override
  public ServoControl publishServoEnable(ServoControl sc) {
    return sc;
  }

  /**
   * BEHOLD THE NEW PUBLISHING INTERFACE POINTS !!!!! Subscribers can now
   * subscribe to servo command events, which allows the ability for the
   * framework to take care of all the details of multiple consumers/controllers
   */

  @Override
  public ServoControl publishServoMoveTo(ServoControl sc) {
    return sc;
  }

  @Override
  public ServoControl publishServoSetSpeed(ServoControl sc) {
    return sc;
  }

  @Override
  public ServoControl publishServoStop(ServoControl sc) {
    return sc;
  }

  @Override
  public ServoControl publishServoStopped(ServoControl sc) {
    return sc;
  }

  public List<String> refreshControllers() {
    List<String> cs = Runtime.getServiceNamesFromInterface(ServoController.class);
    return cs;
  }

  /**
   * will disable then detach this servo from all controllers
   */
  public void releaseService() {    
    if (encoder != null) {
      encoder.disable();
    }
    detach();
    super.releaseService();
  }

  @Override
  public void rest() {
    log.info("here");
    targetPos = rest;
    moveTo(rest);
  }

  /**
   * Auto disable automatically disables the servo stopping the power to it
   * after an idleTimeout time if no move command was sent. After a move it will
   * begin at the "end" of the movement.
   */
  @Override
  public void setAutoDisable(Boolean autoDisable) {

    boolean valueChanged = !this.autoDisable.equals(autoDisable);

    this.autoDisable = autoDisable;

    if (autoDisable) {
      // FIXME - will need to know if disabled manually (by user) or by timer
      // (re-enable-able with move)
      addTaskOneShot(idleTimeout, "idleDisable");
    } else {
      purgeTask("idleDisable");
      idleDisabled = false;
    }

    if (valueChanged) {
      broadcastState();
    }
  }

  public int setIdleTimeout(int idleTimeout) {
    this.idleTimeout = idleTimeout;
    broadcastState();
    return idleTimeout;
  }

  @Override
  public void setInverted(Boolean invert) {
    mapper.setInverted(invert);
    broadcastState();
  }

  @Override
  public void setMapper(Mapper mapper) {
    this.mapper = mapper;
    broadcastState();
  }

  @Override
  public void setMaxSpeed(Double maxSpeed) {
    this.maxSpeed = maxSpeed;
    broadcastState();
  }

  /* decide on one deprecate the other ! */
  @Deprecated
  public void setMaxVelocity(Double maxSpeed) {
    setMaxSpeed(maxSpeed);
  }

  @Override
  // SEE - http://myrobotlab.org/content/servo-limits
  public void setMinMax(Double min, Double max) {
    mapper.setMinMax(min, max);
    broadcastState();
  }

  @Override
  @Config // default - if pin is different - output servo.setPin()
  public void setPin(Integer pin) {
    if (pin == null) {
      log.info("{}.setPin(null) as pin is not a valid pin value", pin);
      return;
    }
    setPin(pin + "");
  }

  /**
   * pin is a string value at its core "address" is an int or long
   */
  @Override
  public void setPin(String pin) {
    if (pin == null) {
      log.info("{}.setPin(null) as pin is not a valid pin value", pin);
      return;
    }
    this.pin = pin;
    broadcastState();
  }

  @Override
  public void setPosition(Double pos) {
    currentPos = targetPos = pos;
    if (encoder != null) {
      if (encoder instanceof TimeEncoder)
        ((TimeEncoder) encoder).setPos(pos);
    }
    broadcastState();
  }

  @Override
  public void setRest(Double rest) {
    this.rest = rest;
    broadcastState();
  }

  @Override
  @Config
  public void setSpeed(Double degreesPerSecond) {
    if (degreesPerSecond == null) {
      fullSpeed();
      return;
    }

    if (maxSpeed != -1 && degreesPerSecond != null && degreesPerSecond > maxSpeed) {
      speed = maxSpeed;
      log.info("Trying to set speed to a value greater than max speed");
    }
    speed = degreesPerSecond;
    invoke("publishServoSetSpeed", this);
    broadcastState();
  }

  @Deprecated /* this is really speed not velocity, velocity is a vector */
  @Override
  public void setVelocity(Double degreesPerSecond) {
    setSpeed(degreesPerSecond);
  }

  // FIXME targetPos = pos, reportedSpeed, vs speed - set
  @Override
  public void stop() {
    isSweeping = false;
    // FIXME - figure out the appropriate thing to do for a TimeEncoder ????
    processMove(getPos(), false, null);
    invoke("publishServoStop", this);
    broadcastState();
  }

  /**
   * disable servo
   */
  public void stopService() {
    super.stopService();
    disable();

    // not happy - too type specific
    if (encoder != null) {
      encoder.disable();
    }
  }

  public void sweep() {
    sweep(null, null);
  }

  public void sweep(Double min, Double max) {
    sweep(min, max, null);
  }

  public void sweep(Double min, Double max, Double speed) {
    if (min == null) {
      sweepMin = mapper.getMin();
    } else {
      sweepMin = min;
    }

    if (max == null) {
      sweepMax = mapper.getMax();
    } else {
      sweepMax = max;
    }

    if (speed != null) {
      setSpeed(speed);
    }

    isSweeping = true;
    sweepingToMax = false;
    moveTo(sweepMin);
  }

  @Override
  public void sync(ServoControl sc) {
    if (sc == null) {
      log.error("{}.sync(null)", getName());
    }
    syncedServos.add(sc.getName());
  }

  @Override
  public void unsetSpeed() {
    speed = null;
    
  }

  @Override
  public void unsync(ServoControl sc) {
    if (sc == null) {
      log.error("{}.unsync(null)", getName());
    }
    syncedServos.remove(sc.getName());
  }

  
  @Override
  public void waitTargetPos() {
    //
    // while (this.pos != this.targetPos) {
    // Some sleep perhaps?
    // TODO:
    // }

  }

  
  public void writeMicroseconds(int uS) {
    invoke("publishServoWriteMicroseconds", this, uS);
  }

}
