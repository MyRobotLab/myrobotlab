package org.myrobotlab.control;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Motor;
import org.slf4j.Logger;

public class Motor_UnknownGUI extends MotorControllerPanel {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MotorControllerPanel.class);

	Motor motor = null;

	@Override
	public void set(Motor motor) {
		log.warn("setData on an unknown MotorGUI Panel :P");
		this.motor = motor;
	}

	@Override
	void setAttached(boolean state) {
		// TODO Auto-generated method stub

	}

}
