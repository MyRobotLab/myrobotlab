package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Message;

public interface Invoker {
  public Object invoke(Message msg);

  public Object invoke(String method);

  public Object invoke(String method, Object... params);

  public Object invokeOn(Object obj, String method, Object... params);
}
