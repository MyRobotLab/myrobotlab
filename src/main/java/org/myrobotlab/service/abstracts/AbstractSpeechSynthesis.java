package org.myrobotlab.service.abstracts;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.AudioFile;
import org.myrobotlab.service.HttpClient;
import org.myrobotlab.service.Security;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

public abstract class AbstractSpeechSynthesis extends Service implements SpeechSynthesis, TextListener {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(AbstractSpeechSynthesis.class);
  private String lastUtterance = "";
  private boolean engineStatus = false;
  private String engineError = "Not initialized";
  transient HashMap<AudioData, String> utterances = new HashMap<AudioData, String>();
  protected String language;
  transient AudioFile audioFile = null;
  protected transient Security security = null;
  protected transient HttpClient httpClient = null;
  private String audioCacheExtension = "mp3";
  private transient List<String> voiceList = new ArrayList<String>();
  private String mrlCloudUrl = "http://www.myai.cloud/mrl/audio/";

  // useful to store personal voice parameter inside json config
  // this var receive info from services

  // This is the format string that will be used when asking for confirmation.
  public String confirmationString = "did you say %s ?";
  /**
   * cache must be based on text + other parameters like filters
   */
  protected String audioCacheParameters = "";

  public AbstractSpeechSynthesis(String reservedKey) {
    super(reservedKey);
  }


  public String publishStartSpeaking(String utterance) {
    log.info("publishStartSpeaking - {}", utterance);
    lastUtterance = utterance;
    broadcastState();
    return utterance;
  }


  public String publishEndSpeaking(String utterance) {
    log.info("publishEndSpeaking - {}", utterance);
    return utterance;
  }

  /**
   * attach method responsible for routing to type-mangled attach
   */
  public void attach(Attachable attachable) {
    if (attachable instanceof TextPublisher) {
      attachTextPublisher((TextPublisher) attachable);
    } else {
      log.error("don't know how to attach a %s", attachable.getName());
    }
  }

  /**
   * detach method responsible for routing to type-mangled attach
   */
  public void detach(Attachable attachable) {
    if (attachable instanceof TextPublisher) {
      detachTextPublisher((TextPublisher) attachable);
    }
  }

  public void attachTextPublisher(TextPublisher textPublisher) {
    subscribe(textPublisher.getName(), "publishText");
    // FIXME -
    // if (!isAttached(textPublisher.getName())){
    // textPublisher.attach(this) ???
  }

  public void detachTextPublisher(TextPublisher textPublisher) {
    unsubscribe(textPublisher.getName(), "publishText");
  }

