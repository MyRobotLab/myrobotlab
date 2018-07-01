package org.myrobotlab.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

/**
 * Indian TTS speech to text service based on http://indiantts.com This code is
 * basically all the same as NaturalReaderSpeech by Kwatters...
 * 
 * http://indiantts.com/
 */
public class IndianTts extends AbstractSpeechSynthesis {

  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(IndianTts.class);

  transient HttpClient httpClient = null;

  public final String INDIANTTS_USER_USERID = "indiantts.user.userid";
  public final String INDIANTTS_USER_API = "indiantts.user.api";

  public IndianTts(String reservedKey) {
    super(reservedKey);
  }

  public void startService() {
    super.startService();
    httpClient = (HttpClient) startPeer("httpClient");
    httpClient.startService();
  }

  public String getMp3Url(String toSpeak) {
    Security security = Runtime.getSecurity();

    String userid = security.getKey(INDIANTTS_USER_USERID);
    String secret = security.getKey(INDIANTTS_USER_API);

    String encoded = toSpeak;
    try {
      encoded = URLEncoder.encode(toSpeak, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      log.error(e.getMessage(), e);
    }

    // TOOD: also the speed setting is passed in as s=

    String url = "http://ivrapi.indiantts.co.in/tts?type=indiantts&text=" + encoded + "&api_key=" + secret + "&user_id=" + userid + "&action=play";
    log.info("URL FOR AUDIO:{}", url);
    return url;
  }

  @Override
  public AudioData generateAudioData(String toSpeak) {
   
    AudioData audioData = null;
    
    try {
      String cache = getLocalFileName(toSpeak);
      audioData = new AudioData(cache);

      String userid = getKey(INDIANTTS_USER_USERID);
      String secret = getKey(INDIANTTS_USER_API);

      String encoded = URLEncoder.encode(toSpeak, "UTF-8");
      
      // TOOD: also the speed setting is passed in as s=

      String mp3Url = "http://ivrapi.indiantts.co.in/tts?type=indiantts&text=" + encoded + "&api_key=" + secret + "&user_id=" + userid + "&action=play";

      byte[] b = null;
      // get mp3 file & save to cache
      // cache the mp3 content
      b = httpClient.getBytes(mp3Url);
      if (b == null || b.length == 0) {
        error("%s returned 0 byte file !!! - it may block you", getName());
        return null;
      }

    } catch (Exception e) {
      log.error("generateAudioData threw",e);
    }

    return audioData;
  }

  static public ServiceType getMetaData() {
    
    ServiceType meta = AbstractSpeechSynthesis.getMetaData(IndianTts.class.getCanonicalName());
    meta.addDescription("Hindi tts support");
    meta.setCloudService(true);
    meta.addCategory("speech");
    meta.setSponsor("moz4r");
    meta.addCategory("speech", "sound");
    meta.addPeer("httpClient", "HttpClient", "httpClient");
 
    return meta;
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
  protected void loadVoices() {
    addVoice("Sri", "female", "hi", null);
  }
  
  public static void main(String[] args) throws Exception {

    LoggingFactory.init(Level.INFO);
    // try {
    // Runtime.start("webgui", "WebGui");
    IndianTts indianTts = (IndianTts) Runtime.start("indianTts", "IndianTts");
    // Runtime.start("gui", "SwingGui");
    // demo api key
    // indianTts.setKeys("XXXXXXX", "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    indianTts.speakBlocking("नमस्ते भारत मित्र");
    indianTts.speak("नमस्ते नमस्ते भारत मित्र");

    // }
  }


}
