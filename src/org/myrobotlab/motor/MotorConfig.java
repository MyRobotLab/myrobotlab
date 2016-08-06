package org.myrobotlab.motor;

public abstract class MotorConfig {
	
	String type = this.getClass().getSimpleName();//.getType();//MotorConfigDualPwm.class.getSimpleName();;
	
	public String[] getTypes() {
		return new String[]{MotorConfigSimpleH.class.getSimpleName(), MotorConfigDualPwm.class.getSimpleName(), MotorConfigPulse.class.getSimpleName()};
	}
	
	public String getType() {
		return type;
	}

}
