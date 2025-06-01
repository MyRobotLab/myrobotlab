package org.myrobotlab.framework.interfaces;

public interface Invoker {

  public Object invoke(String method);

  public Object invoke(String method, Object... params);

}
