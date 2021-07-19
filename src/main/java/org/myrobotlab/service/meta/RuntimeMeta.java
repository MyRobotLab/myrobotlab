package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class RuntimeMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(RuntimeMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * @param name n
   * 
   */
  public RuntimeMeta(String name) {

    super(name);
    // TODO: can we remove this line?  it's not used locally, but it does initialize Platform...
    Platform platform = Platform.getLocalInstance();
    addDescription("is a singleton service responsible for the creation, starting, stopping, releasing and registration of all other services");
    addCategory("framework");
    includeServiceInOneJar(true);
    // apache 2.0 license
    addDependency("com.google.code.gson", "gson", "2.8.5");
    // apache 2.0 license
    addDependency("org.apache.ivy", "ivy", "2.4.0-5");
    // apache 2.0 license
    addDependency("org.apache.httpcomponents", "httpclient", "4.5.2");
    // apache 2.0 license
    addDependency("org.atmosphere", "wasync", "2.1.5");
    // apache 2.0 license
    addDependency("info.picocli", "picocli", "4.4.0");
    // all your logging needs
    addDependency("org.slf4j", "slf4j-api", "1.7.21");
    addDependency("ch.qos.logback", "logback-classic", "1.0.13");
    // for config file support.
    addDependency("org.yaml", "snakeyaml", "1.29");

  }

}
