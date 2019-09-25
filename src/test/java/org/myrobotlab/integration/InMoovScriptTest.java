package org.myrobotlab.integration;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.net.Http;
import org.myrobotlab.service.InMoov;
import org.myrobotlab.service.Python;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.test.AbstractTest;

@Ignore
public class InMoovScriptTest extends AbstractTest {

  boolean testLocal = true;

  // set this to null to check the checked in scripts
  static String scriptRoot = "C:\\mrl\\mrl.develop\\pyrobotlab\\service\\";
  
  static String branch = "develop";
  
  static boolean virtual = true;

  static List<Script> scripts = new ArrayList<>();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    addScript("InMoov.py", scriptRoot);
    addScript("InMoovArm.py", scriptRoot);
    addScript("InMoovEyelids.py", scriptRoot);
    addScript("InMoovFingerStarter.py", scriptRoot);
    addScript("InMoovGestureCreator.py", scriptRoot);
    addScript("InMoovHand.py", scriptRoot);
    addScript("InMoovHead.py", scriptRoot);
    addScript("InMoov.py", scriptRoot);
    addScript("InMoovTorso.py", scriptRoot);

  }

  public static void addScript(String filename, String root) throws IOException {

    String content = null;
    if (root == null) {
      content = new String(Http.get("https://raw.githubusercontent.com/MyRobotLab/pyrobotlab/"+branch+"/service/" + filename));
    } else {
      content = FileIO.toString(root + filename);
    }

    scripts.add(new Script(filename, content));
  }

  static class Script {
    public String filename;
    public String content;

    public Script(String filename, String content) {
      this.filename = filename;
      this.content = content;
    }
  }

  @Test
  public void testInMoovScript() throws IOException {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    
    if (!isHeadless()) {
      Runtime.start("gui", "SwingGui");
    }
    Python python = (Python) Runtime.start("python", "Python");
    // SwingGui gui = (SwingGui) Runtime.start("gui", "SwingGui");
    
    String v = "";
    
    if (virtual) {
      v = "Platform.setVirtual(True)\n";
    }
        
    for (Script script : scripts) {

      log.warn("testing inmoov script {}", script.filename);
      
      String content = v + script.content;

      
      // python.execAndWait(script);
      log.warn("############### script starts ###############");
      log.warn(content);
      log.warn("############### script ends ###############");
      python.exec(content);

      InMoov i01 = (InMoov) Runtime.getService("i01");
      // Assert something
      assertNotNull(i01);
      
      // do other testing .... !!! ask it something, get something back
      // test virtual inmoov
      
      // tear down
      Runtime.releaseAll();
      
    }
  }

}
