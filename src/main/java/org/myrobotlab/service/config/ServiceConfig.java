package org.myrobotlab.service.config;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Peer;
import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

/**
 * Base service configuration class. All services must have a type. The name of
 * the service config file implies the name of the service.
 * 
 * FIXME - make a 2 param constructor that makes the "default" callback name
 *
 */
public class ServiceConfig {

  public final static Logger log = LoggerFactory.getLogger(ServiceConfig.class);
  
  
  public static class Listener {
    
    public Listener() {
    }
    
    public Listener(String method, String listener, String callback) {
      this.method = method;
      this.listener = listener;
      this.callback = callback;
    }
    
    public Listener(String method, String listener) {
      this.method = method;
      this.listener = listener;
      this.callback = CodecUtils.getCallbackTopicName(method);
    }


    public String method;

    /**
     * globally unique name of Service the a topic message will be sent to
     */
    public String listener;

    /**
     * the method which will be invoked
     */
    public String callback;
    
    

    @Override
    final public int hashCode() {
      return 37 + method.hashCode() + listener.hashCode() + callback.hashCode();
    }

    @Override
    public String toString() {
      return String.format("%s -will activate-> %s.%s", method, listener, callback);
    }

  }
  

  /**
   * simple type name of service defined for this config
   */
  public String type;
  

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
  // public Map<String, Peer> peers = new TreeMap<>();
  public Map<String, Peer> peers = null;
  public  List<Listener> listeners = null;
                                           

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

  public Map<String, Peer> getPeers() {
    if (peers == null) {
      return new TreeMap<>();
    }
    return peers;
  }

  public Peer getPeer(String peerKey) {
    if (peers == null || peers.get(peerKey) == null) {
      log.error("{} not found in peer keys - possible keys follow:", peerKey);
      return null;
    }
    return peers.get(peerKey);
  }
  
  public String getPeerName(String peerKey) {    
    if (peers == null || getPeer(peerKey) == null) {
      return null;
    }
    return getPeer(peerKey).name;
  }


  // FIXME - need an autostart param
  // public ServiceConfig addDefaultPeerConfig(Plan plan, String name, String key, Peer peer) {
  
  public ServiceConfig addDefaultPeerConfig(Plan plan, String name, String key, String peerType) {
    return addDefaultPeerConfig(plan, name, key, peerType, true);
  }
  
  public ServiceConfig addDefaultPeerConfig(Plan plan, String name, String key, String peerType, boolean autoStart) {
    ServiceConfig sc = addDefaultGlobalConfig(plan, key, String.format("%s.%s", name, key), peerType, autoStart);
    return sc;
  }
  
  public ServiceConfig addDefaultGlobalConfig(Plan plan, String key, String globalName, String peerType) {
    return addDefaultGlobalConfig(plan, key, globalName, peerType, true);
  }


  /**
   * Used typically for shared services.  E.g. headTracking and eyeTracking share the same OpenCV instance.
   * So, the peer of headTracking and eyeTracking global name for the cv key would be "i01.cv"
   * @param plan
   * @param key
   * @param globalName
   * @param peer
   * @return
   */
  public ServiceConfig addDefaultGlobalConfig(Plan plan, String key, String globalName, String peerType, boolean autoStart) {
    Peer peer = new Peer(globalName, peerType, autoStart);
    // recursive
    ServiceConfig.getDefault(plan, peer.name, peer.type);
    ServiceConfig sc = plan.get(peer.name);
    if (peers == null) {
      peers = new TreeMap<>();
    }
    peers.put(key, peer);
    return sc;
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
  
  public Peer putPeerType(String peerKey, String fullName, String peerType) {
    if (peers == null) {
      peers = new TreeMap<>();
    }
    Peer peer = peers.get(peerKey);
    if (peer == null) {
      peer = new Peer(fullName, peerType);
    } else {
      peer.type = peerType;
    }    
    peers.put(peerKey, peer);
    return peer;
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
    } catch (Exception e) {
      Runtime.getInstance().error(e);
    }

    return plan;
  }

  public static ServiceConfig getDefaultServiceConfig(String type) {
    // Plan plan = getDefault(Runtime.getPlan(), type.toLowerCase(), type);
    // FIXED - do not modify current plan - this is only to get deps on 
    // install
    Plan plan = getDefault(new Plan("runtime"), type.toLowerCase(), type);
    return plan.get(type.toLowerCase());
  }
}
