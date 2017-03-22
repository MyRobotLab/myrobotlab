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
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.interfaces.AudioListener;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.slf4j.Logger;

/**
 * Natural Reader speech to text service based on naturalreaders.com
 * This code is basically all the same as AcapelaSpeech...
 */
public class NaturalReaderSpeech extends Service implements TextListener, SpeechSynthesis, AudioListener {

  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(NaturalReaderSpeech.class);
  // default voice
  // TODO: natural reader has this voice.. there are others
  // but for now.. only Ryan is wired in.. it maps to voice id "33"
  public String voice = "Ryan";
  public HashMap<String, Integer> voiceMap = new HashMap<String,Integer>();
  transient AudioFile audioFile = null;
  // private float volume = 1.0f;
  transient CloseableHttpClient client;
  transient Stack<String> audioFiles = new Stack<String>();
  // audioData to utterance map TODO: revisit the design of this
  transient HashMap<AudioData, String> utterances = new HashMap<AudioData, String>();
  
  public NaturalReaderSpeech(String reservedKey) {
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
    // needed because of an ssl error on the natural reader site
    System.setProperty("jsse.enableSNIExtension", "false");
    
    voiceMap.put("Mike",1);
    voiceMap.put("Crystal",11);
    voiceMap.put("Rich",13);
    voiceMap.put("Ray",14);
    voiceMap.put("Heather",26); 
    voiceMap.put("Laura",17);
    voiceMap.put("Lauren",17);
    voiceMap.put("Ryan",33);
    voiceMap.put("Peter",31);
    voiceMap.put("Rachel",32);
    voiceMap.put("Charles",2);
    voiceMap.put("Audrey",3);
    voiceMap.put("Graham",25);
    voiceMap.put("Bruno",22);
    voiceMap.put("Alice",21);
    voiceMap.put("Alain",7);
    voiceMap.put("Juliette",8); 
    voiceMap.put("Klaus",28);
    voiceMap.put("Sarah",35);
    voiceMap.put("Reiner",5);
    voiceMap.put("Klara",6);
    voiceMap.put("Rose",20);
    voiceMap.put("Alberto",19); 
    voiceMap.put("Vittorio",36);
    voiceMap.put("Chiara",23);
    voiceMap.put("Anjali",4);
    voiceMap.put("Arnaud",9);
    voiceMap.put("Giovanni",10); 
    voiceMap.put("Crystal",11);
    voiceMap.put("Francesca",12);
    voiceMap.put("Claire",15);
    voiceMap.put("Julia",16);
    voiceMap.put("Mel",18);
    voiceMap.put("Juli",27);
    voiceMap.put("Laura",29);
    voiceMap.put("Lucy",30);
    voiceMap.put("Salma",34);
    voiceMap.put("Tracy",37);
    voiceMap.put("Lulu",38);
    voiceMap.put("Sakura",39);
    voiceMap.put("Mehdi",40);

    
  }

  public AudioFile getAudioFile() {
    return audioFile;
  }
  
  @Override
  public ArrayList<String> getVoices() {
    // TODO:return the list of voices names for this.
   
    ArrayList<String> voices = new ArrayList<String>();
    voices.addAll(voices);
    

     
//    Mike
//    Crystal
//    Ray
//    Lauren
//    Rich
//    Julia
//    Mel
//    Claire
//    Ryan
//    Heather
//    Laura
//    Charles
//    Audrey
//    Peter
//    Rachel
//    Graham
//    Lucy
//    Anjali
//    Alain
//    Juliette
//    Bruno
//    Alice
//    Reiner
//    Klara
//    Rosa
//    Alberto
//    Giovanni
//    Francesca
//    Salma
//    Mehdi

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
    
    // TODO: expose thge "r=33" as the selection for Ryans voice.
    // TOOD: also the speed setting is passed in as s= 
    String url = "https://api.naturalreaders.com/v2/tts/?t=" + encoded + "&r="+voiceId+"&s=0";
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
      if (b == null || b.length == 0){
        error("%s returned 0 byte file !!! - it may block you", getName());
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

    if (voice == null) {
      log.warn("voice is null! setting to default: Ryan");
      voice = "Ryan";
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
    log.warn("Volume control not implemented in Natural Reader Speech yet.");
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
    if (voice == null) {
      log.warn("voice is null! setting to default: Ryan");
      voice = "Ryan";
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

  public AudioData speak(String voice, String toSpeak) throws IOException {
    setVoice(voice);
    return speak(toSpeak);
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
    // TODO this is ignored.. only Ryan voice currently enabled.    
  }
  
  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(NaturalReaderSpeech.class.getCanonicalName());
    meta.addDescription("Natural Reader based speech service.");
    meta.addCategory("speech");
    meta.setSponsor("kwatters");
    meta.addPeer("audioFile", "AudioFile", "audioFile");
//    meta.addTodo("test speak blocking - also what is the return type and AudioFile audio track id ?");
    meta.addDependency("org.apache.commons.httpclient", "4.5.2");
    return meta;
  }
  
  public static void main(String[] args) throws Exception {
 
    LoggingFactory.init(Level.INFO);
    //try {
      // Runtime.start("webgui", "WebGui");
      NaturalReaderSpeech speech = (NaturalReaderSpeech) Runtime.start("speech", "NaturalReaderSpeech");
      // speech.setVoice("Ryan");
      // TODO: fix the volume control
      // speech.setVolume(0);
     // speech.speakBlocking("does this work");
//       speech.getMP3ForSpeech("hello world");
      
      speech.speakBlocking("does this work");
      
      speech.setVoice("Lauren");

      speech.speakBlocking("horray for worky!");
  
    //}
  }


}
