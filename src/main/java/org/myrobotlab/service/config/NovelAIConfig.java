package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class NovelAIConfig extends SpeechSynthesisConfig {


  public NovelAIConfig() {
    voice = "Aini";
  }

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    return plan;
  }

}
