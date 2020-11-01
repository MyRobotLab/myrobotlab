package org.myrobotlab.net;

public class RouteEntry {
  
  public String destination;
  public String uuid; // interface
  public String flags;
  public int metric;
  
  public RouteEntry(String route, String gateway, int metric) {
    this.destination = route;
    this.uuid = gateway;
    this.metric = metric;
  }
}
