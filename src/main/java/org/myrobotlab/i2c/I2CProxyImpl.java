package org.myrobotlab.i2c;

import com.pi4j.io.i2c.I2CBus;

public class I2CProxyImpl {

  public static I2CBus getBus(int busNumber) {
    // TODO Auto-generated method stub
    return new I2CProxyBusImpl();
  }

}
