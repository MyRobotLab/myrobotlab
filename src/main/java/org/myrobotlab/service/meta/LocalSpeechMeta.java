package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.AbstractSpeechSynthesisMeta;
import org.slf4j.Logger;

public class LocalSpeechMeta extends AbstractSpeechSynthesisMeta {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(LocalSpeechMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public LocalSpeechMeta(String name) {

    super(name);
    addCategory("speech", "sound");
    addDescription("Local OS text to speech ( tts.exe / say etc ... )");
    setAvailable(true);
    addCategory("speech");
    // addDependency("com.microsoft", "tts", "1.1", "zip");
    // addDependency("mycroftai.mimic", "mimic_win64", "1.0", "zip");

  }

}
