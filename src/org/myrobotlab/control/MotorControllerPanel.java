package org.myrobotlab.control;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.myrobotlab.service.Motor;

/**
 * @author GroG interface to update a MotorGUI's Controller Panel
 * 
 */
abstract class MotorControllerPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	MotorControllerPanel() {
		setBorder(BorderFactory.createTitledBorder("type"));
	}

	abstract public void set(Motor motor);

	abstract void setAttached(boolean state);

}
