package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;

public interface ServiceStatus {

  public Service broadcastState();

  public Status getLastError();
}
