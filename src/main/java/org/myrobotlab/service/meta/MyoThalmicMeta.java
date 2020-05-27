package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class MyoThalmicMeta {
  public final static Logger log = LoggerFactory.getLogger(MyoThalmicMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.MyoThalmic");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("Myo service to control with the Myo armband");
    meta.addCategory("control", "sensors");

    meta.addDependency("com.github.nicholasastuart", "myo-java", "0.9.1");
    return meta;
  }

  
  
}

