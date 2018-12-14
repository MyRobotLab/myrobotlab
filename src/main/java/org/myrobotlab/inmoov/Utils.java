package org.myrobotlab.inmoov;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.repo.Category;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Python;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class Utils {

  public final static Logger log = LoggerFactory.getLogger(Utils.class);

  transient private static Runtime myRuntime = (Runtime) Runtime.getInstance();
  transient private static ServiceData serviceData = myRuntime.getServiceData();

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
    boolean result = p.exec(script, true, true);
    if (!result) {
      log.error("Error while loading file {}", f.getAbsolutePath());
      return false;
    } else {
      log.debug("Successfully loaded {}", f.getAbsolutePath());
    }
    return true;
  }

  /**
   * This method will try to launch a python comman from java land
   */
  public static String execPy(String command) {
    Python python = (Python) Runtime.getService("python");
    if (python == null) {
      log.warn("execGesture : No jython engine...");
    }
    try {
      return python.evalAndWait(command);
    } catch (Exception e) {
      log.error("execPy : {}", e);
    }
    return null;
  }

  //list services from meta category (pasted from RuntimeGui.java)
  public static List<String> getServicesFromCategory(final String filter) {
    List<String> servicesFromCategory = new ArrayList<String>();
    Category category = serviceData.getCategory(filter);
    HashSet<String> filtered = null;
    filtered = new HashSet<String>();
    ArrayList<String> f = category.serviceTypes;
    for (int i = 0; i < f.size(); ++i) {
      filtered.add(f.get(i));
    }

    // populate with serviceData
    List<ServiceType> possibleService = serviceData.getServiceTypes();
    for (int i = 0; i < possibleService.size(); ++i) {
      ServiceType serviceType = possibleService.get(i);
      if (filtered.contains(serviceType.getName())) {
        if (serviceType.isAvailable()) {
          // log.debug("serviceType : " + serviceType.getName());
          servicesFromCategory.add(serviceType.getSimpleName());
        }
      }

    }
    return servicesFromCategory;
  }
  
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
