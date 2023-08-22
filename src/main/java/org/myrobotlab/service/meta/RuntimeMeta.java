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

    // for proxy generation
    addDependency("net.bytebuddy", "byte-buddy", "1.12.16");

    addDependency("com.fasterxml.jackson.core", "jackson-core", "2.14.0");
    addDependency("com.fasterxml.jackson.core", "jackson-annotations", "2.14.0");
    addDependency("com.fasterxml.jackson.core", "jackson-databind", "2.14.0");
    addDependency("com.fasterxml.jackson.module", "jackson-module-no-ctor-deser", "2.14.0");
    // apache 2.0 license
    // addDependency("org.apache.ivy", "ivy", "2.4.0-5");
    addDependency("org.apache.ivy", "ivy", IvyWrapper.IVY_VERSION);

    // apache 2.0 license - REMOVE in favor of okhttp
    // FIXME - replace apache with okhttp
    addDependency("org.apache.httpcomponents", "httpclient", "4.5.13");
    
    // apache 2.0 license
    addDependency("info.picocli", "picocli", "4.4.0");
    // all your logging needs
    addDependency("org.slf4j", "slf4j-api", "1.7.36");

    // for config file support.
    addDependency("org.yaml", "snakeyaml", "1.32");

    // ws best client websockets with Apache license
    addDependency("com.squareup.okhttp3", "okhttp", "3.9.0");
        
    // force correct version of netty - needed for Vertx but not for Runtime ?
    addDependency("io.netty", "netty-all", "4.1.82.Final");

    // Used just as a Python exe redistributable.
    // ABSOLUTELY NO JNI/JNA IS USED
    addDependency("org.bytedeco", "cpython-platform", "3.10.8-1.5.8");
    addDependency("org.bytedeco", "cpython", "3.10.8-1.5.8");
    addDependency("org.bytedeco", "javacpp", "1.5.8");
    addDependency("org.bytedeco", "javacpp-platform", "1.5.8");

//    addDependency("org.apache.commons", "commons-lang3", "3.3.2");

  }

}
