package org.myrobotlab.arduino.virtual;

import org.myrobotlab.arduino.VirtualMsg;
import org.myrobotlab.service.VirtualArduino;

public class MrlI2CBus extends Device {
  
  private static int TWCR = 0;
  int bus;


  MrlI2CBus(int deviceId, VirtualArduino virtual) {
    super (deviceId, VirtualMsg.DEVICE_TYPE_I2C, virtual);
  if (TWCR == 0) { //// do this check so that Wire only gets initialized once
    Wire.begin();
      // Force 400 KHz i2c
    Wire.setClock(400000L);
  }
}

boolean attach(int bus) {
  this.bus = bus;
  return true;
}

// I2CWRITE | DEVICE_INDEX | I2CADDRESS | DATASIZE | DATA.....
void i2cWrite(int deviceAddress, int dataSize, int[] data) {

  Wire.beginTransmission(deviceAddress);    // address to the i2c device
  for (int i = 0; i < dataSize; i++) { // data to write
    Wire.write(data[i]);
  }
  Wire.endTransmission();
}

// I2CREAD | DEVICE_INDEX | I2CADDRESS | DATASIZE
// PUBLISH_SENSOR_DATA | DEVICE_INDEX | DATASIZE | DATA ....
// DEVICE_INDEX = Index to the I2C bus
// I2CADDRESS = The address of the i2c device
// DATA_SIZE = The number of bytes to read from the i2c device
void i2cRead(int deviceAddress, int size) {

  int answer = Wire.requestFrom(deviceAddress, size); // reqest a number of bytes to read

  for (int i = 0; i < answer; i++) {
    msg.add(Wire.read());
  }

  // int deviceId = ioCmd[1]; not needed we have our own deviceId
  msg.publishI2cData(id, msg.getBuffer());
}

// I2WRITEREAD | DEVICE_INDEX | I2CADDRESS | DATASIZE | DEVICE_MEMORY_ADDRESS
// PUBLISH_SENSOR_DATA | DEVICE_INDEX | DATASIZE | DATA ....
// DEVICE_INDEX = Index to the I2C bus
// I2CADDRESS = The address of the i2c device
// DATA_SIZE = The number of bytes to read from the i2c device
void i2cWriteRead(int deviceAddress, int readSize, int writeValue) {
  Wire.beginTransmission(deviceAddress); // address to the i2c device
  Wire.write(writeValue);             // device memory address to read from
  Wire.endTransmission();
  int answer = Wire.requestFrom(deviceAddress, readSize); // reqest a number of bytes to read

  for (int i = 0; i < answer; i++) {
    msg.add(Wire.read());
  }

  // int deviceId = ioCmd[1];
  msg.publishI2cData(id, msg.getBuffer());
}

void update() {
  //Nothing to do
}

}
