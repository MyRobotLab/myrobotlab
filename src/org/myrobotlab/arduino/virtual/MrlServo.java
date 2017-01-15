package org.myrobotlab.arduino.virtual;

import org.myrobotlab.arduino.Msg;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.VirtualArduino;
import org.slf4j.Logger;


/**
 * copy pasted from MrlServo.h
 *
 */
public class MrlServo extends Device {
  
  public final static Logger log = LoggerFactory.getLogger(MrlServo.class);

  // Servo* servo; // servo pointer - in case our device is a servo
  public int pin;
  boolean isMoving;
  boolean isSweeping;
  int targetPos;
  float currentPos;
  int min;
  int max;
  long lastUpdate;
  public int velocity; // in deg/sec | velocity < 0 == no speed control
  int sweepStep;
  int maxVelocity;

  public MrlServo(int deviceId) {
    super(deviceId, Msg.DEVICE_TYPE_SERVO);
  }

  public boolean attach(int pin, int initPos, int initVelocity) {
    this.pin = pin;
    this.currentPos = initPos;
    this.velocity = initVelocity;
    return true;
  }

  public void update() {
  }

  public void detachPin() {
    log.info("servo id {}->detachPin()", id);
  }

  public void attachPin(int pin) {
    log.info("servo id {}->attachPin({})", id, pin);
    this.pin = pin;
  }

  public void servoWrite(int position) {
    targetPos = position;
    isMoving = true;
    lastUpdate = millis();
  }

  private long millis() {
    return System.currentTimeMillis();
  }

  public void servoWriteMicroseconds(int position) {
  }

  public void startSweep(int min, int max, int step) {
  }

  public void stopSweep() {
  }

  public void setMaxVelocity(int velocity) {
    this.maxVelocity = velocity;
  }

  public void setVelocity(int velocity) {
    this.velocity = velocity;
  }

  public void setAcceleration(Integer acceleration) {
    // TODO Auto-generated method stub

  }

  public void moveToMicroseconds(int target) {
    // TODO Auto-generated method stub
    
  }
};

