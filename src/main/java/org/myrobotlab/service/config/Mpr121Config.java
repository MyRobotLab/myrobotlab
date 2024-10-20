package org.myrobotlab.service.config;

public class Mpr121Config extends ServiceConfig {

  /**
   * address typically is 0x20 unless specified with jumpers or switches
   */
  public String address = "0x5A";
  
  /**
   * arduino is always 0 raspi is usually 1
   */
  public String bus = "1";
  
  /**
   * pin controller for this pcf
   */
  public String controller;
  
  /**
   * read polling rate in Hertz
   */
  public double rateHz = 1;

}
