/**
 * Azure Translator by Microsoft - Service
 * 
 * @author Giovanni Mirulla (Papaouitai), thanks GroG and kwatters
 * moz4r updated 10/5/17
 * 
 *         References : https://github.com/boatmeme/microsoft-translator-java-api 
 */

package org.myrobotlab.service;

import java.util.List;
import java.util.Map;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.AzureTranslatorConfig;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.myrobotlab.service.interfaces.Translator;
import org.slf4j.Logger;

import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AzureTranslator extends Service<AzureTranslatorConfig> implements Translator,TextListener,TextPublisher
{

  private static final long serialVersionUID = 1L;

  /**
   * key field name for security service
   */
  public final static String AZURE_TRANSLATOR_KEY = "azure.translator.key";

  public final static Logger log = LoggerFactory.getLogger(AzureTranslator.class);

  /**
   * detect incoming text language
   */
  boolean detect = false;

  public boolean isDetect() {
    return detect;
  }

  public void setDetect(boolean detect) {
    this.detect = detect;
  }

  /**
   * language to translate to
   */
  String to = "fr";

  /**
   * language to translate from
   */
  String from = "en";

  /**
   * location
   */
  String location = "eastus";

  String[] languages = null;

  transient OkHttpClient client = new OkHttpClient();

  public AzureTranslator(String n, String id) {
    super(n, id);
    languages = getLanguages();
  }

  @Override
  public void attachTextListener(String name) {
    addListener("publishText", name);
  }

  @Override
  public void attachTextListener(TextListener service) {
    attachTextListener(service.getName());
  }

  @Override
  public void attachTextPublisher(TextPublisher service) {
    if (service == null) {
      log.warn("{}.attachTextPublisher(null)");
      return;
    }
    subscribe(service.getName(), "publishText");
  }

  public String getKey() {
    Security security = Runtime.getSecurity();
    return security.getKey(AZURE_TRANSLATOR_KEY);
  }

  public String[] getLanguages() {
    String[] languages = { "af", "sq", "am", "ar", "hy", "as", "az", "bn", "ba", "eu", "bs", "bg", "yue", "ca", "lzh",
        "zh-Hans", "zh-Hant", "hr", "cs", "da", "prs", "dv", "nl",
        "en", "et", "fj", "fil", "fi", "fr", "fr-ca", "gl", "ka", "de", "el", "gu", "ht", "he", "hi", "mww", "hu", "is",
        "id", "ikt", "iu", "iu-Latn", "ga", "it", "ja", "kn", "kk",
        "km", "tlh-Latn", "tlh-Piqd", "ko", "ku", "kmr", "ky", "lo", "lv", "lt", "mk", "mg", "ms", "ml", "mt", "mi",
        "mr", "mn-Cyrl", "mn-Mong", "my", "ne", "nb", "or", "ps", "fa",
        "pl", "pt", "pt-pt", "pa", "otq", "ro", "ru", "sm", "sr-Cyrl", "sr-Latn", "sk", "sl", "so", "es", "sw", "sv",
        "ty", "ta", "tt", "te", "th", "bo", "ti", "to", "tr", "tk",
        "uk", "hsb", "ur", "ug", "uz", "vi", "cy", "yua", "zu" };
    return languages;
  }

  @Override
  public void onText(String text) {
    translate(text);
  }

  @Override
  public String publishText(String text) {
    return text;
  }

  public String publishDetectedLanguage(String text) {
    return text;
  }

  public String setFrom(String from) {
    this.from = from;
    return from;
  }

  public void setKey(String keyValue) {
    Security security = Security.getInstance();
    security.setKey(AZURE_TRANSLATOR_KEY, keyValue);
    broadcastState();
  }

  public String setLocation(String location) {
    this.location = location;
    return location;
  }

  public String setTo(String to) {
    this.to = to;
    return to;
  }

  public String translate(String toTranslate) {
    return translate(toTranslate, from, to);
  }

  // This function performs a POST request.
  public String translate(String toTranslate, String from, String to) {
    StringBuilder sb = new StringBuilder();
    try {
      Builder builder = new HttpUrl.Builder().scheme("https").host("api.cognitive.microsofttranslator.com")
          .addPathSegment("/translate").addQueryParameter("api-version", "3.0")
          .addQueryParameter("to", to);

      if (!detect) {
        builder.addQueryParameter("from", from);
      }
      HttpUrl url = builder.build();
      toTranslate = toTranslate.replace("\"", "");
      MediaType mediaType = MediaType.parse("application/json");
      RequestBody body = RequestBody.create(mediaType, "[{\"Text\": \" " + toTranslate + " \"}]");
      Request request = new Request.Builder().url(url).post(body).addHeader("Ocp-Apim-Subscription-Key", getKey())
          .addHeader("Ocp-Apim-Subscription-Region", location)
          .addHeader("Content-type", "application/json").build();
      Response response = client.newCall(request).execute();
      String resp = response.body().string();
      // if ()
      List<Map> list = CodecUtils.fromJson(resp, List.class);
      for (Map t : list) {
        Map detected = (Map) t.get("detectedLanguage");
        if (detected != null) {
          invoke("publishDetectedLanguage", detected.get("language"));
        }
        List<Map> translations = (List<Map>) t.get("translations");
        for (Map trans : translations) {
          sb.append(trans.get("text"));
        }
      }
    } catch (Exception e) {
      error(e);
    }
    invoke("publishText", sb.toString());
    return sb.toString();
  }

  public static void main(String[] args) throws Exception {
    LoggingFactory.init(Level.INFO);
    try {

      AzureTranslator translator = (AzureTranslator) Runtime.start("translator", "AzureTranslator");
      // translator.setKey("xxxxxxxxxxxxxxxxxxxxxxxx");
      translator.setLocation("eastus");
      translator.setFrom("en");
      translator.setTo("fr");
      String translated = translator.translate("Hey, I think I got it to work !!!");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      log.info("translated {}", translated);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public void setToLanguage(String to) {
    this.to = to;
  }

  @Override
  public void setFromLanguage(String from) {
    this.from = from;
  }

}
