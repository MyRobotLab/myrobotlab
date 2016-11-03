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
  private:
    Servo* servo; // servo pointer - in case our device is a servo
    int pin;
    bool isMoving;
    bool isSweeping;
    int targetPos;
    float currentPos;
    int min;
    int max;
    unsigned long lastUpdate;
    unsigned int velocity; // in deg/sec
    int sweepStep;
    unsigned int maxVelocity;
  public:
    MrlServo();
    ~MrlServo();
    bool deviceAttach(unsigned char config[], int configSize);
    void attach(int pin);
    void detach();
    void update();
    void servoWrite(int position);
    void servoWriteMicroseconds(int position);
    void startSweep(int min, int max, int step);
    void stopSweep();
    void setMaxVelocity(unsigned int velocity);
    void setVelocity(unsigned int velocity);
};

#endif
