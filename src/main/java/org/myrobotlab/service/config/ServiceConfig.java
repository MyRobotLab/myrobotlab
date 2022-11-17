package org.myrobotlab.service.config;

import java.lang.reflect.Constructor;
import org.myrobotlab.service.Runtime;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.framework.ServiceReservation;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * Base service configuration class. All services must have a type. The name of
 * the service config file implies the name of the service.
 *
 */
public class ServiceConfig {

  public final static Logger log = LoggerFactory.getLogger(ServiceConfig.class);

  /**
   * simple type name of service defined for this config
   */
  public String type;

  // FIXME - change to enum !
  // heh non transient makes it easy to debug !
  transient public String state = "INIT"; // INIT | LOADED | CREATED | STARTED |
                                          // STOPPED | RELEASED
  // FIXME - SO IMPORTANT !

  public String getx(String key) {
    // FIXME - return reflected value
    return null;
  }

  public String getPath(String name, String peerKey) {
    if (name == null) {
      return peerKey;
    } else {
      return name + "." + peerKey;
    }
  }

  /**
   * key'd structure of other services that are necessary for the correct
   * function of this service can be modified with overrides before starting
   * named instance of this service
   */
  public Map<String, ServiceReservation> peers = null; // new TreeMap<String,
                                                       // ServiceReservation>();

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

  public Plan getDefault(Plan plan, String name) {
    plan.put(name, this);
    return plan;
  }

  ////////// migrated peer methods //////////////////////////////

  public Map<String, ServiceReservation> getPeers() {
    return peers;
  }

  public ServiceReservation getPeer(String peerKey) {
    if (peers.get(peerKey) == null) {
      log.warn("{} not found in peer keys - possible keys follow:", peerKey);
      for (String key : peers.keySet()) {
        log.info(key);
      }
    }
    return peers.get(peerKey);
  }

  public ServiceConfig addPeer(Plan plan, String name, String key, String actualName, String peerType) {
    return addPeer(plan, name, key, actualName, peerType, null);
  }

  public ServiceConfig addPeer(Plan plan, String name, String key, String actualName, String peerType, String comment) {
    if (peers == null) {
      peers = new TreeMap<String, ServiceReservation>();
    }
    
    if (actualName == null) {
      actualName = String.format("%s.%s", name, key);
    }
    // recursive !!!
    ServiceConfig.getDefault(plan, actualName, peerType);
    ServiceConfig sc = plan.get(actualName);
    // plan.put(actualName, sc); don't need to do this 
    peers.put(key, new ServiceReservation(key, actualName, peerType, comment));
    return sc;
  }

  protected void setPeerName(String key, String actualName) {
    // FIXME - do we bother to check if a peer exists or just make one? - we
    // don't have type info ...
    // FIXME - do we bother to check if its already set ? (merge ???)
    ServiceReservation sr = peers.get(key);
    log.error("key {} does not for peer", key);
  }

  static public String getConfigType(String type) {
    if (type.contains(".") && type.endsWith("Config")) {
      return type;
    }

    if (!type.contains(".") && !type.endsWith("Config")) {
      type = String.format("org.myrobotlab.service.config.%sConfig", type);
    } else {
      int pos = type.lastIndexOf(".");
      String serviceTypeName = type.substring(pos + 1);
      type = type.substring(0, pos) + ".config." + serviceTypeName + "Config";
    }
    return type;
  }

  public static Plan getDefault(Plan plan, String name, String inType) {
   
    try {

      // if (type == null) {
      // log.error("getDefault(null)");
      // return null;
      // }

      // FIXME - at some point setting, examining and changing
      // peer keys to actual names will need to be worky
      String fullType = getConfigType(inType);

      Class<?> c = Class.forName(fullType);
      Constructor<?> mc = c.getConstructor();
      ServiceConfig config = (ServiceConfig) mc.newInstance();
      // FIXME pass in plan
      // plan.merge();
      config.getDefault(plan, name);

    } catch (ClassNotFoundException e) {
      log.info("could not find {} loading generalized ServiceConfig", inType);
      ServiceConfig sc = new ServiceConfig();
      sc.type = inType;
      plan.put(name, sc);
    } catch(Exception e) {
      Runtime.getInstance().error(e);
    }

    return plan;
  }

  public static ServiceConfig getDefaultServiceConfig(String type) {
    Plan plan = getDefault(Runtime.getPlan(), type.toLowerCase(), type);
    return plan.get(type.toLowerCase());
  }
}
