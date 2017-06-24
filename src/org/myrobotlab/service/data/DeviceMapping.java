package org.myrobotlab.service.data;

import java.io.Serializable;
import java.util.Arrays;

import org.myrobotlab.framework.interfaces.Attachable;

public class DeviceMapping implements Serializable{	
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	// transient too help prevent infinite recursion in gson 
	// encoding since Arduino will have a reference
	// to itself as a device
	// transient DeviceControl device;
	// Changed by Mats to use an AnnotationExclusionStrategy
	// See http://stackoverflow.com/questions/4802887/gson-how-to-exclude-specific-fields-from-serialization-without-annotations?rq=1
	// for reference
	transient Attachable device;
	
	/**
	 * the unique integer id for this device
	 */
	Integer id;
	/**
	 * the original config used to attach the device
	 */
	Object[] config;
	
	public DeviceMapping(Attachable device, Object... config) {
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
	
	public Attachable getDevice(){
		return device;
	}
	
	public Object[] getConfig(){
		return config;
	}
	
	public String toString(){
		return String.format("id:%d name:%s config:%s", id, device.getName(), Arrays.toString(config));
	}
}
