package org.myrobotlab.service.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Arduino;
import org.slf4j.Logger;

public class ArduinoConfig extends ServiceConfig {
  

  public String port;
  public boolean connect;
  public String serial; // name of serial service 
  
  public Map<String, ServiceConfig> getDefault(String name) {
    Map<String, ServiceConfig> config = new LinkedHashMap<>();

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
