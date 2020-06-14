package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class VirtualDeviceMeta {
  public final static Logger log = LoggerFactory.getLogger(VirtualDeviceMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.VirtualDevice");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("a service which can create virtual serial ports and behaviors implemented in python for them");
    meta.addCategory("testing");
    // put peer definitions in
    meta.addPeer("uart", "Serial", "uart");
    meta.addPeer("logic", "Python", "logic to implement");

    // this is used for testing, and does not need to be tested
    meta.setAvailable(false);

    return meta;
  }
  
}

