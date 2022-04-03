package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class RasPiMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(RasPiMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public RasPiMeta() {

    addDescription("Raspberry Pi service used for accessing specific RasPi hardware like th GPIO pins and i2c");
    addCategory("i2c", "control");
    setSponsor("Mats");
    addDependency("com.pi4j", "pi4j-core", "1.4");
    addDependency("com.pi4j", "pi4j-native", "1.4", "pom");

  }

}
