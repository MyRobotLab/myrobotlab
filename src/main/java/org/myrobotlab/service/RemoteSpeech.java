package org.myrobotlab.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.config.HttpClientConfig;
import org.myrobotlab.service.config.RemoteSpeechConfig;
import org.myrobotlab.service.config.RemoteSpeechConfig.Endpoint;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

/**
 * A generalized "remote" speech synthesis interface service.  I can be used for potentially many
 * remote TTS services, however, the first one will be MozillaTTS, which we will assume is 
 * working locally with docker. See https://github.com/synesthesiam/docker-mozillatts.
 * Example GET: http://localhost:5002/api/tts?text=Hello%20I%20am%20a%20speech%20synthesis%20system%20version%202
 * 
 * @author GroG
 *
 */
public class RemoteSpeech extends AbstractSpeechSynthesis<RemoteSpeechConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(RemoteSpeech.class);

  /**
   * HttpClient peer for GETs and POSTs
   */
  public transient HttpClient<HttpClientConfig> http = null;
  
  public RemoteSpeech(String n, String id) {
    super(n, id);
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public void startService() {
    super.startService();
    http = (HttpClient)startPeer("http");
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("webgui", "WebGui");
      Runtime.start("python", "Python");
      Runtime.start("mouth12", "RemoteSpeech");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
  
  public void setSpeechType(String type) {
    RemoteSpeechConfig.Endpoint  endpoint = config.speechTypes.get(type);
    if (endpoint == null) {
      error("SpeechType %s not found", type);
      return;
    }
    config.speechType = type;
    info("Setting speech type to %s", type);
    broadcastState();
  }

  @Override
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws Exception {

    try {
      // IF GET must url encode .. use replace tags like {urlEncodedText}
      String localFileName = getLocalFileName(toSpeak);
      // merge template with text and/or config
      RemoteSpeechConfig.Endpoint endpoint = config.speechTypes.get(config.speechType);
      if (endpoint == null) {
        error("Remote speech requires an endpoint");
        return null;
      }
      if (endpoint.verb == null) {
        error("An HTTP verb is required in the endpoint.");
        return null;
      }
      
      Map<String,String> headers = null;
      
      if (endpoint.authToken != null) {
        headers = new HashMap<>();
        headers.put("Authorization", String.format("Bearer %s", endpoint.authToken));
        headers.put("Content-Type", "application/json");
      }
      
 
      String body = null;
      if (endpoint.template != null) {
        body = endpoint.template.replace("{{text}}", toSpeak.replace("\n", " ").replace("\"", "").replace("'", ""));
      }
      
      if ("post".equals(endpoint.verb.toLowerCase())) {
        byte[] bytes = http.postBytes(endpoint.url, headers, body.getBytes());        
        FileIO.toFile(localFileName, bytes);
        return new AudioData(localFileName);        
      } else {
        // FIXME add Authorization header if available
        String urlEncodedText = URLEncoder.encode(toSpeak, StandardCharsets.UTF_8.toString());
        String url = endpoint.url.replace("{{text}}", urlEncodedText);
        byte[] bytes = http.getBytes(url);
        FileIO.toFile(localFileName, bytes);
        return new AudioData(localFileName);        
      }
      
    } catch (Exception e) {
      error(e);
    }

    return null;
  }
  
  public void addSpeechType(String name, LinkedHashMap<String, Object> endpoint) {
    Endpoint ep = new Endpoint();
    ep.url = (String)endpoint.get("url");
    ep.verb = (String)endpoint.get("verb");
    ep.template = (String)endpoint.get("template");
    ep.authToken = (String)endpoint.get("authToken");
    config.speechTypes.put(name, ep);
    broadcastState();
  }

  @Override
  public void loadVoices() throws Exception {
    addVoice("default", null, null, "remote");
  }
}
