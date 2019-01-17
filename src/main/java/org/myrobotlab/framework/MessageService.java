package org.myrobotlab.framework;

import org.myrobotlab.service.interfaces.CommunicationInterface;

public abstract class MessageService {

  // FIXME NO DUPLICATE FIELDS - YOU'LL NEED TO PULL NAME OUT OF SERVICE !!!
  // private String name;

  transient protected Outbox outbox = null;

  transient protected CommunicationInterface cm = null;

  public MessageService() {
  }

  public MessageService(String name) {
  }

}
