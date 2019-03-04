package org.myrobotlab.service.interfaces;

import org.myrobotlab.fsm.api.State;

public interface StateListener {

  void onState(State state);
  
}
