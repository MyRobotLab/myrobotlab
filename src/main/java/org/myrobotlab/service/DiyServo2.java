/**
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

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.sensor.EncoderListener;
import org.myrobotlab.service.config.DiyServo2Config;
import org.myrobotlab.service.data.ServoMove;
import org.myrobotlab.service.data.ServoSpeed;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoControlPublisher;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.ServoEvent;
import org.myrobotlab.service.interfaces.ServoStatusPublisher;

/**
 * Simple(ish) DiyServo2.  
 * This service requires an Encoder and a MotorControl.  It uses PID control to 
 * control the position of an actuator measured by the encoder with the motor.
 * The standard PID params are supported Kp,Ki,Kd.
 * This implements servo control.
 * 
 * Data is published from the encoder to this service, that updates the input to the Pid control.
 * The Pid output is computed by default at 20Hz and is controlled by the sampleTime parameter.
 * The output of the pid control is then written to the motor control
 */

public class DiyServo2 extends Service<DiyServo2Config> implements EncoderListener, ServoControl, ServoControlPublisher, ServoStatusPublisher {

  private static final long serialVersionUID = 1L;
  private volatile boolean enabled = true;
  private MotorControl motorControl;
  private Double currentAngle;
  public Pid pid;

  // TODO: use the motor name.
  public String pidKey = "diy2";

  private double kp = 0.05;
  private double ki = 0.001; // 0.020;
  private double kd = 0.001; // 0.020;
  public double setpoint = 90.0; // Intial
  // samples per second.
  public int sampleTime = 20;
  static final public int MODE_AUTOMATIC = 1;
  
  transient MotorUpdater motorUpdater;
  EncoderControl encoder;
  private Double rest = 90.0;
  private long lastActivityTimeMS;
  
  public DiyServo2(String reservedKey, String inId) {
    super(reservedKey, inId);
  }

  @Override
  synchronized public void startService() {
    super.startService();
    pidKey = getFullName();
    // TODO:figure out what we should do here?  perhaps nothing?
    initPid();
  }

  void initPid() {
    // are the peers going to be created?
    pid = (Pid)Runtime.start("pid", "Pid");
    //pid = (Pid) createPeer("pid");
    pidKey = this.getName();
    pid.setPid(pidKey, kp, ki, kd); // Create a PID with the name of this
    // service instance
    pid.setMode(pidKey, MODE_AUTOMATIC); // Initial mode is manual
    pid.setOutputRange(pidKey, -1.0, 1.0); // Set the Output range to match the Motor input
    pid.setSampleTime(pidKey, sampleTime); // Sets the sample time
    pid.setSetpoint(pidKey, setpoint);
    pid.startService();
  }
  
  @Override
  public void onEncoderData(EncoderData data) {
    // System.err.println("DIY Servo Encoder Data: " + data);
    this.currentAngle = data.angle;
    // TODO: could we just update the PID here?
  }

  public void attachEncoderControl(EncoderControl enc) {
    encoder = enc;
    // Tell the encoder to publish encoder data to this service
    encoder.attachEncoderListener(this);
  }

  private void attachMotorControl(MotorControl mot) {
    // use the motor name as the pid key
    this.motorControl = mot;
    
    //  this.pidKey = mot.getName();
    if (motorUpdater == null) {
      log.info("Starting MotorUpdater");
      motorUpdater = new MotorUpdater(getName());
      motorUpdater.start();
      log.info("MotorUpdater started");
    }

  }

  public Double moveTo(Double angle) {
    log.info("Servo Move to {}", angle);
    // This updates the setpoint of the pid control.
    this.setpoint = angle;
    pid.setSetpoint(pidKey, angle);
    lastActivityTimeMS = System.currentTimeMillis();
    // Why does this return a boolean?
    
    // invoke("publishMoveTo", this);
    // invoke("publishServoEvent", angle.doubleValue());
    return angle;
  }

  /**
   * MotorUpdater The control loop to update the MotorControl with new values
   * based on the PID calculations
   * 
   */
  public class MotorUpdater extends Thread {

    double lastOutput = 0.0;
    // degree threshold for saying that we've arrived.
    double threshold = 0.25;
    // goal is to not use this
    
    public MotorUpdater(String name) {
      super(String.format("%s.motorUpdater", name));
    }

    @Override
    public void run() {
      log.info("Motor updater started");
      try {
        while (true) {
          if (isRunning()) {
            // log.info("Updating control loop");
            // Calculate the new value for the motor
            if (pid.data.containsKey(pidKey) && currentAngle != null && pidKey != null) {
              // Update the pid input value.
              // pass the current angle from the encoder to the pid controller
              Double output = pid.compute(pidKey, currentAngle);
              if (output == null) {
                continue;
              }
              double delta = Math.abs(currentAngle - setpoint);
              if (delta < threshold ) {
                log.info("Arrived!");
                motorControl.move(0);
                // TODO: some debouncing logic here.
                // TODO: publish the servo events for started/stopped here.
              } else if (output != lastOutput) {
                log.info("move motor : Power: {}  Target: {}  Current: {}  Delta: {}", output, setpoint, currentAngle, delta);
                motorControl.move(output);
                lastOutput = output;
              } else {
                log.info("delta {} threshold {} current {} setpoint {} output {} lastOutput {}", delta, threshold, currentAngle, setpoint, output, lastOutput);
              }
            }
            // TODO: how long do we need to sleep
            // TODO: maybe a more accurate loop timer?
            // This is a samples per second.. we need to only wait for the remainaing amount of time in the period.
            Thread.sleep(1000 / sampleTime);
          } else {
            log.info("Not running?!");
          }
          
        }
      } catch (Exception e) {
        if (e instanceof InterruptedException) {
          motorControl.stop();
        } else {
          log.error("motor updater threw", e);
        }
      }
    }

