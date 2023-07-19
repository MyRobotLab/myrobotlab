package org.myrobotlab.framework.interfaces;

public interface FutureInvoker {
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


}
