#ifndef MrlComm_h
#define MrlComm_h

#include "Arduino.h"
#include "ArduinoMsgCodec.h"
#include "MrlMsg.h"
#include "LinkedList.h"
#include "MrlServo.h"
#include "Device.h"
#include "MrlI2cBus.h"
#include "MrlNeopixel.h"

/**
* FIXME - first rule of generate club is: whole file should be generated
* so this needs to be turned itno a .h if necessary - but the manual munge
* should be replaced
*
* Addendum up for vote:
*   Second rule of generate club is , to complete the mission, this file must/should go away...
*   It should be generated, completely.  device subclasses, #defines and all..  muahahahhah! project mayhem...
*
*   Third rule of generate club is, if something has no code and isn't used, remove it. If it has code, move the code.
*
*/

// TODO: this isn't ready for an official bump to mrl comm 35
// when it's ready we can update ArduinoMsgCodec  (also need to see why it's not publishing "goodtimes" anymore.)
#define MRLCOMM_VERSION         37

/***********************************************************************
 * Class MrlComm
 * 
*/
class MrlComm{
  private:
    /**
     * "global var"
     */
    // The mighty device List.  This contains all active devices that are attached to the arduino.
    LinkedList<Device*> deviceList;
    // MRLComm message buffer and current count from serial port ( MAGIC | MSGSIZE | FUNCTION | PAYLOAD ...
    unsigned char ioCmd[MAX_MSG_SIZE];  // message buffer for all inbound messages
    unsigned char* config;
    // performance metrics  and load timing
    // global debug setting, if set to true publishDebug will write to the serial port.
    bool debug;
    int byteCount;
    int msgSize;
    bool loadTimingEnabled;
    int loadTimingModulus; // the frequency in which to report the load timing metrics (in number of main loops)
    unsigned long lastMicros; // timestamp of last loop (if stats enabled.)
    void softReset();
    int getFreeRam();
    void publishDebug(String message);
    void publishError(int type);
    void publishError(int type, String message);
    void publishCommandAck();
    void publishAttachedDevice(int id, int nameSize, int namePos);
    void publishStatus(unsigned long loadTime, int freeMemory);
    bool getCommand();
    void processCommand();
    void setPWMFrequency(int address, int prescalar);
    void setSerialRate();
    void deviceAttach();
    void deviceDetach(int id);
    Device* getDevice(int id);
    void addDevice(Device* device);
  public:
    unsigned long loopCount; // main loop count
    MrlComm();
    void updateStatus();
    void publishVersion();
    void publishBoardInfo();
    void readCommand();
    void updateDevices();
};

#endif
