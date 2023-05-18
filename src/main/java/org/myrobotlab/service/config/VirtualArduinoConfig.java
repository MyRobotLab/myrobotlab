package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class VirtualArduinoConfig extends ServiceConfig {

  public String port;
  public boolean connect;

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    // default peer names
    // serial = name + ".serial";
    addDefaultPeerConfig(plan, name, "uart", "Serial");

    // == Peer serial - TODO - automagically add it when you add peers (meta's
    // peers have type info)
    // SerialConfig serialConfig = (SerialConfig) plan.get("uart");

    // pull out specific config and modify
    // serialConfig.port = arduinoConfig.port;

    return plan;
  }
}
