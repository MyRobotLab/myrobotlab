#ifndef MrlServo_h
#define MrlServo_h

#include <Servo.h>
#include "Device.h"
#include "MrlMsg.h"

// servo event types
// ===== published sub-types based on device type begin ===
#define  SERVO_EVENT_STOPPED          1
#define  SERVO_EVENT_POSITION_UPDATE  2
// ===== published sub-types based on device type begin ===

/**
 * Servo Device
 */
class MrlServo : public Device {
  public:
    Servo* servo; // servo pointer - in case our device is a servo
    int pin;
    bool isMoving;
    bool isSweeping;
    int targetPos;
    float currentPos;
    int speed; // servos have a "speed" associated with them that's not part of the base servo driver
    bool eventsEnabled;
    float step;
    int min;
    int max;
    // TODO: remove this, the last update timestamp is 
    // computed at the end of the main loop once for all devices.
    // CAL:no, this is still need. We need to know when THIS device do it's last update operation. 
    //     calling update will check if it's time to do the update operations.
    //     ie: for the servo update, updating  the position is done each 10ms wich is more than enough considering
    //     the speed of the servo. but call to update will be more in the range of 10us depending of the load on the
    //     microcontroller
    unsigned long lastUpdate;
    MrlServo() : Device(DEVICE_TYPE_SERVO) {
       isMoving = false;
       isSweeping = false;
       speed = 100;// 100% speed
       // TODO: target/curent position?
       // create the servo
       servo = new Servo();
       eventsEnabled = false;
       lastUpdate = 0;
       currentPos = 0.0;
       targetPos = 0;
    }

    ~MrlServo() {
      if (servo){
        servo->detach();
        delete servo;
      }
    }

    // this method "may" be called with a pin or pin & pos depending on
    // config size
    bool deviceAttach(unsigned char config[], int configSize){
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
    void attach(int pin){
      servo->attach(pin);
      servo->write((int)currentPos); //return to it's last know state (may be 0 if currentPos is not set)
      // TODO-KW: we should always have a moveTo for safety, o/w we have no idea what angle we're going to start up at.. maybe
    }

    void detach(){
      servo->detach();
    }

    void update(unsigned long lastMicros) {
      if (lastUpdate+10>millis() || servo == NULL) 
        return;
      if (isMoving) {
        if ((int)currentPos != targetPos) {
          currentPos += step;
          if((step > 0.0 && (int)currentPos > targetPos) || (step < 0.0 && (int)currentPos < targetPos)) {
            currentPos=targetPos; 
          }
          servo->write((int)currentPos);
          if (eventsEnabled){
            publishServoEvent(SERVO_EVENT_POSITION_UPDATE);
          }
        } else {
          if (isSweeping) {
            if (targetPos == min) {
              targetPos = max;
            } else {
              targetPos = min;
            }
            step*=-1;
          } else {
            if (eventsEnabled)
              publishServoEvent(SERVO_EVENT_STOPPED);
            isMoving = false;
          }
        }
      }
      lastUpdate=millis();
    }
    
    // TODO: consider moving all Serial.write stuff out of device classes! -KW
    // I don't want devices knowing about the serial port directly.
    // consider a lifecycle where a device yeilds a message  to publish perhaps.
    // GR - this would be good - I've had issues with varargs and memory leaks before
    // perhaps as Mats mentioned - we can just supply a new array sendMsg(PUBLISH_THINGY, d0, d1, d(n));
    void publishServoEvent(int eventType) {
      MrlMsg msg(PUBLISH_SERVO_EVENT,id);
      msg.addData(eventType);
      msg.addData((int)currentPos);
      msg.addData(targetPos);
      msg.sendMsg();
    }
    
    void servoEventEnabled(int value) {
      eventsEnabled=value;
    }
    
    void servoWrite(int position) {
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
        int baseSpeed=(int)(60.0/0.14); // deg/sec base on speed of HS805B servo 6V under no load //should be modifiable
        long delta=targetPos-(int)currentPos;
        float currentSpeed=(baseSpeed*speed)/100;
        long timeToReach=abs((delta))*1000/currentSpeed; // time to reach target in ms
        if(timeToReach==0){
          timeToReach=1;
        }
        step=((float)delta*10/timeToReach);
      }
    }

    void servoWriteMicroseconds(int position) {
      if (servo) {
        servo->writeMicroseconds(position);
      }
    }

    void setSpeed(int speed) {
      this->speed = speed;
    }
    
    void startSweep(int min, int max, int step) {
      this->min = min;
      this->max = max;
      this->step = step;
      targetPos = max;
      isMoving = true;
      isSweeping = true;
    }
    
    void stopSweep() {
      isMoving = false;
      isSweeping = false;
    }

};

#endif
