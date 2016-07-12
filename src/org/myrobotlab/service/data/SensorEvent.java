package org.myrobotlab.service.data;

public class SensorEvent extends Event {

	Object data;
	
	public SensorEvent(String name, Object data) {
		this.source = name;
		this.data = data;
	}
	
	public Object getData(){
		return data;
	}

	public String getSource(){
		return source;
	}
}
