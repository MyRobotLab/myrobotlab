package org.myrobotlab.service.config;

public abstract class AbstractMotorConfig extends ServiceConfig {

  public boolean locked;

  // mapper values
  public boolean inverted;
  public boolean clip;
  public Double minIn;
  public Double maxIn;
  public Double minOut;
  public Double maxOut;

}
