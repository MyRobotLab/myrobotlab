package org.myrobotlab.service.config;

import java.util.Map;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * Base service configuration class. All services must have a type. The name of
 * the service config file implies the name of the service.
 *
 */
public class ServiceConfig {
  
  transient public final static Logger log = LoggerFactory.getLogger(ServiceConfig.class);

  /**
   * type of service defined for this config
   */
  public String type;
  
  // FIXME - change to enum !
  // heh non transient makes it easy to debug !
  transient public String state = "INIT"; // INIT | LOADED | CREATED | STARTED | STOPPED | RELEASED

  // public String name; I DO NOT WANT TO PUT THIS IN
  
  /**
   * if this service has peers - auto start them / and autoRelease them
   */
  public boolean autoStartPeers = true;
  
  /**
   * if this service is a peer - autostart it first when creating its parent
   */
  // public boolean autoStart = true;

  public ServiceConfig() {
    String configTypeName = this.getClass().getSimpleName();
    String serviceType = configTypeName.substring(0, configTypeName.length() - "Config".length());

    /**
     * this is more a immutable "label" than config because most of the time it
     * wouldn't make sense to switch configuration with a different service type
     * but it is easy to look at for a human, and easy to use when runtime is
     * starting up services
     */
    type = serviceType;
  }


}
