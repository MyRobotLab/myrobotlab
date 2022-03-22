package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class CronMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(CronMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @param name
   *          n
   * 
   */
  public CronMeta() {

    addDescription("is a cron like service capable of scheduling future actions using cron syntax");
    addCategory("scheduling");
    addDependency("it.sauronsoftware.cron4j", "cron4j", "2.2.5");
  }

}
