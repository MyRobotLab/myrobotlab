package org.myrobotlab.arduino.virtual;

import org.myrobotlab.arduino.Msg;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.VirtualArduino;
import org.myrobotlab.service.interfaces.Simulator;
import org.myrobotlab.virtual.VirtualServo;
import org.slf4j.Logger;


/**
 * copy pasted from MrlServo.h
 *
 */
public class MrlServo extends Device {
  
  public final static Logger log = LoggerFactory.getLogger(MrlServo.class);

	public static final int SERVO_EVENT_STOPPED = 1;
	public static final int SERVO_EVENT_POSITION_UPDATE = 2;
	
  VirtualServo servo; // servo pointer - in case our device is a servo
  public int pin;
  public boolean isMoving;
  boolean isSweeping;
  public int targetPosUs;
  public float currentPosUs;
  public int minUs;
  public int maxUs;
  public long lastUpdate;
  public int velocity; // in deg/sec  |  velocity < 0 == no speed control
  public int sweepStep;
  public int acceleration;
  public long moveStart;

  public MrlServo(int deviceId, VirtualArduino virtual) {
    super(deviceId, Msg.DEVICE_TYPE_SERVO, virtual);
  isMoving = false;
  isSweeping = false;
  // create the servo
  servo = new HardwareServo();
  lastUpdate = 0;
  currentPosUs = 0.0f;
  targetPosUs = 0;
  velocity = -1;
  acceleration = -1;
  moveStart = 0;
  }

/*
MrlServo::~MrlServo() {
  if (servo){
    servo->detach();
    delete servo;
  }
}
*/
  
// this method "may" be called with a pin or pin & pos depending on
// config size
boolean attach(int pin, int initPosUs, int initVelocity, String name){
      // msg->publishDebug("MrlServo.deviceAttach !");
  Simulator sim = virtual.getSimulator();
  if (sim != null){
    servo = sim.createVirtualServo(name);
  } else {
    servo = new HardwareServo();
  }
  servo.writeMicroseconds(initPosUs);
  currentPosUs = initPosUs;
  targetPosUs = initPosUs;
  velocity = initVelocity;
  servo.attach(pin);
  //publishServoEvent(SERVO_EVENT_STOPPED);
  return true;
  }

// This method is equivalent to Arduino's Servo.attach(pin) - (no pos)
public void attachPin(int pin) {
  log.info("servo id {}->attachPin({})", id, pin);
  this.pin = pin;
  servo.attach(pin);
}

public void detachPin() {
  log.info("servo id {}->detachPin()", id);
  servo.detach();
}

// FIXME - what happened to events ?
  public void update() {
    //it may have an imprecision of +- 1 due to the conversion of currentPosUs to int
    if (isMoving) {
      if ((int)currentPosUs != targetPosUs) {
        long deltaTime = millis() - lastUpdate;
        lastUpdate = millis();
        float _velocity = velocity;
        if (acceleration != -1) {
          _velocity *= acceleration * Math.pow(((float)(millis()- moveStart)) / 1000,2) / 10;
          //msg->publishDebug(String(_velocity));
          if (_velocity > velocity) {
            _velocity = velocity;
          }
        }
        if(targetPosUs > 500) {
          _velocity = map(_velocity, 0, 180, 544, 2400) - 544;
        }
        float step = _velocity * deltaTime;
        step /= 1000; //for deg/ms;
        if (isSweeping) {
          step = sweepStep;
        }
        if (velocity < 0) { // when velocity < 0, it mean full speed ahead
          step = targetPosUs - (int)currentPosUs;
        }
        else if (currentPosUs > targetPosUs) {
          step *=-1;
        }
        int previousCurrentPosUs = (int)currentPosUs;
        currentPosUs += step;
        if ((step > 0.0 && (int)currentPosUs > targetPosUs) || (step < 0.0 && (int)currentPosUs < targetPosUs)) {
          currentPosUs = targetPosUs;
        }
        if (!(previousCurrentPosUs == (int)currentPosUs)) {
          servo.writeMicroseconds((int)currentPosUs);
          if ((int)currentPosUs == targetPosUs) {
            publishServoEvent(SERVO_EVENT_STOPPED);
          }
          else {
            publishServoEvent(SERVO_EVENT_POSITION_UPDATE);
          }
        }
      }
      else {
        if (isSweeping) {
          if (targetPosUs == minUs) {
            targetPosUs = maxUs;
          }
          else {
            targetPosUs = minUs;
          }
          sweepStep *= -1;
        }
        else {
          isMoving = false;
          publishServoEvent(SERVO_EVENT_STOPPED);
        }
      }
    }
  }

 
 public void moveToMicroseconds(int posUs) {
    if (servo == null){ 
      return;
    }
    targetPosUs = posUs;
    isMoving = true;
    lastUpdate = millis();
    moveStart = lastUpdate;
    publishServoEvent(SERVO_EVENT_POSITION_UPDATE);
  }

void startSweep(int minUs, int maxUs, int step) {
  this.minUs = minUs;
  this.maxUs = maxUs;
  sweepStep = step;
  targetPosUs = maxUs;
  isMoving = true;
  isSweeping = true;
}

void stopSweep() {
  isMoving = false;
  isSweeping = false;
}

void setVelocity(int velocity) {
  this.velocity = velocity;
}
 
void setAcceleration(int acceleration) {
  this.acceleration = acceleration;
}

  private long millis() {
    return System.currentTimeMillis();
  }

  private void publishServoEvent(int type) {
    msg.publishServoEvent(id, type, (int)currentPosUs, targetPosUs);  
  }

  public void servoWriteMicroseconds(int position) {
  }

  private float map(float value, int inMin, int inMax, int outMin, int outMax) {
    return outMin + ((value - inMin) * (outMax - outMin)) / (inMax - outMax);
  }

};