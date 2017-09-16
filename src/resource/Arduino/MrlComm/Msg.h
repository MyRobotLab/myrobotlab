/**
 * <pre>
 *
	Welcome to Msg.h
	Its created by running ArduinoMsgGenerator
	which combines the MrlComm message schema (src/resource/Arduino/generate/arduinoMsg.schema)
	with the cpp template (src/resource/Arduino/generate/Msg.template.cpp)

	IDL Type Conversions

	IDL        ARDUINO					Java							Range
	none		byte/unsigned char		int (cuz Java byte bites)		1 byte - 0 to 255
	boolean		boolean					boolean							0 1
    b16			int						int (short)						2 bytes	-32,768 to 32,767
    b32			long					int								4 bytes -2,147,483,648 to 2,147,483, 647
    bu32		unsigned long			long							0 to 4,294,967,295
    str			char*, size				String							variable length
    []			byte[], size			int[]							variable length

	All message editing should be done in the arduinoMsg.schema

	The binary wire format of an Arduino is:

	MAGIC_NUMBER|MSG_SIZE|METHOD_NUMBER|PARAM0|PARAM1 ...

</pre>
*/

#ifndef Msg_h
#define Msg_h

#include <Arduino.h>
#include "ArduinoMsgCodec.h"

// forward defines to break circular dependency
class MrlComm;

class Msg {

public:
	bool debug = false;

private:
	// msg reading FIXME - rename recvBuffer
	byte ioCmd[MAX_MSG_SIZE];

	int byteCount = 0;
	int msgSize = 0;

	int sendBufferSize = 0;
	byte sendBuffer[MAX_MSG_SIZE];

	// serial references
	HardwareSerial* serial;

	// heartbeat
	bool heartbeat;
	unsigned long lastHeartbeatUpdate;

	// implements callback
	MrlComm* mrlComm;

	// my singlton instance
	static Msg* instance;

    // private constructor
    Msg();

public:
    ~Msg();
    static Msg* getInstance(MrlComm* mrlComm);
    static Msg* getInstance();

    // send buffering methods
    void add(const int value);
    void add16(const int value);
    void add(unsigned long value);
    byte* getBuffer();
    int getBufferSize();
    void reset();
    void flush();

    // utility methods
    static int b16(const unsigned char* buffer, const int start = 0);
    static long b32(const unsigned char* buffer, const int start = 0);
    static unsigned long bu32(const unsigned char* buffer, const int start = 0);
    static float f32(const unsigned char* buffer, const int start = 0);

    // FIXME - remove publishBoardInfo() .. its generated
    static void publishBoardInfo();
    void publishError(const String& message);
    void publishDebug(const String& message);

	// generated send (PC <-- MrlComm) methods
	void publishMRLCommError(const char* errorMsg,  byte errorMsgSize);
	void publishBoardInfo( byte version,  byte boardType,  int microsPerLoop,  int sram,  byte activePins, const byte* deviceSummary,  byte deviceSummarySize);
	void publishAck( byte function);
	void publishEcho( float myFloat,  byte myByte,  float secondFloat);
	void publishCustomMsg(const byte* msg,  byte msgSize);
	void publishI2cData( byte deviceId, const byte* data,  byte dataSize);
	void publishDebug(const char* debugMsg,  byte debugMsgSize);
	void publishPinArray(const byte* data,  byte dataSize);
	void publishServoEvent( byte deviceId,  byte eventType,  int currentPos,  int targetPos);
	void publishSerialData( byte deviceId, const byte* data,  byte dataSize);
	void publishUltrasonicSensorData( byte deviceId,  int echoTime);

	// handles all (PC --> MrlComm) methods
	// void handle(int[] ioCmd); // send size too ?
	void processCommand();

	// io
	void begin(HardwareSerial& hardwareSerial);
	void write(const unsigned char value);
	void writebool(const bool value);
	void writeb16(const int value);
	void writeb32(const long value);
	void writebu32(const unsigned long value);
	void writef32(const float value);
	void write(const unsigned char* buffer, int len);
	bool readMsg();
	byte getMethod();

};

#endif // Mrl_h
