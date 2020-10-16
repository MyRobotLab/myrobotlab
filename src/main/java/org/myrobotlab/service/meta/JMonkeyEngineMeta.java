package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class JMonkeyEngineMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(JMonkeyEngineMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public JMonkeyEngineMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("is a 3d game engine, used for simulators");

    String jmeVersion = "3.3.2-stable";
    addDependency("org.jmonkeyengine", "jme3-core", jmeVersion);
    addDependency("org.jmonkeyengine", "jme3-desktop", jmeVersion);
    // addDependency("org.jmonkeyengine", "jme3-lwjgl", jmeVersion);
    // nev version of lwjgl3 which works with java 11
    addDependency("org.jmonkeyengine", "jme3-lwjgl", jmeVersion);
    addDependency("org.jmonkeyengine", "jme3-jogg", jmeVersion);
    
    
    addDependency("org.lwjgl.lwjgl","lwjgl","2.9.3");
    addDependency("org.lwjgl.lwjgl","lwjgl-platform","2.9.3", "pom");
    // addDependency("org.jmonkeyengine", "jme3-test-data", jmeVersion);
    addDependency("com.simsilica", "lemur", "1.11.0");
    addDependency("com.simsilica", "lemur-proto", "1.10.0");

    addDependency("org.jmonkeyengine", "jme3-bullet", jmeVersion);
    addDependency("org.jmonkeyengine", "jme3-bullet-native", jmeVersion);

    // addDependency("jme3utilities", "Minie", "0.6.2");
    // "new" physics - ik forward kinematics ...

    // not really supposed to use blender models - export to j3o
    addDependency("org.jmonkeyengine", "jme3-blender", jmeVersion);

    // jbullet ==> org="net.sf.sociaal" name="jME3-jbullet" rev="3.0.0.20130526"
    // audio dependencies
    addDependency("de.jarnbjo", "j-ogg-all", "1.0.0");
    addCategory("simulator");

  }
}
