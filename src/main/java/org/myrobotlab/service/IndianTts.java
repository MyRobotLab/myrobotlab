package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.apache.commons.codec.digest.DigestUtils;
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
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.interfaces.AudioListener;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.slf4j.Logger;

/**
 * Indian TTS speech to text service based on http://indiantts.com
 * This code is basically all the same as NaturalReaderSpeech by Kwatters...
 */
public class IndianTts extends AbstractSpeechSynthesis implements TextListener, AudioListener {

  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(IndianTts.class);
  // default voice

  public String voice = "Default";
  public String api = "";
  public String userid = "";
  public boolean credentialsError = false;
  public HashMap<String, Integer> voiceMap = new HashMap<String,Integer>();
  transient AudioFile audioFile = null;
  // private float volume = 1.0f;
  transient CloseableHttpClient client;
  transient Stack<String> audioFiles = new Stack<String>();
  // audioData to utterance map TODO: revisit the design of this
  transient HashMap<AudioData, String> utterances = new HashMap<AudioData, String>();

  private String language;
  
  public IndianTts(String reservedKey) {
    super(reservedKey);
  }

  public void startService() {
    super.startService();
    if (client == null) {
        // new MultiThreadedHttpConnectionManager()
        client = HttpClients.createDefault();
    }
    audioFile = (AudioFile) startPeer("audioFile");
    audioFile.startService();
    subscribe(audioFile.getName(), "publishAudioStart");
    subscribe(audioFile.getName(), "publishAudioEnd");
    // attach a listener when the audio file ends playing.
    audioFile.addListener("finishedPlaying", this.getName(), "publishEndSpeaking");
    
    voiceMap.put("Default",1);
      
  }

  public AudioFile getAudioFile() {
    return audioFile;
  }
  
  @Override
  public ArrayList<String> getVoices() {
    // TODO:return the list of voices names for this.
   
    ArrayList<String> voices = new ArrayList<String>();
    voices.addAll(voices);
    
    return null;
  }

  @Override
  public String getVoice() {
    return voice;
  }
  
