package org.myrobotlab.framework;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.RuntimeConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;

/**
 * 
 * @author GroG
 * 
 *         Plans are processed by runtime as requests for future states. They
 *         can be complex multi service multi configuration with overrides and
 *         different peer names, but ultimately its a request to runtime for
 *         services and configuration in the desired "planned" state
 *
 */
public class Plan {
  /**
   * the root of the tree of configuration
   */
  final String name;

  public final static Logger log = LoggerFactory.getLogger(Plan.class);

  LinkedHashMap<String, ServiceConfig> config = new LinkedHashMap<>();

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
   * Puts a service name and its service config into the plan, replacing any
   * previous definition. It WILL NOT replace a runtime config !
   * 
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

    // modify runtime config registry for starting services
    RuntimeConfig rt = (RuntimeConfig) get("runtime");
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
   * Sometimes composite services need to remove default config put in place by
   * its peers. This method removes unwanted default config
   * 
   * @param actualName
   * @return
   */
  public ServiceConfig remove(String actualName) {
    return config.remove(actualName);
  }

  public boolean containsKey(String name) {
    return config.containsKey(name);
  }

  public LinkedHashMap<String, ServiceConfig> getConfig() {
    return config;
  }

  public int size() {
    return config.size();
  }

  /**
   * a way to remove a service from auto starting
   * 
   * @param service
   */
  public void removeRegistry(String service) {
    RuntimeConfig runtime = (RuntimeConfig) config.get("runtime");
    if (runtime == null) {
      log.error("removeRegistry - runtime null !");
      return;
    }
    runtime.remove(service);
  }

  /**
   * a way to add to a request services to start
   * 
   * @param actualName
   */
  public void addRegistry(String actualName) {
    RuntimeConfig runtime = (RuntimeConfig) config.get("runtime");
    if (runtime == null) {
      log.error("removeRegistry - runtime null !");
      return;
    }
    runtime.add(actualName);
  }

}
