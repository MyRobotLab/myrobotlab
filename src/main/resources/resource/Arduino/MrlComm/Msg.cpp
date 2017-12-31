/**
 * <pre>
 *
 Welcome to Msg.java
 Its created by running ArduinoMsgGenerator
 which combines the MrlComm message schema (src/resource/Arduino/arduinoMsg.schema)
 with the cpp template (src/resource/Arduino/generate/Msg.template.cpp)

 	Schema Type Conversions

	Schema      ARDUINO					Java							Range
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

#include "Msg.h"
#include "LinkedList.h"
#include "MrlComm.h"

Msg* Msg::instance = NULL;

Msg::Msg() {
	this->mrlComm = mrlComm;
}

Msg::~Msg() {
}

// the two singleton methods - the one with the MrlComm paramters
// must be used for initialization
Msg* Msg::getInstance(MrlComm* mrlComm) {
	instance = new Msg();
	instance->mrlComm = mrlComm;
	return instance;
}

Msg* Msg::getInstance() {
	return instance;
}

/**
 * Expected Interface - these are the method signatures which will be called
 *  by Msg class
 *
 *    PC --serialized--> Msg --de-serialized--> MrlComm.method(parm0, param1, ...)
 *
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

 */


void Msg::publishMRLCommError(const char* errorMsg,  byte errorMsgSize) {
  write(MAGIC_NUMBER);
  write(1 + (1 + errorMsgSize)); // size
  write(PUBLISH_MRLCOMM_ERROR); // msgType = 1
  write((byte*)errorMsg, errorMsgSize);
  flush();
  reset();
}

void Msg::publishBoardInfo( byte version,  byte boardType,  int microsPerLoop,  int sram,  byte activePins, const byte* deviceSummary,  byte deviceSummarySize) {
  write(MAGIC_NUMBER);
  write(1 + 1 + 1 + 2 + 2 + 1 + (1 + deviceSummarySize)); // size
  write(PUBLISH_BOARD_INFO); // msgType = 3
  write(version);
  write(boardType);
  writeb16(microsPerLoop);
  writeb16(sram);
  write(activePins);
  write((byte*)deviceSummary, deviceSummarySize);
  flush();
  reset();
}

void Msg::publishAck( byte function) {
  write(MAGIC_NUMBER);
  write(1 + 1); // size
  write(PUBLISH_ACK); // msgType = 9
  write(function);
  flush();
  reset();
}

void Msg::publishEcho( float myFloat,  byte myByte,  float secondFloat) {
  write(MAGIC_NUMBER);
  write(1 + 4 + 1 + 4); // size
  write(PUBLISH_ECHO); // msgType = 11
  writef32(myFloat);
  write(myByte);
  writef32(secondFloat);
  flush();
  reset();
}

void Msg::publishCustomMsg(const byte* msg,  byte msgSize) {
  write(MAGIC_NUMBER);
  write(1 + (1 + msgSize)); // size
  write(PUBLISH_CUSTOM_MSG); // msgType = 13
  write((byte*)msg, msgSize);
  flush();
  reset();
}

void Msg::publishI2cData( byte deviceId, const byte* data,  byte dataSize) {
  write(MAGIC_NUMBER);
  write(1 + 1 + (1 + dataSize)); // size
  write(PUBLISH_I2C_DATA); // msgType = 19
  write(deviceId);
  write((byte*)data, dataSize);
  flush();
  reset();
}

void Msg::publishDebug(const char* debugMsg,  byte debugMsgSize) {
  write(MAGIC_NUMBER);
  write(1 + (1 + debugMsgSize)); // size
  write(PUBLISH_DEBUG); // msgType = 28
  write((byte*)debugMsg, debugMsgSize);
  flush();
  reset();
}

void Msg::publishPinArray(const byte* data,  byte dataSize) {
  write(MAGIC_NUMBER);
  write(1 + (1 + dataSize)); // size
  write(PUBLISH_PIN_ARRAY); // msgType = 29
  write((byte*)data, dataSize);
  flush();
  reset();
}

void Msg::publishServoEvent( byte deviceId,  byte eventType,  int currentPos,  int targetPos) {
  write(MAGIC_NUMBER);
  write(1 + 1 + 1 + 2 + 2); // size
  write(PUBLISH_SERVO_EVENT); // msgType = 40
  write(deviceId);
  write(eventType);
  writeb16(currentPos);
  writeb16(targetPos);
  flush();
  reset();
}

void Msg::publishSerialData( byte deviceId, const byte* data,  byte dataSize) {
  write(MAGIC_NUMBER);
  write(1 + 1 + (1 + dataSize)); // size
  write(PUBLISH_SERIAL_DATA); // msgType = 43
  write(deviceId);
  write((byte*)data, dataSize);
  flush();
  reset();
}

