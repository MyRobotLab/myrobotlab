package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class DruppNeckConfig extends ServiceConfig {
  
  // this is an offset angle that is added to the solution from the IK solver
  public double upOffset = 90;
  public double middleOffset = 120 + 90;
  public double downOffset = -120 + 90;


  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "up", "Servo");
    addDefaultPeerConfig(plan, name, "middle", "Servo");
    addDefaultPeerConfig(plan, name, "down", "Servo");
    return plan;
  }

}
