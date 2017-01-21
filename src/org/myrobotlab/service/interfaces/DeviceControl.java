package org.myrobotlab.service.interfaces;

import java.util.Set;

/**
 * A device which can be attached to a microcontroller implementers are Sensor
 * and Stepper - perhaps more not sure exactly what all it should impelement -
 * but represents something which can be attached to a microcontroller
 * 
 * This is where all supported devices are defined. They all have a unique type
 * identifier which can be communicated to a microcontroller
 * 
 * It also binds how the microcontroller identifies its service (getIndex())
 * with the service (getName())
 *
 */
public interface DeviceControl extends NameProvider {

	/**
	 * detach the controller with this name from the control service
	 * 
	 * @param controllerName
	 */
	public void detach(String controllerName);
	
	/**
	 * gets the controller for this device
	 * (GroG says - this is flawed in that it only supports 1,
	 * it should probably return a list or set)
	 * 
	 * @return
	 */
	public DeviceController getController();

	/**
	 * returns if the DeviceController has been set or not
	 * @return
	 */
	public boolean isAttached(String name);
	
	/**
	 * returns the set of attached services to this service
	 * @return
	 */
	public Set<String> getAttached();

}
