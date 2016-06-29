package org.myrobotlab.sensor;

import org.myrobotlab.service.data.SensorData;
import org.myrobotlab.service.interfaces.Microcontroller;
import org.myrobotlab.service.interfaces.SensorDataListener;
import org.myrobotlab.service.interfaces.SensorDataPublisher;

public class AnalogPinSensor implements SensorDataPublisher {

  private final int pin;
  private final int sampleRate;
  
  public AnalogPinSensor(int pin, int sampleRate) {
    super();
    this.pin = pin;
    // TODO: fix the concept of sample rate! should be Hertz.. not number of skiped loops.
    this.sampleRate = sampleRate;
  }
  
  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "A" + pin;
  }


  @Override
  public SensorData publishSensorData(SensorData data) {
    return data;
  }


  @Override
  public void addSensorDataListener(SensorDataListener listener, int[] config) {
  	// TODO Auto-generated method stub
  	
  }


  public int getPin() {
    return pin;
  }

  public int getSampleRate() {
    return sampleRate;
  }



}
