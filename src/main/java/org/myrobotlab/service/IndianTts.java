package org.myrobotlab.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.slf4j.Logger;

/**
 * Indian TTS speech to text service based on http://indiantts.com This code is
 * basically all the same as NaturalReaderSpeech by Kwatters...
 */
public class IndianTts extends AbstractSpeechSynthesis {

  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(IndianTts.class);
  // stored inside json, this must be UNIQUE identifiers
  HashMap<String, String> voiceInJsonConfig;
  // end

  // default voice

  private String voice = "Default";
  transient HttpClient httpClient = null;

  public IndianTts(String reservedKey) {
    super(reservedKey);
  }

  public void startService() {
    super.startService();
    httpClient = (HttpClient) startPeer("httpClient");
    httpClient.startService();
    security = (Security) startPeer("security");

    subSpeechStartService();

    setEngineError("Online");
    setEngineStatus(true);
  }

  @Override
  public List<String> getVoices() {

    getVoiceList().clear();
    getVoiceList().add("Default");

    return getVoiceList();
  }

  public String getMp3Url(String toSpeak) {

    String userid = getKeys()[0];
    String secret = getKeys()[1];

    String encoded = toSpeak;
    try {
      encoded = URLEncoder.encode(toSpeak, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // TOOD: also the speed setting is passed in as s=

    String url = "http://ivrapi.indiantts.co.in/tts?type=indiantts&text=" + encoded + "&api_key=" + secret + "&user_id=" + userid + "&action=play";
    log.info("URL FOR AUDIO:{}", url);
    return url;
  }

  @Override
  public byte[] generateByteAudio(String toSpeak) {
    String mp3Url = getMp3Url(toSpeak);

    byte[] b = null;
    try {

      log.info("mp3Url {}", mp3Url);
      // get mp3 file & save to cache
      // cache the mp3 content
      b = httpClient.getBytes(mp3Url);
      if (b == null || b.length == 0) {
        error("%s returned 0 byte file !!! - it may block you", getName());
        b = null;
      }

    } catch (Exception e) {
      Logging.logError(e);
    }

    return b;
  }


  static public ServiceType getMetaData() {
    // ServiceType meta = new ServiceType(IndianTts.class.getCanonicalName());
    ServiceType meta = AbstractSpeechSynthesis.getMetaData(IndianTts.class.getCanonicalName());
    meta.addDescription("Hindi tts support");
    meta.addCategory("speech");
    meta.setSponsor("moz4r");
    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.addCategory("speech", "sound");
 
    meta.addPeer("security", "Security", "security");
    meta.addPeer("httpClient", "HttpClient", "httpClient");
    // meta.addTodo("test speak blocking - also what is the return type and
    // AudioFile audio track id ?");

    return meta;
  }

  public static void main(String[] args) throws Exception {

    LoggingFactory.init(Level.INFO);
    // try {
    // Runtime.start("webgui", "WebGui");
    IndianTts indianTts = (IndianTts) Runtime.start("indianTts", "IndianTts");
    Runtime.start("gui", "SwingGui");
    // demo api key
    // indianTts.setKeys("80", "2d108780-0b86-11e6-b056-07d516fb06e1");

    indianTts.speakBlocking("नमस्ते भारत मित्र");

    indianTts.speak("नमस्ते नमस्ते भारत मित्र");

    // }
  }

  @Override
  public void setKeys(String keyId, String keyIdSecret) {
    security.addSecret("indiantts.user.userid", keyId);
    security.addSecret("indiantts.user.api", keyIdSecret);
    security.saveStore();
    getVoices();
    setVoice(this.voice);
    broadcastState();

  }

  @Override
  public String[] getKeys() {
    String[] Keys = new String[2];
    security.loadStore();
    Keys[0] = security.getSecret("indiantts.user.userid");
    Keys[1] = security.getSecret("indiantts.user.api");
    return Keys;
  }

  @Override
  public String getVoiceInJsonConfig() {
    if (voiceInJsonConfig == null) {
      voiceInJsonConfig = new HashMap<String, String>();
    }

    return voiceInJsonConfig.get(this.getClass().getSimpleName());
  }

  @Override
  public void setVoiceInJsonConfig(String voice) {
    voiceInJsonConfig.put(this.getClass().getSimpleName(), voice);

  }

}
