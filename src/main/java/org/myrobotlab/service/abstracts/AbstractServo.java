package org.myrobotlab.service.abstracts;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.Config;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.sensor.EncoderPublisher;
import org.myrobotlab.sensor.TimeEncoder;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.data.AngleData;
import org.myrobotlab.service.data.ServoMove;
import org.myrobotlab.service.data.ServoSpeed;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoControlPublisher;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.ServoEvent;
import org.myrobotlab.service.interfaces.ServoStatusPublisher;
import org.slf4j.Logger;

/**
 * 
 * @author GroG, and many others.
 * 
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
 *         TODO - make a publishing interface which publishes "CONTROL" angles
 *         vs status of angles
 *
 */
public abstract class AbstractServo extends Service implements ServoControl, ServoControlPublisher, ServoStatusPublisher, EncoderPublisher {

  public final static Logger log = LoggerFactory.getLogger(AbstractServo.class);

  private static final long serialVersionUID = 1L;

  /**
   * The automatic disabling of the servo in idleTimeout ms This de-energizes
   * the servo. By default this is disabled.
   * 
   */
  protected boolean autoDisable = false;

  /**
   * The current servo controller that this servo is attached to. TODO: move
   * this to Servo.java , DiyServo doesn't care about this detail.
   */
  protected String controller;

  /**
   * This allows the servo to attach disabled, and only energize after the first
   * moveTo command is processed
   */
  protected boolean firstMove = true;

  /**
   * the "current" INPUT position of the servo - this never gets updated from
   * "command" methods such as moveTo - its always status information, and its
   * typically updated from an encoder of some form
   */
  protected double currentInputPos;

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
   * if autoDisable is true - then after any move a timer is set to disable the
   * servo. if the servo is idle for any length of time after Default timeout is
   * 3000 milliseconds
   * 
   */
  protected int idleTimeout = 3000;

  /**
   * if the servo is doing a blocking call - it will block other blocking calls
   * until the move it complete or a timeout has been reached. A "moveTo"
   * command will be canceled (not blocked) when isBlocking == true
   */
  protected boolean isBlocking = false;

  /**
   * status only field - updated by encoder
   */
  protected boolean isMoving = false;

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
  protected MapperLinear mapper = new MapperLinear(0, 180, 0, 180);

  /**
   * the 'pin' for this Servo - it is Integer because it can be in a state of
   * 'not set' or null.
   * 
   * pin is the ONLY value which cannot and will not be 'defaulted'
   */
  protected String pin;

  /**
   * default rest is 90 default target position will be 90 if not specified
   */
  protected double rest = 90.0;

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
   * the requested INPUT position of the servo
   */
  protected double targetPos;

  protected double actualAngleDeltaError = 0.1;

  /**
   * load the last position the servo was in in general I think this is a bad
   * idea - TimeEncoder does this - but I don't think its desired in general
   */
  protected boolean loadSavedPositions = false;

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
   * @param n
   *          the name
   * @param id
   *          the instance id
   * 
   */

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
    targetPos = rest;

