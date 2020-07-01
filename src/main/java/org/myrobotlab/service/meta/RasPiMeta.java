package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class RasPiMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(RasPiMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.RasPi");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("Raspberry Pi service used for accessing specific RasPi hardware like th GPIO pins and i2c");
    meta.addCategory("i2c", "control");
    meta.setSponsor("Mats");
    meta.addDependency("com.pi4j", "pi4j-core", "1.2");
    meta.addDependency("com.pi4j", "pi4j-native", "1.2", "pom");
    return meta;
  }

}

