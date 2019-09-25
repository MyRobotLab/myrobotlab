package org.myrobotlab.service.abstracts;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Config;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.sensor.TimeEncoder;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.ServoData;
import org.myrobotlab.service.interfaces.ServoData.ServoStatus;
import org.myrobotlab.service.interfaces.ServoDataListener;
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
public abstract class AbstractServo extends Service implements ServoControl {

  public final static Logger log = LoggerFactory.getLogger(AbstractServo.class);

  private static final long serialVersionUID = 1L;

  /**
   * acceleration of servo - not really implemented
   */
  protected Double acceleration;

  /**
   * 
   */
  protected Boolean autoDisable = false;

  /**
   * controller name this control is attached to
   */
  protected String controller = null;

  /**
   * the current input position (pre-mapper)
   */
  protected Double currentPosInput;

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
   * last time the servo has moved
   */
  protected long lastActivityTimeTs = 0;

  /**
   * list of servo listeners names
   */
  // @Deprecated /* use notifyEntries !!!*/
  // protected Set<String> listenersx = new LinkedHashSet<>();

  /**
   * input mapper
   */
  protected Mapper mapper = new Mapper(0, 180, 0, 180);

  /**
   * maximum speed default is 500 degrees per second or operating speed of 60
   * degrees in 0.12 seconds
   */
  protected Double maxSpeed = 500.0;

  /**
   * status if the servo thinks its moving ..
   */
  protected boolean moving;

  /**
   * the 'pin' for this Servo - it is Integer because it can be in a state of
   * 'not set' or null.
   * 
   * pin is the ONLY value which cannot and will not be 'defaulted'
   */
  protected String pin;

  /**
   * the "current" position of the servo - this never gets updated from
   * "command" methods such as moveTo - its always status information, and its
   * typically updated from an encoder of some form
   */
  protected Double currentPos;

  /**
   * if the servo is doing a blocking call - it will block other blocking calls
   * until the move it complete or a timeout has been reached. A "moveTo"
   * command will be canceled (not blocked) when isBlocking == true
   */
  protected boolean isBlocking = false;

  /**
   * default rest is 90 default target position will be 90 if not specified
   */
  protected Double rest = 90.0;

  /**
   * speed of the servo - null is no speed control
   */
  protected Double speed = null;

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
   * status only field - updated by encoder
   */
  boolean isMoving = false;

  /**
   * weather a move request was successful. The cases it would be false is no
   * controller or calling moveTo when blocking is in process
   */
  boolean validMoveRequest = false;

  /**
   * controls if the servo is sweeping
   */
  protected boolean isSweeping = false;

  /**
   * min sweep value
   */
  protected Double sweepMin = null;

  /**
   * max sweep value
   */
  protected Double sweepMax = null;

  /**
   * direction of sweep
   */
  protected boolean sweepingToMax = true;

  /**
   * if autoDisable is true - then after any move a timer is set to disable the
   * servo. if the servo is idle for any length of time after
   */
  int idleTimeout = 3000;

  /**
   * if the servo was disabled through an idle-timeout
   */
  boolean idleDisabled = false;

