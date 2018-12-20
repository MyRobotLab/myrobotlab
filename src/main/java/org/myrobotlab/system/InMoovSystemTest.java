package org.myrobotlab.system;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.service.Python;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.test.TestUtils;

/**
 * InMoov system test.  given an inmoov script location, it will load that script.
 * This is useful for developers to be able to launch the inmoov scripts from inside of an IDE like eclipse or other.
 *
 */
@Ignore
public class InMoovSystemTest {

  private String inmoovScript = "./InMoov/InMoov.py";
  
  @Test
  public void testInMoovBuild() throws IOException {

    // setup some logging.. etc.
    TestUtils.initEnvirionment();
    // TODO: make sure the inmoov service (and dependencies are installed)
    // TODO: maybe just force an install all here.
    // for now assume the start_inmoov is installed locally.
    // Ok. first thing to do.. we have 2 arduinos.. upload them with the latest
    
    Python python = (Python)Runtime.createAndStart("python", "Python");
    python.execFile(inmoovScript);
    System.err.println("Press any key to exit.");
    System.out.flush();
    System.in.read();
    System.err.println("Ahhhh... the any key!");
    System.err.flush();
  }
}
