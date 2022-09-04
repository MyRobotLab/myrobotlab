package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class SwingGuiMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(SwingGuiMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public SwingGuiMeta() {

    addDescription("Service used to graphically display and control other services");
    addCategory("display");

    includeServiceInOneJar(true);
    addDependency("com.fifesoft", "rsyntaxtextarea", "2.0.5.1");
    addDependency("com.fifesoft", "autocomplete", "2.0.5.1");
    addDependency("com.jidesoft", "jide-oss", "3.6.18");
    addDependency("com.mxgraph", "jgraphx", "1.12.0.2");

  }

}
