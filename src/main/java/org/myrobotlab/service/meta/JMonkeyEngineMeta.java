package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class JMonkeyEngineMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(JMonkeyEngineMeta.class);

  private static final String[] LWJGL_NATIVE_MODULES = { "lwjgl", "lwjgl-opengl", "lwjgl-glfw", "lwjgl-jemalloc",
      "lwjgl-openal" };

  /**
   * Classifiers published by jme3-lwjgl3 3.6.1 / LWJGL 3.3.2. Must include
   * {@code natives-linux-arm64} for Raspberry Pi OS 64-bit.
   */
  private static final String[] LWJGL_NATIVE_CLASSIFIERS = { "natives-windows", "natives-windows-x86", "natives-linux",
      "natives-linux-arm32", "natives-linux-arm64", "natives-macos", "natives-macos-arm64" };

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

    // JME 3.6 loads bulletjme at context init whenever this Java jar is present.
    // 3.3.2 natives live under native/linux/aarch64/ (not JME 3.6's arm64/).
    // JmePlatform.prepareBulletNatives() remaps the loader path for the Pi.
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
    // Keep LWJGL in lockstep with jme3-lwjgl3 (3.6.1 → LWJGL 3.3.2).
    // Older 3.2.x pins broke Eclipse with NoClassDefFoundError: CallbackI$V
    // Native classifiers must be explicit: Ivy compile→default does not always
    // retrieve Maven runtime-scoped classified artifacts, and retrieve by
    // [originalname] can overwrite lwjgl.jar with the last native jar.
    String lwjglVersion = "3.3.2";
    addDependency("org.lwjgl", "lwjgl-opencl", lwjglVersion);
    for (String module : LWJGL_NATIVE_MODULES) {
      addDependency("org.lwjgl", module, lwjglVersion);
      for (String classifier : LWJGL_NATIVE_CLASSIFIERS) {
        addDependency("org.lwjgl", module, lwjglVersion, null, classifier);
      }
    }

    addCategory("simulator");

  }
}
