package org.myrobotlab.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.config.PollyConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.polly.AmazonPollyClientBuilder;
import com.amazonaws.services.polly.model.AmazonPollyException;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.polly.model.TextType;

/**
 * Amazon's cloud speech service
 * 
 * Free Tier The Amazon Polly free tier includes 5 million characters per month,
 * for the first 12 months, starting from the first request for speech.
 * 
 * Polly Pricing Pay-as-you-go $4.00 per 1 million characters (when outside the
 * free tier).
 * 
 * Ssml - https://docs.aws.amazon.com/polly/latest/dg/supportedtags.html
 * https://docs.amazonaws.cn/en_us/polly/latest/dg/polly-dg.pdf
 *
 * @author GroG
 *
 */
public class Polly extends AbstractSpeechSynthesis<PollyConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Polly.class);

  public final static String AMAZON_POLLY_USER_KEY = "amazon.polly.user.key";
  public final static String AMAZON_POLLY_USER_SECRET = "amazon.polly.user.secret";

  private transient AWSCredentials credentials;
  private transient AmazonPolly polly;

  Regions defaultRegion;

  public Polly(String n, String id) {
    super(n, id);
  }

  /**
   * for the user's convenience for amazon other cloud providers have single
   * keys or different details
   * 
   * @param keyId
   *          aws polly user key
   * @param keyIdSecret
   *          aws polly user secret
   */
  public void setKeys(String keyId, String keyIdSecret) {
    setKey(AMAZON_POLLY_USER_KEY, keyId);
    setKey(AMAZON_POLLY_USER_SECRET, keyIdSecret);
    loadVoices();
  }

  /**
   * loadVoices - must be loaded by SpeechSynthesis class - contract of
   * AbstractSpeechSynthesis
   */
  @Override
  public void loadVoices() {
    getPolly();

    if (polly == null) {
      error("cannot load voices - polly not initialized");
      return;
    } else {
      setReady(true);
    }

    // Create describe voices request.
    DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();
    DescribeVoicesResult describeVoicesResult = polly.describeVoices(describeVoicesRequest);
    List<com.amazonaws.services.polly.model.Voice> awsVoices = describeVoicesResult.getVoices();
    log.info("found {} voices", awsVoices.size());
    for (int i = 0; i < awsVoices.size(); ++i) {
      com.amazonaws.services.polly.model.Voice awsImpl = awsVoices.get(i);
      log.info("{} voice {}", i, awsImpl);
      // stripAccents : Voices with accent seem no worky ( anymore )
      addVoice(StringUtils.stripAccents(awsImpl.getName()), awsImpl.getGender(), awsImpl.getLanguageCode(), awsImpl);
    }
    broadcastState();
  }

  /**
   * @param regionName
   *          set default region for polly
   */
  public void setRegion(String regionName) {
    defaultRegion = Regions.fromName(regionName);
  }

  /**
   * For a cloud provider we have to make sure certain dependencies are met,
   * inter-net connect, keys, and the population of AbstractSpeechSynthesis
   * voices.
   * 
   * @return polly client
   */
  private AmazonPolly getPolly() {

    if (defaultRegion == null) {
      defaultRegion = Regions.DEFAULT_REGION;
    }

    String key = getKey(AMAZON_POLLY_USER_KEY);
    String secret = getKey(AMAZON_POLLY_USER_SECRET);

    if (polly == null) {

      if (key == null || secret == null) {
        error("this service requires a key and a secret");
        return null;
      }

      try {

        if (credentials == null) {
          // try credential chain - in case they have set env vars
          credentials = new BasicAWSCredentials(key, secret);
        }

        polly = AmazonPollyClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(defaultRegion).build();

      } catch (Exception e) {
        try {
          log.error("could not get client with keys supplied - trying default chain", e);
          polly = AmazonPollyClient.builder().withCredentials(new DefaultAWSCredentialsProviderChain()).withClientConfiguration(new ClientConfiguration()).withRegion(defaultRegion)
              .build();
        } catch (Exception e2) {

          error("could not get Polly client - did you set the keys?");
          log.error("giving up", e2);

          polly = null;
          credentials = null;
        }
      }
    }
    if (polly != null) {
      // check for credentials OK, because this is not catch upper
      try {
        DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();
        polly.describeVoices(describeVoicesRequest);
      } catch (AmazonPollyException e3) {
        error("The keys are invalid");
        polly = null;
        credentials = null;
      }
    }
    return polly;
  }

  @Override
  public String[] getKeyNames() {
    return new String[] { AMAZON_POLLY_USER_KEY, AMAZON_POLLY_USER_SECRET };
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
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws IOException {
    // if (!configured) {
    // log.error("polly not configured yet");
    // return null;
    // }
    log.info("toSpeak {}", toSpeak);

    PollyConfig c = (PollyConfig) config;
    getPolly();
    setVoice(c.voice);
    Voice voice = getVoice();
    if (voice == null) {
      error("invalid voice - have keys been set ?");
      return null;
    }
    // com.amazonaws.services.polly.model.Voice awsVoice =
    // ((com.amazonaws.services.polly.model.Voice) voice.getVoiceProvider());
    SynthesizeSpeechRequest synthReq = new SynthesizeSpeechRequest().withText(toSpeak).withVoiceId(voice.getName()).withOutputFormat("mp3");

    if (toSpeak.contains("<speak") && c.autoDetectSsml) {
      c.ssml = true;
    } else if (!toSpeak.contains("<speak") && c.autoDetectSsml) {
      c.ssml = false;
    }

    if (c.ssml) {
      synthReq.setTextType(TextType.Ssml);
    }
    SynthesizeSpeechResult synthRes = polly.synthesizeSpeech(synthReq);
    InputStream data = synthRes.getAudioStream();
    byte[] d = FileIO.toByteArray(data);
    // could just save it to file ...
    // return new AudioData(data);
    FileIO.toFile(audioData.getFileName(), d);
    return audioData;
  }

  public boolean setSsml(boolean ssml) {
    PollyConfig c = (PollyConfig) config;
    c.ssml = ssml;
    return ssml;
  }

  public boolean setAutoDetectSsml(boolean ssml) {
    PollyConfig c = (PollyConfig) config;
    c.ssml = ssml;
    return ssml;
  }

  @Override
  public void releaseService() {
    super.releaseService();
    if (polly != null) {
      polly.shutdown();
    }
  }

  @Override
  public boolean isReady() {
    setReady(polly != null ? true : false);
    return ready;
  }

  public PollyConfig apply(PollyConfig c) {
    super.apply(c);
    getVoices();
    return c;
  }

  public void setPollyClient(AmazonPolly pollyClient) {
    this.polly = pollyClient;
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init("WARN");

      Runtime.start("polly", "Polly");
      // Runtime runtime = Runtime.getInstance();
      // runtime.save();

      // Runtime.start("python", "Python");

      // Plan plan = Runtime.load("webgui", "WebGui");
      // // Plan plan = Runtime.load("polly", "Polly");
      //
      // WebGuiConfig webgui = (WebGuiConfig)plan.get("webgui");
      // webgui.autoStartBrowser = false;
      //
      // Runtime.setConfig("default");
      // Runtime.load("polly");
      //
      // Runtime.startConfig("webgui");
      // Runtime.startConfig("polly");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      boolean b = true;
      if (b) {
        return;
      }

      // iterate through all speech services
      // all will "load" voices and adhere to the AbtractSpeechSynthesis
      // processing of data

      // test all caching

      // test all effects

      // test default

      // test overriding default

      // test setting Runtime.locale
      // Platform.setVirtual(true);

      // LoggingFactory.init(Level.INFO);

      // set language universally
      // Runtime.setLanguage("pt");
      // Runtime.start("gui", "SwingGui");

      Polly polly = (Polly) Runtime.start("polly", "Polly");
      // polly.setKey(keyName, keyValue);
      // polly.setKeys("XXXXXXXX", "XXXXXXXXXXXXXXXXXXXXXXXXXX");

      // polly.setLanguage("de");
      log.info("polly voice is {}", polly.getVoice());
      // polly.speak(String.format("allo there my name is %s",
      // polly.getVoice().getName()));

      // Runtime.start("gui", "SwingGui");
      // add your amazon access key & secret
      // use gui to do this, or force it here only ONCE :
      // polly.setKeys("key","secret");

      List<Voice> voices = polly.getVoices();
      for (Voice voice : voices) {
        // polly.setVoice(voice); String lang = polly.getLanguage();
        log.info("{}", voice);
      }

      polly.speak(String.format("Hello my name is %s I will be your voice today", polly.getVoice().getName()));
      polly.speakBlocking("or to take arms against a see of troubles");
      polly.speak("the slings and arrows of ourtrageous fortune");
      polly.speak("#THROAT01_M# hi! it works.");

      for (Voice voice : voices) {
        polly.setVoice((String) voice.getVoiceProvider());
        String lang = voice.getLanguage();
        log.info("{} speaks {}", voice, lang);

        if (lang.startsWith("es")) {
          polly.speak(String.format("Hola, mi nombre es %s y creo que myrobotlab es genial!", voice));
        } else if (lang.startsWith("en")) {
          polly.speak(String.format("Hi my name is %s and I think myrobotlab is great!", voice));
        } else if (lang.startsWith("fr")) {
          polly.speak(String.format("Bonjour, mon nom est %s et je pense que myrobotlab est génial!!", voice));
        } else if (lang.startsWith("nl")) {
          polly.speak(String.format("Hallo, mijn naam is %s en ik denk dat myrobotlab is geweldig!", voice));
        } else if (lang.startsWith("ru")) {
          polly.speak(String.format("Привет, меня зовут %s, и я думаю, что myrobotlab - это здорово!", voice));
        } else if (lang.startsWith("ro")) {
          polly.speak(String.format("Buna ziua, numele meu este %s și cred că myrobotlab este mare!", voice));
        } else if (lang.startsWith("ro")) {
          polly.speak(String.format("Witam, nazywam się %s i myślę, że myrobotlab jest świetny!", voice));
        } else if (lang.startsWith("it")) {
          polly.speak(String.format("Ciao, il mio nome è %s e penso myrobotlab è grande!", voice));
        } else if (lang.startsWith("is")) {
          polly.speak(String.format("Halló, Nafn mitt er %s og ég held myrobotlab er frábært!", voice));
        } else if (lang.startsWith("cy")) {
          polly.speak(String.format("Helo, fy enw i yw %s a fi yn meddwl myrobotlab yn wych!", voice));
        } else if (lang.startsWith("de")) {
          polly.speak(String.format("Hallo, mein Name ist %s und ich denke, myrobotlab ist toll!", voice));
        } else if (lang.startsWith("da")) {
          polly.speak(String.format("Hej, mit navn er %s og jeg tror myrobotlab er fantastisk!", voice));
        } else if (lang.startsWith("sv")) {
          polly.speak(String.format("Hej, mitt namn %s och jag tror ElektronikWikin är stor!", voice));
        } else if (lang.startsWith("pl")) {
          polly.speak(String.format("Witam, nazywam się %s i myślę, że myrobotlab jest świetny!", voice));
        } else if (lang.startsWith("tr")) {
          polly.speak(String.format("Merhaba, adım adım %s ve myrobotlab'ın harika olduğunu düşünüyorum!", voice));
        } else if (lang.startsWith("pt")) {
          polly.speak(String.format("Olá, meu nome é %s e eu acho que myrobotlab é ótimo!！", voice));
        } else if (lang.startsWith("ja")) {
          polly.speak(String.format("こんにちは、私の名前は%sで、私はmyrobotlabが素晴らしいと思います！", voice));
        } else {
          log.info("dunno");
        }
      }

      polly.setVoice("Russel");
      polly.setVoice("Nicole");

      polly.setVoice("Brian");
      polly.setVoice("Amy");
      polly.setVoice("Emma");

      polly.setVoice("Brian");
      polly.setVoice("Kimberly");

      polly.setVoice("Justin");
      polly.setVoice("Joey");
      polly.setVoice("Raveena");
      polly.setVoice("Ivy");
      polly.setVoice("Kendra");

      polly.speak("this is a new thing");

      polly.speak("Hello there, i am a cloud service .. i probably sound like the echo");
      polly.speak("Here is another sentence");
      polly.speak("To be or not to be that is the question");
      polly.speakBlocking("now i am blocking my speech");
      polly.speakBlocking("put one foot in front of the other and"); // xxx
      polly.speakBlocking("soon you'll be walking out the door");
      polly.speak("this is a new sentence");
      polly.speak("to be or not to be that is the question");
      polly.speak("weather tis nobler in the mind to suffer");
      polly.speak("the slings and arrows of ourtrageous fortune");
      polly.speak("or to take arms against a see of troubles");

      log.info("finished");
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
