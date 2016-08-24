#include "MrlServo.h"

MrlServo::MrlServo() : Device(DEVICE_TYPE_SERVO) {
   isMoving = false;
   isSweeping = false;
   speed = 100;// 100% speed
   // TODO: target/curent position?
   // create the servo
   servo = new Servo();
   lastUpdate = 0;
   currentPos = 0.0;
   targetPos = 0;
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
  if (configSize < 1 || configSize > 2){
    MrlMsg msg(PUBLISH_MRLCOMM_ERROR);
    msg.addData(ERROR_DOES_NOT_EXIST);
    msg.addData(String(F("MrlServo invalid attach config size")));
    return false;
  }
  attachDevice();
  pin = config[0];
  attach(pin);
  if (configSize == 2){
    targetPos = config[1];
    servo->write(targetPos);
    currentPos = targetPos;
  }
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
  if (lastUpdate+10>millis() || servo == NULL) 
    return;
  if (isMoving) {
    if ((int)currentPos != targetPos) {
      currentPos += step;
      if((step > 0.0 && (int)currentPos > targetPos) || (step < 0.0 && (int)currentPos < targetPos)) {
        currentPos=targetPos; 
      }
      servo->write((int)currentPos);
    } else {
      if (isSweeping) {
        if (targetPos == min) {
          targetPos = max;
        } else {
          targetPos = min;
        }
        step*=-1;
      } else {
        isMoving = false;
      }
    }
  }
  lastUpdate=millis();
}

void MrlServo::publishServoEvent(int eventType) {
  MrlMsg msg(PUBLISH_SERVO_EVENT,id);
  msg.addData(eventType);
  msg.addData((int)currentPos);
  msg.addData(targetPos);
  msg.sendMsg();
}

void MrlServo::servoWrite(int position) {
  if (servo == NULL) 
    return;
  if (speed == 100) {
    // move at regular/full 100% speed
    targetPos = position;
    isMoving = true;
    step=targetPos-(int)currentPos;
  } else if (speed < 100 && speed > 0) {
    targetPos = position;
    isMoving = true;
    //int baseSpeed=(int)(60.0/0.14); // deg/sec base on speed of HS805B servo 6V under no load //should be modifiable
    long delta=targetPos-(int)currentPos;
    float currentSpeed=(baseSpeed*speed)/100;
    long timeToReach=abs((delta))*1000/currentSpeed; // time to reach target in ms
    if(timeToReach==0){
      timeToReach=1;
    }
    step=((float)delta*10/timeToReach);
  }
}

void MrlServo::servoWriteMicroseconds(int position) {
  if (servo) {
    servo->writeMicroseconds(position);
  }
}

void MrlServo::setSpeed(int speed) {
  this->speed = speed;
}

void MrlServo::startSweep(int min, int max, int step) {
  this->min = min;
  this->max = max;
  this->step = step;
  targetPos = max;
  isMoving = true;
  isSweeping = true;
}

void MrlServo::stopSweep() {
  isMoving = false;
  isSweeping = false;
}

void MrlServo::setMaxVelocity(unsigned int velocity){
	baseSpeed = velocity;
}

