package org.myrobotlab.service.config;

public class SpeechRecognizerConfig extends ServiceConfig {

  public boolean listening = false;
  public boolean recording = false;
  public String[] textListeners;
  public String wakeWord;

}
