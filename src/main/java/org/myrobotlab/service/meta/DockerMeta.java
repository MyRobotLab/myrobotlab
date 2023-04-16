package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class DockerMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(DockerMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public DockerMeta() {

    // add a cool description
    addDescription("Docker service to manage docker containers");

    // false will prevent it being seen in the ui
    // setAvailable(true);

    // add dependencies if necessary
    // addDependency("com.twelvemonkeys.common", "common-lang", "3.1.1");
    
    addDependency("com.github.docker-java", "docker-java", "3.2.13");
    exclude("io.netty", "*");
    addDependency("io.netty", "netty-all", "4.1.82.Final");

    // setAvailable(false);

    // add it to one or many categories
    addCategory("docker","platform");

    // add a sponsor to this service
    // the person who will do maintenance
    // setSponsor("GroG");

  }

}
