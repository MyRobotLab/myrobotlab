package org.myrobotlab.i2c;

import java.io.IOException;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;

public class I2CProxyBusImpl implements I2CBus {

  // private int bus = -1;

  @Override
  public void close() throws IOException {

  }

  @Override
  public I2CDevice getDevice(int bus) throws IOException {
    // this.bus = bus;
    return new I2CProxyDeviceImpl();
  }

  @Override
  public int getBusNumber() {
    // TODO Auto-generated method stub
    return 0;
  }

  // Removing because they're not actually in the released version of pi4j 1.1
  // TODO: clean this up and refactor to support the released pi4j 1.1
  // // New in pi4j 1.1
  // @Override
  // public int getFileDescriptor() {
  // // TODO Auto-generated method stub
  // return 0;
  // }
  //
  // @Override
  // public String getFileName() {
  // // TODO Auto-generated method stub
  // return null;
  // }
  // //
}
