package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class HtmlFilterMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(HtmlFilterMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.HtmlFilter");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("This service will strip html markup from the input text");
    meta.addCategory("filter");
    meta.addDependency("org.jsoup", "jsoup", "1.8.3");
    meta.addDependency("org.apache.commons", "commons-lang3", "3.3.2");
    return meta;
  }

  
  
}

