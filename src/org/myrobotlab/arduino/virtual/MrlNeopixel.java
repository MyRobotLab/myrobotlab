package org.myrobotlab.arduino.virtual;

import org.myrobotlab.arduino.Msg;

public class MrlNeopixel extends Device {

  public MrlNeopixel(int deviceId) {
    super(deviceId, Msg.DEVICE_TYPE_NEOPIXEL);
  }

  public void setAnimation(Integer animation, Integer red, Integer green, Integer blue, Integer speed) {
    // TODO Auto-generated method stub

  }

  /*
  public void attach(int pin, int numPixels) {
    
  }
  */

  public void setAnimation(int animation, int red, int green, int blue, int speed) {
    // TODO Auto-generated method stub
    
  }

  @Override
  void update() {
    // TODO Auto-generated method stub
    
  }

  public void neopixelWriteMatrix(int bufferSize, int[] buffer) {
    // TODO Auto-generated method stub
    
  }

  public void attach(int pin, long numPixels) {
    // TODO Auto-generated method stub
    
  }

}