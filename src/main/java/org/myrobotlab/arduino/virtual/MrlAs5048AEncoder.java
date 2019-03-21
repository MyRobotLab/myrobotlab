package org.myrobotlab.arduino.virtual;

import org.myrobotlab.arduino.Msg;
import org.myrobotlab.service.VirtualArduino;

public class MrlAs5048AEncoder extends Device {

  public int pin;

  // TODO: where is the publi
  MrlAs5048AEncoder(int deviceId, VirtualArduino virtual) {
    super(deviceId, Msg.DEVICE_TYPE_ENCODER, virtual);
  }

  @Override
  void update() {
    // TODO: implement some virtual simulation support here? could be fun.
  }

  public boolean attach(Integer pin) {
    // TODO: attach the encoder here.. i guess? what does the virtual encoder
    // want to simulate?
    // Ultimately it'd be awesome to actually measure the virtual inmoov encoder
    // positions... wow.. that'd be cool.
    this.pin = pin;
    return true;
  }

  public void setZeroPoint() {
    // TODO: do some virtual encoder implementation fun stuff here.
  }

}
