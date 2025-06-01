package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WebkitSpeechRecognitionMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WebkitSpeechRecognitionMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public WebkitSpeechRecognitionMeta() {
    addDescription("Speech recognition using Google Chrome webkit");
    addCategory("speech recognition");

  }

}
