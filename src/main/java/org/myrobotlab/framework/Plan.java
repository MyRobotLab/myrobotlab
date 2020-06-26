package org.myrobotlab.framework;

import java.util.Map;
import java.util.TreeMap;

/**
 * 
 * A convenient wrapper to show what peers and services "could"
 * be created depending on current MetaData and ServiceData overrides
 *
 */
public class Plan {
  /**
   * Map<{actualName}, {Type}>
   */
  Map<String, String> services = new TreeMap<>();
  
  public String toString() {
    StringBuilder sb = new StringBuilder("\n");
    for(Map.Entry<String,String> e : services.entrySet()) {
      sb.append(e.getKey()).append("=").append(e.getValue()).append("\n");
    }
    return sb.toString();
  }

  public void put(String serviceName, String serviceType) {
    services.put(serviceName, serviceType);
  }

  public String get(String serviceName) {    
    return services.get(serviceName);
  }
  
}
