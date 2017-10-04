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
   * This attach when overriden "routes" to the appropriately typed parameterized
   * attach within a service.
   * 
   * When overriden, the first thing it should do is check to see if the
   * referenced service is already attached. If it is already attached it should
   * simply return.
   * 
   * If its attached to this service, it should first attach itself, modifying
   * its own data if necessary. The last thing it should do is call the
   * parameterized service's attach. This gives the other service an opportunity
   * to attach. e.g.
   * 
   * <pre>
   * 
   * public void attach(Attachable service) {
   *    if (ServoControl.class.isAssignableFrom(service.getClass())) {
   *        attachServoControl((ServoControl) service);
   *        return;
   *    }
   *    
   *    ...  route to more attach functions   ....
   *    
   *    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
   *  }
   *  
   *  And within attachServoControl :
   *  
   *  public void attachServoControl(ServoControl service) {
   *       // guard
   *       if (!isAttached(service)){
   *           return;
   *       }
   *       
   *       ... attach logic ....
   * 
   *       // call to attaching service
   *       service.attach(this);  
   * }  
   * </pre>
   * 
   * @param service
   *          - the service to attach from this service
   * @throws Exception - throws on error and cannot attach
   */
  public void attach(Attachable service) throws Exception;

  /**
   * calls attach(Attachable)
   * 
   * @param serviceName - service name
   * @throws Exception - thrown if error
   */
  public void attach(String serviceName) throws Exception;

  /**
   * This detach when overriden "routes" to the appropriately typed parameterized
   * detach within a service.
   * 
   * When overriden, the first thing it should do is check to see if the
   * referenced service is already detached. If it is already detached it should
   * simply return.
   * 
   * If its detached to this service, it should first detach itself, modifying
   * its own data if necessary. The last thing it should do is call the
   * parameterized service's detach. This gives the other service an opportunity
   * to detach. e.g.
   * 
   * <pre>
   * 
   * public void detach(Attachable service) {
   *    if (ServoControl.class.isAssignableFrom(service.getClass())) {
   *        detachServoControl((ServoControl) service);
   *        return;
   *    }
   *    
   *    ...  route to more detach functions   ....
   *    
   *    error("%s doesn't know how to detach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
   *  }
   *  
   *  And within detachServoControl :
   *  
   *  public void detachServoControl(ServoControl service) {
   *       // guard
   *       if (!isAttached(service)){
   *           return;
   *       }
   *       
   *       ... detach logic ....
   * 
   *       // call to detaching service
   *       service.detach(this);  
   * }  
   * </pre>
   * 
   * @param service
   *          - the service to detach from this service
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
   * @param instance - referenced service to test
   * @return true if service is already attached false otherwise
   */
  public boolean isAttached(Attachable instance);

  /**
   * returns if the Attachable has been set or not - name interface
   * 
   * @param name - name of service
   * @return True or False depending if service is attached
   */
  public boolean isAttached(String name);
  
  public boolean isLocal();

}
