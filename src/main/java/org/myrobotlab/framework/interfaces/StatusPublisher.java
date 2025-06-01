package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Status;

public interface StatusPublisher extends NameProvider {

  public Status publishStatus(Status status);

  public void broadcastStatus(Status status);

}
