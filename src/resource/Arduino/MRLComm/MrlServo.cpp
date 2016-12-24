<<<<<<< HEAD
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
  currentPos = 0.0;
  targetPos = 0;
  velocity = -1;
  acceleration = 1;
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
bool MrlServo::attach(byte pin, byte initPos, int initVelocity){
  // msg->publishDebug("MrlServo.deviceAttach !!!");
  servo->write(initPos);
  currentPos = initPos;
  targetPos = initPos;
  velocity = initVelocity;
  servo->attach(pin);
  return true;
}

// This method is equivalent to Arduino's Servo.attach(pin) - (no pos)
void MrlServo::attachPin(int pin){
  this->pin = pin;
  servo->attach(pin);
  servo->write((int)currentPos); //return to it's last know state (may be 0 if currentPos is not set)
  // TODO-KW: we should always have a moveTo for safety, o/w we have no idea what angle we're going to start up at.. maybe
}

void MrlServo::detachPin(){
  servo->detach();
}

// FIXME - what happened to events ?
void MrlServo::update() {
  //it may have an imprecision of +- 1 due to the conversion of currentPos to int
  if (isMoving) {
    if ((int)currentPos != targetPos) {
      long deltaTime = millis() - lastUpdate;
      float _velocity = velocity;
      if (acceleration != -1) {
        _velocity *= acceleration * pow(((float)(millis()- moveStart)) / 1000,2) / 10;
        //msg->publishDebug(String(_velocity));
        if (_velocity > velocity) {
          _velocity = velocity;
        }
      }
      float step = _velocity * deltaTime;
      step /= 1000; //for deg/ms;
      if (isSweeping) {
        step = sweepStep;
      }
      if (velocity < 0) { // when velocity < 0, it mean full speed ahead
        step = targetPos - currentPos;
      }
      else if ((int)currentPos > targetPos) {
        step *=-1;
      }
      currentPos += step;
      if ((step > 0.0 && (int)currentPos > targetPos) || (step < 0.0 && (int)currentPos < targetPos)) {
        currentPos = targetPos;
      }
      lastUpdate = millis();
      servo->write((int)currentPos);
    }
    else {
      if (isSweeping) {
        if (targetPos == min) {
          targetPos = max;
        }
        else {
          targetPos = min;
        }
        sweepStep *= -1;
      }
      else {
        isMoving = false;
      }
    }
  }
}

void MrlServo::servoWrite(int position) {
  if (servo == NULL) 
    return;
  targetPos = position;
  isMoving = true;
  lastUpdate = millis();
  moveStart = lastUpdate;
}

void MrlServo::servoWriteMicroseconds(int position) {
  if (servo) {
    servo->writeMicroseconds(position);
  }
}

void MrlServo::startSweep(int min, int max, int step) {
  this->min = min;
  this->max = max;
  sweepStep = step;
  targetPos = max;
  isMoving = true;
  isSweeping = true;
}

void MrlServo::stopSweep() {
  isMoving = false;
  isSweeping = false;
}

void MrlServo::setMaxVelocity(unsigned int velocity){
  maxVelocity = velocity;
}

void MrlServo::setVelocity(int velocity) {
  this->velocity = velocity;
}

void MrlServo::setAcceleration(int acceleration) {
  this->acceleration = acceleration;
}
=======
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
  currentPos = 0.0;
  targetPos = 0;
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
bool MrlServo::attach(byte pin, byte initPos, int initVelocity){
  // msg->publishDebug("MrlServo.deviceAttach !!!");
  servo->write(initPos);
  currentPos = initPos;
  targetPos = initPos;
  velocity = initVelocity;
  servo->attach(pin);
  return true;
}

// This method is equivalent to Arduino's Servo.attach(pin) - (no pos)
void MrlServo::enablePwm(int pin){
  this->pin = pin;
  servo->attach(pin);
  servo->write((int)currentPos); //return to it's last know state (may be 0 if currentPos is not set)
  // TODO-KW: we should always have a moveTo for safety, o/w we have no idea what angle we're going to start up at.. maybe
}

void MrlServo::disablePwm(){
  servo->detach();
}

// FIXME - what happened to events ?
void MrlServo::update() {
  //it may have an imprecision of +- 1 due to the conversion of currentPos to int
  if (isMoving) {
    if ((int)currentPos != targetPos) {
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
      float step = _velocity * deltaTime;
      step /= 1000; //for deg/ms;
      if (isSweeping) {
        step = sweepStep;
      }
      if (velocity < 0) { // when velocity < 0, it mean full speed ahead
        step = targetPos - currentPos;
      }
      else if ((int)currentPos > targetPos) {
        step *=-1;
      }
      currentPos += step;
      if ((step > 0.0 && (int)currentPos > targetPos) || (step < 0.0 && (int)currentPos < targetPos)) {
        currentPos = targetPos;
      }
      servo->write((int)currentPos);
    }
    else {
      if (isSweeping) {
        if (targetPos == min) {
          targetPos = max;
        }
        else {
          targetPos = min;
        }
        sweepStep *= -1;
      }
      else {
        isMoving = false;
      }
    }
  }
}

void MrlServo::servoWrite(int position) {
  if (servo == NULL) 
    return;
  targetPos = position;
  isMoving = true;
  lastUpdate = millis();
  moveStart = lastUpdate;
}

void MrlServo::servoWriteMicroseconds(int position) {
  if (servo) {
    servo->writeMicroseconds(position);
  }
}

void MrlServo::startSweep(int min, int max, int step) {
  this->min = min;
  this->max = max;
  sweepStep = step;
  targetPos = max;
  isMoving = true;
  isSweeping = true;
}

void MrlServo::stopSweep() {
  isMoving = false;
  isSweeping = false;
}

void MrlServo::setMaxVelocity(unsigned int velocity){
  maxVelocity = velocity;
}

void MrlServo::setVelocity(int velocity) {
  this->velocity = velocity;
}

void MrlServo::setAcceleration(int acceleration) {
  this->acceleration = acceleration;
}
>>>>>>> branch 'develop' of https://github.com/MyRobotLab/myrobotlab.git

