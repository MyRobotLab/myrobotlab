package org.myrobotlab.inmoov;

import java.util.List;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.InMoov;
import org.slf4j.Logger;

/**
 * InMoov extra methods for SpeechRecognition ( also used by UI later)
 */
public class SpeechSynthesizer {
  public Boolean speechEnabled;
  String speechEngine;
  transient public InMoov instance;
  public final static Logger log = LoggerFactory.getLogger(SpeechSynthesizer.class);
  transient public List<String> speechEngines = Utils.getServicesFromCategory("speech");

  /**
   * Init default config parameters
   */
  public void init() {
    setSpeechEnabled(true);
  }

  public void setSpeechEnabled(boolean enable) {
    speechEnabled = enable;
  }

  public String getSpeechEngine() {
    if (this.speechEngine == null) {
      setSpeechEngine("MarySpeech");
    }
    return speechEngine;
  }

  public void setSpeechEngine(String speechEngine) {
    if (!speechEngine.contains(speechEngine)) {
      log.error("Sorry, {} is an unknown speech service. {}", speechEngine);
      return;
    }
    this.speechEngine = speechEngine;
    log.info("Set InMoov speech engine : %s", speechEngine);
    instance.broadcastState();
  }

}