package org.myrobotlab.framework;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.RuntimeConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;
/**
 * 
 * @author GroG
 * 
 * Plans are processed by runtime as requests for future states.  They can
 * be complex multi service multi configuration with overrides and different peer names, but ultimately its
 * a request to runtime for services and configuration in the desired "planned" state
 *
 */
public class Plan {
  /**
   * the root of the tree of configuration
   */
  final String name;

  public final static Logger log = LoggerFactory.getLogger(Plan.class);

  LinkedHashMap<String, ServiceConfig> config = new LinkedHashMap<>();

  @Deprecated /* use definition in config and contract of String fieldnames */
  public Map<String, Map<String, ServiceReservation>> peers = new TreeMap<String, Map<String, ServiceReservation>>();

  public Plan(String rootName) {
    name = rootName;
    config.put("runtime", new RuntimeConfig());
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("\n");
    for (Map.Entry<String, ServiceConfig> e : config.entrySet()) {
      sb.append(e.getKey()).append("=").append(e.getValue()).append("\n");
    }
    return sb.toString();
  }

  /**
   * Puts a service name and its service config into the plan, replacing
   * any previous definition.  It WILL NOT replace a runtime config !
   * @param name
   * @param sc
   * @return
   */
  public ServiceConfig put(String name, ServiceConfig sc) {

    if (name.equals("runtime")) {
      // we do not replace root - we keep our root
      // and add their request to start
      log.info("request to replace root runtime ! - probably not what you want - not gonna do it");
      return sc;
    }
    
    RuntimeConfig rt = (RuntimeConfig)get("runtime");
    rt.add(name);
    return config.put(name, sc);
  }

  public ServiceConfig get(String name) {
    return config.get(name);
  }

  public Set<String> keySet() {
    // return a copy of the keyset to avoid concurrent modification
    Set<String> ret = new LinkedHashSet<>(config.keySet());
    return ret;
  }

  public void clear() {
    // rtConfig.clear();
    config.clear();
  }

  /**
   * Merges 2 plans. Puts/replaces all config with newPlan. The exception is
   * with runtime. All newPlan config is updated in this plan, but the registry
   * is merged separately. 
   * @param newPlan
   */
  public void merge(Plan newPlan) {
    if (newPlan == null) {
      return;
    }

    for (String peerName : newPlan.peers.keySet()) {
      peers.put(peerName, newPlan.peers.get(peerName));
    }

    // config.putAll(ret.config);
    for (String key: newPlan.keySet()) {
      if (key.equals("runtime")) {
        // skipping "our" runtime config and start list
        // FIXME - probably want to merge all other config "besides" registry
        // registry should always be merged - until then it will be hard to 
        // change runtime config from defaults
        
        RuntimeConfig rtConfig = (RuntimeConfig)get("runtime");
        RuntimeConfig newRtConfig = (RuntimeConfig)newPlan.get("runtime");
        for (String startService: newRtConfig.getRegistry()) {
          rtConfig.add(startService);
        }
        continue;
      }
      config.put(key, newPlan.get(key));
      // WRONG -> put(key, newPlan.get(key));      
    }
  }

  public String getPath(String peerKey) {
    if (name == null) {
      return peerKey;
    } else {
      return name + "." + peerKey;
    }
  }

  // NOTE ! - this uses actualName
  private ServiceConfig addConfig(String actualName, String type) {
    Plan plan = MetaData.getDefault(actualName, type);
    // merge config - do not replace root
    merge(plan);
    return config.get(actualName);
  }

  public ServiceConfig remove(String name) {
    return config.remove(name);
  }

  public boolean containsKey(String name) {
    return config.containsKey(name);
  }

  public LinkedHashMap<String, ServiceConfig> getConfig() {
    return config;
  }

  public ServiceConfig addPeer(String name, ServiceConfig sc) {
    config.put(name, sc);
    return sc;
  }

  // THIS WILL BUILD OUT DEFAULT CONFIG
  public void putPeers(String name, Map<String, ServiceReservation> peers) {
    this.peers.put(name, peers);
    if (peers != null) {
      for (String peerKey : peers.keySet()) {
        addPeerConfig(peerKey);
      }
    }
  }

  public void setPeerName(String peerKey, String actualName) {
    Map<String, ServiceReservation> myPeers = peers.get(name);
    if (myPeers == null) {
      log.error("setPeerName({},{}) but {} peers do not exist", peerKey, actualName, name);
      return;
    }
    ServiceReservation sr = myPeers.get(peerKey);
    if (sr == null) {
      log.error("setPeerName({},{}) but {} service reservation does not exist", peerKey, actualName, name);
      return;
    }
    sr.actualName = actualName;
  }

  public ServiceConfig addPeerConfig(String peerKey) {
    ServiceReservation sr = peers.get(name).get(peerKey);
    String actualName = null;
    if (sr == null) {
      log.error("%s key %s not found", name, peerKey);
      return null;
    }
    if (sr.actualName != null) {
      actualName = sr.actualName;
    } else {
      actualName = name + "." + peerKey;
    }
    return addConfig(actualName, sr.type);
  }

  public ServiceConfig addConfig(ServiceConfig sc) {
    // recently changed from return config.put(name, sc); to
    return put(name, sc);
  }

  public ServiceConfig getPeerConfig(String peerKey) {
    ServiceReservation sr = peers.get(name).get(peerKey);
    String actualName = null;
    if (sr == null) {
      log.error("%s key %s not found", name, peerKey);
      return null;
    }
    if (sr.actualName != null) {
      actualName = sr.actualName;
    } else {
      actualName = name + "." + peerKey;
    }
    return config.get(actualName);
  }

  public ServiceConfig removeConfig(String actualName) {
    return config.remove(actualName);
  }

  public Map<String, Map<String, ServiceReservation>> getPeers() {
    return peers;
  }

  public int size() {
    return config.size();
  }

}
