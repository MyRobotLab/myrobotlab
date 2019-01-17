package org.myrobotlab.system;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Python;
import org.myrobotlab.service.Runtime;

/**
 * InMoov system test. given an inmoov script location, it will load that
 * script. This is useful for developers to be able to launch the inmoov scripts
 * from inside of an IDE like eclipse or other.
 *
 */
@Ignore
public class InMoovSystemTest {

  private String inmoovScript = "./InMoov/InMoov.py";

  @Test
  public void testInMoovBuild() throws IOException {

    // setup some logging.. etc.
    LoggingFactory.init("INFO");
    // make sure the inmoov zip is installed locally
    // Runtime.install();

    Python python = (Python) Runtime.createAndStart("python", "Python");
    python.execFile(inmoovScript);
    System.err.println("Press any key to exit.");
    System.out.flush();
    System.in.read();
    System.err.println("Ahhhh... the any key!");
    System.err.flush();
  }
}
