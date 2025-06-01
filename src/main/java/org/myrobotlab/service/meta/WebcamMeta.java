package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WebcamMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WebcamMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public WebcamMeta() {

    addDescription("used as a general webcam");
    addCategory("video");
    // if (platform.isLinux()) {
    addDependency("com.github.sarxos", "webcam-capture-driver-v4l4j", "0.3.13-SNAPSHOT");
    // } else {
    // ?? windows ok with default of v4l4j ???
    // addDependency("com.github.sarxos", "webcam-capture-driver-v4l4j",
    // "0.3.12");
    // }

  }

}
