package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Mpr121Meta {
  public final static Logger log = LoggerFactory.getLogger(Mpr121Meta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Mpr121");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("MPR121 Touch sensor & LED Driver");
    meta.addCategory("shield", "sensors", "i2c");
    meta.setSponsor("Mats");
    meta.setAvailable(false);
    return meta;
  }

  
}

