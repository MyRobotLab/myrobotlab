package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Mpr121Meta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Mpr121Meta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public Mpr121Meta() {
    addDescription("MPR121 Touch sensor & LED Driver");
    addCategory("shield", "sensors", "i2c");
    setSponsor("Mats");
    setAvailable(true);

  }

}
