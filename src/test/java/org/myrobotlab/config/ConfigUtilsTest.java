package org.myrobotlab.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.framework.StartYml;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.service.Runtime;

public class ConfigUtilsTest {

  @Before
  public void beforeTest() {
    Runtime.releaseAll(true, true);
    // remove config
    FileIO.rm("data/config/default");
  }

  @Test
  public void testGetResourceRoot() {
    String resource = ConfigUtils.getResourceRoot();
    // could be affected by dirty filesystem
    assertEquals("resource", resource);
  }

  @Test
  public void testLoadRuntimeConfig() {
    String resource = ConfigUtils.getResourceRoot();
    assertNotNull(resource);
  }

  @Test
  public void testLoadStartYml() {
    StartYml start = ConfigUtils.loadStartYml();
    assertNotNull(start);
  }

  @Test
  public void testGetId() {
    assertEquals(ConfigUtils.getId(), ConfigUtils.loadRuntimeConfig(null).id);
  }


}
