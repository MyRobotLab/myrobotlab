package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.StaticType;
import org.myrobotlab.framework.TimeoutException;

public interface MessageSender extends NameProvider, SimpleMessageSender {

  /**
   * Send invoking messages to remote location to invoke {name} instance's
   * {method} with no parameters.
   * 
   * @param name
   *          - name of destination service
   * @param method
   *          - method of destination service
   */
  void send(String name, String method);

  /**
   * Send invoking messages to remote location to invoke {name} instance's
   * {method} with parameters data.
   * 
   * @param name
   *          - name of destination service
   * @param method
   *          - method of destination service
   * @param data
   *          - parameter data
   */
  void send(String name, String method, Object... data);

  /**
   * Base method for sending messages.
   * 
   * @param msg
   *          - message to be sent
   */
  void send(Message msg);

  default Object sendBlocking(String name, String method) throws InterruptedException, TimeoutException {
    return sendBlocking(name, method, new StaticType<>() {});
  }

  <R> R sendBlocking(String name, String method, StaticType<R> returnType) throws InterruptedException, TimeoutException;

  default Object sendBlocking(String name, String method, Object... data) throws InterruptedException, TimeoutException {
    return sendBlocking(name, method, new StaticType<>(){}, data);
  }

  <R> R sendBlocking(String name, String method, StaticType<R> returnType, Object... data) throws InterruptedException, TimeoutException;

  default Object sendBlocking(String name, Integer timeout, String method, Object... data) throws InterruptedException, TimeoutException {
    return sendBlocking(name, timeout, method, new StaticType<>(){}, data);
  }

  <R> R sendBlocking(String name, Integer timeout, String method, StaticType<R> returnType, Object... data) throws InterruptedException, TimeoutException;

  default Object sendBlocking(Message msg, Integer timeout) throws InterruptedException, TimeoutException {
    return sendBlocking(msg, timeout, new StaticType<>(){});
  }

  <R> R sendBlocking(Message msg, Integer timeout, StaticType<R> returnType) throws InterruptedException, TimeoutException;

  default Object waitFor(String fullName, String method, Integer timeout) throws InterruptedException, TimeoutException {
    return waitFor(fullName, method, timeout, new StaticType<>() {});
  }

  <R> R waitFor(String fullName, String method, Integer timeout, StaticType<R> returnType) throws InterruptedException, TimeoutException;

}
