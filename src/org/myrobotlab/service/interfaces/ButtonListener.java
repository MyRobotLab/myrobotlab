package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.Joystick.Button;

public interface ButtonListener {

	public String getName();

	public void onButton(Button button) throws Exception;

}
