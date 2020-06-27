package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Lm75aMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(Lm75aMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Lm75a");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("LM75A Digital temperature sensor");
    meta.addCategory("shield", "sensors", "i2c");
    meta.setSponsor("Mats");
    return meta;
  }
  
}

