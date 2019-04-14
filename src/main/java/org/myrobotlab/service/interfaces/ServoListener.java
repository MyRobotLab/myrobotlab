package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface ServoListener extends NameProvider {
  
  void onServoEvent(ServoData se);
  
}
