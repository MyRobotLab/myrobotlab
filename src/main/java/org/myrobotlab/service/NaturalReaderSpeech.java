package org.myrobotlab.service;

import java.io.IOException;
import java.net.URLEncoder;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

/**
 * Natural Reader speech to text service based on naturalreaders.com This code
 * is basically all the same as AcapelaSpeech...
 * 
 * FIXME - see if voices can be pulled down from API
 * 
 */
public class NaturalReaderSpeech extends AbstractSpeechSynthesis {

  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(NaturalReaderSpeech.class);

  transient HttpClient httpClient = null;

  private int rate = 100;

  public NaturalReaderSpeech(String reservedKey) {
    super(reservedKey);
  }
  
  /**
   * implementation specific value
   * @param rate
   */
  public void setRate(int rate) {
    this.rate = rate;
  }

  public void startService() {
    super.startService();
    httpClient = (HttpClient) startPeer("httpClient");
  }

  static public ServiceType getMetaData() {
    ServiceType meta = AbstractSpeechSynthesis.getMetaData(NaturalReaderSpeech.class.getCanonicalName());
    meta.addDescription("Natural Reader based speech service.");
    meta.setCloudService(true);
    meta.addCategory("speech");
    meta.setSponsor("kwatters");
    meta.addPeer("httpClient", "HttpClient", "httpClient");
    meta.addCategory("speech", "sound");
    meta.setAvailable(true);
    return meta;
  }

  public static void main(String[] args) throws Exception {

    LoggingFactory.init(Level.INFO);
    
    // try {
    // Runtime.start("webgui", "WebGui");
    Runtime.start("gui", "SwingGui");
    NaturalReaderSpeech reader = (NaturalReaderSpeech) Runtime.start("speech", "NaturalReaderSpeech");
    // speech.setVoice("US-English_Ronald");
    // TODO: fix the volume control
    // speech.setVolume(0);
    // speech.speakBlocking("does this work");
    // speech.getMP3ForSpeech("hello world");
    // speech.setRate(0);
    // speech.setVoice("British-English_Emily");
    reader.speakBlocking(String.format("my name is %s and i do believe I can speak?, yes, yes, i can", reader.getVoice().getName()));

    // speech.setRate(-50);
    reader.setVoice("US-English_Ronald");
    reader.speakBlocking("Hey, Watson was here!");

    // speech.setRate(-60);
    reader.setVoice("Japanese_Midori");
    reader.speakBlocking("ミロボトラブ岩");

    // }
  }

  @Override
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws IOException {

      // other examples :
      // https://api.naturalreaders.com/v4/tts/awsspeak?voiceId=de-DE_BirgitVoice&rate=100&text=test&outputFormat=mp3
      // https://api.naturalreaders.com/v4/tts/awsspeak?voiceId=Salli&rate=100&text=test&outputFormat=mp3
      // https://api.naturalreaders.com/v4/tts/ibmspeak?speaker=en-US_MichaelVoice&text=<prosody
      // rate="0%">starting left arm, I am a watson voice</prosody>
      // String url =
      // "http://api.naturalreaders.com/v4/tts/macspeak?apikey=b98x9xlfs54ws4k0wc0o8g4gwc0w8ss&src=pw&t="
      // + encoded + "&r=2&s=0";

      Voice voice = getVoice();
      String encoded = URLEncoder.encode(toSpeak, "UTF-8");
      String url = "http://api.naturalreaders.com/v4/tts/awsspeak?voiceId=" + voice.getVoiceProvider().toString() + "&rate=" + rate + "&text=" + encoded + "&outputFormat=mp3";

      byte[] b = null;

      log.info("url {}", url);
      // get mp3 file & save to cache
      // cache the mp3 content
      b = httpClient.getBytes(url);
      if (b == null || b.length == 0) {
        error("%s returned 0 byte file !!! - it may block you", getName());
        b = null;
      } else {
        FileIO.toFile(audioData.getFileName(), b);
      }
      return audioData;
  }

