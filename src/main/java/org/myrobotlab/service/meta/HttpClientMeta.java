package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class HttpClientMeta {
  public final static Logger log = LoggerFactory.getLogger(HttpClientMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.HttpClient");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("a general purpose http client, used to fetch information on the web");
    meta.addCategory("network");
    return meta;
  }
  
}

