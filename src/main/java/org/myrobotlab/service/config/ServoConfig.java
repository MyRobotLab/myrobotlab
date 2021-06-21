package org.myrobotlab.service.config;

public class ServoConfig extends Config {

  public Boolean autoDisable;
  public String controller;
  public Boolean enabled;
  public Boolean idleDisabled;
  public Integer idleTimeout;
  
  public String pin;
  public Double rest;
  public Double speed;

  // mapper values
  public Boolean inverted;
  public Boolean clip;
  public Double minX;
  public Double maxX;
  public Double minY;
  public Double maxY;

  public Double sweepMax;
  public Double sweepMin;

}
