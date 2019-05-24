/**
 *                    
 * @author GroG (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.ServoData;
import org.myrobotlab.service.interfaces.ServoData.ServoStatus;
import org.myrobotlab.service.interfaces.ServoDataListener;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         Servos have both input and output. Input is usually of the range of
 *         integers between 0.0 - 180.0, and output can relay those values
 *         directly to the servo's firmware (Arduino ServoLib, I2C controller,
 *         etc)
 * 
 *         However there can be the occasion that the input comes from a system
 *         which does not have the same range. Such that input can vary from 0.0
 *         to 1.0. For example, OpenCV coordinates are often returned in this
 *         range. When a mapping is needed Servo.map can be used. For this
 *         mapping Servo.map(0.0, 1.0, 0, 180) might be desired. Reversing input
 *         would be done with Servo.map(180, 0, 0, 180)
 * 
 *         outputY - is the values sent to the firmware, and should not
 *         necessarily be confused with the inputX which is the input values
 *         sent to the servo
 * 
 *         FIXME - inherit from AbstractMotor ..
 * 
 */

public class Servo extends Service implements ServoControl {

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

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Servo.class);

  /**
   * Routing Attach - routes ServiceInterface.attach(service) to appropriate
   * methods for this class
   */
  @Override
  public void attach(Attachable attachable) throws Exception {
    if (attachable == null || isAttached(attachable)) {
      return; // null or already attached
    }

    if (ServoController.class.isAssignableFrom(attachable.getClass())) {

      ServoController sc = (ServoController) attachable;

      if (controller != null && controller != sc) {
        // we're switching controllers - need to detach first
        detach();
      }

      targetOutput = getTargetOutput();

      // set the controller
      controller = sc;
      controllerName = sc.getName();

      // now attach the attachable the attachable better have
      // isAttach(ServoControl) to prevent an infinite loop
      // if attachable.attach(this) is not successful, this should throw
      sc.attach(this);

      // the controller is attached now
      // its time to attach the pin
      enable(pin);
      broadcastState();

    } else {
      error("%s doesn't know how to attach a %s", getClass().getSimpleName(), attachable.getClass().getSimpleName());
    }
  }

  /**
   * Routing Attach - routes ServiceInterface.attach(service) to appropriate
   * methods for this class
   */
  @Override
  public void detach(Attachable service) {
    if (!isAttached(controller)) { // not attached - we're done
      return;
    }

    if (ServoController.class.isAssignableFrom(service.getClass())) {
      // disable before detaching .. that's the law !
      // send last message to disable
      disable();
      if (controller != null) {
        ServoController temp = controller;
        controller = null;
        controllerName = null;
        temp.detach(this);
      }

      broadcastState();
    } else {
      error("%s doesn't know how to detach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
    }
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Servo.class.getCanonicalName());
    meta.addDescription("Controls a servo");
    meta.addCategory("motor", "control");

    return meta;
  }

  transient ServoController controller;

  String controllerName;

  Mapper mapper;

  /**
   * default rest is 90 default target position will be 90 if not specified
   */
  Double rest;

  /**
   * last time the servo has moved
   */
  long lastActivityTime = 0;

  /**
   * the 'pin' for this Servo - it is Integer because it can be in a state of
   * 'not set' or null.
   * 
   * pin is the ONLY value which cannot and will not be 'defaulted'
   */
  Integer pin;

  /**
   * the requested INPUT position of the servo
   */
  Double targetPos;
  Double targetPosBeforeSensorFeebBackCorrection;
  Boolean intialTargetPosChange = true;

  /**
   * the calculated output for the servo
   */
  Double targetOutput;

  /**
   * list of names of possible controllers
   */
  public Set<String> controllers;

  boolean isSweeping = false;
  int sweepDelay = 100;

  double sweepStep = 1;
  boolean sweepOneWay = false;

  // sweep types
  // TODO - computer implemented speed control (non-sweep)
  boolean speedControlOnUC = false;

  transient Thread sweeper = null;

  /**
   * feedback of both incremental position and stops. would allow blocking
   * moveTo if desired
   */
  boolean isEventsEnabled = true;
  boolean isIKEventEnabled = false;

  // "default" for all subsquent created servos
  static Boolean isEventsEnabledDefault = null;

  Double maxVelocity;

  boolean enabled = false;

  Double velocity;

  double acceleration = -1;

  Double lastPos;

  @Deprecated
  private boolean autoEnable = true;

  /**
   * defaultDisableDelayNoVelocity this make sense if velocity == -1 a timer is
   * launched to delay disable
   */
  public Integer disableDelayNoVelocity;

  /**
   * disableDelayIfVelocity this make sense if velocity &gt; 0 a timer is
   * launched for an extra delay to disable
   */
  public Integer disableDelay;
  private boolean moving;
  double currentPosInput;
  boolean autoDisable;

  private boolean overrideAutoDisable = false;

  private transient Timer autoDisableTimer;
  private int SensorPin = -1;

  // this var will break a current moveToBlocking to avoid potential conflicts
  transient Object moveToBlocked = new Object();

  public transient static final int SERVO_EVENT_STOPPED = 1;
  public transient static final int SERVO_EVENT_POSITION_UPDATE = 2;

  public static class ServoEventData {
    public String name;
    public Double pos;
    public Integer state;
    public double velocity;
    public Double targetPos;
    Servo src;
    // public int type;
  }

  public Servo(String n) {
    super(n);
    refreshControllers();
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
    lastActivityTime = System.currentTimeMillis();
    if (isEventsEnabledDefault != null) {
      isEventsEnabled = isEventsEnabledDefault;
    }

    // here we define default values if not inside servo.json
    if (mapper == null) {
      mapper = new Mapper(0, 180, 0, 180);
    }
    if (rest == null) {
      rest = 90.0;
    }
    if (lastPos == null) {
      lastPos = rest;
    }
    if (velocity == null) {
      velocity = -1.0;
    }
    if (maxVelocity == null) {
      maxVelocity = -1.0;
    }
    if (disableDelayNoVelocity == null) {
      disableDelayNoVelocity = 10000;
    }
    if (disableDelay == null) {
      disableDelay = 1000;
    }
    if (targetPos == null) {
      targetPos = rest;
    }
  }

  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();
  }

  @Deprecated
  public void addServoEventListener(NameProvider service) {
    isEventsEnabled = true;
    subscribe(getName(), "publishServoEvent");
  }

  @Deprecated
  public void removeServoEventListener(NameProvider service) {
    isEventsEnabled = false;
    unsubscribe(getName(), "publishServoEvent");
  }

  public void addIKServoEventListener(NameProvider service) {
    isIKEventEnabled = true;
    addListener("publishIKServoEvent", service.getName(), "onIKServoEvent");
  }

  public boolean isSweeping() {
    return isSweeping;
  }

  /**
   * Equivalent to the Arduino IDE Servo.attach(). It energizes the servo
   * sending pulses to maintain its current position.
   */
  @Override
  public void enable() {
    enable(pin);
  }

  public void enable(String pin) {
    enable(Integer.valueOf(pin));
  }

  /**
   * Enabling PWM for the Servo. Equivalent to Arduino's Servo.attach(pin). It
   * energizes the servo sending pulses to maintain its current position.
   */
  public void enable(Integer pin) {
    log.info("Enable called on {}", getName());
    if (this.pin != null && this.pin != pin) {
      disable();
    }
    lastActivityTime = System.currentTimeMillis();
    this.pin = pin;
    if (controller != null) {
      enabled = true;
      controller.servoEnable(this);
    }
    broadcastState();
  }

  /**
   * This method will disable and disconnect the servo from it's controller.
   */
  @Override
  public void detach() {
    detach(controller);
  }

  /**
   * This method will leave the servo connected to the controller, however it
   * will stop sending pwm messages to the servo.
   */
  @Override
  public void disable() {
    log.info("Disable called on {}", getName());
    enabled = false;
    if (controller != null) {
      controller.servoDisable(this);
    }
    broadcastState();
  }

  public boolean eventsEnabled(boolean b) {
    isEventsEnabled = b;
    broadcastState();
    return b;
  }

  static public boolean eventsEnabledDefault(boolean b) {
    isEventsEnabledDefault = b;
    return b;
  }

  public boolean isEventsEnabled() {
    return isEventsEnabled;
  }

  // @Override
  public ServoController getController() {
    return controller;
  }

  public long getLastActivityTime() {
    return lastActivityTime;
  }

  public Double getMax() {
    return mapper.getMaxX();
  }

  public Double getMin() {
    return mapper.getMinX();
  }

  public Double getMaxInput() {
    return mapper.getMaxInput();
  }

  public Double getMinInput() {
    return mapper.getMinInput();
  }

  public Double getMaxOutput() {
    return mapper.getMaxOutput();
  }

  public Double getMinOutput() {
    return mapper.getMinOutput();
  }

  @Override
  public Double getRest() {
    return rest;
  }

  public Boolean isAttached() {
    // this is not pin attach
    return controller != null;
  }

  public boolean enabled() {
    return enabled;
  }

  public Boolean isEnabled() {
    return enabled();
  }

  @Deprecated
  public boolean isAutoDisabled() {
    return autoDisable;
  }

  @Override
  public Boolean isInverted() {
    return mapper.isInverted();
  }

  public void map(double minX, double maxX, double minY, double maxY) {
    if (mapper == null) {
      mapper = new Mapper(0, 180, 0, 180);
    }
    if (minX != mapper.getMinX() || maxX != mapper.getMaxX() || minY != mapper.getMinY() || maxY != mapper.getMaxY()) {
      mapper = new Mapper(minX, maxX, minY, maxY);
      broadcastState();
    }
  }

  public ServoEventData publishMoveTo() {
    ServoEventData ret = new ServoEventData();
    ret.src = this;
    return ret;
  }

  @Override
  public synchronized boolean moveTo(Double pos) {
    log.info("Move To Called {} {}", getName(), pos);
    // breakMoveToBlocking=true;
    synchronized (moveToBlocked) {
      moveToBlocked.notify(); // Will wake up MoveToBlocked.wait()
    }
    if (controller == null) {
      error(String.format("%s's controller is not set", getName()));
      return false;
    }

    if (pos < mapper.getMinX()) {
      pos = mapper.getMinX();
    }
    if (pos > mapper.getMaxX()) {
      pos = mapper.getMaxX();
    }
    targetPos = pos;

    if (!isEnabled()) {
      if (pos != lastPos || overrideAutoDisable || !getAutoDisable()) {
        enable();
      }
    }

    if (intialTargetPosChange) {
      targetPosBeforeSensorFeebBackCorrection = targetPos;
    }

    targetOutput = getTargetOutput();

    lastActivityTime = System.currentTimeMillis();

    if (lastPos != pos || !isEventsEnabled) {
      // take care if servo will disable soon
      if (autoDisable)
        scheduleDisableTimer();

      controller.servoMoveTo(this);
    }
    if (!isEventsEnabled || lastPos == pos) {
      lastPos = targetPos;
      broadcastState();
    }
    return true;
  }

  @Override
  public Double moveToBlocking(Double pos) {
    if (velocity < 0) {
      log.info("No effect on moveToBlocking if velocity == -1");
    }
    if (!isEventsEnabled) {
      this.addServoEventListener((NameProvider) this);
    }
    targetPos = pos;
    this.moveTo(pos);
    // breakMoveToBlocking=false;
    waitTargetPos();
    return pos;
  }

  @Override
  public void waitTargetPos() {
    {
      if (isMoving() || Math.round(lastPos) != Math.round(targetPos)) {
        if (velocity > 0) {
          synchronized (moveToBlocked) {
            try {
              // Will block until moveToBlocked.notify() is called on another
              // thread.
              moveToBlocked.wait(30000);// 30s timeout security delay
            } catch (InterruptedException e) {
              log.info("servo {} moveToBlocked was interrupted", getName());
            }
          }
        }
      }
    }
  }

  private void delayDisable() {
    lastPos = targetPos;
    broadcastState();
    if (!isMoving()) {
      if (autoDisable) {
        scheduleDisableTimer();
      } else {
        synchronized (moveToBlocked) {
          moveToBlocked.notify(); // Will wake up MoveToBlocked.wait()
        }
      }
    }
    broadcastState();
  }

  private synchronized void scheduleDisableTimer() {
    log.info("Schedule Disable Timer called");
    int delayBeforeDisable = disableDelayNoVelocity;
    if (velocity > -1) {
      delayBeforeDisable = disableDelay;
    }
    if (autoDisableTimer != null) {
      autoDisableTimer.cancel();
      autoDisableTimer = null;
    }
    autoDisableTimer = new Timer();
    autoDisableTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        if (!overrideAutoDisable && !isMoving()) {
          disable();
        }
        synchronized (moveToBlocked) {
          moveToBlocked.notify(); // Will wake up MoveToBlocked.wait()
        }
      }
    }, (long) delayBeforeDisable);
  }

  public Double publishServoEvent(Double position) {
    return position;
  }

  public List<String> refreshControllers() {
    List<String> cs = Runtime.getServiceNamesFromInterface(ServoController.class);
    controllers = new HashSet<String>();
    for (String c : cs) {
      controllers.add(c);
    }
    // controllers.addAll(Runtime.getServiceNamesFromInterface(Simulator.class));
    return cs;
  }

  @Override
  public void releaseService() {
    detach(controller);
    super.releaseService();
  }

  @Override
  public void startService() {
    super.startService();
    if (isEventsEnabled) {
      this.addServoEventListener(this);
    }
  }

  public void rest() {
    moveTo(rest);
  }

  public void setInverted(boolean invert) {
    mapper.setInverted(invert);
    broadcastState();
  }

  @Override
  public void setMinMax(Double min, Double max) {
    map(min, max, min, max);
  }

  @Override
  public void setRest(Double rest) {
    this.rest = rest;
  }

  public void setAnalogSensorPin(Integer pin) {
    // TODO: update the interface with the double version to make all servos
    // implement
    // a double for their positions.
    this.SensorPin = pin;
  }

  /**
   * setSpeed is deprecated, new function for speed control is setVelocity()
   */
  @Deprecated
  public void setSpeed(double speed) {

    // KWATTERS: The realtionship between the old set speed value and actual
    // angular velocity was exponential.
    // To create a model to map these, I took the natural log of the speed
    // values, computed a linear regression line
    // y=mx+b
    // And then convert it back to exponential space with the e^y
    // approximating this with the equation
    // val = e ^ (slope * x + intercept)
    // slope & intercept were fitted by taking a linear regression of the
    // log values
    // of the mapping.

    // Speed,NewFunction,OldMeasured
    // 0.1,3,6
    // 0.2,5,7
    // 0.3,7,9
    // 0.4,9,9
    // 0.5,13,11
    // 0.6,19,13
    // 0.7,26,18
    // 0.8,36,27
    // 0.9,50,54

    // These 2 values can be tweaked for a slightly different curve that
    // fits the observed data.
    double slope = 3.25;
    double intercept = 1;

    double vel = Math.exp(slope * speed + intercept);
    // set velocity to 0.0 if the speed = 1.0.. This skips the velocity
    // calculation logic.
    if (speed >= 1.0) {
      vel = maxVelocity;
    }
    setVelocity((int) vel);
  }

  // choose to handle sweep on arduino or in MRL on host computer thread.
  public void setSpeedControlOnUC(boolean b) {
    speedControlOnUC = b;
  }

  public void setSweepDelay(int delay) {
    sweepDelay = delay;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.service.interfaces.ServoControl#stopServo()
   */
  @Override
  public void stop() {
    isSweeping = false;
    sweeper = null;
    controller.servoSweepStop(this);
    broadcastState();
  }

  public void sweep() {
    double min = mapper.getMinX();
    double max = mapper.getMaxX();
    sweep(min, max, 50, 1);
  }

  public void sweep(double min, double max) {
    sweep(min, max, 1, 1);
  }

  // FIXME - is it really speed control - you don't currently thread for
  // factional speed values
  public void sweep(double min, double max, int delay, double step) {
    sweep(min, max, delay, step, false);
  }

  public void sweep(double min, double max, int delay, double step, boolean oneWay) {
    mapper.setMinMaxInput(min, max);
    /*
     * THIS IS A BUGG !!! mapper.setMin(min); mapper.setMax(max);
     */

    this.sweepDelay = delay;
    this.sweepStep = step;
    this.sweepOneWay = oneWay;

    // FIXME - CONTROLLER TYPE SWITCH
    if (speedControlOnUC) {
      controller.servoSweepStart(this); // delay &
      // step
      // implemented
    } else {
      if (isSweeping) {
        stop();
      }

      sweeper = new Sweeper(getName());
      sweeper.start();
    }

    isSweeping = true;
    broadcastState();
  }

  /**
   * Writes a value in microseconds (uS) to the servo, controlling the shaft
   * accordingly. On a standard servo, this will set the angle of the shaft. On
   * standard servos a parameter value of 1000 is fully counter-clockwise, 2000
   * is fully clockwise, and 1500 is in the middle.
   * 
   * Note that some manufactures do not follow this standard very closely so
   * that servos often respond to values between 700 and 2300. Feel free to
   * increase these endpoints until the servo no longer continues to increase
   * its range. Note however that attempting to drive a servo past its endpoints
   * (often indicated by a growling sound) is a high-current state, and should
   * be avoided.
   * 
   * Continuous-rotation servos will respond to the writeMicrosecond function in
   * an analogous manner to the write function.
   * 
   * @param uS
   *          - microseconds
   */
  public void writeMicroseconds(Integer uS) {
    log.info("writeMicroseconds({})", uS);
    controller.servoWriteMicroseconds(this, uS);
    lastActivityTime = System.currentTimeMillis();
    broadcastState();
  }

  @Override
  public String getPin() {
    if (pin != null)
      return pin.toString();
    else
      return null;
  }

  @Override
  public Double getPos() {
    if (targetPos == null) {
      return rest;
    } else {
      return targetPos;
    }
  }

  /**
   * attach will default the position to a default reset position since its not
   * specified
   */
  @Deprecated
  public void attach(ServoController controller, int pin) throws Exception {
    this.pin = pin;
    attach(controller);
  }
  
  public void attach(String controller, String pin, double pos) throws Exception {
    log.info("Servo Attach called. {} {} {}", controller, pin, pos);
    this.pin = Integer.parseInt(pin);
    this.targetPos = pos;
    // need to look up the controller by name
    ServoController cont = (ServoController) Runtime.getService(controller);
    attach(cont);
  }


  public void attach(String controller, int pin, double pos) throws Exception {
    log.info("Servo Attach called. {} {} {}", controller, pin, pos);
    this.pin = pin;
    this.targetPos = pos;
    // need to look up the controller by name
    ServoController cont = (ServoController) Runtime.getService(controller);
    attach(cont);
  }

  @Deprecated
  public void attach(ServoController controller, int pin, double pos) throws Exception {
    log.info("Servo Controller Attach pin pos.");
    this.pin = pin;
    this.targetPos = pos;
    attach(controller);
  }

  @Deprecated
  public void attach(ServoController controller, int pin, double pos, double velocity) throws Exception {
    this.pin = pin;
    this.targetPos = pos;
    this.velocity = velocity;
    attach(controller);
  }

  @Override
  public boolean isAttached(Attachable instance) {
    if (controller == null || controller != instance) {
      return false;
    }
    return true;
  }

  public void setMaxVelocity(double velocity) {
    this.maxVelocity = velocity;
  }

  public void setVelocity(double velocity) {
    if (maxVelocity != -1 && velocity > maxVelocity) {
      velocity = maxVelocity;
      log.info("Trying to set velocity to a value greater than max velocity");
    }
    this.velocity = velocity;
    if (controller != null) {
      controller.servoSetVelocity(this);
      broadcastState();
    }
  }

  /* setAcceleration is not fully implemented **/
  public void setAcceleration(double acceleration) {
    this.acceleration = acceleration;
    if (controller != null) {
      controller.servoSetAcceleration(this);
    }
  }

  @Deprecated
  public Double getMaxVelocity() {
    return maxVelocity;
  }

  @Override
  public Double getVelocity() {
    return velocity;
  }

  public ServoEventData publishIKServoEvent(ServoEventData data) {
    return data;
  }

  @Override
  public void setPin(String pin) {
    this.pin = Integer.valueOf(pin);
  }

  @Override
  public boolean isAttached(String name) {
    return (controller != null && controller.getName().equals(name));
  }

  @Override
  public Set<String> getAttached() {
    HashSet<String> ret = new HashSet<String>();
    if (controller != null) {
      ret.add(controller.getName());
    }
    return ret;
  }

  @Override
  public Double getAcceleration() {
    return acceleration;
  }

  /**
   * getCurrentPos() - return the calculated position of the servo use
   * lastActivityTime and velocity for the computation
   * 
   * @return the current position of the servo
   */
  public Double getCurrentPos() {
    return currentPosInput;
  }

  public double getCurrentPosOutput() {
    return mapper.calcOutput(getCurrentPos());
  }

  /**
   * return time to move the servo to the target position, in ms
   * 
   * @return
   */
  int timeToMove() {
    if (velocity <= 0.0) {
      return 1;
    }
    double delta = Math.abs(mapper.calcOutput(targetPos) - mapper.calcOutput(lastPos));
    double time = delta / velocity * 1000;
    return (int) time;
  }

  @Deprecated
  public void enableAutoEnable(boolean autoEnable) {
    this.autoEnable = autoEnable;
  }

  @Deprecated
  public void enableAutoDisable(boolean autoDisable) {
    setAutoDisable(autoDisable);
  }

  @Override
  public void setAutoDisable(Boolean autoDisable) {
    this.autoDisable = autoDisable;
    addServoEventListener(this);
    log.info("setAutoDisable : " + autoDisable);
    delayDisable();
    broadcastState();
  }

  @Override
  public Boolean getAutoDisable() {
    return autoDisable;
  }

  public double microsecondsToDegree(int microseconds) {
    if (microseconds <= 180)
      return microseconds;
    return (double) (microseconds - 544) * 180 / (2400 - 544);
  }

  /**
   * this output is 'always' be between 0-180
   */
  @Override
  public Double getTargetOutput() {
    if (targetPos == null) {
      targetPos = rest;
    }
    if (mapper != null) {
      targetOutput = mapper.calcOutput(targetPos);
    } else {
      targetOutput = targetPos;
    }
    if (targetOutput == null) {
      targetOutput = 0.0;
    }
    return targetOutput;
  }

  @Override
  public String getControllerName() {
    return controllerName;
  }

  public boolean isMoving() {
    return moving;

  }

  @Deprecated
  public void onServoEvent(Integer eventType, double currentPos) {
    currentPosInput = mapper.calcInput(currentPos);
    if (isIKEventEnabled) {
      ServoEventData data = new ServoEventData();
      data.name = getName();
      data.pos = currentPosInput;
      data.state = eventType;
      data.velocity = velocity;
      data.targetPos = this.targetPos;
      invoke("publishIKServoEvent", data);
    }
    if (eventType == SERVO_EVENT_STOPPED) {
      moving = false;
    } else {
      moving = true;
    }
    if (isEventsEnabled) {
      invoke("publishServoEvent", currentPosInput);
    }
  }

  public void onServoEvent(Integer eventType, Integer currentPosUs, double targetPos) {
    double currentPos = microsecondsToDegree(currentPosUs);
    onServoEvent(eventType, currentPos);
  }

  public void onIMAngles(Object[] data) {
    String name = (String) data[0];
    if (name.equals(this.getName())) {
      moveTo((double) data[1]);
    }
  }

  public void onServoEvent(Double position) {
    // log.info("{}.ServoEvent {}", getName(), position);
    if (!isMoving() && enabled()) {
      delayDisable();
    }
  }

  /*
   * Set the controller for this servo but does not attach it. see also attach()
   */
  public void setController(ServoController controller) {
    this.controller = controller;
  }

  /**
   * used to synchronize 2 servos e.g. servo1.sync(servo2) - now they move as
   * one
   */
  public void sync(ServoControl sc) {
    this.addServoEventListener(this);
    ((Servo) sc).addServoEventListener((NameProvider) sc);
    subscribe(sc.getName(), "publishServoEvent", getName(), "moveTo");
  }

  public void unsync(ServoControl sc) {
    // remove
    this.removeServoEventListener(this);
    ((Servo) sc).removeServoEventListener((NameProvider) sc);

    unsubscribe(sc.getName(), "publishServoEvent", getName(), "moveTo");
  }

  public static void main(String[] args) throws InterruptedException {
    try {
      String arduinoPort = "COM9";
      LoggingFactory.init(Level.INFO);

      Runtime.start("gui", "SwingGui");
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      Servo servo = (Servo) Runtime.start("servo", "Servo");
      
      arduino.connect(arduinoPort);
      // Adafruit16CServoDriver adafruit16CServoDriver = (Adafruit16CServoDriver) Runtime.start("adafruit16CServoDriver", "Adafruit16CServoDriver");
      // adafruit16CServoDriver.attach(arduino, "0", "0x40");
      
      servo.attach(arduino.getName(), 5, 120.0);
      // servo.attach(adafruit16CServoDriver, 1);

      servo.setVelocity(20);
      log.info("It should take some time..");
      servo.moveTo(5.0);
      servo.moveTo(175.0);
      servo.moveToBlocking(0.0);
      servo.moveToBlocking(180.0);
      servo.moveToBlocking(0.0);
      log.info("Right?");
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  // pasted from inmoov1
  /**
   * Export servo configuration to a .py file
   */
  public void saveCalibration(String calibrationFilename) {
    if (calibrationFilename == null) {
      calibrationFilename = this.getName() + ".py";
    }
    File calibFile = new File(calibrationFilename);
    FileWriter calibrationWriter = null;
    try {
      calibrationWriter = new FileWriter(calibFile);
      calibrationWriter.write("##################################\n");
      calibrationWriter.write("# " + this.getSimpleName() + " auto generated calibration \n");
      calibrationWriter.write("# " + new Date() + "\n");
      calibrationWriter.write("##################################\n");

      calibrationWriter.write("\n");
      calibrationWriter.write("# Servo Config : " + this.getName() + "\n");
      calibrationWriter.write(this.getName() + ".detach()\n");
      calibrationWriter.write(this.getName() + ".setVelocity(" + this.getVelocity() + ")\n");
      calibrationWriter.write(this.getName() + ".setRest(" + this.getRest() + ")\n");
      if (this.getPin() != null) {
        calibrationWriter.write(this.getName() + ".setPin(" + this.getPin() + ")\n");
      } else {
        calibrationWriter.write("# " + this.getName() + ".setPin(" + this.getPin() + ")\n");
      }

      // save the servo map
      calibrationWriter.write(this.getName() + ".map(" + this.getMinInput() + "," + this.getMaxInput() + "," + this.getMinOutput() + "," + this.getMaxOutput() + ")\n");
      // if there's a controller reattach it at rest
      if (this.getController() != null) {
        String controller = this.getController().getName();
        calibrationWriter.write(this.getName() + ".attach(\"" + controller + "\"," + this.getPin() + "," + this.getRest() + ")\n");
      }
      String pythonBool = "False";
      if (getAutoDisable()) {
        pythonBool = "True";
      }
      calibrationWriter.write(this.getName() + ".setAutoDisable(" + pythonBool + ")\n");

      pythonBool = "False";
      if (isInverted()) {
        pythonBool = "True";
      }
      calibrationWriter.write(this.getName() + ".setInverted(" + pythonBool + ")\n");

      calibrationWriter.write("\n");
      calibrationWriter.close();
      log.info("Exported calibration file {}", calibrationFilename);

    } catch (IOException e) {
      log.error("Error writing calibration file {}", calibrationFilename);
      e.printStackTrace();
      return;
    }
  }

  public void saveCalibration() {
    saveCalibration(null);
  }

  @Deprecated
  public void setOverrideAutoDisable(boolean overrideAutoDisable) {
    this.overrideAutoDisable = overrideAutoDisable;
    if (!overrideAutoDisable) {
      delayDisable();
    }
  }

  @Override
  public ServoControl publishMoveTo(ServoControl sc) {
    return sc;
  }

  @Deprecated
  public Double getLastPos() {
    return lastPos;
  }

  public void preShutdown() {
    detach();
  }

  @Override
  public void onEncoderData(EncoderData data) {
    // TODO Auto-generated method stub

  }

  @Override
  public void attach(ServoDataListener listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public void detach(ServoDataListener listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public Set<String> getControllers() {
    // TODO Auto-generated method stub
    return controllers;
  }

  @Override
  public Double getSpeed() {
    // TODO Auto-generated method stub
    return velocity;
  }

  @Override
  public void map(Double minX, Double maxX, Double minY, Double maxY) {
    // TODO Auto-generated method stub
    Mapper m = new Mapper(minX, maxX, minY, maxY);
    this.mapper = m;

  }

  @Override
  public void setMapper(Mapper m) {
    // TODO Auto-generated method stub
    this.mapper = m;
  }

  @Override
  public Mapper getMapper() {
    // TODO Auto-generated method stub
    return mapper;
  }

  @Override
  public void setMaxSpeed(Double speed) {
    // TODO Auto-generated method stub
    this.maxVelocity = speed;
  }

  @Override
  public Double getMaxSpeed() {
    // TODO Auto-generated method stub
    return maxVelocity;
  }

  @Override
  public ServoData publishServoData(ServoStatus eventType, Double currentPosUs) {
    // TODO Auto-generated method stub
    ServoData d = new ServoData(eventType, getName(), currentPosUs);
    return d;
  }

  @Override
  public Double publishServoStopped(Double pos) {
    // TODO Auto-generated method stub
    return pos;
  }

  @Override
  public void setAcceleration(Double acceleration) {
    // TODO Auto-generated method stub
    this.acceleration = acceleration;
  }

  @Override
  public void setInverted(Boolean invert) {
    // TODO Auto-generated method stub
    this.mapper.setInverted(invert);
  }

  @Override
  public void setPin(Integer pin) {
    // TODO Auto-generated method stub
    this.pin = pin;
  }

  @Override
  public void setVelocity(Double speed) {
    // TODO Auto-generated method stub
    this.velocity = velocity;

  }

  @Override
  public void setSpeed(Double d) {
    // TODO : reconcile speed & velocities.. kill the old speed control
    this.velocity = d;
  }

  @Override
  public Double getTargetPos() {
    // TODO Auto-generated method stub
    return targetPos;
  }

  @Override
  public void setPosition(Double pos) {
    moveTo(pos);
    return;
  }

  @Override
  public EncoderControl getEncoder() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isBlocking() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Double moveToBlocking(Double pos, Long timeoutMs) {
    // TODO Auto-generated method stub
    return null;
  }

}