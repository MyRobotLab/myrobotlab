package org.myrobotlab.service.data;

public class PinData extends SensorData {
	private static final long serialVersionUID = 1L;
	Integer address;
	
	public PinData(Integer address, Integer value){
		super(value);
		this.address = address;
	}

	public int getValue() {
		return (int)data;
	}

	public Integer getAddress() {
		return address;
	}

	public void setAddress(Integer address) {
		this.address = address;
	}

	
}
