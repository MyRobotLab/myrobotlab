package org.myrobotlab.service.data;

import java.io.Serializable;

public class SensorData implements Serializable {

	private static final long serialVersionUID = 1L;
	Object data;
	
	public SensorData(Object data) {
		this.data = data;
	}
	
	public Object getData(){
		return data;
	}

}
