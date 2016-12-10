#include "Msg.h"
#include "Device.h"
#include "MrlI2cBus.h"

MrlI2CBus::MrlI2CBus(int deviceId) :
		Device(deviceId, DEVICE_TYPE_I2C) {
	if (TWCR == 0) { //// do this check so that Wire only gets initialized once
		WIRE.begin();
	    // Force 400 KHz i2c
		WIRE.setClock(400000L);
	}
}

bool MrlI2CBus::attach(byte bus) {
	this->bus = bus;
	return true;
}

// I2CWRITE | DEVICE_INDEX | I2CADDRESS | DATASIZE | DATA.....
void MrlI2CBus::i2cWrite(byte deviceAddress, byte dataSize, const byte*data) {

	WIRE.beginTransmission(deviceAddress);    // address to the i2c device
	for (int i = 0; i < dataSize; i++) { // data to write
		WIRE.write(data[i]);
	}
	WIRE.endTransmission();
}

// I2CREAD | DEVICE_INDEX | I2CADDRESS | DATASIZE
// PUBLISH_SENSOR_DATA | DEVICE_INDEX | DATASIZE | DATA ....
// DEVICE_INDEX = Index to the I2C bus
// I2CADDRESS = The address of the i2c device
// DATA_SIZE = The number of bytes to read from the i2c device
void MrlI2CBus::i2cRead(byte deviceAddress, byte size) {

	int answer = WIRE.requestFrom(deviceAddress, size); // reqest a number of bytes to read

	for (int i = 0; i < answer; i++) {
		msg->add(Wire.read());
	}

	// byte deviceId = ioCmd[1]; not needed we have our own deviceId
	msg->publishI2cData(id, msg->getBuffer(), msg->getBufferSize());
}

// I2WRITEREAD | DEVICE_INDEX | I2CADDRESS | DATASIZE | DEVICE_MEMORY_ADDRESS
// PUBLISH_SENSOR_DATA | DEVICE_INDEX | DATASIZE | DATA ....
// DEVICE_INDEX = Index to the I2C bus
// I2CADDRESS = The address of the i2c device
// DATA_SIZE = The number of bytes to read from the i2c device
void MrlI2CBus::i2cWriteRead(byte deviceAddress, byte readSize, byte writeValue) {
	WIRE.beginTransmission(writeValue); // address to the i2c device
	WIRE.write(writeValue);             // device memory address to read from
	WIRE.endTransmission();
	int answer = WIRE.requestFrom(deviceAddress, readSize); // reqest a number of bytes to read

	for (int i = 0; i < answer; i++) {
		msg->add(Wire.read());
	}

	// byte deviceId = ioCmd[1];
	msg->publishI2cData(id, msg->getBuffer(), msg->getBufferSize());
}

void MrlI2CBus::update() {
	//Nothing to do
}

