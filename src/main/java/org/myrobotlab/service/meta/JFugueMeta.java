package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class JFugueMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(JFugueMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public JFugueMeta() {

    addDescription("service wrapping Jfugue, used for music and sound generation");
    addCategory("sound", "music");
    // org="jfugue" name="jfugue" rev="5.0.7
    addDependency("jfugue", "jfugue", "5.0.7");

  }

}
