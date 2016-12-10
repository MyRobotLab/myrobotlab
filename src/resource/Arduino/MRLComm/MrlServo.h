#ifndef MrlServo_h
#define MrlServo_h


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
    int velocity; // in deg/sec  |  velocity < 0 == no speed control
    int sweepStep;
    unsigned int maxVelocity;

  public:
    MrlServo(int deviceId);
    ~MrlServo();
    bool attach(byte pin, byte initPos, int initVelocity);
    void enablePwm(int pin);
    void disablePwm();
    void update();
    void servoWrite(int position);
    void servoWriteMicroseconds(int position);
    void startSweep(int min, int max, int step);
    void stopSweep();
    void setMaxVelocity(unsigned int velocity);
    void setVelocity(unsigned int velocity);
};

#endif
