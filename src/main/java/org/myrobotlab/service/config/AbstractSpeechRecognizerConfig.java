package org.myrobotlab.service.config;

public abstract class AbstractSpeechRecognizerConfig extends ServiceConfig {

  public boolean listening;
  public String[] textListeners;
  public String wakeWord;
     
}
