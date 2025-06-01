package org.myrobotlab.service.config;

import org.myrobotlab.framework.Peer;
import org.myrobotlab.framework.Plan;

public class TestCatcherConfig extends ServiceConfig {

 
  public Peer globalPeer = new Peer("subpeer", "TestThrower");

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "subpeer", "TestThrower");
    addDefaultGlobalConfig(plan, "builder", "builder", "TestThrower");
    return plan;
  }

}
