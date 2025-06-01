package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class OculusRiftMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OculusRiftMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public OculusRiftMeta() {

    addDescription("The Oculus Rift Head Tracking Service");
    addCategory("video", "control", "sensors", "telerobotics");
    // dependencies. we also need lwjgl3 added here. currently it's pulled in
    // from jme3-lwjgl3
    addDependency("org.saintandreas", "jovr", "1.8.0.0");
    addDependency("slick-util", "slick-util", "1.0.0");
    addDependency("org.jscience", "jscience", "4.3.1");
    addDependency("org.saintandreas", "xres", "1.0.3");
    addDependency("org.saintandreas", "oria-resources", "1.0.4");
    exclude("org.slf4j", "slf4j-api");
    exclude("org.lwjgl.lwjgl", "lwjgl");
    exclude("com.google.guava", "guava");

  }

}
