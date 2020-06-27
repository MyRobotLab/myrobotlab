package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TopCodesMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(TopCodesMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.TopCodes");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("Topcodes finds visual references and identifiers");
    meta.addCategory("vision", "video", "sensors");
    meta.addDependency("topcodes", "topcodes", "1.0.0");
    return meta;
  }
  
}

