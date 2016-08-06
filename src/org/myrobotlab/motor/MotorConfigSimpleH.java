package org.myrobotlab.motor;

public class MotorConfigSimpleH extends MotorConfig {

	Integer pwrPin;
	Integer dirPin;
	
	public MotorConfigSimpleH(){
		
	}
	
	public MotorConfigSimpleH(int pwrPin, int dirPin) {
		this.pwrPin = pwrPin;
		this.dirPin = dirPin;
	}
	
	public int getDirPin(){
		return dirPin;
	}
	
	public int getPwrPin(){
		return pwrPin;
	}

}