  public void onText(String text) {
    // default implemetation/behavior for onText
    log.info("ON Text Called: {}", text);
    try {
      speak(text);
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public void onAudioStart(AudioData data) {
    log.info("onAudioStart {} {}", getName(), data.toString());
    // filters on only our speech
    if (utterances.containsKey(data)) {
      String utterance = utterances.get(data);
      invoke("publishStartSpeaking", utterance);
    }
  }

  public String getLanguage() {
    return language;
  }

  public void onAudioEnd(AudioData data) {
    log.info("onAudioEnd {} {}", getName(), data.toString());
    // filters on only our speech
    if (utterances.containsKey(data)) {
      String utterance = utterances.get(data);
      invoke("publishEndSpeaking", utterance);
      utterances.remove(data);
    }
  }

  public void addEar(SpeechRecognizer ear) {
    // when we add the ear, we need to listen for request confirmation
    addListener("publishStartSpeaking", ear.getName(), "onStartSpeaking");
    addListener("publishEndSpeaking", ear.getName(), "onEndSpeaking");
  }

  @Override
  public void setVolume(float volume) {
    audioFile.setVolume(volume);
  }

  public void setVolume(double volume) {
    audioFile.setVolume((float) volume);
  }

  @Override
  public float getVolume() {
    return audioFile.getVolume();
  }

  public void onRequestConfirmation(String text) {
    try {
      // FIXME - not exactly language independent
      speakBlocking(String.format(confirmationString, text));
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public String getLocalFileName(SpeechSynthesis provider, String toSpeak) throws UnsupportedEncodingException {
    if (provider.getVoice() != null) {
      return provider.getClass().getSimpleName() + File.separator + URLEncoder.encode(provider.getVoice(), "UTF-8") + File.separator
          + URLEncoder.encode(audioCacheParameters, "UTF-8") + File.separator + DigestUtils.md5Hex(toSpeak) + "." + getAudioCacheExtension();
    } else {
      return null;
    }
  }

  public byte[] cacheFile(String toSpeak) throws IOException {

    byte[] mp3File = null;
    // cache it begin -----
    String localFileName = getLocalFileName(this, toSpeak);
    File file = new File(AudioFile.getGlobalFileCacheDir() + File.separator + localFileName);

    // just dust it off ...
    if (file.exists() && file.length() == 0) {
      file.delete();
      log.info(localFileName + " deleted, because empty...");
    }

    if (!audioFile.cacheContains(localFileName)) {
      log.info("retrieving speech from tts - {}", localFileName);
      if (toSpeak.startsWith("#")) {
        mp3File = grabRemoteAudioEffect(toSpeak.replace("#", ""));
      } else {
        mp3File = generateByteAudio(toSpeak);
      }
      if (mp3File == null || mp3File.length == 0) {
        log.error("Tried to cache null data... check the speech engine");
        return null;
      }
      audioFile.cache(localFileName, mp3File, toSpeak);
    } else {
      log.info("using local cached file");
      mp3File = FileIO.toByteArray(file);

    }
    return mp3File;
  }

  public AudioData[] speak(String toSpeak) {

    toSpeak = cleanUptext(toSpeak);
    int i = 0;
    String splitedSpeak[] = splitAndConservSeparator(toSpeak, "#");
    AudioData audioData[] = new AudioData[splitedSpeak.length];
    for (String s : splitedSpeak) {

      try {
        cacheFile(s);

        audioData[i] = audioFile.playCachedFile(getLocalFileName(this, s));
        utterances.put(audioData[i], s);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      i += 1;

    }

    return audioData;
  }

  @Override
  public boolean speakBlocking(String toSpeak) {
    toSpeak = cleanUptext(toSpeak);
    int i = 0;
    String splitedSpeak[] = splitAndConservSeparator(toSpeak, "#");
    for (String s : splitedSpeak) {
      try {
        cacheFile(s);

        invoke("publishStartSpeaking", s);
        audioFile.playBlocking(audioFile.getGlobalFileCacheDir() + File.separator + getLocalFileName(this, s));
        invoke("publishEndSpeaking", s);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return false;
  }

  private String cleanUptext(String toSpeak) {

    toSpeak = toSpeak.replaceAll("\\n", " ");
    toSpeak = toSpeak.replaceAll("\\r", " ");
    toSpeak = toSpeak.replaceAll("\\s{2,}", " ");
    if (toSpeak.isEmpty() || toSpeak == " " || toSpeak == null) {
      toSpeak = " , ";
    }
    return toSpeak;
  }

  /**
   * We need to extract voices effect tagged by #
   */
  private String[] splitAndConservSeparator(String input, String regex) {

    Matcher m = Pattern.compile(regex).matcher(input);

    List<String> matchList = new ArrayList<String>();

    int start = 0;
    int end = 0;

    while (m.find()) {

      if (start > 0 && !input.substring(start, m.start()).isEmpty() && (input.substring(start - 1, start).matches(regex)) && (!input.substring(start, start + 1).matches(" "))) {
        log.info("dbg" + input.substring(start, m.start()) + "dbg");

        matchList.add(regex + input.substring(start, m.start()) + regex);

      } else if (!input.substring(start, m.start()).isEmpty()) {
        matchList.add(input.substring(start, m.start()));
      }
      // log.info(start+"tata"+input.length());
      start = m.end();
      end = input.length();

    }

    if (start == 0) {

      return new String[] { input };
    }

    if (start < end) {

      matchList.add(input.substring(start, end));
    }
    return matchList.toArray(new String[matchList.size()]);
  }

  public AudioFile getAudioFile() {
    return audioFile;
  }

  public String getlastUtterance() {
    return lastUtterance;
  }

  public boolean getEngineStatus() {
    return engineStatus;
  }

  public String getEngineError() {
    return engineError;
  }

  public void setEngineStatus(boolean engineStatus) {
    this.engineStatus = engineStatus;
    broadcastState();
  }

  public void setEngineError(String engineError) {
    this.engineError = engineError;
    broadcastState();
  }

  public void interrupt() {
    // never used
  }

  protected static void subGetMetaData(ServiceType meta) {
    meta.addPeer("httpClient", "HttpClient", "httpClient");
    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.addCategory("speech", "sound");
  }

  protected void subSpeechStartService() {

    audioFile = (AudioFile) startPeer("audioFile");
    audioFile.startService();
    httpClient = (HttpClient) startPeer("httpClient");
    httpClient.startService();
    subscribe(audioFile.getName(), "publishAudioStart");
    subscribe(audioFile.getName(), "publishAudioEnd");
    // attach a listener when the audio file ends playing.
    audioFile.addListener("finishedPlaying", this.getName(), "publishEndSpeaking");

    info("Voice in config : " + getVoice());

    setVoice(getVoice());

  }

  public boolean setVoice(String voice) {
    return subSetVoice(voice);

  }

  protected boolean subSetVoice(String voice) {
    getVoices();
    if (voice == null || voice.isEmpty()) {
      voice = getVoiceList().get(0);
    }
    if (getVoiceList().contains(voice)) {
      setVoiceInJsonConfig(voice);
      broadcastState();
      info(this.getIntanceName() + " set voice to : " + voice);
      setEngineError("Ready");
      setEngineStatus(true);
      return true;
    } else {
      error("Unknown " + this.getClass().getSimpleName() + " Voice : " + voice);
      return false;
    }
  }

  public String getVoice() {
    return getVoiceInJsonConfig();

  }

  public List<String> getVoiceList() {
    return voiceList;
  }

  public List<String> getVoiceEffects() {
    JSONArray json = null;
    List<String> list = new ArrayList<String>();
    if (httpClient == null) {
      
      return list;
    }

    try {
      json = new JSONArray(httpClient.get(mrlCloudUrl));
    } catch (Exception e) {
      error("getVoiceEffects error : %s", e);
      return null;
    }
    log.info("getVoiceEffects json : " + json);
    for (int i = 0; i < json.length(); i++) {
      try {
        String content = json.getString(i);
        if (content.contains(".mp3")) {
          list.add("#" + content.replace(".mp3", "") + "#");
        }
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return list;
 
  }

  public void setVoiceList(List<String> voiceList) {
    this.voiceList = voiceList;
  }

  public String getAudioCacheExtension() {
    return audioCacheExtension;
  }

  public void setAudioCacheExtension(String audioCacheExtension) {
    this.audioCacheExtension = audioCacheExtension;
  }

  public byte[] grabRemoteAudioEffect(String effect) throws ClientProtocolException, IOException {

    byte[] audioEffect = httpClient.getBytes(mrlCloudUrl + effect + ".mp3");
    // error detection
    if (audioEffect.length <= 225) {
      log.error("grabRemoteAudioEffect can't grab " + mrlCloudUrl + effect + ".mp3");
      return null;
    }
    log.info("grabRemoteAudioEffect length " + audioEffect.length);

    return audioEffect;

  }

}
