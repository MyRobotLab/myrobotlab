package org.myrobotlab.service.config;

public class ClockConfig extends ServiceConfig {

  /**
   * interval of pulses in milliseconds
   */
  public int interval = 1000;

  /**
   * determines if the clock should be running or not null would be to leave in
   * its current state
   */
  public Boolean running = false;

}
