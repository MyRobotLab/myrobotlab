package org.myrobotlab.service.config;

public class UltrasonicSensorConfig extends ServiceConfig {

  /**
   * controller for the sensor
   */
  public String controller;
  
  /**
   * pulse pin
   */
  @Deprecated /* Pins need to be Strings eg "D64" */
  public Integer triggerPin;
  
  /**
   * listening pin
   */
  @Deprecated /* Pins need to be Strings eg "D63" */
  public Integer echoPin;
  
  /**
   * 500 ms timeout default
   */
  public Long timeout = 500L;

}
