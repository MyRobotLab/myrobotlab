package org.myrobotlab.service.config;

import org.myrobotlab.framework.Peer;
import org.myrobotlab.framework.Plan;

public class UpdaterConfig extends ServiceConfig {
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    addDefaultPeerConfig(plan, name, "git", "Git");
    addDefaultPeerConfig(plan, name, "builder", "Maven");

    return plan;
  }

}
