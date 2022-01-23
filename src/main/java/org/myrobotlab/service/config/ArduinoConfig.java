package org.myrobotlab.service.config;

import java.util.LinkedHashMap;

public class ArduinoConfig extends ServiceConfig {
  

  public String port;
  public boolean connect;
  public String serial; // name of serial service 
  
  @Override
  public LinkedHashMap<String, ServiceConfig> getDefault(String name) {
    LinkedHashMap<String, ServiceConfig> config = new LinkedHashMap<>();

    // set local names and config
    serial = name + ".serial";
    
    // build a config with all peer defaults
    config.putAll(ServiceConfig.getDefault(serial, "Serial"));
    
    // pull out specific config and modify
    SerialConfig serialConfig = (SerialConfig)config.get(serial);
    serialConfig.port = port;
    
    // put self in
    config.put(name, this);

    return config; 
  }
  
}
