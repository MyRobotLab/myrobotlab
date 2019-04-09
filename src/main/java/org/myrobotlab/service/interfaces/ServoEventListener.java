package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface ServoEventListener extends NameProvider {
  
  void onServoEvent(ServoData se);
  
}
