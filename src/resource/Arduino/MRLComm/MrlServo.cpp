#include "MrlServo.h"

MrlServo::MrlServo() : Device(DEVICE_TYPE_SERVO) {
  isMoving = false;
  isSweeping = false;
  // create the servo
  servo = new Servo();
  lastUpdate = 0;
  currentPos = 0.0;
  targetPos = 0;
  velocity = 0;
}

MrlServo::~MrlServo() {
  if (servo){
    servo->detach();
    delete servo;
  }
}

// this method "may" be called with a pin or pin & pos depending on
// config size
bool MrlServo::deviceAttach(unsigned char config[], int configSize){
  if (configSize < 1 || configSize > 4){
    MrlMsg msg(PUBLISH_MRLCOMM_ERROR);
    msg.addData(ERROR_DOES_NOT_EXIST);
    msg.addData(String(F("MrlServo invalid attach config size")));
    return false;
  }
  attachDevice();
  pin = config[0];
  if (configSize == 2) {
    velocity = 0;
    //servoWrite(config[1]);
    servo->write(config[1]);
    currentPos = config[1];
    targetPos = config[1];
  }
  else if (configSize == 4) {
    velocity = MrlMsg::toInt(config,2);
    //servoWrite(config[1]);
    servo->write(config[1]);
    currentPos = config[1];
    targetPos = config[1];
  }
  servo->attach(pin);
  return true;
}

// This method is equivalent to Arduino's Servo.attach(pin) - (no pos)
void MrlServo::attach(int pin){
  servo->attach(pin);
  servo->write((int)currentPos); //return to it's last know state (may be 0 if currentPos is not set)
  // TODO-KW: we should always have a moveTo for safety, o/w we have no idea what angle we're going to start up at.. maybe
}

void MrlServo::detach(){
  servo->detach();
}

void MrlServo::update() {
  //it may have an imprecision of +- 1 due to the conversion of currentPos to int
  if (isMoving) {
    if ((int)currentPos != targetPos) {
      long deltaTime = millis() - lastUpdate;
      float step = velocity * deltaTime;
      step /= 1000; //for deg/ms;
      if (isSweeping) {
        step = sweepStep;
      }
      if (velocity == 0) { // when velocity == 0, it mean full speed ahead
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

void MrlServo::setVelocity(unsigned int velocity) {
  this->velocity = velocity;
}

