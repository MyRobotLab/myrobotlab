package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class Gpt3Config extends ServiceConfig {

  public String currentUserName;

  /**
   * current sleep/wake value
   */
  public boolean sleeping = false;

  public int maxTokens = 256;
  public float temperature = 0.7f;
  public String url = "https://api.openai.com/v1/chat/completions";
  public String token = null;
  public String engine = "gpt-3.5-turbo"; // "text-davinci-003"
  public String wakeWord = "wake";
  public String sleepWord = "sleep";
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    // http peer to retrieve emojis
    addDefaultPeerConfig(plan, name, "http", "HttpClient");    
    return plan;
  }

}
