package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class MobilePlatformMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MobilePlatformMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public MobilePlatformMeta() {

    addDescription(
        "used to encapsulate many of the functions and formulas regarding 2 motor platforms encoders and other feedback mechanisms can be added to provide heading, location and other information");
    addCategory("robot", "control");
    setAvailable(false);

  }

}
