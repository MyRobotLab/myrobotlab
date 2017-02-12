package org.myrobotlab.motor;

public class MotorConfigSimpleH extends MotorConfig {

	Integer pwrPin;
	Integer dirPin;
	Integer pwmFreq;
	
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

	public Integer getPwmFreq(){
		return this.pwmFreq;
	}
	
	public void setPwmFreq(Integer pwmFreq){
		this.pwmFreq = pwmFreq;
	}
}	

