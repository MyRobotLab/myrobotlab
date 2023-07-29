package org.myrobotlab.service.config;

public class PirConfig extends ServiceConfig {

  /**
   * pin controller name
   */
  public String controller;
  
  /**
   * pin this pir sensor is attached to
   */
  public String pin;

  /**
   * poll rate in Hz
   * FIXME change to double or float to support 0.01 Hz
   */
  public int rate = 1;
  
  
  /**
   * if polling is currently enabled
   */
  public boolean enable = true;

}
