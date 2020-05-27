package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OscMeta {
  public final static Logger log = LoggerFactory.getLogger(OscMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Osc");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("Service for the Open Sound Control using the JavaOsc library");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    meta.setLink("http://www.illposed.com/software/javaosc.html");
    // add dependency if necessary
    meta.addDependency("com.illposed.osc", "javaosc-core", "0.4");
    meta.addCategory("network", "music");
    return meta;
  }

  
}

