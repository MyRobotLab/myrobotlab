package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class TarsosDspMeta {
  public final static Logger log = LoggerFactory.getLogger(TarsosDspMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.TarsosDsp");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("digital signal processing - used for audio effects, although it could have many other uses");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("general");
    return meta;
  }

  
  
}

