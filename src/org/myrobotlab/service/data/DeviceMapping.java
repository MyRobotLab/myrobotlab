package org.myrobotlab.service.data;

import org.myrobotlab.service.interfaces.Device;

public class DeviceMapping {
	
	Device device;
	Integer index;
	
	public DeviceMapping(Device device){
		this.device = device;
	}

	public String getName(){
		return device.getName();
	}
	
	public void setIndex(int index){
		this.index = index; 
	}
	
	public Integer getIndex(){
		return index;
	}
	
	public Device getDevice(){
		return device;
	}
}
