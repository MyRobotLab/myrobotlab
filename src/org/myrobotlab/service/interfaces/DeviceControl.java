package org.myrobotlab.service.interfaces;

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
	 * sets the controller for this device
	 * @param controller
	 */
	public void setController(DeviceController controller);

	/**
	 * gets the controller for this device
	 * @return
	 */
	public DeviceController getController();

	/**
	 * returns if the DeviceController has been set or not
	 * @return
	 */
	public boolean isAttached();

}
