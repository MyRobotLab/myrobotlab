package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class OpenWeatherMapMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenWeatherMapMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public OpenWeatherMapMeta() {

    addDescription("This service will query OpenWeatherMap for the current weather.  Get an API key at http://openweathermap.org/");
    addCategory("weather");
    setCloudService(true);
    addDependency("org.json", "json", "20230227");

  }

}
