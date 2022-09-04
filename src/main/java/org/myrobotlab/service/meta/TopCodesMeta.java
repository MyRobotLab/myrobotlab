package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TopCodesMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(TopCodesMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public TopCodesMeta() {

    addDescription("Topcodes finds visual references and identifiers");
    addCategory("vision", "video", "sensors");
    addDependency("topcodes", "topcodes", "1.0.0");

  }

}
