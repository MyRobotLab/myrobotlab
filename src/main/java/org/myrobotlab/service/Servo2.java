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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.ServoData;
import org.myrobotlab.service.interfaces.ServoEventListener;
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

public class Servo2 extends Service implements ServoControl {


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
  
  // "default" for all subsquent created servos
  // FIXME - remove this
  static Boolean isEventsEnabledDefault = null;

  public final static Logger log = LoggerFactory.getLogger(Servo2.class);

  private static final long serialVersionUID = 1L;

  static public boolean eventsEnabledDefault(boolean b) {
    isEventsEnabledDefault = b;
    return b;
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

    ServiceType meta = new ServiceType(Servo2.class.getCanonicalName());
    meta.addDescription("Controls a servo");
    meta.addCategory("motor", "control");

    return meta;
  }


  double acceleration = -1;

  boolean autoDisable;

  private transient Timer autoDisableTimer;
  
  @Deprecated
  private boolean autoEnable = true;
  
  transient ServoController controller;

  String controllerName;

  /**
   * list of names of possible controllers - is a List to support real and simulated controllers
   * at the same time
   */
  public List<String> controllers;

  double currentPosInput;
  /**
   * disableDelayIfVelocity this make sense if velocity &gt; 0 a timer is
   * launched for an extra delay to disable
   */
  public Integer disableDelay;

  /**
   * defaultDisableDelayNoVelocity this make sense if velocity == -1 a timer is
   * launched to delay disable
   */
  public Integer disableDelayNoVelocity;
  
  /**
   * if this servo is getting a pwm stream currently
   */
  boolean enabled = false;

  boolean isEventsEnabled = true;
  
  boolean isSweeping = false;

  /**
   * last time the servo has moved
   */
  long lastActivityTime = 0;

  Double lastPos;

  Mapper mapper;

  Double maxVelocity;

  private boolean moving;

  /**
   * the 'pin' for this Servo - it is Integer because it can be in a state of
   * 'not set' or null.
   * 
   * pin is the ONLY value which cannot and will not be 'defaulted'
   */
  Integer pin;

  /**
   * default rest is 90 default target position will be 90 if not specified
   */
  Double rest;

  // sweep types
  // TODO - computer implemented speed control (non-sweep)
  boolean speedControlOnUC = false;
  
  int sweepDelay = 100;
  
  transient Thread sweeper = null;
  
  boolean sweepOneWay = false;

  double sweepStep = 1;

  /**
   * the calculated output for the servo
   */
  Double targetOutput;

  /**
   * the requested INPUT position of the servo
   */
  Double targetPos;
  Double targetPosBeforeSensorFeebBackCorrection;

  Double velocity;

  Set<ServoControl> syncServos;

