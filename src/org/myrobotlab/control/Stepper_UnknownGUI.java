package org.myrobotlab.control;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Stepper_UnknownGUI extends StepperControllerPanel {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(StepperControllerPanel.class.getCanonicalName());

	Object[] data = null;

	@Override
	void setAttached(boolean state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(Object[] data) {
		log.warn("setData on an unknown StepperGUI Panel :P");
		this.data = data;
	}

}
