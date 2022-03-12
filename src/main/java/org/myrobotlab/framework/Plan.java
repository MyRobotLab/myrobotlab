package org.myrobotlab.framework;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;

public class Plan {
  /**
   * the root of the tree of configuration
   */
  String root;

  LinkedHashMap<String, ServiceConfig> config = new LinkedHashMap<>();

  final MetaData metaData;

  public Plan(MetaData metaData) {
    this.metaData = metaData;
  }

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

    for (String peerName : ret.metaData.peers.keySet()) {
      if (replaceMatching || !metaData.peers.containsKey(peerName)) {
        metaData.peers.put(peerName, ret.metaData.peers.get(peerName));
      }
    }
    
    
    for (String peerName : ret.keySet()) {
      if (replaceMatching || !config.containsKey(peerName)) {
        config.put(peerName, ret.get(peerName));
      }
    }
  }

  public String getPath(String peerKey) {
    if (root == null) {
      return peerKey;
    } else {
      return root + "." + peerKey;
    }
  }

  public ServiceConfig getPeer(String name, String type, Boolean autoStart) {
    Plan plan = MetaData.getDefault(name, type, autoStart);
    // merge config - do not replace root
    merge(plan);
    return config.get(name);
  }

  // public ServiceConfig addPeer(String peerKey, String type) {
  // Plan plan = MetaData.getDefault(getPath(peerKey), type);
  // // merge config - do not replace root
  // mergeConfig(plan.config);
  // return config.get(getPath(peerKey));
  // }

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

  public ServiceConfig getPeer(String key) {
    // TODO Auto-generated method stub
    return null;
  }

}
