package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Service;

public interface StatePublisher {

  public Service publishState();

  public Service broadcastState();

}
