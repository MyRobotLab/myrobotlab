package org.myrobotlab.sensor;

public class EncoderData {

  // pin, id, or address of source
  public String address;
  // service (EncoderController)
  public String source;
  // data from the encoder - typically current position
  public Double value;
  // option  "type" of data - MrlEncoder has movement  vs finish

  public EncoderData(String name, double value) {
    this.source = name;
    this.value = value;
  }

  public String toString() {
    return String.format("%s %s %d", source, address, value);
  }

}
