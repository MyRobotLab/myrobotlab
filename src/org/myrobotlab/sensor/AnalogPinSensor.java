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
  public int[] getSensorConfig() {
    // one pin.
    return new int[]{pin};
    
  }

  @Override
  public SensorData publishSensorData(SensorData data) {
    return data;
  }

  @Override
  public void start() {
    // TODO Auto-generated method stub
    // here we should start/stop analog polling for our pin..
    // the arduino ...  Maybe these life cycle methods shouldn't be here?
  }

  @Override
  public void stop() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void update(Object data) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addSensorDataListener(SensorDataListener listener) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getSensorType() {
    // TODO replace this with an ENUM ...
    return "ANALOG_PIN";
  }

  @Override
  public void attach(Microcontroller controller) {
    // TODO: here we want to register our pin with the controller
    // hmm.. what to do here?  this feels a little inverted for the interfaces.
    controller.sensorAttach(this);
  }

  @Override
  public void detach() {
    // TODO Auto-generated method stub
    
  }

  public int getPin() {
    return pin;
  }

  public int getSampleRate() {
    return sampleRate;
  }

}
