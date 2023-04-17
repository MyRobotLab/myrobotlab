package org.myrobotlab.service.config;

import org.myrobotlab.framework.Peer;
import org.myrobotlab.framework.Plan;

public class LidarConfig extends ServiceConfig {
  
  // names of peers
  public Peer serial = new Peer("serial", "Serial");

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "topStom", "Serial");
    return plan;
  }

}
