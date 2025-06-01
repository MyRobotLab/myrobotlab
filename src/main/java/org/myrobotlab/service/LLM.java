package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.StaticType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.programab.Response;
import org.myrobotlab.service.config.HttpClientConfig;
import org.myrobotlab.service.config.LLMConfig;
import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.data.Utterance;
import org.myrobotlab.service.interfaces.ImageListener;
import org.myrobotlab.service.interfaces.ResponsePublisher;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.myrobotlab.service.interfaces.UtteranceListener;
import org.myrobotlab.service.interfaces.UtterancePublisher;
import org.slf4j.Logger;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.OllamaResult;
import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequestModel;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.models.generate.OllamaStreamHandler;
import io.github.ollama4j.utils.Options;
import io.github.ollama4j.utils.OptionsBuilder;

/**
 * https://beta.openai.com/account/api-keys
 * 
 * https://beta.openai.com/playground
 * 
 * https://beta.openai.com/examples
 * 
 * <pre>
     * curl https://api.openai.com/v1/completions \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $OPENAI_API_KEY" \
      -d '{
      "model": "text-ada-001",
      "prompt": "why is the sun so bright ?\n\nThe sun is bright because it is a star. It is born on theentlement, or radiance, which is the product of the surface area of the sun's clouds and the surface area of the sun's atmosphere.",
      "temperature": 0.7,
      "max_tokens": 256,
      "top_p": 1,
      "frequency_penalty": 0,
      "presence_penalty": 0
    }'
 * 
 * </pre>
 * 
 * @author GroG
 *
 */

