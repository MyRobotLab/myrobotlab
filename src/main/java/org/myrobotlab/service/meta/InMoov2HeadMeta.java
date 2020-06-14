package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class InMoov2HeadMeta {
  public final static Logger log = LoggerFactory.getLogger(InMoov2HeadMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.InMoov2Head");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("The inmoov2 head");
    meta.addPeer("jaw", "Servo", "Jaw servo");
    meta.addPeer("eyeX", "Servo", "Eyes pan servo");
    meta.addPeer("eyeY", "Servo", "Eyes tilt servo");
    meta.addPeer("rothead", "Servo", "Head pan servo");
    meta.addPeer("neck", "Servo", "Head tilt servo");
    meta.addPeer("rollNeck", "Servo", "rollNeck Mod servo");
    // meta.addPeer("arduino", "Arduino", "Arduino controller for this arm");

    meta.addPeer("eyelidLeft", "Servo", "eyelidLeft or both servo");
    meta.addPeer("eyelidRight", "Servo", "Eyelid right servo");
    
    return meta;
  }
  
  
}

