package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class RemoteSpeechConfig extends SpeechSynthesisConfig {

  public String verb = "GET";

  public String url = "http://localhost:5002/api/tts?text={text}";

  public String template = null;
  
  public String speechType = "ModzillaTTS";

  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "http", "HttpClient", true);
    return plan;
  }

}
