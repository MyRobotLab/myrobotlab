package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class ArduinoConfig extends ServiceConfig {

  public String port;
  public boolean connect;
  
  // name of peers
  public String serial;

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    
    // default name
    serial = name + ".serial";
    SerialConfig serialConfig = (SerialConfig) addPeer(plan, name, "serial", serial, "Serial", "for serial connectivity");

    // pull out specific config and modify
    serialConfig.port = port;

    return plan;
  }
}
