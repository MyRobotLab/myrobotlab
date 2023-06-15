package org.myrobotlab.framework.interfaces;

public interface MessageSubscriber {

  /**
   * This will subscribe to a NameProviders method. The callback will be
   * automatically generated.  Rules are publish{Method} or get{Method} will
   * callback with on{Method}.
   * 
   * @param service
   * @param method
   */
  public void subscribe(NameProvider service, String method);

  /**
   * Service name is supplied and method to subscribe to.  The callback will be
   * automatically generated.  Rules are publish{Method} or get{Method} 
   * will callback with on{Method}.
   * 
   * @param service
   * @param method
   */
  public void subscribe(String service, String method);

  /**
   * Subscribe with callback.  The callback is explicitly set.
   * @param service
   * @param method
   * @param callback
   */
  public void subscribe(String service, String method, String callback);

  /***
   * Unsubscribe from a NameProviders method. 
   * @param service
   * @param method
   */
  public void unsubscribe(NameProvider service, String method);

  /**
   * Unsubscribe from a service method.
   * @param service
   * @param method
   */
  public void unsubscribe(String service, String method);

  /**
   * Unsubscribe from a service method with an explicit callback.
   * @param service
   * @param method
   * @param callback
   */
  public void unsubscribe(String service, String method, String callback);

}
