package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TrackingMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(TrackingMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public TrackingMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("uses a video input and vision library to visually track objects");
    addCategory("vision", "video", "sensors", "control");
    addPeer("pid", "Pid", "Pid service - for all your pid needs");
    addPeer("opencv", "OpenCV", "Tracking OpenCV instance");

  }

}
