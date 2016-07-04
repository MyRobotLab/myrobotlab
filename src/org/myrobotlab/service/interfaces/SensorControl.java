package org.myrobotlab.service.interfaces;

public interface SensorControl extends SensorDataListener, DeviceControl {
	
	public void activate(Object... conf);

	public void deactivate();

}
