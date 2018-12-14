package org.myrobotlab.inmoov;

import java.util.Arrays;
import java.util.List;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.InMoov;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

/**
 * InMoov extra methods for SpeechRecognition ( also used by UI later)
 */
public class SpeechRecognition {
  public Boolean earEnabled;
  String earEngine;
  String lockPhrase;
  transient public InMoov instance;
  public final static Logger log = LoggerFactory.getLogger(SpeechRecognition.class);
  transient public List<String> earEngines = Utils.getServicesFromCategory("speech recognition");

  /**
   * Init default config parameters
   */
  public void init() {
    setEarEnabled(true);
    setLockPhrase("wake up");
  }

  public void setEarEnabled(boolean enable) {
    earEnabled = enable;
  }

  public String getEarEngine() {
    if (this.earEngine == null) {
      setEarEngine("WebkitSpeechRecognition");
    }
    return earEngine;
  }

  public void setEarEngine(String earEngine) {
    if (!earEngines.contains(earEngine)) {
      log.error("Sorry, {} is an unknown ear service. {}", earEngine);
      return;
    }
    this.earEngine = earEngine;
    log.info("Set InMoov ear engine : %s", earEngine);
    instance.broadcastState();
  }

  public String getLockPhrase() {
    return lockPhrase;
  }

  public void setLockPhrase(String lockPhrase) {
    this.lockPhrase = lockPhrase;
    log.info("Set lockPhrase : %s", lockPhrase);
  }

}