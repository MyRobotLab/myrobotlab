package org.myrobotlab.service.config;

/**
 * config for an as5048a spi based absolute position encoder
 * This is a magenetic encoder.
 */
public class As5048AEncoderConfig extends ServiceConfig {
  // the only config i guess is what pin it attaches to it's controller on.
  public Integer pin = 10;
  public String controller = null;
  
}