    private boolean isRunning() {
      // if we are enabled, have a motor connected and have received encoder data.
      return enabled && motorControl != null && currentAngle != null;
    }
  }

  @Override
  public void disable() {
    // TODO: what do do here?
    // motorControl.disable();
    // TODO: we should disable the encoder also here.. 
    motorControl.stop();
    enabled = false;
    // TODO: broadcast enabled/disabled messages?
  }

  @Override
  public void enable() {
    // TODO: what do to here?  
    // motorControl.enable();
    enabled = true;
  }

  @Override
  public double getRest() {
    // Ok.. not a bad idea.. let's have a rest position for the servo.. default to 90 deg? or something?
    return rest;
  }

  @Override
  public boolean isAutoDisable() {
    // TODO: impl this.. safety is good.
    return false;
  }
  
  @Override
  public void attach(ServoController listener) {
    // TODO: remove from ServoControl interface... NoOp here.
    // NoOp : no servo controllers here..
    log.warn("Diy Servo 2 doesn't use a controller..  no implemented.");
  }

  @Override
  public void detach(ServoController listener) {
    // TODO maybe remove from interface?  this service doesn't give a crapola about servo controllers.
    log.warn("Diy Servo doesn't use a controller..  no implemented.");    
  }

  @Override
  public String getController() {
    // TODO remove from interface?. we have no controller.
    log.warn("Diy Servo 2 doesn't use a controller..  no implemented.");
    return null;
  }

  @Override
  public EncoderControl getEncoder() {
    // TODO: we just subscribe to the encoder.. we don't have/need a handle to it!  
    // why are we expected to return it.. remove from interface.
    return encoder;
  }

  @Override
  public long getLastActivityTime() {
    return lastActivityTimeMS;
  }

  @Override
  public Mapper getMapper() {
    // TODO - we have no mapper...
    return null;
  }

  @Override
  public double getMax() {
    // TODO: should implement.. safety limits are important.
    // This might be useful to know what the max/min value that this encoder can get to.. but for us.. it's 360 degrees.. and can rotate as much as we like.
    return 360;
  }

  @Override
  public double getMin() {
    // TODO safety limits are good..  
    return 0;
  }

  @Override
  public String getPin() {
    log.warn("DiyServo doesn't have pins.  No implemented.");
    // TODO: This doesn't mean anything here. 
    // maybe this is the pin from the encoder? but why..  
    return null;
  }

  @Override
  public double getCurrentInputPos() {
    // TODO: return currentAngle? we have no mapper.
    return currentAngle;
  }

  @Override
  public double getCurrentOutputPos() {
    // TODO: this interface has way too much stuff in it... 
    // return the last known encoder angle
    return currentAngle;
  }

  @Override
  public Double getSpeed() {
    // TODO: implement speed control
    return null;
  }

  @Override
  public double getTargetOutput() {
    // the setPoint for the pid control is the target output.
    // we have no mapper..
    return setpoint;
  }

  @Override
  public double getTargetPos() {
    // This is the setPoint for the pid control.. the target position.  
    return setpoint;
  }

