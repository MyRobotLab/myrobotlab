package org.myrobotlab.service.config;

import java.util.Map;

public class AbstractSpeechSynthesisConfig extends ServiceConfig {
  
  public boolean mute;
  public boolean blocking;
  public Map<String,String> substitutions;
  public String voice;

}
