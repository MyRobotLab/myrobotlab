package org.myrobotlab.service.interfaces;

public abstract class PinDefinition {
	
	Integer address;
	
	public abstract boolean isAnalog();

	public abstract boolean isPwm();

	public abstract boolean isDigital();
	
	public Integer getAddress(){
		return address;
	}
}
