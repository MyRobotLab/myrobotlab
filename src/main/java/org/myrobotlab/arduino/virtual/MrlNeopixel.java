package org.myrobotlab.arduino.virtual;

import org.myrobotlab.arduino.Msg;
import org.myrobotlab.service.VirtualArduino;

public class MrlNeopixel extends Device {

  int id;
  int pin;
  int numPixels;
  int depth;

  public MrlNeopixel(int deviceId, VirtualArduino virtual) {
    super(deviceId, Msg.DEVICE_TYPE_NEOPIXEL, virtual);
  }

  public MrlNeopixel(Integer deviceId, Integer pin, Integer numPixels, Integer depth) {
    super(deviceId);
    this.id = deviceId;
    this.pin = pin;
    this.numPixels = numPixels;
    this.depth = depth;
  }

  public void setAnimation(Integer animation, Integer red, Integer green, Integer blue, Integer speed) {
    // TODO Auto-generated method stub

  }

  /*
   * public void attach(int pin, int numPixels) {
   * 
   * }
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

  public void attach(int pin, long numPixels, int depth) {
    // TODO Auto-generated method stub

  }

}