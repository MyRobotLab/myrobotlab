package org.myrobotlab.inmoov;

import java.io.File;
import java.io.IOException;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Python;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class Utils {

  public final static Logger log = LoggerFactory.getLogger(Utils.class);

  /**
   * This method will load a python file into the python interpreter.
   */
  public static boolean loadPythonFile(String file, String intanceName) {
    File f = new File(file);

    Python p = (Python) Runtime.getService("python");
    if (!intanceName.equals("inMoov")) {
      // p.exec("inMoov=" + intanceName, true, true);
    }
    log.info("Loading  Python file {}", f.getAbsolutePath());
    String script = null;
    try {
      script = FileIO.toString(f.getAbsolutePath());
    } catch (IOException e) {
      log.error("IO Error loading file");
      return false;
    }
    // evaluate the scripts in a blocking way.
    boolean result = p.exec(script, true, true);
    if (!result) {
      log.error("Error while loading file {}", f.getAbsolutePath());
    } else {
      log.info("Successfully loaded {}", f.getAbsolutePath());
    }
    return true;
  }

}
