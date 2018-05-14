package org.myrobotlab.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.slf4j.Logger;

/**
 * Natural Reader speech to text service based on naturalreaders.com This code
 * is basically all the same as AcapelaSpeech...
 */
public class NaturalReaderSpeech extends AbstractSpeechSynthesis {

  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(NaturalReaderSpeech.class);

  HashMap<String, String> voiceMap = new HashMap<String, String>();
  HashMap<String, String> voiceMapType = new HashMap<String, String>();
  // stored inside json
  HashMap<String, String> voiceInJsonConfig;
  // end

  transient Stack<String> audioFiles = new Stack<String>();

  private int IbmRate = 0;
  private int AwsRate = 100;
  private int rate = 0;

  public NaturalReaderSpeech(String reservedKey) {
    super(reservedKey);
  }

  public void startService() {
    super.startService();

    // needed because of an ssl error on the natural reader site
    System.setProperty("jsse.enableSNIExtension", "false");

    voiceMap.put("Australian-English_Noah", "Russell");
    voiceMap.put("Australian-English_Olivia", "Nicole");
    voiceMap.put("Brazilian-Portuguese_Manuela", "Vitoria");
    voiceMap.put("Brazilian-Portuguese_Miguel", "Ricardo");
    voiceMap.put("British-English_Charlotte", "Amy");
    voiceMap.put("British-English_Emily", "Emma");
    voiceMap.put("British-English_John", "Brian");
    voiceMap.put("Canadian-French_Adèle", "Chantal");
    voiceMap.put("Castilian-Spanish_Alejandro", "Enrique");
    voiceMap.put("Castilian-Spanish_Lucia", "Conchita");
    voiceMap.put("Danish_Line", "Naja");
    voiceMap.put("Danish_Mikkel", "Mads");
    voiceMap.put("Dutch_Birgit", "de-DE_BirgitVoice");
    voiceMap.put("Dutch_Daan", "Ruben");
    voiceMap.put("Dutch_Dieter", "de-DE_DieterVoice");
    voiceMap.put("Dutch_Roos", "Lotte");
    voiceMap.put("French_Chloé", "Celine");
    voiceMap.put("French_Gabriel", "Mathieu");
    voiceMap.put("French_Renee", "fr-FR_ReneeVoice");
    voiceMap.put("GB-English_Carrie", "en-GB_KateVoice");
    voiceMap.put("German_Ida", "Marlene");
    voiceMap.put("German_Johann", "Hans");
    voiceMap.put("German_Vicki", "Vicki");
    voiceMap.put("Icelandic_Gunnar", "Karl");
    voiceMap.put("Icelandic_Helga", "Dora");
    voiceMap.put("Indian-English_Aditi", "Aditi");
    voiceMap.put("Indian-English_Padma", "Raveena");
    voiceMap.put("Italian_Francesca", "it-IT_FrancescaVoice");
    voiceMap.put("Italian_Francesco", "Giorgio");
    voiceMap.put("Italian_Giulia", "Carla");
    voiceMap.put("Japanese_Hana", "Mizuki");
    voiceMap.put("Japanese_Midori", "ja-JP_EmiVoice");
    voiceMap.put("Japanese_Takumi", "Takumi");
    voiceMap.put("Korean_Seoyeon", "Seoyeon");
    voiceMap.put("Norwegian_Ingrid", "Liv");
    voiceMap.put("Polish_Jakub", "Jan");
    voiceMap.put("Polish_Kacper", "Jacek");
    voiceMap.put("Polish_Lena", "Maja");
    voiceMap.put("Polish_Zofia", "Ewa");
    voiceMap.put("Portuguese_BR-Isabela", "pt-BR_IsabelaVoice");
    voiceMap.put("Portuguese_Joao", "Cristiano");
    voiceMap.put("Portuguese_Mariana", "Ines");
    voiceMap.put("Romanian_Elena", "Carmen");
    voiceMap.put("Russian_Olga", "Tatyana");
    voiceMap.put("Russian_Sergei", "Maxim");
    voiceMap.put("Spanish_Enrique", "es-ES_EnriqueVoice");
    voiceMap.put("Spanish_Laura", "es-ES_LauraVoice");
    voiceMap.put("Spanish_Sofia", "es-LA_SofiaVoice");
    voiceMap.put("Swedish_Elsa", "Astrid");
    voiceMap.put("Turkish_Esma", "Filiz");
    voiceMap.put("US-English_Amber", "en-US_AllisonVoice");
    voiceMap.put("US-English_David", "Justin");
    voiceMap.put("US-English_James", "Joey");
    voiceMap.put("US-English_Jennifer", "Joanna");
    voiceMap.put("US-English_Kathy", "Kimberly");
    voiceMap.put("US-English_Leslie", "en-US_LisaVoice");
    voiceMap.put("US-English_Linda", "Kendra");
    voiceMap.put("US-English_Mary", "Salli");
    voiceMap.put("US-English_Matthew", "Matthew");
    voiceMap.put("US-English_Polly", "Ivy");
    voiceMap.put("US-English_Ronald", "en-US_MichaelVoice");
    voiceMap.put("US-English_Sofia", "es-US_SofiaVoice");
    voiceMap.put("US-Spanish_Isabella", "Penelope");
    voiceMap.put("US-Spanish_Matías", "Miguel");
    voiceMap.put("Welsh_Seren", "Gwyneth");
    voiceMap.put("Welsh-English_Gareth", "Geraint");

    voiceMapType.put("Australian-English_Noah", "aws");
    voiceMapType.put("Australian-English_Olivia", "aws");
    voiceMapType.put("Brazilian-Portuguese_Manuela", "aws");
    voiceMapType.put("Brazilian-Portuguese_Miguel", "aws");
    voiceMapType.put("British-English_Charlotte", "aws");
    voiceMapType.put("British-English_Emily", "aws");
    voiceMapType.put("British-English_John", "aws");
    voiceMapType.put("Canadian-French_Adèle", "aws");
    voiceMapType.put("Castilian-Spanish_Alejandro", "aws");
    voiceMapType.put("Castilian-Spanish_Lucia", "aws");
    voiceMapType.put("Danish_Line", "aws");
    voiceMapType.put("Danish_Mikkel", "aws");
    voiceMapType.put("Dutch_Birgit", "ibm");
    voiceMapType.put("Dutch_Daan", "aws");
    voiceMapType.put("Dutch_Dieter", "ibm");
    voiceMapType.put("Dutch_Roos", "aws");
    voiceMapType.put("French_Chloé", "aws");
    voiceMapType.put("French_Gabriel", "aws");
    voiceMapType.put("French_Renee", "ibm");
    voiceMapType.put("GB-English_Carrie", "ibm");
    voiceMapType.put("German_Ida", "aws");
    voiceMapType.put("German_Johann", "aws");
    voiceMapType.put("German_Vicki", "aws");
    voiceMapType.put("Icelandic_Gunnar", "aws");
    voiceMapType.put("Icelandic_Helga", "aws");
    voiceMapType.put("Indian-English_Aditi", "aws");
    voiceMapType.put("Indian-English_Padma", "aws");
    voiceMapType.put("Italian_Francesca", "ibm");
    voiceMapType.put("Italian_Francesco", "aws");
    voiceMapType.put("Italian_Giulia", "aws");
    voiceMapType.put("Japanese_Hana", "aws");
    voiceMapType.put("Japanese_Midori", "ibm");
    voiceMapType.put("Japanese_Takumi", "aws");
    voiceMapType.put("Korean_Seoyeon", "aws");
    voiceMapType.put("Norwegian_Ingrid", "aws");
    voiceMapType.put("Polish_Jakub", "aws");
    voiceMapType.put("Polish_Kacper", "aws");
    voiceMapType.put("Polish_Lena", "aws");
    voiceMapType.put("Polish_Zofia", "aws");
    voiceMapType.put("Portuguese_BR-Isabela", "ibm");
    voiceMapType.put("Portuguese_Joao", "aws");
    voiceMapType.put("Portuguese_Mariana", "aws");
    voiceMapType.put("Romanian_Elena", "aws");
    voiceMapType.put("Russian_Olga", "aws");
    voiceMapType.put("Russian_Sergei", "aws");
    voiceMapType.put("Spanish_Enrique", "ibm");
    voiceMapType.put("Spanish_Laura", "ibm");
    voiceMapType.put("Spanish_Sofia", "ibm");
    voiceMapType.put("Swedish_Elsa", "aws");
    voiceMapType.put("Turkish_Esma", "aws");
    voiceMapType.put("US-English_Amber", "ibm");
    voiceMapType.put("US-English_David", "aws");
    voiceMapType.put("US-English_James", "aws");
    voiceMapType.put("US-English_Jennifer", "aws");
    voiceMapType.put("US-English_Kathy", "aws");
    voiceMapType.put("US-English_Leslie", "ibm");
    voiceMapType.put("US-English_Linda", "aws");
    voiceMapType.put("US-English_Mary", "aws");
    voiceMapType.put("US-English_Matthew", "aws");
    voiceMapType.put("US-English_Polly", "aws");
    voiceMapType.put("US-English_Ronald", "ibm");
    voiceMapType.put("US-English_Sofia", "ibm");
    voiceMapType.put("US-Spanish_Isabella", "aws");
    voiceMapType.put("US-Spanish_Matías", "aws");
    voiceMapType.put("Welsh_Seren", "aws");
    voiceMapType.put("Welsh-English_Gareth", "aws");

    getVoiceList().addAll(voiceMap.keySet());
    subSpeechStartService();
  }

