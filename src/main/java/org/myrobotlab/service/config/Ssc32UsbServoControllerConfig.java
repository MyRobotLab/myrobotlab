package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class Ssc32UsbServoControllerConfig extends ServiceConfig {

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "serial", "Serial");
    return plan;
  }

}
