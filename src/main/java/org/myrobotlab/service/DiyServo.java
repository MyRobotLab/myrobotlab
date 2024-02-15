/**
 *                    
 * @author GroG &amp; Mats (at) myrobotlab.org
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

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.service.abstracts.AbstractServo;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinListener;
import org.myrobotlab.service.interfaces.ServoEvent;
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
 * 
 *         TODO : move is not accurate ( 1° step seem not possible )
 */

public class DiyServo extends AbstractServo<ServoConfig> implements PinListener {

  double lastOutput = 0.0;
  /**
   * In most cases TargetPos is never reached So we need to emulate it ! if
   * currentPosInput is the same since X we guess it is reached based on
   * targetPosAngleTolerence
   */
  private int nbSamePosInputSinceX = 0;
  private double lastCurrentPosInput = 0;
  // goal is to not use this
  private double targetPosAngleTolerence = 15;

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

  transient EncoderControl encoderControl;

  /**
   * List of available pins on the analog input service
   */
  public List<Integer> pinList = new ArrayList<Integer>();

  double currentVelocity = 0;

  /**
   * Round pos values based on this digit count useful later to compare target
   * &gt; pos
   */
  int roundPos = 0;

  /**
   * feedback of both incremental position and stops. would allow blocking
   * moveTo if desired
   */
  boolean isEventsEnabled = false;

  private double maxVelocity = -1;

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
  public double setPoint = 90.0; // Intial

  /**
   * AD converter needs to be remapped to 0 - 180. D1024 is the default for the
   * Arduino
   */
  double resolution = 1024;
  /**
   * Sample time 20 ms = 50 Hz
   */
  int sampleTime = 20;

  double powerLevel = 0;
  double maxPower = 1.0;
  double minPower = -1.0;

  transient Mapper powerMap = new MapperLinear(-1.0, 1.0, -255.0, 255.0);

  public String disableDelayIfVelocity;

  public String defaultDisableDelayNoVelocity;

  double deltaVelocity = 1;

  transient Object moveToBlocked = new Object();
  /**
   * disableDelayGrace : a timer is launched after targetpos reached
   */
  public int disableDelayGrace = 1000;

  public transient static final int SERVO_EVENT_STARTED = 0;
  public transient static final int SERVO_EVENT_STOPPED = 1;

  // TODO: KW moved from base class. should remove it here too.
  private double currentPosInput = 0;

  /**
   * Constructor
   * 
   * @param n
   *          name of the service
   * @param id
   *          the instance id
   */
  public DiyServo(String n, String id) {
    super(n, id);
    subscribeToRuntime("registered");
    lastActivityTimeTs = System.currentTimeMillis();
  }

