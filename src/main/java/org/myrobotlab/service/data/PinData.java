package org.myrobotlab.service.data;

import java.io.Serializable;

public class PinData implements Serializable {
  private static final long serialVersionUID = 1L;

  /**
   * The pin - this can be A0 D2 M3 etc ... it is the responsibility of the
   * controller to convert it to an address or from an address to the pin
   * identifier/label
   */
  public String pin;

  /**
   * The value of the pin - can support analog or digital pins
   */
  public Integer value;

  public PinData(String pin, int value) {
    this.pin = pin;
    this.value = value;
  }

  public PinData(int pin, int value) {
    this.pin = String.format("%d", pin);
    this.value = value;
  }

  @Override
  public String toString() {
    return String.format("pin=%s value=%d", pin, value);
  }

}
