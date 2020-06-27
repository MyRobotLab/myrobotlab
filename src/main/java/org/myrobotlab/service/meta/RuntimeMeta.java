package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class RuntimeMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(RuntimeMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Runtime");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("is a singleton service responsible for the creation, starting, stopping, releasing and registration of all other services");
    meta.addCategory("framework");

    meta.includeServiceInOneJar(true);
    // apache 2.0 license
    meta.addDependency("com.google.code.gson", "gson", "2.8.5");
    // apache 2.0 license
    meta.addDependency("org.apache.ivy", "ivy", "2.4.0-5");
    // apache 2.0 license
    meta.addDependency("org.apache.httpcomponents", "httpclient", "4.5.2");
    // apache 2.0 license
    meta.addDependency("org.atmosphere", "wasync", "2.1.5");
    // apache 2.0 license
    meta.addDependency("info.picocli", "picocli", "4.0.0-beta-2");

    // EDL (new-style BSD) licensed
    meta.addDependency("org.eclipse.jgit", "org.eclipse.jgit", "5.4.0.201906121030-r");

    // all your logging needs
    meta.addDependency("org.slf4j", "slf4j-api", "1.7.21");
    meta.addDependency("ch.qos.logback", "logback-classic", "1.0.13");

    // meta.addDependency("org.apache.maven", "maven-embedder", "3.1.1");
    // meta.addDependency("ch.qos.logback", "logback-classic", "1.2.3");

    return meta;
  }

  
  
}

