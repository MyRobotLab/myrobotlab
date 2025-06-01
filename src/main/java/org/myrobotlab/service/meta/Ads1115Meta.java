package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Ads1115Meta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Ads1115Meta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   */
  public Ads1115Meta() {
    addDescription("a higher-precision analog to digital converter 16-bit");
    setLicenseApache();
    addCategory("shield", "sensors", "i2c");
    setSponsor("Mats");
  }

}
