package org.myrobotlab.service;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggingFactory;

// Grr.. TODO: too hard a test for our weak jenkins oven in the cloud :(

public class InMoovScriptTest {

  private static final String V_PORT_1 = "COM99";
  private static final String V_PORT_2 = "COM100";

  public Arduino ard1;
  public Arduino ard2;

  private String scriptRoot = "src/test/resources/InMoov";

  @Before
  public void setup() throws Exception {
    // setup the test environment ,and create an arduino with a virtual backend for it.
    LoggingFactory.init("WARN");
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
    // TODO: move these scripts to test resources
    String inmoovScript = scriptRoot + "/InMoov.minimal.py";
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
    String inmoovScript = scriptRoot + "/InMoov.minimalArm.py";
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
    String inmoovScript = scriptRoot + "/InMoov.minimalFingerStarter.py";
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
    String inmoovScript = scriptRoot + "/InMoov.minimalHead.py";
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
    String inmoovScript = scriptRoot + "/InMoov.minimalTorso.py";
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
