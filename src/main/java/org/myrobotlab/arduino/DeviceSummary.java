package org.myrobotlab.arduino;

import java.io.Serializable;

public class DeviceSummary implements Serializable {	
	private static final long serialVersionUID = 1L;
	
	String name;
	Integer id;
	String type;
	Integer typeId;
	
	public DeviceSummary(String name, int id, String type, int typeId){
		this.name = name;
		this.id = id;
		this.type = type;
		this.typeId = typeId;
	}
}
