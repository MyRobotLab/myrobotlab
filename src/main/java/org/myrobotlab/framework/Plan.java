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
 * Plans are processed by runtime as requests for future states. They can be
 * complex multi service multi configuration with overrides and different peer
 * names, but ultimately its a request to runtime for services and configuration
 * in the desired "planned" state
 *
 * @author GroG
 */
public class Plan {
  /**
   * the root of the tree of configuration - not currently used, but 
   * there is value in being able to create and save a plan, yet not execute it.
   * A "name" was thought might help in this endeavor, to identify unique "plans".
   * Not currently used.
   */
  final String name;

  public final static Logger log = LoggerFactory.getLogger(Plan.class);

  /**
   * The configuration for this plan
   */
  protected Map<String, ServiceConfig> config = new LinkedHashMap<>();

  public Plan(String rootName) {
    name = rootName;
    /**
     * A plan needs a default RuntimeConfig, a sort of bootstrap, since it will be
     * responsible for all other service life-cycles
     */
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
   * @param name - name of the service
   * @param sc - service config to be updated
   * @return - updated service config
   */
  public ServiceConfig put(String name, ServiceConfig sc) {

    if (name.equals("runtime") && get("runtime") != null && config.containsKey("runtime")) {
      // once runtime is set in a plan it will not be "replaced"
      // log.error("request to replace root runtime !");
      return sc;
    }
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
    config.clear();
  }

  /**
   * Sometimes composite services need to remove default config put in place by
   * its peers. This method removes unwanted default config
   * 
   * @param service
   *          - all plan names of services are "actual" names vs peerKeys. ie.
   *          These would be the names of the services you would see in the
   *          registry
   * @return - the service config it replaced
   */
  public ServiceConfig remove(String service) {
    RuntimeConfig rtConfig = (RuntimeConfig) config.get("runtime");
    rtConfig.registry.remove(service);
    return config.remove(service);
  }

  public boolean containsKey(String name) {
    return config.containsKey(name);
  }

  public Map<String, ServiceConfig> getConfig() {
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
   * @param service
   *          - all Plan names in the keySet are the names the services will
   *          have when started and put in the runtime registry
   */
  public void addRegistry(String service) {
    RuntimeConfig runtime = (RuntimeConfig) config.get("runtime");
    if (runtime == null) {
      log.error("removeRegistry - runtime null !");
      return;
    }
    runtime.add(service);
  }

  /**
   * good to prune trees of peers from starting - expecially if the peers
   * require re-configuring
   * 
   * @param startsWith
   *          - removes RuntimeConfig.registry all services that start with
   *          input
   */
  public void removeStartsWith(String startsWith) {
    RuntimeConfig runtime = (RuntimeConfig) config.get("runtime");
    if (runtime == null) {
      log.error("removeRegistry - runtime null !");
      return;
    }
    runtime.removeStartsWith(startsWith);
  }

}
