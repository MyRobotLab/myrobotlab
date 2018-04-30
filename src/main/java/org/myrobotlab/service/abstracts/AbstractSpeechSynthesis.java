package org.myrobotlab.service.abstracts;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.AudioFile;
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
  private String audioCacheExtension = "mp3";
  private transient List<String> voiceList = new ArrayList<String>();
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

  /**
   * start callback for speech synth. (Invoked when speaking starts)
   */
  public String publishStartSpeaking(String utterance) {
    log.info("publishStartSpeaking - {}", utterance);
    lastUtterance = utterance;
    broadcastState();
    return utterance;
  }

  /**
   * stop callback for speech synth. (Invoked when speaking stops.)
   */
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

    // just dust it ...
    if (file.exists() && file.length() == 0) {
      file.delete();
      log.info(localFileName + " deleted because size=0");
    }

    if (!audioFile.cacheContains(localFileName)) {
      log.info("retrieving speech from tts - {}", localFileName);
      mp3File = generateByteAudio(toSpeak);
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

  public AudioData speak(String toSpeak) {

    toSpeak = cleanUptext(toSpeak);

    AudioData audioData = null;
    try {
      cacheFile(toSpeak);

      audioData = audioFile.playCachedFile(getLocalFileName(this, toSpeak));
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    utterances.put(audioData, toSpeak);
    return audioData;
  }

  @Override
  public boolean speakBlocking(String toSpeak) {
    toSpeak = cleanUptext(toSpeak);
    try {
      cacheFile(toSpeak);

      invoke("publishStartSpeaking", toSpeak);
      audioFile.playBlocking(audioFile.getGlobalFileCacheDir() + File.separator + getLocalFileName(this, toSpeak));
      invoke("publishEndSpeaking", toSpeak);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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

  protected void subSpeechStartService() {
    audioFile = (AudioFile) startPeer("audioFile");
    audioFile.startService();
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

  public void setVoiceList(List<String> voiceList) {
    this.voiceList = voiceList;
  }

  public String getAudioCacheExtension() {
    return audioCacheExtension;
  }

  public void setAudioCacheExtension(String audioCacheExtension) {
    this.audioCacheExtension = audioCacheExtension;
  }
}
