package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class PingdarMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(PingdarMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public PingdarMeta() {
    addDescription("used as a ultra sonic radar");
    addCategory("sensors", "display");

    // theoretically - Servo should follow the same share config
    // sharePeer("servo.controller", "controller", "Arduino", "shared
    // arduino");

  }

}
