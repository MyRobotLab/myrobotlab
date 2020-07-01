package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Mpr121Meta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(Mpr121Meta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Mpr121");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("MPR121 Touch sensor & LED Driver");
    meta.addCategory("shield", "sensors", "i2c");
    meta.setSponsor("Mats");
    meta.setAvailable(false);
    return meta;
  }

  
}

