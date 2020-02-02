package org.myrobotlab.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.data.Locale;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
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
public class MarySpeech extends AbstractSpeechSynthesis {

  public final static Logger log = LoggerFactory.getLogger(MarySpeech.class);
  
  private static final long serialVersionUID = 1L;

  private transient MaryInterface marytts = null;

  String maryBase = "mary";
 
  public MarySpeech(String n, String id) throws MaryConfigurationException {
    super(n, id);
    getMaryTts();
  }
  
  synchronized MaryInterface getMaryTts() {
    if (marytts != null) {
      return marytts;
    }
    
    String maryBase = "mary";
      
    // Set some envirionment variables so we can load Mary libraries.
    System.setProperty("mary.base", maryBase);
    System.setProperty("mary.downloadDir", new File(maryBase + "/download").getPath());
    System.setProperty("mary.installedDir", new File(maryBase + "/installed").getPath());

    /*
     * FIXME - standardize with SSML input <pre>
     *
     * setEffectsList("TractScaler", "amount:1.5"); setEffectsList("F0Scale",
     * "f0Scale:2.0"); setEffectsList("F0Add", "f0Add:50.0"); //
     * setEffectsList("Rate", "durScale:1.5"); setEffectsList("Robot",
     * "amount:100.0"); setEffectsList("Whisper", "amount:100.0");
     * setEffectsList("Stadium", "amount:100.0"); setEffectsList("Chorus",
     * "delay1:466;amp1:0.54;delay2:600;amp2:-"); setEffectsList("FIRFilter",
     * "type:3;fc1:500.0;fc2:2000.0"); setEffectsList("JetPilot", ""); </pre>
     */

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

    try {
      marytts = new LocalMaryInterface();
    } catch (Exception e) {
      error(e);
    }
    return marytts;
  }

  /*
   * public void installComponentsAcceptLicense(String component) {
   * installComponentsAcceptLicense(new String[] { component }); }
   */

  /*
   * INSTALL NOW USES MAVEN public void installComponentsAcceptLicense(String[]
   * components) { if (components == null) { return; } //
   * log.info("Installing components from {}", maryComponentsUrl);
   * InstallFileParser installer = new InstallFileParser(new URL(
   * "https://raw.github.com/marytts/marytts/master/download/marytts-components.xml"
   * ));
   * 
   * Map<String, LanguageComponentDescription> maryLanguages =
   * installer.getLanguages(); List<VoiceComponentDescription>voices =
   * installer.getVoiceDescriptions();
   * 
   * List<ComponentDescription> toInstall = new ArrayList<>(); for (String
   * component : components) { if (component == null || component.isEmpty() ||
   * component.trim().isEmpty()) { continue; } if
   * (maryLanguages.containsKey(component)) {
   * toInstall.add(maryLanguages.get(component)); } else if
   * (voices.containsKey(component)) { toInstall.add(voices.get(component)); }
   * else { log.warn("can't find component for installation"); } }
   * 
   * 
   * log.info("starting marytts component installation: " + toInstall);
   * installer.installSelectedLanguagesAndVoices(toInstall);
   * log.info("moving files to correct places ..."); File srcDir = new
   * File(maryBase + File.separator + "lib"); File destDir = new
   * File("libraries" + File.separator + "jar"); try {
   * FileUtils.copyDirectory(srcDir, destDir);
   * log.info("finished marytts component installation");
   * log.info("PLEASE RESTART TO APPLY CHANGES !!!"); } catch (IOException e) {
   * log.error("moving files FAILED!"); } }
   */

  /**
   * default cache file type for Mary
   */
  public String getAudioCacheExtension() {
    return ".wav";
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

    ServiceType meta = AbstractSpeechSynthesis.getMetaData(MarySpeech.class.getCanonicalName());

    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.addCategory("speech", "sound");
    meta.addDescription("Speech synthesis based on MaryTTS");

    meta.addDependency("de.dfki.mary", "marytts", "5.2", "pom");
    // FIXME - use the following config file to generate the needed data for
    // loadVoice()
    // main config for voices
    // https://github.com/marytts/marytts-installer/blob/master/components.json

  
    String[] voices = new String[] { "voice-bits1-hsmm", "voice-bits3-hsmm", "voice-cmu-bdl-hsmm", "voice-cmu-nk-hsmm", "voice-cmu-rms-hsmm", "voice-cmu-slt-hsmm",
        "voice-dfki-obadiah-hsmm", "voice-dfki-ot-hsmm", "voice-dfki-pavoque-neutral-hsmm", "voice-dfki-poppy-hsmm", "voice-dfki-prudence-hsmm", "voice-dfki-spike-hsmm",
        "voice-enst-camille-hsmm", "voice-enst-dennys-hsmm", "voice-istc-lucia-hsmm", "voice-upmc-jessica-hsmm", "voice-upmc-pierre-hsmm" };

    for (String voice : voices) {
      meta.addDependency("de.dfki.mary", voice, "5.2");
      meta.exclude("org.apache.httpcomponents", "httpcore");
      meta.exclude("org.apache.httpcomponents", "httpclient");

      if ("voice-bits1-hsmm".equals(voice) || "voice-cmu-slt-hsmm".equals(voice)) {
        meta.exclude("org.slf4j", "slf4j-log4j12");
      }
    }
    meta.exclude("org.slf4j", "slf4j-api");
    meta.exclude("commons-io", "commons-io");
    meta.exclude("log4j", "log4j");
    meta.exclude("commons-lang", "commons-lang");
    meta.exclude("com.google.guava", "guava");
    meta.exclude("org.apache.opennlp", "opennlp-tools");
    meta.exclude("org.slf4j", "slf4j-log4j12");

    return meta;
  }

