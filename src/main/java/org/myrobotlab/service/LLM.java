package org.myrobotlab.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import org.myrobotlab.service.data.Utterance;
import org.myrobotlab.service.interfaces.ResponsePublisher;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.myrobotlab.service.interfaces.UtteranceListener;
import org.myrobotlab.service.interfaces.UtterancePublisher;
import org.slf4j.Logger;

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

public class LLM extends Service<LLMConfig> implements TextListener, TextPublisher, UtterancePublisher, UtteranceListener, ResponsePublisher {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(LLM.class);

  protected String currentChannel;

  protected String currentBotName;

  protected String currentChannelName;

  protected String currentChannelType;

  protected Map<String, Object> inputs = new LinkedHashMap<>();

  List<LinkedHashMap<String, Object>> userMessages = new ArrayList<>();

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

  public Response getResponse(String text) {

    try {

      invoke("publishRequest", text);

      if (text == null || text.trim().length() == 0) {
        log.info("emtpy text, not responding");
        return null;
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
        log.error("url: {}", config.url);
        log.error("json: {}", json);
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

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      // Runtime runtime = Runtime.getInstance();
      // Runtime.startConfig("gpt3-01");
      Runtime.start("llm", "LLM");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      /*
       * Gpt3 i01_chatBot = (Gpt3) Runtime.start("i01.chatBot", "Gpt3");
       * 
       * bot.attach("i01.chatBot"); i01_chatBot.attach("bot");
       * 
       * i01_chatBot.getResponse("hi, how are you?");
       * 
       * Runtime.start("webgui", "WebGui");
       */

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
