package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class DiyServoConfig extends ServoConfig {

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "motor", "MotorDualPwm");
    addDefaultPeerConfig(plan, name, "pid", "Pid");
    return plan;
  }

}
