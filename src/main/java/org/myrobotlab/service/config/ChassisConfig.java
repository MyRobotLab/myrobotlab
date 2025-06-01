package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class ChassisConfig extends ServiceConfig {

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "left", "Motor");
    addDefaultPeerConfig(plan, name, "right", "Motor");
    addDefaultPeerConfig(plan, name, "joystick", "Joystick");
    addDefaultPeerConfig(plan, name, "controller", "Sabertooth");
    return plan;
  }

}
