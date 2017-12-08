/**
 *                    
 * @author GroG &amp; Mats (at) myrobotlab.org
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

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * @author Grog &amp; Mats
 * 
 *         Servos have both input and output. Input is usually of the range of
 *         integers between 0 - 180, and output can relay those values directly
 *         to the servo's firmware (Arduino ServoLib, I2C controller, etc)
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
 *         This service is to be used if you have a motor without feedback and
 *         you want to use it as a Servo. So you connect the motor as a Motor
 *         and use an Aduino, Ads1115 or some other input source that can give
 *         an analog input from a potentiometer or other device that can give
 *         analog feedback.
 */

public class DiyServo extends Service implements ServoControl, PinListener {

  /**
   * Sweeper - thread used to sweep motor back and forth
   * 
   */
  public class Sweeper extends Thread {

    public Sweeper(String name) {
      super(String.format("%s.sweeper", name));
    }

    @Override
    public void run() {

      if (targetPos == null) {
        targetPos = sweepMin;
      }

      try {
        while (isSweeping) {
          // increment position that we should go to.
          if (targetPos < sweepMax && sweepStep >= 0) {
            targetPos += sweepStep;
          } else if (targetPos > sweepMin && sweepStep < 0) {
            targetPos += sweepStep;
          }

          // switch directions or exit if we are sweeping 1 way
          if ((targetPos <= sweepMin && sweepStep < 0) || (targetPos >= sweepMax && sweepStep > 0)) {
            if (sweepOneWay) {
              isSweeping = false;
              break;
            }
            sweepStep = sweepStep * -1;
          }
          moveTo(targetPos.intValue());
          Thread.sleep(sweepDelay);
        }

      } catch (Exception e) {
        isSweeping = false;
        if (e instanceof InterruptedException) {
          // info("shutting down sweeper");
        } else {
          log.error("sweeper threw", e);
        }
      }
    }

  }

  /**
   * MotorUpdater The control loop to update the MotorControl with new values
   * based on the PID calculations
   * 
   */
  public class MotorUpdater extends Thread {

    double lastOutput = 0;

    public MotorUpdater(String name) {
      super(String.format("%s.motorUpdater", name));
    }

    @Override
    public void run() {

      try {
        while (true) {
          if (motorControl != null) {
            // Calculate the new value for the motor
            if (pid.compute(pidKey)) {
              double setPoint = pid.getSetpoint(pidKey);
              double output = pid.getOutput(pidKey);
              motorControl.setPowerLevel(output);
              log.debug(String.format("setPoint(%s), processVariable(%s), output(%s)", setPoint, processVariable, output));
              if (output != lastOutput) {
                motorControl.move(output);
                lastOutput = output;
              }
            }
            Thread.sleep(1000 / sampleTime);
          }
        }

      } catch (Exception e) {
        if (e instanceof InterruptedException) {
          // info("Shutting down MotorUpdater");
        } else {
          log.error("motor updater threw", e);
        }
      }
    }
  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(DiyServo.class);

  /**
   * controls low level methods of the motor
   */
  transient MotorControl motorControl;
  public String motorControlName = "motor";

  /**
   * Reference to the Analog input service
   */
  public List<String> pinArrayControls;
  /**
   * // Handle to the selected analog input service and it's name
   */
  transient PinArrayControl pinArrayControl;
  public String pinControlName;

  /**
   * List of available pins on the analog input service
   */
  public List<Integer> pinList = new ArrayList<Integer>();
  public Integer pin;

  /**
   * mapper to be able to remap input values
   */
  Mapper mapper = new Mapper(0, 180, 0, 180);

  Double rest = 90.0;

  long lastActivityTime = 0;

  /**
   * the requested INPUT position of the servo
   */
  Double targetPos;

  /**
   * the calculated output for the servo
   */
  Integer targetOutput;

  /**
   * list of names of possible controllers
   */
  public List<String> controllers;

  // FIXME - currently is only computer control - needs to be either
  // microcontroller or computer
  boolean isSweeping = false;
  double sweepMin = 0;
  double sweepMax = 180;
  int sweepDelay = 1;

  int sweepStep = 1;
  boolean sweepOneWay = false;

  transient Thread sweeper = null;

  /**
   * feedback of both incremental position and stops. would allow blocking
   * moveTo if desired
   */
  boolean isEventsEnabled = false;

  private int maxVelocity = 425;

  private boolean isAttached = false;
  private boolean isControllerSet = false;
  private boolean isPinArrayControlSet = false;

  // Initial parameters for PID.

  static final public int MODE_AUTOMATIC = 1;
  static final public int MODE_MANUAL = 0;
  public int mode = MODE_MANUAL;

