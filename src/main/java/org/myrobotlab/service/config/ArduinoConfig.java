package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class ArduinoConfig extends ServiceConfig {

  public String port;
  public boolean connect;

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    SerialConfig serialConfig = (SerialConfig) addDefaultPeerConfig(plan, name, "serial", "Serial");
    serialConfig.port = port;
    return plan;
  }
}
