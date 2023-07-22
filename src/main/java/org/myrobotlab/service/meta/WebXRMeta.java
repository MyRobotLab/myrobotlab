package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WebXRMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WebXRMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public WebXRMeta() {

    // add a cool description
    addDescription("WebXr allows hmi devices to add input and get data back from mrl");

    // false will prevent it being seen in the ui
    setAvailable(true);

    // add it to one or many categories
    addCategory("remote","control");

    // add a sponsor to this service
    // the person who will do maintenance
    // setSponsor("GroG");

  }

}
