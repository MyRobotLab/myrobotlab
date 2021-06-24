package org.myrobotlab.service.config;

public class ServiceConfig {
  
  public String name;
  public String type;   
  public Config config;
  
  public ServiceConfig() {    
  }
  
  public ServiceConfig(String name, String type, Config config) {
    this.name = name;
    this.type = type;
    this.config = config;
  }

}
