package org.myrobotlab.framework.interfaces;

public interface MessageSubscriber {

  /**
   * This will subscribe to a NameProviders method. The callback will be
   * automatically generated.  Rules are publish{Method} or get{Method} will
   * callback with on{Method}.
   * 
   * @param service - target service name
   * @param method - method of interest
   */
  public void subscribe(NameProvider service, String method);

  /**
   * Service name is supplied and method to subscribe to.  The callback will be
   * automatically generated.  Rules are publish{Method} or get{Method} 
   * will callback with on{Method}.
   * 
   * @param service - target service name
   * @param method - method of interest
   */
  public void subscribe(String service, String method);

  /**
   * Subscribe with callback.  The callback is explicitly set.
   * @param service - target service name
   * @param method - method of interest
   * @param callback - callback method name
   */
  public void subscribe(String service, String method, String callback);

  /***
   * Unsubscribe from a NameProviders method. 
   * @param service - target service name
   * @param method - method of interest
   */
  public void unsubscribe(NameProvider service, String method);

  /**
   * Unsubscribe from a service method.
   * @param service - target service name
   * @param method - method of interest
   */
  public void unsubscribe(String service, String method);

  /**
   * Unsubscribe from a service method with an explicit callback.
   * @param service - target service name
   * @param method - method of interest
   * @param callback - callback method name
   */
  public void unsubscribe(String service, String method, String callback);

}