  public Pid pid;
  private String pidKey;
  private double kp = 0.020;
  private double ki = 0.001; // 0.020;
  private double kd = 0.0; // 0.020;
  public double setPoint = 90; // Intial
  // setpoint
  // corresponding
  // to
  // a
  // centered
  // servo
  // The
  // pinListener
  // value
  // depends
  // on
  // the
  // hardwawe
  // behind
  // it,
  // so
  // the
  // value
  // from
  // the
  /**
   * AD converter needs to be remapped to 0 - 180. D1024 is the default for the
   * Arduino
   */
  double resolution = 1024;
  /**
   * Sample time 20 ms = 50 Hz
   */
  int sampleTime = 20;
  /**
   * Initial process variable
   */
  public double processVariable = 0;
  transient MotorUpdater motorUpdater = null;

  double powerLevel = 0;
  double maxPower = 1.0;
  double minPower = -1.0;

  Mapper powerMap = new Mapper(-1.0, 1.0, -255.0, 255.0);

  public String disableDelayIfVelocity;

  public String defaultDisableDelayNoVelocity;

  /**
   * Constructor
   * 
   * @param n
   *          name of the service
   */
  public DiyServo(String n) {
    super(n);
    refreshPinArrayControls();
    motorControl = (MotorControl) createPeer("motor", "MotorDualPwm");
    initPid();
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
    lastActivityTime = System.currentTimeMillis();
  }

  /*
   * Update the list of PinArrayControls
   */
  public void onRegistered(ServiceInterface s) {
    refreshPinArrayControls();
    broadcastState();

  }

  /**
   * Initiate the PID controller
   */
  void initPid() {
    pid = (Pid) createPeer("pid");
    pidKey = this.getName();
    pid.setPID(pidKey, kp, ki, kd); // Create a PID with the name of this
    // service instance
    pid.setMode(pidKey, MODE_AUTOMATIC); // Initial mode is manual
    pid.setOutputRange(pidKey, -1.0, 1.0); // Set the Output range to match
    // the
    // Motor input
    pid.setSampleTime(pidKey, sampleTime); // Sets the sample time
    pid.setSetpoint(pidKey, setPoint);
    pid.startService();
  }

  @Override
  public void addServoEventListener(NameProvider service) {
    addListener("publishServoEvent", service.getName(), "onServoEvent");
  }

  /**
   * Re-attach to servo's current pin. The pin must have be set previously.
   * Equivalent to Arduino's Servo.attach(currentPin) In this service it stops
   * the motor and PID is set to manual mode
   */
  @Override
  public void attach() {
    attach(pin);
    broadcastState();
  }

  /**
   * Equivalent to Arduino's Servo.attach(pin). It energizes the servo sending
   * pulses to maintain its current position.
   */
  @Override
  public void attach(int pin) {
    // TODO Activate the motor and PID
    lastActivityTime = System.currentTimeMillis();
    isAttached = true;
    broadcastState();
  }

  /**
   * Equivalent to Arduino's Servo.detach() it de-energizes the servo
   */
  @Override
  // TODO DeActivate the motor and PID
  public void detach() {
    if (motorControl != null) {
      motorControl.stop();
    }
    isAttached = false;
    broadcastState();
  }

  /*
   * Method to check if events are enabled or not
   */

  public boolean eventsEnabled(boolean b) {
    isEventsEnabled = b;
    broadcastState();
    return b;
  }

  public long getLastActivityTime() {
    return lastActivityTime;
  }

  public double getMax() {
    return mapper.getMaxX();
  }

  public double getPos() {
    return targetPos;
  }

  public double getRest() {
    return rest;
  }

  // FIXME - change to enabled()
  public boolean isAttached() {
    return isAttached;
  }

  public boolean isControllerSet() {
    return isControllerSet;
  }

  public boolean isPinArrayControlSet() {
    return isPinArrayControlSet;
  }

  public boolean isInverted() {
    return mapper.isInverted();
  }

  public void map(double minX, double maxX, double minY, double maxY) {
    if (minX != this.getMinInput() || maxX != this.getMaxInput() || minY != this.getMinOutput() || maxY != this.getMaxOutput()) {
      mapper = new Mapper(minX, maxX, minY, maxY);
      broadcastState();
    }
  }

  /**
   * The most important method, that tells the servo what position it should
   * move to
   */
  public void moveTo(double pos) {

    if (motorControl == null) {
      error(String.format("%s's controller is not set", getName()));
      return;
    }

    if (motorUpdater == null) {
      // log.info("Starting MotorUpdater");
      motorUpdater = new MotorUpdater(getName());
      motorUpdater.start();
      // log.info("MotorUpdater started");
    }

    targetPos = pos;
    targetOutput = mapper.calcOutputInt(targetPos);

    pid.setSetpoint(pidKey, targetOutput);
    lastActivityTime = System.currentTimeMillis();

    if (isEventsEnabled) {
      // update others of our position change
      invoke("publishServoEvent", targetOutput);
    }
  }

