package org.myrobotlab.service.meta;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.myrobotlab.framework.repo.ServiceDependency;

public class JMonkeyEngineMetaTest {

  @Test
  public void declaresLinuxArm64LwjglNatives() {
    JMonkeyEngineMeta meta = new JMonkeyEngineMeta();
    boolean glfwArm64 = false;
    boolean lwjglArm64 = false;
    for (ServiceDependency d : meta.getDependencies()) {
      if (!"org.lwjgl".equals(d.getOrgId())) {
        continue;
      }
      if ("natives-linux-arm64".equals(d.getClassifier())) {
        if ("lwjgl".equals(d.getArtifactId())) {
          lwjglArm64 = true;
        }
        if ("lwjgl-glfw".equals(d.getArtifactId())) {
          glfwArm64 = true;
        }
      }
    }
    assertTrue("lwjgl natives-linux-arm64 must be a Meta dependency for Raspberry Pi 64-bit", lwjglArm64);
    assertTrue("lwjgl-glfw natives-linux-arm64 must be a Meta dependency for Raspberry Pi 64-bit", glfwArm64);
  }

  @Test
  public void declaresBulletNativeJar() {
    JMonkeyEngineMeta meta = new JMonkeyEngineMeta();
    boolean bulletNative = false;
    for (ServiceDependency d : meta.getDependencies()) {
      if ("jme3-bullet-native".equals(d.getArtifactId())) {
        bulletNative = true;
        break;
      }
    }
    assertTrue("jme3-bullet-native 3.3.2 contains native/linux/aarch64/libbulletjme.so for the Pi", bulletNative);
  }

}
