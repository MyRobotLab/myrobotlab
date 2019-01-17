package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Status;

public interface StatusPublisher {

  public Status publishStatus(Status status);

  public void broadcastStatus(Status status);

}
