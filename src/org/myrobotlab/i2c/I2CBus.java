package org.myrobotlab.i2c;

import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.I2CBusControl;

public class I2CBus implements DeviceController, I2CBusControl{

	String name;
	DeviceController controller;
	
	public I2CBus(String Name){
		this.name = Name;
	}
	@Override
	public void setController(DeviceController controller) {
		this.controller = controller;
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
		
}
