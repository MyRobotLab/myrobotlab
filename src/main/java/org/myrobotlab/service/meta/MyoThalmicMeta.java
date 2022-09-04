package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class MyoThalmicMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MyoThalmicMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public MyoThalmicMeta() {

    addDescription("Myo service to control with the Myo armband");
    addCategory("control", "sensors");

    addDependency("com.github.nicholasastuart", "myo-java", "0.9.1");

  }

}
