package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class OculusDiyMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(OculusDiyMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.OculusDiy");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("Service to receive and compute data from a DIY Oculus");
    meta.addCategory("video", "control", "sensors", "telerobotics");
    meta.addPeer("arduino", "Arduino", "Arduino for DIYOculus and Myo");
    meta.addPeer("mpu6050", "Mpu6050", "mpu6050");
    return meta;
  }

  
}

