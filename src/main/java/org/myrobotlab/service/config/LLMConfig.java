package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class LLMConfig extends ServiceConfig {

  public String currentUserName;
  public String type; // Ollama or OpenAI

  /**
   * current sleep/wake value
   */
  public boolean sleeping = false;

  public int maxTokens = 256;
  public float temperature = 0.7f;
  // public String url = "https://api.openai.com/v1/chat/completions";
  // http://localhost:11434/v1/chat/completions
  public String url = null;
  public String password = null;
  public String model = "llama3"; //"gpt-3.5-turbo"; // "text-davinci-003"
  public String wakeWord = "wake";
  public String sleepWord = "sleep";
  
  /**
   * maximum history of chats to re-submit
   */
  public int maxHistory = 5; 

  
  /**
   * static prefix to send to gpt3
   * e.g. " talk like a pirate when responding, "
   */
  public String system = "You are a helpful robot."; 
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    // http peer to retrieve emojis
    addDefaultPeerConfig(plan, name, "http", "HttpClient");    
    return plan;
  }

}
