package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class WiiDarMeta {
  public final static Logger log = LoggerFactory.getLogger(WiiDarMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.WiiDar");
    Platform platform = Platform.getLocalInstance();
    // meta.addDependency("wiiuse.wiimote", "0.12b");

    meta.addDependency("wiiusej", "wiiusej", "wiiusej");
    meta.addCategory("sensors");
    // no longer have hardware for this ...
    meta.setAvailable(false);
    return meta;
  }
  
}

