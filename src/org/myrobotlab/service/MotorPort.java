package org.myrobotlab.service;

import org.myrobotlab.service.abstracts.AbstractMotor;

/**
 * Motor - MotorController which uses a "Port".  
 * Examples are Adafruit Motor Controller which uses ports
 * M1 M2 M3 M4
 * Sabertooth has M1 &amp; M2 ports.
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
  
  public String getPort() {
    return port;
  }
 
  
}
