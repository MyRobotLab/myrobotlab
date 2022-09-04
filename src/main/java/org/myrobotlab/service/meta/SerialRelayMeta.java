package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class SerialRelayMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(SerialRelayMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public SerialRelayMeta() {

    addDescription("Relaying Serial data to a different serial port on mega Arduino");
    setAvailable(false); // false if you do not want it viewable in a gui
    // add dependency if necessary
    // addDependency("org.coolproject", "1.0.0");
    addCategory("general");

  }

}
