package org.myrobotlab.service.config;

public class UltrasonicSensorConfig extends ServiceConfig {

  /**
   * controller for the sensor
   */
  public String controller;
  
  /**
   * pulse pin
   */
  public Integer triggerPin;
  
  /**
   * listening pin
   */
  public Integer echoPin;
  
  /**
   * 500 ms timeout default
   */
  public Long timeout = 500L;

}
