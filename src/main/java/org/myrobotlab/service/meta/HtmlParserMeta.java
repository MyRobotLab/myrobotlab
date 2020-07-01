package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class HtmlParserMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(HtmlParserMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.HtmlParser");
    Platform platform = Platform.getLocalInstance();
    meta.addDependency("org.jsoup", "jsoup", "1.8.3");
    meta.addDescription("html parser");
    meta.addCategory("document");
    // Set to false since no JSoup service exists
    meta.setAvailable(false);
    return meta;
  }

}

