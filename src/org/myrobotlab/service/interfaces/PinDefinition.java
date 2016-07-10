package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.PinData;

public class PinDefinition {
	
	String name;
	Integer address;

	boolean isAnalog;

	boolean isPwm;

	boolean isDigital;
	
	PinData pinData;
	
	
	public String getName(){
		return name;
	}
	
	public Integer getAddress() {
		return address;
	}
	
	public PinData getPinData(){
		return pinData;
	}
	
	public boolean isAnalog(){
		return isAnalog;
	}
	
	
	public boolean isDigital(){
		return isDigital;
	}
	
	
	public boolean isPwm(){
		return isPwm;
	}

	public void setName(int i) {
		name = String.format("%d",i);
	}
	
	public void setName(String address) {
		this.name = address;
	}
	
	public void setAnalog(boolean b) {
		isAnalog = b;
	}

	public void setDigital(boolean b) {
		isDigital = b;
	}
	
	public void setAddress(int index) {
		this.address = index;
	}
	
	public void setPinData(PinData pinData){
		this.pinData =  pinData;
	}
	
	public void setPwm(boolean b) {
		isPwm = b;
	}
}
