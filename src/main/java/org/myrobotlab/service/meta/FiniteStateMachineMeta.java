package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class FiniteStateMachineMeta {
  public final static Logger log = LoggerFactory.getLogger(FiniteStateMachineMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.FiniteStateMachine");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("general service which can create and maintaine multiple finite state machines");
    // meta.addDependency("orgId", "artifactId", "2.4.0");
    meta.addCategory("general", "ai");
    return meta;
  }
  
  
}

