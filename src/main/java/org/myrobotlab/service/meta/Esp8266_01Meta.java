package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Esp8266_01Meta {
  public final static Logger log = LoggerFactory.getLogger(Esp8266_01Meta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Esp8266_01");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("ESP8266-01 service to communicate using WiFi and i2c");
    meta.addCategory("i2c", "control");
    meta.setSponsor("Mats");
    // FIXME - add HttpClient as a peer .. and use its interface .. :)
    // then remove direct dependencies to httpcomponents ...
    // One HttpClient to Rule them all !!
    /*
     * Runtime currently includes these dependencies
     * meta.addDependency("org.apache.httpcomponents", "httpclient", "4.5.2");
     * meta.addDependency("org.apache.httpcomponents", "httpcore", "4.4.6");
     */

    return meta;
  }

  
}

