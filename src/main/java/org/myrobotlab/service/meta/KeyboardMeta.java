package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class KeyboardMeta {
  public final static Logger log = LoggerFactory.getLogger(KeyboardMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Keyboard");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("keyboard interface");
    meta.addCategory("control");

    meta.addDependency("com.1stleg", "jnativehook", "2.0.3");

    return meta;
  }

  
}

