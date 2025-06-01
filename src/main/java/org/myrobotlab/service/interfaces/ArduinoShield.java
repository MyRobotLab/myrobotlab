package org.myrobotlab.service.interfaces;

public interface ArduinoShield {

  boolean attach(PinArrayControl arduino);

  boolean isAttached();
}
