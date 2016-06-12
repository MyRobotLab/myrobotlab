package org.myrobotlab.service.interfaces;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.Pin;

public interface Microcontroller {

  // connectivity
  public void connect(String port);
  public void disconnect();
  public boolean isConnected();

  // metadata about the controller
  public String getBoardType();
  public Integer getVersion();
  public List<Pin> getPinList();
  
  public boolean sensorAttach(SensorDataPublisher sensor);
  
}
