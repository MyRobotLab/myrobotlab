package org.myrobotlab.service.interfaces;

import java.io.Serializable;

public class PinDefinition implements Serializable{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	String name;
	Integer address;
	// String color; ?? 

	/**
	 * means actively reading
	 */
	boolean enabled = false;
	
	/**
	 * pin mode INPUT or OUTPUT
	 */
	String mode; 
	
	/**
	 * statistics
	 */
	int totalSamples;
	int min;
	int max;
	int avg;

	boolean isAnalog = false;

	boolean isPwm = false;

	boolean isDigital = true;
	
	boolean isRx = false;
	
	boolean isTx = false;
	
	boolean canRead = true;
	
	boolean canWrite = true;

	Integer value;

	public Integer getValue() {
		return value;
	}

	public void setValue(Integer value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public Integer getAddress() {
		return address;
	}

	public boolean isAnalog() {
		return isAnalog;
	}

	public boolean isDigital() {
		return isDigital;
	}

	public boolean isPwm() {
		return isPwm;
	}

	public void setName(int i) {
		name = String.format("%d", i);
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


	public void setPwm(boolean b) {
		isPwm = b;
	}

	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("pin def ");
		sb.append(name);
		sb.append(" ");
		sb.append(address);
		sb.append(" ");
		if (isPwm)
			{ sb.append("isPwm ");}
		if(isAnalog){
			sb.append("isAnalog ");
		}
		if(isDigital){
			sb.append("isDigital ");
		}
		if(isTx){
			sb.append("isTx ");
		}
		if(isRx){
			sb.append("isRx ");
		}

		if (value != null){
			sb.append("value ");
			sb.append(value);
		}
		return sb.toString();
	}

	public void setRx(boolean b) {
		isRx = true;
	}
	
	public boolean isRx(){
		return isRx;
	}
	
	public void setTx(boolean b) {
		isTx = true;
	}
	
	public boolean isTx(){
		return isTx;
	}
	

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}
	
	public boolean canRead(){
	  return canRead;
	}
	
	public boolean canWrite(){
    return canWrite;
  }

  public void canWrite(boolean canWrite) {
    this.canWrite = canWrite;
  }
  
  public void canRead(boolean canRead) {
    this.canRead = canRead;
  }


}













