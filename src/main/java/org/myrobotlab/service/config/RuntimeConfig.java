package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.service.data.Locale;

public class RuntimeConfig extends ServiceConfig {

  /**
   * instance id - important to be unique when connecting multiple
   * mrl instances together
   */
  public String id;
  
  /**
   * virtual hardware if enabled all services created will enable virtualization if applicable
   */
  public Boolean virtual = false;
  public boolean enableCli = true;
  public String logLevel = "info";
  public String locale;
  
  // NEED THIS PRIVATE BUT CANNOT BE
  public List<String> registry = new ArrayList<>();
    
  /**
   * Root of resource location
   */
  public String resource = "resource";
  
  
  public RuntimeConfig() {
    if (locale == null) {
      locale = Locale.getDefault().getTag();
    }
  }
  

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

  public boolean remove(String name) {
    return registry.remove(name);
  }
  
  public void removeStartsWith(String startsWith) {
    registry.removeIf(n -> n.startsWith(startsWith));
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
