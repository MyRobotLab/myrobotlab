package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class PidMeta {
  public final static Logger log = LoggerFactory.getLogger(PidMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Pid");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("A proportional integral derivative controller (Pid controller) commonly used in industrial control systems");
    meta.addCategory("control", "industrial");
    return meta;
  }

  
}