  /*
   * basic move command of the servo - usually is 0 - 180 valid range but can be
   * adjusted and / or re-mapped with min / max and map commands
   * 
   * TODO - moveToBlocking - blocks until servo sends "ARRIVED_TO_POSITION"
   * response
   */

  // uber good
  public Integer publishServoEvent(Integer position) {
    return position;
  }

  public List<String> refreshPinArrayControls() {
    pinArrayControls = Runtime.getServiceNamesFromInterface(PinArrayControl.class);
    return pinArrayControls;
  }

  @Override
  public void releaseService() {
    // FYI - super.releaseService() calls detach
    // detach();
    if (motorUpdater != null) {
      // shutting down motor updater thread
      motorUpdater.interrupt();
    }

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
    map(min, max, min, max);
    broadcastState();
  }

  public void setRest(double rest) {
    this.rest = rest;
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
    // TODO Replace with internal logic for motor and PID
    // getController().servoSweepStop(this);
    broadcastState();
  }

  public void sweep() {
    double min = mapper.getMinX();
    double max = mapper.getMaxX();
    // sweep(min, max, 1, 1);
  }

  public void sweep(double min, double max) {
    // sweep(min, max, 1, 1);
  }

  // FIXME - is it really speed control - you don't currently thread for
  // fractional speed values
  public void sweep(int min, int max, int delay, int step) {
    sweep(min, max, delay, step, false);
  }

