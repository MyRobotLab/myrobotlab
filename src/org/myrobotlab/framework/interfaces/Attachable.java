package org.myrobotlab.framework.interfaces;

import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.service.Runtime;

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
	 * detach the attachable with this name from the service by name
	 * 
	 * @param service
	 */
	public void detach(String service);
	
	/**
	 * detach the attachable with this name from the service by reference
	 * 
	 * @param service
	 */
	public void detach(Attachable service);

	/**
	 * returns if the Attachable has been set or not - name interface
	 * @return
	 */
	public boolean isAttached(String name);
	
	 /**
   * returns if the Attachable has been set or not - name interface
   * @return
   */
  public boolean isAttached(Attachable instance);

	
	/**
	 * returns the set of attached service names to this service
	 * @return
	 */
	public Set<String> getAttached();
	
	 /**
   * the "routing" attach - routes to a specific strongly typed attach of the
   * service if it exists
   * 
   * @param name
   */
  public void attach(Attachable service) throws Exception;
  
  
  public void attach(String service) throws Exception;
  
  
  /**
   * @return - the current count of devices its controlling
   */
  /*
  default public int getAttachedCount(){
    return 0;
  }
  */
  /**
   * get the current set of connected 'control' devices attached to this controller
   * @return
   */
  /*
  default public Set<String> getAttachedNames(){
    Set<String> ret = new TreeSet<String>();
    return ret;
  }
  */

}
