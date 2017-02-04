package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.JoystickData;

public interface JoystickListener {

  public String getName();

  public void onJoystickInput(JoystickData input) throws Exception;

}
