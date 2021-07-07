package org.myrobotlab.service.config;

public class ServoConfig extends ServiceConfig {

  public Boolean autoDisable;
  // public String controller;
  public Boolean enabled;
  public Integer idleTimeout;
  
  public String pin;
  public Double rest;
  public Double speed;

  // mapper values
  public Boolean inverted;
  public Boolean clip;
  public Double minIn;
  public Double maxIn;
  public Double minOut;
  public Double maxOut;

  public Double sweepMax;
  public Double sweepMin;

}
