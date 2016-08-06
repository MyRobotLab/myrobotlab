package org.myrobotlab.motor;

public abstract class MotorConfig {
	
	String type = this.getClass().getSimpleName();//.getType();//MotorConfigDualPwm.class.getSimpleName();;
	
	static String [] types = new String[]{MotorConfigSimpleH.class.getSimpleName(), MotorConfigDualPwm.class.getSimpleName(), MotorConfigPulse.class.getSimpleName()};
	
	static public String[] getTypes() {
		return types;
	}
	
	public String getType() {
		return type;
	}

}
