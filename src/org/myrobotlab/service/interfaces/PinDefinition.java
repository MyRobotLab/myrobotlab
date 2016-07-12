package org.myrobotlab.service.interfaces;

public class PinDefinition {

	String name;
	Integer address;

	boolean isAnalog;

	boolean isPwm;

	boolean isDigital;
	
	boolean isRx;
	
	boolean isTx;

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
}













