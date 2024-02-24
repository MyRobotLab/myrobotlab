package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class RemoteSpeechConfig extends SpeechSynthesisConfig {

  /**
   * GET or POST, currently only GET is implemented
   */
  public String verb = "GET";

  /**
   * Speech url {text} will be url encoded text that will be transformed to audio speech
   */
  public String url = "http://localhost:5002/api/tts?text={text}";

  /**
   * Template for POST body, usually JSON, not implemented yet
   */
  public String template = null;
  
  /**
   * Default speech type
   */
  public String speechType = "MozillaTTS";

  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "http", "HttpClient", true);
    return plan;
  }

}
