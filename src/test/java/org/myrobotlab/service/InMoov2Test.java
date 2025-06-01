package org.myrobotlab.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.myrobotlab.framework.StaticType;
import org.myrobotlab.service.config.OpenCVConfig;
import org.myrobotlab.test.AbstractTest;

public class InMoov2Test extends AbstractTest {

  @Test
  public void testCvFilters() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

    Runtime.setConfig("InMoov2Test");

    InMoov2 i01 = (InMoov2) Runtime.start("i01", "InMoov2");
    

    // flip
    i01.setPeerConfigValue("opencv", "flip", true);
    OpenCVConfig cvconfig = i01.getPeerConfig("opencv", new StaticType<>() {
    });
    assertTrue(cvconfig.flip);

    i01.setPeerConfigValue("opencv", "flip", false);
    cvconfig = i01.getPeerConfig("opencv", new StaticType<>() {
    });
    assertFalse(cvconfig.flip);
    
    i01.startPeer("mouth");

    long start = System.currentTimeMillis();
    // i01.setSpeechType("LocalSpeech");
    i01.speakBlocking(
        "Hello this is a way to test if speech is actually blocking, if it blocks it should take a little time to say this, if it doesn't work it will execute the next line immediately.");
    System.out.println(String.format("speech blocking time taken %d", System.currentTimeMillis() - start));
    assertTrue(start > 2000);

  }

}
