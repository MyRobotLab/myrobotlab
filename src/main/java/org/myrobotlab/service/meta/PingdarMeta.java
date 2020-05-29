package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class PingdarMeta {
  public final static Logger log = LoggerFactory.getLogger(PingdarMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Pingdar");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("used as a ultra sonic radar");
    meta.addCategory("sensors", "display");
    // put peer definitions in
    meta.addPeer("controller", "Arduino", "controller for servo and sensor");
    meta.addPeer("sensors", "UltrasonicSensor", "sensors");
    meta.addPeer("servo", "Servo", "servo");

    meta.sharePeer("sensor.controller", "controller", "Arduino", "shared arduino");
    // theoretically - Servo should follow the same share config
    // meta.sharePeer("servo.controller", "controller", "Arduino", "shared
    // arduino");

    return meta;
  }
  
  
}

