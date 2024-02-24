package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class RemoteSpeechMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(RemoteSpeechMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public RemoteSpeechMeta() {

    // add a cool description
    addDescription("used as a general template");

    // false will prevent it being seen in the ui
    setAvailable(true);

    // add it to one or many categories
    addCategory("speech");

    // add a sponsor to this service
    // the person who will do maintenance
    // setSponsor("GroG");

  }

}
