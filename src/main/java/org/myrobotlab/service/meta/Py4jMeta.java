package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Py4jMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Py4jMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public Py4jMeta() {

    addDescription("Python 3.x support");
    addCategory("programming");
    setSponsor("GroG");
    addDependency("net.sf.py4j", "py4j", "0.10.9.7");

    // Used just as a Python exe redistributable.
    // ABSOLUTELY NO JNI/JNA IS USED
    addDependency("org.bytedeco", "cpython-platform", "3.11.3-1.5.9");
    addDependency("org.bytedeco", "cpython", "3.11.3-1.5.9");
  }

}
