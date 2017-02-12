package org.myrobotlab.motor;

import java.io.Serializable;

public class MotorConfig implements Serializable {

	private static final long serialVersionUID = 1L;

	public MotorConfig() {
	};

	String type = this.getClass().getSimpleName();

	static String[] types = new String[] { MotorConfigSimpleH.class.getSimpleName(), MotorConfigDualPwm.class.getSimpleName(), MotorConfigPulse.class.getSimpleName() };

	static public String[] getTypes() {
		return types;
	}

	public String getType() {
		return type;
	}

}
