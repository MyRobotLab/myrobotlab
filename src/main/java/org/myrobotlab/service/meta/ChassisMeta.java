package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class ChassisMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(ChassisMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Chassis");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("control platform");
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("general");
    meta.addPeer("left", "Motor", "left drive motor");
    meta.addPeer("right", "Motor", "right drive motor");
    meta.addPeer("joystick", "Joystick", "joystick control");
    meta.addPeer("controller", "Sabertooth", "serial controller");
    return meta;
  }

  
}

