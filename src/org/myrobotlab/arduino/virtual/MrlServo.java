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

	public static final int SERVO_EVENT_STOPPED = 2;

  VirtualServo servo; // servo pointer - in case our device is a servo
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

  public MrlServo(int deviceId, VirtualArduino virtual) {
    super(deviceId, Msg.DEVICE_TYPE_SERVO, virtual);
  isMoving = false;
  isSweeping = false;
  // create the servo (check attach)

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
    targetPosUs = position;
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

  public void moveToMicroseconds(int posUs) {
    if (servo != null){
      servo.writeMicroseconds(posUs);
    }

  }
//  private void publishServoEvent(int type) {
//	// TODO Auto-generated method stub
//	
//}

};

