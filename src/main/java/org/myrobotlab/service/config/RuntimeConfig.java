package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RuntimeConfig extends ServiceConfig {

  // public String id; Not ready to process this ... yet
  public Boolean virtual = null;
  public boolean enableCli = true;
  public String logLevel;
  public String locale;
  public List<String> registry = new ArrayList<>();

  transient private Set<String> registrySet = new LinkedHashSet<>();

  /**
   * add and remove a service using these methods and the uniqueness will be
   * preserved so there are no double entries in the registry list
   * 
   * @param name
   */
  public void add(String name) {
    if (!registrySet.contains(name)) {
      registry.add(name);
      registrySet.add(name);
    }
  }

  public void remove(String name) {
    registry.remove(name);
    registrySet.remove(name);
  }

  public void clear() {
    registry.clear();
    registrySet.clear();
  }
  
  public String toString() {
    return registry.toString();
  }

  public List<String> getRegistry() {
    return new ArrayList<>(registry);
  }

}
