package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Status;

public interface ServiceStatus {

  public void broadcastState();

  public Status getLastError();
}
