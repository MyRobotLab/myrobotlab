package org.myrobotlab.service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.myrobotlab.service.config.VLMConfig;
import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.data.Utterance;
import org.myrobotlab.service.interfaces.ImageListener;
import org.myrobotlab.service.interfaces.ImagePublisher;
import org.myrobotlab.service.interfaces.ResponsePublisher;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.myrobotlab.service.interfaces.UtteranceListener;
import org.myrobotlab.service.interfaces.UtterancePublisher;
import org.slf4j.Logger;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.OllamaResult;
import io.github.ollama4j.models.generate.OllamaStreamHandler;
import io.github.ollama4j.utils.Options;
import io.github.ollama4j.utils.OptionsBuilder;

/**
 * VLM - Vision Language Model service.
 * 
 * Accepts text and images and responds with a relevant, grounded answer. It is
 * backed by an open source vision model that can be run completely offline
 * through a local <a href="https://ollama.com">Ollama</a> server. The default
 * model is <b>LLaVA</b> (Large Language and Vision Assistant), an open source,
 * Apache/MIT licensed multimodal model.
 * 
 * <pre>
 *   # one time setup - download the model (a few GB)
 *   ollama pull llava
 *   # ollama serves on http://localhost:11434 by default
 * </pre>
 * 
 * Images can be supplied as:
 * <ul>
 * <li>a local file path or {@code file://} / {@code http(s)://} URL</li>
 * <li>a base64 encoded string (e.g. from {@link OpenCV#getBase64Image()})</li>
 * <li>an {@link ImageData} published by any {@link ImagePublisher} (OpenCV,
 * etc.) - attach the VLM as an image listener</li>
 * </ul>
 * 
 * Responses are published as text, utterances and responses so the service can
 * drive speech or chat just like the {@link LLM} service.
 */
