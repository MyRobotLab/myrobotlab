package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WebGuiMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WebGuiMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public WebGuiMeta() {

    Platform platform = Platform.getLocalInstance();
    addDescription("web display");
    addCategory("display");

    includeServiceInOneJar(true);
    addDependency("org.atmosphere", "nettosphere", "3.2.1");
    addDependency("javax.annotation", "javax.annotation-api", "1.3.2");

    // MAKE NOTE !!! - we currently distribute myrobotlab.jar with a webgui
    // hence these following dependencies are zipped with myrobotlab.jar !
    // and are NOT listed as dependencies, because they are already included

    // Its now part of myrobotlab.jar - unzipped in
    // build.xml (part of myrobotlab.jar now)

    // addDependency("io.netty", "3.10.0"); // netty-3.10.0.Final.jar
    // addDependency("org.atmosphere.nettosphere", "2.3.0"); //
    // nettosphere-assembly-2.3.0.jar
    // addDependency("org.atmosphere.nettosphere", "2.3.0");//
    // geronimo-servlet_3.0_spec-1.0.jar

  }

}
