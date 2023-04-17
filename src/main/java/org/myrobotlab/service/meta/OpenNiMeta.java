package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class OpenNiMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenNiMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public OpenNiMeta() {

    addDescription("OpenNI Service - 3D sensor");
    addCategory("video", "vision", "sensors", "telerobotics");
    // addDependency("com.googlecode.simpleopenni", "1.96");
    addDependency("simpleopenni", "openni", "1.96");
    addDependency("org.myrobotlab.openni", "openni-deps", "0.1", "zip");
    addDependency("javax.vecmath", "vecmath", "1.5.2");
    addDependency("java3d", "j3d-core", "1.3.1");
    addDependency("java3d", "j3d-core-utils", "1.3.1");

  }

}
