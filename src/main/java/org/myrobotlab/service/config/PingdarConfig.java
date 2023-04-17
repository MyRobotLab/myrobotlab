package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class PingdarConfig extends ServiceConfig {

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    addDefaultPeerConfig(plan, name, "controller", "Arduino");
    addDefaultPeerConfig(plan, name, "ultrasonic", "UltrasonicSensor");
    addDefaultPeerConfig(plan, name, "servo", "Servo");

    return plan;
  }

}
