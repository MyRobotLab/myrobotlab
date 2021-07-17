package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class AdafruitIna219Meta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(AdafruitIna219Meta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * @param name n
   */
  public AdafruitIna219Meta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("measures voltage and current of a circuit");
    setLicenseApache();
    addCategory("shield", "sensors", "i2c");
    setSponsor("Mats");

  }

}
