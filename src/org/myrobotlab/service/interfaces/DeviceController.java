package org.myrobotlab.service.interfaces;

import java.util.Set;

// FIXME - promote into ServiceInterface
public interface DeviceController extends NameProvider {

	void detach(DeviceControl device);
	
	/**
	 * @return - the current count of devices its controlling
	 */
	public int getAttachedCount();
	
	/**
	 * get the current set of connected 'control' devices attached to this controller
	 * @return
	 */
	public Set<String> getAttachedNames();
	
}
