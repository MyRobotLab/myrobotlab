package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class LocalSpeechConfig extends SpeechSynthesisConfig {

  public String speechType;
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    return plan;
  }

}
