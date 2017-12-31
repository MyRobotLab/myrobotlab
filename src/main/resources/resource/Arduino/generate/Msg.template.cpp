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
 %generatedCallBacks%
 */

%cppMethods%

void Msg::processCommand() {

	int startPos = 0;
	int method = ioCmd[0];

	switch (method) {
%cppHandleCases%
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
