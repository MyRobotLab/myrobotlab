package org.myrobotlab.service.interfaces;

public interface DeviceController extends NameProvider {

	void deviceAttach(DeviceControl device, Object... conf) throws Exception;

	void deviceDetach(DeviceControl device);
	
}
