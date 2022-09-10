package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class HtmlFilterMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(HtmlFilterMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public HtmlFilterMeta() {
    addDescription("This service will strip html markup from the input text");
    addCategory("filter");
    addDependency("org.jsoup", "jsoup", "1.15.3");
    addDependency("org.apache.commons", "commons-lang3", "3.3.2");
  }

}
