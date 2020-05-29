package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class TwitterMeta {
  public final static Logger log = LoggerFactory.getLogger(TwitterMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Twitter");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("Service which can relay tweets");
    meta.addCategory("cloud");

    meta.addDependency("org.twitter4j", "twitter4j-core", "3.0.5");
    meta.setCloudService(true);
    return meta;
  }
  
}