  @Override
  public boolean isBlocking() {
    // TODO What does this mean? should we remove this from the interface?
    return false;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public boolean isInverted() {
    // TODO not sure what this means for a diyservo.
    return false;
  }

  @Override
  public boolean isMoving() {
    // TODO we should trigger this off the  power output being sent to the motor.
    return false;
  }

  @Override
  public void map(double minX, double maxX, double minY, double maxY) {
    // TODO No mapper in the diyservo (yet.)
  }

  @Override
  public Double moveToBlocking(Double pos) {
    return moveToBlocking(pos, null);
  }

  @Override
  public Double moveToBlocking(Double pos, Long timeoutMs) {
    // TODO : implement a timed out blocking move.
    this.moveTo(pos);
    //TODO: block until we get there!
    return null;
  }

  @Override
  public void rest() {
    // ok. move to base class.
    moveTo(rest);
  }

  @Override
  public void setMapper(Mapper m) {
    // TODO Do we actually need a mapper?  
  }

  @Override
  public void setMinMax(double minXY, double maxXY) {
    // TODO: implement this.. for safty limits.. dont support a move call outside the specified range.
    // we don't even have a mapper.. we dont' need one..we might want a mapper that gives us a phase shift.
    // but reality is. that should be handled by the encoder.
  }

  @Override
  public void setMinMaxOutput(double minY, double maxY) {
    // TODO: implement this.. for safty limits.. dont support a move call outside the specified range.
  }

  @Override
  public void setPin(Integer pin) {
    // TODO: There are no pins!  we have no pins! perhaps this could be the pin that the encoder is connected to?
    log.warn("setPin not implemented in DiyServo.");
  }

  @Override
  public void setPin(String pin) {
    // TODO: remove from interface?  We don't have any pins.
    log.warn("setPin not implemented in DiyServo.");
  }

  @Override
  public void setPosition(double pos) {
    // TODO: maybe deprecate and  remove from interface ?
    // This method had a strange functionality of setting a position
    // even though the servo wasn't attached / enabled?
    moveTo(pos);
  }

  @Override
  public void setRest(double rest) {
    // TODO move to base class
    this.rest = rest;
  }

  @Override
  public void setSpeed(Double degreesPerSecond) {
    // TODO: velocity control.
  }

  @Override
  public void stop() {
    // Stop the motor.
    motorControl.move(0.0);
  }

  @Override
  public void sync(ServoControl sc) {
    // TODO Impl me.
  }

  @Override
  public void unsync(ServoControl sc) {
    // TODO Impl me    
  }

  @Override
  public void waitTargetPos() {
    // TODO: here we should wait until we have "arrived"  ...
  }

  @Override
  public void writeMicroseconds(int uS) {
    // NoOp here... should be removed from ServoControl interface.. this is specific to a pwm controlled servo
    log.warn("Write Microseconds not implemented for DiyServo.");
  }

  @Override
  public void fullSpeed() {
    // TODO: add a velocity control.
    // TODO: deprecated, remove from interface?
    // This would disable any velocity control for the servo.
  }

  @Override
  public ServoControl publishMoveTo(ServoControl sc) {
    return sc;
  }

  @Override
  public ServoControl publishServoStop(ServoControl sc) {
    return sc;
  }

  @Override
  public Double moveToBlocking(Integer newPos) {
    // TODO this should get implemented for certain.
    return null;
  }

  @Override
  public Double moveToBlocking(Integer newPos, Long timeoutMs) {
    // TODO this should get implemented for certain.
    return null;
  }

  @Override
  public ServoEvent publishServoStarted(String name, Double position) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ServoEvent publishServoStopped(String name, Double position) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ServoMove publishServoMoveTo(ServoMove pos) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String publishServoEnable(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void attachServoControlListener(String name) {
    // TODO: this should get implemented like the normal Servo
    
  }

  @Override
  public void setAutoDisable(boolean autoDisable) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setInverted(boolean invert) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setSpeed(Integer degreesPerSecond) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void sync(String name) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void unsync(String name) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void attachServoController(String sc) {
   // NoOp for DiyServo2, the Motor Control and the Encoder will have their own controllers...
    log.info("DiyServo2 doesn't use attachServoController");
  }

  @Override
  public void setMaxSpeed() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Double moveTo(Integer newPos) {
    // TODO Auto-generated method stub
    return moveTo(Double.valueOf(newPos));
  }

  @Override
  public ServoSpeed publishServoSetSpeed(ServoControl sc) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String publishServoEnable(ServoControl sc) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String publishServoDisable(ServoControl sc) {
    // TODO Auto-generated method stub
    return null;
  }

  
  public static void main(String[] args) throws Exception {

    LoggingFactory.init("info");
    
    Runtime.start("python", "Python");
    WebGui webgui = (WebGui)Runtime.start("webgui", "WebGui");
   
    // Compose the components of the diy servo and attach them.
    // Make one.. and stuff.
    // setup the encoder.
    Arduino ard = (Arduino)Runtime.start("ard", "Arduino");
    ard.connect("COM3");
    // ard.setDebug(true);
    As5048AEncoder encoder = (As5048AEncoder) Runtime.start("encoder", "As5048AEncoder");
    encoder.setPin(10);
    ard.attach(encoder);
    // setup the motor.
    // encoder.ttach
    MotorDualPwm mot = (MotorDualPwm) Runtime.start("diyServo.motor", "MotorDualPwm");
    mot.setPwmPins(6, 7);
    ard.attach(mot);
    // TODO: attach both to the diyservo and set the pin.
    
    
    
    if (true) {
      // What else do we need?
      DiyServo2 diy = (DiyServo2)Runtime.start("diy", "DiyServo2");
      // attach the encoder and motor to the diy servo.
      diy.attachEncoderControl(encoder);
      diy.attachMotorControl(mot);

      // Now we can move it??



      // attach the encoder and motor
      //  diy.attachEncoderControl(encoder);
      //  diy.attachMotorControl(mot);
      // Tell the servo to move somewhere.

     // diy.moveTo(75.0);
      //Thread.sleep(2000);
      // diy.disable();
      // Thread.sleep(1000);
      // diy.enable();
      diy.moveTo(250.0);
    }
    System.out.println("Press the any key");
    System.in.read();

  }
  
  
  
}