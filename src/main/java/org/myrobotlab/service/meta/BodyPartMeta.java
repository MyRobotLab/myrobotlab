package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class BodyPartMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(BodyPartMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public BodyPartMeta() {
    addDescription("An easier way to control a body ...");
    addCategory("robot");
    setAvailable(true);
    addDependency("org.apache.commons", "commons-lang3", "3.3.2");
  }

}