  public String getMp3Url(String toSpeak) {

    // TODO: url encode this.

    String encoded = toSpeak;
    String voiceId = voiceMap.get(getVoice());
    String provider = voiceMapType.get(getVoice());
    String url = "";
    if (provider == "ibm") {
      rate = IbmRate;
      try {
        encoded = URLEncoder.encode("<prosody rate=\"" + rate + "%\">" + toSpeak + "</prosody>", "UTF-8");
      } catch (UnsupportedEncodingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      url = "http://api.naturalreaders.com/v4/tts/ibmspeak?speaker=" + voiceId + "&text=" + encoded;
    }
    if (provider == "aws") {
      rate = AwsRate;
      try {
        encoded = URLEncoder.encode(toSpeak, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      url = "http://api.naturalreaders.com/v4/tts/awsspeak?voiceId=" + voiceId + "&rate=" + rate + "&text=" + encoded + "&outputFormat=mp3";
    }

    // https://api.naturalreaders.com/v4/tts/awsspeak?voiceId=Salli&rate=100&text=test&outputFormat=mp3
    // https://api.naturalreaders.com/v4/tts/ibmspeak?speaker=en-US_MichaelVoice&text=<prosody
    // rate="0%">starting left arm, I am a watson voice</prosody>
    // String url =
    // "http://api.naturalreaders.com/v4/tts/macspeak?apikey=b98x9xlfs54ws4k0wc0o8g4gwc0w8ss&src=pw&t="
    // + encoded + "&r=2&s=0";

    log.info("URL FOR AUDIO:{}", url);
    return url;
  }

  public void setRate(int rate) {
    // 0 is normal +x fast / -x slow
    this.IbmRate = rate;
    this.AwsRate = rate + 100;
    audioCacheParameters = "rate-" + rate;
  }

  @Override
  public String getLanguage() {
    return null;
  }

  @Override
  public List<String> getLanguages() {
    // TODO Auto-generated method stub
    ArrayList<String> ret = new ArrayList<String>();
    // FIXME - add iso language codes currently supported e.g. en en_gb de
    // etc..
    return ret;
  }

  @Override
  public void setLanguage(String l) {
    this.language = l;
  }

  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(NaturalReaderSpeech.class.getCanonicalName());
    meta.addDescription("Natural Reader based speech service.");
    meta.setCloudService(true);
    meta.addCategory("speech");
    meta.setSponsor("kwatters");
    subGetMetaData(meta); // meta.addTodo("test speak blocking - also what is
                          // the return type and
    // AudioFile audio track id ?");
    // end of support
    meta.setAvailable(false);
    return meta;
  }

  public static void main(String[] args) throws Exception {

    LoggingFactory.init(Level.INFO);
    // try {
    // Runtime.start("webgui", "WebGui");
    Runtime.start("gui", "SwingGui");
    NaturalReaderSpeech speech = (NaturalReaderSpeech) Runtime.start("speech", "NaturalReaderSpeech");
    // speech.setVoice("US-English_Ronald");
    // TODO: fix the volume control
    // speech.setVolume(0);
    // speech.speakBlocking("does this work");
    // speech.getMP3ForSpeech("hello world");
    speech.setRate(0);
    speech.setVoice("British-English_Emily");
    speech.speakBlocking("does it works?");

    speech.setRate(-50);
    speech.setVoice("US-English_Ronald");
    speech.speakBlocking("Hey, Watson was here!");

    speech.setRate(-60);
    speech.setVoice("Japanese_Midori");
    speech.speakBlocking("ミロボトラブ岩");

    // }
  }

  @Override
  public byte[] generateByteAudio(String toSpeak) {
    String mp3Url = getMp3Url(toSpeak);

    byte[] b = null;
    try {

      log.info("mp3Url {}", mp3Url);
      // get mp3 file & save to cache
      // cache the mp3 content
      b = httpClient.getBytes(mp3Url);
      if (b == null || b.length == 0) {
        error("%s returned 0 byte file !!! - it may block you", getName());
        b = null;
      }

    } catch (Exception e) {
      Logging.logError(e);
    }

    return b;
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
  public ArrayList<String> getVoices() {
    return new ArrayList<String>(getVoiceList());
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
