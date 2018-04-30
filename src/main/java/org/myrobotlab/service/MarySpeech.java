package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.commons.io.FileUtils;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.maryspeech.tools.install.MaryInstaller;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.interfaces.AudioListener;
import org.myrobotlab.service.interfaces.TextListener;
import org.slf4j.Logger;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.SynthesisException;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.data.audio.MaryAudioUtils;

/**
 * The Mary Speech service is a wrapper for the MaryTTS project. This service
 * implements the speech synthesis interface and provides a Text To Speech
 * capability.
 * 
 * More info at : http://mary.dfki.de/
 * 
 */
public class MarySpeech extends AbstractSpeechSynthesis implements TextListener, AudioListener {

  public final static Logger log = LoggerFactory.getLogger(MarySpeech.class);
  private static final long serialVersionUID = 1L;

  private transient MaryInterface marytts = null;

  private String maryBase = "mary";

  // stored inside json
  String audioEffects;
  HashMap<String, String> voiceInJsonConfig;
  // end
  public String maryComponentsUrl = "https://raw.github.com/marytts/marytts/master/download/marytts-components.xml";

  public MarySpeech(String reservedKey) {
    super(reservedKey);

  }

  @Override
  public List<String> getVoices() {
    List<String> list = null;
    try {
      list = new ArrayList<>(marytts.getAvailableVoices());
      setEngineError("Ready");
      setEngineStatus(true);

    } catch (Exception e) {
      return null;
    }
    log.info("{} has {} voices", getName(), list.size());
    for (int i = 0; i < list.size(); ++i) {
      log.info(list.get(i));
    }
    voiceList = list;
    return voiceList;
  }

  @Override
  public boolean setVoice(String voice) {
    if (subSetVoice(voice) && voice != null) {
      try {
        marytts.setVoice(voice);
        return true;
      } catch (IllegalArgumentException e) {
        error("marytts.setVoice error : " + voice);

      }
    }
    return false;
  }

  public void setAudioEffects(String audioEffects) {
    marytts.setAudioEffects(audioEffects);
    this.audioEffects = audioEffects;
    audioCacheParameters = audioEffects;
  }

  public String getAudioEffects() {
    return this.audioEffects;
  }

  public void installComponentsAcceptLicense(String component) {
    installComponentsAcceptLicense(new String[] { component });
  }

