package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class GoogleSearchMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(GoogleSearchMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public GoogleSearchMeta() {
    addDescription("used as a general google search");
    addDependency("org.jsoup", "jsoup", "1.15.3");
    addCategory("search");

  }

}
