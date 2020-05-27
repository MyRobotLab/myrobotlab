package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class CronMeta {
  public final static Logger log = LoggerFactory.getLogger(CronMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Cron");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("is a cron like service capable of scheduling future actions using cron syntax");
    meta.addCategory("scheduling");
    meta.addDependency("it.sauronsoftware.cron4j", "cron4j", "2.2.5");
    return meta;
  }
  
}

