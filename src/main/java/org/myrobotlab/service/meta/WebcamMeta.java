package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WebcamMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WebcamMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public WebcamMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    // setAutoOpenMode(true);

    addDescription("used as a general webcam");
    addCategory("video");

  }

}
