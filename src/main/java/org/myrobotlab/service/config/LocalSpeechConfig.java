package org.myrobotlab.service.config;

import java.util.Map;

public class LocalSpeechConfig extends SpeechSynthesisConfig {

  /**
   * speech template type - can be Say, Festival, Espeak, etc
   */
  public String speechType;
  /**
   * replacement characters - by default double quotes will escaped unless this is
   * explicitly defined
   */
  public Map<String,String> replaceChars;
}
