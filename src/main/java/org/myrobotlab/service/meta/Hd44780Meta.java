package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Hd44780Meta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(Hd44780Meta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Hd44780");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("I2C LCD Display driver");
    meta.addCategory("i2c", "display");
    return meta;
  }

  
  
}

