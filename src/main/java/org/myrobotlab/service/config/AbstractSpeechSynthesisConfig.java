package org.myrobotlab.service.config;

import java.util.Map;

public class AbstractSpeechSynthesisConfig extends ServiceConfig {
  
  public Boolean mute;
  public Boolean blocking;
  public Map<String,String> substitutions;
  public String voice;

}
