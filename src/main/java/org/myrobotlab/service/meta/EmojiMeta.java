package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class EmojiMeta {
  public final static Logger log = LoggerFactory.getLogger(EmojiMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Emoji");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("used as a general template");

    meta.addPeer("display", "ImageDisplay", "image display");
    meta.addPeer("http", "HttpClient", "downloader");
    meta.addPeer("fsm", "FiniteStateMachine", "emotional state machine");

    // meta.setAvailable(false);
    meta.addCategory("general");
    return meta;
  }

  
}

