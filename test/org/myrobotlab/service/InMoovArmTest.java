package org.myrobotlab.service;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class InMoovArmTest {

  // private InMoovArm testArm;
  private String port = "COM21";

  @Before
  public void setUp() throws Exception {

  }

  @Test
  public void testInMoovArm() throws Exception {
    // Runtime.createAndStart("webgui", "WebGui");
    Runtime.start("gui", "SwingGui");
    // testArm = (InMoovArm)Runtime.createAndStart("left", "InMoovArm");
    Python python = (Python) Runtime.start("python", "Python");
    String script = "leftArm = Runtime.start(\"leftArm\", \"InMoovArm\")\n" + "leftArm.connect(\"" + port + "\")";
    python.exec(script);
    // testArm.connect(port);

  }

  @After
  public void tearDown() throws IOException {
    System.out.println("Done.. press any key to exit.");
    System.in.read();
  }
}
