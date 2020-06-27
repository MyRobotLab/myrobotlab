package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class AdafruitMotorHat4PiMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(AdafruitMotorHat4PiMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.AdafruitMotorHat4Pi");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("description of service");
    meta.addCategory("general","unknown");
    // meta.addDependency("net.java.jinput", "jinput", "2.0.7");
    
    return meta;
  }
  
  
}

