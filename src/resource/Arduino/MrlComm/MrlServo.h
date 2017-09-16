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
    int targetPosUs;
    float currentPosUs;
    int minUs;
    int maxUs;
    unsigned long lastUpdate;
    int velocity; // in deg/sec  |  velocity < 0 == no speed control
    int sweepStep;
    int acceleration;
    unsigned long moveStart;

  public:
    MrlServo(int deviceId);
    ~MrlServo();
    bool attach(byte pin, int initPos, int initVelocity);
    void attachPin(int pin);
    void detachPin();
    void update();
    // with speed control
    void moveToMicroseconds(int position);
    // without speed control ? necessary ?
    // void servoWriteMicroseconds(int position);
    void startSweep(int minUs, int maxUs, int step);
    void stopSweep();
    void setMaxVelocity(unsigned int velocity);
    void setVelocity(int velocity);
    void setAcceleration(int acceleration);
    void publishServoEvent(int type);
};

#endif
