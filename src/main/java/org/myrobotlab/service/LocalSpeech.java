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
import org.myrobotlab.service.interfaces.AudioListener;
import org.slf4j.Logger;

public class LocalSpeech extends AbstractSpeechSynthesis implements AudioListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(LocalSpeech.class);

  transient Integer voice = 0;
  transient String voiceName = "0";


  private String ttsFolder = "tts";


  private String windowsTtsExecutable = ttsFolder + File.separator + "tts.exe";
  private String macOsTtsExecutable = "say";
  // TODO private String linuxTtsExecutable = "";

  public String ttsExeOutputFilePath = System.getProperty("user.dir") + File.separator + ttsFolder + File.separator;
  boolean ttsExecutableExist;

  transient Map<Integer, String> voiceMap = new HashMap<Integer, String>();

  public LocalSpeech(String n) {
    super(n);
  }

  @Override
  public List<String> getVoices() {
    if (Platform.getLocalInstance().isWindows()) {
      ArrayList<String> args = new ArrayList<String>();
      args.add("-V");
      String cmd = Runtime.execute(System.getProperty("user.dir") + File.separator + windowsTtsExecutable, "-V");
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
    return null;
  }

  @Override
  public boolean setVoice(String voice) {
    // macos get voices not necessaries
    if (Platform.getLocalInstance().isWindows()) {
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
    info("could not set voice to {}", voice);
    return false;
  }

  @Override
  public String getVoice() {
    return voice.toString();
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

    ServiceType meta = new ServiceType(LocalSpeech.class.getCanonicalName());
    meta.addDescription("Local OS text to speech ( tts.exe / say etc ... )");
    meta.setAvailable(true); // false if you do not want it viewable in a
    // gui
    // add dependency if necessary
    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.addCategory("speech");
    meta.addDependency("com.microsoft", "tts", "1.1", "zip");
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
      this.audioCacheExtension = "AIFF";
    }

  }

  public static void main(String[] args) throws Exception {

    LoggingFactory.init(Level.INFO);

    LocalSpeech localSpeech = (LocalSpeech) Runtime.start("localSpeech", "LocalSpeech");
    // microsoftLocalTTS.ttsExeOutputFilePath="c:\\tmp\\";
    localSpeech.getVoices();
    localSpeech.setVoice("1");
    localSpeech.speakBlocking("local tts");
    localSpeech.speak("unicode éléphant");

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

  @Override
  public byte[] generateByteAudio(String toSpeak) throws IOException {

    String uuid = UUID.randomUUID().toString();
    String command = System.getProperty("user.dir") + File.separator + windowsTtsExecutable + " -f 9 -v " + voice + " -t -o " + ttsExeOutputFilePath + uuid + " \"" + toSpeak
        + " \"";
    String cmd = "null";
    File f = new File(ttsExeOutputFilePath + uuid + "0." + audioCacheExtension);
    // windows os local tts
    if (Platform.getLocalInstance().isWindows()) {
      f = new File(ttsExeOutputFilePath + uuid + "0." + audioCacheExtension);
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

}
