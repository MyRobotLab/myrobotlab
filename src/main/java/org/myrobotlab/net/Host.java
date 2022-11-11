package org.myrobotlab.net;

public class Host {

  public String name;
  public String ip;
  public String mac;
  public String state; // online | offline
  public String description;
  public Long lastActiveTs;

  @Override
  public String toString() {
    return String.format("%s %s %s %d", ip, name, state, lastActiveTs);
  }

}
