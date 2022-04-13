/**
 * Azure Translator by Microsoft - Service
 * 
 * @author Giovanni Mirulla (Papaouitai), thanks GroG and kwatters
 * moz4r updated 10/5/17
 * 
 *         References : https://github.com/boatmeme/microsoft-translator-java-api 
 */

package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AzureTranslator extends Service implements TextListener, TextPublisher {

  private static final long serialVersionUID = 1L;

  /**
   * key field name for security service
   */
  public final static String AZURE_TRANSLATOR_KEY = "azure.translator.key";

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

  transient OkHttpClient client = new OkHttpClient();

  public final static Logger log = LoggerFactory.getLogger(AzureTranslator.class);

  public String setTo(String to) {
    this.to = to;
    return to;
  }

  public String setFrom(String from) {
    this.from = from;
    return from;
  }

  public String setLocation(String location) {
    this.location = location;
    return location;
  }

  public AzureTranslator(String n, String id) {
    super(n, id);
  }

  public String translate(String toTranslate) {
    return translate(toTranslate, from, to);
  }

  // This function performs a POST request.
  public String translate(String toTranslate, String from, String to) {
    try {
      HttpUrl url = new HttpUrl.Builder().scheme("https").host("api.cognitive.microsofttranslator.com").addPathSegment("/translate").addQueryParameter("api-version", "3.0")
          .addQueryParameter("from", from).addQueryParameter("to", to).build();

      toTranslate = toTranslate.replace("\"", "");
      MediaType mediaType = MediaType.parse("application/json");
      RequestBody body = RequestBody.create(mediaType, "[{\"Text\": \" " + toTranslate + " \"}]");
      Request request = new Request.Builder().url(url).post(body).addHeader("Ocp-Apim-Subscription-Key", getKey()).addHeader("Ocp-Apim-Subscription-Region", location)
          .addHeader("Content-type", "application/json").build();
      Response response = client.newCall(request).execute();
      String ret = response.body().string();
      
      invoke("publishText", ret);

      return ret;
    } catch (Exception e) {
      error(e);
    }
    return null;
  }

  // This function prettifies the json response.
  public static String prettify(String json_text) {
    JsonParser parser = new JsonParser();
    JsonElement json = parser.parse(json_text);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(json);
  }

  @Override
  public String publishText(String text) {
    return text;
  }

  @Override
  public void attachTextListener(TextListener service) {
    attachTextListener(service.getName());
  }

  @Override
  public void onText(String text) {
    translate(text);
  }

  @Override
  public void attachTextPublisher(TextPublisher service) {
    if (service == null) {
      log.warn("{}.attachTextPublisher(null)");
      return;
    }
    subscribe(service.getName(), "publishText");
  }

  @Override
  public void attachTextListener(String name) {
    addListener("publishText", name);
  }

  public void setKey(String keyValue) {
    Security security = Security.getInstance();
    security.setKey(AZURE_TRANSLATOR_KEY, keyValue);
    broadcastState();
  }

  public String getKey() {
    Security security = Runtime.getSecurity();
    return security.getKey(AZURE_TRANSLATOR_KEY);
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
      log.info("translated {}", translated);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
