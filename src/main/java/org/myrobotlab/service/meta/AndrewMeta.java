package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class AndrewMeta {
  public final static Logger log = LoggerFactory.getLogger(AndrewMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Andrew");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("service for the Andrew robot");
    meta.addCategory("robot");

    return meta;
  }
  
  
}

