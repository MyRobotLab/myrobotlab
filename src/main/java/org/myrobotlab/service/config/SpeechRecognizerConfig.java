package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class SpeechRecognizerConfig extends ServiceConfig {

  public boolean listening = false;
  public boolean recording = false;
  public String[] textListeners;
  public String wakeWord;

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    return plan;
  }

}
