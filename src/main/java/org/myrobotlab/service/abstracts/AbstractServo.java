package org.myrobotlab.service.abstracts;

import java.util.LinkedHashSet;
import java.util.Set;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.HobbyServo;
import org.myrobotlab.service.Runtime;
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

  @Deprecated // create a TimeEncoder to support this functionality
  public class Sweeper extends Thread {

    public Sweeper(String name) {
      super(String.format("%s.sweeper", name));
    }

    /**
     * Sweeping works on input, a thread is used as the "controller" (this is
     * input) and input sweeps back and forth - the servo parameters know what
     * to do for output
     */

    @Override
    public void run() {

      double sweepMin = 0.0;
      double sweepMax = 0.0;
      // start in the middle
      double sweepPos = mapper.getMinX() + (mapper.getMaxX() - mapper.getMinX()) / 2;
      isSweeping = true;

      try {
        while (isSweeping) {

          // set our range to be inside 'real' min & max input
          sweepMin = mapper.getMinX() + 1;
          sweepMax = mapper.getMaxX() - 1;

          // if pos is too small or too big flip direction
          if (sweepPos >= sweepMax || sweepPos <= sweepMin) {
            sweepStep = sweepStep * -1;
          }

          sweepPos += sweepStep;
          moveTo(sweepPos);
          Thread.sleep(sweepDelay);
        }
      } catch (Exception e) {
        isSweeping = false;
      }
    }

  }

  public final static Logger log = LoggerFactory.getLogger(AbstractServo.class);

  private static final long serialVersionUID = 1L;

  public static void main(String[] args) throws InterruptedException {
    try {

      Runtime.start("gui", "SwingGui");
      Platform.setVirtual(false);

      Arduino mega = (Arduino) Runtime.start("mega", "Arduino");
      mega.setBoardMega();
      HobbyServo servo = (HobbyServo) Runtime.start("servo", "HobbyServo");
      servo.setPin(12);
      servo.sweepDelay = 3;
      // servo.save();
      servo.load();
      servo.save();
      log.info("sweepDely {}", servo.sweepDelay);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  /**
   * acceleration of servo - not really implemented
   */
  protected Double acceleration;

  /**
   * 
   */
  protected Boolean autoDisable = false;

  /**
   * list of controller names this control is attached to
   */
  protected Set<String> controllers = new LinkedHashSet<>();

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
  EncoderControl encoder;

  protected boolean isSweeping = false;

  /**
   * last time the servo has moved
   */
  protected long lastActivityTime = 0;

  /**
   * servo's last position
   */
  protected Double lastPos;

  /**
   * list of servo listeners names
   */
  protected transient Set<String> listeners = new LinkedHashSet<>();

  /**
   * input mapper
   */
  protected Mapper mapper;

  /**
   * maximum speed
   */
  protected Double maxSpeed;

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

  private Double pos;

  /**
   * default rest is 90 default target position will be 90 if not specified
   */
  protected Double rest;

  /**
   * speed of the servo
   */
  protected Double speed;

  protected int sweepDelay = 100;

  protected transient Thread sweeper = null;

  protected boolean sweepOneWay = false;

  protected Double sweepStep = 1.0;

  /**
   * synchronized servos - when this one moves, it sends move commands to these
   * servos
   */
  protected transient Set<String> syncedServos = new LinkedHashSet<>();

  /**
   * the calculated output for the servo
   */
  protected Double targetOutput;

  /**
   * the requested INPUT position of the servo
   */
  protected Double targetPos;

  /**
   * default blocking timeout if not specified
   */
  Integer timeoutMs = 30000;

  public AbstractServo(String reservedKey) {
    super(reservedKey);

  }

  @Override
  public void attach(ServoController controller, Integer pin) throws Exception {
    attach(controller, pin, null);
  }

  @Override
  public void attach(ServoController controller, Integer pin, Double pos) throws Exception {
    attach(controller, pin, null, null);
  }

  /**
   * maximum complexity attach with reference to controller
   */
  @Override
  public void attach(ServoController controller, Integer pin, Double pos, Double speed) throws Exception {
    if (controller == null) {
      log.error("{}.attach(null)", getName());
      return;
    }

    // check if already attached
    if (controllers.contains(controller.getName())) {
      log.info("{}.attach({}) controller already attached", getName(), controller.getName());
      return;
    }

    // update pin if non-null value supplied
    if (pin != null) {
      setPin(pin);
    }

    // update pos if non-null value supplied
    if (pos != null) {
      targetPos = pos;
      lastPos = targetPos;
    }

    // update speed if non-null value supplied
    if (speed != null) {
      setSpeed(speed);
    }

    controllers.add(controller.getName());
    controller.attach(this);
  }

  @Override
  public void attach(ServoDataListener service) {
    listeners.add(service.getName());
  }

  @Override
  public void attach(String controllerName, Integer pin) throws Exception {
    attach(controllerName, pin, null);
  }

  @Override
  public void attach(String controllerName, Integer pin, Double pos) throws Exception {
    attach(controllerName, pin, pos, null);
  }

  /**
   * maximum complexity attach with "name" of controller - look for errors then
   * call maximum complexity attach with reference to controller
   */
  @Override
  public void attach(String controllerName, Integer pin, Double pos, Double speed) throws Exception {
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
    for (String controller : controllers) {
      ServiceInterface si = Runtime.getService(controller);
      if (si.isLocal()) {
        ((ServoController) Runtime.getService(controller)).detach(this);
      } else {
        send(controller, "detach", this);
      }
    }
  }

  @Override
  public void detach(ServoController controller) {
    if (controller == null) {
      return;
    }
    controllers.remove(controller.getName());
  }

  @Override
  public void detach(ServoDataListener service) {
    if (service != null) {
      listeners.remove(service.getName());
    }
  }

  @Override
  public void disable() {
    for (String controller : controllers) {
      ServiceInterface si = Runtime.getService(controller);
      if (si.isLocal()) {
        ((ServoController) Runtime.getService(controller)).servoDisable(this);
      } else {
        send(controller, "servoDisable", this);
      }
    }
  }

  @Override
  public void enable() {
    for (String controller : controllers) {
      ServiceInterface si = Runtime.getService(controller);
      if (si.isLocal()) {
        ((ServoController) Runtime.getService(controller)).servoEnable(this);
      } else {
        send(controller, "servoEnable", this);
      }
    }
  }

  @Override
  public void enable(Integer pin) {
    setPin(pin);
    enable();
  }

  @Override
  public void enable(String pin) {
    setPin(pin);
    enable();
  }

  @Override
  public Double getAcceleration() {
    return acceleration;
  }

  @Override
  public Boolean getAutoDisable() {
    return autoDisable;
  }

  @Deprecated /* its a set now */
  @Override
  public String getControllerName() {
    if (controllers.size() > 0) {
      return controllers.iterator().next();
    }
    return null;
  }

  @Override
  public Set<String> getControllers() {
    return controllers;
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
  public Double getMaxSpeed() {
    return maxSpeed;
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
    return pos;
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
    return targetOutput;
  }

  @Override
  public Boolean isAttached() {
    return (controllers.size() > 0);
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
  public void map(Integer minX, Integer maxX, Integer minY, Integer maxY) {
    map(minX, maxX, minY, maxY);
  }

  @Override
  public void moveTo(Double newPos) {
    moveTo(newPos, false, null);
  }

  /**
   * max complexity moveTo
   * 
   * @param newPos
   * @param isBlocking
   * @param timeout
   */
  public Double moveTo(Double newPos, Boolean isBlocking, Long timeoutMs) {
    // FIXME - implement encoder blocking ...
    // FIXME - when and what should a servo publish and when ?

    // breakMoveToBlocking=true;
    // synchronized (moveToBlocked) {
    // moveToBlocked.notify(); // Will wake up MoveToBlocked.wait()
    // }

    if (controllers.size() == 0) {
      error(String.format("%s's controller is not set", getName()));
      return pos;
    }

    if (newPos < mapper.getMinX()) {
      pos = mapper.getMinX();
    }
    if (newPos > mapper.getMaxX()) {
      pos = mapper.getMaxX();
    }

    targetPos = newPos;

    if (!isEnabled()) {
      if (newPos != lastPos || !getAutoDisable()) {
        enable();
      }
    }

    targetOutput = getTargetOutput();
    lastActivityTime = System.currentTimeMillis();

    if (lastPos != newPos) {
      for (String controller : controllers) {
        ServiceInterface si = Runtime.getService(controller);
        if (si.isLocal()) {
          ((ServoController) Runtime.getService(controller)).servoMoveTo(this);
        } else {
          send(controller, "servoMoveTo", this);
        }
      }
    }

    if (lastPos == newPos) {
      lastPos = targetPos;
      broadcastState(); // publishServo vs publishServoData .. GAH ! :P
    }

    return pos;
  }

  @Override
  public void moveTo(Float newPos) {
    moveTo(newPos.doubleValue());
  }

  @Override
  public void moveTo(Integer newPos) {
    moveTo(newPos.doubleValue());
  }

  @Override
  public Double moveToBlocking(Double pos) {
    return moveTo(pos, true, null);
  }

  @Override
  public Double moveToBlocking(Double newPos, Long timeoutMs) {
    return moveTo(newPos, true, timeoutMs);
  }

  @Override
  public Double moveToBlocking(Float newPos) {
    return moveTo(newPos.doubleValue(), true, null);
  }

  @Override
  public Double moveToBlocking(Float newPos, Long timeoutMs) {
    return moveTo(newPos.doubleValue(), true, timeoutMs);
  }

  @Override
  public Double moveToBlocking(Integer newPos) {
    return moveTo(newPos.doubleValue(), true, null);
  }

  @Override
  public Double moveToBlocking(Integer newPos, Long timeoutMs) {
    return moveTo(newPos.doubleValue(), true, timeoutMs);
  }

  @Override
  public ServoControl publishMoveTo(ServoControl sc) {
    return sc;
  }

  @Override
  public ServoData publishServoData(ServoStatus status, Double pos) {
    ServoData sd = new ServoData();
    sd.name = getName();
    sd.pos = pos;
    sd.speed = speed;
    sd.targetPos = targetPos;
    sd.state = status;
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
    targetPos = rest;
    for (String controller : controllers) {
      ServiceInterface si = Runtime.getService(controller);
      if (si.isLocal()) {
        ((ServoController) Runtime.getService(controller)).servoMoveTo(this);
      } else {
        send(controller, "moveTo", this);
      }
    }
  }

  @Override
  public void setAcceleration(Double acceleration) {
    this.acceleration = acceleration;
  }

  @Override
  public void setAcceleration(Integer acceleration) {
    this.acceleration = acceleration.doubleValue();
  }

  @Override
  public void setAutoDisable(Boolean autoDisable) {
    this.autoDisable = autoDisable;
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
  public void setMinMax(Integer min, Integer max) {
    mapper.setMinMaxInput(min, max);
  }

  @Override
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
  public void setRest(Integer rest) {
    this.rest = rest.doubleValue();
  }

  @Override
  public void setSpeed(Double degreesPerSecond) {
    if (maxSpeed != -1 && degreesPerSecond > maxSpeed) {
      speed = maxSpeed;
      log.info("Trying to set speed to a value greater than max speed");
    }
    this.speed = degreesPerSecond;
    for (String controller : controllers) {
      ServiceInterface si = Runtime.getService(controller);
      if (si.isLocal()) {
        ((ServoController) Runtime.getService(controller)).servoSetVelocity(this);
      } else {
        send(controller, "servoSetVelocity", this);
      }
    }
    broadcastState();
  }

  @Override
  public void setSpeed(Integer degreesPerSecond) {
    setSpeed(degreesPerSecond.doubleValue());
  }

  @Override
  @Deprecated /* use setSpeed */
  public void setVelocity(Double speed) {
    setSpeed(speed);
  }

  @Override
  @Deprecated /* use setSpeed */
  public void setVelocity(Integer speed) {
    setSpeed(speed);
  }

  // FIXME targetPos = pos, reportedSpeed, vs speed - set
  @Override
  public void stop() {
    isSweeping = false;
    sweeper = null;
    for (String controller : controllers) {
      ServiceInterface si = Runtime.getService(controller);
      if (si.isLocal()) {
        ((ServoController) Runtime.getService(controller)).servoSweepStop(this);
      } else {
        send(controller, "servoSweepStop", this);
      }
    }
    broadcastState();
  }

  /**
   * disable servo
   */
  public void stopService() {
    super.stopService();
    disable();
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
    // TODO Auto-generated method stub

  }

}
