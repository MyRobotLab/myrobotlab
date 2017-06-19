package org.myrobotlab.service.interfaces;

public interface PortConnector {

  public void connect(String port) throws Exception;
  
  default public void connect(String port, int rate) throws Exception {
    connect(port, rate, 8, 1, 0);
  }

  public void connect(String port, int rate, int databits, int stopbits, int parity) throws Exception;

  public void disconnect();

  public boolean isConnected();
  
}
