package org.myrobotlab.service.config;

public class ServoConfig extends ServiceConfig {

  /**
   * The automatic disabling of the servo in idleTimeout ms This de-energizes
   * the servo. By default this is disabled.
   * 
   */
  public boolean autoDisable = false;

  public boolean enabled = true;
  public Integer idleTimeout;

  public String pin;
  public Double rest = 90.0;
  public Double speed;

  // mapper values
  public boolean inverted = false;
  public boolean clip = true;
  public Double minIn = 0.0;
  public Double maxIn = 180.0;
  public Double minOut = 0.0;
  public Double maxOut = 180.0;

  public Double sweepMax;
  public Double sweepMin;

  public String controller;

  public String[] synced;

}
