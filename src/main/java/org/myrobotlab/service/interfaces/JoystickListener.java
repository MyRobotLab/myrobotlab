package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.JoystickData;

public interface JoystickListener extends NameProvider {

  public void onJoystickInput(JoystickData input) throws Exception;

}
