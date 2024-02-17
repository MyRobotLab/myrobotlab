package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Message;

public interface MessageInvoker {
  /**
   * Invoke a method on a service with a Message POPO, typical internal 
   * messaging used from dequing messages from the inbox
   * 
   * @param msg - message to invoke
   * @return - returned value
   */
  public Object invoke(Message msg);

  /**
   * Invoke a method on a service with params.
   * 
   * @param blockLocally - if true, the method will block until the method is done processing
   * @param obj - the object which the method is invoked on
   * @param method - the method to invoke
   * @param params - the parameters to pass to the method
   * @return - the return value of the method
   */
  public Object invokeOn(boolean blockLocally, Object obj, String method, Object... params);

}
