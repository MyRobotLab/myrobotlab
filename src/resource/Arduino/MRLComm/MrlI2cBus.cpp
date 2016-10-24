#include "MrlI2cBus.h"

MrlI2CBus::MrlI2CBus() :
		Device(DEVICE_TYPE_I2C) {
	if (TWCR == 0) { //// do this check so that Wire only gets initialized once
		WIRE.begin();
	    // Force 400 KHz i2c
		WIRE.setClock(400000L);
	}
}

bool MrlI2CBus::deviceAttach(unsigned char config[], int configSize) {
	if (configSize != 2) {
		MrlMsg msg(PUBLISH_MRLCOMM_ERROR);
		msg.addData(ERROR_DOES_NOT_EXIST);
		msg.addData(String(F("MrlI2CBus invalid attach config size")));
		return false;
	}
	bus = config[1];
	attachDevice();
	return true;
}

// I2CWRITE | DEVICE_INDEX | I2CADDRESS | DATASIZE | DATA.....
void MrlI2CBus::i2cWrite(unsigned char* ioCmd) {
	int dataSize = ioCmd[3];
	WIRE.beginTransmission(ioCmd[2]);    // address to the i2c device
	for (int i = 0; i < dataSize; i++) { // data to write
		WIRE.write(ioCmd[i +4]);
	}
	WIRE.endTransmission();
}

// I2CREAD | DEVICE_INDEX | I2CADDRESS | DATASIZE
// PUBLISH_SENSOR_DATA | DEVICE_INDEX | DATASIZE | DATA ....
// DEVICE_INDEX = Index to the I2C bus
// I2CADDRESS = The address of the i2c device
// DATA_SIZE = The number of bytes to read from the i2c device
void MrlI2CBus::i2cRead(unsigned char* ioCmd) {

	int answer = WIRE.requestFrom(ioCmd[2], ioCmd[3]); // reqest a number of bytes to read
	MrlMsg msg(PUBLISH_SENSOR_DATA);
	msg.addData(ioCmd[1]); // DEVICE_INDEX
	msg.addData(ioCmd[3]); // DATASIZE
	for (int i = 0; i < answer; i++) {
		msg.addData(Wire.read());
	}
	msg.sendMsg();
}

// I2WRITEREAD | DEVICE_INDEX | I2CADDRESS | DATASIZE | DEVICE_MEMORY_ADDRESS
// PUBLISH_SENSOR_DATA | DEVICE_INDEX | DATASIZE | DATA ....
// DEVICE_INDEX = Index to the I2C bus
// I2CADDRESS = The address of the i2c device
// DATA_SIZE = The number of bytes to read from the i2c device
void MrlI2CBus::i2cWriteRead(unsigned char* ioCmd) {
	WIRE.beginTransmission(ioCmd[2]); // address to the i2c device
	WIRE.write(ioCmd[4]);             // device memory address to read from
	WIRE.endTransmission();
	int answer = WIRE.requestFrom((uint8_t) ioCmd[2], (uint8_t) ioCmd[3]); // reqest a number of bytes to read
	MrlMsg msg(PUBLISH_SENSOR_DATA);
	msg.addData(ioCmd[1]); // DEVICE_INDEX
	msg.addData(ioCmd[3]); // DATASIZE
	for (int i = 0; i < answer; i++) {
		msg.addData(Wire.read());
	}
	msg.sendMsg();
}

void MrlI2CBus::update() {
	//Nothing to do
}

