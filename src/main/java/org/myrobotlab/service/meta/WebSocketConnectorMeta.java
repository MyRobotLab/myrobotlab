package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WebSocketConnectorMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WebSocketConnectorMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public WebSocketConnectorMeta() {

    addDescription("connect to a websocket");
    // addCategory("");
    addDependency("javax.websocket", "javax.websocket-api", "1.1");
    /*
     * addDependency("org.glassfish.tyrus", "tyrus-client", "1.1");
     * addDependency("org.glassfish.tyrus", "tyrus-container-grizzly", "1.1");
     */

  }

}
