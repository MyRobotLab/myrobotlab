package org.myrobotlab.framework;

public class NameAndType {
 
  public String id;
  public String name;
  public String type;
  
  public NameAndType(String id, String name, String type) {
    this.id = id;
    this.name = name;
    this.type = type;
  }
  
  public String toString() {
    return String.format("%s %s %s",  id, name, type);
  }
}
