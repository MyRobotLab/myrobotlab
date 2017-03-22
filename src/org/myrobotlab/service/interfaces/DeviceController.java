package org.myrobotlab.service.interfaces;

import java.util.Set;

public interface DeviceController extends NameProvider {

	// FIXED - attaching is to complex to generalize - each controller provides a specific
	// and 'typed' attach
	// void deviceAttach(DeviceControl device, Object... conf) throws Exception;

	void detach(DeviceControl device);
	
	/**
	 * @return - the current count of devices its controlling
	 */
	public int getDeviceCount();
	
	/**
	 * get the current set of connected 'control' devices attached to this controller
	 * @return
	 */
	public Set<String> getDeviceNames();
	
}
