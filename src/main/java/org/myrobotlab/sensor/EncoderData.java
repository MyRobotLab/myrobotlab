package org.myrobotlab.sensor;

public class EncoderData {

  // pin, id, or address of source
  public String pin;
  
  // any freeform type information to be sent - could be STOP | MOVING or directional
  public String type;
  
  // service (EncoderController)
  public String source;
  // data from the encoder - typically current position
  public Double value;
  // option  "type" of data - MrlEncoder has movement  vs finish

  public EncoderData(String source, String pin, double value) {
    this.source = source;
    this.value = value;
  }

  public String toString() {
    return String.format("%s %s %d", source, pin, value);
  }

}
