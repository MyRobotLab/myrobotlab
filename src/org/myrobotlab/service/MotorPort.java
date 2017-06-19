package org.myrobotlab.service;

import org.myrobotlab.service.abstracts.AbstractMotor;

/**
 * Motor - MotorController which uses a "Port".  
 * Examples are Adafruit Motor Controller which uses ports
 * M1 M2 M3 M4
 * Sabertooth has M1 & M2 ports.
 * @author GroG
 * 
 * Some ports are labeled by numbers some by string values,
 * since a string value can handle either we use a String port.
 *
 */
public class MotorPort extends AbstractMotor {
  private static final long serialVersionUID = 1L;

  String port;

  public MotorPort(String n) {
    super(n);
  }

  public void setPort(String port) {
    this.port = port;
  }
  
  public void setPortNumber(Integer portNumber){
    port = String.format("%d", portNumber);
  }

  public Integer getPortNumber(){
    try {
    return Integer.parseInt(port);
    } catch(Exception e){
      error("port %s is not a valid number", port);
    }
    return null;
  }
  
}
