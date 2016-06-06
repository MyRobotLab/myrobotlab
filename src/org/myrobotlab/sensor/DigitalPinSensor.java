package org.myrobotlab.sensor;

import java.util.ArrayList;

import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.data.SensorData;
import org.myrobotlab.service.interfaces.Microcontroller;
import org.myrobotlab.service.interfaces.SensorDataListener;
import org.myrobotlab.service.interfaces.SensorDataPublisher;

public class DigitalPinSensor implements SensorDataPublisher {

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int[] getSensorConfig() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void update(Object data) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public SensorData publishSensorData(SensorData data) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void addSensorDataListener(SensorDataListener listener) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getSensorType() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void attach(Microcontroller controller) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void detach() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void start() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void stop() {
    // TODO Auto-generated method stub
    
  }

}