public class VLM extends Service<VLMConfig>
    implements ImageListener, TextListener, TextPublisher, UtterancePublisher, UtteranceListener, ResponsePublisher {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(VLM.class);

  transient protected OllamaAPI api = null;

  protected String ollamaUrl = "";

  /**
   * the most recent text prompt - used as the question when a bare image
   * arrives via {@link #onImage(ImageData)}
   */
  protected String lastPrompt = null;

  protected String currentChannel;
  protected String currentBotName;
  protected String currentChannelName;
  protected String currentChannelType;

  public VLM(String n, String id) {
    super(n, id);
  }

  /**
   * Lazily build / rebuild the ollama4j client when the configured url changes.
   */
  OllamaAPI getOllamaApi() throws MalformedURLException {
    if (api == null || ollamaUrl == null || !ollamaUrl.contentEquals(config.url)) {
      URL url = new URL(config.url);
      ollamaUrl = config.url;
      int port = url.getPort();
      if (port == -1) {
        port = "https".equals(url.getProtocol()) ? 443 : 80;
      }
      api = new OllamaAPI(String.format("%s://%s:%d", url.getProtocol(), url.getHost(), port));
      api.setRequestTimeoutSeconds(config.timeout);
    }
    return api;
  }

  Options buildOptions() {
    OptionsBuilder builder = new OptionsBuilder();
    builder.setTemperature(config.temperature);
    return builder.build();
  }

  /**
   * Build the prompt sent to the model, prepending the configured system
   * context.
   */
  String buildPrompt(String prompt) {
    if (prompt == null || prompt.trim().length() == 0) {
      prompt = config.defaultImagePrompt;
    }
    if (config.system != null && config.system.trim().length() > 0) {
      return config.system + "\n\n" + prompt;
    }
    return prompt;
  }

  // ------------------------------------------------------------------
  // image url / file based responses (ollama4j handles base64 encoding)
  // ------------------------------------------------------------------

  /**
   * Describe one or more images using the default image prompt.
   */
  public Response describeImages(List<String> imageUrls) {
    return getResponse(config.defaultImagePrompt, imageUrls);
  }

  /**
   * Ask a question about one image referenced by a path or url.
   */
  public Response getResponse(String prompt, String imageUrl) {
    List<String> urls = new ArrayList<>();
    urls.add(imageUrl);
    return getResponse(prompt, urls);
  }

  /**
   * Ask a question about one or more images referenced by path or url. Local
   * file paths and {@code file://} urls are read directly, {@code http(s)} urls
   * are downloaded by ollama4j.
   * 
   * @param prompt
   *          the question / instruction for the model
   * @param imageUrls
   *          file paths, file:// or http(s):// urls
   * @return the model response
   */
  public Response getResponse(String prompt, List<String> imageUrls) {
    try {
      if (imageUrls == null || imageUrls.isEmpty()) {
        error("no images supplied to VLM");
        return null;
      }

      List<File> files = new ArrayList<>();
      List<String> remoteUrls = new ArrayList<>();
      boolean firstImage = true;
      for (String img : imageUrls) {
        if (img == null || img.trim().length() == 0) {
          continue;
        }
        // let the ui display the image being analyzed alongside its prompt
        invoke("publishImageRequest", new ImageRequest(img, firstImage ? prompt : null));
        firstImage = false;
        if (img.startsWith("http://") || img.startsWith("https://")) {
          remoteUrls.add(img);
        } else {
          files.add(uriToFile(img));
        }
      }

      OllamaAPI ollama = getOllamaApi();
      Options options = buildOptions();
      StreamHandler handler = new StreamHandler();
      String fullPrompt = buildPrompt(prompt);

      log.info("vlm request host={} model={} files={} urls={} prompt={}", ollamaUrl, config.model, files.size(), remoteUrls.size(), preview(prompt, 120));

      String responseText = null;

      if (!files.isEmpty()) {
        OllamaResult result = ollama.generateWithImageFiles(config.model, fullPrompt, files, options, config.stream ? handler : null);
        responseText = result.getResponse();
      } else {
        OllamaResult result = ollama.generateWithImageURLs(config.model, fullPrompt, remoteUrls, options, config.stream ? handler : null);
        responseText = result.getResponse();
      }

      log.info("vlm response: {}", preview(responseText, 300));
      return publishVlmResponse(responseText, handler);

    } catch (Exception e) {
      log.error("vlm getResponse failed for prompt={}", preview(prompt, 120), e);
      error(e);
    }
    return null;
  }

  // ------------------------------------------------------------------
  // base64 based response - posts directly to ollama /api/generate
  // ------------------------------------------------------------------

  public Response getImageResponse(String base64Image) {
    return getImageResponse(base64Image, null);
  }

  /**
   * Ask a question about a base64 encoded image. Useful with
   * {@link OpenCV#getBase64Image()}.
   * 
   * @param base64Image
   *          base64 encoded image bytes (no data uri prefix)
   * @param prompt
   *          the question, defaults to the configured image prompt when null
   * @return the model response
   */
  public Response getImageResponse(String base64Image, String prompt) {
    try {
      if (base64Image == null || base64Image.trim().length() == 0) {
        error("no image supplied to VLM");
        return null;
      }

      if (prompt == null) {
        prompt = config.defaultImagePrompt;
      }

      String fullPrompt = buildPrompt(prompt);
      // let the ui display the image being analyzed alongside its prompt
      invoke("publishImageRequest", new ImageRequest("data:image/jpeg;base64," + base64Image, prompt));

      LinkedHashMap<String, Object> request = new LinkedHashMap<>();
      request.put("model", config.model);
      request.put("prompt", fullPrompt);
      List<String> images = new ArrayList<>();
      images.add(base64Image);
      request.put("images", images);
      // single consolidated json response is simplest to parse over http
      request.put("stream", false);

      String json = CodecUtils.toJson(request);

      String generateUrl = config.url.replaceAll("/+$", "") + "/api/generate";

      log.info("vlm base64 request url={} model={} prompt={}", generateUrl, config.model, preview(prompt, 120));

      String msg = postOllamaGenerate(generateUrl, json);

      if (msg == null || msg.trim().length() == 0) {
        warn("empty response from {}", generateUrl);
        return null;
      }

      Map<String, Object> payload = CodecUtils.fromJson(msg, new StaticType<Map<String, Object>>() {
      });

      if (payload != null && payload.get("response") != null) {
        String responseText = payload.get("response").toString();
        log.info("vlm base64 response: {}", preview(responseText, 300));
        return publishVlmResponse(responseText, null);
      }

      warn("unrecognized response shape from {} keys={}", generateUrl, payload == null ? "null" : payload.keySet());

    } catch (Exception e) {
      error(e);
    }
    return null;
  }

  /**
   * Grabs a single frame from an OpenCV (or any {@link ImagePublisher} that
   * exposes {@code getBase64Image}) service and asks the model about it.
   * 
   * @param cvName
   *          name of the OpenCV service to grab a frame from
   * @param prompt
   *          the question, defaults to the configured image prompt when null
   * @return the model response
   */
  public Response getResponseFromCamera(String cvName, String prompt) {
    if (cvName == null || cvName.trim().length() == 0) {
      error("no opencv service specified");
      return null;
    }
    if (Runtime.getService(cvName) == null) {
      error("service %s not found", cvName);
      return null;
    }
    Object base64 = invokeOn(cvName, "getBase64Image");
    if (base64 == null) {
      error("could not grab a frame from %s", cvName);
      return null;
    }
    return getImageResponse(base64.toString(), prompt);
  }

  /**
   * Request published so a ui can display the image being analyzed along with
   * its prompt. {@code img} is a ready to use image source - either a
   * {@code data:image/...;base64,...} uri or an {@code http(s)} url.
   */
  public static class ImageRequest {
    public String img;
    public String prompt;

    public ImageRequest(String img, String prompt) {
      this.img = img;
      this.prompt = prompt;
    }
  }

  public ImageRequest publishImageRequest(ImageRequest request) {
    return request;
  }

  /**
   * Posts a generate request to the Ollama server over http. Isolated so it can
   * be overridden in tests to avoid the external dependency.
   * 
   * @param url
   *          the {@code /api/generate} endpoint
   * @param json
   *          the request body
   * @return the raw response body
   * @throws Exception
   *           on transport failure
   */
  @SuppressWarnings("unchecked")
  protected String postOllamaGenerate(String url, String json) throws Exception {
    HttpClient<HttpClientConfig> http = (HttpClient<HttpClientConfig>) startPeer("http");
    return http.postJson(config.password, url, json);
  }

  /**
   * Publishes the model response as text, utterance and response. When a
   * streaming handler was used its partial sentences have already been
   * published, so only the trailing remainder is emitted to avoid duplicates.
   */
  Response publishVlmResponse(String responseText, StreamHandler handler) {
    if (handler != null) {
      // flush any remaining buffered text that did not end in punctuation
      String remainder = handler.sentenceBuilder.toString().trim();
      if (remainder.length() > 0) {
        publish(remainder);
      }
    } else if (responseText != null && responseText.trim().length() > 0) {
      publish(responseText);
    }
    return new Response("friend", getName(), responseText, null);
  }

  /**
   * Emit a single chunk of text as text / utterance / response.
   */
  void publish(String text) {
    if (text == null || text.trim().length() == 0) {
      return;
    }
    Utterance utterance = new Utterance();
    utterance.username = getName();
    utterance.text = text;
    utterance.isBot = true;
    utterance.channel = currentChannel;
    utterance.channelType = currentChannelType;
    utterance.channelBotName = currentBotName;
    utterance.channelName = currentChannelName;
    invoke("publishUtterance", utterance);
    invoke("publishResponse", new Response("friend", getName(), text, null));
    if (config.publishSpeech) {
      invoke("publishText", text);
    }
  }

  // ------------------------------------------------------------------
  // streaming - publish complete sentences as they arrive
  // ------------------------------------------------------------------

  public class StreamHandler implements OllamaStreamHandler {

    final StringBuilder sentenceBuilder = new StringBuilder();
    int lastProcessedLength = 0;

    @Override
    public void accept(String message) {
      final int MIN_SENTENCE_LENGTH = 5;

      // ollama4j hands us the full accumulated text each call - append only the
      // new portion
      if (message.length() < lastProcessedLength) {
        lastProcessedLength = 0;
      }
      String newText = message.substring(lastProcessedLength);
      sentenceBuilder.append(newText);
      lastProcessedLength = message.length();

      int lastPeriodIndex = sentenceBuilder.lastIndexOf(".");
      int lastQuestionIndex = sentenceBuilder.lastIndexOf("?");
      int lastExclaimIndex = sentenceBuilder.lastIndexOf("!");
      int lastSentenceEndIndex = Math.max(lastPeriodIndex, Math.max(lastQuestionIndex, lastExclaimIndex));

      if (lastSentenceEndIndex != -1) {
        String potentialSentence = sentenceBuilder.substring(0, lastSentenceEndIndex + 1).trim();
        if (potentialSentence.length() >= MIN_SENTENCE_LENGTH) {
          publish(potentialSentence);
          sentenceBuilder.delete(0, lastSentenceEndIndex + 1);
        }
      }
    }
  }

  // ------------------------------------------------------------------
  // listeners / publishers
  // ------------------------------------------------------------------

  /**
   * Receives an image from an {@link ImagePublisher} (e.g. OpenCV) and asks the
   * model about it using the last text prompt or the default image prompt.
   */
  @Override
  public void onImage(ImageData img) {
    try {
      String src = (img.src != null) ? img.src : img.source;
      if (src == null) {
        error("received image with no src");
        return;
      }
      String prompt = (lastPrompt != null) ? lastPrompt : config.defaultImagePrompt;
      List<String> urls = new ArrayList<>();
      urls.add(src);
      getResponse(prompt, urls);
    } catch (Exception e) {
      error(e);
    }
  }

  /**
   * Incoming text is remembered as the prompt to use for the next image.
   */
  @Override
  public void onText(String text) {
    lastPrompt = text;
  }

  @Override
  public void onUtterance(Utterance utterance) throws Exception {
    currentChannelType = utterance.channelType;
    currentChannel = utterance.channel;
    currentBotName = utterance.channelBotName;
    currentChannelName = utterance.channelName;
    if (utterance.isBot) {
      log.info("not responding to bots");
      return;
    }
    lastPrompt = utterance.text;
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
  public Response publishResponse(Response response) {
    return response;
  }

  public String publishRequest(String text) {
    return text;
  }

  @Override
  public Status error(String error) {
    Status status = super.error(error);
    invoke("publishText", error);
    Utterance utterance = new Utterance();
    utterance.text = error;
    invoke("publishUtterance", utterance);
    invoke("publishResponse", new Response("friend", getName(), error, null));
    return status;
  }

  public void setModel(String model) {
    config.model = model;
  }

  public void setToken(String password) {
    config.password = password;
  }

  @Override
  public void attach(Attachable attachable) {
    if (attachable instanceof ImagePublisher) {
      attachImagePublisher((ImagePublisher) attachable);
    } else if (attachable instanceof TextPublisher) {
      attachTextPublisher((TextPublisher) attachable);
    } else if (attachable instanceof TextListener) {
      addListener("publishText", attachable.getName(), "onText");
    } else if (attachable instanceof UtteranceListener) {
      attachUtteranceListener(attachable.getName());
    } else {
      log.error("don't know how to attach a {}", attachable.getName());
    }
  }

  // ------------------------------------------------------------------
  // helpers
  // ------------------------------------------------------------------

  static String preview(String text, int maxLen) {
    if (text == null) {
      return "null";
    }
    if (text.length() <= maxLen) {
      return text;
    }
    return text.substring(0, maxLen) + "... (" + text.length() + " chars total)";
  }

  static File uriToFile(String pathOrUri) throws URISyntaxException {
    if (pathOrUri.startsWith("file:")) {
      return new File(URI.create(pathOrUri));
    }
    Path path = Paths.get(pathOrUri);
    return path.toFile();
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);

      WebGui webgui = (WebGui)Runtime.start("webui", "WebGui");
      OpenCV cv = (OpenCV)Runtime.start("opencv", "OpenCV");
      VLM vlm = (VLM) Runtime.start("vlm", "VLM");
      
      // requires a local ollama server with a vision model:
      // ollama pull llava
      vlm.config.url = "http://localhost:11434";
      vlm.config.model = "llava";

      Response response = vlm.getResponse("What is in this image?", "https://ollama.com/public/blog/embedding-models.png");
      if (response != null) {
        System.out.println(response.msg);
      }

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
