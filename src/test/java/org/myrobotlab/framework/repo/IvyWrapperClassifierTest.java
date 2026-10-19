package org.myrobotlab.framework.repo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

public class IvyWrapperClassifierTest {

  @Test
  public void ivyXmlIncludesMavenClassifier() {
    ServiceDependency natives = new ServiceDependency("org.lwjgl", "lwjgl", "3.3.2");
    natives.setClassifier("natives-linux-arm64");
    StringBuilder sb = new StringBuilder();
    IvyWrapper.appendIvyDependency(sb, natives);
    String xml = sb.toString();
    assertTrue(xml.contains("m:classifier=\"natives-linux-arm64\""));
    assertTrue(xml.contains("name=\"lwjgl\""));
  }

  @Test
  public void retrievePatternKeepsClassifier() {
    assertTrue(IvyWrapper.IVY_RETRIEVE_PATTERN.contains("[classifier]"));
    assertTrue(IvyWrapper.IVY_RETRIEVE_PATTERN.contains("[artifact]-[revision]"));
  }

  @Test
  public void supersededRevisionJarsAreRemoved() throws Exception {
    File dir = Files.createTempDirectory("mrl-ivy-jars").toFile();
    File current = new File(dir, "lwjgl-3.3.2.jar");
    File currentNative = new File(dir, "lwjgl-3.3.2-natives-windows.jar");
    File old = new File(dir, "lwjgl-3.2.3.jar");
    File oldNative = new File(dir, "lwjgl-3.2.3-natives-linux.jar");
    File otherModule = new File(dir, "lwjgl-opengl-3.2.3.jar");
    assertTrue(current.createNewFile());
    assertTrue(currentNative.createNewFile());
    assertTrue(old.createNewFile());
    assertTrue(oldNative.createNewFile());
    assertTrue(otherModule.createNewFile());

    IvyWrapper.removeOtherRevisionJars(dir, "lwjgl", "3.3.2");

    assertTrue(current.exists());
    assertTrue(currentNative.exists());
    assertTrue(otherModule.exists());
    assertFalse(old.exists());
    assertFalse(oldNative.exists());
  }

}
