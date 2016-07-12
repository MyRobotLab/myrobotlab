package org.myrobotlab.service.data;

public class PinEvent extends Event {
	private static final long serialVersionUID = 1L;
	Integer address;
	Integer value;
	
	public PinEvent(String source, Integer address, Integer value){
		this.source = source;
		this.address = address;
		this.value = value;
	}

	public Integer getValue() {
		return value;
	}

	public void setValue(Integer value) {
		this.value = value;
	}

	public Integer getAddress() {
		return address;
	}

	public void setAddress(Integer address) {
		this.address = address;
	}

	
}
