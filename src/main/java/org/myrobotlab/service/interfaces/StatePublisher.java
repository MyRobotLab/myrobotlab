package org.myrobotlab.service.interfaces;

import org.myrobotlab.fsm.api.State;

public interface StatePublisher {

  State publishState(State state);
  
}
