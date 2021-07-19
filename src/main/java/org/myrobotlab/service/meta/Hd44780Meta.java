package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Hd44780Meta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Hd44780Meta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * @param name n
   * 
   */
  public Hd44780Meta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("I2C LCD Display driver");
    addCategory("i2c", "display");

  }

}
