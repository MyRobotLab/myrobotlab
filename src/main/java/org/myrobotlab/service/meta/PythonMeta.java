package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class PythonMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(PythonMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public PythonMeta() {

    addDescription("the Jython script engine compatible with pure Python 2.7 scripts");
    addCategory("programming", "control");

    includeServiceInOneJar(true);
    addDependency("org.python", "jython-standalone", "2.7.2");

  }

}
