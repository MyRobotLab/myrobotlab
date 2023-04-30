package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class SpotMicroMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(SpotMicroMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public SpotMicroMeta() {

    // add a cool description
    addDescription("used as a general template");

    // add it to one or many categories
    addCategory("robot");

    setSponsor("Cyber_One");

  }

}
