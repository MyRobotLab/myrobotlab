package org.myrobotlab.service.config;

public class Mpu6050Config extends ServiceConfig {

  /**
   * auto start mpu 6050
   */
  public boolean start = false;
  
  /**
   * orientation sample rate in hz 
   */
  public Double sampleRate;
}
