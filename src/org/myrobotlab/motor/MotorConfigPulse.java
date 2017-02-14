package org.myrobotlab.motor;

public class MotorConfigPulse extends MotorConfig {
	
	Integer pulsePin;
	Integer dirPin;
	
	public MotorConfigPulse(int pulsePin, int dirPin){
		this.pulsePin = pulsePin;
		this.dirPin = dirPin;
	}
	
	public int getPulsePin() {
		return pulsePin;
	}

	public int getDirPin(){
		return dirPin;
	}
}
