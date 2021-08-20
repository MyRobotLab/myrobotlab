package org.myrobotlab.service.config;

public class AbstractMotorConfig extends ServiceConfig {

  public AbstractMotorConfig() {
  }
  
  public AbstractMotorConfig(ServiceConfig c) {
    this.name = c.name;
    this.type = c.type;
    this.locale = c.locale;
    this.attach = c.attach;
  }
  
  public boolean locked;
  
  // mapper values
  public boolean inverted;
  public boolean clip;
  public Double minIn;
  public Double maxIn;
  public Double minOut;
  public Double maxOut;

}
