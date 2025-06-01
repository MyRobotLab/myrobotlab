package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class RSSConnectorMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(RSSConnectorMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public RSSConnectorMeta() {
    addDescription("This will crawl an rss feed at the given url and break apart the page into Documents");
    setCloudService(true);
    addCategory("cloud");
    // Tried to add this dependency, but no luck with defining the ivy.xml
    addDependency("rome", "rome", "1.0");
    // addDependency("feed4j", "feed4j", "1.0.0");

  }

}
