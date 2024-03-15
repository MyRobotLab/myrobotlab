package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class JMonkeyEngineMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(JMonkeyEngineMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public JMonkeyEngineMeta() {

    addDescription("is a 3d game engine, used for simulators");

    String jmeVersion = "3.6.1-stable";
    addDependency("org.jmonkeyengine", "jme3-core", jmeVersion);
    addDependency("org.jmonkeyengine", "jme3-desktop", jmeVersion);
    // addDependency("org.jmonkeyengine", "jme3-lwjgl", jmeVersion);
    // nev version of lwjgl3 which works with java 11
    addDependency("org.jmonkeyengine", "jme3-lwjgl3", jmeVersion);
    addDependency("org.jmonkeyengine", "jme3-jogg", jmeVersion);

    addDependency("org.jmonkeyengine", "jme3-bullet", "3.3.2-stable");
    addDependency("org.jmonkeyengine", "jme3-bullet-native", "3.3.2-stable");

    addDependency("org.jmonkeyengine", "jme3-plugins", jmeVersion);

    // addDependency("jme3utilities", "Minie", "0.6.2");
    // "new" physics - ik forward kinematics ...

    // not really supposed to use blender models - export to j3o
    addDependency("org.jmonkeyengine", "jme3-blender", "3.3.2-stable");

    // jbullet ==> org="net.sf.sociaal" name="jME3-jbullet" rev="3.0.0.20130526"
    // audio dependencies
    addDependency("de.jarnbjo", "j-ogg-all", "1.0.0");
    addDependency("org.lwjgl", "lwjgl-opengl", "3.3.3");
    addDependency("org.lwjgl", "lwjgl-glfw", "3.3.3");

    addCategory("simulator");

  }
}
