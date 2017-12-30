package org.myrobotlab.service;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.test.TestUtils;

// Grr.. TODO: disable this until we can figure out why travis is dying on it.
// @Ignore
public class InMoovScriptTest {

  private static final String V_PORT_1 = "COM99";
  private static final String V_PORT_2 = "COM100";

  public Arduino ard1;
  public Arduino ard2;
  
  @Before
  public void setup() throws Exception {
    // setup the test environment ,and create an arduino with a virtual backend for it.
    TestUtils.initEnvirionment();
    // initialize 2 serial ports (virtual arduino)
    VirtualArduino va1 = (VirtualArduino)Runtime.createAndStart("va1", "VirtualArduino");
    VirtualArduino va2 = (VirtualArduino)Runtime.createAndStart("va2", "VirtualArduino");
    // one for the left port
    va1.connect(V_PORT_1);
    // one for the right port.
    va2.connect(V_PORT_2);
  }

  // Test the inmoov minimal script.
  @Test
  public void testInMoovMinimal() throws IOException {
    String inmoovScript = "src/resource/InMoov/InMoov.minimal.py";
    File f = new File(inmoovScript);
    System.out.println("IN MOOV SCRIPT: " + f.getAbsolutePath());
    //InputStream is = this.getClass().getResourceAsStream(inmoovScript);
    String script = FileIO.toString(inmoovScript);
    //String script = new String(FileIO.toByteArray(is));
    Python python = (Python)Runtime.createAndStart("python", "Python");
    python.createPythonInterpreter();
    // python.execAndWait(script);
    python.interp.exec(script);
    InMoov i01 = (InMoov)Runtime.getService("i01");
    // Assert something
    assertNotNull(i01);   
  }
  
  
  // Test the inmoov minimal arm script.
  @Test
  public void testInMoovMinimalArm() throws IOException {
    String inmoovScript = "src/resource/InMoov/InMoov.minimalArm.py";
    File f = new File(inmoovScript);
    System.out.println("IN MOOV SCRIPT: " + f.getAbsolutePath());
    //InputStream is = this.getClass().getResourceAsStream(inmoovScript);
    String script = FileIO.toString(inmoovScript);
    //String script = new String(FileIO.toByteArray(is));
    Python python = (Python)Runtime.createAndStart("python", "Python");
    python.createPythonInterpreter();
    // python.execAndWait(script);
    python.interp.exec(script);
    InMoov i01 = (InMoov)Runtime.getService("i01");
    // Assert something
    assertNotNull(i01);   
  }
    
  // Test the inmoov minimal arm script.
  @Test
  public void testInMoovMinimalFingerStarter() throws IOException {
    String inmoovScript = "src/resource/InMoov/InMoov.minimalFingerStarter.py";
    File f = new File(inmoovScript);
    System.out.println("IN MOOV SCRIPT: " + f.getAbsolutePath());
    //InputStream is = this.getClass().getResourceAsStream(inmoovScript);
    String script = FileIO.toString(inmoovScript);
    //String script = new String(FileIO.toByteArray(is));
    Python python = (Python)Runtime.createAndStart("python", "Python");
    python.createPythonInterpreter();
    // python.execAndWait(script);
    python.interp.exec(script);
    InMoov i01 = (InMoov)Runtime.getService("i01");
    // Assert something
    assertNotNull(i01);   
  }
  
  // Test the inmoov minimal arm script.
  @Test
  public void testInMoovMinimalHead() throws IOException {
    String inmoovScript = "src/resource/InMoov/InMoov.minimalHead.py";
    File f = new File(inmoovScript);
    System.out.println("IN MOOV SCRIPT: " + f.getAbsolutePath());
    //InputStream is = this.getClass().getResourceAsStream(inmoovScript);
    String script = FileIO.toString(inmoovScript);
    //String script = new String(FileIO.toByteArray(is));
    Python python = (Python)Runtime.createAndStart("python", "Python");
    python.createPythonInterpreter();
    // python.execAndWait(script);
    python.interp.exec(script);
    InMoov i01 = (InMoov)Runtime.getService("i01");
    // Assert something
    assertNotNull(i01);   
  }
  
  // Test the inmoov minimal arm script.
  @Test
  public void testInMoovMinimalTorso() throws IOException {
    String inmoovScript = "src/resource/InMoov/InMoov.minimalTorso.py";
    File f = new File(inmoovScript);
    System.out.println("IN MOOV SCRIPT: " + f.getAbsolutePath());
    //InputStream is = this.getClass().getResourceAsStream(inmoovScript);
    String script = FileIO.toString(inmoovScript);
    //String script = new String(FileIO.toByteArray(is));
    Python python = (Python)Runtime.createAndStart("python", "Python");
    python.createPythonInterpreter();
    // python.execAndWait(script);
    python.interp.exec(script);
    InMoov i01 = (InMoov)Runtime.getService("i01");
    // Assert something
    assertNotNull(i01);   
  }
  
  
}

