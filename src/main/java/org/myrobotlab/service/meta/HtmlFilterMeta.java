package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class HtmlFilterMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(HtmlFilterMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public HtmlFilterMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("This service will strip html markup from the input text");
    addCategory("filter");
    addDependency("org.jsoup", "jsoup", "1.8.3");
    addDependency("org.apache.commons", "commons-lang3", "3.3.2");

  }

}
