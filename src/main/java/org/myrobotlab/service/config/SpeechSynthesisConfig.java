package org.myrobotlab.service.config;

import java.util.Map;

public class SpeechSynthesisConfig extends ServiceConfig {

  public boolean mute = false;
  public boolean blocking = false;
  public String[] speechRecognizers;
  public Map<String, String> substitutions;
  public String voice;
  public String audioFile;

}
