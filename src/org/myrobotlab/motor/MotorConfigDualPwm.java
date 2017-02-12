package org.myrobotlab.motor;

public class MotorConfigDualPwm extends MotorConfig {


	private static final long serialVersionUID = 1L;
	Integer leftPin;
	Integer rightPin;
	Integer pwmFreq;
	
	public MotorConfigDualPwm(){
		
	}
	
	
	public MotorConfigDualPwm(int leftPin, int rightPin){
		this.leftPin = leftPin;
		this.rightPin = rightPin;
	}	
	
	public void setPwmPins(int leftPin, int rightPin) {
		this.leftPin = leftPin;
		this.rightPin = rightPin;
	}
	
	public Integer getLeftPin() {
		return leftPin;
	}
	
	public Integer getRightPin() {
		return rightPin;
	}
	
	public Integer getPwmFreq(){
		return this.pwmFreq;
	}
	
	public void setPwmFreq(Integer pwmFreq){
		this.pwmFreq = pwmFreq;
	}
	
	static public void main(String[] args){
		MotorConfigDualPwm c = new MotorConfigDualPwm(3,5);
		String t = c.getType();
		
	}

}
