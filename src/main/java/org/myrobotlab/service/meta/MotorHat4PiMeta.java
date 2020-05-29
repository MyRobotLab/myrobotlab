package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class MotorHat4PiMeta {
  public final static Logger log = LoggerFactory.getLogger(MotorHat4PiMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.MotorHat4Pi");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("Motor service for the Raspi Motor HAT");
    meta.addCategory("motor");
    meta.addPeer("hat", "AdafruitMotorHat4Pi", "Motor HAT");
    meta.setAvailable(true);

    return meta;
  }
  
}

