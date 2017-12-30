package org.myrobotlab.service.interfaces;

import java.io.IOException;

public interface PortConnector {

  public void connect(String port) throws Exception;

  public void connect(String port, int rate) throws Exception;

  public void connect(String port, int rate, int databits, int stopbits, int parity) throws Exception;

  public void disconnect() throws IOException;

  public boolean isConnected();
  
}
