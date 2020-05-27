package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class ArmMeta {
  public final static Logger log = LoggerFactory.getLogger(ArmMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Arm");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("robot arm service");
    meta.addCategory("robot");
    meta.setLicenseApache();
    meta.addTodo("add IK interfacing points");
    // FIXME - add IK & DH Parameters
    // not ready for primetime - nothing implemented
    meta.setAvailable(false);
    return meta;
  }
  
  
}

