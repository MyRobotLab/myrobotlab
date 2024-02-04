package org.myrobotlab.service;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.myrobotlab.framework.StaticType;
import org.myrobotlab.service.config.OpenCVConfig;

public class InMoov2Test {

  @Test
  public void testCvFilters() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

    InMoov2 i01 = (InMoov2)Runtime.start("i01", "InMoov2");
      
      // flip
      i01.setPeerConfigValue("opencv", "flip", true);
      OpenCVConfig cvconfig = i01.getPeerConfig("opencv", new StaticType<>() {});
      assertTrue(cvconfig.flip);

      i01.setPeerConfigValue("opencv", "flip", false);
      cvconfig = i01.getPeerConfig("opencv", new StaticType<>() {});
      assertFalse(cvconfig.flip);
      
  }

  
}

