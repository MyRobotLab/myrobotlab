package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class OakDMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OakDMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public OakDMeta() {

    addDescription("OAK-D stereo depth camera — voxel cloud, RGB mesh, and HUD overlay");
    setAvailable(true);
    addCategory("video", "vision", "sensors");

  }

}
