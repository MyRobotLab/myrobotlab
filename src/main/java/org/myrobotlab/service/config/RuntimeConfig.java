package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

public class RuntimeConfig extends ServiceConfig {

  // public String id; Not ready to process this ... yet
  public Boolean virtual = null;
  public boolean enableCli = true;
  public String logLevel;
  public String locale;
  // NEED THIS PRIVATE BUT CANNOT BE
  public List<String> registry = new ArrayList<>();

  /**
   * add and remove a service using these methods and the uniqueness will be
   * preserved so there are no double entries in the registry list
   * 
   * @param name
   */
  synchronized public void add(String name) {
    if (name == null) {
      log.error("RuntimeConfig.add(null)");
      return;
    }
    for (String n : registry) {
      if (n.equals(name)) {
        return;
      }
    }
    registry.add(name);
  }

  public void remove(String name) {
    registry.remove(name);
  }

  public void clear() {
    registry.clear();
  }

  public String toString() {
    return registry.toString();
  }

  public List<String> getRegistry() {
    return new ArrayList<>(registry);
  }

}
