package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

public class MimicSpeech extends AbstractSpeechSynthesis implements AudioListener {
  public final static Logger log = LoggerFactory.getLogger(MimicSpeech.class);
  private static final long serialVersionUID = 1L;

  // end
  // TODO: make this cross platform..
  private String mimicFolder = "mimic";
  // stored inside json
  HashMap<String, String> voiceInJsonConfig;
  // end
  private String mimicExecutable = mimicFolder + File.separator + "mimic.exe";
  public String mimicOutputFilePath = System.getProperty("user.dir") + File.separator + mimicFolder + File.separator;

  public MimicSpeech(String reservedKey) {
    super(reservedKey);
  }

  public List<String> getVoices() {
    List<String> list = new ArrayList<String>();
    if (Platform.getLocalInstance().isWindows()) {

      String cmd = Runtime.execute(System.getProperty("user.dir") + File.separator + mimicExecutable, "-lv");

      try {

        String voiceLine = cmd.substring(cmd.indexOf("Voices available: ") + 18, cmd.length()).replace("Exit Value : 0", "").replaceAll("\\s{2,}", " ");

        log.info("voiceLine:" + voiceLine);
        for (String p : voiceLine.split(" ")) {
          if (p != " ") {
            list.add(p);
            log.info("voice added :" + p);
          }
        }
        voiceList = list;
      } catch (Exception e) {
        log.debug(e.toString());
      }

    } else {
      voiceList.clear();
      voiceList.add("Default");
      error("Platform not yet supported");

      list = null;
    }
    return list;
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
  public void startService() {
    super.startService();
    audioCacheExtension = "wav";
    subSpeechStartService();

  }

  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(MimicSpeech.class.getCanonicalName());
    meta.addDescription("Speech synthesis based on Mimic from the MyCroft AI project.");
    meta.addCategory("speech", "sound");
    meta.addDependency("mycroftai.mimic", "mimic_win64", "1.0", "zip");
    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.setSponsor("Kwatters");
    // meta.addDependency("marytts", "5.2");
    // meta.addDependency("com.sun.speech.freetts", "1.2");
    // meta.addDependency("opennlp", "1.6");
    // TODO: build it for all platforms and add it to the repo as a zip file
    // so each os can download a pre-built version of mimic ...
    return meta;
  }

  public static void main(String[] args) throws Exception {
    Runtime.start("gui", "SwingGui");
    MimicSpeech mimic = (MimicSpeech) Runtime.createAndStart("mimic", "MimicSpeech");
    LoggingFactory.init(Level.INFO);

    mimic.speakBlocking("hello \"world\", it's a  test .. testing 1 2 3 , unicode éléphant");
  }

  @Override
  public byte[] generateByteAudio(String toSpeak) throws IOException {
    toSpeak = toSpeak.replace("\"", "\"\"");
    String fileName = mimicOutputFilePath + UUID.randomUUID().toString() + "." + audioCacheExtension;
    String command = System.getProperty("user.dir") + File.separator + mimicExecutable + " -voice " + getVoice() + " -o \"" + fileName + "\" -t \"" + toSpeak + "\"";
    String cmd = "null";
    File f = new File(fileName);

    if (Platform.getLocalInstance().isWindows()) {

      f.delete();
      cmd = Runtime.execute("cmd.exe", "/c", command);

      log.info(cmd);
      if (!f.exists()) {
        log.error("mimic caused an error : " + cmd);
      } else {
        if (f.length() == 0) {
          log.error("mimic caused an error : empty file " + cmd);
        } else {
          byte[] mp3File = FileIO.toByteArray(f);
          f.delete();
          return mp3File;
        }

      }
    }
    setEngineError("OS not supported");
    setEngineStatus(false);
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
