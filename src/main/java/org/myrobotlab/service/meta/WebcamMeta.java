package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class WebcamMeta {
  public final static Logger log = LoggerFactory.getLogger(WebcamMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Webcam");
    Platform platform = Platform.getLocalInstance();
    // setAutoOpenMode(true);
    
    meta.addDescription("used as a general webcam");
    meta.addCategory("video");
    return meta;
  }
  
  
}

