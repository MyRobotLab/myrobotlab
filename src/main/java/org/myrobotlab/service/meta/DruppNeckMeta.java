package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class DruppNeckMeta {
  public final static Logger log = LoggerFactory.getLogger(DruppNeckMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.DruppNeck");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("InMoov Drupp Neck Service");
    meta.addCategory("robot");

    meta.addPeer("up", "Servo", "Up servo");
    meta.addPeer("middle", "Servo", "Middle servo");
    meta.addPeer("down", "Servo", "Down servo");

    meta.setAvailable(true);

    return meta;
  }

  
}

