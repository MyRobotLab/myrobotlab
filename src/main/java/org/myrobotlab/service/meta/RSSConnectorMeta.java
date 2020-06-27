package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class RSSConnectorMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(RSSConnectorMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.RSSConnector");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("This will crawl an rss feed at the given url and break apart the page into Documents");
    meta.setCloudService(true);
    meta.addCategory("cloud");
    // Tried to add this dependency, but no luck with defining the ivy.xml
    meta.addDependency("rome", "rome", "1.0");
    // meta.addDependency("feed4j", "feed4j", "1.0.0");

    return meta;
  }

  
}

