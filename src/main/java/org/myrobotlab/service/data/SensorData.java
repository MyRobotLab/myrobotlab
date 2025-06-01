package org.myrobotlab.service.data;

import java.io.Serializable;

/**
 * A generalized bucket for simplified channels to hold
 * any type of sensor information and its source.
 * 
 * @author GroG
 *
 */
public class SensorData implements Serializable {

  private static final long serialVersionUID = 1L;
  
  /**
   * timestamp
   */
  public long ts = System.currentTimeMillis();
  
  /**
   * Type of string data so downstream receivers can
   * interpret data field correctly, typically the
   * name of the type of service which generated it.
   */
  public String type;
  
  /**
   * Service name where data came from.
   */
  public String src;
  
  /**
   * data of sensor
   */
  public Object data;

  public SensorData() {    
  }
  
  public SensorData(String src, String type, Object data) {
    this.src = src;
    this.type = type;
    this.data = data;
  }

  
}
