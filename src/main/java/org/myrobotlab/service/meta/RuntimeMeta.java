package org.myrobotlab.service.meta;

import org.myrobotlab.framework.repo.IvyWrapper;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class RuntimeMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(RuntimeMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public RuntimeMeta() {

    addDescription("is a singleton service responsible for the creation, starting, stopping, releasing and registration of all other services");
    addCategory("framework");
    
    // logback gets upset if its in the jar and in the libraries dir
    // so backend will just be in the libraries dir
    addDependency("ch.qos.logback", "logback-classic", "1.2.3");
    
    includeServiceInOneJar(true);
    // apache 2.0 license
    addDependency("com.google.code.gson", "gson", "2.8.5");

    addDependency("com.fasterxml.jackson.core", "jackson-core", "2.13.3");
    addDependency("com.fasterxml.jackson.core", "jackson-annotations", "2.13.3");
    addDependency("com.fasterxml.jackson.core", "jackson-databind", "2.13.3");
    addDependency("com.fasterxml.jackson.module", "jackson-module-no-ctor-deser", "2.13.3");
    // apache 2.0 license
    // addDependency("org.apache.ivy", "ivy", "2.4.0-5");
    addDependency("org.apache.ivy", "ivy", IvyWrapper.IVY_VERSION);
    
    // apache 2.0 license
    addDependency("org.apache.httpcomponents", "httpclient", "4.5.13");
    // apache 2.0 license
    addDependency("org.atmosphere", "wasync", "2.1.5");
    // apache 2.0 license
    addDependency("info.picocli", "picocli", "4.4.0");
    // all your logging needs
    addDependency("org.slf4j", "slf4j-api", "1.7.36");
    
    // for config file support.
    addDependency("org.yaml", "snakeyaml", "1.29");

    // ws client sockets 
    addDependency("org.asynchttpclient", "async-http-client", "2.12.3");

  }

}
