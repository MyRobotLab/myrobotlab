package org.myrobotlab.service.interfaces;

import java.io.IOException;

import org.myrobotlab.service.Arduino;

@Deprecated /*
             * service should just be a serial device - with more configuration
             */
public interface SerialRelayListener {
  public void connect(Arduino controller, String serialPort) throws IOException;
}
