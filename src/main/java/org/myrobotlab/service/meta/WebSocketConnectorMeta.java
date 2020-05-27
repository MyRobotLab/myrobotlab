package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class WebSocketConnectorMeta {
  public final static Logger log = LoggerFactory.getLogger(WebSocketConnectorMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.WebSocketConnector");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("connect to a websocket");
    // meta.addCategory("");
    meta.addDependency("javax.websocket", "javax.websocket-api", "1.1");
    /*
    meta.addDependency("org.glassfish.tyrus", "tyrus-client", "1.1");
    meta.addDependency("org.glassfish.tyrus", "tyrus-container-grizzly", "1.1");
    */
    return meta;
  }

  
  
}

