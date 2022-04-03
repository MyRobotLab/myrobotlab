package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class I2cMuxMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(I2cMuxMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public I2cMuxMeta() {
    addDescription("Multiplexer for i2c to be able to use multiple i2c devices");
    addCategory("i2c", "control");
    setAvailable(true);
    setSponsor("Mats");

  }

}
