#ifndef MrlComm_h
#define MrlComm_h

#include "ArduinoMsgCodec.h"
#include "MrlSerialRelay.h"
#if defined(ESP8266)
  #include <WebSocketsServer.h>
  extern "C" {
    #include "user_interface.h"
  }
#endif

// forward defines to break circular dependency
class Device;
class Msg;
class MrlComm;
class Pin;


/***********************************************************************
   * Class MrlComm - This class represents the Arduino service as a device. It
   * can hosts devices such as Motors, Servos, Steppers, Sensors, etc. You can
   * dynamically add or remove devices, and the deviceList should be in synch
   * with the Java-Land deviceList. It has a list of pins which can be read from
   * or written to. It also follows some of the same methods as the Device in
   * Device.h It has an update() which is called each loop to do any necessary
   * processing
 * 
*/
class MrlComm{
  private:
    /**
     * "global var"
     */
  // The mighty device List. This contains all active devices that are attached
  // to the arduino.
    LinkedList<Device*> deviceList;

  // list of pins currently being read from - can contain both digital and
  // analog
    LinkedList<Pin*> pinList;

    unsigned char* config;
    // performance metrics  and load timing
    // global debug setting, if set to true publishDebug will write to the serial port.
    int byteCount;
    int msgSize;

 // last time board info was published
  long lastBoardInfoUs;

    boolean boardStatusEnabled;
 
    unsigned long lastHeartbeatUpdate;

    byte customMsgBuffer[MAX_MSG_SIZE];
    int customMsgSize;

    // handles all messages to and from pc
    Msg* msg;

    bool heartbeatEnabled;

public:
    // utility methods
    int getFreeRam();
    Device* getDevice(int id);

    bool ackEnabled = true;

    Device* addDevice(Device* device);
    void update();

    // Below are generated callbacks controlled by
    // arduinoMsgs.schema
    // <generatedCallBacks>
	// > getBoardInfo
	void getBoardInfo();
	// > enablePin/address/type/b16 rate
	void enablePin( byte address,  byte type,  int rate);
	// > setDebug/bool enabled
	void setDebug( boolean enabled);
	// > setSerialRate/b32 rate
	void setSerialRate( long rate);
	// > softReset
	void softReset();
	// > enableAck/bool enabled
	void enableAck( boolean enabled);
	// > echo/f32 myFloat/myByte/f32 secondFloat
	void echo( float myFloat,  byte myByte,  float secondFloat);
	// > customMsg/[] msg
	void customMsg( byte msgSize, const byte*msg);
	// > deviceDetach/deviceId
	void deviceDetach( byte deviceId);
	// > i2cBusAttach/deviceId/i2cBus
	void i2cBusAttach( byte deviceId,  byte i2cBus);
	// > i2cRead/deviceId/deviceAddress/size
	void i2cRead( byte deviceId,  byte deviceAddress,  byte size);
	// > i2cWrite/deviceId/deviceAddress/[] data
	void i2cWrite( byte deviceId,  byte deviceAddress,  byte dataSize, const byte*data);
	// > i2cWriteRead/deviceId/deviceAddress/readSize/writeValue
	void i2cWriteRead( byte deviceId,  byte deviceAddress,  byte readSize,  byte writeValue);
	// > neoPixelAttach/deviceId/pin/b32 numPixels
	void neoPixelAttach( byte deviceId,  byte pin,  long numPixels);
	// > neoPixelSetAnimation/deviceId/animation/red/green/blue/b16 speed
	void neoPixelSetAnimation( byte deviceId,  byte animation,  byte red,  byte green,  byte blue,  int speed);
	// > neoPixelWriteMatrix/deviceId/[] buffer
	void neoPixelWriteMatrix( byte deviceId,  byte bufferSize, const byte*buffer);
	// > disablePin/pin
	void disablePin( byte pin);
	// > disablePins
	void disablePins();
	// > setTrigger/pin/triggerValue
	void setTrigger( byte pin,  byte triggerValue);
	// > setDebounce/pin/delay
	void setDebounce( byte pin,  byte delay);
	// > servoAttach/deviceId/pin/b16 initPos/b16 initVelocity/str name
	void servoAttach( byte deviceId,  byte pin,  int initPos,  int initVelocity,  byte nameSize, const char*name);
	// > servoAttachPin/deviceId/pin
	void servoAttachPin( byte deviceId,  byte pin);
	// > servoDetachPin/deviceId
	void servoDetachPin( byte deviceId);
	// > servoSetVelocity/deviceId/b16 velocity
	void servoSetVelocity( byte deviceId,  int velocity);
	// > servoSweepStart/deviceId/min/max/step
	void servoSweepStart( byte deviceId,  byte min,  byte max,  byte step);
	// > servoSweepStop/deviceId
	void servoSweepStop( byte deviceId);
	// > servoMoveToMicroseconds/deviceId/b16 target
	void servoMoveToMicroseconds( byte deviceId,  int target);
	// > servoSetAcceleration/deviceId/b16 acceleration
	void servoSetAcceleration( byte deviceId,  int acceleration);
	// > serialAttach/deviceId/relayPin
	void serialAttach( byte deviceId,  byte relayPin);
	// > serialRelay/deviceId/[] data
	void serialRelay( byte deviceId,  byte dataSize, const byte*data);
	// > ultrasonicSensorAttach/deviceId/triggerPin/echoPin
	void ultrasonicSensorAttach( byte deviceId,  byte triggerPin,  byte echoPin);
	// > ultrasonicSensorStartRanging/deviceId
	void ultrasonicSensorStartRanging( byte deviceId);
	// > ultrasonicSensorStopRanging/deviceId
	void ultrasonicSensorStopRanging( byte deviceId);
	// > setAref/b16 type
	void setAref( int type);
	// > motorAttach/deviceId/type/[] pins
	void motorAttach( byte deviceId,  byte type,  byte pinsSize, const byte*pins);
	// > motorMove/deviceId/pwr
	void motorMove( byte deviceId,  byte pwr);
	// > motorMoveTo/deviceId/pos
	void motorMoveTo( byte deviceId,  byte pos);
    // </generatedCallBacks>
    // end

  public:
    unsigned long loopCount; // main loop count
    MrlComm();
    ~MrlComm();
    void publishBoardStatus();
    void publishVersion();
    void publishBoardInfo();
    void processCommand();
    void processCommand(int ioType);
    void updateDevices();
    unsigned int getCustomMsg();
    int getCustomMsgSize();
    void begin(HardwareSerial& serial);
    bool readMsg();
    void onDisconnect();
    void sendCustomMsg(const byte* msg, byte size);
#if defined(ESP8266)
    void begin(WebSocketsServer& wsServer);
    void webSocketEvent(unsigned char num, WStype_t type, unsigned char* payload, unsigned int lenght);
#endif
};
  
#endif
