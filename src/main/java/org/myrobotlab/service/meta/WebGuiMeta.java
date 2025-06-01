package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WebGuiMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WebGuiMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public WebGuiMeta() {

    addDescription("web display");
    addCategory("display");

    includeServiceInOneJar(true);

    addDependency("org.jmdns", "jmdns", "3.5.5");
    addDependency("org.atmosphere", "nettosphere", "3.2.2");
    exclude("io.netty", "*"); // it brings in an old version of netty
    exclude("logback-classic", "*"); 
    exclude("logback-core", "*"); 

    addDependency("javax.annotation", "javax.annotation-api", "1.3.2");
    // force correct version of netty
    addDependency("io.netty", "netty-all", "4.1.82.Final");


  }

}
