package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class WebkitSpeechRecognitionConfig extends SpeechRecognizerConfig {

  public WebkitSpeechRecognitionConfig() {
  }
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    return plan;
  }



}
