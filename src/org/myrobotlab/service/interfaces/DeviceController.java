package org.myrobotlab.service.interfaces;

public interface DeviceController extends NameProvider {
	
	public void attachDevice(Device device, Object... config) throws Exception;

	public void detachDevice(Device device);

}
