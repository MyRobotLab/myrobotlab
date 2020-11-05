package org.myrobotlab.net;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RouteTable {

  protected RouteEntry defaultRoute = null;

  protected Map<String, RouteEntry> routes = new HashMap<>();

  public void addRoute(String destination, String uuid, int metric) {
    if (routes.containsKey(destination)) {
      return;
    }
    RouteEntry r = new RouteEntry(destination, uuid, metric);
    /*
    if (defaultRoute == null || r.metric < defaultRoute.metric) {
      defaultRoute = r;
    }
    */
    // "latest" route strategy
    defaultRoute = r;
    routes.put(destination, r);
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
   * @param uuid
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
}
