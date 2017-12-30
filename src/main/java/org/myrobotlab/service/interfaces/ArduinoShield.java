package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.Arduino;

public interface ArduinoShield {

  boolean attach(Arduino arduino);

  boolean isAttached();
}
