package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class IpCameraMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(IpCameraMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public IpCameraMeta() {
    addDescription("control and video stream capture for generic ip camera");
    addCategory("video");
    // FIXME - should be webcam dependency not opencv !
    // addDependency("org.bytedeco.javacpp","1.1");

    // FIXME - should just add IpFrameGrabber and drop the dependency !!!
    // addDependency("org.bytedeco", "javacv-platform", "1.3.3");

  }
}
