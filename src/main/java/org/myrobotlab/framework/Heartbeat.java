package org.myrobotlab.framework;

import java.util.List;

/**
 * 
 * @author GroG
 * 
 * The purpose of this data is to broadcast from a process the general
 * health and state, like the number and types of services currently running
 *
 */
public class Heartbeat {

 
  long ts = System.currentTimeMillis();
  String id;
  String name;
  List<Registration> serviceList;
  
  public Heartbeat(String name, String id, List<Registration> serviceList) {
    this.id = id;
    this.name = name;
    this.serviceList = serviceList;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Registration r : serviceList) {
      sb.append(r.name);
      sb.append(" ");
    }
    return String.format("%s@%s - %s", name, id, sb);
  }

}
