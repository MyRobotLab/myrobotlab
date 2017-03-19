package org.myrobotlab.service.data;

import java.io.Serializable;

public class PinData implements Serializable {
	private static final long serialVersionUID = 1L;
	public Integer address;
	public Integer value;
	
	public PinData(int address, int value){
		this.address = address;
		this.value = value;
	}

	public String toString(){
		return String.format("address=%d value=%d", address, value);
	}
	
}
