package org.myrobotlab.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.slf4j.Logger;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.polly.AmazonPollyClientBuilder;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.polly.model.Voice;

/**
 * Amazon's cloud speech service
 * 
 * Free Tier The Amazon Polly free tier includes 5 million characters per month,
 * for the first 12 months, starting from the first request for speech.
 * 
 * Polly Pricing Pay-as-you-go $4.00 per 1 million characters (when outside the
 * free tier).
 *
 * @author gperry
 *
 */
public class Polly extends AbstractSpeechSynthesis {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Polly.class);
  // stored inside json
  HashMap<String, String> voiceInJsonConfig;
  // end
  transient AWSCredentials credentials;
  transient AmazonPollyClient polly;
  transient Voice awsVoice;
  transient List<Voice> awsVoices;

  transient Map<String, Voice> voiceMap = new HashMap<String, Voice>();
  transient Map<String, Voice> langMap = new HashMap<String, Voice>();

  public Polly(String n) {
    super(n);
  }

  @Override
  public void setKeys(String keyId, String keyIdSecret) {
    security.addSecret("amazon.polly.user.key", keyId);
    security.addSecret("amazon.polly.user.secret", keyIdSecret);
    security.saveStore();
    setVoice(getVoice());
    setEngineError("Ready");
    speak("ok");

    ;
    broadcastState();
  }

  @Override
  public String[] getKeys() {
    String[] Keys = new String[2];
    security.loadStore();
    Keys[0] = security.getSecret("amazon.polly.user.key");
    Keys[1] = security.getSecret("amazon.polly.user.secret");
    return Keys;
  }

  @Override
  public List<String> getVoices() {
    getPolly();
    getVoiceList().clear();
    getVoiceList().addAll(voiceMap.keySet());
    return getVoiceList();
  }

  @Override
  public boolean setVoice(String voice) {
    if (subSetVoice(voice)) {
      try {
        awsVoice = voiceMap.get(voice);
        // this.language = awsVoice.getLanguageCode();
        return true;
      } catch (IllegalArgumentException e) {
        error("Unknown Polly Voice : " + voice);

      }
    }
    return false;
  }

  @Override
  public String getLanguage() {
    return language;
  }

  @Override
  public List<String> getLanguages() {
    return new ArrayList<String>(langMap.keySet());
  }

  private void processVoicesRequest() {
    // Create describe voices request.
    DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

    // Synchronously ask Polly Polly to describe available TTS voices.
    DescribeVoicesResult describeVoicesResult = polly.describeVoices(describeVoicesRequest);
    awsVoices = describeVoicesResult.getVoices();
    log.info("found {} voices", awsVoices.size());
    for (int i = 0; i < awsVoices.size(); ++i) {
      Voice voice = awsVoices.get(i);
      voiceMap.put(voice.getName(), voice);
      langMap.put(voice.getLanguageCode(), voice);
      log.info("{} {} - {}", i, voice.getName(), voice.getLanguageCode());
    }

  }

  private AmazonPollyClient getPolly() {
    String key = getKeys()[0];
    String secret = getKeys()[1];
    if (polly == null && key != null && !key.isEmpty() && secret != null && !secret.isEmpty()) {
      try {

        if (credentials == null) {
          // try credential chain - in case they have set env vars
          credentials = new BasicAWSCredentials(key, secret);
        }

        // polly = (AmazonPollyClient)
        // AmazonPollyClientBuilder.standard().withCredentials(new
        // AWSStaticCredentialsProvider(credentials)).withRegion(Regions.US_EAST_1).build();
        polly = (AmazonPollyClient) AmazonPollyClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(Regions.US_WEST_2).build();
        processVoicesRequest();
        setEngineError("Online");
        setEngineStatus(true);
      } catch (Exception e) {
        try {
          log.error("could not get client with keys supplied - trying default chain", e);
          polly = new AmazonPollyClient(new DefaultAWSCredentialsProviderChain(), new ClientConfiguration());
          // polly.setRegion(Region.getRegion(Regions.US_EAST_1));
          polly.setRegion(Region.getRegion(Regions.US_WEST_2));
          processVoicesRequest();
          setEngineError("Online");
          setEngineStatus(true);
        } catch (Exception e2) {

          error("could not get Polly client - did you setKeys ?");
          error("Environment variables â€“ AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY or");
          error("check http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html");
          log.error("giving up", e2);

          polly = null;
          credentials = null;
        }
      }
    }
    return polly;

  }

  @Override
  public void setLanguage(String l) {
    if (langMap.containsKey(l)) {
      setVoice(langMap.get(l).getName());
    }
    this.language = l;
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

    ServiceType meta = new ServiceType(Polly.class.getCanonicalName());
    meta.addDescription("used as a general template");
    meta.setAvailable(true); // false if you do not want it viewable in a
    // gui
    // add dependency if necessary
    meta.addPeer("audioFile", "AudioFile", "audioFile");
    meta.addPeer("security", "Security", "security");
    /*
     * meta.addDependency("org.joda", "2.9.4");
     * meta.addDependency("org.apache.commons.httpclient", "4.5.2");
     */
    meta.addDependency("com.fasterxml.jackson.core", "jackson-core", "2.9.5");
    meta.addDependency("com.fasterxml.jackson.core", "jackson-databind", "2.9.5");
    meta.addDependency("com.fasterxml.jackson.core", "jackson-annotations", "2.9.5");

    meta.addDependency("com.amazonaws", "aws-java-sdk-polly", "1.11.118");

    // <!-- https://mvnrepository.com/artifact/com.amazonaws/aws-java-sdk-polly
    // -->
    // <dependency org="com.amazonaws" name="aws-java-sdk-polly"
    // rev="1.11.118"/>

    meta.addCategory("speech");
    meta.setCloudService(true);
    return meta;
  }

  public void startService() {
    super.startService();
    security = (Security) startPeer("security");
    security.startService();
    subSpeechStartService();

  }

  public static void main(String[] args) {

    LoggingFactory.init(Level.WARN);

    Polly polly = (Polly) Runtime.start("polly", "Polly");
    Runtime.start("gui", "SwingGui");
    // add your amazon access key & secret
    // use gui to do this, or force it here only ONCE :
    // polly.setKeys("key","secret");

    List<String> voices = polly.getVoices();
    /*
     * for (String voice : voices) { polly.setVoice(voice); String lang =
     * polly.getLanguage(); log.info("{} speaks {}", voice, lang);
     * 
     * if (lang.startsWith("es")) { polly.speak(String.
     * format("Hola, mi nombre es %s y creo que myrobotlab es genial!", voice));
     * } else if (lang.startsWith("en")) { polly.speak(String.
     * format("Hi my name is %s and I think myrobotlab is great!", voice)); }
     * else if (lang.startsWith("fr")) { polly.speak(String.
     * format("Bonjour, mon nom est %s et je pense que myrobotlab est génial!!",
     * voice)); } else if (lang.startsWith("nl")) { polly.speak(String.
     * format("Hallo, mijn naam is %s en ik denk dat myrobotlab is geweldig!",
     * voice)); } else if (lang.startsWith("ru")) { polly.speak(String.
     * format("Привет, меня зовут %s, и я думаю, что myrobotlab - это здорово!",
     * voice)); } else if (lang.startsWith("ro")) { polly.speak(String.
     * format("Buna ziua, numele meu este %s și cred că myrobotlab este mare!",
     * voice)); } else if (lang.startsWith("ro")) { polly.speak(String.
     * format("Witam, nazywam się %s i myślę, że myrobotlab jest świetny!",
     * voice)); } else if (lang.startsWith("it")) { polly.speak(String.
     * format("Ciao, il mio nome è %s e penso myrobotlab è grande!", voice)); }
     * else if (lang.startsWith("is")) { polly.speak(String.
     * format("Halló, Nafn mitt er %s og ég held myrobotlab er frábært!",
     * voice)); } else if (lang.startsWith("cy")) { polly.speak(String.
     * format("Helo, fy enw i yw %s a fi yn meddwl myrobotlab yn wych!",
     * voice)); } else if (lang.startsWith("de")) { polly.speak(String.
     * format("Hallo, mein Name ist %s und ich denke, myrobotlab ist toll!",
     * voice)); } else if (lang.startsWith("da")) { polly.speak(String.
     * format("Hej, mit navn er %s og jeg tror myrobotlab er fantastisk!",
     * voice)); } else if (lang.startsWith("sv")) { polly.speak(String.
     * format("Hej, mitt namn %s och jag tror ElektronikWikin är stor!",
     * voice)); } else if (lang.startsWith("pl")) { polly.speak(String.
     * format("Witam, nazywam się %s i myślę, że myrobotlab jest świetny!",
     * voice)); } else if (lang.startsWith("tr")) { polly.speak(String.
     * format("Merhaba, adım adım %s ve myrobotlab'ın harika olduğunu düşünüyorum!"
     * , voice)); } else if (lang.startsWith("pt")) { polly.speak(String.
     * format("Olá, meu nome é %s e eu acho que myrobotlab é ótimo!！", voice));
     * } else if (lang.startsWith("ja")) {
     * polly.speak(String.format("こんにちは、私の名前は%sで、私はmyrobotlabが素晴らしいと思います！",
     * voice)); } else { log.info("dunno"); } }
     * 
     * polly.setVoice("Russel"); polly.setVoice("Nicole");
     * 
     * polly.setVoice("Brian"); polly.setVoice("Amy"); polly.setVoice("Emma");
     * 
     * polly.setVoice("Brian"); polly.setVoice("Kimberly");
     * 
     * polly.setVoice("Justin"); polly.setVoice("Joey");
     * polly.setVoice("Raveena"); polly.setVoice("Ivy");
     * polly.setVoice("Kendra");
     * 
     * polly.speak("this is a new thing");
     * 
     * polly.
     * speak("Hello there, i am a cloud service .. i probably sound like the echo"
     * ); polly.speak("Here is another sentence");
     * polly.speak("To be or not to be that is the question");
     * polly.speakBlocking("now i am blocking my speech");
     * polly.speakBlocking("put one foot in front of the other and"); // xxx
     * polly.speakBlocking("soon you'll be walking out the door");
     * polly.speak("this is a new sentence");
     * polly.speak("to be or not to be that is the question");
     * polly.speak("weather tis nobler in the mind to suffer");
     * polly.speak("the slings and arrows of ourtrageous fortune");
     * polly.speak("or to take arms against a see of troubles");
     * 
     * 
     */

    polly.speakBlocking("or to take arms against a see of troubles");
    polly.speak("the slings and arrows of ourtrageous fortune");

    log.info("finished");
  }

  /*
   * Proxy works in 3 modes client - consumer of mrl services relay - proxy
   * (running as cloud service) direct - goes direct to service In Polly's case
   * - client - would be an end user using a client key relay - is the mrl proxy
   * service direct would be from a users, by-passing mrl and going directly to
   * Amazon with amazon keys cache file - caches file locally (both client or
   * relay)
   */
  @Override
  public byte[] generateByteAudio(String toSpeak) {
    try {
      AmazonPollyClient polly = getPolly();
      awsVoice = voiceMap.get(getVoice());
      SynthesizeSpeechRequest synthReq = new SynthesizeSpeechRequest().withText(toSpeak).withVoiceId(awsVoice.getId()).withOutputFormat(getAudioCacheExtension());
      SynthesizeSpeechResult synthRes = polly.synthesizeSpeech(synthReq);
      InputStream data = synthRes.getAudioStream();
      return FileIO.toByteArray(data);
    } catch (Exception e) {
      log.error("Polly generateByteAudio threw, API error ? : ", e);
      setEngineError("generateByteAudio failed");
      setEngineStatus(false);
      return null;
    }
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
