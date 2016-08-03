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
  private:
	int bus;
  public:
    MrlI2CBus();
    bool deviceAttach(unsigned char config[], int configSize);
    void i2cRead(unsigned char* ioCmd);
    void i2cWrite(unsigned char* ioCmd);
    void i2cWriteRead(unsigned char* ioCmd);
    void update();
};

#endif