  @Override
  protected void loadVoices() {

    // addVoice(String name, String gender, String lang, Object voiceProvider)
    addVoice("Russell", "male", "en", "Russell");
    addVoice("Nicole", "female", "en", "Nicole");
    addVoice("Vitoria", "female", "pt", "Vitoria");
    addVoice("Ricardo", "male", "pt", "Ricardo");
    addVoice("Amy", "female", "en", "Amy");
    addVoice("Audrey", "female", "en-GB", "Audrey");
    addVoice("Emma", "female", "en", "Emma");
    addVoice("Brian", "male", "en", "Brian");
    addVoice("Chantal", "female", "fr", "Chantal");
    addVoice("Enrique", "male", "es", "Enrique");
    addVoice("Conchita", "female", "es", "Conchita");
    addVoice("Naja", "female", "da", "Naja");
    addVoice("Mads", "male", "da", "Mads");
    addVoice("Ruben", "male", "nl", "Ruben");
    addVoice("Lotte", "female", "nl", "Lotte");
    addVoice("Celine", "female", "fr", "Celine");
    addVoice("Mathieu", "male", "fr", "Mathieu");
    addVoice("Marlene", "female", "de", "Marlene");
    addVoice("Hans", "male", "de", "Hans");
    addVoice("Vicki", "female", "de", "Vicki");
    addVoice("Karl", "male", "is", "Karl");
    addVoice("Dora", "female", "is", "Dora");
    addVoice("Aditi", "female", "en", "Aditi");
    addVoice("Raveena", "female", "en", "Raveena");
    addVoice("Giorgio", "male", "it", "Giorgio");
    addVoice("Carla", "female", "it", "Carla");
    addVoice("Mizuki", "female", "jp", "Mizuki");

    addVoice("Takumi", "male", "jp", "Takumi");
    addVoice("Seoyeon", "female", "ko", "Seoyeon");
    addVoice("Liv", "female", "no", "Liv");
    addVoice("Jan", "female", "pl", "Jan");
    addVoice("Jacek", "male", "pl", "Jacek");
    addVoice("Maja", "female", "pl", "Maja");
    addVoice("Ewa", "female", "pl", "Ewa");

    addVoice("Cristiano", "male", "pt", "Cristiano");
    addVoice("Ines", "female", "pt", "Ines");
    addVoice("Carmen", "female", "ro", "Carmen");
    addVoice("Tatyana", "female", "ru", "Tatyana");
    addVoice("Maxim", "male", "ru", "Maxim");
    addVoice("Enrique", "male", "es", "Enrique");
    // addVoice("Spanish_Laura", "es-ES_LauraVoice");
    // addVoice("Spanish_Sofia", "es-LA_SofiaVoice");
    addVoice("Astrid", "female", "sv", "Astrid");
    addVoice("Filiz", "male", "tr", "Filiz");
    // addVoice("US-English_Amber", "en-US_AllisonVoice");
    addVoice("Justin", "female", "en", "Justin");
    addVoice("Joey", "male", "en", "Joey");
    addVoice("Joanna", "female", "en", "Joanna");
    addVoice("Kimberly", "female", "en", "Kimberly");
    // addVoice("US-English_Leslie", "en-US_LisaVoice");
    addVoice("Kendra", "female", "en", "Kendra");
    addVoice("Salli", "female", "en", "Salli");
    addVoice("Matthew", "male", "en", "Matthew");
    addVoice("Ivy", "female", "en", "Ivy");
    // addVoice("US-English_Ronald", "en-US_MichaelVoice");
    // addVoice("US-English_Sofia", "es-US_SofiaVoice");
    addVoice("Penelope", "female", "es", "Penelope");
    addVoice("Miguel", "male", "es", "Miguel");
    addVoice("Gwyneth", "female", "cy", "Gwyneth");
    addVoice("Geraint", "male", "cy", "Geraint");

  }

}
