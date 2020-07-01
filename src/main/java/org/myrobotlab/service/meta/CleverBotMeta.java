package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class CleverBotMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(CleverBotMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.CleverBot");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("chatbot service");
    meta.addCategory("ai");
    meta.addDependency("ca.pjer", "chatter-bot-api", "2.0.1");
    meta.addDependency("com.squareup.okhttp3", "okhttp", "3.9.0");
    meta.setCloudService(true);
    return meta;
  }
  
}

