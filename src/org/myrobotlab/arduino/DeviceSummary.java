package org.myrobotlab.arduino;

public class DeviceSummary {
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