    // TODO: this value is default already.
    // mapper.setMinMax(0, 180);
    // create our default TimeEncoder
    if (encoder == null) {
      encoder = new TimeEncoder(this);
      // if the encoder has a current value - we initialize the
      // servo with that value
      Double savedPos = encoder.getPos();
      if (savedPos != null && loadSavedPositions) {
        log.info("found previous values for {} setting initial position to {}", getName(), savedPos);
        // TODO: kw: output position shouldn't be set to the targetPos..
        currentInputPos = targetPos = savedPos;
      }
    }
    // currentInputP = mapper.calcOutput(targetPos); FIXME - FIXED - encoder now
    // publishing input not output
  }

  /**
   * if a new service is added to the system refresh the controllers
   */
  @Deprecated /*
               * lifecycle events not necessary for ui, probably should be
               * pulled out
               */
  public void onStarted(String name) {
    invoke("refreshControllers");
  }

  /**
   * overloaded routing attach
   */
  @Override
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
   *          the encoder
   * @throws Exception
   *           boom
   * 
   */
  public void attach(EncoderControl enc) throws Exception {
    if (enc == null) {
      log.warn("encoder is null");
      return;
    }
    if (enc.equals(encoder)) {
      log.info("encoder {} already attached", enc.getName());
    }
    encoder = enc;
    enc.attach(this);
    broadcastState();
  }

  @Override
  public void attach(ServoController sc) {
    attach(sc, null, null, null);
  }

  @Deprecated /* setPin then attach(String) */
  public void attach(ServoController sc, Integer pin) {
    attachServoController(sc.getName(), pin, null, null);
  }

  public void attach(ServoController sc, Integer pin, Double pos) {
    attachServoController(sc.getName(), pin, pos, null);
  }

  @Deprecated /* setPin setPos setSpeed then attach(String) */
  public void attach(ServoController sc, Integer pin, Double pos, Double speed) {
    attachServoController(sc.getName(), pin, pos, speed);
  }

  @Override
  public void attach(String sc) throws Exception {
    attachServoController(sc, null, null, null);
  }

  @Deprecated /* setPin then attach(String) */
  public void attach(String controllerName, Integer pin) {
    attach(controllerName, pin, null);
  }

  @Deprecated /* setPin setPos then attach(String) */
  public void attach(String controllerName, Integer pin, Double pos) {
    attach(controllerName, pin, pos, null);
  }

  @Deprecated
  /*
   * Servos Do Not publish Joint Angles - they only publish their position !
   */
  public AngleData publishJointAngle(AngleData angle) {
    log.debug("{}.publishJointAngle({})", getName(), angle);
    return angle;
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
  @Override
  public void attachServoController(String sc, Integer pin, Double pos, Double speed) {
    if (controller != null && controller.equals(sc)) {
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
    addListener("publishServoWriteMicroseconds", sc);
    addListener("publishServoSetSpeed", sc);
    addListener("publishServoEnable", sc);
    addListener("publishServoDisable", sc);
    controller = sc;

    ServoController servoController = (ServoController) Runtime.getService(sc);
    if (servoController != null) {
      servoController.attachServoControl(this);
    }
    // FIXME - remove !!!
    // FIXME change to broadcast ?
    // TODO: there is a race condition here.. we need to know that
    // the servo control ackowledged this.
    // try {
    // sendBlocking(sc, "attachServoControl", this); // <-- change to broadcast
    // ?
    // } catch (Exception e) {
    // log.error("sendBlocking attachServoControl threw", e);
    // }
    // TOOD: we need to wait here for the servo controller to acknowledge that
    // it was attached.

    broadcastState();
  }

  /**
   * disables and detaches from all controllers
   */
  @Override
  public void detach() {
    detach(controller);
  }

  @Override
  public void detach(Attachable service) {
    detach(service.getName());
  }

  @Override
  public void detach(ServoController sc) {
    detach(sc.getName());
  }

  // AbstractServo -
  @Override
  public void detach(String controllerName) {
    if (controller == null) {
      log.info("already detached");
      return;
    }

    if (controller != null && !controller.equals(controllerName)) {
      log.warn("{} not attached to {}", getName(), controllerName);
      return;
    }

    disable();

    // the subscribes .... or addListeners in this case ...
    removeListener("publishServoMoveTo", controllerName);
    removeListener("publishServoStop", controllerName);
    removeListener("publishServoWriteMicroseconds", controllerName);
    removeListener("publishServoSetSpeed", controllerName);
    removeListener("publishServoEnable", controllerName);
    removeListener("publishServoDisable", controllerName);
    controller = null;
    // 20210703 - grog I don't know why a sleep was put here
    // junit ServoTest will fail without this :P
    // sleep(500);
    firstMove = true;

    send(controllerName, "detach", getName());
    broadcastState();
  }

  @Override
  public void disable() {
    stop();
    enabled = false;
    broadcast("publishServoDisable", (ServoControl) this);
  }

  @Override
  public void enable() {

    if (autoDisable) {
      if (!isMoving) {
        // not moving - safe & expected to put in a disable
        purgeTask("disable");
        addTaskOneShot(idleTimeout, "disable");
      }
    }

    enabled = true;
    broadcast("publishServoEnable", this);
    // broadcastState();
  }

  @Override
  @Deprecated /* use setMaxSpeed */
  public void fullSpeed() {
    setSpeed((Double) null);
  }

  @Override
  public boolean isAutoDisable() {
    return autoDisable;
  }

  @Override
  public String getController() {
    return controller;
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
  public double getMax() {
    return mapper.getMaxY();
  }

  @Override
  public double getMin() {
    return mapper.getMinY();
  }

  @Override
  public String getPin() {
    return pin;
  }

  /**
   * Returns the current position of the servo as computed/updated by the
   * encoder
   */
  @Override
  public double getCurrentInputPos() {
    // return mapper.calcInput(currentInputP);
    return currentInputPos;
  }

  @Override
  public double getCurrentOutputPos() {
    return mapper.calcOutput(currentInputPos);
  }

  @Override
  public double getRest() {
    return rest;
  }

  @Override
  public Double getSpeed() {
    return speed;
  }

  @Override
  public double getTargetOutput() {
    return mapper.calcOutput(targetPos);
  }

  @Override
  public double getTargetPos() {
    return targetPos;
  }

  @Deprecated /* use getSpeed() */
  public Double getVelocity() {
    return speed;
  }

  @Override
  public boolean isAttached(Attachable attachable) {
    return controller != null && controller.equals(attachable.getName());
  }

  @Override
  public boolean isAttached(String name) {
    return controller != null && controller.equals(name);
  }

  @Override
  public boolean isBlocking() {
    return isBlocking;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public boolean isInverted() {
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
  public void map(double minX, double maxX, double minY, double maxY) {
    mapper.map(minX, maxX, minY, maxY);
    // broadcastState();
  }

  /**
   * formula for calculating the position from microseconds to degrees
   * 
   * @param microseconds
   *          ms to convert
   * @return the degrees converted
   * 
   */
  public static double microsecondsToDegree(double microseconds) {
    if (microseconds <= 180)
      return microseconds;
    return (microseconds - 544) * 180 / (2400 - 544);
  }

  @Override
  public Double moveTo(Double newPos) {
    /**
     * weather a move request was successful. The cases it would be false is no
     * controller or calling moveTo when blocking is in process
     */

    if (newPos == null) {
      log.info("will not move to null position - not moving");
      return newPos;
    }

    processMove(newPos, false, null);
    return newPos;
  }

  /**
   * incrementally move - takes the servo's current position (could be in the
   * middle of a move) adds the increment and tells the servo to go to the new
   * location. Returns the new location if the moveTo was successfully started -
   * otherwise returns null
   * 
   * @param increment
   * @return
   */
  public Double moveIncr(Double increment) {
    if (increment == null) {
      log.info("will not move to null position - not moving");
      return null;
    }
    double newPos = getCurrentInputPos() + increment;
    if (processMove(newPos, false, null)) {
      return newPos;
    }
    return null;
  }

  @Override
  public Double moveToBlocking(Integer pos) {

    try {
      return moveToBlocking((double) pos, null);
    } catch (Exception e) {
      log.error("moveToBlocking threw", e);
    }

    return null;
  }

  @Override
  public Double moveToBlocking(Double pos) {
    return moveToBlocking(pos, null);
  }

  @Override
  public Double moveToBlocking(Integer newPos, Long timeoutMs) {
    if (newPos != null) {
      return moveToBlocking(newPos.doubleValue(), timeoutMs);
    } else
      return null;
  }

  @Override
  public Double moveToBlocking(Double newPos, Long timeoutMs) {
    processMove(newPos, true, timeoutMs);
    return currentInputPos;
  }

  @Override
  public void onEncoderData(EncoderData data) {
    // log.debug("data {}", data);
    // log.info("onEncoderData - {}", data.value); - helpful to debug
    // currentInputP = data.angle;
    currentInputPos = data.angle; // mapper.calcInput(currentInputP);
    data.mappedValue = mapper.calcOutput(data.angle);

    // assuming this came from TimeEncoder - we re-calculate input and then
    // publish it
    data.value = currentInputPos;
    broadcast("publishEncoderData", data);
    // invoke("publishEncoderData", data);
    // log.error("bpublishEncoderData {}", data.value);

    boolean equal = Math.abs(targetPos - currentInputPos) < actualAngleDeltaError;

    // FIXME - fix blocking - determine when publishJointAngle should be
    // published
    if (equal) {
      broadcast("publishJointAngle", new AngleData(getName(), data.angle));
      // broadcast("publishServoEvent", ServoStatus.SERVO_STOPPED,
      // currentInputPos);

    }
  }

  public void onRegistered(Registration s) {
    refreshControllers();
  }

  /**
   * Servo has the ability to act as an encoder if it is using TimeEncoder.
   * TimeEncoder will use Servo to publish a series of encoder events with
   * estimated trajectory
   */
  @Override
  public EncoderData publishEncoderData(EncoderData data) {
    log.debug("{}.publishEncoderData({})", getName(), data);
    return data;
  }

  /**
   * moveTo requests are published through this publishing point
   */
  @Override
  public ServoControl publishMoveTo(ServoControl sc) {
    log.debug("{}.publishMoveTo()", getName());
    return sc;
  }

  /**
   * Derived dervo type will need to implement the details of processing a move
   * 
   * @param newPos
   * @param blocking
   * @param timeoutMs
   * @return
   */
  abstract protected boolean processMove(Double newPos, boolean blocking, Long timeoutMs);

  @Override /*
             * FIXME these should be returning null - the event itself is enough
             * info - sending whole servo is excessive
             */
  public String publishServoDisable(ServoControl sc) {
    log.debug("{}.publishServoDisable()", getName());
    return sc.getName();
  }

  @Override /*
             * FIXME these should be returning null - the event itself is enough
             * info - sending whole servo is excessive
             */
  public String publishServoEnable(ServoControl sc) {
    log.debug("{}.publishServoEnable()", getName());
    return sc.getName();
  }

  // TODO: why do we need this method here , invoke message cache misses
  // otherwise.
  public String publishServoEnable(AbstractServo sc) {
    log.debug("{}.publishServoEnable()", getName());
    return publishServoEnable((ServoControl) sc);
  }

  @Override
  public ServoMove publishServoMoveTo(ServoMove move) {
    log.debug("{}.publishServoMoveTo({})", getName(), move);
    return move;
  }

  @Override
  public ServoSpeed publishServoSetSpeed(ServoControl sc) {
    log.debug("{}.publishServoSetSpeed() {}", getName(), getSpeed());
    return new ServoSpeed(sc.getName(), sc.getSpeed());
  }

  @Override
  public ServoControl publishServoStop(ServoControl sc) {
    log.debug("{}.publishServoStop()", getName());
    return sc;
  }

  public List<String> refreshControllers() {
    List<String> cs = Runtime.getServiceNamesFromInterface(ServoController.class);
    return cs;
  }

  /**
   * will disable then detach this servo from all controllers
   */
  @Override
  public void releaseService() {

    if (encoder != null) {
      encoder.disable();
    }

    disable();

    detach();
    super.releaseService();
  }

  @Override
  public void rest() {
    log.info("rest");
    targetPos = rest;
    moveTo(rest);
  }

  /**
   * Auto disable automatically disables the servo stopping the power to it
   * after an idleTimeout time if no move command was sent. After a move it will
   * begin at the "end" of the movement.
   */
  @Override
  public void setAutoDisable(boolean autoDisable) {
    if (autoDisable) {
      if (!isMoving) {
        // not moving - safe & expected to put in a disable
        addTaskOneShot(idleTimeout, "disable");
      }
    } else {
      purgeTask("disable");
    }
    boolean valueChanged = this.autoDisable != autoDisable;
    this.autoDisable = autoDisable;
    if (valueChanged) {
      broadcastState();
    }
  }

  // TODO: consider moving these to the ServoControl interface.
  public int setIdleTimeout(int idleTimeout) {
    if (this.idleTimeout != idleTimeout) {
      this.idleTimeout = idleTimeout;
      broadcastState();
    }
    return idleTimeout;
  }

  // Getter for the current idleTimeout setting.
  public int getIdleTimeout() {
    return idleTimeout;
  }

  @Override
  public void setInverted(boolean invert) {
    mapper.setInverted(invert);
    broadcastState();
  }

  @Override
  public void setMapper(Mapper mapper) {
    this.mapper = (MapperLinear) mapper;
    broadcastState();
  }

  @Override
  public void setMinMaxOutput(double minY, double maxY) {
    mapper.map(mapper.getMinX(), mapper.getMaxX(), minY, maxY);
    broadcastState();
  }

  @Override
  public void setMinMax(double minXY, double maxXY) {
    mapper.map(minXY, maxXY, minXY, maxXY);
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
  public void setPosition(double pos) {
    // currentPos = targetPos = pos;
    // i think this is desired
    targetPos = pos;
    currentInputPos = pos; // mapper.calcInput(pos);
    if (encoder != null) {
      if (encoder instanceof TimeEncoder)
        ((TimeEncoder) encoder).setPos(pos);
    }
    broadcastState();
  }

  @Override
  public void setRest(double rest) {
    this.rest = rest;
    broadcastState();
  }

  @Override
  public void setSpeed(Integer degreesPerSecond) {
    try {
      setSpeed((double) degreesPerSecond);
    } catch (Exception e) {
      log.error("setSpeed threw", e);
    }
  }

  @Override
  public void setSpeed(Double degreesPerSecond) {
    // KW: TODO: technically the Arduino will read this speed as a 16 bit int..
    // so max Speed is 32,767 ...
    // if (maxSpeed != -1 && degreesPerSecond != null && degreesPerSecond >
    // maxSpeed) {
    // speed = maxSpeed;
    // log.info("Trying to set speed to a value greater than max speed");
    // }

    speed = degreesPerSecond;

    if (degreesPerSecond == null) {
      log.debug("disabling speed control");
    }

    // FIXME 1. remove broadcastState 2. send speed and name over
    // publishServoSetSpeed
    broadcast("publishServoSetSpeed", this);
    // broadcastState();
  }

  @Deprecated /* this is really speed not velocity, velocity is a vector */
  public void setVelocity(Double degreesPerSecond) {
    setSpeed(degreesPerSecond);
  }

  @Override
  public void stop() {
    isSweeping = false;
    if (encoder.getClass() == TimeEncoder.class) {
      ((TimeEncoder) encoder).stopMove();
    }
    targetPos = getCurrentInputPos();
    broadcast("publishServoStop", this);
  }

  public void sweep() {
    sweep(null, null);
  }

  public void sweep(Double min, Double max) {
    sweep(min, max, null);
  }

  public void sweep(Double min, Double max, Double speed) {
    if (min == null) {
      sweepMin = mapper.getMinX();
    } else {
      sweepMin = min;
    }
    if (max == null) {
      sweepMax = mapper.getMaxX();
    } else {
      sweepMax = max;
    }
    if (speed != null) {
      setSpeed(speed);
    }
    isSweeping = true;
    sweepingToMax = false;
    moveTo(sweepMin);
    // broadcastState();
  }

  @Override
  public void sync(ServoControl sc) {
    if (sc == null) {
      log.error("{}.sync(null)", getName());
    }
    sync(sc.getName());
  }

  @Deprecated /* Use fullSpeed() instead. */
  public void unsetSpeed() {
    fullSpeed();
  }

  @Override
  public void unsync(String name) {
    if (name == null) {
      log.error("{}.unsync(null)", getName());
    }
    syncedServos.remove(name);
  }

  @Override
  public void sync(String name) {
    if (name == null) {
      log.error("{}.sync(null)", getName());
    }
    if (getName().equals(name)) {
      error("you cannot set a servo synced to itself");
      return;
    }
    syncedServos.add(name);
  }

  @Override
  public void unsync(ServoControl sc) {
    if (sc == null) {
      log.error("{}.unsync(null)", getName());
    }
    unsync(sc.getName());
  }

  @Override
  public void waitTargetPos() {
    //
    // while (this.pos != this.targetPos) {
    // Some sleep perhaps?
    // TODO:
    // }
  }

  @Override
  public void writeMicroseconds(int uS) {
    broadcast("publishServoWriteMicroseconds", this, uS);
  }

  /**
   * Proxied servo event "stopped" from either TimeEncoder or a Controller that
   * supports it
   */
  @Override
  public ServoEvent publishServoStarted(String name, Double position) {
    log.debug("{}.publishServoStarted({},{})", name, name, position);
    isMoving = true;
    // FIXME currentOutputPos or currentInputPos
    return new ServoEvent(name, position);
  }

  /**
   * Proxied servo event "stopped" from either TimeEncoder or a Controller that
   * supports it
   * 
   * FIXME - it would be nice to push this up into the interface as a static -
   * but there are "many" local vars here :(
   * 
   * FYI - this method is called by the TimeEncoder
   * 
   */
  @Override
  public ServoEvent publishServoStopped(String name, Double position) {
    log.debug("publishServoStopped({}, {})", name, position);

    // log.info("TIME-ENCODER SERVO_STOPPED - {}", name);
    // if currently configured to autoDisable - the timer starts now
    // if we are "stopping" going from moving to not moving
    if (autoDisable && isMoving) {
      // we cancel any pre-existing timer if it exists
      purgeTask("disable");
      // and start our countdown
      addTaskOneShot(idleTimeout, "disable");
    }

    // notify all blocking moves - we have stopped
    synchronized (this) {
      this.notifyAll();
    }

    // we've received a stop event from the TimeEncoder or real encoder
    isMoving = false;

    if (isSweeping) {
      double inputPos = currentInputPos; // mapper.calcInput(currentInputP);

      // We got a stop event from the servo - which "should" be
      // the end of a sweep. "Should" be.
      // If our current position from the encoder says we
      // are closer to one side vs the other .. we go to the
      // opposite side.

      double deltaMin = Math.abs(inputPos - sweepMin);
      double deltaMax = Math.abs(inputPos - sweepMax);

      if (deltaMin < deltaMax) {
        send(getName(), "moveTo", sweepMax);
      } else {
        send(getName(), "moveTo", sweepMin);
      }
    }
    // FIXME currentOutputPos or currentInputPos ????
    return new ServoEvent(name, position);
  }

  @Override
  public void startService() {
    super.startService();
    Runtime.getInstance().attachServiceLifeCycleListener(getName());
  }

  @Override
  public String publishServoEnable(String name) {
    log.debug("{}.publishServoEnable()", name);
    return name;
  }

  @Override
  public Double moveTo(Integer newPos) {
    try {
      return moveTo((double) newPos);
    } catch (Exception e) {
      log.error("AbsolutePositionControl threw", e);
    }
    return null;
  }

  @Override
  public void attachServoControlListener(String name) {
    // Add the listener calls.
    addListener("publishServoMoveTo", name);
    addListener("publishMoveTo", name);
    // TODO: this is an ambigious call because we have two flavors of this
    // method.
    // one that takes/returns the string name.. the other that takes/returns the
    // ServoControl.
    addListener("publishServoEnable", name);
    addListener("publishServoDisable", name);
    addListener("publishServoStop", name);

  }

}
