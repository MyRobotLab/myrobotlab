package org.myrobotlab.service.interfaces;

public interface SensorController extends DeviceController {
	
	public void sensorActivate(SensorControl sensor, Object... conf);

	public void sensorDeactivate(SensorControl sensor);

}
