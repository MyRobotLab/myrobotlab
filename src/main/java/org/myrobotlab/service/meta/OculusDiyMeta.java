package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class OculusDiyMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OculusDiyMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public OculusDiyMeta() {

    Platform platform = Platform.getLocalInstance();
    addDescription("Service to receive and compute data from a DIY Oculus");
    addCategory("video", "control", "sensors", "telerobotics");
    addPeer("arduino", "Arduino", "Arduino for DIYOculus and Myo");
    addPeer("mpu6050", "Mpu6050", "mpu6050");

  }

}
