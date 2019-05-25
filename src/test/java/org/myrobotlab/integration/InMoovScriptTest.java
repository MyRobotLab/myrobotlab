package org.myrobotlab.integration;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.service.InMoov;
import org.myrobotlab.service.Python;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.test.AbstractTest;

public class InMoovScriptTest extends AbstractTest {

  private String scriptRoot = "src/test/resources/InMoov";

  // Test the inmoov minimal script.
  @Test
  public void testInMoovMinimal() throws IOException {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    // TODO: move these scripts to test resources
    String inmoovScript = scriptRoot + "/InMoov.minimal.py";
    File f = new File(inmoovScript);
    System.out.println("IN MOOV SCRIPT: " + f.getAbsolutePath());
    // InputStream is = this.getClass().getResourceAsStream(inmoovScript);
    String script = FileIO.toString(inmoovScript);
    // String script = new String(FileIO.toByteArray(is));
    Python python = (Python) Runtime.createAndStart("python", "Python");
    // python.execAndWait(script);
    python.exec(script);
    InMoov i01 = (InMoov) Runtime.getService("i01");
    // Assert something
    assertNotNull(i01);
  }

  // Test the inmoov minimal arm script.
  @Test
  public void testInMoovMinimalArm() throws IOException {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    if (!isHeadless()) {
      Runtime.start("gui", "SwingGui");
    }
    String inmoovScript = scriptRoot + "/InMoov.minimalArm.py";
    File f = new File(inmoovScript);
    System.out.println("IN MOOV SCRIPT: " + f.getAbsolutePath());
    String script = FileIO.toString(inmoovScript);
    // FileIO.toFile("script.py", script);
    // String script = new String(FileIO.toByteArray(is));
    Python python = (Python) Runtime.start("python", "Python");
    python.exec(script);
    InMoov i01 = (InMoov) Runtime.getService("i01");
    // Assert something
    assertNotNull(i01);
  }

  // Test the inmoov minimal arm script.
  @Test
  public void testInMoovMinimalFingerStarter() throws IOException {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    String inmoovScript = scriptRoot + "/InMoov.minimalFingerStarter.py";
    File f = new File(inmoovScript);
    System.out.println("IN MOOV SCRIPT: " + f.getAbsolutePath());
    // InputStream is = this.getClass().getResourceAsStream(inmoovScript);
    String script = FileIO.toString(inmoovScript);
    // String script = new String(FileIO.toByteArray(is));
    Python python = (Python) Runtime.createAndStart("python", "Python");
    python.createPythonInterpreter();
    // python.execAndWait(script);
    python.exec(script);
    InMoov i01 = (InMoov) Runtime.getService("i01");
    // Assert something
    assertNotNull(i01);
  }

  // Test the inmoov minimal arm script.
  @Test
  public void testInMoovMinimalHead() throws IOException {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    String inmoovScript = scriptRoot + "/InMoov.minimalHead.py";
    File f = new File(inmoovScript);
    System.out.println("IN MOOV SCRIPT: " + f.getAbsolutePath());
    // InputStream is = this.getClass().getResourceAsStream(inmoovScript);
    String script = FileIO.toString(inmoovScript);
    FileIO.toFile("script.py", script);
    // String script = new String(FileIO.toByteArray(is));
    Python python = (Python) Runtime.start("python", "Python");
    // python.execAndWait(script);
    python.exec(script);
    InMoov i01 = (InMoov) Runtime.getService("i01");
    // Assert something
    assertNotNull(i01);
  }

  // Test the inmoov minimal arm script.
  @Test
  public void testInMoovMinimalTorso() throws IOException {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    String inmoovScript = scriptRoot + "/InMoov.minimalTorso.py";
    File f = new File(inmoovScript);
    System.out.println("IN MOOV SCRIPT: " + f.getAbsolutePath());
    String script = FileIO.toString(inmoovScript);
    // String script = new String(FileIO.toByteArray(is));
    Python python = (Python) Runtime.createAndStart("python", "Python");    
    python.exec(script);
    InMoov i01 = (InMoov) Runtime.getService("i01");
    // Assert something
    assertNotNull(i01);
  }

}
