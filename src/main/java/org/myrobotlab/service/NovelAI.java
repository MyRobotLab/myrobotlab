package org.myrobotlab.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.config.NovelAIConfig;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class NovelAI extends AbstractSpeechSynthesis<NovelAIConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(NovelAI.class);

  private transient OkHttpClient client = new OkHttpClient();

  public NovelAI(String n, String id) {
    super(n, id);
    client = new OkHttpClient();
  }

  /**
   * The methods apply and getConfig can be used, if more complex configuration
   * handling is needed. By default, the framework takes care of most of it,
   * including subscription handling.
   * 
   * <pre>
  &#64;Override
  public ServiceConfig apply(ServiceConfig c) {
    super.apply(c)
    return c;
  }
  
  @Override
  public ServiceConfig getConfig() {
    super.getConfig()
    return config;
  }
   * </pre>
   **/

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("novelai", "NovelAI");
      Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws Exception {

    String baseUrl = "https://api.novelai.net/ai/generate-voice?voice=-1&seed=" + config.voice + "&opus=false&version=v2&text=";
    String encodedText = URLEncoder.encode(toSpeak, StandardCharsets.UTF_8.toString());
    String url = baseUrl + encodedText;

    Request request = new Request.Builder().url(url).build();

    try {
      Response response = client.newCall(request).execute();

      if (response.isSuccessful()) {
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
          byte[] bytes = responseBody.bytes();
          FileIO.toFile(audioData.getFileName(), bytes);
        }
      } else {
        error("request failed with code: " + response.code());
      }
    } catch (Exception e) {
      error(e);
    }
    return audioData;
  }

  @Override
  public void loadVoices() throws Exception {
    addVoice("Aini", "female", "en", "Aini");
    addVoice("Ligeia", "female", "en", "Ligeia");
    addVoice("Orea", "female", "en", "Orea");
    addVoice("Claea", "female", "en", "Claea");
    addVoice("Lim", "female", "en", "Lim");
    addVoice("Aurae", "female", "en", "Aurae");
    addVoice("Naia", "female", "en", "Naia");

    addVoice("Aulon", "male", "en", "Aulon");
    addVoice("Elei", "male", "en", "Elei");
    addVoice("Ogma", "male", "en", "Ogma");
    addVoice("Raid", "male", "en", "Raid");
    addVoice("Pega", "male", "en", "Pega");
    addVoice("Lam", "male", "en", "Lam");
  }
}
