package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Message;

public interface Invoker {
  public Object invoke(Message msg);

  public Object invoke(String method);

  public Object invoke(String method, Object... params);

  public Object invokeOn(boolean blockLocally, Object obj, String method, Object... params);

  /**
   * Invoke method in the future without params and wait delayMs
   * 
   * @param method
   *          - method to be invoked
   * @param delayMs
   *          - the delay from "now" before invoking this method
   */
  public void invokeFuture(String method, long delayMs);

  /**
   * Invoke a function with parameters in the future with delay milliseconds
   * wait TODO - instead of return void return a Future ?
   * 
   * @param method
   *          - method name
   * @param delayMs
   *          - number of milliseconds to delay before executing this method
   * @param params
   *          - params for the method
   */
  public void invokeFuture(String method, long delayMs, Object... params);

  /**
   * checks if a named tasks exist
   * 
   * @param taskName
   *          - name of task
   * @return - returns task if defined
   */
  public boolean containsTask(String taskName);

}
