package org.myrobotlab.service.config;

public class PollyConfig extends SpeechSynthesisConfig {

  
  public boolean ssml = false;
  public boolean autoDetectSsml = true;
  
  public PollyConfig() {
    voice = "Brian";
  }
  
}
