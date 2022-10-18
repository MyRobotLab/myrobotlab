package org.myrobotlab.framework;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Plan {
  /**
   * the root of the tree of configuration
   */
  final String name;

  public final static Logger log = LoggerFactory.getLogger(Plan.class);

  LinkedHashMap<String, ServiceConfig> config = new LinkedHashMap<>();

  @Deprecated /* use definition in config and contract of String fieldnames */
  public Map<String, Map<String, ServiceReservation>> peers = new TreeMap<String, Map<String, ServiceReservation>>();

  // final MetaData metaData;

  public Plan(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("\n");
    for (Map.Entry<String, ServiceConfig> e : config.entrySet()) {
      sb.append(e.getKey()).append("=").append(e.getValue()).append("\n");
    }
    return sb.toString();
  }

  public ServiceConfig put(String name, ServiceConfig sc) {
    return config.put(name, sc);
  }

  public void putAll(Map<String, ServiceConfig> c) {
    config.putAll(c);
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
    config.clear();
  }

  public void setConfig(LinkedHashMap<String, ServiceConfig> config) {
    this.config = config;
  }

  public void merge(Plan ret) {
    merge(ret, null);
  }

  public void merge(Plan ret, Boolean replaceMatching) {
    if (ret == null) {
      return;
    }

    if (replaceMatching == null) {
      replaceMatching = false;
    }

    for (String peerName : ret.peers.keySet()) {
      if (replaceMatching || !peers.containsKey(peerName)) {
        peers.put(peerName, ret.peers.get(peerName));
      }
    }

    for (String peerName : ret.keySet()) {
      if (replaceMatching || !config.containsKey(peerName)) {
        config.put(peerName, ret.get(peerName));
      }
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
    return config.put(name, sc);
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
