package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class IntegratedMovementMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(IntegratedMovementMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public IntegratedMovementMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("a 3D kinematics service supporting D-H parameters");
    addCategory("robot", "control");
    addPeer("openni", "OpenNi", "Kinect service");
    addDependency("inmoov.fr", "inmoov", null, "zip");
    addDependency("inmoov.fr", "jm3-model", "1.0.0", "zip");
    setAvailable(true);

  }

}
