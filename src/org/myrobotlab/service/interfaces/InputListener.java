package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.Joystick.Input;

public interface InputListener {

	public String getName();

	public void onInput(Input input) throws Exception;

}
