package org.myrobotlab.sensor;

import java.util.Objects;

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
  
  public String getPin() {
    return pin;
  }

  public void setPin(String pin) {
    this.pin = pin;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public Double getAngle() {
    return angle;
  }

  public void setAngle(Double angle) {
    this.angle = angle;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }


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

  @Override
  public int hashCode() {
    return Objects.hash(angle, mappedValue, pin, source, timestamp, type, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    EncoderData other = (EncoderData) obj;
    return Objects.equals(angle, other.angle) && Double.doubleToLongBits(mappedValue) == Double.doubleToLongBits(other.mappedValue) && Objects.equals(pin, other.pin)
        && Objects.equals(source, other.source) && timestamp == other.timestamp && Objects.equals(type, other.type)
        && Double.doubleToLongBits(value) == Double.doubleToLongBits(other.value);
  }

}
