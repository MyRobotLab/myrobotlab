#ifndef MrlUltrasonicSensor_h
#define MrlUltrasonicSensor_h

#include "NewPing.h"

/**
 * Ultrasonic Sensor
 * TODO: add a description about this device, what is it? what does it do?
 * How does it work?
 */
class MrlUltrasonicSensor : public Device {
  public:

    unsigned int maxDistanceCm;
    bool isRanging = false;

    unsigned long lastDistance;

    NewPing* newping;

    MrlUltrasonicSensor(int deviceId);
    ~MrlUltrasonicSensor();

    void attach(byte trigPin, byte echoPin);
    void update();
    void startRanging();
    void stopRanging();
};

#endif
