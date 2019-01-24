package org.myrobotlab.jme3;

/**
 * class for doing manipulations of the scene graph with the JME thread do not
 * call its methods directly
 */
public class Jme3Msg {
  
  String method;
  Object data[];
  
  public Jme3Msg(String method, Object[] params) {
    this.method = method;
    this.data = params;
  }
}
