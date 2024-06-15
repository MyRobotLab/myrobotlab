package org.myrobotlab.framework.interfaces;

import java.util.Set;

/**
 * A device which can be attached to a microcontroller implementers are Sensor
 * and Stepper - perhaps more not sure exactly what all it should implement -
 * but represents something which can be attached to a microcontroller
 * 
 * This is where all supported devices are defined. They all have a unique type
 * identifier which can be communicated to a microcontroller
 * 
 * It also binds how the microcontroller identifies its service (getIndex())
 * with the service (getName())
 *
 */
public interface Attachable extends NameProvider {

  /**
   * implementation of attaching a service
   * 
   * @param service
   *          the service to attach to
   * @throws Exception
   *           if an error occcurs
   * 
   */
  public void attach(Attachable service) throws Exception;

  /**
   * Explicit/custom callback name
   * 
   * @param localTopic
   *          the local topic
   * @param otherService
   *          the remote serivce
   * @param callback
   *          the method to call on the remote service
   */
  public void addListener(String localTopic, String otherService, String callback);

  /**
   * Preferred add listener, a callback will be created. from
   * CodecUtils.getCallbackTopicName pub/get{Method} called on on{Method}
   * 
   * @param localTopic
   *          the local publish method
   * @param otherService
   *          the remote service to call the on method.
   * 
   */
  public void addListener(String localTopic, String otherService);

  public void removeListener(String localTopic, String otherService, String callback);

  /**
   * Preferred remove listener
   * 
   * @param localTopic
   *          the local publish method
   * @param otherService
   *          the remote servie
   * 
   */
  public void removeListener(String localTopic, String otherService);

  /**
   * calls attach(Attachable)
   * 
   * @param serviceName
   *          - service name
   * @throws Exception
   *           - thrown if error
   */
  public void attach(String serviceName) throws Exception;

  /**
   * implementation of detaching an attached service
   * 
   * @param service
   *          the service to detach from
   *
   */
  public void detach(Attachable service);

  /**
   * This detach calls detach(Attachable) method for a reference
   * 
   * @param serviceName
   *          - name of service
   */
  public void detach(String serviceName);

  /**
   * detach which detaches ALL other services from this service.
   */
  public void detach();

  /**
   * @return the set of attached service names to this service
   */
  public Set<String> getAttached();

  /**
   * get all attached to a specific publishing point/method
   * 
   * @param publishingPoint
   *          the publishing method.
   * @return a set of attached services
   * 
   */
  public Set<String> getAttached(String publishingPoint);

  /**
   * @param instance
   *          - referenced service to test
   * @return true if service is already attached false otherwise
   */
  public boolean isAttached(Attachable instance);

  /**
   * returns if the Attachable has been set or not - name interface
   * 
   * @param name
   *          - name of service
   * @return True or False depending if service is attached
   */
  public boolean isAttached(String name);

  public boolean isLocal();

  /**
   * safe method to query interface without having to invoke class
   * 
   * @param interfaze
   *          the interface to test
   * @return true if it has the interface
   * 
   */
  public boolean hasInterface(String interfaze);

  public boolean hasInterface(Class<?> interfaze);

  public boolean isType(Class<?> clazz);

  /**
   * safe method to query interface without having to invoke class
   * 
   * @param clazz
   *          the class
   * @return true if it is of the specified type
   * 
   */
  public boolean isType(String clazz);

}
