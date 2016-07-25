package org.myrobotlab.service.data;

import java.io.Serializable;

public class PinData implements Serializable {
	private static final long serialVersionUID = 1L;
	Integer address;
	Integer value;
	
	public PinData(int address, int value){
		this.address = address;
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public Integer getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}

	public String toString(){
		return String.format("address=%d value=%d", address, value);
	}
	
}
