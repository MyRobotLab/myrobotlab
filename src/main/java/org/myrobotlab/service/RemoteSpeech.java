package org.myrobotlab.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.config.HttpClientConfig;
import org.myrobotlab.service.config.RemoteSpeechConfig;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

/**
 * A generalized "remote" speech synthesis interface service. I can be used for
 * potentially many remote TTS services, however, the first one will be
 * MozillaTTS, which we will assume is working locally with docker. See
 * https://github.com/synesthesiam/docker-mozillatts. Example GET:
 * http://localhost:5002/api/tts?text=Hello%20I%20am%20a%20speech%20synthesis%20system%20version%202
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

  /**
   * Currently only support MozillaTTS
   */
  protected Set<String> types = new HashSet<>(Arrays.asList("MozillaTTS"));

  public RemoteSpeech(String n, String id) {
    super(n, id);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public void startService() {
    super.startService();
    http = (HttpClient) startPeer("http");
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("webgui", "WebGui");
      Runtime.start("python", "Python");
      Runtime.start("mouth", "RemoteSpeech");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws Exception {

    try {
      // IF GET must url encode .. use replace tags like {urlEncodedText}
      String localFileName = getLocalFileName(toSpeak);
      // merge template with text and/or config
      String url = config.url.replace("{text}", URLEncoder.encode(toSpeak, StandardCharsets.UTF_8.toString()));
      byte[] bytes = http.getBytes(url);
      FileIO.toFile(localFileName, bytes);
      return new AudioData(localFileName);
    } catch (Exception e) {
      error(e);
    }

    return null;
  }

  @Override
  public void loadVoices() throws Exception {
    addVoice("default", null, null, "remote");
  }
}