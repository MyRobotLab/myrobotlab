package org.myrobotlab.motor;

public class MotorConfigDualPwm extends MotorConfig {

	Integer leftPin;
	Integer rightPin;
	
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
	
	static public void main(String[] args){
		MotorConfigDualPwm c = new MotorConfigDualPwm(3,5);
		String t = c.getType();
		
	}

}
