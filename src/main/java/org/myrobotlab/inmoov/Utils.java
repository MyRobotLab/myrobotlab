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
  public static boolean loadFile(String file) {
    File f = new File(file);
    Python p = (Python) Runtime.getService("python");
    log.info("Loading  Python file {}", f.getAbsolutePath());
    if (p == null) {
      log.error("Python instance not found");
      return false;
    }
    String script = null;
    try {
      script = FileIO.toString(f.getAbsolutePath());
    } catch (IOException e) {
      log.error("IO Error loading file : ", e);
      return false;
    }
    // evaluate the scripts in a blocking way.
    boolean result = p.exec(script, true);
    if (!result) {
      log.error("Error while loading file {}", f.getAbsolutePath());
      return false;
    } else {
      log.debug("Successfully loaded {}", f.getAbsolutePath());
    }
    return true;
  }
  
 // WHY ???
  public static File makeDirectory(String directory) {
    File dir = new File(directory);
    dir.mkdirs();
    if (!dir.isDirectory()) {
      // TODO: maybe create the directory ?
      log.warn("Directory {} doest not exist.", directory);
      return null;
    }
    return dir;
  }

}
