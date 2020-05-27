package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class AdafruitMotorHat4PiMeta {
  public final static Logger log = LoggerFactory.getLogger(AdafruitMotorHat4PiMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.AdafruitMotorHat4Pi");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("description of service");
    meta.addCategory("general","unknown");
    // meta.addDependency("net.java.jinput", "jinput", "2.0.7");
    
    return meta;
  }
  
  
}

