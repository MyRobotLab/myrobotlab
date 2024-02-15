package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class SabertoothConfig extends AbstractMotorControllerConfig {

  public String port;
  public boolean connect = false;

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "serial", "Serial");
    return plan;
  }

}
