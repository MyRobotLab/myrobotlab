package org.myrobotlab.arduino.virtual;

import org.myrobotlab.arduino.Msg;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.VirtualArduino;
import org.myrobotlab.service.interfaces.Simulator;
import org.myrobotlab.virtual.VirtualMotor;
import org.slf4j.Logger;


/**
 * copy pasted from MrlMotor.h
 *
 */
public class MrlMotor extends Device {
  
  public final static Logger log = LoggerFactory.getLogger(MrlMotor.class);

	public static final int SERVO_EVENT_STOPPED = 1;
	public static final int SERVO_EVENT_POSITION_UPDATE = 2;
	
  VirtualMotor motor; // motor pointer - in case our device is a motor
  public  int pin;
  public  boolean isMoving;
  public  boolean isSweeping;
  public  int targetPosUs;
  public  float currentPosUs;
  public  int minUs;
  public  int maxUs;
  public  long lastUpdate;
  public  int velocity; // in deg/sec  |  velocity < 0 == no speed control
  public  int sweepStep;
  public  int maxVelocity;
  public  int acceleration;
  public  long moveStart;

  public MrlMotor(int deviceId, VirtualArduino virtual) {
    super(deviceId, Msg.DEVICE_TYPE_SERVO, virtual);
  isMoving = false;
  isSweeping = false;
  // create the motor (check attach)

  lastUpdate = 0;
  currentPosUs = 0.0f;
  targetPosUs = 0;
  velocity = -1;
  acceleration = -1;
  moveStart = 0;
  }

/*
MrlMotor::~MrlMotor() {
  if (motor){
    motor->detach();
    delete motor;
  }
}
*/
  
// this method "may" be called with a pin or pin & pos depending on
// config size
boolean attach(int pin, int initPosUs, int initVelocity, String name){
      // msg->publishDebug("MrlMotor.deviceAttach !");
  Simulator sim = virtual.getSimulator();
  if (sim != null){
    motor = sim.createVirtualMotor(name);
  } else {
    motor = new HardwareMotor();
  }
  motor.move(initPosUs);
  currentPosUs = initPosUs;
  targetPosUs = initPosUs;
  velocity = initVelocity;
  //motor.attach(pin);
  //publishMotorEvent(SERVO_EVENT_STOPPED);
  return true;
  }

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
          motor.moveTo((int)currentPosUs);
          if ((int)currentPosUs == targetPosUs) {
            publishMotorEvent(SERVO_EVENT_STOPPED);
          }
          else {
            publishMotorEvent(SERVO_EVENT_POSITION_UPDATE);
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
        }
      }
    }
  }

  public void detachPin() {
    log.info("motor id {}->detachPin()", id);
  }

  public void attachPin(int pin) {
    log.info("motor id {}->attachPin({})", id, pin);
    this.pin = pin;
  }

  public void motorWrite(int position) {
    targetPosUs = position;
    isMoving = true;
    lastUpdate = millis();
  }

  private long millis() {
    return System.currentTimeMillis();
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

  private void publishMotorEvent(int type) {
    // msg.publishMotorEvent(id, type, (int)currentPosUs, targetPosUs);
	
  }
  
  private float map(float value, int inMin, int inMax, int outMin, int outMax) {
    return outMin + ((value - inMin) * (outMax - outMin)) / (inMax - outMax);
  }

  public void move(int pwr) {
    
  }

  public void moveTo(Integer pos) {
    // TODO Auto-generated method stub
    
  }

};

