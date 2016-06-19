package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.service.data.Pin;

public interface Microcontroller extends NameProvider {

  // connectivity
  public void connect(String port);
  public void disconnect();
  public boolean isConnected();

  // metadata about the controller
  public String getBoardType();
  public Integer getVersion();
  public List<Pin> getPinList();
  
  public void attachDevice(Device device) throws Exception;
  public void sensorPollingStart(String nameToIndex);
  public void sensorPollingStop(String nameToIndex);
  
}
