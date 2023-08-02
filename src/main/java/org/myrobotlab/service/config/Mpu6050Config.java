package org.myrobotlab.service.config;

public class Mpu6050Config extends ServiceConfig {

  /**
   * bus for this device String to support writing to hex
   */
  public String bus = "1";

  /**
   * address for this device String to support writing to hex
   */
  public String address = "0x68";

  /**
   * auto start mpu 6050
   */
  public boolean start = false;

  /**
   * orientation sample rate in hz
   */
  public Double sampleRate = 1.0;

  /**
   * I2C Controller
   */
  public String controller;

}
