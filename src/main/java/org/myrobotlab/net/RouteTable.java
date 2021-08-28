package org.myrobotlab.net;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class RouteTable {

  public final static Logger log = LoggerFactory.getLogger(Runtime.class);

  protected RouteEntry defaultRoute = null;

  protected Map<String, RouteEntry> routes = new HashMap<>();
  protected Map<String, String> localGatewayKeysToUuid = new HashMap<>();

  public void addRoute(String destination, String uuid, int metric) {
    if (routes.containsKey(destination)) {
      return;
    }
    RouteEntry r = new RouteEntry(destination, uuid, metric);
    /*
     * if (defaultRoute == null || r.metric < defaultRoute.metric) {
     * defaultRoute = r; }
     */
    // "latest" route strategy
    log.info("adding route and setting default to {}", r);
    routes.put(destination, r);
    defaultRoute = r;
  }

  public boolean contains(String id) {
    return routes.containsKey(id);
  }

  public String getRoute(String id) {
    RouteEntry r = routes.get(id);
    if (r != null) {
      return r.uuid;
    }
    if (defaultRoute != null) {
      return defaultRoute.uuid;
    }
    return null;
  }

  /**
   * Thread safe removal of routes
   * 
   * @param uuid
   *          route id to remove.
   * 
   */
  public void removeRoute(String uuid) {
    Map<String, RouteEntry> newTable = new HashMap<>();
    for (RouteEntry r : routes.values()) {
      if (!uuid.equals(r.uuid)) {
        newTable.put(r.destination, r);
      }
    }
    routes = newTable;
  }

  public Set<String> getAllIdsFor(String uuid) {
    Set<String> ids = new HashSet<>();
    for (RouteEntry r : routes.values()) {
      if (uuid.equals(r.uuid)) {
        ids.add(r.destination);
      }
    }
    return ids;
  }

  public void addLocalGatewayKey(String localGatewayKey, String uuid) {
    localGatewayKeysToUuid.put(localGatewayKey, uuid);

  }

  public String getConnectionUuid(String localGatewayKey) {
    return localGatewayKeysToUuid.get(localGatewayKey);
  }

  public String remove(String localGatewayKey) {
    return localGatewayKeysToUuid.remove(localGatewayKey);
  }
}
