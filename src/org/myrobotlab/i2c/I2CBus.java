package org.myrobotlab.i2c;

import org.myrobotlab.service.data.SensorData;
import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.I2CBusControl;
import org.myrobotlab.service.interfaces.I2CBusController;
import org.myrobotlab.service.interfaces.SensorDataListener;

public class I2CBus implements DeviceController, I2CBusControl, SensorDataListener{

	String name;
	I2CBusController controller;
	
	public I2CBus(String Name){
		this.name = Name;
	}
	@Override
	public void setController(DeviceController controller) {
		this.controller = (I2CBusController) controller;
	}

	@Override
	public DeviceController getController() {
		return controller;
	}

	@Override
	public boolean isAttached() {
		return true;
	}

	@Override
	public String getName() {
		return name;
	}
	@Override
	public void deviceAttach(DeviceControl device, Object... conf) throws Exception {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void deviceDetach(DeviceControl device) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onSensorData(SensorData data) {
		// This is where the data read from the i2c bus gets returned 
		// pass it back to the I2cController ( Arduino ) so that it can be 
		// returned to the i2cdevice
		controller.i2cReturnData(data);
		
	}
	@Override
	public boolean isLocal() {
		return true;
	}
		
}
