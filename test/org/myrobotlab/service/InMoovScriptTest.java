package org.myrobotlab.service;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.test.TestUtils;

// Grr.. TODO: disable this until we can figure out why travis is dying on it.
// @Ignore
public class InMoovScriptTest {

  private static final String V_PORT_1 = "test_port_1";
  private static final String V_PORT_2 = "test_port_2";

  public Arduino ard1;
  public Arduino ard2;
  
  @Before
  public void setup() throws Exception {
    // setup the test environment , and create an arduino with a virtual backend for it.
    TestUtils.initEnvirionment();
    // initialize 2 serial ports (virtual arduino)
    VirtualArduino va1 = (VirtualArduino)Runtime.createAndStart("va1", "VirtualArduino");
    VirtualArduino va2 = (VirtualArduino)Runtime.createAndStart("va2", "VirtualArduino");
    // one for the left port
    va1.connect(V_PORT_1);
    // one for the right port.
    va2.connect(V_PORT_2);
  }
  
  @Test
  public void testInMoovMinimal() throws IOException {
    // The script should reference V_PORT_1 or V_PORT_2 
    String inmoovScript = "test/resources/InMoov/Inmoov3.minimal.py";
    String script = FileIO.toString(inmoovScript);
    Python python = (Python)Runtime.createAndStart("python", "Python");
    python.createPythonInterpreter();
    // python.execAndWait(script);
    python.interp.exec(script);
    // Assert something
    assertNotNull(Runtime.getService("i01"));
  }
}
