package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class GpsMeta {
  public final static Logger log = LoggerFactory.getLogger(GpsMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Gps");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("parses NMEA sentences coming in over a Serial service");
    meta.addCategory("location", "sensors");
    meta.addPeer("serial", "Serial", "serial port for GPS");
    meta.setLicenseApache();

    return meta;
  }

  
}