public class LLM extends Service<LLMConfig> implements TextListener, TextPublisher, UtterancePublisher, UtteranceListener, ResponsePublisher, ImageListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(LLM.class);

  protected String currentChannel;

  protected String currentBotName;

  protected String currentChannelName;

  protected String currentChannelType;

  transient protected OllamaAPI api = null;

  protected String ollam4JUrl = "";

  protected Map<String, Object> inputs = new LinkedHashMap<>();

  List<LinkedHashMap<String, Object>> userMessages = new ArrayList<>();

  List<OllamaChatMessage> history = new ArrayList<>();

  public void addInput(String key, Object value) {
    inputs.put(key, value);
  }

  public void removeInput(String key) {
    inputs.remove(key);
  }

  public Object getInput(String key) {
    return inputs.get(key);
  }

  public void clearInputs() {
    inputs.clear();
  }

  public String createImagePrompt(String model, String prompt, List<String> images) {

    if (model == null) {
      model = config.model;
    }

    if (prompt == null) {
      prompt = config.defaultImagePrompt;
    }

    if (images == null || images.size() == 0) {
      error("no images in image request");
      return null;
    }

    LinkedHashMap<String, Object> msg = new LinkedHashMap<>();
    msg.put("model", model);
    msg.put("prompt", prompt);
    msg.put("images", images);
    msg.put("stream", true);
    msg.put("n", 1);

    return CodecUtils.toJson(msg);

  }

  OllamaAPI getOllamaApi() throws MalformedURLException {
    if (api == null || ollam4JUrl == null || !ollam4JUrl.contentEquals(config.url)) {
      URL url = new URL(config.url);
      ollam4JUrl = config.url;
      Integer port = null;
      if (url.getPort() == -1) {
        if (url.getProtocol() == "https") {
          port = 443;
        } else {
          port = 80;
        }
      } else {
        port = url.getPort();
      }
      api = new OllamaAPI(String.format("%s://%s:%d", url.getProtocol(), url.getHost(), port));
      api.setRequestTimeoutSeconds(config.timeout);
    }
    return api;
  }

  public String createChatCompletionPayload(String model, String systemContent, String userContent, int n, float temperature, int maxTokens) {
    try {
      // Create the map to hold the request parameters
      LinkedHashMap<String, Object> requestPayload = new LinkedHashMap<>();
      requestPayload.put("model", model);

      // Create and format date and time strings
      LocalDateTime currentDateTime = LocalDateTime.now();
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
      DateTimeFormatter fullDateFormatter = DateTimeFormatter.ofPattern("EEEE MMMM d'th' yyyy h:mm a");

      inputs.put("Date", currentDateTime.format(dateFormatter));
      inputs.put("Time", currentDateTime.format(timeFormatter));
      inputs.put("DateTime", currentDateTime.format(fullDateFormatter));

      // Replace placeholders in system content
      for (Map.Entry<String, Object> entry : inputs.entrySet()) {
        if (entry.getValue() != null) {
          systemContent = systemContent.replace(String.format("{{%s}}", entry.getKey()), entry.getValue().toString());
        }
      }

      // Create system message
      LinkedHashMap<String, Object> systemMessage = new LinkedHashMap<>();
      systemMessage.put("role", "system");
      systemMessage.put("content", systemContent);

      // Handle message history
      LinkedHashMap<String, Object> userMessage = new LinkedHashMap<>();
      userMessage.put("role", "user");
      userMessage.put("content", userContent);
      userMessages.add(userMessage);

      if (config.maxHistory > 0) {
        while (userMessages.size() > config.maxHistory) {
          userMessages.remove(0);
        }
      } else {
        userMessages.clear();
      }

      // Combine messages
      List<LinkedHashMap<String, Object>> allMessages = new ArrayList<>();
      allMessages.add(systemMessage);
      allMessages.addAll(userMessages);
      requestPayload.put("messages", allMessages);

      // Add other parameters
      requestPayload.put("n", n);
      requestPayload.put("temperature", temperature);
      requestPayload.put("max_tokens", maxTokens);

      return CodecUtils.toJson(requestPayload);

    } catch (Exception e) {
      error(e);
      return null;
    }
  }

  public LinkedHashMap<String, Object> createFunctionDefinition(String name, String description, LinkedHashMap<String, Object> parameters) {
    LinkedHashMap<String, Object> functionDefinition = new LinkedHashMap<>();
    functionDefinition.put("name", name);
    functionDefinition.put("description", description);
    functionDefinition.put("parameters", parameters);
    return functionDefinition;
  }

  public LLM(String n, String id) {
    super(n, id);
  }

  public Response getResponseStream(String text) {
    return getResponseStream(text, null);
  }

  public static byte[] readUriContent(String uriString) throws Exception {
    URI uri = new URI(uriString);

    // Check if it's a local file
    if ("file".equals(uri.getScheme())) {
      return Files.readAllBytes(Paths.get(uri));
    }

    // For remote URIs (http or https)
    try (InputStream inputStream = uri.toURL().openStream()) {
      return inputStream.readAllBytes();
    }
  }

  /***
   * Converts images into a default chat completion prompt
   * 
   * @param imageUrls
   * @return
   */
  public Response getResponse(List<String> imageUrls) {
    return getResponse(config.defaultImagePrompt, imageUrls);
  }

  /**
   * convert images to array of bytes
   * 
   * @param text
   * @param imageUrls
   * @return
   */
  public Response getResponse(String text, List<String> imageUrls) {
    // List<byte[]> bimages = null;
    // if (images != null) {
    // bimages = new ArrayList<>();
    // for (String uri : images) {
    // if (uri.startsWith("file://")) {
    // try {
    // bimages.add(readUriContent(uri));
    // } catch (Exception e) {
    // error(e);
    // }
    // }
    // }
    // }
    return getResponseStream(text, imageUrls);
  }

  public Response getResponseStream(String text, List<String> imageUrls) {
    try {

      if (config.sleepWord != null && text.contains(config.sleepWord) && !config.sleeping) {
        sleep();
      }

      if (config.sleeping) {
        return null;
      }

      // Create and format date and time strings
      LocalDateTime currentDateTime = LocalDateTime.now();
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
      DateTimeFormatter fullDateFormatter = DateTimeFormatter.ofPattern("EEEE MMMM d'th' yyyy h:mm a");

      inputs.put("Date", currentDateTime.format(dateFormatter));
      inputs.put("Time", currentDateTime.format(timeFormatter));
      inputs.put("DateTime", currentDateTime.format(fullDateFormatter));

      String systemContent = config.system;

      // Replace placeholders in system content
      for (Map.Entry<String, Object> entry : inputs.entrySet()) {
        if (entry.getValue() != null) {
          systemContent = systemContent.replace(String.format("{{%s}}", entry.getKey()), entry.getValue().toString());
        }
      }

      history.add(new OllamaChatMessage(OllamaChatMessageRole.USER, text));

      if (config.maxHistory > 0) {
        while (history.size() > config.maxHistory) {
          history.remove(0);
        }
      } else {
        history.clear();
      }

      StreamHandler handler = new StreamHandler();

      List<OllamaChatMessage> msgList = new ArrayList<>();

      OllamaChatMessage msg = new OllamaChatMessage();
      msg.setRole(OllamaChatMessageRole.SYSTEM);
      msg.setContent(systemContent);
      // msg.setImages(images); does not work for chat completions !!!!
      msgList.add(msg);
      msgList.addAll(history);

      // system

      // history

      // files

      // tools (lame)

      OptionsBuilder optionBuilder = new OptionsBuilder();
      Options options = optionBuilder.build();

      OllamaChatRequestModel request = new OllamaChatRequestModel(config.model, msgList);

      OllamaAPI ollamaApi = getOllamaApi();
      String responseText = null;

      if (imageUrls != null) {
        OllamaResult result = ollamaApi.generateWithImageURLs(config.model, text, imageUrls, options, handler);
        responseText = result.getResponse();
      } else {
        OllamaChatResult result = ollamaApi.chat(request, handler);
        history.add(new OllamaChatMessage(OllamaChatMessageRole.ASSISTANT, result.getResponse()));
        responseText = result.getResponse();
      }

      // we are at the end of our response of streaming, and now the "result"
      // will unblock signalling the end of the response
      // now we have to check to see if there is any extra text on the end that
      // did not get published
      if (handler.sentenceBuilder[0] != null && handler.sentenceBuilder[0].toString().trim().length() > 0) {
        invoke("publishText", handler.sentenceBuilder[0].toString());

        Utterance utterance = new Utterance();
        utterance.username = getName();
        utterance.text = handler.sentenceBuilder[0].toString();
        utterance.isBot = true;
        utterance.channel = currentChannel;
        utterance.channelType = currentChannelType;
        utterance.channelBotName = currentBotName;
        utterance.channelName = currentChannelName;
        invoke("publishUtterance", utterance);

        Response response = new Response("friend", getName(), handler.sentenceBuilder[0].toString(), null);
        invoke("publishResponse", response);

      }

      Response response = new Response("friend", getName(), responseText, null);
      invoke("publishResponse", response);
      return response;

    } catch (Exception e) {
      error(e);
    }

    return null;
  }

  public class StreamHandler implements OllamaStreamHandler {

    final StringBuilder[] sentenceBuilder = { new StringBuilder() };
    final int[] lastProcessedLength = { 0 }; // Track the length of already
    public String potentialSentence = null;

    @Override
    public void accept(String message) {
      final int MIN_SENTENCE_LENGTH = 5; // Minimum length for a valid sentence

      // Append only the new portion of the text
      String newText = message.substring(lastProcessedLength[0]);
      sentenceBuilder[0].append(newText);

      // Update the last processed length
      lastProcessedLength[0] = message.length();

      // Check for completed sentences in the accumulated text
      int lastPeriodIndex = sentenceBuilder[0].lastIndexOf(".");
      int lastQuestionIndex = sentenceBuilder[0].lastIndexOf("?");

      // Use the last index of either punctuation as the sentence end
      int lastSentenceEndIndex = Math.max(lastPeriodIndex, lastQuestionIndex);

      if (lastSentenceEndIndex != -1) {
        // Extract the potential sentence
        potentialSentence = sentenceBuilder[0].substring(0, lastSentenceEndIndex + 1).trim();

        // Only process if the sentence is above the minimum length
        if (potentialSentence.length() >= MIN_SENTENCE_LENGTH) {
          invoke("publishText", potentialSentence);

          Utterance utterance = new Utterance();
          utterance.username = getName();
          utterance.text = potentialSentence;
          utterance.isBot = true;
          utterance.channel = currentChannel;
          utterance.channelType = currentChannelType;
          utterance.channelBotName = currentBotName;
          utterance.channelName = currentChannelName;
          invoke("publishUtterance", utterance);

          Response response = new Response("friend", getName(), potentialSentence, null);
          invoke("publishResponse", response);

          // Keep any remaining text after the last sentence-ending character
          sentenceBuilder[0] = new StringBuilder(sentenceBuilder[0].substring(lastSentenceEndIndex + 1));
        }
      }
    }
  }

  public Response getResponse(String text) {

    try {

      invoke("publishRequest", text);

      if (text == null || text.trim().length() == 0) {
        log.info("emtpy text, not responding");
        return null;
      }

      if (config.stream && !config.url.contains("openai")) {
        return getResponseStream(text);
      }

      String responseText = "";

      if (config.sleepWord != null && text.contains(config.sleepWord) && !config.sleeping) {
        sleep();
        responseText = "Ok, I will go to sleep";
      }

      if (!config.sleeping) {
        // avoid 0,8000000
        String temp = String.format(Locale.US, "\t\"temperature\": %f\r\n", config.temperature);

        // chat completions
        // String json = "{\r\n" + " \"model\": \"" + config.model + "\",\r\n" +
        // " \"messages\": [{\"role\": \"user\", \"content\": \"" + text +
        // "\"}],\r\n" + temp + " }";

        String json = createChatCompletionPayload(config.model, config.system, text, 1, config.temperature, config.maxTokens);

        HttpClient<HttpClientConfig> http = (HttpClient) startPeer("http");

        log.info("curl {} -d '{}'", config.url, json);

        String msg = http.postJson(config.password, config.url, json);
        log.info("url: {}", config.url);
        log.info("json: {}", json);
        System.out.print(json);

        Map<String, Object> payload = CodecUtils.fromJson(msg, new StaticType<>() {
        });

        @SuppressWarnings({ "unchecked", "rawtypes" })
        Map<String, Object> errors = (Map) payload.get("error");
        if (errors != null) {
          error((String) errors.get("message"));
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<Object> choices = (List) payload.get("choices");
        if (choices != null && choices.size() > 0) {
          @SuppressWarnings({ "unchecked", "rawtypes" })
          Map<String, Object> textObject = (Map) choices.get(0);
          responseText = (String) textObject.get("text");
          if (responseText == null) {
            // /chat/completions
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Map<String, Object> content = (Map) textObject.get("message");
            // role=assistant
            responseText = (String) content.get("content");
          }

        } else {
          warn("no response for %s", text);
        }

      } else {
        log.info("sleeping waiting for wake word \"{}\"", config.wakeWord);
      }

      if (config.wakeWord != null && text.contains(config.wakeWord) && config.sleeping) {
        responseText = "Hello, I am awake";
        wake();
      }

      Response response = new Response("friend", getName(), responseText, null);
      Utterance utterance = new Utterance();
      utterance.username = getName();
      utterance.text = responseText;
      utterance.isBot = true;
      utterance.channel = currentChannel;
      utterance.channelType = currentChannelType;
      utterance.channelBotName = currentBotName;
      utterance.channelName = currentChannelName;
      if (responseText != null && responseText.length() > 0) {
        invoke("publishUtterance", utterance);
        invoke("publishResponse", response);
        invoke("publishText", responseText);
      }

      return response;

    } catch (IOException e) {
      error(e);
    }
    return null;
  }

  public Response getImageResponse(String base64Image) {
    return getImageResponse(base64Image, null, null);
  }

  public Response getImageResponse(String base64Image, String prompt) {
    return getImageResponse(base64Image, prompt, null);
  }

  public Response getImageResponse(String base64Image, String prompt, String model) {
    try {

      if (prompt == null) {
        prompt = config.defaultImagePrompt;
      }
      // String.format("data:image/%s;base64,%s",
      invoke("publishImageRequest", new ImageRequest(base64Image, prompt));

      List<String> images = new ArrayList<>();
      images.add(base64Image);
      String json = createImagePrompt(model, prompt, images);

      HttpClient<HttpClientConfig> http = (HttpClient) startPeer("http");

      // log.info("curl {} -d '{}'", config.url, json);

      String msg = http.postJson(config.password, config.url, json);
      log.error("url: {}", config.url);

      Map<String, Object> payload = CodecUtils.fromJson(msg, new StaticType<>() {
      });

      Response response = null;

      if (payload.get("response") != null) {
        String responseText = payload.get("response").toString();
        response = new Response("friend", getName(), responseText, null);
        Utterance utterance = new Utterance();
        utterance.username = getName();
        utterance.text = responseText;
        utterance.isBot = true;
        utterance.channel = currentChannel;
        utterance.channelType = currentChannelType;
        utterance.channelBotName = currentBotName;
        utterance.channelName = currentChannelName;
        if (responseText != null && responseText.length() > 0) {
          invoke("publishUtterance", utterance);
          invoke("publishResponse", response);
          invoke("publishText", responseText);
        }
      }

      return response;

    } catch (Exception e) {
      error(e);
    }
    return null;
  }

  /**
   * Overridden error to also publish the errors probably would be a better
   * solution to self subscribe to errors and have the subscriptions publish
   * utterances/responses/text
   */
  @Override
  public Status error(String error) {
    Status status = super.error(error);
    invoke("publishText", error);
    Response response = new Response("friend", getName(), error, null);
    Utterance utterance = new Utterance();
    utterance.text = error;
    invoke("publishUtterance", utterance);
    invoke("publishResponse", response);
    return status;
  }

  public String publishRequest(String text) {
    return text;
  }

  public static class ImageRequest {
    public String base64Image;
    public String prompt;

    public ImageRequest(String base64Image, String prompt) {
      this.base64Image = base64Image;
      this.prompt = prompt;

    }
  }

  public ImageRequest publishImageRequest(ImageRequest request) {
    return request;
  }

  public void setToken(String password) {
    config.password = password;
  }

  public String setEngine(String engine) {
    config.model = engine;
    return engine;
  }

  @Override
  public void onUtterance(Utterance utterance) throws Exception {
    currentChannelType = utterance.channelType;
    currentChannel = utterance.channel;
    currentBotName = utterance.channelBotName;
    currentChannelName = utterance.channelName;
    // prevent bots going off the rails
    if (utterance.isBot) {
      log.info("Not responding to bots.");
      return;
    }
    getResponse(utterance.text);
  }

  @Override
  public Utterance publishUtterance(Utterance utterance) {
    return utterance;
  }

  @Override
  public String publishText(String text) {
    return text;
  }

  @Override
  public void onText(String text) throws IOException {
    getResponse(text);
  }

  @Override
  public Response publishResponse(Response response) {
    return response;
  }

  /**
   * wakes the global session up
   */
  public void wake() {
    log.info("wake now");
    config.sleeping = false;
  }

  /**
   * sleeps the global session
   */
  public void sleep() {
    log.info("sleeping now");
    config.sleeping = true;
  }

  @Override
  public void attach(Attachable attachable) {

    /*
     * if (attachable instanceof ResponseListener) { // this one is done
     * correctly attachResponseListener(attachable.getName()); } else
     */
    if (attachable instanceof TextPublisher) {
      attachTextPublisher((TextPublisher) attachable);
    } else if (attachable instanceof TextListener) {
      addListener("publishText", attachable.getName(), "onText");
    } else if (attachable instanceof UtteranceListener) {
      attachUtteranceListener(attachable.getName());
    } else {
      log.error("don't know how to attach a {}", attachable.getName());
    }
  }

  public void clearHistory() {
    userMessages.clear();
  }

  public static URI toFileURI(String filePath) throws URISyntaxException {
    try {
        URI uri = new URI(filePath);
        if (uri.isAbsolute()) {
            return uri;
        }
    } catch (URISyntaxException e) {
    }

    Path path = Paths.get(filePath);
    
    return path.toUri();
}
  
  @Override
  public void onImage(ImageData img) {
    try {
    String src = (img.src != null)?img.src:img.source;
    List<String> urls = new ArrayList<>();
    urls.add(toFileURI(src).toASCIIString());
    getResponse(urls);
    } catch(Exception e) {
      error(e);
    }
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Response response = null;
      LLM llm = (LLM) Runtime.start("llm", "LLM");
      // llm.getConfig().timeout = 1;
      Runtime.start("python", "Python");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      Runtime.start("cv", "OpenCV");

      boolean done = true;
      if (done) {
        return;
      }

      LLM imagellm = (LLM) Runtime.start("imagellm", "LLM");

      llm.config.url = "http://192.168.0.24:11434/v1/chat/completions";
      llm.config.stream = true;
      // response = llm.getResponse("Hello, why is the sky blue?");
      response = llm.getResponse("Tell me a very long story about 3 dragons");
      if (response != null) {
        System.out.println(response.msg);
      }

      OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");
      cv.capture();

      for (int i = 0; i < 100; ++i) {

        while (cv.getBase64Image() == null) {
          Service.sleep(1000);
        }

        String base64Image = cv.getBase64Image();
        imagellm.config.url = "http://fast:11434/api/generate";
        imagellm.config.model = "bakllava";
        response = imagellm.getImageResponse(base64Image);
        System.out.println(response.msg);
      }

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