  public AbstractServo(String reservedKey) {
    super(reservedKey);

    // this servo is interested in new services which support either
    // ServoControllers or EncoderControl interfaces
    // we subscribe to runtime here for new services
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");

    /*
     * // new feature - // extracting the currentPos from serialized servo
     * Double lastCurrentPos = null; try { lastCurrentPos = (Double)
     * loadField("currentPos"); } catch (IOException e) {
     * log.info("current pos cannot be found in saved file"); }
     */

    // if no position could be loaded - set to rest
    // we have no "historical" info - assume we are @ rest
    currentPos = targetPos = rest;

    // create our default TimeEncoder
    if (encoder == null) {
      encoder = new TimeEncoder(this);
    }
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

  /**
   * overloaded routing attach
   */
  public void attach(Attachable service) throws Exception {
    if (ServoController.class.isAssignableFrom(service.getClass())) {
      attach((ServoController) service, null, null, null);
    } else if (EncoderControl.class.isAssignableFrom(service.getClass())) {
      attach((EncoderControl) service);
    } else if (ServoDataListener.class.isAssignableFrom(service.getClass())) {
      attach((ServoDataListener) service);
    } else {
      warn(String.format("%s.attach does not know how to attach to a %s", this.getClass().getSimpleName(), service.getClass().getSimpleName()));
    }
  }

  public void attach(ServoController sc) throws Exception {
    attach(sc, null, null, null);
  }

  public void attach(ServoController sc, Integer pin) throws Exception {
    attach(sc, pin, null, null, null);
  }

  public void attach(ServoController sc, Integer pin, Double pos) throws Exception {
    attach(sc, pin, pos, null, null);
  }

  public void attach(ServoController sc, Integer pin, Double pos, Double speed) throws Exception {
    attach(sc, pin, pos, speed, null);
  }

  /**
   * maximum complexity attach with reference to controller
   */
  public void attach(ServoController sc, Integer pin, Double pos, Double speed, Double acceleration) throws Exception {
    
    if (isAttached(sc)) {
      return;
    }

    // check if already attached
    if (controller != null && controller.equals(sc.getName())) {
      log.info("{}.attach({}) controller already attached", getName(), sc.getName());
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
      setVelocity(speed);
    }

    if (acceleration != null) {
      setAcceleration(acceleration);
    }

    controller = sc.getName();
    sc.attach(this);
    enabled = true;

    broadcastState();
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

  @Override
  public void attach(ServoDataListener service) {
    // listeners.add(service.getName());
    addListener("publishServoData", service.getName());
  }

  // @Override
  public void attach(String controllerName, Integer pin) throws Exception {
    attach(controllerName, pin, null);
  }

  // @Override
  public void attach(String controllerName, Integer pin, Double pos) throws Exception {
    attach(controllerName, pin, pos, null);
  }

  // @Override
  public void attach(String controllerName, Integer pin, Double pos, Double speed) throws Exception {
    attach(controllerName, pin, pos, speed, 0.0);
  }

  /**
   * maximum complexity attach with "name" of controller - look for errors then
   * call maximum complexity attach with reference to controller
   */
  // @Override
  public void attach(String controllerName, Integer pin, Double pos, Double speed, Double acceleration) throws Exception {
    ServiceInterface si = Runtime.getService(controllerName);
    if (si == null) {
      error("{}.attach({}) cannot find {} in runtime registry", getName(), controllerName, controllerName);
      return;
    }
    if (!ServoController.class.isAssignableFrom(si.getClass())) {
      error("{} is not a ServoController");
      return;
    }
    attach((ServoController) si, pin, pos, speed);
  }

  /**
   * disables and detaches from all controllers
   */
  public void detach() {
    disable();
    invokeOn(controller, "detach", this);
    controller = null;
    broadcastState();
  }

  // @Override
  public void detach(ServoController sc) {
    if (sc != null && sc.getName().equals(controller)) {
      controller = null;
    }
  }

  @Override
  public void detach(ServoDataListener service) {
    removeListener("publishServoData", service.getName(), CodecUtils.getCallbackTopicName("publishServoData"));
  }

  @Override
  public void disable() {
    if (controller != null) {
      invokeOn(controller, "servoDisable", this);
      enabled = false;
      broadcastState();
    }
  }

  @Override
  public void enable() {
    if (controller != null) {
      invokeOn(controller, "servoEnable", this);
      enabled = true;
      broadcastState();
    }
  }

  @Override
  public Double getAcceleration() {
    return acceleration;
  }

  @Override
  public Boolean getAutoDisable() {
    return autoDisable;
  }

  @Override
  public String getControllerName() {
    return controller;
  }

  @Override
  public Double getMax() {
    return mapper.getMaxX();
  }

  @Override
  public Double getMaxOutput() {
    return mapper.getMaxOutput();
  }

  @Override
  public Double getMin() {
    return mapper.getMinX();
  }

  @Override
  public Double getMinOutput() {
    return mapper.getMinOutput();
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
  public Boolean isAttached() {
    return (controller != null);
  }

  @Override
  public boolean isAttached(Attachable instance) {
    return instance.getName().equals(controller);
  }

  @Override
  public void detach(Attachable service) {
    if (isAttached(service)) {
      if (service.getName().equals(service.getName())) {
        String name = controller;
        controller = null;
        invokeOn(name, "detach", getName());
      }
    }
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
  public void map(Double minX, Double maxX, Double minY, Double maxY) {
    mapper = new Mapper(minX, maxX, minY, maxY);
    broadcastState();
  }

  @Override
  public long getLastActivityTime() {
    return lastActivityTimeTs;
  }

  @Override
  public boolean moveTo(Double newPos) {
    processMove(newPos, false, null);
    return validMoveRequest;
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

    if (controller == null) {
      error(String.format("%s's controller is not set", getName()));
      validMoveRequest = false;
      return;
    }

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

    targetPos = newPos;

    if (targetPos < mapper.getMinX()) {
      targetPos = mapper.getMinX();
    }

    if (targetPos > mapper.getMaxX()) {
      targetPos = mapper.getMaxX();
    }

    if (!isEnabled() && autoDisable) { // FIXME - still not right - need to know
                                       // if this servo was disabled through
                                       // timer or not
      // if (newPos != lastPos || !getAutoDisable()) {
      if (targetPos != currentPos || !isEnabled()) {
        enable();
      }
    }

    targetOutput = getTargetOutput();
    lastActivityTimeTs = System.currentTimeMillis();

    if (currentPos != targetPos) {
      if (controller != null) {
        invokeOn(controller, "servoMoveTo", this);
        // in theory - we're moving now ...
        isMoving = true;
        validMoveRequest = true;
      }
    }

    // "real" encoders are electrically hooked up to the servo and get their
    // events through
    // data lines - faux encoders need to be told in software when servos begin
    // movement
    // usually knowing about encoder type is "bad" but the timer encoder is the
    // default native encoder
    if (encoder != null && encoder instanceof TimeEncoder) {
      TimeEncoder timeEncoder = (TimeEncoder) encoder;
      // calculate trajectory calculates and processes this move
      timeEncoder.calculateTrajectory(getPos(), getTargetPos(), getSpeed());
    }

    invoke("publishMoveTo", this);
    broadcastState();

    if (isBlocking) {
      // our thread did a blocking call - we will wait until encoder notifies us
      // to continue or timeout (if supplied) has been reached
      synchronized (this) {
        try {
          this.wait();
        } catch (InterruptedException e) {/* don't care */
        }
      }
    }
    return;
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
  public ServoControl publishMoveTo(ServoControl sc) {
    return sc;
  }

  @Override
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

  /**
   * will disable then detach this servo from all controllers
   */
  public void releaseService() {
    super.releaseService();
    detach();
  }

  @Override
  public void rest() {
    log.info("here");
    targetPos = rest;
    moveTo(rest);
  }

  @Override
  public void setAcceleration(Double acceleration) {
    this.acceleration = acceleration;
  }

  /**
   * Auto disable automatically disables the servo stopping the power to it
   * after an idleTimeout time if no move command was sent. After a move it will
   * begin at the "end" of the movement.
   */
  @Override
  public void setAutoDisable(Boolean autoDisable) {
    this.autoDisable = autoDisable;

    if (autoDisable) {
      // FIXME - will need to know if disabled manually (by user) or by timer
      // (re-enable-able with move)
      addTaskOneShot(idleTimeout, "idleDisable");
    } else {
      purgeTask("idleDisable");
      idleDisabled = false;
    }
  }

  /**
   * a method called by the idle timer - we will know that this disable is
   * allowed to re-enable
   */
  public void idleDisable() {
    idleDisabled = true;
    disable();
  }

  @Override
  public void setInverted(Boolean invert) {
    mapper.setInverted(invert);
  }

  @Override
  public void setMinMax(Double min, Double max) {
    mapper.setMinMaxInput(min, max);
  }

  @Override
  @Config // default - if pin is different - output servo.setPin()
  public void setPin(Integer pin) {
    this.pin = pin + "";
  }

  @Override
  public void setPin(String pin) {
    this.pin = pin;
  }

  @Override
  public void setRest(Double rest) {
    this.rest = rest;
  }

  @Override
  public void setVelocity(Double degreesPerSecond) {
    setSpeed(degreesPerSecond);
  }

  // FIXME targetPos = pos, reportedSpeed, vs speed - set
  @Override
  public void stop() {
    isSweeping = false;
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

  @Override
  public void sync(ServoControl sc) {
    if (sc == null) {
      log.error("{}.sync(null)", getName());
    }
    syncedServos.add(sc.getName());
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

  @Override
  public void setMapper(Mapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Mapper getMapper() {
    return mapper;
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
        isBlocking = false;
      }
      isMoving = false;
      invoke("publishServoData", ServoStatus.SERVO_STOPPED, currentPos);
      invoke("publishServoStopped", currentPos);

      // new feature - saving the last position
      save();

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
      invoke("publishServoData", ServoStatus.SERVO_POSITION_UPDATE, currentPos);
      // log.info("encoder data says -> moving {} {}", currentPos, targetPos);
    }
  }

  @Override
  public Double publishServoStopped(Double pos) {
    return pos;
  }

  @Override
  public Double getTargetPos() {
    return targetPos;
  }

  @Override
  public void setPosition(Double pos) {
    currentPos = targetPos = pos;
  }

  @Override
  public EncoderControl getEncoder() {
    return encoder;
  }

  @Override
  public void setMaxSpeed(Double maxSpeed) {
    this.maxSpeed = maxSpeed;
  }

  /* decide on one deprecate the other ! */
  public void setMaxVelocity(Double maxSpeed) {
    this.maxSpeed = maxSpeed;
  }

  @Override
  public Double getMaxSpeed() {
    return maxSpeed;
  }

  @Override
  public Double getVelocity() {
    return speed;
  }

  @Override
  public boolean isBlocking() {
    return isBlocking;
  }

  @Override
  public boolean isMoving() {
    return isMoving;
  }

  @Override
  @Config
  public void setSpeed(Double degreesPerSecond) {
    if (degreesPerSecond == null) {
      log.info("disabling speed control");
      speed = null;
    }

    if (maxSpeed != -1 && degreesPerSecond != null && degreesPerSecond > maxSpeed) {
      speed = maxSpeed;
      log.info("Trying to set speed to a value greater than max speed");
    }
    this.speed = degreesPerSecond;
    if (controller != null) {
      invokeOn(controller, "servoSetVelocity", this);
    }
    broadcastState();
  }

  public List<String> refreshControllers() {
    List<String> cs = Runtime.getServiceNamesFromInterface(ServoController.class);
    return cs;
  }

  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();
  }

  public void sweep() {
    sweep(null, null);
  }

  public void sweep(Double min, Double max) {
    sweep(min, max, null);
  }

  public void sweep(Double min, Double max, Double speed) {
    if (min == null) {
      sweepMin = mapper.getMinInput();
    } else {
      sweepMin = min;
    }

    if (max == null) {
      sweepMax = mapper.getMaxInput();
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

  public boolean isSweeping() {
    return isSweeping;
  }

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

}
