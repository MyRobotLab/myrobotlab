package org.myrobotlab.service.config;

/**
 * configuration for an AMT203 absolute position rotary encoder.
 * This is a capacitive encoder. 
 */
public class Amt203EncoderConfig extends ServiceConfig {
  public Integer pin = 10;
  public String controller = null;
}
