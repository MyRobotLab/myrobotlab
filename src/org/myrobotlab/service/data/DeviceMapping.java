package org.myrobotlab.service.data;

import org.myrobotlab.service.interfaces.Device;

public class DeviceMapping {
	
	Device device;
	/**
	 * the unique integer id for this device
	 */
	Integer index;
	/**
	 * the original config used to attach the device
	 */
	int[] config;
	
	public DeviceMapping(Device device){
		this.device = device;
	}
	
	public DeviceMapping(Device device, int[] config){
		this.device = device;
		this.config = config;
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
