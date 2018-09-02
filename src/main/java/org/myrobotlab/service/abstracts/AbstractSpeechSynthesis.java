package org.myrobotlab.service.abstracts;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.AudioFile;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Security;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.interfaces.AudioListener;
import org.myrobotlab.service.interfaces.KeyConsumer;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

public abstract class AbstractSpeechSynthesis extends Service implements SpeechSynthesis, TextListener, KeyConsumer, AudioListener {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(AbstractSpeechSynthesis.class);

  static String globalFileCacheDir = "audioFile";
  public static final String journalFilename = "journal.txt";
  
  /**
   * substitutions are phonetic substitutions for a specific instance of speech synthesis service
   */
  Map<String,String> substitutions = new HashMap<String,String>();

  public static class Voice {
    /**
     * unique name of the voice
     */
    String name;
    /**
     * gender of the voice either male or female
     */
    String gender;

    /**
     * description
     */
    String description;

    // TODO - age ? child youth adult senior

    /**
     * Locale of the voice, if it has one
     */
    Locale locale;

    /**
     * Installed means the voice is ready without any additional components
     */
    boolean installed = true;

    /**
     * Serializable key of voice implementation - to be used to map this MRL
     * Voice to a voice implementation
     */
    Object voiceProvider;

    public Voice(String name, String gender, String lang, Object voiceProvider) {
      this.name = name;
      this.voiceProvider = voiceProvider;

      if (gender != null) {
        String g = gender.toLowerCase();
        if (!"male".equals(g) && !"female".equals(g)) {
          log.warn("only know about male or female - but will set gender to {} because we don't want to discriminate", g);
        }
        this.gender = g;
      }
      if (lang != null) {
        String[] l = lang.split("-");
        if (l.length > 1) {
          this.locale = new Locale(l[0], l[1]);
        } else {
          this.locale = new Locale(l[0]);
        }
      }
    }

    public String toString() {
      // return String.format("%s %s %s %s", name, gender, locale, voiceProvider);
      return String.format("%s %s %s", name, gender, locale);
    }

    public void setInstalled(boolean b) {
      installed = b;
    }

    public boolean isInstalled() {
      return installed;
    }

    public String getName() {
      return name;
    }

    public String getGender() {
      return gender;
    }

    public String getLanguage() {
      if (locale == null) {
        return null;
      }
      return locale.getDisplayLanguage();
    }
    
    public Locale getLocal() {
      return locale;
    }

    public String getLanguageCode() {
      if (locale == null) {
        return null;
      }
      return locale.getLanguage();
    }

    public Object getVoiceProvider() {
      return voiceProvider;
    }
  }

  /**
   * current voice
   */
  protected Voice voice;

  /**
   * default voice of the speech service - must be set ..
   */
  protected Voice defaultVoice;

  /**
   * last utterance of the speech service
   */
  protected String lastUtterance;

  transient HashMap<AudioData, String> utterances = new HashMap<AudioData, String>();

  /**
   * AudioFile peer for caching and playing effects
   */
  protected transient AudioFile audioFile = null;

  // FIXME - should be inside of audiofile cache service not here ..
  // private String audioCacheExtension = "mp3";

  // TODO - switch to true for default
  // FIXME - implement this
  protected boolean useCache = false;

  /**
   * voices supported by SpeechSynthesis service
   */
  protected Map<String, Voice> voices;

  protected Map<String, List<Voice>> langIndex;

  protected Map<String, List<Voice>> langCodeIndex;

  protected Map<String, List<Voice>> genderIndex;

  private transient HashMap<String, String> effectsList = new HashMap<String, String>();

  // FIXME - remove...
  // This is the format string that will be used when asking for confirmation.
  // FIXME - why is this in english - does it make sense ? - should probably not
  // be here
  public String confirmationString = "did you say %s ?";

  // FIXME - deprecate - begin using SSML
  // specific effects and effect notation needs to be isolated to the
  // implementing service

