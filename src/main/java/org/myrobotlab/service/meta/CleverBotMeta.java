package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class CleverBotMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(CleverBotMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * 
   * 
   */
  public CleverBotMeta() {

    Platform platform = Platform.getLocalInstance();
    addDescription("chatbot service");
    addCategory("ai");
    addDependency("ca.pjer", "chatter-bot-api", "2.0.1");
    addDependency("com.squareup.okhttp3", "okhttp", "3.9.0");
    setCloudService(true);

  }

}
