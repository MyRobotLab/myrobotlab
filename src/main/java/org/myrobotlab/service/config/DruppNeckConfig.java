package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class DruppNeckConfig extends ServiceConfig {

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "up", "Servo");
    addDefaultPeerConfig(plan, name, "middle", "Servo");
    addDefaultPeerConfig(plan, name, "down", "Servo");
    return plan;
  }

}
