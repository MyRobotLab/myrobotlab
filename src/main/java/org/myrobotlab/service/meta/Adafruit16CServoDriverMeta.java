package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Adafruit16CServoDriverMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(Adafruit16CServoDriverMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Adafruit16CServoDriver");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("controls 16 pwm pins for 16 servos/LED or 8 motors");
    meta.addCategory("shield", "servo", "pwm");
    meta.setSponsor("Mats");
    // meta.addDependency("com.pi4j.pi4j", "1.1-SNAPSHOT");
    /*
     * meta.addPeer("arduino", "Arduino", "our Arduino"); meta.addPeer("raspi",
     * "RasPi", "our RasPi");
     */
    return meta;
  }
  
  
}

