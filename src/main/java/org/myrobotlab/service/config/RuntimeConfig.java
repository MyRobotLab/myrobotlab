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
  
  /**
   * Determines if stdin can be used for commands 
   */
  public boolean enableCli = true;
  
  /**
   * Log level debug, info, warning, error
   */
  public String logLevel = "info";
  
  /**
   * Locale setting for the instance, initial default will be set by the default jvm/os
   * through java.util.Locale.getDefault()
   */
  public String locale;
  

  /**
   * Although this should be a set of unique services, it cannot be a LinkedHashSet
   * because SnakeYml's interpretation would be a map with null values.  Instead
   * its a protected member with accessors that prevent duplicates.
   */
  protected List<String> registry = new ArrayList<>();
    
  /**
   * Root of resource location
   */
  public String resource = "resource";
  
  
  /**
   * Constructor sets the default locale if not already set.
   */
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
