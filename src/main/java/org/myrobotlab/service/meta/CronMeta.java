package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class CronMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(CronMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Cron");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("is a cron like service capable of scheduling future actions using cron syntax");
    meta.addCategory("scheduling");
    meta.addDependency("it.sauronsoftware.cron4j", "cron4j", "2.2.5");
    return meta;
  }
  
}

