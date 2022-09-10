package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class HtmlParserMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(HtmlParserMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public HtmlParserMeta() {
    addDependency("org.jsoup", "jsoup", "1.15.3");
    addDescription("html parser");
    addCategory("document");
    // Set to false since no JSoup service exists
    setAvailable(false);

  }

}
