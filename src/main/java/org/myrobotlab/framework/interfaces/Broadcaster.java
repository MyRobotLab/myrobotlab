package org.myrobotlab.framework.interfaces;

public interface Broadcaster {

  public Object broadcast(String method);

  public Object broadcast(String method, Object... params);

}
