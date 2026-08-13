package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

/**
 * Configuration for offline Vosk speech recognition.
 */
public class VoskSpeechRecognitionConfig extends SpeechRecognizerConfig {

  public VoskSpeechRecognitionConfig() {
    afterSpeakingPauseMs = 500;
  }

  @Override
  public Plan getDefault(Plan plan, String name) {
    afterSpeakingPauseMs = 500;
    return super.getDefault(plan, name);
  }

  /**
   * Vosk model directory name, e.g. {@code vosk-model-small-en-us-0.15}.
   * When null, the service picks a default small model for the active locale.
   */
  public String model = "vosk-model-small-en-us-0.15";

  /**
   * Optional absolute/relative path override for an already-extracted model
   * directory. When set, {@link #model} is ignored for loading.
   */
  public String modelPath = null;

  /**
   * Microphone / recognizer sample rate in Hz. Vosk models expect 16 kHz.
   */
  public float sampleRate = 16000.0f;

  /**
   * When true, missing models are downloaded from {@link #modelBaseUrl} on
   * first use.
   */
  public boolean autoDownloadModel = true;

  /**
   * Base URL for official Vosk model zip archives (no trailing slash).
   */
  public String modelBaseUrl = "https://alphacephei.com/vosk/models";

  /**
   * Publish interim / partial transcripts as listening events.
   */
  public boolean publishPartial = false;

}
