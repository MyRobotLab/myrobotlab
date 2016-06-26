package org.myrobotlab.service.data;

import org.myrobotlab.service.interfaces.DeviceControl;

public class DeviceMapping {
	
	DeviceControl device;
	/**
	 * the unique integer id for this device
	 */
	Integer id;
	/**
	 * the original config used to attach the device
	 */
	Object[] config;
	
	public DeviceMapping(DeviceControl device, Object... config){
		this.device = device;
		this.config = config;
	}

	public String getName(){
		return device.getName();
	}
	
	public void setId(int id){
		this.id = id; 
	}
	
	public Integer getId(){
		return id;
	}
	
	public DeviceControl getDevice(){
		return device;
	}
	
	public Object[] getConfig(){
		return config;
	}
}
