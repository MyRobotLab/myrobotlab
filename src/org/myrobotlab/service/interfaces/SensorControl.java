package org.myrobotlab.service.interfaces;

public interface SensorControl extends SensorDataListener, DeviceControl {
	
	public void attach(SensorController controller, Object...conf);
	
	// are these possible ???
	public void activate(Object... conf);

	public void deactivate();

}