  /**
   * NOT NEEDED AS KEY PROBLEMS ARE AUTO-HANDLED Is the SpeechSynthesis service
   * ready .. ? Several speech synthesis services require cloud api keys or in
   * some cases, only certain operating systems are supported. We are going to
   * be pessimistic - MarySpeech is "always" ready :)
   */
  // protected boolean isReady = false;

  public AbstractSpeechSynthesis(String reservedKey) {
    super(reservedKey);
    
    audioFile = (AudioFile) createPeer("audioFile");
    
    // for json loading
    if (voices == null) {
      voices = new TreeMap<String, Voice>();
    }

    if (langIndex == null) {
      langIndex = new HashMap<String, List<Voice>>();
    }

    if (langCodeIndex == null) {
      langCodeIndex = new HashMap<String, List<Voice>>();
    }

    if (genderIndex == null) {
      genderIndex = new HashMap<String, List<Voice>>();
    }
  }

  /**
   * Set a key & value for some required key info, can be both user as a key and
   * key secret - required keys are returned by getKeyNames() For Cloud Speech
   * Synthesis systems which typically require keys
   * 
   * @param keyId
   * @param keyIdSecret
   */
  public void setKey(String keyName, String keyValue) {
    Security security = Security.getInstance();
    security.addSecret(keyName, keyValue);
    broadcastState();
  }

  /**
   * this event occurs when the first audio starts for an utterance
   */
  @Override
  public String publishStartSpeaking(String utterance) {
    log.debug("publishStartSpeaking - {}", utterance);
    lastUtterance = utterance;
    return utterance;
  }

  /**
   * this event occurs when the last audio stops playing for this utterance
   */
  @Override
  public String publishEndSpeaking(String utterance) {
    log.debug("publishEndSpeaking - {}", utterance);
    return utterance;
  }
  
  /**
   * Because all AbstractSpeechSynthesis derived classes use audioFile it is
   * also an AudioData publisher.
   * 
   * @param data
   * @return
   */
  public AudioData publishAudioStart(AudioData data) {
    return data;
  }

  /**
   * Because all AbstractSpeechSynthesis derived classes use audioFile it is
   * also an AudioData publisher.
   * 
   * @param data
   * @return
   */
  public AudioData publishAudioEnd(AudioData data) {
    return data;
  }


  /**
   * attach method responsible for routing to type-mangled attach FIXME - add
   * AudioFile for caching ..
   */
  @Override
  public void attach(Attachable attachable) {
    if (attachable instanceof TextPublisher) {
      attachTextPublisher((TextPublisher) attachable);
    } else if (attachable instanceof TextPublisher) {
      attachSpeechRecognizer((SpeechRecognizer) attachable);
    } else if (attachable instanceof AudioFile) {
      audioFile = (AudioFile) attachable;
    } else {
      log.error("don't know how to attach a %s", attachable.getName());
    }
  }

  // FIXME - add attach & detach router for SpeechRecognizer !!!!

  /**
   * detach method responsible for routing to type-mangled attach
   */
  @Override
  public void detach(Attachable attachable) {
    if (attachable instanceof TextPublisher) {
      detachTextPublisher((TextPublisher) attachable);
    }
  }

  public void attachTextPublisher(TextPublisher textPublisher) {
    subscribe(textPublisher.getName(), "publishText");
  }

  public void detachTextPublisher(TextPublisher textPublisher) {
    unsubscribe(textPublisher.getName(), "publishText");
  }

  @Override
  public void onText(String text) {
    log.info("onText({})", text);
    speak(text);
  }

  /**
   * These methods are callback events from the AudioFile service which is a
   * peer of this service. The speech service sends its own event based on
   * textual data, in addition it re-broadcasts the events from audiofile which
   * were used with this text to speak, including the sound file info
   */
  public void onAudioStart(AudioData data) {
    log.info("onAudioStart {} {}", getName(), data.toString());
    
    // filters on only our speech
    if (utterances.containsKey(data)) {
      invoke("publishAudioStart", data);
      invoke("publishStartSpeaking", utterances.get(data));
    }
  }

