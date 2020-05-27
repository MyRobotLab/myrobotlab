package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class GoogleSearchMeta {
  public final static Logger log = LoggerFactory.getLogger(GoogleSearchMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.GoogleSearch");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("used as a general google search");
    meta.addDependency("org.jsoup", "jsoup", "1.8.3");
    meta.addCategory("search");
    return meta;
  }
  
  
}

