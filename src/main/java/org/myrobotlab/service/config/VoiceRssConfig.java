package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class VoiceRssConfig extends SpeechSynthesisConfig {
  
  public String key;

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    return plan;
  }

}
