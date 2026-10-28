package org.myrobotlab.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.StaticType;
import org.myrobotlab.service.config.OpenCVConfig;
import org.myrobotlab.test.AbstractTest;

/**
 * InMoov2's web panel calls {@code i01.getPeerConfig("opencv")} as soon as it
 * opens. That used to NPE inside {@link OpenCV#getConfig()} when the peer was
 * registered but {@code config} had not been applied.
 */
public class OpenCVConfigNullTest extends AbstractTest {

  @Before
  public void setUp() {
    Runtime.releaseAll(true, true);
    Runtime.removeConfig("opencv-null-config");
    Runtime.setConfig("opencv-null-config");
  }

  @After
  public void tearDown() {
    Runtime.releaseAll(true, true);
    Runtime.removeConfig("opencv-null-config");
  }

  @Test
  public void peerConfigBeforeOpenCvExists() {
    InMoov2 i01 = (InMoov2) Runtime.create("i01", "InMoov2");
    assertNotNull(i01);

    OpenCVConfig cfg = i01.getPeerConfig("opencv", new StaticType<OpenCVConfig>() {
    });
    assertNotNull(cfg);
    assertTrue("OpenCV".equals(cfg.type));
  }

  @Test
  public void nullConfigDoesNotBreakGetPeerConfig() throws Exception {
    InMoov2 i01 = (InMoov2) Runtime.create("i01", "InMoov2");
    OpenCV cv = (OpenCV) Runtime.create("i01.opencv", "OpenCV");
    assertNotNull(cv.getConfig());
    assertTrue(cv.isConfigApplied());
    assertNotNull(cv.apply(null));
    assertNotNull(cv.getConfig());

    Field configField = Service.class.getDeclaredField("config");
    configField.setAccessible(true);
    configField.set(cv, null);

    assertNull(cv.getConfig());
    cv.flip(true);

    OpenCVConfig cfg = i01.getPeerConfig("opencv", new StaticType<OpenCVConfig>() {
    });
    assertNotNull(cfg);
  }

}
