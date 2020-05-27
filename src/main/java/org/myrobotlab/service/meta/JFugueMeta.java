package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class JFugueMeta {
  public final static Logger log = LoggerFactory.getLogger(JFugueMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.JFugue");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("service wrapping Jfugue, used for music and sound generation");
    meta.addCategory("sound","music");
    // org="jfugue" name="jfugue" rev="5.0.7
    meta.addDependency("jfugue", "jfugue", "5.0.7");
    return meta;
  }

  
}

