package org.myrobotlab.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.interfaces.AudioListener;
import org.myrobotlab.service.interfaces.TextListener;
import org.slf4j.Logger;

/**
 * Indian TTS speech to text service based on http://indiantts.com This code is
 * basically all the same as NaturalReaderSpeech by Kwatters...
 */
public class IndianTts extends AbstractSpeechSynthesis implements TextListener, AudioListener {

  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(IndianTts.class);
  // default voice

  private String voice = "Default";

  transient CloseableHttpClient client;

  public IndianTts(String reservedKey) {
    super(reservedKey);
  }

  public void startService() {
    super.startService();
    security = (Security) startPeer("security");
    if (client == null) {
      // new MultiThreadedHttpConnectionManager()
      client = HttpClients.createDefault();
    }
    subSpeechStartService();

    setEngineError("Online");
    setEngineStatus(true);
  }

  @Override
  public List<String> getVoices() {

    voiceList.clear();
    voiceList.add("Default");

    return voiceList;
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

  public byte[] generateByteAudio(String toSpeak) {

    String mp3Url = getMp3Url(toSpeak);
    HttpGet get = null;
    byte[] b = null;
    try {
      HttpResponse response = null;
      // fetch file
      get = new HttpGet(mp3Url);
      log.info("mp3Url {}", mp3Url);
      // get mp3 file & save to cache
      response = client.execute(get);
      log.info("got {}", response.getStatusLine());
      HttpEntity entity = response.getEntity();
      // cache the mp3 content
      b = FileIO.toByteArray(entity.getContent());

      if (b == null || b.length == 0 || b.length == 81 || b.length == 47) {
        error("%s returned 0 byte file or API error !!! - it may block you", getName());
        b = null;
        setEngineError("API Error");
        setEngineStatus(false);
      }
      EntityUtils.consume(entity);
    } catch (Exception e) {
      Logging.logError(e);
    } finally {
      if (get != null) {
        get.releaseConnection();
      }
    }
    return b;
  }

  @Override
  public String getLanguage() {
    return null;
  }

  public List<String> getLanguages() {
    log.warn("not yet implemented");
    return null;
  }

  @Override
  public void setLanguage(String l) {
    // todo : implement generic method & language code

    log.warn("not yet implemented");
  }

  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(IndianTts.class.getCanonicalName());
    meta.addDescription("Hindi tts support");
    meta.addCategory("speech");
    meta.setSponsor("moz4r");
    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.addPeer("security", "Security", "security");
    // meta.addTodo("test speak blocking - also what is the return type and
    // AudioFile audio track id ?");
    // FIXME - addPeer("httpClient","HttpClient") - to pull in dependencies -
    // and use Mrl's HttpClient service
    // then its "one HttpClient to Rule them all"
    meta.addDependency("org.apache.httpcomponents", "httpclient", "4.5.2");
    meta.addDependency("org.apache.httpcomponents", "httpcore", "4.4.6");

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

    indianTts.speak("नमस्ते भारत मित्र");

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
}
