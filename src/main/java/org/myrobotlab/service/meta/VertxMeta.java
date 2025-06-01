package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class VertxMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(VertxMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public VertxMeta() {

    // add a cool description
    addDescription("vertx stack service");

    // false will prevent it being seen in the ui
    setAvailable(true);

    // add dependencies if necessary
    addDependency("io.vertx", "vertx-core", "4.3.8");
    exclude("io.netty", "*"); // it brings in an old version of netty

    addDependency("io.vertx", "vertx-web", "4.3.8");
    exclude("io.netty", "*"); // it brings in an old version of netty
    
    // force correct version of netty
    addDependency("io.netty", "netty-all", "4.1.82.Final");


    // add it to one or many categories
    addCategory("network");

    // add a sponsor to this service
    // the person who will do maintenance
    // setSponsor("GroG");

  }

}
