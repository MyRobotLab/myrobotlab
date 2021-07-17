package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class GoProMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(GoProMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * @param name n
   * 
   */
  public GoProMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    // add dependency if necessary
    // addDependency("org.coolproject", "1.0.0");
    addCategory("video");
    addPeer("http", "HttpClient", "Http for GoPro control");
    addDescription("Go pro camera support");

  }

}
