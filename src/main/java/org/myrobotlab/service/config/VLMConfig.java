package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

/**
 * Configuration for the {@link org.myrobotlab.service.VLM} Vision Language
 * Model service.
 * 
 * Defaults target a local, offline Ollama server running an open source vision
 * model (LLaVA). To use it:
 * 
 * <pre>
 *   ollama pull llava
 *   ollama serve
 * </pre>
 * 
 * Other open source vision models that can be run offline through Ollama and
 * dropped into {@link #model} include {@code llava:13b}, {@code bakllava},
 * {@code llama3.2-vision}, {@code moondream}, {@code minicpm-v} and
 * {@code qwen2.5vl}.
 */
public class VLMConfig extends ServiceConfig {

  /**
   * base url of the Ollama server - offline by default
   */
  public String url = "http://localhost:11434";

  /**
   * open source vision capable model to run
   */
  public String model = "llava";

  /**
   * optional bearer token / password for secured endpoints (not needed for a
   * local Ollama install)
   */
  public String password = null;

  /**
   * prompt used when an image arrives without an accompanying question
   */
  public String defaultImagePrompt = "Describe this image in detail.";

  /**
   * system prompt prepended to give the model context / persona
   */
  public String system = "You are a helpful vision assistant. Describe what you see clearly and concisely.";

  public float temperature = 0.7f;

  public int maxTokens = 512;

  /**
   * stream partial sentences as they are generated
   */
  public boolean stream = true;

  /**
   * when false, responses are still published as utterances/responses but
   * {@code publishText} is suppressed so speech synthesis is not triggered
   */
  public boolean publishSpeech = true;

  /**
   * client side timeout (seconds) waiting for a response - vision models can be
   * slow on CPU
   */
  public int timeout = 120;

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    // http peer used for the base64 /api/generate path
    addDefaultPeerConfig(plan, name, "http", "HttpClient");
    return plan;
  }

}
