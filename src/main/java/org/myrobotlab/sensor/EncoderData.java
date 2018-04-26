package org.myrobotlab.sensor;

public class EncoderData {
  
  public String source; 
  public Long value;
  
  public EncoderData(String name, long value) {
    this.source = name;
    this.value = value;
  }
  
  public String toString() {
    return String.format("%s %d",  source, value);
  }
  
}