void Msg::publishUltrasonicSensorData( byte deviceId,  int echoTime) {
  write(MAGIC_NUMBER);
  write(1 + 1 + 2); // size
  write(PUBLISH_ULTRASONIC_SENSOR_DATA); // msgType = 47
  write(deviceId);
  writeb16(echoTime);
  flush();
  reset();
}


void Msg::processCommand() {

	int startPos = 0;
	int method = ioCmd[0];

	switch (method) {
	case GET_BOARD_INFO: { // getBoardInfo
			mrlComm->getBoardInfo();
			break;
	}
	case ENABLE_PIN: { // enablePin
			byte address = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte type = ioCmd[startPos+1]; // bu8
			startPos += 1;
			int rate = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			mrlComm->enablePin( address,  type,  rate);
			break;
	}
	case SET_DEBUG: { // setDebug
			boolean enabled = (ioCmd[startPos+1]);
			startPos += 1;
			mrlComm->setDebug( enabled);
			break;
	}
	case SET_SERIAL_RATE: { // setSerialRate
			long rate = b32(ioCmd, startPos+1);
			startPos += 4; //b32
			mrlComm->setSerialRate( rate);
			break;
	}
	case SOFT_RESET: { // softReset
			mrlComm->softReset();
			break;
	}
	case ENABLE_ACK: { // enableAck
			boolean enabled = (ioCmd[startPos+1]);
			startPos += 1;
			mrlComm->enableAck( enabled);
			break;
	}
	case ECHO: { // echo
			float myFloat = f32(ioCmd, startPos+1);
			startPos += 4; //f32
			byte myByte = ioCmd[startPos+1]; // bu8
			startPos += 1;
			float secondFloat = f32(ioCmd, startPos+1);
			startPos += 4; //f32
			mrlComm->echo( myFloat,  myByte,  secondFloat);
			break;
	}
	case CUSTOM_MSG: { // customMsg
			const byte* msg = ioCmd+startPos+2;
			byte msgSize = ioCmd[startPos+1];
			startPos += 1 + ioCmd[startPos+1];
			mrlComm->customMsg( msgSize, msg);
			break;
	}
	case DEVICE_DETACH: { // deviceDetach
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->deviceDetach( deviceId);
			break;
	}
	case I2C_BUS_ATTACH: { // i2cBusAttach
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte i2cBus = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->i2cBusAttach( deviceId,  i2cBus);
			break;
	}
	case I2C_READ: { // i2cRead
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte deviceAddress = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte size = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->i2cRead( deviceId,  deviceAddress,  size);
			break;
	}
	case I2C_WRITE: { // i2cWrite
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte deviceAddress = ioCmd[startPos+1]; // bu8
			startPos += 1;
			const byte* data = ioCmd+startPos+2;
			byte dataSize = ioCmd[startPos+1];
			startPos += 1 + ioCmd[startPos+1];
			mrlComm->i2cWrite( deviceId,  deviceAddress,  dataSize, data);
			break;
	}
	case I2C_WRITE_READ: { // i2cWriteRead
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte deviceAddress = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte readSize = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte writeValue = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->i2cWriteRead( deviceId,  deviceAddress,  readSize,  writeValue);
			break;
	}
	case NEO_PIXEL_ATTACH: { // neoPixelAttach
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			long numPixels = b32(ioCmd, startPos+1);
			startPos += 4; //b32
			mrlComm->neoPixelAttach( deviceId,  pin,  numPixels);
			break;
	}
	case NEO_PIXEL_SET_ANIMATION: { // neoPixelSetAnimation
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte animation = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte red = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte green = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte blue = ioCmd[startPos+1]; // bu8
			startPos += 1;
			int speed = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			mrlComm->neoPixelSetAnimation( deviceId,  animation,  red,  green,  blue,  speed);
			break;
	}
	case NEO_PIXEL_WRITE_MATRIX: { // neoPixelWriteMatrix
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			const byte* buffer = ioCmd+startPos+2;
			byte bufferSize = ioCmd[startPos+1];
			startPos += 1 + ioCmd[startPos+1];
			mrlComm->neoPixelWriteMatrix( deviceId,  bufferSize, buffer);
			break;
	}
	case ANALOG_WRITE: { // analogWrite
			byte pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte value = ioCmd[startPos+1]; // bu8
			startPos += 1;
			analogWrite( pin,  value);
			break;
	}
	case DIGITAL_WRITE: { // digitalWrite
			byte pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte value = ioCmd[startPos+1]; // bu8
			startPos += 1;
			digitalWrite( pin,  value);
			break;
	}
	case DISABLE_PIN: { // disablePin
			byte pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->disablePin( pin);
			break;
	}
	case DISABLE_PINS: { // disablePins
			mrlComm->disablePins();
			break;
	}
	case PIN_MODE: { // pinMode
			byte pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte mode = ioCmd[startPos+1]; // bu8
			startPos += 1;
			pinMode( pin,  mode);
			break;
	}
	case SET_TRIGGER: { // setTrigger
			byte pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte triggerValue = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->setTrigger( pin,  triggerValue);
			break;
	}
	case SET_DEBOUNCE: { // setDebounce
			byte pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte delay = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->setDebounce( pin,  delay);
			break;
	}
	case SERVO_ATTACH: { // servoAttach
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			int initPos = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			int initVelocity = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			const char* name = (char*)ioCmd+startPos+2;
			byte nameSize = ioCmd[startPos+1];
			startPos += 1 + ioCmd[startPos+1];
			mrlComm->servoAttach( deviceId,  pin,  initPos,  initVelocity,  nameSize, name);
			break;
	}
	case SERVO_ATTACH_PIN: { // servoAttachPin
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->servoAttachPin( deviceId,  pin);
			break;
	}
	case SERVO_DETACH_PIN: { // servoDetachPin
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->servoDetachPin( deviceId);
			break;
	}
	case SERVO_SET_VELOCITY: { // servoSetVelocity
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			int velocity = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			mrlComm->servoSetVelocity( deviceId,  velocity);
			break;
	}
	case SERVO_SWEEP_START: { // servoSweepStart
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte min = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte max = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte step = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->servoSweepStart( deviceId,  min,  max,  step);
			break;
	}
	case SERVO_SWEEP_STOP: { // servoSweepStop
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->servoSweepStop( deviceId);
			break;
	}
	case SERVO_MOVE_TO_MICROSECONDS: { // servoMoveToMicroseconds
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			int target = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			mrlComm->servoMoveToMicroseconds( deviceId,  target);
			break;
	}
	case SERVO_SET_ACCELERATION: { // servoSetAcceleration
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			int acceleration = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			mrlComm->servoSetAcceleration( deviceId,  acceleration);
			break;
	}
	case SERIAL_ATTACH: { // serialAttach
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte relayPin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->serialAttach( deviceId,  relayPin);
			break;
	}
	case SERIAL_RELAY: { // serialRelay
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			const byte* data = ioCmd+startPos+2;
			byte dataSize = ioCmd[startPos+1];
			startPos += 1 + ioCmd[startPos+1];
			mrlComm->serialRelay( deviceId,  dataSize, data);
			break;
	}
	case ULTRASONIC_SENSOR_ATTACH: { // ultrasonicSensorAttach
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte triggerPin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte echoPin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->ultrasonicSensorAttach( deviceId,  triggerPin,  echoPin);
			break;
	}
	case ULTRASONIC_SENSOR_START_RANGING: { // ultrasonicSensorStartRanging
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->ultrasonicSensorStartRanging( deviceId);
			break;
	}
	case ULTRASONIC_SENSOR_STOP_RANGING: { // ultrasonicSensorStopRanging
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->ultrasonicSensorStopRanging( deviceId);
			break;
	}
	case SET_AREF: { // setAref
			int type = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			mrlComm->setAref( type);
			break;
	}
	case MOTOR_ATTACH: { // motorAttach
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte type = ioCmd[startPos+1]; // bu8
			startPos += 1;
			const byte* pins = ioCmd+startPos+2;
			byte pinsSize = ioCmd[startPos+1];
			startPos += 1 + ioCmd[startPos+1];
			mrlComm->motorAttach( deviceId,  type,  pinsSize, pins);
			break;
	}
	case MOTOR_MOVE: { // motorMove
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte pwr = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->motorMove( deviceId,  pwr);
			break;
	}
	case MOTOR_MOVE_TO: { // motorMoveTo
			byte deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			byte pos = ioCmd[startPos+1]; // bu8
			startPos += 1;
			mrlComm->motorMoveTo( deviceId,  pos);
			break;
	}

		default:
		publishError("unknown method " + String(method));
		break;
	} // end switch
	  // ack that we got a command (should we ack it first? or after we process the command?)
	lastHeartbeatUpdate = millis();
} // process Command

