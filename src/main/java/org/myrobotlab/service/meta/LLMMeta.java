package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class LLMMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(LLMMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public LLMMeta() {

    // add a cool description
    addDescription("Ollama or OpenApi inteface service");

    // add it to one or many categories
    addCategory("AI");
    
    setCloudService(true);

    setSponsor("GroG");

  }

}
