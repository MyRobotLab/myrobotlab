package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Gpt3Meta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Gpt3Meta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public Gpt3Meta() {

    // add a cool description
    addDescription("used as a general template");

    // false will prevent it being seen in the ui
    setAvailable(true);

    // not worth it
    // addDependency("com.theokanning.openai-gpt3-java", "client", "0.8.0");

    setAvailable(false);

    // add it to one or many categories
    addCategory("general");

    // add a sponsor to this service
    // the person who will do maintenance
    // setSponsor("GroG");

  }

}
