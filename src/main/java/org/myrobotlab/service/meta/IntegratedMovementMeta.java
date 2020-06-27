package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class IntegratedMovementMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(IntegratedMovementMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.IntegratedMovement");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("a 3D kinematics service supporting D-H parameters");
    meta.addCategory("robot", "control");
    meta.addPeer("openni", "OpenNi", "Kinect service");
    meta.addDependency("inmoov.fr", "inmoov", "1.1.22", "zip");
    meta.addDependency("inmoov.fr", "jm3-model", "1.0.0", "zip");
    meta.setAvailable(true);
    return meta;
  }
  
}

