package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class InMoov2HandMeta {
  public final static Logger log = LoggerFactory.getLogger(InMoov2HandMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.InMoov2Hand");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("an easier way to create gestures for InMoov");
    meta.addCategory("robot");

    meta.addPeer("thumb", "Servo", "Thumb servo");
    meta.addPeer("index", "Servo", "Index servo");
    meta.addPeer("majeure", "Servo", "Majeure servo");
    meta.addPeer("ringFinger", "Servo", "RingFinger servo");
    meta.addPeer("pinky", "Servo", "Pinky servo");
    meta.addPeer("wrist", "Servo", "Wrist servo");
    meta.addPeer("arduino", "Arduino", "Arduino controller for this hand");
    meta.addPeer("leap", "LeapMotion", "Leap Motion Service");

    return meta;
  }
  
}

