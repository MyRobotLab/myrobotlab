package org.myrobotlab.service.config;

public class ServoConfig extends ServiceConfig {

  public boolean autoDisable;
  // public String controller;
  public boolean enabled;
  public Integer idleTimeout;
  
  public String pin;
  public Double rest;
  public Double speed;

  // mapper values
  public boolean inverted;
  public boolean clip;
  public Double minIn;
  public Double maxIn;
  public Double minOut;
  public Double maxOut;

  public Double sweepMax;
  public Double sweepMin;

}
