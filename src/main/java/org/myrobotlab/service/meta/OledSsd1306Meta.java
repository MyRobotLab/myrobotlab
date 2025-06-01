package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class OledSsd1306Meta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OledSsd1306Meta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public OledSsd1306Meta() {

    addDescription("OLED driver using SSD1306 driver and the i2c protocol");
    addCategory("i2c", "control");
    setAvailable(true);
    setSponsor("Mats");

  }

}
