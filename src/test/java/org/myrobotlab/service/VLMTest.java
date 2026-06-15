package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.myrobotlab.programab.Response;
import org.myrobotlab.service.config.VLMConfig;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.OllamaResult;
import io.github.ollama4j.models.generate.OllamaStreamHandler;
import io.github.ollama4j.utils.Options;

/**
 * Unit tests for the {@link VLM} Vision Language Model service.
 * 
 * The Ollama server is mocked so these tests run completely offline with no
 * external dependency:
 * <ul>
 * <li>the image url / file path is intercepted by overriding
 * {@link VLM#getOllamaApi()} with a fake {@link OllamaAPI}</li>
 * <li>the base64 path is intercepted by overriding
 * {@link VLM#postOllamaGenerate(String, String)}</li>
 * </ul>
 */
public class VLMTest {

  /**
   * Fake Ollama client that returns a canned result instead of contacting a
   * server. It also exercises the streaming handler when one is supplied.
   */
  static class FakeOllamaAPI extends OllamaAPI {

    final String cannedResponse;
    String lastModel;
    String lastPrompt;
    List<String> lastUrls;
    List<File> lastFiles;

    FakeOllamaAPI(String cannedResponse) {
      super("http://localhost:11434");
      this.cannedResponse = cannedResponse;
    }

    @Override
    public OllamaResult generateWithImageURLs(String model, String prompt, List<String> imageURLs, Options options, OllamaStreamHandler streamHandler) {
      this.lastModel = model;
      this.lastPrompt = prompt;
      this.lastUrls = imageURLs;
      if (streamHandler != null) {
        streamHandler.accept(cannedResponse);
      }
      return new OllamaResult(cannedResponse, 1L, 200);
    }

    @Override
    public OllamaResult generateWithImageFiles(String model, String prompt, List<File> imageFiles, Options options, OllamaStreamHandler streamHandler) {
      this.lastModel = model;
      this.lastPrompt = prompt;
      this.lastFiles = imageFiles;
      if (streamHandler != null) {
        streamHandler.accept(cannedResponse);
      }
      return new OllamaResult(cannedResponse, 1L, 200);
    }
  }

  /**
   * VLM under test with the Ollama dependency mocked out.
   */
  static class TestVLM extends VLM {
    private static final long serialVersionUID = 1L;

    transient FakeOllamaAPI fake;
    String base64Response;
    String lastPostedJson;

    TestVLM(String n, FakeOllamaAPI fake) {
      super(n, n);
      this.fake = fake;
    }

    @Override
    OllamaAPI getOllamaApi() {
      return fake;
    }

    @Override
    protected String postOllamaGenerate(String url, String json) {
      this.lastPostedJson = json;
      return base64Response;
    }
  }

  static TestVLM newVlm(String cannedResponse) {
    FakeOllamaAPI fake = new FakeOllamaAPI(cannedResponse);
    TestVLM vlm = new TestVLM("vlm", fake);
    vlm.apply(new VLMConfig());
    return vlm;
  }

  @Test
  public void testGetResponseWithImageUrl() {
    TestVLM vlm = newVlm("A cat sitting on a mat.");

    Response response = vlm.getResponse("What is in this image?", "http://example.com/cat.jpg");

    assertNotNull(response);
    assertEquals("A cat sitting on a mat.", response.msg);
    assertEquals("llava", vlm.fake.lastModel);
    assertEquals(1, vlm.fake.lastUrls.size());
    assertEquals("http://example.com/cat.jpg", vlm.fake.lastUrls.get(0));
    // the configured system prompt is prepended to the user prompt
    assertTrue(vlm.fake.lastPrompt.contains("What is in this image?"));
    assertTrue(vlm.fake.lastPrompt.contains(vlm.getConfig().system));
  }

  @Test
  public void testGetResponseWithImageFile() {
    TestVLM vlm = newVlm("A dog in a park.");

    Response response = vlm.getResponse("Describe this.", "some/local/path/dog.jpg");

    assertNotNull(response);
    assertEquals("A dog in a park.", response.msg);
    // a non-url path is routed to the image-file overload
    assertNotNull(vlm.fake.lastFiles);
    assertEquals(1, vlm.fake.lastFiles.size());
  }

  @Test
  public void testDescribeImagesUsesDefaultPrompt() {
    TestVLM vlm = newVlm("A sunset over the ocean.");

    Response response = vlm.describeImages(java.util.Arrays.asList("http://example.com/sunset.jpg"));

    assertNotNull(response);
    assertEquals("A sunset over the ocean.", response.msg);
    assertTrue(vlm.fake.lastPrompt.contains(vlm.getConfig().defaultImagePrompt));
  }

  @Test
  public void testStreamingReturnsFullResponse() {
    TestVLM vlm = newVlm("The first sentence. The second sentence.");
    vlm.getConfig().stream = true;

    Response response = vlm.getResponse("What is happening?", "http://example.com/scene.jpg");

    assertNotNull(response);
    assertEquals("The first sentence. The second sentence.", response.msg);
    // streaming handler should have been invoked with the canned text
    assertNotNull(vlm.fake.lastUrls);
  }

  @Test
  public void testGetImageResponseBase64() {
    TestVLM vlm = newVlm("unused");
    vlm.base64Response = "{\"response\":\"A red sports car.\",\"done\":true}";

    Response response = vlm.getImageResponse("aGVsbG8=", "What car is this?");

    assertNotNull(response);
    assertEquals("A red sports car.", response.msg);
    // the request body should carry the model and the base64 image
    assertTrue(vlm.lastPostedJson.contains("aGVsbG8="));
    assertTrue(vlm.lastPostedJson.contains("llava"));
  }

  @Test
  public void testGetImageResponseEmptyImage() {
    TestVLM vlm = newVlm("unused");
    Response response = vlm.getImageResponse(null, "anything");
    // no http call is made and no model response is produced
    assertNull(vlm.lastPostedJson);
    assertNull(response);
  }

  @Test
  public void testGetResponseNoImagesReturnsNull() {
    TestVLM vlm = newVlm("unused");
    Response response = vlm.getResponse("hello", (List<String>) null);
    assertNull(response);
  }

  @Test
  public void testBuildPromptPrependsSystem() {
    TestVLM vlm = newVlm("unused");
    String prompt = vlm.buildPrompt("What is this?");
    assertTrue(prompt.startsWith(vlm.getConfig().system));
    assertTrue(prompt.endsWith("What is this?"));
  }

  @Test
  public void testBuildPromptFallsBackToDefault() {
    TestVLM vlm = newVlm("unused");
    String prompt = vlm.buildPrompt(null);
    assertTrue(prompt.contains(vlm.getConfig().defaultImagePrompt));
  }
}
