package org.myrobotlab.i2c;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.Platform;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;
import com.pi4j.io.i2c.I2CFactoryProvider;
import com.pi4j.io.i2c.impl.I2CProviderImpl;

public class I2CFactory {

  public static final long DEFAULT_LOCKAQUIRE_TIMEOUT = 1000;
  public static final TimeUnit DEFAULT_LOCKAQUIRE_TIMEOUT_UNITS = TimeUnit.MILLISECONDS;

  volatile static I2CFactoryProvider provider = new I2CProviderImpl();

  /**
   * Create new I2CBus instance
   * 
   * @param busNumber
   *          b
   * 
   * @return Return a new I2CBus impl instance.
   * @throws IOException
   *           e
   */
  public static I2CBus getInstance(int busNumber) throws IOException {

    // pi4j's factory calls the implementation directly
    // which would not be my first choice - but since it does
    // I will do the same here... otherwise I'll need to invoke
    // without an interface
    Platform platform = Platform.getLocalInstance();

    if (platform.isArm()) {

      try {
        return provider.getBus(busNumber, DEFAULT_LOCKAQUIRE_TIMEOUT, DEFAULT_LOCKAQUIRE_TIMEOUT_UNITS);
      } catch (UnsupportedBusNumberException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        return null;
      }
    } else {
      return I2CProxyImpl.getBus(busNumber);
    }
  }

}
