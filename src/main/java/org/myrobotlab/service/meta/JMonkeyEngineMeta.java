package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class JMonkeyEngineMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(JMonkeyEngineMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.JMonkeyEngine");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("is a 3d game engine, used for simulators");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // TODO: extract version numbers like this into a constant/enum
    String jmeVersion = "3.2.2-stable";
    meta.addDependency("org.jmonkeyengine", "jme3-core", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-desktop", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-lwjgl", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-jogg", jmeVersion);
    // meta.addDependency("org.jmonkeyengine", "jme3-test-data", jmeVersion);
    meta.addDependency("com.simsilica", "lemur", "1.11.0");
    meta.addDependency("com.simsilica", "lemur-proto", "1.10.0");

    meta.addDependency("org.jmonkeyengine", "jme3-bullet", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-bullet-native", jmeVersion);

    // meta.addDependency("jme3utilities", "Minie", "0.6.2");

    // "new" physics - ik forward kinematics ...

    // not really supposed to use blender models - export to j3o
    meta.addDependency("org.jmonkeyengine", "jme3-blender", jmeVersion);

    // jbullet ==> org="net.sf.sociaal" name="jME3-jbullet" rev="3.0.0.20130526"

    // audio dependencies
    meta.addDependency("de.jarnbjo", "j-ogg-all", "1.0.0");

    meta.addCategory("simulator");
    return meta;
  }
}

