package org.myrobotlab.service.interfaces;

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
	 * detach the controller with this name from the control service
	 * 
	 * @param service
	 */
	default public void detach(String service){
	  detach(Runtime.getService(service));
	}
	
	
	default public void detach(Attachable service){
	  
	}

	/**
	 * returns if the DeviceController has been set or not
	 * @return
	 */
	default public boolean isAttached(String name){
	  return false;
	}
	
	/**
	 * returns the set of attached services to this service
	 * @return
	 */
	default public Set<String> getAttached() {
	  Set<String> ret = new TreeSet<String>();
	  return ret;
	}
	
	 /**
   * the "routing" attach - routes to a specific strongly typed attach of the
   * service if it exists
   * 
   * @param name
   */
  /*
   * HEH - this did not work - trying to generalize that which should not be
   * generalized :P public void attach(String name) throws Exception;
   * 
   * public void attach(Attachable instance) throws Exception;
   */
  
  default public void attach(Attachable service) throws Exception {
    /*
    if (isAttached(service)){
      return;
    }
    service.attach(this);
    */
    // error("don't know how to attach a %s", service.getClass().getSimpleName());
  }
  /*
   * default public boolean isAttached(Attachable instance){ return true; }
   */
  
  default public void attach(String service) throws Exception {
    attach(Runtime.getService(service));
  }  
  
  
  /**
   * @return - the current count of devices its controlling
   */
  default public int getAttachedCount(){
    return 0;
  }
  
  /**
   * get the current set of connected 'control' devices attached to this controller
   * @return
   */
  default public Set<String> getAttachedNames(){
    Set<String> ret = new TreeSet<String>();
    return ret;
  }

}
