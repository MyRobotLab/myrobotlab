package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class MobilePlatformMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(MobilePlatformMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.MobilePlatform");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription(
        "used to encapsulate many of the functions and formulas regarding 2 motor platforms encoders and other feedback mechanisms can be added to provide heading, location and other information");
    meta.addCategory("robot", "control");
    meta.setAvailable(false);

    return meta;
  }
  
  
}

