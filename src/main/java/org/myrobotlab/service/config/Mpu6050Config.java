package org.myrobotlab.service.config;

public class Mpu6050Config extends ServiceConfig {

  /**
   * bus for this device String to support writing to hex
   */
  public String bus;

  /**
   * address for this device String to support writing to hex
   */
  public String address;

  /**
   * auto start mpu 6050
   */
  public boolean start = false;

  /**
   * orientation sample rate in hz
   */
  public Double sampleRate;

  /**
   * I2C Controller
   */
  public String controller;

}
