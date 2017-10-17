package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.interfaces.AudioListener;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

public class MicrosoftLocalTTS extends AbstractSpeechSynthesis implements AudioListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(MicrosoftLocalTTS.class);

  transient Integer voice = 0;
  transient String voiceName;
  transient List<Integer> voices;
  private String ttsFolder = "tts";
  private String ttsExecutable = ttsFolder + File.separator+"tts.exe";
  public String ttsExeOutputFilePath = System.getProperty("user.dir") + File.separator + ttsFolder + File.separator;
  boolean ttsExecutableExist;

  // this is a peer service.
  transient AudioFile audioFile = null;

  transient Map<Integer, String> voiceMap = new HashMap<Integer, String>();

  Stack<String> audioFiles = new Stack<String>();

  transient HashMap<AudioData, String> utterances = new HashMap<AudioData, String>();

  String language = "en";

  public MicrosoftLocalTTS(String n) {
    super(n);
  }

  @Override
  public List<String> getVoices() {
    ArrayList<String> args = new ArrayList<String>();
    args.add("-V");
    String cmd = Runtime.execute(System.getProperty("user.dir") + File.separator + ttsExecutable, "-V");
    String[] lines = cmd.split(System.getProperty("line.separator"));
    List<String> voiceList = (List<String>) Arrays.asList(lines);

    for (int i = 0; i < voiceList.size() && i < 10; i++) {
      // error(voiceList.get(i).substring(0,2));
      if (voiceList.get(i).substring(0, 1).matches("\\d+")) {
        voiceMap.put(i, voiceList.get(i).substring(2, voiceList.get(i).length()));
        log.info("voice : " + voiceMap.get(i) + " index : " + i);
      }
    }
    return voiceList;
  }

  @Override
  public boolean setVoice(String voice) {
    getVoices();
    Integer voiceId = Integer.parseInt(voice);
    if (voiceMap.containsKey(voiceId)) {
      this.voiceName = voiceMap.get(voiceId);
      this.voice = voiceId;
      log.info("setting voice to {}", voice, "( ", voiceName, " ) ");
      return true;
    }
    error("could not set voice to {}", voice);
    return false;
  }

  @Override
  public void setLanguage(String l) {
    this.language = l;
  }

  @Override
  public String getLanguage() {
    return language;
  }

  public byte[] cacheFile(String toSpeak) throws IOException {
    byte[] mp3File = null;
    String localFileName = getLocalFileName(this, toSpeak, "mp3");
    if (voiceName == null) {
      setVoice(voice.toString());
    }

    String uuid = UUID.randomUUID().toString();
    if (!audioFile.cacheContains(localFileName)) {
      log.info("retrieving speech from locals - {}", localFileName, voiceName);
      String command = System.getProperty("user.dir") + File.separator + ttsExecutable + " -f 9 -v " + voice + " -t -o " + ttsExeOutputFilePath  + uuid + " \""+toSpeak+" \"";
      File f = new File(ttsExeOutputFilePath + uuid + "0.mp3");
      f.delete();
      String cmd = Runtime.execute("cmd.exe", "/c", command);
      log.debug(command);
      if (!f.exists()) {
        log.error(ttsExecutable + " caused an error : " + cmd);
      } else {
        mp3File = FileIO.toByteArray(f);
        audioFile.cache(localFileName, mp3File, toSpeak);
        f.delete();
      }

    } else {
      log.info("using local cached file");
      mp3File = FileIO.toByteArray(new File(AudioFile.globalFileCacheDir + File.separator + getLocalFileName(this, toSpeak, "mp3")));
    }

    return mp3File;
  }

  @Override
  public AudioData speak(String toSpeak) throws Exception {
    toSpeak = toSpeak.replaceAll("\\s{2,}", " ");
    cacheFile(toSpeak);
    AudioData audioData = audioFile.playCachedFile(getLocalFileName(this, toSpeak, "mp3"));
    utterances.put(audioData, toSpeak);
    return audioData;
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
  public boolean speakBlocking(String toSpeak) throws Exception {
    toSpeak = toSpeak.replaceAll("\\s{2,}", " ");
    cacheFile(toSpeak);
    invoke("publishStartSpeaking", toSpeak);
    audioFile.playBlocking(AudioFile.globalFileCacheDir + File.separator + getLocalFileName(this, toSpeak, "mp3"));
    invoke("publishEndSpeaking", toSpeak);
    return false;
  }

  @Override
  public void setVolume(float volume) {
    audioFile.setVolume(volume);
  }

  @Override
  public float getVolume() {
    return audioFile.getVolume();
  }

  @Override
  public void interrupt() {
    // TODO Auto-generated method stub

  }

  @Override
  public String getVoice() {
    return voice.toString();
  }

  @Override
  public String getLocalFileName(SpeechSynthesis provider, String toSpeak, String audioFileType) throws UnsupportedEncodingException {
    // TODO: make this a base class sort of thing.
    // having - AudioFile.globalFileCacheDir exposed like this is a bad idea ..
    // AudioFile should just globallyCache - the details of that cache should
    // not be exposed :(

    return provider.getClass().getSimpleName() + File.separator + URLEncoder.encode(provider.getVoice(), "UTF-8") + File.separator + DigestUtils.md5Hex(toSpeak) + "."
        + audioFileType;

  }

  // can this be defaulted ?
  @Override
  public void addEar(SpeechRecognizer ear) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onRequestConfirmation(String text) {
    // TODO Auto-generated method stub

  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(MicrosoftLocalTTS.class.getCanonicalName());
    meta.addDescription("implements speech using Microsoft's tts.exe");
    meta.setAvailable(true); // false if you do not want it viewable in a
    // gui
    // add dependency if necessary
    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.addCategory("speech");
    meta.addDependency("tts.microsoftspeech", "1.1");
    return meta;
  }

  @Override
  public void startService() {
    super.startService();
    audioFile = (AudioFile) startPeer("audioFile");
    audioFile.startService();
    subscribe(audioFile.getName(), "publishAudioStart");
    subscribe(audioFile.getName(), "publishAudioEnd");
    // attach a listener when the audio file ends playing.
    audioFile.addListener("finishedPlaying", this.getName(), "publishEndSpeaking");
    File f = new File(System.getProperty("user.dir") + File.separator + ttsExecutable);
    ttsExecutableExist = true;
    if (!f.exists()) {
      error("Missing : " + System.getProperty("user.dir") + File.separator + ttsExecutable);
      ttsExecutableExist = false;
    }

  }

  public static void main(String[] args) throws Exception {

    LoggingFactory.init(Level.INFO);

    MicrosoftLocalTTS microsoftLocalTTS = (MicrosoftLocalTTS) Runtime.start("microsoftLocalTTS", "MicrosoftLocalTTS");
    // microsoftLocalTTS.ttsExeOutputFilePath="c:\\tmp\\";
    microsoftLocalTTS.getVoices();
    microsoftLocalTTS.setVoice("1");
    microsoftLocalTTS.speakBlocking("local tts");
    microsoftLocalTTS.speak("unicode éléphant");

  }

  @Override
  public List<String> getLanguages() {
    // TODO Auto-generated method stub
    return null;
  }

}
