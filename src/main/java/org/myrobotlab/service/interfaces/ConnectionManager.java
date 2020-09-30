package org.myrobotlab.service.interfaces;

import org.myrobotlab.net.Connection;

public interface ConnectionManager {

  public void addConnection(String uuid, Connection attributes);

  public void removeConnection(String uuid);

}