  @Override
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws IOException, SynthesisException {
    getMaryTts();
    Voice voice = getVoice();
    marytts.setVoice(voice.getVoiceProvider().toString());
    // marytts.setInputType("SSML"); FIXME - MUST BE VALID XML WITH HEADER !!!
    // marytts.setOutputType("TARGETFEATURES");
    // marytts.setLocale(Locale.SWEDISH);

    AudioInputStream maryOutput = marytts.generateAudio(toSpeak);

    DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(MaryAudioUtils.getSamplesAsDoubleArray(maryOutput)), maryOutput.getFormat());
    FileOutputStream fos = new FileOutputStream(audioData.getFileName());
    AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, fos);
    fos.close();
    return audioData;
  }

  @Override
  protected void loadVoices() throws MalformedURLException, IOException, SAXException {
    getMaryTts();
    // It is great that we can query to get voices - but regrettably they are
    // lacking a lot of useful meta-data
    // such as locale and gender. So, we will "augment" the meta information..
    Set<String> list = marytts.getAvailableVoices();
    list.size();

    for (String k : list) {
      log.info("voice-{}" , k);
    }

    // compare against installer and config files to find more voices
    /*
     * <pre> InstallFileParser installer = new InstallFileParser(new URL(
     * "https://raw.github.com/marytts/marytts/master/download/marytts-components.xml"
     * )); List<VoiceComponentDescription> moreVoices =
     * installer.getVoiceDescriptions(); for (VoiceComponentDescription k :
     * moreVoices) { log.info("voice-" + k.getName()); }
     * moreVoices.size(); </pre>
     */

    // installComponentsAcceptLicense("bits2");

    addVoice("Obadiah", "male", "en-GB", "dfki-obadiah-hsmm");
    addVoice("Lucia", "female", "it", "istc-lucia-hsmm");
    addVoice("Emma", "female", "de", "bits1-hsmm");
    addVoice("Henry", "male", "en", "cmu-rms-hsmm");
    addVoice("Alim", "male", "tr", "dfki-ot-hsmm");
    addVoice("Jessica", "female", "fr", "upmc-jessica-hsmm");
    addVoice("Spike", "male", "en-GB", "dfki-spike-hsmm");
    addVoice("Sally", "female", "en-US", "cmu-slt-hsmm");
    addVoice("Camille", "female", "fr", "enst-camille-hsmm");
    addVoice("Hans", "male", "de", "dfki-pavoque-neutral-hsmm");
    addVoice("Poppy", "female", "en-GB", "dfki-poppy-hsmm");
    addVoice("Mark", "male", "en-US", "cmu-bdl-hsmm");
    addVoice("Pierre", "male", "fr", "upmc-pierre-hsmm");
    addVoice("Mahi", "female", "te", "cmu-nk-hsmm");
    addVoice("Dennys", "male", "fr-CA", "enst-dennys-hsmm");
    addVoice("Conrad", "male", "de", "bits3-hsmm");
    addVoice("Prudence", "female", "en-GB", "dfki-prudence-hsmm");
    // addVoice("Prudence", "female", "en-GB", "dfki-prudence-hsmm");
  }
  

  public String setAudioEffects(String audioEffects) {
    marytts.setAudioEffects(audioEffects);
    return audioEffects;
  }

  public static void main(String[] args) throws IOException {
    LoggingFactory.init(Level.INFO);

    try {

      Runtime.start("gui", "SwingGui");
      Runtime.start("webgui", "WebGui");
      MarySpeech mary = (MarySpeech) Runtime.start("mary", "MarySpeech");
     
      // mary.grabRemoteAudioEffect("LAUGH01_F");
      Runtime.start("python", "Python");

      // examples are generously copied from
      // marytts.signalproc.effects.EffectsApplier.java L319-324
      // String strEffectsAndParams = "FIRFilter+Robot(amount=50)";
      //// String strEffectsAndParams = "Robot(amount=100)+Chorus(delay1=866,
      // amp1=0.24, delay2=300, amp2=-0.40,)";
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
      //// mary.getVoices();
      // mary.speak("world");
      //// mary.speak("");
      //// mary.setAudioEffects("");
      //// mary.setVolume(0.9f);
      //// mary.speakBlocking("Hello world");
      //// mary.setVolume(0.7f);
      //// mary.speakBlocking("Hello world");
      //// mary.setVolume(0.9f);
      // mary.speakBlocking("unicode test, éléphant");
      // test audioeffect on cached text
      //// mary.setAudioEffects("FIRFilter+Robot(amount=50)");
      //// mary.speak("Hello world");

      // mary.speakBlocking("my name is worky");
      // mary.speakBlocking("I am Mary TTS and I am open source");
      // mary.speakBlocking("and I will evolve quicker than any closed source
      // application if not in a short window of time");
      // mary.speakBlocking("then in the long term evolution of software");

      mary.speak("to be or not to be that is the question");
      // mary.speak("#THROAT01_F# Hello world, it is so funny #LAUGH02_F#");
      // mary.setVoice("cmu-bdl-hsmm");
      // mary.speak("#THROAT01_M# hi! it works.");
      // mary.speak("#LAUGH01_M#");
      // mary.setVolume(0.8);
      // mary.speak("I am your R 2 D 2 #R2D2# ,how was that");
      // mary.setVolume(1.0);
      // WOW - that is a big install !
      // mary.installComponentsAcceptLicense("bits1");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }



 

}
