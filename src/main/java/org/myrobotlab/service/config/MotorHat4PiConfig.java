package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class MotorHat4PiConfig extends GeneralMotorConfig {

  // TODO - all motor id's
  public String motorId;
   
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    return plan;
  }


}