  public void installComponentsAcceptLicense(String[] components) {
    if (components == null) {
      return;
    }
    log.info("Installing components from {}", maryComponentsUrl);
    org.myrobotlab.maryspeech.tools.install.MaryInstaller installer = new MaryInstaller(maryComponentsUrl);
    Map<String, org.myrobotlab.maryspeech.tools.install.LanguageComponentDescription> languages = installer.getLanguages();
    Map<String, org.myrobotlab.maryspeech.tools.install.VoiceComponentDescription> voices = installer.getVoices();

    List<org.myrobotlab.maryspeech.tools.install.ComponentDescription> toInstall = new ArrayList<>();
    for (String component : components) {
      if (component == null || component.isEmpty() || component.trim().isEmpty()) {
        continue;
      }
      if (languages.containsKey(component)) {
        toInstall.add(languages.get(component));
      } else if (voices.containsKey(component)) {
        toInstall.add(voices.get(component));
      } else {
        log.warn("can't find component for installation");
      }
    }

    log.info("starting marytts component installation:" + toInstall);
    installer.installSelectedLanguagesAndVoices(toInstall);
    log.info("moving files to correct places ...");
    File srcDir = new File(maryBase + File.separator + "lib");
    File destDir = new File("libraries" + File.separator + "jar");
    try {
      FileUtils.copyDirectory(srcDir, destDir);
      log.info("finished marytts component installation");
      log.info("PLEASE RESTART TO APPLY CHANGES !!!");
    } catch (IOException e) {
      log.error("moving files FAILED!");
    }
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      // Runtime.start("webgui", "WebGui");
      MarySpeech mary = (MarySpeech) Runtime.start("mary", "MarySpeech");
      Runtime.start("python", "Python");
      Runtime.start("gui", "SwingGui");
      // examples are generously copied from
      // marytts.signalproc.effects.EffectsApplier.java L319-324
      // String strEffectsAndParams = "FIRFilter+Robot(amount=50)";
      String strEffectsAndParams = "Robot(amount=100)+Chorus(delay1=866, amp1=0.24, delay2=300, amp2=-0.40,)";
      // "Robot(amount=80)+Stadium(amount=50)";
      // String strEffectsAndParams = "FIRFilter(type=3,fc1=6000,
      // fc2=10000) + Robot";
      // String strEffectsAndParams = "Stadium(amount=40) +
      // Robot(amount=87) +
      // Whisper(amount=65)+FIRFilter(type=1,fc1=1540;)++";
      // mary.setAudioEffects(strEffectsAndParams);

      // mary.setVoice("dfki-spike en_GB male unitselection general");
      // mary.setVoice("cmu-bdl-hsmm");
      // mary.setVoice("cmu-slt-hsmm");
      mary.getVoices();
      // mary.speak("world");
      mary.speak("");
      mary.setAudioEffects("");
      mary.setVolume(0.9f);
      mary.speakBlocking("Hello world");
      mary.setVolume(0.7f);
      mary.speakBlocking("Hello world");
      mary.setVolume(0.9f);
      // mary.speakBlocking("unicode test, éléphant");
      // test audioeffect on cached text
      mary.setAudioEffects("FIRFilter+Robot(amount=50)");
      mary.speak("Hello world");

      // mary.speakBlocking("my name is worky");
      // mary.speakBlocking("I am Mary TTS and I am open source");
      // mary.speakBlocking("and I will evolve quicker than any closed source
      // application if not in a short window of time");
      // mary.speakBlocking("then in the long term evolution of software");
      // mary.speak("Hello world");

      // WOW - that is a big install !
      // mary.installComponentsAcceptLicense("bits1");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @Override
  public void startService() {

    super.startService();
    // TODO: this should all be in startService, no?
    File file = new File(maryBase);
    if (!file.exists()) {
      file.mkdirs();
    }
    file = new File(maryBase + File.separator + "download");
    if (!file.exists()) {
      file.mkdirs();
    }
    file = new File(maryBase + File.separator + "installed");
    if (!file.exists()) {
      file.mkdirs();
    }
    file = new File(maryBase + File.separator + "lib");
    if (!file.exists()) {
      file.mkdirs();
    }
    file = new File(maryBase + File.separator + "log");
    if (!file.exists()) {
      file.mkdirs();
    }

    // Set some envirionment variables so we can load Mary libraries.
    System.setProperty("mary.base", maryBase);
    System.setProperty("mary.downloadDir", new File(maryBase + "/download").getPath());
    System.setProperty("mary.installedDir", new File(maryBase + "/installed").getPath());

    setEngineError("Starting...");
    setEngineStatus(false);

    try {
      marytts = new LocalMaryInterface();
    } catch (Exception e) {
      Logging.logError(e);
      setEngineError("LocalMaryInterface KO");
      setEngineStatus(false);
    }

    audioCacheExtension = "wav";

    subSpeechStartService();
    if (audioEffects == null) {
      audioEffects = "";
      audioCacheParameters = audioEffects;
    }
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
    ServiceType meta = new ServiceType(MarySpeech.class);
    meta.addDescription("Speech synthesis based on MaryTTS");
    meta.addCategory("speech", "sound");
    meta.addDependency("de.dfki.mary", "marytts", "5.2", "pom");
    meta.addPeer("audioFile", "AudioFile", "audioFile");

    // hmm..TODO: refactor this.
    String[] voices = new String[] { "voice-bits1-hsmm", "voice-bits3-hsmm", "voice-cmu-bdl-hsmm", "voice-cmu-nk-hsmm", "voice-cmu-rms-hsmm", "voice-cmu-slt-hsmm",
        "voice-dfki-obadiah-hsmm", "voice-dfki-ot-hsmm", "voice-dfki-pavoque-neutral-hsmm", "voice-dfki-poppy-hsmm", "voice-dfki-prudence-hsmm", "voice-dfki-spike-hsmm",
        "voice-enst-camille-hsmm", "voice-enst-dennys-hsmm", "voice-istc-lucia-hsmm", "voice-upmc-jessica-hsmm", "voice-upmc-pierre-hsmm" };
    for (String voice : voices) {
      meta.addDependency("de.dfki.mary", voice, "5.2");
    }

    meta.exclude("org.slf4j", "slf4j-api");
    meta.exclude("commons-io", "commons-io");
    meta.exclude("log4j", "log4j");
    meta.exclude("commons-lang", "commons-lang");
    meta.exclude("com.google.guava", "guava");
    meta.exclude("org.apache.opennlp", "opennlp-tools");
    meta.exclude("org.slf4j", "slf4j-log4j12");
    meta.exclude("org.apache.httpcomponents", "httpcore");

    // meta.addDependency("opennlp", "1.6");
    return meta;
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
    // generate mary to wav
    AudioInputStream maryOutput = null;
    try {
      maryOutput = marytts.generateAudio(toSpeak);
    } catch (SynthesisException e) {
      setEngineError("MarySpeech cannot generate audiofile");
      setEngineStatus(false);
      error(getEngineError() + " : %s,e");
    }

    DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(MaryAudioUtils.getSamplesAsDoubleArray(maryOutput)), maryOutput.getFormat());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, outputStream);
    return outputStream.toByteArray();

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