  /**
   * Initiate the PID controller
   */
  void initPid() {
    pid = (Pid) startPeer("pid");
    pidKey = this.getName();
    pid.setPid(pidKey, kp, ki, kd); // Create a PID with the name of this
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
  public void startService() {
    super.startService();
    refreshPinArrayControls();
    motorControl = (MotorControl) startPeer("motor");
    initPid();
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

  @Override
  public long getLastActivityTime() {
    return lastActivityTimeTs;
  }

  public boolean isControllerSet() {
    return isControllerSet;
  }

  public boolean isPinArrayControlSet() {
    return isPinArrayControlSet;
  }

  @Override
  public boolean isInverted() {
    return mapper.isInverted();
  }

  @Override
  public void map(double minX, double maxX, double minY, double maxY) {
    mapper = new MapperLinear(minX, maxX, minY, maxY);
    broadcastState();
  }

  /**
   * The most important method, that tells the servo what position it should
   * move to
   */
  @Override
  public Double moveTo(Double pos) {
    synchronized (moveToBlocked) {
      moveToBlocked.notify(); // Will wake up MoveToBlocked.wait()
    }
    deltaVelocity = 1;
    double lastPosInput = currentInputPos;

    if (motorControl == null) {
      error(String.format("%s's controller is not set", getName()));
      return pos;
    }

    if (!isEnabled()) {
      if (pos != lastPosInput || !isAutoDisable()) {
        enable();
      }
    }

    targetPos = pos;
    double targetOutput = getTargetOutput();

    pid.setSetpoint(pidKey, targetOutput);
    lastActivityTimeTs = System.currentTimeMillis();

    // if (isEventsEnabled) {
    // update others of our position change
    invoke("publishMoveTo", this);
    invoke("publishServoEvent", targetOutput);
    // }

    broadcastState();
    return pos;
  }

  /*
   * basic move command of the servo - usually is 0 - 180 valid range but can be
   * adjusted and / or re-mapped with min / max and map commands
   * 
   * TODO - moveToBlocking - blocks until servo sends "ARRIVED_TO_POSITION"
   * response
   */

  // uber good
  public Double publishServoEvent(Double position) {
    return position;
  }

  public List<String> refreshPinArrayControls() {
    pinArrayControls = Runtime.getServiceNamesFromInterface(PinArrayControl.class);
    return pinArrayControls;
  }

  @Override
  public void rest() {
    moveTo(rest);
  }

  @Override
  public void setInverted(boolean invert) {
    mapper.setInverted(invert);
    motorControl.setInverted(invert);
    broadcastState();
  }

  /**
   * update the output min/max for the mapper input values to the mapper are
   * unchanged.
   */
  @Override
  public void setMinMaxOutput(double minY, double maxY) {
    mapper.map(mapper.getMinX(), mapper.getMaxX(), minY, maxY);
    broadcastState();
  }

  @Override
  public void setMinMax(double min, double max) {
    map(min, max, min, max);
  }

  @Override
  public void setRest(double rest) {
    this.rest = rest;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.service.interfaces.ServoControl#stopServo()
   */
  @Override
  public void stop() {
    isSweeping = false;
    broadcastState();
  }

  // FIXME - is it really speed control - you don't currently thread for
  // fractional speed values
  public void sweep(double min, double max, int delay, double step) {
    sweep(min, max, delay, step, false);
  }

  public void sweep(double min, double max, int delay, double step, boolean oneWay) {

    this.sweepMin = min;
    this.sweepMax = max;

    if (isSweeping) {
      stop();
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
   *          - the microseconds value
   */
  public void writeMicroseconds(Integer uS) {
    // log.info("writeMicroseconds({})", uS);
    // TODO. This need to be remapped to Motor and PID internal to this
    // Service
    // getController().servoWriteMicroseconds(this, uS);
    lastActivityTimeTs = System.currentTimeMillis();
    broadcastState();
  }

  /*
   * @Override public void setPin(int pin) { this.pin = pin; }
   */

  @Override
  public double getTargetOutput() {
    return mapper.calcOutput(targetPos);
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

  public void setMaxVelocity(double velocity) {
    this.maxVelocity = velocity;
    broadcastState();
  }

  public Double getMaxVelocity() {
    return maxVelocity;
  }

  @Override
  public void onPin(PinData pindata) {
    double inputValue = pindata.value;
    currentPosInput = 180 * inputValue / resolution;
    // log.debug(String.format("onPin received value %s converted to
    // %s",inputValue, processVariable));
    // we need to read here real angle / seconds
    // before try to control velocity

    // TODO: kw: is this computing the "mapped" velocity? or the calibrated
    // "output" speed of the servo?
    currentVelocity = MathUtils.round(Math.abs(((currentPosInput - currentInputPos) * (500 / sampleTime))), roundPos);

    // log.info("currentPosInput : " + currentPosInput);

    // info(currentVelocity + " " + currentPosInput);

    // pid.setInput(pidKey, currentPosInput);

    // offline feedback ! if diy servo is disabled
    // useful to "learn" gestures ( later ... ) or simply start a moveTo() at
    // real lastPos & sync with UI
    if (!isEnabled() && MathUtils.round(currentInputPos, roundPos) != MathUtils.round(currentPosInput, roundPos)) {
      targetPos = currentInputPos;
      broadcastState();
    }
    // TODO: kw: this seems wrong. the input position should be the invsere
    // mapped input position.
    currentInputPos = currentPosInput;

    ///////////////////////////////////////////////

    if (isEnabled()) {
      if (motorControl != null) {
        // Calculate the new value for the motor
        Double newValue = pid.compute(pidKey, pindata.value);
        if (newValue != null) {
          // double setPoint = pid.getSetpoint(pidKey);

          // TEMP SANTA TRICK TO CONTROL MAX VELOCITY
          deltaVelocity = 1;

          if (currentVelocity > maxVelocity && maxVelocity > 0) {
            deltaVelocity = currentVelocity / maxVelocity;
          }
          // END TEMP SANTA TRICK

          // double output = pid.getOutput(pidKey) / deltaVelocity;

          // motorControl.setPowerLevel(output);
          // log.debug(String.format("setPoint(%s), processVariable(%s),
          // output(%s)", setPoint, processVariable, output));
          if (newValue != lastOutput) {
            motorControl.move(newValue);
            lastOutput = newValue;

            if (isMoving()) {

              // tolerance
              if (currentPosInput == lastCurrentPosInput && Math.abs(targetPos - getTargetOutput()) <= targetPosAngleTolerence) {
                nbSamePosInputSinceX += 1;
              } else {
                nbSamePosInputSinceX = 0;
              }

              // ok targetPos is reached ( with tolerance )
              if (nbSamePosInputSinceX >= 3 || getTargetOutput() == targetPos) {
                publishServoEvent(SERVO_EVENT_STOPPED, getTargetOutput());
              } else {

                if (getTargetOutput() != targetPos) {
                  publishServoEvent(SERVO_EVENT_STARTED, getTargetOutput());
                }
              }
            }
          }
        }
        lastCurrentPosInput = currentPosInput;
        // Thread.sleep(1000 / sampleTime);
      }
    }
  }

  @Override
  public void attach(EncoderControl encoder) {
    // TODO: do i need anything else?
    this.encoderControl = encoder;
  }

  public void attach(PinArrayControl pinArrayControl, int pin) throws Exception {
    if (pinArrayControl == null) {
      error("pinArrayCOntrol cannot be null");
      return;
    }
    this.pinArrayControl = pinArrayControl;
    if (pinArrayControl != null) {
      pinControlName = pinArrayControl.getName();
      isPinArrayControlSet = true;
      this.pin = pin + "";
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
    pinArrayControl.attach(getName());
    pinArrayControl.enablePin(this.pin, rate);
    broadcastState();
  }

  public void setPowerLevel(double power) {
    this.powerLevel = power;
  }

  @Override
  public Double moveToBlocking(Double pos) {

    targetPos = pos;
    this.moveTo(pos);
    // breakMoveToBlocking=false;
    waitTargetPos();
    return pos; // FIXME probably incorrect
  }

  @Override
  public void waitTargetPos() {
    if (isMoving()) {

      synchronized (moveToBlocked) {
        try {
          // Will block until moveToBlocked.notify() is called on another
          // thread.
          log.info("servo {} moveToBlocked was initiate", getName());
          moveToBlocked.wait(15000);// 30s timeout security delay
        } catch (InterruptedException e) {
          log.info("servo {} moveToBlocked was interrupted", getName());
        }
      }

    }
  }

  public ServoEvent publishServoEvent(Integer eventType, double currentPos) {
    ServoEvent sd = new ServoEvent(getName(), currentPos);
    return sd;
  }

  /**
   * getCurrentPos() - return the calculated position of the servo use
   * lastActivityTime and velocity for the computation
   * 
   * @return the current position of the servo
   */
  public Double getCurrentPos() {
    return MathUtils.round(currentPosInput, roundPos);
  }

  @Override
  public boolean isEnabled() {
    return !motorControl.isLocked();
  }

  @Override
  public boolean isSweeping() {
    return isSweeping;
  }

  /**
   * getCurrentVelocity() - return Current velocity ( realtime / based on
   * frequency)
   * 
   * @return degrees / second
   */
  public double getCurrentVelocity() {
    return currentVelocity;
  }

  public void setDisableDelayGrace(int disableDelayGrace) {
    this.disableDelayGrace = disableDelayGrace;
  }

  @Override
  public void enable() {
    // TODO Activate the motor and PID
    lastActivityTimeTs = System.currentTimeMillis();
    isAttached = true;

    motorControl.unlock();
    enabled = true;
    broadcastState();
  }

  @Override
  public void disable() {
    motorControl.stopAndLock();
    enabled = false;
    broadcastState();
  }

  @Override
  public void stopService() {
    super.stopService();
    disable();
  }

  public static void main(String[] args) throws InterruptedException {

    LoggingFactory.getInstance().configure();
    LoggingFactory.getInstance().setLevel(Level.INFO);
    try {
      // Runtime.start("webgui", "WebGui");
      // Runtime.start("gui", "SwingGui");
      // VirtualArduino virtual = (VirtualArduino) Runtime.start("virtual",
      // "VirtualArduino");
      // virtual.connect("COM3");
      // boolean done = false;
      // if (done) {
      // return;
      // }

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      Runtime.start("diy", "DiyServo");

      boolean done = true;
      if (done) {
        return;
      }

      String port = "COM4";
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      // arduino.setBoardUno();
      arduino.connect(port);

      // Adafruit16CServoDriver adafruit16CServoDriver =
      // (Adafruit16CServoDriver) Runtime.start("adafruit16CServoDriver",
      // "Adafruit16CServoDriver");
      // adafruit16CServoDriver.attach(arduino,"1","0x40");
      // Ads1115 ads = (Ads1115) Runtime.start("ads", "Ads1115");
      // ads.attach(arduino,"2","0x40");

      MotorDualPwm motor = (MotorDualPwm) Runtime.start("diyServo.motor", "MotorDualPwm");

      int leftPwmPin = 6;
      int rightPwmPin = 7;
      motor.setPwmPins(leftPwmPin, rightPwmPin);

      motor.attach(arduino);

      Thread.sleep(1000);
      // let's start the encoder!! Amt203Encoder("encoder");
      Amt203Encoder encoder = (Amt203Encoder) Runtime.start("encoder", "Amt203Encoder");

      encoder.setPin(3);

      arduino.attachEncoderControl(encoder);
      Thread.sleep(1000);

      // Ads1115 ads = (Ads1115) Runtime.start("Ads1115", "Ads1115");
      // ads.setController(arduino, "1", "0x48");

      DiyServo diyServo = (DiyServo) Runtime.create("diyServo", "DiyServo");
      Python python = (Python) Runtime.start("python", "Python");
      // diyServo.attachServoController((ServoController)arduino);
      // diyServo.attach((ServoController)arduino);

      // diyServo.map(0, 180, 60, 175);
      diyServo = (DiyServo) Runtime.start("diyServo", "DiyServo");
      diyServo.pid.setPid("diyServo", 1.0, 0.2, 0.1);
      // diyServo.pid.setOutputRange("diyServo", 1, -1);
      // diyServo.pid.setOutput("diyS, Output);
      // diyServo.attach((PinArrayControl) arduino, 14); // PIN 14 = A0
      // diyServo.setInverted(true);
      diyServo.attach(encoder);

      diyServo.setMaxVelocity(-1);

      // diyServo.setAutoDisable(true);
      // diyServo.setMaxVelocity(10);
      // diyServo.moveToBlocking(0);
      // diyServo.moveToBlocking(180);
      // diyServo.setMaxVelocity(-1);
      // diyServo.moveTo(0);

      // Servo Servo = (Servo) Runtime.start("Servo", "Servo");

      // servo.test();
    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  @Override
  public Double getVelocity() {
    return currentVelocity;
  }

  @Override
  public void setSpeed(Double d) {
    setVelocity(d);
  }

  @Override /* grog: TODO ? */
  protected boolean processMove(Double newPos, boolean blocking, Long timeoutMs) {
    // TODO Auto-generated method stub
    return false;
  }

}
