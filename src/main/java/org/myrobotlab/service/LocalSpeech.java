package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.slf4j.Logger;

/**
 * Local OS speech service
 * 
 * windows & macos compatible
 *
 * @author moz4r
 *
 */
public class LocalSpeech extends AbstractSpeechSynthesis {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(LocalSpeech.class);
  // stored inside json

  HashMap<String, String> voiceInJsonConfig = new HashMap<String, String>();
  // end

  private String ttsFolder = "tts";

  private String windowsTtsExecutable = ttsFolder + File.separator + "tts.exe";
  private String macOsTtsExecutable = "say";
  // TODO private String linuxTtsExecutable = "";

  public String ttsExeOutputFilePath = System.getProperty("user.dir") + File.separator + ttsFolder + File.separator;
  boolean ttsExecutableExist;

  transient Map<String, String> voiceMap = new HashMap<String, String>();

  public LocalSpeech(String n) {
    super(n);
  }

  public List<String> getVoices() {
    List<String> list = new ArrayList<String>();
    if (Platform.getLocalInstance().isWindows()) {
      ArrayList<String> args = new ArrayList<String>();
      args.add("-V");
      String cmd = Runtime.execute(System.getProperty("user.dir") + File.separator + windowsTtsExecutable, "-V");

      String[] lines = cmd.split(System.getProperty("line.separator"));
      setVoiceList((List<String>) Arrays.asList(lines));

      for (int i = 0; i < getVoiceList().size() && i < 10; i++) {
        try {
          int voiceNumber = Integer.parseInt((getVoiceList().get(i).substring(0, 2)).replace(" ", ""));
          String voiceName = getVoiceList().get(i).substring(2, getVoiceList().get(i).length());
          log.info("voice : " + voiceName + " index : " + i);
          list.add(voiceName);
          voiceMap.put(voiceName, voiceNumber + "");

        } catch (Exception e) {
          log.debug(e.toString());
        }

      }

    } else {
      getVoiceList().clear();
      getVoiceList().add("Default");
      voiceMap.clear();
      voiceMap.put("Default", "0");
    }
    setVoiceList(list);
    return list;
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

    // ServiceType meta = new ServiceType(LocalSpeech.class.getCanonicalName());
    ServiceType meta = AbstractSpeechSynthesis.getMetaData(LocalSpeech.class.getCanonicalName());

    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.addCategory("speech", "sound");

    meta.addDescription("Local OS text to speech ( tts.exe / say etc ... )");
    meta.setAvailable(true); // false if you do not want it viewable in a
    // gui
    // add dependency if necessary
    meta.addCategory("speech");
    meta.addDependency("com.microsoft", "tts", "1.1", "zip");
    return meta;
  }

  @Override
  public void startService() {
    super.startService();

    File f = new File(System.getProperty("user.dir") + File.separator + windowsTtsExecutable);
    ttsExecutableExist = true;

    if (!new File(System.getProperty("user.dir") + File.separator + ttsFolder).exists()) {
      File dir = new File(ttsFolder);
      dir.mkdir();
    }
    if (!f.exists() && Platform.getLocalInstance().isWindows()) {
      error("Missing : " + System.getProperty("user.dir") + File.separator + windowsTtsExecutable);
      ttsExecutableExist = false;
    }
    if (Platform.getLocalInstance().isLinux()) {
      error("generic Linux local tts not yet implemented, want help ?");
    }
    if (Platform.getLocalInstance().isMac()) {
      this.setAudioCacheExtension("AIFF");
    }
    subSpeechStartService();
  }

  public static void main(String[] args) throws Exception {

    LoggingFactory.init(Level.INFO);
    Runtime.start("gui", "SwingGui");
    LocalSpeech localSpeech = (LocalSpeech) Runtime.start("localSpeech", "LocalSpeech");
    // microsoftLocalTTS.ttsExeOutputFilePath="c:\\tmp\\";
    localSpeech.getVoices();
    // localSpeech.setVoice("1");
    localSpeech.speakBlocking("I am your R 2 D 2 #R2D2#");
    localSpeech.speak("unicode éléphant");
    localSpeech.getVoiceEffects();
  }


  @Override
  public byte[] generateByteAudio(String toSpeak) throws IOException {
    toSpeak = toSpeak.replace("\"", "\"\"");
    String uuid = UUID.randomUUID().toString();
    String command = System.getProperty("user.dir") + File.separator + windowsTtsExecutable + " -f 9 -v " + voiceMap.get(getVoice()) + " -t -o " + ttsExeOutputFilePath + uuid
        + " \"" + toSpeak + " \"";
    String cmd = "null";
    File f = new File(ttsExeOutputFilePath + uuid + "0." + getAudioCacheExtension());
    // windows os local tts
    if (Platform.getLocalInstance().isWindows()) {
      f = new File(ttsExeOutputFilePath + uuid + "0." + getAudioCacheExtension());
      f.delete();
      cmd = Runtime.execute("cmd.exe", "/c", command);
    }
    // macos local tts ( it is WAVE not mp3, but worky... )
    // TODO : Convert to mp3
    if (Platform.getLocalInstance().isMac()) {
      f = new File(ttsExeOutputFilePath + uuid + "0.AIFF");
      f.delete();
      cmd = Runtime.execute(macOsTtsExecutable, toSpeak, "-o", ttsExeOutputFilePath + uuid + "0.AIFF");
    }
    log.info(cmd);
    if (!f.exists()) {
      log.error("local tts caused an error : " + cmd);
    } else {
      if (f.length() == 0) {
        log.error("local tts caused an error : empty file " + cmd);
      } else {
        byte[] mp3File = FileIO.toByteArray(f);
        f.delete();
        return mp3File;
      }

    }
    return null;
  }

  @Override
  public void setKeys(String keyId, String keyIdSecret) {
    // TODO Auto-generated method stub

  }

  @Override
  public String[] getKeys() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getVoiceInJsonConfig() {
    return voiceInJsonConfig.get(this.getClass().getSimpleName());
  }

  @Override
  public void setVoiceInJsonConfig(String voice) {
    voiceInJsonConfig.put(this.getClass().getSimpleName(), voice);

  }

}
