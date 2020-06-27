package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class MotorPortMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(MotorPortMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.MotorPort");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("supports port related motor controllers such as the Sabertooth and AdaFruitMotorShield");
    meta.addCategory("motor");
    meta.setAvailable(true);
    return meta;
  }
  
  
}

