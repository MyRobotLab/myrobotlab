#include <Servo.h>
#include "Msg.h"
#include "Device.h"
#include "MrlServo.h"

MrlServo::MrlServo(int deviceId) : Device(deviceId, DEVICE_TYPE_SERVO) {
  isMoving = false;
  isSweeping = false;
  // create the servo
  servo = new Servo();
  lastUpdate = 0;
  currentPosUs = 0.0;
  targetPosUs = 0;
  velocity = -1;
  acceleration = -1;
  moveStart = 0;
}

MrlServo::~MrlServo() {
  if (servo){
    servo->detach();
    delete servo;
  }
}

// this method "may" be called with a pin or pin & pos depending on
// config size
bool MrlServo::attach(byte pin, int initPosUs, int initVelocity){
  msg->publishDebug("MrlServo.attach !");
  // set the initial position for the servo.
  servo->writeMicroseconds(initPosUs);
  // current position is initial position
  currentPosUs = initPosUs;
  // we have arrived
  targetPosUs = initPosUs;
  // this is the velocity, however the servo will move to the initial position as fast as possible.
  velocity = initVelocity;
  this->pin = pin;
  return true;
}

// This method is equivalent to Arduino's Servo.attach(pin) - (no pos)
void MrlServo::attachPin(int pin){
  // attach the servo to the pin!
  servo->attach(pin);

}

void MrlServo::detachPin(){
  servo->detach();
}

// FIXME - what happened to events ?
void MrlServo::update() {
  //it may have an imprecision of +- 1 due to the conversion of currentPosUs to int
  if (isMoving) {
    if ((int)currentPosUs != targetPosUs) {
      // msg->publishDebug("UPDATE");
      long deltaTime = millis() - lastUpdate;
      lastUpdate = millis();
      float _velocity = velocity;
      if (acceleration != -1) {
        _velocity *= acceleration * pow(((float)(millis()- moveStart)) / 1000,2) / 10;
        //msg->publishDebug(String(_velocity));
        if (_velocity > velocity) {
          _velocity = velocity;
        }
      }
      if(targetPosUs > 500) {
        // target position less than 500 is considered an angle!
        // if the target position is greater than 500 we assume it's microseconds
        // and as a result, our velicity needs to be mapped from degrees to microseconds.
        _velocity = map(_velocity, 0, 180, 544, 2400) - 544;
      }
      float step = _velocity * deltaTime;
      step /= 1000; //for deg/ms;
      if (isSweeping) {
        step = sweepStep;
      }
      if (velocity < 0) {
    	// when velocity < 0, it mean full speed ahead
        step = targetPosUs - (int)currentPosUs;
      }
      if (currentPosUs > targetPosUs) {
        // check the direction of the update moving forward or backwards
        step *=-1;
      }
      int previousCurrentPosUs = (int)currentPosUs;
      currentPosUs += step;
      if ((step > 0.0 && (int)currentPosUs > targetPosUs) || (step < 0.0 && (int)currentPosUs < targetPosUs)) {
        // don't over shoot the target.
        currentPosUs = targetPosUs;
      }
      // There was a change in the currentPosition
      if (previousCurrentPosUs != (int)currentPosUs) {
        servo->writeMicroseconds((int)currentPosUs);
        // publishServoEvent(SERVO_EVENT_STARTED);
        // if we're not sweeping and we reached the target position., then we are stopped here.
        if (!isSweeping && ((int)currentPosUs == targetPosUs)) {
            publishServoEvent(SERVO_EVENT_STOPPED);
            isMoving = false;
        }
      }
    } else {
    	// current position is target position.
      // if we're sweeping, flip our target position
      if (isSweeping) {
        if (targetPosUs == minUs) {
          targetPosUs = maxUs;
        } else {
          targetPosUs = minUs;
        }
        sweepStep *= -1;
      } else {
    	// if we're not sweeping, we have arrived at our final destination.
        isMoving = false;
        publishServoEvent(SERVO_EVENT_STOPPED);
      }
    }
  }
}

void MrlServo::moveToMicroseconds(int posUs) {
  if (servo == NULL){ 
    return;
  }
  targetPosUs = posUs;
  isMoving = true;
  lastUpdate = millis();
  moveStart = lastUpdate;
  publishServoEvent(SERVO_EVENT_STARTED);
}

void MrlServo::startSweep(int minUs, int maxUs, int step) {
  this->minUs = minUs;
  this->maxUs = maxUs;
  sweepStep = step;
  targetPosUs = maxUs;
  isMoving = true;
  isSweeping = true;
  publishServoEvent(SERVO_EVENT_STARTED);
}

void MrlServo::stop() {
  isMoving = false;
  isSweeping = false;
  publishServoEvent(SERVO_EVENT_STOPPED);
}

void MrlServo::stopSweep() {
  isMoving = false;
  isSweeping = false;
  publishServoEvent(SERVO_EVENT_STOPPED);
}

void MrlServo::setVelocity(int velocity) {
  this->velocity = velocity;
}

void MrlServo::setAcceleration(int acceleration) {
  this->acceleration = acceleration;
}

void MrlServo::publishServoEvent(int type) {
  msg->publishServoEvent(id, type, (int)currentPosUs, targetPosUs);
}
