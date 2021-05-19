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
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoControlPublisher;
import org.myrobotlab.service.interfaces.ServoController;
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

public class DiyServo2 extends Service implements EncoderListener, ServoControl, ServoControlPublisher, ServoStatusPublisher {

  private static final long serialVersionUID = 1L;
  private volatile boolean enabled = true;
  private MotorControl motorControl;
  private Double currentAngle;
  public Pid pid;

  //TODO: use the motor name.
  public String pidKey = "diy2";

  private double kp = 0.05;
  private double ki = 0.001; // 0.020;
  private double kd = 0.001; // 0.020;
  public double setPoint = 90.0; // Intial
  public int sampleTime = 20;
  static final public int MODE_AUTOMATIC = 1;
  
  MotorUpdater motorUpdater;
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
    initPid();
  }

  void initPid() {
    pid = (Pid) createPeer("pid");
    pidKey = this.getName();
    pid.setPID(pidKey, kp, ki, kd); // Create a PID with the name of this
    // service instance
    pid.setMode(pidKey, MODE_AUTOMATIC); // Initial mode is manual
    pid.setOutputRange(pidKey, -1.0, 1.0); // Set the Output range to match the Motor input
    pid.setSampleTime(pidKey, sampleTime); // Sets the sample time
    pid.setSetpoint(pidKey, setPoint);
    pid.startService();
  }
  
  @Override
  public void onEncoderData(EncoderData data) {
    // System.err.println("DIY Servo Encoder Data: " + data);
    this.currentAngle = data.angle;
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
      // log.info("Starting MotorUpdater");
      motorUpdater = new MotorUpdater(getName());
      motorUpdater.start();
      // log.info("MotorUpdater started");
    }

  }

  public boolean moveTo(Integer angle) {
    return moveTo(Double.valueOf(angle));
  }

  public boolean moveTo(Double angle) {
    // This updates the setpoint of the pid control.
    this.setPoint = angle;
    pid.setSetpoint(pidKey, angle);
    lastActivityTimeMS = System.currentTimeMillis();
    // Why does this return a boolean?
    return true;
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

      try {
        while (true) {
          if (isRunning()) {
            // Calculate the new value for the motor
            // TODO: this probably needs to be synchronized.
            if (pid.compute(pidKey)) {
              // Update the pid input value.
              pid.setInput(pidKey, currentAngle);
              double output = pid.getOutput(pidKey);
              double delta = Math.abs(currentAngle - setPoint);
              if (delta < threshold ) {
                log.info("Arrived!");
                // TODO: some debouncing logic here.
                // TODO: publish the servo events for started/stopped here.
                
              } else if (output != lastOutput) {
                log.info("move motor : Power: {}  Target: {}  Current: {}  Delta: {}", output, setPoint, currentAngle, delta);
                motorControl.move(output);
                lastOutput = output;
              }
            }
          }
          // TODO: maybe a more accurate loop timer?
          Thread.sleep(1000 / sampleTime);
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
    log.warn("Diy Servo doesn't use a controller..  no implemented.");
  }

  @Override
  public void detach(ServoController listener) {
    // TODO maybe remove from interface?  this service doesn't give a crapola about servo controllers.
    log.warn("Diy Servo doesn't use a controller..  no implemented.");    
  }

  @Override
  public String getController() {
    // TODO remove from interface?. we have no controller.
    log.warn("Diy Servo doesn't use a controller..  no implemented.");
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
    return setPoint;
  }

  @Override
  public double getTargetPos() {
    // This is the setPoint for the pid control.. the target position.  
    return setPoint;
  }

  @Override
  public boolean isBlocking() {
    // TODO What does this mean? should we remove this from the interface?
    return false;
  }

  @Override
  public Boolean isEnabled() {
    return enabled;
  }

  @Override
  public Boolean isInverted() {
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
  public void setAutoDisable(Boolean autoDisable) {
    // TODO ugh.  impl me.
  }

  @Override
  public void setInverted(Boolean invert) {
    // TODO: impl me.. this should probably control the pid  output * -1 ?
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
    // TODO: There are no pins!  we have no pins!
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
    // Stop the motor... anything else?
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
    // TODO Auto-generated method stub
    // really? ok.
    // here we should wait until we have "arrived"  ...
    
  }

  @Override
  public void writeMicroseconds(int uS) {
    // NoOp here... should be removed from ServoControl interface.. this is specific to a pwm controlled servo
    log.warn("Write Microseconds not implemented for DiyServo.");
  }

  @Override
  public void attachServoController(String sc, Integer pin, Double pos, Double speed) {
    // NoOp here.. should be removed from ServoControl interface
    log.warn("Attach Servo Controller not implemented for DiyServo.");
  }

  @Override
  public void fullSpeed() {
    // TODO: add a velocity control.
    // TODO: deprecated, remove from interface?
  }

  @Override
  public String publishServoStarted(String name) {
    return name;
  }

  @Override
  public String publishServoStopped(String name) {
    return name;
  }

  @Override
  public ServoControl publishServoMoveTo(ServoControl sc) {
    return sc;
  }

  @Override
  public ServoControl publishMoveTo(ServoControl sc) {
    return sc;
  }

  @Override
  public ServoControl publishServoSetSpeed(ServoControl sc) {
    return sc;
  }

  @Override
  public ServoControl publishServoEnable(ServoControl sc) {
    return sc;
  }

  @Override
  public ServoControl publishServoDisable(ServoControl sc) {
    return sc;
  }

  @Override
  public ServoControl publishServoStop(ServoControl sc) {
    return sc;
  }

  
  public static void main(String[] args) throws Exception {

    LoggingFactory.init("info");
    
    Runtime.start("gui", "SwingGui");
    Thread.sleep(1000);
    Runtime.start("python", "Python");
    Thread.sleep(1000);

    // Make one.. and stuff.
    // setup the encoder.
    Arduino ard = (Arduino)Runtime.start("ard", "Arduino");
    ard.connect("COM4");
    // ard.setDebug(true);
    As5048AEncoder encoder = (As5048AEncoder) Runtime.start("encoder", "As5048AEncoder");
    encoder.setPin(10);
    ard.attach(encoder);
    // setup the motor.
    // encoder.ttach
    MotorDualPwm mot = (MotorDualPwm) Runtime.start("diyServo.motor", "MotorDualPwm");
    mot.setPwmPins(6, 5);
    ard.attach(mot);
    // TODO: attach both to the diyservo and set the pin.
    DiyServo2 diy = (DiyServo2)Runtime.createAndStart("diy", "DiyServo2");
    // attach the encoder and motor
    diy.attachEncoderControl(encoder);
    diy.attachMotorControl(mot);
    // Tell the servo to move somewhere.
    
    diy.moveTo(75.0);
    Thread.sleep(2000);
//    diy.disable();
//    Thread.sleep(1000);
//    diy.enable();
    Thread.sleep(1000);
    diy.moveTo(125.0);
    
    System.out.println("Press the any key");
    System.in.read();

  }

}