package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class PollyConfig extends SpeechSynthesisConfig {

  public boolean ssml = false;
  public boolean autoDetectSsml = true;

  public PollyConfig() {
    voice = "Brian";
  }

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    return plan;
  }

}
