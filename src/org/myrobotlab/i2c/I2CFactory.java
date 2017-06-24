package org.myrobotlab.i2c;

import java.io.IOException;

import org.myrobotlab.framework.Platform;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.impl.I2CBusImpl;

public class I2CFactory {

  /**
   * Create new I2CBus instance
   * @param busNumber b
   * 
   * @return Return a new I2CBus impl instance.
   * @throws IOException e 
   */
  public static I2CBus getInstance(int busNumber) throws IOException {

    /*
     * String architecture = Platform.getArch(); try { String I2CBusType =
     * "org.myrobotlab.i2c.I2CBusProxy"; if
     * (architecture.equals(Platform.ARCH_ARM)) { // raspi I2CBusType =
     * "com.pi4j.io.i2c.impl.I2CBusImpl"; }
     * 
     * Object[] param = new Object[0];
     * 
     * Class<?> c; c = Class.forName(I2CBusType); Class<?>[] paramTypes = new
     * Class[param.length]; for (int i = 0; i < param.length; ++i) {
     * paramTypes[i] = param[i].getClass(); } Constructor<?> mc =
     * c.getConstructor(paramTypes); return (I2CBus) mc.newInstance(param); }
     * catch (Exception e) { Logging.logException(e); return null; }
     */
    // pi4j's factory calls the implementation directly
    // which would not be my first choice - but since it does
    // I will do the same here... otherwise I'll need to invoke
    // without an interface
    Platform platform = Platform.getLocalInstance();

    if (platform.isArm()) {
      // raspi
      return I2CBusImpl.getBus(busNumber);
    } else {
      return I2CProxyImpl.getBus(busNumber);
    }

    // return I2CBusImpl.getBus(busNumber);
  }

  public static void main(String[] args) {
    // TODO Auto-generated method stub

  }

}
