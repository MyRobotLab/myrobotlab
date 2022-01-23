package org.myrobotlab.service.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * Base service configuration class. All services must have a type. The name of
 * the service config file implies the name of the service.
 *
 */
public class ServiceConfig {
  
  transient public final static Logger log = LoggerFactory.getLogger(ServiceConfig.class);

  /**
   * type of service defined for this config
   */
  public String type;

  // public String name; I DO NOT WANT TO PUT THIS IN

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

  static public Map<String, ServiceConfig> getDefault(String name, String type) {
    Map<String, ServiceConfig> config = new LinkedHashMap<>();
    try {
      
      String configClass = "org.myrobotlab.service.config." + type + "Config";
      
      Class<?> clazz = Class.forName(configClass);
      Method method = clazz.getMethod("getDefault", String.class);
      
      // create new instance
      Constructor<?> ctor = clazz.getConstructor();
      Object configObject = ctor.newInstance();
      
      // I chose "non"-static method for getDefault - because Java has
      // an irritating rule of not allowing static overloads and abstracts
      config = (Map<String, ServiceConfig>)method.invoke(configObject, name);

      if (config == null || config.keySet().size() == 0) {
          log.warn("{} does not currently have any default configurations", configClass);
      }
      
    } catch (Exception e) {
      log.error("ServiceConfig.getDefault({},{}) threw", name, type, e);
    }
    return config;
  }
  
  public Map<String, ServiceConfig> getDefault(String name) {
    Map<String, ServiceConfig> config = new LinkedHashMap<>();
    config.put(name, this);
    return config; 
  }

}
