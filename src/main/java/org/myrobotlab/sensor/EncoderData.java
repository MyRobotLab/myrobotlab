package org.myrobotlab.sensor;

public class EncoderData {

  /**
   * pin, id, or address of source
   */
  public String pin;
  
  /**
   * any free-form type information to be sent - could be STOP | MOVING or directional
   */
  public String type;
  
  /**
   * the service from which this encoder data came from
   */
  public String source;
  
  /**
   * data from the encoder - typically current position
   */
  public Double value;
  
  /**
   * time data was generated
   */
  public long timestamp;

  public EncoderData(String source, String pin, double value) {
    this.timestamp = System.currentTimeMillis();
    this.source = source;
    this.value = value;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[EncoderData: source:");
    sb.append(source);
    sb.append(" pin:");
    sb.append(pin);
    sb.append(" value:");
    sb.append(value);
    return sb.toString();
  }

}
