package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class AudioCaptureMeta {
  public final static Logger log = LoggerFactory.getLogger(AudioCaptureMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.AudioCapture");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("captures and stores audio from microphone");
    meta.addCategory("sound");
    return meta;
  }

  
}

