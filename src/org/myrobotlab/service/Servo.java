/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.interfaces.NameProvider;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
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
 */

public class Servo extends Service implements ServoControl {

  /**
   * Sweeper - TODO - should be implemented in the arduino code for smoother
   * function
   * 
   */
  public class Sweeper extends Thread {

    public Sweeper(String name) {
      super(String.format("%s.sweeper", name));
    }

    @Override
    public void run() {

      try {
        while (isSweeping) {
          // increment position that we should go to.
          if (targetPos < mapper.getMaxX() && sweepStep >= 0) { // GroG: dunno
                                                                // if this
                                                                // should be
                                                                // MaxX or
                                                                // MaxOutputX
            targetPos += sweepStep;
          } else if (targetPos > mapper.getMinOutput() && sweepStep < 0) {// GroG:
                                                                          // dunno
                                                                          // if
                                                                          // this
                                                                          // should
                                                                          // be
                                                                          // MinX
                                                                          // or
                                                                          // MinOutputX
            targetPos += sweepStep;
          }

          // switch directions or exit if we are sweeping 1 way
          if ((targetPos <= mapper.getMinX() && sweepStep < 0) || (targetPos >= mapper.getMaxX() && sweepStep > 0)) {
            if (sweepOneWay) {
              isSweeping = false;
              break;
            }
            sweepStep = sweepStep * -1;
          }
          moveTo(targetPos);
          Thread.sleep(sweepDelay);
        }

      } catch (Exception e) {
        isSweeping = false;
        if (e instanceof InterruptedException) {
          info("shutting down sweeper");
        } else {
          logException(e);
        }
      }
    }

  }

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Servo.class);

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

  String controllerName = null;

  Mapper mapper = new Mapper(0, 180, 0, 180);

  /**
   * default rest is 90 default target position will be 90 if not specified
   */
  double rest = 90;

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
  double targetPos = 0;

  /**
   * the calculated output for the servo
   */
  double targetOutput;

  /**
   * list of names of possible controllers
   */
  public List<String> controllers;

  boolean isSweeping = false;
  // double sweepMin = 0;
  // double sweepMax = 180;
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
  boolean isEventsEnabled = false;

  double maxVelocity = -1;

  // GroG says,
  // FIXME - do "final" refactor with attachPin/detachPin and
  // only use controllerName to determine service to service attach !!!
  // to determine if a service is attached is -> controllerName != null
  // to determine if a pin is attached is isPinAttached
  boolean isPinAttached = false;

  double velocity = -1;

  double acceleration = -1;

  double lastPos;

  boolean autoAttach = false;

  public long defaultDetachDelay = 10000;

  class IKData {
    String name;
    Double pos;
  }

  public Servo(String n) {
    super(n);
    refreshControllers();
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
    lastActivityTime = System.currentTimeMillis();
  }

  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();

  }

  public void addServoEventListener(NameProvider service) {
    addListener("publishServoEvent", service.getName(), "onServoEvent");
  }

  public void addIKServoEventListener(NameProvider service) {
    eventsEnabled(true);
    addListener("publishIKServoEvent", service.getName(), "onIKServoEvent");
  }

  /**
   * Re-attach to servo's current pin. The pin must have be set previously.
   * Equivalent to Arduino's Servo.attach(currentPin)
   * 
   * TODO ? should have been named attachPin()
   * 
   */
  @Override
  public void attach() {
    attach(pin);
  }

  /**
   * Equivalent to Arduino's Servo.attach(pin). It energizes the servo sending
   * pulses to maintain its current position.
   */
  @Override
  public void attach(int pin) {
    lastActivityTime = System.currentTimeMillis();
    controller.servoAttachPin(this, pin);
    this.pin = pin;
    isPinAttached = true;
    broadcastState();
    invoke("publishServoAttach", getName());
  }

  /**
   * Equivalent to Arduino's Servo.detach() it de-energizes the servo IT DOES
   * NOT DETACH THE SERVO CONTROLLER !!!
   */
  @Override
  public void detach() {
    this.isPinAttached = false;
    if (controller != null) {
      controller.servoDetachPin(this);
    }
    broadcastState();
    invoke("publishServoDetach", getName());
  }

  public boolean eventsEnabled(boolean b) {
    isEventsEnabled = b;
    broadcastState();
    return b;
  }

  @Override
  public ServoController getController() {
    return controller;
  }

  public long getLastActivityTime() {
    return lastActivityTime;
  }

  @Override
  public double getMax() {
    return mapper.getMaxX();
  }

  @Override
  public double getMin() {
    return mapper.getMinX();
  }

  @Override
  public double getRest() {
    return rest;
  }

  // FIXME - change name to isPinAttached()
  // python scripts might use this ? :(
  public boolean isAttached() {
    // this is not pin attach
    return controller != null;
  }

  public boolean isPinAttached() {
    return isPinAttached;
  }

  @Override
  public boolean isInverted() {
    return mapper.isInverted();
  }

  public void map(double minX, double maxX, double minY, double maxY) {
    mapper = new Mapper(minX, maxX, minY, maxY);
    broadcastState();
  }

  public void moveTo(double pos) {

    if (controller == null) {
      error(String.format("%s's controller is not set", getName()));
      return;
    }
    if (autoAttach && !isPinAttached()) {
      attach();
    }
    lastPos = targetPos;
    if (pos < mapper.getMinX()) {
      pos = mapper.getMinX();
    }
    if (pos > mapper.getMaxX()) {
      pos = mapper.getMaxX();
    }
    targetPos = pos;
    targetOutput = mapper.calcOutput(targetPos); // calculated degrees

    // calculated degrees
    controller.servoMoveTo(this);
    lastActivityTime = System.currentTimeMillis();

    if (autoAttach) {
      if (velocity != -1) {
        this.addTask("DetachServo", 250, "autoDetach");
      }
    }

    if (isEventsEnabled) {
      // update others of our position change
      invoke("publishServoEvent", targetOutput);
      IKData data = new IKData();
      data.name = getName();
      data.pos = targetPos;
      invoke("publishIKServoEvent", data);
      broadcastState();
    }
  }

  /**
   * basic move command of the servo - usually is 0 - 180 valid range but can be
   * adjusted and / or re-mapped with min / max and map commands
   * 
   * TODO - moveToBlocking - blocks until servo sends "ARRIVED_TO_POSITION"
   * response
   */

  public double publishServoEvent(double position) {
    return position;
  }

  public List<String> refreshControllers() {
    controllers = Runtime.getServiceNamesFromInterface(ServoController.class);
    return controllers;
  }

  @Override
  public void releaseService() {
    detach();
    detach(controller);
    super.releaseService();
  }

  public void rest() {
    moveTo(rest);
  }

  public void setInverted(boolean invert) {
    mapper.setInverted(invert);
  }

  @Override
  public void setMinMax(double min, double max) {
    mapper.setMin(min);
    mapper.setMax(max);
    broadcastState();
  }

  @Override
  public void setRest(int rest) {
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
    setVelocity((int) vel);
    // Method from build 1670
    // if(speed <= 0.1d) setVelocity(6);
    // else if (speed <= 0.2d) setVelocity(7);
    // else if (speed <= 0.3d) setVelocity(8);
    // else if (speed <= 0.4d) setVelocity(9);
    // else if (speed <= 0.5d) setVelocity(11);
    // else if (speed <= 0.6d) setVelocity(13);
    // else if (speed <= 0.7d) setVelocity(18);
    // else if (speed <= 0.8d) setVelocity(27);
    // else if (speed <= 0.9d) setVelocity(54);
    // else setVelocity(0);
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

    mapper.setMin(min);
    mapper.setMax(max);

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
   * @param pos
   */
  public void writeMicroseconds(Integer uS) {
    log.info("writeMicroseconds({})", uS);
    controller.servoWriteMicroseconds(this, uS);
    lastActivityTime = System.currentTimeMillis();
    broadcastState();
  }

  /*
   * @Override public void setPin(int pin) { this.pin = pin; }
   */

  @Override
  public Integer getPin() {
    return pin;
  }

  @Override
  public double getPos() {
    return targetPos;
  }

  // This was originally named setController
  // and Tracking service depended on it to set
  // the servos to a controller where pins could
  // be assigned later...
  public void attach(ServoController controller) throws Exception {
    if (isAttached(controller)) {
      log.info("{} servo attached to controller {}", getName(), this.controller.getName());
      return;
    } else if (this.controller != null && this.controller != controller) {
      log.warn("already attached to controller %s - please detach before attaching to controller %s", this.controller.getName(), controller.getName());
      return;
    }

    targetOutput = mapper.calcOutput(targetPos);

    // set the controller
    this.controller = controller;
    this.controllerName = controller.getName();

    // now attach the controller
    // the controller better have
    // isAttach(ServoControl) to prevent infinit loop
    controller.attach(this);
    sleep(300);
    // the controller is attached now
    // its time to attach the pin
    attach(pin);

    broadcastState();
  }

  public void attach(String controllerName, int pin) throws Exception {
    this.pin = pin;
    attach((ServoController) Runtime.getService(controllerName));
  }

  public void attach(String controllerName, int pin, double pos) throws Exception {
    this.pin = pin;
    this.targetPos = pos;
    attach((ServoController) Runtime.getService(controllerName));
  }

  public void attach(String controllerName, int pin, Integer pos, Integer velocity) throws Exception {
    attach((ServoController) Runtime.getService(controllerName), pin, pos, velocity);
  }

  /**
   * attach will default the position to a default reset position since its not
   * specified
   */
  @Override
  public void attach(ServoController controller, int pin) throws Exception {
    this.pin = pin;
    attach(controller);
  }

  public void attach(ServoController controller, int pin, double pos) throws Exception {
    this.pin = pin;
    this.targetPos = pos;
    attach(controller);
  }

  // FIXME - setController is very deficit in its abilities - compared to the
  // complexity of this
  @Override
  public void attach(ServoController controller, int pin, double pos, double velocity) throws Exception {
    this.pin = pin;
    this.targetPos = pos;
    this.velocity = velocity;
  }

  public boolean isAttached(ServoController controller) {
    return this.controller == controller;
  }

  @Override
  public void detach(String controllerName) {
    detach((ServoController) Runtime.getService(controllerName));
  }

  @Override
  public void detach(ServoController controller) {
    if (this.controller == controller) {
      // detach the this device from the controller
      controller.detach(this);
      // remove the this controller's reference
      this.controller = null;
      this.controllerName = null;
      isPinAttached = false;
      broadcastState();
    }
  }

  public void setMaxVelocity(int velocity) {
    this.maxVelocity = velocity;
    if (controller != null) {
      controller.servoSetMaxVelocity(this);
    }
  }

  public void setVelocity(double velocity) {
    if (maxVelocity != -1 && velocity > maxVelocity) {
      velocity = maxVelocity;
    }
    this.velocity = velocity;
    if (controller != null) {
      controller.servoSetVelocity(this);
    }
  }

  public void setAcceleration(double acceleration) {
    this.acceleration = acceleration;
    if (controller != null) {
      controller.servoSetAcceleration(this);
    }
  }

  @Override
  public double getMaxVelocity() {
    return maxVelocity;
  }

  /*
   * public void moveToOutput(Integer moveTo) { if (controller == null) {
   * error(String.format("%s's controller is not set", getName())); return; }
   * 
   * // targetPos = pos; targetOutput = moveTo;
   * 
   * controller.servoWrite(this); lastActivityTime = System.currentTimeMillis();
   * 
   * if (isEventsEnabled) { // update others of our position change
   * invoke("publishServoEvent", targetOutput); }
   * 
   * }
   */

  @Override
  public double getVelocity() {
    return velocity;
  }

  public IKData publishIKServoEvent(IKData data) {
    return data;
  }

  public static void main(String[] args) throws InterruptedException {
    try {
      LoggingFactory.init(Level.INFO);

      // Runtime.start("webgui", "WebGui");
      // Runtime.start("gui", "GUIService");
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.record();
      // arduino.getSerial().record();

      log.info("ports {}", Arrays.toString(arduino.getSerial().getPortNames().toArray()));
      arduino.connect("COM10");

      log.info("ready here");
      // arduino.ackEnabled = true;
      Servo servo = (Servo) Runtime.start("servo", "Servo");

      servo.attach(arduino, 7);
      servo.moveTo(90);
      servo.setRest(30);

      servo.attach(8);
      servo.moveTo(90);
      servo.moveTo(30);

      servo.attach(9);
      servo.moveTo(90);
      servo.setRest(30);

      // FIXME - JUNIT - test attach - detach - re-attach
      // servo.detach(arduino);

      log.info("servo attach {}", servo.isAttached());

      arduino.disconnect();
      arduino.connect("COM4");

      arduino.reset();

      log.info("ready here 2");
      // servo.attach(arduino, 8);
      // servo.attach(
      servo.attach(arduino, 7);
      servo.moveTo(90);
      servo.moveTo(30);

      servo.attach(9);
      servo.moveTo(90);
      servo.setRest(30);

      servo.moveTo(90);
      servo.setRest(30);
      servo.moveTo(10);
      servo.moveTo(90);
      servo.moveTo(180);
      servo.rest();

      servo.setMinMax(30, 160);

      servo.moveTo(40);
      servo.moveTo(140);

      servo.moveTo(180);

      servo.setSpeed(0.5);
      servo.moveTo(31);
      servo.setSpeed(0.2);
      servo.moveTo(90);
      servo.moveTo(180);
      servo.setSpeed(1.0);

      // servo.test();
    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  @Override
  public void setPin(int pin) {
    this.pin = pin;
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
  public double getAcceleration() {
    return acceleration;
  }

  /**
   * getCurrentPos() - return the calculated position of the servo use
   * lastActivityTime and velocity for the computation
   * 
   * @return the current position of the servo
   */
  public Double getCurrentPos() {
    Double currentPos = null;
    if (velocity == -1) {
      return targetPos;
    } else {
      long currentTime = System.currentTimeMillis();
      double dOutput = velocity * (currentTime - lastActivityTime) / 1000;
      if (mapper.getMaxOutput() > 500) {
        dOutput *= (2400 - 544) / 180;
      }
      log.info("dOutput = {}", mapper.calcInput(dOutput));
      if (targetPos > lastPos) {
        if (mapper.getMaxY() > mapper.getMinY()) {
          currentPos = mapper.calcInput(mapper.calcOutput(lastPos) + dOutput);
          if (currentPos > targetPos) {
            currentPos = targetPos;
          }
        } else {
          currentPos = mapper.calcInput(mapper.calcOutput(lastPos) - dOutput);
          if (currentPos > targetPos) {
            currentPos = targetPos;
          }
        }
      } else if (targetPos < lastPos) {
        if (mapper.getMaxY() > mapper.getMinY()) {
          currentPos = mapper.calcInput(mapper.calcOutput(lastPos) - dOutput);
          if (currentPos < targetPos) {
            currentPos = targetPos;
          }
        } else {
          currentPos = mapper.calcInput(mapper.calcOutput(lastPos) + dOutput);
          if (currentPos < targetPos) {
            currentPos = targetPos;
          }
        }
      } else {
        currentPos = targetPos;
      }
      if (currentPos.isNaN()) {
        return targetPos;
      }
      return currentPos;
    }
  }

  /**
   * enableAutoAttach will attach a servo when ask to move and detach it when
   * the move is complete
   * 
   * @param autoAttach
   */
  public void enableAutoAttach(boolean autoAttach) {
    this.autoAttach = autoAttach;
  }

  public Integer microsecondsToDegree(double microseconds) {
    if (microseconds <= 180)
      return (int) microseconds;
    return (int) ((microseconds - 544) * 180 / (2400 - 544));
  }

  public void autoDetach() {
    if (getCurrentPos().intValue() == targetPos) { // servo reach position
      detach();
      purgeTask("DetachServo");
      return;
    }
    if (System.currentTimeMillis() - lastActivityTime > defaultDetachDelay) { // default
                                                                              // detach
                                                                              // delay
      detach();
      purgeTask("DetachServo");
    }
  }

  public String publishServoAttach(String name) {
    return name;
  }

  public String publishServoDetach(String name) {
    return name;
  }

  /**
   * this output is 'always' in degrees !
   */
  @Override
  public double getTargetOutput() {
    return targetOutput;
  }

}