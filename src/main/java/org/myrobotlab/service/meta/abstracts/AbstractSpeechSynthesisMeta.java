package org.myrobotlab.service.meta.abstracts;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class AbstractSpeechSynthesisMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(AbstractSpeechSynthesisMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public AbstractSpeechSynthesisMeta() {

    addPeer("audioFile", "AudioFile", "audioFile");
    addCategory("speech");

    addDependency("org.myrobotlab.audio", "voice-effects", "1.0", "zip");

  }

}