  public String getMp3Url(String toSpeak) {
    
    // TODO: url encode this.
    
    String encoded = toSpeak;
    try {
      encoded = URLEncoder.encode(toSpeak, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }    
    
    int voiceId = voiceMap.get(voice);

    // TOOD: also the speed setting is passed in as s= 

    String url = "http://ivrapi.indiantts.co.in/tts?type=indiantts&text=" + encoded + "&api_key=" + api + "&user_id=" + userid + "&action=play"; 
    log.info("URL FOR AUDIO:{}",url);
    return url;
  }

  
  public byte[] getRemoteFile(String toSpeak) {

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

      if (b == null || b.length == 0 || b.length == 81 || b.length == 47){
        error("%s returned 0 byte file or API error !!! - it may block you", getName());
        credentialsError = true; 
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
  public boolean speakBlocking(String toSpeak) throws IOException {
    log.info("speak blocking {}", toSpeak);
    
    if (api == "" || userid == "") {
      error("Api or userid not set");
      credentialsError = true; 
      return false;
    }

    if (voice == null) {
      log.warn("voice is null! setting to default: Default");
      voice = "Default";
    }
    String localFileName = getLocalFileName(this, toSpeak, "mp3");
    String filename = AudioFile.globalFileCacheDir + File.separator + localFileName;
    if (!audioFile.cacheContains(localFileName)) {
      byte[] b = getRemoteFile(toSpeak);
      audioFile.cache(localFileName, b, toSpeak);
    }
    invoke("publishStartSpeaking", toSpeak);
    audioFile.playBlocking(filename);
    invoke("publishEndSpeaking", toSpeak);
    log.info("Finished waiting for completion.");
    return false;
  }
  
  @Override
  public void setVolume(float volume) {
    // TODO: fix the volume control
    log.warn("Volume control not implemented in Indian Tts yet.");
  }
  

  @Override
  public float getVolume() {
    return 0;
  }
  
  @Override
  public void interrupt() {
    // TODO: Implement me!
  }

  @Override
  public void onText(String text) {
    log.info("ON Text Called: {}", text);
    try {
      speak(text);
    } catch (Exception e) {
      Logging.logError(e);
    }
  }
    
  @Override
  public String getLanguage() {
    return null;
  }
  
  public AudioData speak(String toSpeak) throws IOException {
    // this will flip to true on the audio file end playing.
    AudioData ret = null;
    log.info("speak {}", toSpeak);
    if (api == "" || userid == "") {
      error("Api or userid not set");
      credentialsError = true; 
      return ret;
    }
    if (voice == null) {
      log.warn("voice is null! setting to default: Defaut");
      voice = "Defaut";
    }
    String filename = this.getLocalFileName(this, toSpeak, "mp3");
    if (audioFile.cacheContains(filename)) {
      ret = audioFile.playCachedFile(filename);
      utterances.put(ret, toSpeak);
      return ret;
    }
    audioFiles.push(filename);
    byte[] b = getRemoteFile(toSpeak);
    audioFile.cache(filename, b, toSpeak);
    ret = audioFile.playCachedFile(filename);
    utterances.put(ret, toSpeak);
    return ret;
  }
  
  @Override
  public String getLocalFileName(SpeechSynthesis provider, String toSpeak, String audioFileType) throws UnsupportedEncodingException {
    // TODO: make this a base class sort of thing.
    return provider.getClass().getSimpleName() + File.separator + URLEncoder.encode(provider.getVoice(), "UTF-8") + File.separator + DigestUtils.md5Hex(toSpeak) + "."
        + audioFileType;
  }

  @Override
  public void addEar(SpeechRecognizer ear) {
    // TODO: move this to a base class. it's basically the same for all
    // mouths/ speech synth stuff.
    // when we add the ear, we need to listen for request confirmation
    addListener("publishStartSpeaking", ear.getName(), "onStartSpeaking");
    addListener("publishEndSpeaking", ear.getName(), "onEndSpeaking");
  }

  public void onRequestConfirmation(String text) {
    try {
      speakBlocking(String.format("did you say. %s", text));
    } catch (Exception e) {
      Logging.logError(e);
    }
  }
  
  @Override
  public List<String> getLanguages() {
    // TODO Auto-generated method stub
    ArrayList<String> ret = new ArrayList<String>();
    // FIXME - add iso language codes currently supported e.g. en en_gb de
    // etc..
    return ret;
  }
  
  @Override
  public String publishStartSpeaking(String utterance) {
    log.info("publishStartSpeaking {}", utterance);
    return utterance;
  }

  @Override
  public String publishEndSpeaking(String utterance) {
    log.info("publishEndSpeaking {}", utterance);
    return utterance;
  }
  

  @Override
  public void onAudioStart(AudioData data) {
    log.info("onAudioStart {} {}", getName(), data.toString());
    // filters on only our speech
    if (utterances.containsKey(data)) {
      String utterance = utterances.get(data);
      invoke("publishStartSpeaking", utterance);
    }
  }

  @Override
  public void onAudioEnd(AudioData data) {
    log.info("onAudioEnd {} {}", getName(), data.toString());
    // filters on only our speech
    if (utterances.containsKey(data)) {
      String utterance = utterances.get(data);
      invoke("publishEndSpeaking", utterance);
      utterances.remove(data);
    }
  }

  @Override
  public boolean setVoice(String voice) {
    if (voiceMap.containsKey(voice)) {
      this.voice = voice;
      return true;
    }
    return false;
  }

  @Override
  public void setLanguage(String l) {
	  this.language=l;   
  }
  
  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(IndianTts.class.getCanonicalName());
    meta.addDescription("Hindi tts support");
    meta.addCategory("speech");
    meta.setSponsor("moz4r");
    meta.addPeer("audioFile", "AudioFile", "audioFile");
//    meta.addTodo("test speak blocking - also what is the return type and AudioFile audio track id ?");
    meta.addDependency("org.apache.commons.httpclient", "4.5.2");
    return meta;
  }
  
  public static void main(String[] args) throws Exception {
 
    LoggingFactory.init(Level.INFO);
    //try {
      // Runtime.start("webgui", "WebGui");
      IndianTts speech = (IndianTts) Runtime.start("speech", "IndianTts");
      //demo api key
      speech.api="2d108780-0b86-11e6-b056-07d516fb06e1";
      speech.userid="80";
      speech.speakBlocking("नमस्ते भारत मित्र");

      speech.speak("नमस्ते भारत मित्र");
  
    //}
  }

}