void Msg::add(const int value) {
	sendBuffer[sendBufferSize] = (value & 0xFF);
	sendBufferSize += 1;
}

void Msg::add16(const int value) {
	sendBuffer[sendBufferSize] = ((value >> 8) & 0xFF);
	sendBuffer[sendBufferSize + 1] = (value & 0xFF);
	sendBufferSize += 2;
}

void Msg::add(unsigned long value) {
	sendBuffer[sendBufferSize] = ((value >> 24) & 0xFF);
	sendBuffer[sendBufferSize + 1] = ((value >> 16) & 0xFF);
	sendBuffer[sendBufferSize + 2] = ((value >> 8) & 0xFF);
	sendBuffer[sendBufferSize + 3] = (value & 0xFF);
	sendBufferSize += 4;
}

int Msg::b16(const byte* buffer, const int start/*=0*/) {
	return (buffer[start] << 8) + buffer[start + 1];
}

long Msg::b32(const byte* buffer, const int start/*=0*/) {
    long result = 0;
    for (int i = 0; i < 4; i++) {
        result <<= 8;
        result |= (buffer[start + i] & 0xFF);
    }
    return result;
}

float Msg::f32(const byte* buffer, const int start/*=0*/) {

	const byte * ptr = buffer + start;

    float newFloat = 0;
    memcpy(&newFloat, ptr, sizeof(newFloat));
    return newFloat;
}

