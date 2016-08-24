#ifndef MrlComm_h
#define MrlComm_h

#include "ArduinoMsgCodec.h"
#include "MrlMsg.h"
#include "MrlCmd.h"
#include "LinkedList.h"
#include "MrlServo.h"
#include "Device.h"
#include "MrlI2cBus.h"
#include "MrlNeopixel.h"
#include "Pin.h"

// TODO - standard convention of dev versions are odd release is even ?
#define MRLCOMM_VERSION         37

/***********************************************************************
 * Class MrlComm -
 * This class represents the Arduino service as a device.
 * It can hosts devices such as Motors, Servos, Steppers, Sensors, etc.
 * You can dynamically add or remove devices, and the deviceList should be in
 * synch with the Java-Land deviceList.
 * It has a list of pins which can be read from or written to.
 * It also follows some of the same methods as the Device in Device.h
 * It has an update() which is called each loop to do any necessary processing
 * 
*/
class MrlComm{
  private:
    /**
     * "global var"
     */
    // The mighty device List.  This contains all active devices that are attached to the arduino.
    LinkedList<Device*> deviceList;

    // list of pins currently being read from - can contain both digital and analog
    LinkedList<Pin*> pinList;

    // MRLComm message buffer and current count from serial port ( MAGIC | MSGSIZE | FUNCTION | PAYLOAD ...
    //unsigned char ioCmd[MAX_MSG_SIZE];  // message buffer for all inbound messages
    unsigned char* config;
    // performance metrics  and load timing
    // global debug setting, if set to true publishDebug will write to the serial port.
    bool debug;
    int byteCount;
    int msgSize;
    bool enableBoardStatus;
    unsigned int publishBoardStatusModulus; // the frequency in which to report the load timing metrics (in number of main loops)
    unsigned long lastMicros; // timestamp of last loop (if stats enabled.)
#if defined(ARDUINO_AVR_MEGA2560) || defined(ARDUINO_AVR_ADK)
    MrlCmd* mrlCmd[4];
#else
    MrlCmd* mrlCmd[1];
#endif
    bool heartbeat;
    bool heartbeatEnabled;
    unsigned long lastHeartbeatUpdate;
    void softReset();
    int getFreeRam();
    void publishError(int type);
    void publishError(int type, String message);
    void publishCommandAck(int function);
    void publishAttachedDevice(int id, int nameSize, unsigned char* name);
    void setPWMFrequency(int address, int prescalar);
    void setSerialRate();
    void deviceAttach(unsigned char* ioCmd);
    void deviceDetach(int id);
    Device* getDevice(int id);
    void addDevice(Device* device);
    void update();

  public:
    unsigned long loopCount; // main loop count
    MrlComm();
    ~MrlComm();
    void publishBoardStatus();
    void publishVersion();
    void publishBoardInfo();
    void readCommand();
    void processCommand(int ioType);
    void updateDevices();
};

#endif
