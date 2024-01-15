package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.StaticType;

public interface MessageInvoker {
  /**
   * Invoke a method on a service with a Message POPO, typical internal 
   * messaging used from dequing messages from the inbox
   * 
   * @param msg
   * @return
   */
  default Object invoke(Message msg) {
    return invoke(msg, new StaticType<>(){});
  }

  <R> R invoke(Message msg, StaticType<R> returnType);

  /**
   * Invoke a method on a service with params.
   * 
   * @param blockLocally - if true, the method will block until the method is done processing
   * @param obj - the object which the method is invoked on
   * @param method - the method to invoke
   * @param params - the parameters to pass to the method
   * @return - the return value of the method
   */
  default Object invokeOn(boolean blockLocally, Object obj, String method, Object... params) {
    return invokeOn(blockLocally, obj, method, new StaticType<>(){}, params);
  }

  <R> R invokeOn(boolean blockLocally, Object obj, String method, StaticType<R> returnType, Object... params);

}
