package org.myrobotlab.arduino.virtual;

import org.myrobotlab.arduino.Msg;
import org.myrobotlab.service.VirtualArduino;

public class MrlAmt203Encoder extends Device {

  public int pin;
  
  MrlAmt203Encoder(int deviceId, VirtualArduino virtual) {
    super(deviceId, Msg.DEVICE_TYPE_ENCODER, virtual);
    // TODO Auto-generated constructor stub
  }

  @Override
  void update() {
    // TODO Auto-generated method stub
    // here the encoder should check if it's position has updated and publish it if so.
  }

  public boolean attach(Integer pin) {
    // TODO: attach the encoder here.. i guess?  what does the virtual encoder want to simulate?
    // Ultimately it'd be awesome to actually measure the virtual inmoov encoder positions...  wow.. that'd be cool.
    this.pin = pin;
    return true;
  }

}
