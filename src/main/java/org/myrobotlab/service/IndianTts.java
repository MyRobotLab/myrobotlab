package org.myrobotlab.service;

import java.io.IOException;
import java.net.URLEncoder;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.config.IndianTtsConfig;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.data.HttpData;
import org.slf4j.Logger;

/**
 * Indian TTS speech to text service based on http://indiantts.com This is a
 * cloud service and depends on a subscription key to the cloud provider
 * 
 * http://indiantts.com/
 */
public class IndianTts extends AbstractSpeechSynthesis<IndianTtsConfig> {

  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(IndianTts.class);

  transient HttpClient httpClient = null;

  public final String INDIANTTS_USER_USERID = "indiantts.user.userid";
  public final String INDIANTTS_USER_API = "indiantts.user.api";

  public IndianTts(String n, String id) {
    super(n, id);
  }

  @Override
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws IOException {

    String userid = getKey(INDIANTTS_USER_USERID);
    String secret = getKey(INDIANTTS_USER_API);

    // check keys
    if (userid == null) {
      error("%s needs to be set - http://ivr.indiantts.co.in", INDIANTTS_USER_USERID);
      return null;
    }

    if (secret == null) {
      error("%s needs to be set - http://ivr.indiantts.co.in", INDIANTTS_USER_API);
      return null;
    }

    String encoded = URLEncoder.encode(toSpeak, "UTF-8");

    // TODO: also the speed setting is passed in as s=
    String uri = "http://ivrapi.indiantts.co.in/tts?type=indiantts&text=" + encoded + "&api_key=" + secret + "&user_id=" + userid + "&action=play";

    HttpData data = httpClient.getResponse(uri);
    // check response
    if (!"audio/x-wav".equals(data.contentType)) {
      String ret = new String(data.data);
      error("non-audio data returned - %s", ret);
      return null;
    }

    // save data to cache file if valid
    if (data.data == null || data.data.length == 0) {
      error("%s returned 0 byte file !!! - it may block you", getName());
      return null;
    } else {
      FileIO.toFile(audioData.getFileName(), data.data);
    }

    return audioData;
  }

  public void setKeys(String keyId, String keyIdSecret) {
    Security security = Runtime.getSecurity();
    security.setKey(INDIANTTS_USER_USERID, keyId);
    security.setKey(INDIANTTS_USER_API, keyIdSecret);
    broadcastState();
  }

  @Override
  public String[] getKeyNames() {
    return new String[] { INDIANTTS_USER_USERID, INDIANTTS_USER_API };
  }

  @Override
  public void loadVoices() {
    addVoice("Sri", "female", "hi", null);
  }

  public static void main(String[] args) throws Exception {

    LoggingFactory.init(Level.INFO);
    // try {
    // Runtime.start("webgui", "WebGui");
    Runtime.start("gui", "SwingGui");
    IndianTts indianTts = (IndianTts) Runtime.start("indianTts", "IndianTts");
    // Runtime.start("gui", "SwingGui");
    // demo api key
    // indianTts.setKeys("XXXXXXX", "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    indianTts.speakBlocking("नमस्ते भारत मित्र");
    indianTts.speak("नमस्ते नमस्ते भारत मित्र");

    // }
  }

}
