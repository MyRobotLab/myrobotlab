package org.myrobotlab.service.config;

public class GeneralMotorConfig extends ServiceConfig {

  public boolean locked = false;

  // mapper values
  public double minIn = -100;
  public double maxIn = 100;
  public double minOut = -100;
  public double maxOut = 100;

  public String controller;

}
