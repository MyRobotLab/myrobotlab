package org.myrobotlab.i2c;

import java.io.IOException;

import com.pi4j.io.i2c.I2CDevice;

public class I2CProxyDeviceImpl implements I2CDevice {

  @Override
  public int read() throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int read(byte[] arg0, int arg1, int arg2) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  // New in pi4j 1.1
  @Override
  public int read(byte[] writeBuffer, int writeOffset, int writeSize, byte[] readBuffer, int readOffset, int readSize) throws IOException {
    return 0;
  }
  //

  @Override
  public int read(int arg0) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int read(int arg0, byte[] arg1, int arg2, int arg3) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void write(byte arg0) throws IOException {
    // TODO Auto-generated method stub

  }

  // New in pi4j 1.1
  @Override
  public void write(int address, byte[] b) throws IOException {
    // TODO Auto-generated method stub
  }
  //

  @Override
  public void write(byte[] arg0, int arg1, int arg2) throws IOException {
    // TODO Auto-generated method stub

  }

  // New in pi4j 1.1
  @Override
  public void write(byte[] b) throws IOException {
    // TODO Auto-generated method stub
  }
  //

  @Override
  public void write(int arg0, byte arg1) throws IOException {
    // TODO Auto-generated method stub

  }

  @Override
  public void write(int arg0, byte[] arg1, int arg2, int arg3) throws IOException {
    // TODO Auto-generated method stub

  }

}