  public Servo2(String n) {
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

  @Override
  public void addServoEventListener(ServoEventListener service) {
    isEventsEnabled = true;
    subscribe(getName(), "publishServoEvent");
  }

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
   * attach will default the position to a default reset position since its not
   * specified
   */
  @Override
  public void attach(ServoController controller, Integer pin) throws Exception {
    this.pin = pin;
    attach(controller);
  }

  @Override
  public void attach(ServoController controller, Integer pin, Double pos) throws Exception {
    this.pin = pin;
    this.targetPos = pos;
    attach(controller);
  }

  @Override
  public void attach(ServoController controller, Integer pin, Double pos, Double velocity) throws Exception {
    this.pin = pin;
    this.targetPos = pos;
    this.velocity = velocity;
    attach(controller);
  }

  /**
   * This method will disable and disconnect the servo from it's controller.
   */
  @Override
  public void detach() {
    detach(controller);
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
   * This method will leave the servo connected to the controller, however it
   * will stop sending pwm messages to the servo.
   */
  @Override
  public void disable() {
    enabled = false;
    if (controller != null) {
      controller.servoDisable(this);
    }
    broadcastState();
  }

  /**
   * Equivalent to the Arduino IDE Servo.attach(). It energizes the servo
   * sending pulses to maintain its current position.
   */
  @Override
  public void enable() {
    enable(pin);
  }

  /**
   * Enabling PWM for the Servo. Equivalent to Arduino's Servo.attach(pin). It
   * energizes the servo sending pulses to maintain its current position.
   */
  public void enable(Integer pin) {
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

  public boolean enabled() {
    return enabled;
  }

  public boolean eventsEnabled(boolean b) {
    isEventsEnabled = b;
    broadcastState();
    return b;
  }

  @Override
  public double getAcceleration() {
    return acceleration;
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
  public boolean getAutoDisable() {
    return autoDisable;
  }

  // @Override
  public ServoController getController() {
    return controller;
  }

  @Override
  public String getControllerName() {
    return controllerName;
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

  public long getLastActivityTime() {
    return lastActivityTime;
  }

  @Override
  public Double getLastPos() {
    return lastPos;
  }

  @Override
  public double getMax() {
    return mapper.getMaxX();
  }

  public double getMaxInput() {
    return mapper.getMaxInput();
  }

  public double getMaxOutput() {
    return mapper.getMaxOutput();
  }

  @Override
  public double getMaxVelocity() {
    return maxVelocity;
  }

  @Override
  public double getMin() {
    return mapper.getMinX();
  }

  public double getMinInput() {
    return mapper.getMinInput();
  }

  public double getMinOutput() {
    return mapper.getMinOutput();
  }

  @Override
  public Integer getPin() {
    return pin;
  }

  @Override
  public double getPos() {
    if (targetPos == null) {
      return rest;
    } else {
      return targetPos;
    }
  }

  @Override
  public double getRest() {
    return rest;
  }

  /**
   * this output is 'always' be between 0-180
   */
  @Override
  public double getTargetOutput() {
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
  public double getVelocity() {
    return velocity;
  }

  public boolean isAttached() {
    // this is not pin attach
    return controller != null;
  }

  @Override
  public boolean isAttached(Attachable instance) {
    if (controller == null || controller != instance) {
      return false;
    }
    return true;
  }

  @Override
  public boolean isAttached(String name) {
    return (controller != null && controller.getName().equals(name));
  }

  @Deprecated
  public boolean isAutoDisabled() {
    return autoDisable;
  }

  public boolean isEnabled() {
    return enabled();
  }

  public boolean isEventsEnabled() {
    return isEventsEnabled;
  }

  @Override
  public boolean isInverted() {
    return mapper.isInverted();
  }

  public boolean isMoving() {
    return moving;

  }

  public boolean isSweeping() {
    return isSweeping;
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

  public double microsecondsToDegree(int microseconds) {
    if (microseconds <= 180)
      return microseconds;
    return (double) (microseconds - 544) * 180 / (2400 - 544);
  }

  // FIXME make max complexity moveTo (pos, velocity, speed, blocking ....) with "all" the implementation
  public synchronized void moveTo(double pos) {

    if (controller == null) {
      error(String.format("%s's controller is not set", getName()));
      return;
    }

    // FIXME - doesn't mapper.calculate do this ?
    if (pos < mapper.getMinX()) {
      pos = mapper.getMinX();
    }
    if (pos > mapper.getMaxX()) {
      pos = mapper.getMaxX();
    }
    
    targetPos = pos;

    targetOutput = getTargetOutput();

    lastActivityTime = System.currentTimeMillis();

    if (lastPos != pos || !isEventsEnabled) { 
      // take care if servo will disable soon
      if (autoDisableTimer != null) {
        autoDisableTimer.cancel();
        autoDisableTimer = null;
      }
      controller.servoMoveTo(this);
    }
    if (!isEventsEnabled || lastPos == pos) {
      lastPos = targetPos;
      broadcastState();
    }
  }

  @Override // FIXME - max complexity moveTo(pos, blocking ......) with implementation ..
  public void moveToBlocking(double pos) {
    if (velocity < 0) {
      log.info("No effect on moveToBlocking if velocity == -1");
    }
    
    targetPos = pos;
    this.moveTo(pos);
    // breakMoveToBlocking=false;
    waitTargetPos();
  }

  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();
  }

  public void preShutdown() {
    detach();
  }

  @Override
  public ServoControl publishMoveTo(ServoControl sc) {
    return sc;
  }

  public Double publishServoEvent(Double position) {
    return position;
  }

  public List<String> refreshControllers() {
    controllers = Runtime.getServiceNamesFromInterface(ServoController.class);
    // controllers.addAll(Runtime.getServiceNamesFromInterface(Simulator.class));
    return controllers;
  }

  @Override
  public void releaseService() {
    detach(controller);
    super.releaseService();
  }

  @Override
  public void removeServoEventListener(ServoEventListener service) {
    isEventsEnabled = false;
    unsubscribe(getName(), "publishServoEvent");
  }

  public void rest() {
    moveTo(rest);
  }

  /* setAcceleration is not fully implemented **/
  public void setAcceleration(double acceleration) {
    this.acceleration = acceleration;
    if (controller != null) {
      controller.servoSetAcceleration(this);
    }
  }

  @Override
  public void setAutoDisable(boolean autoDisable) {
    this.autoDisable = autoDisable;    
    broadcastState();
  }

  /**
   * Set the controller for this servo but does not attach it. see also attach()
   */
  @Deprecated /* can and should just be implemented in attach */
  public void setController(ServoController controller) {
    this.controller = controller;
  }

  public void setInverted(boolean invert) {
    mapper.setInverted(invert);
    broadcastState();
  }

  public void setMaxVelocity(double velocity) {
    this.maxVelocity = velocity;
  }

  @Override
  public void setMinMax(Double min, Double max) {
    map(min, max, min, max);
  }

  @Override
  public void setPin(int pin) {
    this.pin = pin;
  }

  /*
  @Deprecated
  public void enableAutoEnable(boolean autoEnable) {
    this.autoEnable = autoEnable;
  }
  */

  @Override
  public void setRest(double rest) {
    this.rest = rest;
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
    setVelocity(vel);
  }

  // choose to handle sweep on arduino or in MRL on host computer thread.
  public void setSpeedControlOnUC(boolean b) {
    speedControlOnUC = b;
  }

  public void setSweepDelay(int delay) {
    sweepDelay = delay;
  }

  public void setVelocity(Double velocity) {
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
   * used to synchronize 2 servos e.g. servo1.sync(servo2) - now they move as
   * one
   */
  public void sync(ServoControl sc) {
    // FIXME - implement with multiple control references !!!!
    syncServos.add(sc);
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

  public void unsync(ServoControl sc) {
    syncServos.remove(sc);
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
  
  public static void main(String[] args) throws InterruptedException {
    String arduinoPort = "COM5";
    LoggingFactory.init(Level.INFO);
    VirtualArduino virtualArduino = (VirtualArduino) Runtime.start("virtualArduino", "VirtualArduino");
    Runtime.start("python", "Python");

    try {
      virtualArduino.connect(arduinoPort);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
    arduino.connect(arduinoPort);
    Adafruit16CServoDriver adafruit16CServoDriver = (Adafruit16CServoDriver) Runtime.start("adafruit16CServoDriver", "Adafruit16CServoDriver");
    adafruit16CServoDriver.attach(arduino, "0", "0x40");

    Runtime.start("gui", "SwingGui");
    Servo2 servo = (Servo2) Runtime.start("servo", "Servo");
    try {
      servo.attach(adafruit16CServoDriver, 1);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    servo.setVelocity(20.0);
    log.info("It should take some time..");
    servo.moveToBlocking(0);
    servo.moveToBlocking(180);
    servo.moveToBlocking(0);
    log.info("Right?");
  }

  @Override
  public void moveToBlocking(double newPos, long timeoutMs) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void waitTargetPos() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ServoData publishServoData(Integer eventType, double currentPos) {
    ServoData se = new ServoData();
    se.name = getName();
    se.src = this;
    se.pos = currentPos;
    return se;
  }

  @Override
  public void attach(String controllerName, Integer pin) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void attach(String controllerName, Integer pin, Double pos) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void attach(String controllerName, Integer pin, Double pos, Double speed) throws Exception {
    // TODO Auto-generated method stub
    
  }


}