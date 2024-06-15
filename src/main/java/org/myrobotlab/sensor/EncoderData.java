package org.myrobotlab.sensor;

public class EncoderData {

  /**
   * pin, id, or address of source
   */
  public String pin;

  /**
   * any free-form type information to be sent - could be STOP | MOVING or
   * directional
   */
  public String type;

  /**
   * the service from which this encoder data came from
   */
  public String source;

  /**
   * Computed absolute angle from the encoder - IF THIS IS NOT COMPUTED AS
   * ABSOLUTE ANGLE IT SHOULD BE LEFT NULL !!!
   */
  public Double angle;

  /**
   * raw value of the encoder - this is the tick from the encoder can be
   * whatever the encoder supports - required value
   */
  public double value;

  /**
   * time data was generated
   */
  public long timestamp;

  /**
   * mapped value of input
   */
  public double mappedValue;

  public EncoderData(String source, String pin, double value, Double angle) {
    this.timestamp = System.currentTimeMillis();
    this.source = source;
    this.value = value;
    this.angle = angle;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[EncoderData: source:");
    sb.append(source);
    sb.append(" pin:");
    sb.append(pin);
    sb.append(" value:");
    sb.append(value);
    sb.append(" angle:");
    sb.append(angle);
    return sb.toString();
  }

}
