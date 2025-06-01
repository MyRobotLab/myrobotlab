package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class VirtualDeviceMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(VirtualDeviceMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public VirtualDeviceMeta() {

    addDescription("a service which can create virtual serial ports and behaviors implemented in python for them");
    addCategory("testing");

    // this is used for testing, and does not need to be tested
    setAvailable(false);

  }

}
