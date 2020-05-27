package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class InMoovArmMeta {
  public final static Logger log = LoggerFactory.getLogger(InMoovArmMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.InMoovArm");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("the InMoov Arm Service");
    meta.addCategory("robot");

    meta.addPeer("bicep", "Servo", "Bicep servo");
    meta.addPeer("rotate", "Servo", "Rotate servo");
    meta.addPeer("shoulder", "Servo", "Shoulder servo");
    meta.addPeer("omoplate", "Servo", "Omoplate servo");
    meta.addPeer("arduino", "Arduino", "Arduino controller for this arm");

    return meta;
  }

  
  
}

