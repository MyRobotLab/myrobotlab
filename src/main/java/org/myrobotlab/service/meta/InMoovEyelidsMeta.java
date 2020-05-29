package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class InMoovEyelidsMeta {
  public final static Logger log = LoggerFactory.getLogger(InMoovEyelidsMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.InMoovEyelids");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("InMoov Eyelids");
    meta.addCategory("robot");

    meta.addPeer("eyelidleft", "Servo", "eyelidleft or both servo");
    meta.addPeer("eyelidright", "Servo", "Eyelid right servo");

    return meta;
  }
  
}

