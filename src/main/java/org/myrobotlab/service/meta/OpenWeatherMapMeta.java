package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenWeatherMapMeta {
  public final static Logger log = LoggerFactory.getLogger(OpenWeatherMapMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.OpenWeatherMap");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("This service will query OpenWeatherMap for the current weather.  Get an API key at http://openweathermap.org/");
    meta.addCategory("weather");
    meta.setCloudService(true);
    meta.addDependency("org.json", "json", "20090211");
    meta.addPeer("httpClient", "HttpClient", "httpClient");
    return meta;
  }
  
  
}

