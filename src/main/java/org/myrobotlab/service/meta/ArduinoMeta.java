package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class ArduinoMeta {
  public final static Logger log = LoggerFactory.getLogger(ArduinoMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Arduino");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("controls an Arduino microcontroller as a slave, which allows control of all the devices the Arduino is attached to, such as servos, motors and sensors");
    meta.addCategory("microcontroller");
    meta.addPeer("serial", "Serial", "serial device for this Arduino");
    return meta;
  }
  
  
}

