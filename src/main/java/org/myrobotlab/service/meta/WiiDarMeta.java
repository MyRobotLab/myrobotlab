package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WiiDarMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WiiDarMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public WiiDarMeta() {

    addDescription("WiiDar.... who dar?  WiiDar!");
    addDependency("wiiusej", "wiiusej", "0.0.1");
    addCategory("sensors");
    // no longer have hardware for this ...
    setAvailable(false);

  }

}
