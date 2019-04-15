package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface ServoDataListener extends NameProvider {
  
  void onServoData(ServoData se);
  
}
