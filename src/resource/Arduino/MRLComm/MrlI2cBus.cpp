#include "MrlI2cBus.h"

MrlI2CBus::MrlI2CBus() : Device(DEVICE_TYPE_I2C) {
  if (TWCR == 0) { //// do this check so that Wire only gets initialized once
    WIRE.begin();
  }
}

// I2CREAD | DEVICE_INDEX | I2CADDRESS | DATA_SIZE
// PUBLISH_SENSOR_DATA | DEVICE_INDEX | I2CADDRESS | DATA ....
// DEVICE_INDEX = Index to the I2C bus
// I2CADDRESS = The address of the i2c device
// DATA_SIZE = The number of bytes to read from the i2c device
void MrlI2CBus::i2cRead(unsigned char* ioCmd) {

  int answer = WIRE.requestFrom((uint8_t)ioCmd[3], (uint8_t)ioCmd[4]); // reqest a number of bytes to read
  MrlMsg msg(PUBLISH_SENSOR_DATA);
  msg.addData(ioCmd[1]);
  msg.addData(ioCmd[3]);
  for (int i = 1; i<answer; i++) {
    msg.addData(Wire.read());
  }
  msg.sendMsg();
}

// I2WRITE | DEVICE_INDEX | I2CADDRESS | DATA_SIZE | DATA.....
void MrlI2CBus::i2cWrite(unsigned char* ioCmd) {
    int msgSize = ioCmd[4];
    WIRE.beginTransmission(ioCmd[3]);   // address to the i2c device
    for (int i = 5; i < msgSize; i++) { // i2caddress + data to write, 
      WIRE.write(ioCmd[i]);
    }
    WIRE.endTransmission();
}

// I2WRITEREAD | DEVICE_INDEX | I2CADDRESS | DATA_SIZE | DEVICE_MEMORY_ADDRESS | DATA.....
// PUBLISH_SENSOR_DATA | DEVICE_INDEX | I2CADDRESS | DATA ....
// DEVICE_INDEX = Index to the I2C bus
// I2CADDRESS = The address of the i2c device
// DATA_SIZE = The number of bytes to read from the i2c device
void MrlI2CBus::i2cWriteRead(unsigned char* ioCmd) {
  WIRE.beginTransmission(ioCmd[3]); // address to the i2c device
  WIRE.write(ioCmd[5]);             // device memory address to read from
  WIRE.endTransmission();
  int answer = WIRE.requestFrom((uint8_t)ioCmd[3], (uint8_t)ioCmd[4]); // reqest a number of bytes to read
  MrlMsg msg(PUBLISH_SENSOR_DATA);
  msg.addData(ioCmd[2]);
  msg.addData(ioCmd[3]);
  for (int i = 1; i<answer; i++) {
    msg.addData(Wire.read());
  }
  msg.sendMsg();
}

void MrlI2CBus::update() {
  //Nothing to do
}