unsigned long Msg::bu32(const byte* buffer, const int start/*=0*/) {
    unsigned long result = 0;
    for (int i = 0; i < 4; i++) {
        result <<= 8;
        result |= (buffer[start + i] & 0xFF);
    }
    return result;
}

void Msg::publishError(const String& message) {
	publishMRLCommError(message.c_str(), message.length());
}

void Msg::publishDebug(const String& message) {
	if (debug){
		publishDebug(message.c_str(), message.length());
	}
}

bool Msg::readMsg() {
	// handle serial data begin
	int bytesAvailable = serial->available();
	if (bytesAvailable > 0) {
		//publishDebug("RXBUFF:" + String(bytesAvailable));
		// now we should loop over the available bytes .. not just read one by one.
		for (int i = 0; i < bytesAvailable; i++) {
			// read the incoming byte:
			unsigned char newByte = serial->read();
			//publishDebug("RX:" + String(newByte));
			++byteCount;
			// checking first byte - beginning of message?
			if (byteCount == 1 && newByte != MAGIC_NUMBER) {
				publishError(F("error serial"));
				// reset - try again
				byteCount = 0;
				// return false;
			}
			if (byteCount == 2) {
				// get the size of message
				// todo check msg < 64 (MAX_MSG_SIZE)
				if (newByte > 64) {
					// TODO - send error back
					byteCount = 0;
					continue; // GroG - I guess  we continue now vs return false on error conditions?
				}
				msgSize = newByte;
			}
			if (byteCount > 2) {
				// fill in msg data - (2) headbytes -1 (offset)
				ioCmd[byteCount - 3] = newByte;
			}
			// if received header + msg
			if (byteCount == 2 + msgSize) {
				// we've reach the end of the command, just return true .. we've got it
				byteCount = 0;
				return true;
			}
		}
	} // if Serial.available
	  // we only partially read a command.  (or nothing at all.)
	return false;
}

void Msg::write(const unsigned char value) {
	serial->write(value);
}

void Msg::write(const unsigned char* buffer, int len) {
	serial->write(len);
	serial->write(buffer, len);
}

void Msg::writebool(const bool value){
	if (value){
		write(0);
	} else {
		write(1);
	}
}

void Msg::writeb16(const int b16){
	write(b16 >> 8 & 0xFF);
	write(b16 & 0xFF);
}

void Msg::writeb32(const long b32){
	write(b32 >> 24 & 0xFF);
	write(b32 >> 16 & 0xFF);
	write(b32 >> 8 & 0xFF);
	write(b32 & 0xFF);
}

void Msg::writef32(const float f32){
	byte temp [4];

    float newFloat = 0;
    memcpy(temp, &f32, sizeof(newFloat));

	write(temp[3]);
	write(temp[2]);
	write(temp[1]);
	write(temp[0]);
}

void Msg::writebu32(const unsigned long bu32){
	write(bu32 >> 24 & 0xFF);
	write(bu32 >> 16 & 0xFF);
	write(bu32 >> 8 & 0xFF);
	write(bu32 & 0xFF);
}

byte* Msg::getBuffer() {
	return sendBuffer;
}

int Msg::getBufferSize() {
	return sendBufferSize;
}

void Msg::reset() {
	sendBufferSize = 0;
}

void Msg::flush() {
	return serial->flush();
}

void Msg::begin(HardwareSerial& hardwareSerial){
	serial = &hardwareSerial;
}

byte Msg::getMethod(){
	return ioCmd[0];
}
