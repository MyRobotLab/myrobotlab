package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TwitterMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(TwitterMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public TwitterMeta() {

    addDescription("Service which can relay tweets");
    addCategory("cloud");

    addDependency("org.twitter4j", "twitter4j-core", "3.0.5");
    setCloudService(true);

  }

}
