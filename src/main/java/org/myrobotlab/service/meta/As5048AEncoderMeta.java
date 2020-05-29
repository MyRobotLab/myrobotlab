package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class As5048AEncoderMeta {
  public final static Logger log = LoggerFactory.getLogger(As5048AEncoderMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.As5048AEncoder");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("AS5048A Encoder - 14 bit - Absolute position encoder");
    meta.addCategory("encoder", "sensors");
    return meta;
  }
  
}

