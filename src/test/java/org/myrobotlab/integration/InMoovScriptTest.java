package org.myrobotlab.integration;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.service.InMoov;
import org.myrobotlab.service.Python;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.test.AbstractTest;


public class InMoovScriptTest extends AbstractTest {

  boolean testLocal = true;
  
  static boolean virtual = true;

  static List<Script> scripts = new ArrayList<>();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    addScript("./src/main/resources/resource/InMoov/InMoovFingerStarter.py");
    addScript("./src/main/resources/resource/InMoov/InMoov.py");
    addScript("./src/main/resources/resource/InMoovHead/InMoovHead.py");
    addScript("./src/main/resources/resource/InMoovArm/InMoovArm.py");
    // addScript("./src/main/resources/resource/InMoovEyelids/InMoovEyelids.py"); - these script don't even create a i01 which is the only check :P
    addScript("./src/main/resources/resource/InMoovHand/InMoovHand.py");
    // addScript("./src/main/resources/resource/InMoovGestureCreator/InMoovGestureCreator.py");
    addScript("./src/main/resources/resource/InMoovTorso/InMoovTorso.py");
 
  }

  public static void addScript(String path) throws IOException {
    String content = FileIO.toString(path);
    scripts.add(new Script(path, content));
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
      // script = new Script("./src/main/resources/resource/InMoovEyelids/InMoovEyelids.py", FileIO.toString("./src/main/resources/resource/InMoovEyelids/InMoovEyelids.py"));
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
