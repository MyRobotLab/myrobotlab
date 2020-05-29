package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OledSsd1306Meta {
  public final static Logger log = LoggerFactory.getLogger(OledSsd1306Meta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.OledSsd1306");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("OLED driver using SSD1306 driver and the i2c protocol");
    meta.addCategory("i2c", "control");
    meta.setAvailable(true);
    meta.setSponsor("Mats");
    return meta;
  }
  
  
}

