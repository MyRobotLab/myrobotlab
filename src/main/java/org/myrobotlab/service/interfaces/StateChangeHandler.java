package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.FiniteStateMachine.StateChange;

public interface StateChangeHandler {

  public void handleStateChange(StateChange event);
  
}
