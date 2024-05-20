package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Plan;

public class RemoteSpeechConfig extends SpeechSynthesisConfig {

  static public class Endpoint {
    public String url;
    public String verb;
    public String template;
    public String authToken;

    public Endpoint() {
    }
  }

  /**
   * 
   */
  public Map<String, Endpoint> speechTypes = new HashMap<>();

  /**
   * GET or POST, currently only GET is implemented
   */
  public String verb = "GET";

  /**
   * Current speech type
   */
  public String speechType = null;

  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "http", "HttpClient", true);

    Endpoint coqui = new Endpoint();
    coqui.url = "http://localhost:5002/api/tts?text={{text}}&speaker_id=p376&style_wav=&language_id=";
    coqui.verb = "GET";
    coqui.template = null;
    coqui.authToken = null;
    speechTypes.put("CoquiAI", coqui);

    Endpoint mozillatts = new Endpoint();
    mozillatts.url = "http://localhost:5002/api/tts?text={{text}}";
    mozillatts.verb = "GET";
    mozillatts.template = null;
    mozillatts.authToken = null;
    speechTypes.put("MozillaTTS", mozillatts);

    Endpoint openai = new Endpoint();
    openai.url = "https://api.openai.com/v1/audio/speech";
    openai.verb = "POST";
    openai.template = "{\"model\": \"tts-1\",\"input\": \"{{text}}\",\"voice\": \"alloy\"}";
    openai.authToken = null;
    speechTypes.put("OpenAI", openai);

    return plan;
  }

}