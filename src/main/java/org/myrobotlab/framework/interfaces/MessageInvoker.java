package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Message;

public interface MessageInvoker {
  public Object invoke(Message msg);

  public Object invokeOn(boolean blockLocally, Object obj, String method, Object... params);

}
