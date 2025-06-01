package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.myrobotlab.framework.NameGenerator;
import org.myrobotlab.framework.Plan;
import org.myrobotlab.service.data.Locale;

public class RuntimeConfig extends ServiceConfig {

  /**
   * instance id - important to be unique when connecting multiple
   * mrl instances together
   */
  public String id = NameGenerator.getName();
  
  /**
   * virtual hardware if enabled all services created will enable virtualization if applicable
   */
  public Boolean virtual = false;
    
  /**
   * Log level debug, info, warn, error
   */
  public String logLevel = "info";
  
  /**
   * Locale setting for the instance, initial default will be set by the default jvm/os
   * through java.util.Locale.getDefault()
   */
  public String locale = Locale.getDefault().getTag();
  

  /**
   * Although this should be a set of unique services, it cannot be a LinkedHashSet
   * because SnakeYml's interpretation would be a map with null values.  Instead
   * its a protected member with accessors that prevent duplicates.
   */
  public List<String> registry = new ArrayList<>();
    
  /**
   * Root of resource location
   */
  public String resource = "resource";
  
  
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    return plan;
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
