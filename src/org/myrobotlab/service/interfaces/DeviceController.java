package org.myrobotlab.service.interfaces;

public interface DeviceController extends NameProvider {

	/**
	 * Low level base method of all Controller.attach methods
	 * The idea is higher level methods such as MotorController.attach can implement an encoding/configuration 
	 * scheme such that attachDevice will provide a low level standardized serialization, configuration & initialization
	 * 
	 * @param device
	 * @param config
	 * @throws Exception
	 */
	public void attachDevice(DeviceControl device, Object... config) throws Exception;

	public void detachDevice(DeviceControl device);

	
}
