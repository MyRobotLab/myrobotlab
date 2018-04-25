package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.maryspeech.tools.install.MaryInstaller;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.interfaces.AudioListener;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
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
  // TODO: this is completely ignored. TODO: use this to localize the
  // confirmationString.
  private String language;
  private String maryBase = "mary";
  private String audioCacheExtension = "wav";
  public String lastUtterance = "";
  String audioEffects;
  String voice;
  transient public Set<String> voices = null;
  public String maryComponentsUrl = "https://raw.github.com/marytts/marytts/master/download/marytts-components.xml";
  // This is the format string that will be used when asking for confirmation.
  public String confirmationString = "did you say %s ?";
  transient AudioFile audioFile = null;
  transient HashMap<AudioData, String> utterances = new HashMap<AudioData, String>();

  public MarySpeech(String reservedKey) {
    super(reservedKey);

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

    try {
      marytts = new LocalMaryInterface();
    } catch (Exception e) {
      Logging.logError(e);
    }

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
  public boolean speakBlocking(String toSpeak) throws IOException {
    toSpeak = toSpeak.replaceAll("\\s{2,}", " ");
    cacheFile(toSpeak);
    invoke("publishStartSpeaking", toSpeak);
    audioFile.playBlocking(AudioFile.globalFileCacheDir + File.separator + getLocalFileName(this, toSpeak, audioCacheExtension));
    invoke("publishEndSpeaking", toSpeak);
    return false;
  }

  @Override
  public AudioData speak(String toSpeak) throws IOException, SynthesisException, InterruptedException {
    toSpeak = toSpeak.replaceAll("\\s{2,}", " ");
    cacheFile(toSpeak);
    AudioData audioData = audioFile.playCachedFile(getLocalFileName(this, toSpeak, audioCacheExtension));
    utterances.put(audioData, toSpeak);
    return audioData;
  }

  public byte[] cacheFile(String toSpeak) throws IOException {
    byte[] byteArrayOutputStream = null;
    String localFileName = getLocalFileName(this, toSpeak, audioCacheExtension);

    if (!audioFile.cacheContains(localFileName)) {
      log.info("retrieving speech from locals - {}", localFileName, voice);
      // generate mary to wav
      AudioInputStream maryOutput = null;
      try {
        maryOutput = marytts.generateAudio(toSpeak);
      } catch (SynthesisException e) {
        error("MarySpeech cannot generate audiofile : %s,e");
      }

      DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(MaryAudioUtils.getSamplesAsDoubleArray(maryOutput)), maryOutput.getFormat());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, outputStream);
      byteArrayOutputStream = outputStream.toByteArray();
      audioFile.cache(localFileName, byteArrayOutputStream, toSpeak);
    } else {
      log.info("using local cached file");
      byteArrayOutputStream = FileIO.toByteArray(new File(AudioFile.globalFileCacheDir + File.separator + getLocalFileName(this, toSpeak, audioCacheExtension)));
    }

    return byteArrayOutputStream;
  }

  @Override
  public List<String> getVoices() {
    List<String> list = new ArrayList<>(marytts.getAvailableVoices());
    log.info("{} has {} voices", getName(), list.size());
    for (int i = 0; i < list.size(); ++i) {
      log.info(list.get(i));
    }
    return list;
  }

  @Override
  public boolean setVoice(String voice) {
    if (voice == null || voice.isEmpty()) {
      voice = "cmu-slt-hsmm";
    }
    try {
      marytts.setVoice(voice);

    } catch (IllegalArgumentException e) {
      error("Unknown MarySpeech Voice : " + voice);
      return false;
    }
    this.voice = voice;
    broadcastState();
    info(this.getIntanceName() + " set voice to : " + voice);
    return true;
  }

  @Override
  public void setLanguage(String lang) {
    // TODO: why not allow "en" ?!? remove this if check perhaps?
    if (!lang.equalsIgnoreCase("en")) {
      marytts.setLocale(Locale.forLanguageTag(lang));
    }
    this.language = lang;
  }

  @Override
  public void onRequestConfirmation(String text) {
    try {
      // FIXME - not exactly language independent
      speakBlocking(String.format(confirmationString, text));
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @Override
  public String getLanguage() {
    return marytts.getLocale().getLanguage();
  }

  @Override
  public void setVolume(float volume) {
    // TODO implement me!
    log.warn("Set volume not implemented in MarySpeech (yet)");
  }

  @Override
  public float getVolume() {
    // TODO implement me!
    log.warn("Get volume not implemented in MarySpeech (yet)");
    return 0;
  }

  @Override
  public void interrupt() {
    // TODO: interrupt the playback of mary speech
    log.warn("Ignoring your interrupt request... (not implemented)");
  }

  @Override
  public String publishStartSpeaking(String utterance) {
    // framework method to publish the start speaking event.
    log.info("Starting to speak: {}", utterance);
    lastUtterance = utterance;
    broadcastState();
    return utterance;
  }

  @Override
  public String getVoice() {
    return marytts.getVoice();
  }

  // TODO: move this to a common base utility class for all speech synthesis.
  @Override
  public String getLocalFileName(SpeechSynthesis provider, String toSpeak, String audioFileType) throws UnsupportedEncodingException {
    return provider.getClass().getSimpleName() + File.separator + URLEncoder.encode(provider.getVoice(), "UTF-8") + File.separator + URLEncoder.encode(audioEffects, "UTF-8") + File.separator
        + DigestUtils.md5Hex(toSpeak) + "." + audioFileType;
  }

  @Override
  public List<String> getLanguages() {
    List<String> ret = new ArrayList<>();
    for (Locale locale : marytts.getAvailableLocales()) {
      ret.add(locale.getLanguage());
    }
    return ret;
  }

  public void setAudioEffects(String audioEffects) {
    marytts.setAudioEffects(audioEffects);
    this.audioEffects = audioEffects;
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
      mary.setAudioEffects("");
      mary.speakBlocking("Hello world");
      mary.speakBlocking("unicode test, éléphant");
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
    audioFile = (AudioFile) startPeer("audioFile");
    audioFile.startService();
    subscribe(audioFile.getName(), "publishAudioStart");
    subscribe(audioFile.getName(), "publishAudioEnd");
    // attach a listener when the audio file ends playing.
    audioFile.addListener("finishedPlaying", this.getName(), "publishEndSpeaking");

    voices = marytts.getAvailableVoices();
    setVoice(this.voice);
    if (audioEffects==null)
    {
      audioEffects="";
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

}
