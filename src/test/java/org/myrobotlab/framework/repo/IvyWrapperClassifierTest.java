package org.myrobotlab.framework.repo;

import static org.junit.Assert.assertTrue;

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

}
