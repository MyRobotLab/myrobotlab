package org.myrobotlab.framework;

public class Peer {

  public String name;
  public String type;
  public boolean autoStart = true;
  
  public Peer() {    
  }

  public Peer(String name, String type) {
    this.name = name;
    this.type = type;
  }

  public Peer(String name, String type, boolean autoStart) {
    this.name = name;
    this.type = type;
    this.autoStart = autoStart;    
  }
  
  public String toString() {
    return String.format("peer %s %s %b", name, type, autoStart);
  }
}