  public void onAudioEnd(AudioData data) {
    log.info("onAudioEnd {} {}", getName(), data.toString());

    // filters on only our speech
    if (utterances.containsKey(data)) {
      invoke("publishAudioEnd", data);
      invoke("publishEndSpeaking", utterances.get(data));
      utterances.remove(data);
    }
  }
 
  // FIXME - too anthropomorphic should just be more descriptive e.g. addSpeechRecognizer or simply use attach(ear) !!!
  @Deprecated
  public void addEar(SpeechRecognizer ear) {
    attachSpeechRecognizer(ear);
  }
  
  public void attachSpeechRecognizer(SpeechRecognizer recognizer) {
    addListener("publishStartSpeaking", recognizer.getName(), "onStartSpeaking");
    addListener("publishEndSpeaking", recognizer.getName(), "onEndSpeaking");
  }

  /**
   * set the volume of the speech synthesis - default is to set the audio
   * caching system's volume - override if necessary
   */
  @Override
  public void setVolume(double volume) {
    audioFile.setVolume((float) volume);
    info("Set volume to " + volume);
    broadcastState();
  }

  @Override
  public double getVolume() {
    if (audioFile != null) {
      return audioFile.getVolume();
    } else {
      return 1.0f;
    }
  }

  // FIXME - is this in the wrong place ? - evaluate
  @Deprecated
  public void onRequestConfirmation(String text) {
    try {
      // FIXME - not exactly language independent
      speakBlocking(String.format(confirmationString, text));
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  // FIXME gluePath .. I saw //
  public String getLocalFileName(String toSpeak){
    if (getVoice() != null) {
      // no need to cache it, already ondisk
      if (toSpeak.startsWith("#") && toSpeak.endsWith("#")) {
        return "voiceEffects" + File.separator + toSpeak.replace("#", "") + ".mp3";
      }

      String filename = System.getProperty("user.dir") + File.separator + globalFileCacheDir + File.separator;

      // FIXME - I don't under why URLEncoder is here ... URLEncoder.encode(getVoice().getName(), "UTF-8")
      filename += getClass().getSimpleName() + File.separator + getVoice().getName() + File.separator + MathUtils.md5(toSpeak)
          + getAudioCacheExtension();

      // create subdirectories if necessary
      File f = new File(filename);
      File dir = f.getParentFile();
      if (!dir.exists()) {
        dir.mkdirs();
      }

      return filename;

    } else {
      return null;
    }
  }

  public void startService() {
    super.startService();
    audioFile = (AudioFile) startPeer("audioFile");
    subscribe(audioFile.getName(), "publishAudioStart");
    subscribe(audioFile.getName(), "publishAudioEnd");
    getVoices();
  }

  public void stopService() {
    super.stopService();
    unsubscribe(audioFile.getName(), "publishAudioStart");
    unsubscribe(audioFile.getName(), "publishAudioEnd");
  }

  AudioData play(String filename, boolean block) {
    return play(new AudioData(filename), block);
  }

  AudioData play(AudioData data, boolean block) {
    if (block) {
      data.mode = AudioData.MODE_BLOCKING;
    }
    return audioFile.play(data);
  }
  
  /**
   * the textual info originally requested - this may not be the same as
   * publishStartSpeaking text because the pre-processor/parser may need to break
   * it up into pieces to handle effects and other details
   * 
   * @param toSpeak
   * @return
   */
  public String publishSpeechRequested(String toSpeak) {
    return toSpeak;
  }
  
  /**
   * responsible for all parsing and pre-processing for the audio.  Sound effect, sound files,
   * SSML, TarsosDsp would all be prepared here before the audio data is generated
   * 
   * @param toSpeak
   * @param block
   * @return
   */
  public List<AudioData> parse(String toSpeak, boolean block){
    
    // TODO - not sure if we want to support this notation
    // but at the moment it seems useful 
    // splitting on sound effects ...
    // TODO - use SSML speech synthesis markup language
    
    // broadcast the original text to be processed/parsed
    invoke("publishSpeechRequested", toSpeak);
    
    // normalize to lower case
    toSpeak = toSpeak.toLowerCase();
    
    // process substitutions
    for (String substitute: substitutions.keySet()) {
      toSpeak = toSpeak.replace(substitute, substitutions.get(substitute));
    }

    List<String> spokenParts = parseEffects(toSpeak);
    
    toSpeak = filterText(toSpeak);
    
    // we generate a list of audio data to play to support
    // synthesizing this speech
    List<AudioData> playList = new ArrayList<AudioData>();
   
    for (String speak : spokenParts) {
  
      AudioData audioData = null;
      if (speak.startsWith("#") && speak.endsWith("#")) {
        audioData = new AudioData(System.getProperty("user.dir") + File.separator + "audioFile" + File.separator + "voiceEffects" + File.separator + speak.substring(1, speak.length() - 1) + ".mp3");
      } else {
        audioData = new AudioData(getLocalFileName(speak));
      }
      
      if (speak.trim().length() == 0) {
        continue;
      }
      
      process(audioData, speak, block);
      
      // effect files are handled differently from generated audio
      playList.add(audioData);
    }
    // FIXME - in theory "speaking" means generating audio from some text
    // so starting speaking event is when the first audio is "started"
    // and finished speaking is when the last audio is finished
  
    return playList;
  }
  
  /**
   * substitutions for example :
   *  worke could get substituted to worky or work-ee or "something" that phonetically works for the current
   *  speech synthesis service
   *   
   * @param key
   * @param replace
   */
  public void addSubstitution(String key, String replace) {
    substitutions.put(key.toLowerCase(), replace.toLowerCase());
  }
  
  public Long publishGenerationTime(Long timeMs) {
    return timeMs;
  }

  /**
   * process speaking - generate the text to be spoken or play a cache file if appropriate
   * @param toSpeak
   * @param block
   * @return
   * @throws UnsupportedEncodingException 
   */
  public AudioData process(AudioData audioData, String speak, boolean block) {
 
      try {

        long generateStartTs = System.currentTimeMillis();
        utterances.put(audioData, speak);
        
        if (!audioData.isValid()) {
          log.info("try generating audio data [{}] from [{}]", audioData, speak);          
          generateAudioData(audioData, speak);          
        }
        
        invoke("publishGenerationTime", System.currentTimeMillis() - generateStartTs);

        if (!audioData.isValid()) {
          log.error("speech service could not generate audio data [{}]", audioData);
          return audioData;
        }
                
        play(audioData, block);
      } catch (Exception e) {
        log.error("could not generate audio", e);
        error("%s %s", e.getClass().getSimpleName(), e.getMessage());
      }

      return audioData;
  }

  public List<AudioData> speak(String toSpeak) {
    return parse(toSpeak, false);
  }

  @Override
  public List<AudioData> speakBlocking(String toSpeak) {
    return parse(toSpeak, true);
  }

  private String filterText(String toSpeak) {

    if (toSpeak.isEmpty() || toSpeak == " " || toSpeak == null) {
      return " , ";
    }
    toSpeak = toSpeak.trim();
    toSpeak = toSpeak.replaceAll("\\n", " ");
    toSpeak = toSpeak.replaceAll("\\r", " ");
    toSpeak = toSpeak.replaceAll("\\s{2,}", " ");

    return toSpeak;
  }

  /**
   * We need to extract voices effect tagged by #
   */
  private List<String> parseEffects(String toSpeak) {
    List<String> ret = new ArrayList<String>();
    String[] parts = toSpeak.split("#");
    for (int i = 0; i < parts.length; ++i) {
      if (i % 2 == 0) {
        if (parts[i].length() > 0)
          ret.add(parts[i].trim());
      } else {
        ret.add(String.format("#%s#", parts[i]));
      }
    }
    return ret;
  }

  public AudioFile getAudioFile() {
    return audioFile;
  }

  public String getlastUtterance() {
    return lastUtterance;
  }

  /**
   * use tts engine to create an audiofile
   * 
   * @param audioData
   *          TODO
   * @param toSpeak
   *          text
   * 
   * @return byte[]
   */
  abstract public AudioData generateAudioData(AudioData audioData, String toSpeak) throws Exception;

  public void pause() {
    audioFile.pause();
  }
  
  public void resume() {
    audioFile.resume();
  }
  
  public void purgeFile(String filename) {
    audioFile.deleteFile(filename);
  }
  
  public void purgeCache() {
    // audioFile.deleteFiles(String.format("%s%s, args)globalFileCacheDir);
    audioFile.deleteFiles(this.getClass().getSimpleName());
  }

  public Voice getVoice() {
    if (voices.size() == 0) {
      // if voices aren't loaded - load them...
      getVoices();
    }
    return voice;
  }

  @Override
  public List<Voice> getVoices() {
    try {

      // load the voices from the service implementation
      // expectation is voices should be loaded & voice & defaultvoice set
      if (voices == null || voices.size() == 0) {
        log.info("loading voices begin");
        loadVoices();
        log.info("loading voices end");
      }

      // attempt to set a default voice if not set
      setDefaultVoice();

      List<Voice> vs = new ArrayList<Voice>(voices.size());
      for (Voice v : voices.values()) {
        vs.add(v);
      }

      invoke("publishVoices", vs);

      save();
      return vs;
    } catch (Exception e) {
      error("%s", e.getMessage());
    }
    return new ArrayList<Voice>();
  }

  public void setDefaultVoice() {
    log.info("attempting to set default voice");
    if (defaultVoice != null) {
      // default already set
      log.info("default voice already set");
      return;
    }

    // get our Runtime locale
    Runtime runtime = Runtime.getInstance();
    Locale locale = runtime.getLocale();
    if (locale != null) {
      log.info("locale is {}", locale);
      if (langCodeIndex.containsKey(locale.getLanguage())) {
        List<Voice> vs = langCodeIndex.get(locale.getLanguage());
        if (vs.size() > 0) {
          Voice v = vs.get(0);
          log.info("match found with Runtime locale, setting default voice to {}", v);
          defaultVoice = v;
          if (voice == null) {
            voice = defaultVoice;
          }
        } else {
          log.info("could not find language match for default locale");
        }
      } else {
        log.info("could not find locale lang code match - assigning first voice as default");
        for (Voice v : voices.values()) {
          defaultVoice = v;
          log.info("default is now {}", defaultVoice);
          break;
        }
      }
    }

    if (voice == null) {
      log.info("voice currently not set - setting to default {}", defaultVoice);
      voice = defaultVoice;
    }
  }

  /**
   * This method will be called by when AbstractSpeechSynthesis realizes it has
   * no voices. Its the responsibility of the subclass to addVoice("name",
   * "gender", "lang", impl) all the voices it provides
   */
  abstract protected void loadVoices() throws Exception;

  public List<Voice> publishVoices(List<Voice> voices) {
    return voices;
  }

  /**
   * default is no keys are necessary, but if this is a cloud provider, it will
   * probably need keys and this is where the cloud provider returns the key
   * names it needs.
   * 
   * Required keys can be set with setKey(keyname, value)
   */
  @Override
  public String[] getKeyNames() {
    return new String[] {};
  }

  public String getKey(String keyName) {
    Security security = Runtime.getSecurity();
    return security.getKey(keyName);
  }

  public List<File> getVoiceEffectFiles() {
    return audioFile.getFiles("voiceEffects", true);    
  }
  
  /**
   * default cache file type
   */
  // @Deprecated
  public String getAudioCacheExtension() {
    return ".mp3";
  }

  /**
   * supported display languages for this service e.g. Français, English, ...
   * 
   * @return
   */
  String[] getLanguages() {
    return langIndex.keySet().toArray(new String[0]);
  }

  /**
   * return all the supported language tags for this service e.g. en, es, pt
   * 
   * @return
   */
  String[] getLanguageTags() {
    return langCodeIndex.keySet().toArray(new String[0]);
  }

  /**
   * attempt to set language with tag, display and/or runtime Locale ??? - ie no
   * param
   * 
   * @param lang
   * @return
   */
  public boolean setLanguage(String lang) {

    if (voices.size() == 0) {
      try {
        loadVoices();
      } catch (Exception e) {
        error("could not set language could not load voices");
      }
    }

    // set through tag or name (first match)
    if (langIndex.containsKey(lang)) {
      if (langIndex.get(lang).size() > 0) {
        setVoice(langIndex.get(lang).get(0).getName());
        return true;
      }
    }
    if (langCodeIndex.containsKey(lang)) {
      if (langCodeIndex.get(lang).size() > 0) {
        setVoice(langCodeIndex.get(lang).get(0).getName());
        return true;
      }
    }
    return false;
  }

  public boolean setVoice(String name) {
    if (voices.containsKey(name)) {
      voice = voices.get(name);
      broadcastState();
      return true;
    }
    return false;
  }

  /**
   * addVoice adds a voice to the voices and all the voice/gender/lang indexes
   * 
   * @param name
   * @param gender
   * @param lang
   * @param voiceProvider
   */
  protected void addVoice(String name, String gender, String lang, Object voiceProvider) {
    Voice v = new Voice(name, gender, lang, voiceProvider);
    log.info("adding voice {}", v);
    if (voices.containsKey(name)) {
      warn(String.format("%s was already added %s", v.getName(), v));
    }
    voices.put(name, v);
    if (v.locale != null) {
      String langDisplay = v.locale.getDisplayLanguage();
      String langCode = v.locale.getLanguage();

      List<Voice> group = null;

      if (!langIndex.containsKey(langCode)) {
        group = new ArrayList<Voice>();
      } else {
        group = langIndex.get(langCode);
      }
      group.add(v);
      langCodeIndex.put(langCode, group);
      langIndex.put(langDisplay, group);
    }

    if (gender != null) {
      gender = gender.toLowerCase();
      List<Voice> group = null;
      if (!genderIndex.containsKey(gender)) {
        group = new ArrayList<Voice>();
      } else {
        group = genderIndex.get(gender);
      }
      group.add(v);
      genderIndex.put(gender, group);
    }
  }
  
  public void stop() {
    audioFile.stop();
  }


  /*
   * public boolean cacheContains(String filename) { File file = new
   * File(globalFileCacheDir + File.separator + filename); return file.exists();
   * }
   * 
   * public AudioData playCachedFile(String filename) { return
   * audioFile.play(globalFileCacheDir + File.separator + filename); }
   * 
   * public void cache(String filename, byte[] data, String toSpeak) throws
   * IOException { File file = new File(globalFileCacheDir + File.separator +
   * filename); File parentDir = new File(file.getParent()); if
   * (!parentDir.exists()) { parentDir.mkdirs(); } FileOutputStream fos = new
   * FileOutputStream(globalFileCacheDir + File.separator + filename);
   * fos.write(data); fos.close(); // Now append the journal entry to the
   * journal.txt file FileWriter journal = new FileWriter(globalFileCacheDir +
   * File.separator + journalFilename, true); journal.append(filename + "," +
   * toSpeak + "\r\n"); journal.close(); }
   * 
   * public static String getGlobalFileCacheDir() { return globalFileCacheDir; }
   */

  public String setAudioEffects(String audioEffects) {
    return audioEffects;
  }
  
  static public ServiceType getMetaData(String serviceType) {

    ServiceType meta = new ServiceType(serviceType);
    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.addCategory("speech");

    meta.addDependency("org.myrobotlab.audio", "voice-effects", "1.0", "zip");
    meta.addDependency("javazoom", "jlayer", "1.0.1");
    meta.addDependency("com.googlecode.soundlibs", "mp3spi", "1.9.5.4");

    return meta;
  }

}