  public void sweep(int min, int max, int delay, int step, boolean oneWay) {

    this.sweepMin = min;
    this.sweepMax = max;
    this.sweepDelay = delay;
    this.sweepStep = step;
    this.sweepOneWay = oneWay;

    if (isSweeping) {
      stop();
    }

    sweeper = new Sweeper(getName());
    sweeper.start();

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
   *          - the microseconds value
   */
  public void writeMicroseconds(Integer uS) {
    // log.info("writeMicroseconds({})", uS);
    // TODO. This need to be remapped to Motor and PID internal to this
    // Service
    // getController().servoWriteMicroseconds(this, uS);
    lastActivityTime = System.currentTimeMillis();
    broadcastState();
  }

  /*
   * @Override public void setPin(int pin) { this.pin = pin; }
   */

  @Override
  public Integer getPin() {
    return this.pin;
  }

  public static void main(String[] args) throws InterruptedException {

    LoggingFactory.getInstance().configure();
    LoggingFactory.getInstance().setLevel(Level.INFO);
    try {
      // Runtime.start("webgui", "WebGui");
      Runtime.start("gui", "SwingGui");
      VirtualArduino virtual = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
      virtual.connect("COM3");
      boolean done = false;
      if (done) {
        return;
      }
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.connect("COM3");

      // Adafruit16CServoDriver adafruit16CServoDriver =
      // (Adafruit16CServoDriver) Runtime.start("adafruit16CServoDriver",
      // "Adafruit16CServoDriver");
      // adafruit16CServoDriver.attach(arduino,"1","0x40");
      // Ads1115 ads = (Ads1115) Runtime.start("ads", "Ads1115");
      // ads.attach(arduino,"2","0x40");

      MotorDualPwm motor = (MotorDualPwm) Runtime.start("dyiServo.motor", "MotorDualPwm");

      motor.setPwmPins(0, 1);
      motor.attach(arduino);

      // Ads1115 ads = (Ads1115) Runtime.start("Ads1115", "Ads1115");
      // ads.setController(arduino, "1", "0x48");

      DiyServo dyiServo = (DiyServo) Runtime.start("dyiServo", "DiyServo");
      // dyiServo.attachServoController((ServoController)arduino);
      // dyiServo.attach((ServoController)arduino);
      dyiServo.attach((PinArrayControl) arduino, 14); // PIN 14 = A0

      // Servo Servo = (Servo) Runtime.start("Servo", "Servo");

      dyiServo.moveTo(90);
      dyiServo.setRest(30);
      dyiServo.moveTo(10);
      dyiServo.moveTo(90);
      dyiServo.moveTo(180);
      dyiServo.rest();

      dyiServo.setMinMax(30, 160);

      dyiServo.moveTo(40);
      dyiServo.moveTo(140);

      dyiServo.moveTo(180);

      dyiServo.setSpeed(0.5);
      dyiServo.moveTo(31);
      dyiServo.setSpeed(0.2);
      dyiServo.moveTo(90);
      dyiServo.moveTo(180);

      // servo.test();
    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  @Override
  public double getMin() {
    return mapper.getMinX();
  }

  @Override
  public double getTargetOutput() {
    return targetOutput;
  }

  /*
   * public void attach(String controllerName) throws Exception {
   * attach((MotorController) Runtime.getService(controllerName)); }
   */

  @Override
  public void detach(String controllerName) {
    ServiceInterface si = Runtime.getService(controllerName);
    if (si instanceof PinArrayControl) {
      detach((PinArrayControl) Runtime.getService(controllerName));
    }
  }

  public void detach(PinArrayControl pinArrayControl) {
    if (this.pinArrayControl == pinArrayControl) {
      this.pinArrayControl = null;
      isPinArrayControlSet = false;
      broadcastState();
    }
  }

  public void setMaxVelocity(int velocity) {
    this.maxVelocity = velocity;
  }

  @Override
  public double getMaxVelocity() {
    return maxVelocity;
  }

  @Override
  public void onPin(PinData pindata) {
    int inputValue = pindata.value;
    processVariable = 180 * inputValue / resolution;
    // log.debug(String.format("onPin received value %s converted to
    // %s",inputValue, processVariable));
    pid.setInput(pidKey, processVariable);
  }

  public void attach(String pinArrayControlName, Integer pin) throws Exception {
    // myServo = (DiyServo) Runtime.getService(boundServiceName);
    attach((PinArrayControl) Runtime.getService(pinArrayControlName), (int) pin);
  }

  public void attach(PinArrayControl pinArrayControl, int pin) throws Exception {
    this.pinArrayControl = pinArrayControl;
    if (pinArrayControl != null) {
      pinControlName = pinArrayControl.getName();
      isPinArrayControlSet = true;
      this.pin = pin;
    }

    // TODO The resolution is a property of the AD converter and should be
    // fetched thru a method call like controller.getADResolution()
    if (pinArrayControl instanceof Arduino) {
      resolution = 1024;
    }
    if (pinArrayControl instanceof Ads1115) {
      resolution = 65536;
    }
    // log.debug(String.format("Detected %s %s. Setting AD resolution to
    // %s",pinArrayControl.getClass(), pinArrayControl.getName(),resolution));

    int rate = 1000 / sampleTime;
    pinArrayControl.attach(this, pin);
    pinArrayControl.enablePin(pin, rate);
    broadcastState();
  }

  public void setPowerLevel(double power) {
    this.powerLevel = power;
  }

  /**
   * // A bunch of unimplemented methods from ServoControl. // Perhaps I should
   * create a new // DiyServoControl interface. // I was hoping to be able to
   * avoid that, but might be a better solution
   */

  @Override
  @Deprecated
  public void setSpeed(double speed) {
    log.error("speed is depreciated, use setVelocity instead");

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

    ServiceType meta = new ServiceType(DiyServo.class.getCanonicalName());
    meta.addDescription("Controls a motor so that it can be used as a Servo");
    meta.addCategory("control", "servo");
    meta.addPeer("motor", "MotorDualPwm", "MotorControl service");
    meta.addPeer("pid", "Pid", "PID service");
    return meta;
  }

  @Override
  public void setPin(int pin) {
    // This method should never be used in DiyServo since it's a method specific
    // to the Servo service
  }

  @Override
  public double getAcceleration() {
    return 1.0;
  }

  @Override
  public void setVelocity(double velocity) {
    warn("TODO !");
  }

  @Override
  public double getVelocity() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getMinInput() {
    return mapper.getMinInput();
  }

  @Override
  public double getMaxInput() {
    return mapper.getMaxInput();
  }

  @Override
  public void addIKServoEventListener(NameProvider service) {
    // TODO Auto-generated method stub

  }

  @Override
  public void sync(ServoControl sc) {
    // TODO Auto-generated method stub

  }

  // None of the methods below can or should be implemented in DiyServo
  // DiyServo uses a Motor peer

  @Override
  public void attachServoController(ServoController controller) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void detachServoController(ServoController controller) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isAttachedServoController(ServoController controller) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void attach(ServoController controller, int pin) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void attach(ServoController controller, int pin, double pos) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void attach(ServoController controller, int pin, double pos, double speed) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void setAutoDisable(boolean autoDisable) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean getAutoDisable() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean moveToBlocking(double pos) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setOverrideAutoDisable(boolean overrideAutoDisable) {
    // TODO Auto-generated method stub

  }

  @Override
  public void waitTargetPos() {
    // TODO Auto-generated method stub

  }

  @Override
  public void onServoEvent(Integer eventType, double currentPosUs) {
    // TODO Auto-generated method stub

  }

  @Override
  public double getCurrentPosOutput() {
    // TODO Auto-generated method stub
    return 0;
  }

  public double getMinOutput() {
    return mapper.getMinOutput();
  }

  public double getMaxOutput() {
    return mapper.getMaxOutput();
  }

  public boolean isEnabled() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isSweeping() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isEventsEnabled() {
    // TODO Auto-generated method stub
    return false;
  }

}