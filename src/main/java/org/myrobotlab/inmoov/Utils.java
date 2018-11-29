package org.myrobotlab.inmoov;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FilenameUtils;
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
  public static Boolean loadFile(String file, String type) {
    if (!FilenameUtils.getExtension(file).equalsIgnoreCase(type)) {
      log.warn("{} is not a {} file", file, type);
      return null;
    }
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
      log.error("IO Error loading file : ",e);
      return false;
    }
    // evaluate the scripts in a blocking way.
    boolean result = p.exec(script, true, true);
    if (!result) {
      log.error("Error while loading file {}", f.getAbsolutePath());
      return false;
    } else {
      log.info("Successfully loaded {}", f.getAbsolutePath());
    }
    return true;
  }

}
