#ifndef MrlI2cBus_h
#define MrlI2cBus_h

#include "Device.h"
#include "MrlMsg.h"

#define WIRE Wire
#include <Wire.h>

/**
 * I2C bus
 * TODO:KW? don't allow this class to write directly to the global serial port
 * device classes shouldn't have a direct handle to the serial port, rather have
 * a mrlmessage class that it returns.
 * TODO: Mats
 * The I2CBus device represents one I2C bus. 
 * It's the SDA (data line) and SCL pins (clock line) that is used to 
 * communicate with any device that uses the i2c protocol on that bus.
 * It is NOT a representation of the addressable i2c devices, just the bus
 * On Arduino Uno that's pins A4 and A5, Mega 20 and 21, Leonardo 2 and 3, 
 * The pin assignment is defined in Wire.h so it will change to the correct 
 * pins at compile time. We don't have to worry here.
 * However some other i2c implementations exist's so that more pins can be used
 * for i2c communication. That is not supported here yet.
 * 
 */
class MrlI2CBus : public Device {
  public:
    MrlI2CBus() : Device(DEVICE_TYPE_I2C) {
      if (TWCR == 0) { //// do this check so that Wire only gets initialized once
        WIRE.begin();
      }
    }

    // I2CREAD | DEVICE_INDEX | I2CADDRESS | DATA_SIZE
    // PUBLISH_SENSOR_DATA | DEVICE_INDEX | I2CADDRESS | DATA ....
    // DEVICE_INDEX = Index to the I2C bus
    // I2CADDRESS = The address of the i2c device
    // DATA_SIZE = The number of bytes to read from the i2c device
    void i2cRead(unsigned char* ioCmd) {

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
    void i2cWrite(unsigned char* ioCmd) {
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
    void i2cWriteRead(unsigned char* ioCmd) {
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
    void update(unsigned long lastMicros) {
      //Nothing to do
    }
};

#endif
