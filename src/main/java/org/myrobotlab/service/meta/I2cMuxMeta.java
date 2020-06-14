package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class I2cMuxMeta {
  public final static Logger log = LoggerFactory.getLogger(I2cMuxMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.I2cMux");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("Multiplexer for i2c to be able to use multiple i2c devices");
    meta.addCategory("i2c", "control");
    meta.setAvailable(true);
    meta.setSponsor("Mats");
    return meta;
  }
  
}

