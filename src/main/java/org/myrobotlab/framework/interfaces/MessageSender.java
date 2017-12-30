package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Message;

public interface MessageSender extends NameProvider {

  /**
   * Send invoking messages to remote location to invoke
   * {name} instance's {method} with no parameters.
   * 
   * @param name - name of destination service
   * @param method - method of destination service
   */
  public void send(String name, String method);

  /**
   * Send invoking messages to remote location to invoke
   * {name} instance's {method} with parameters data.
   * 
   * @param name - name of destination service
   * @param method - method of destination service
   * @param data - parameter data
   */
  public void send(String name, String method, Object... data);
  
  /**
   * Base method for sending messages.
   * 
   * @param msg - message to be sent
   */
  public void send(Message msg);

  public Object sendBlocking(String name, String method);

  public Object sendBlocking(String name, String method, Object... data);  

  public Object sendBlocking(String name, Integer timeout, String method, Object... data);
  
  public Object sendBlocking(Message msg, Integer timeout);

}
