package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class IndianTtsConfig extends SpeechSynthesisConfig {

  // peer names
  String http;
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    // default names
    http = name + ".http";    
    addPeer(plan, name, "http", http, "Http", "Http");

    return plan;
  }

  
}
