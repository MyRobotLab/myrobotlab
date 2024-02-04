package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.FiniteStateMachine.StateChange;

public interface StateChangeListener {

  public void onStateChange(StateChange event);
  
}
