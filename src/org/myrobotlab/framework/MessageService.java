package org.myrobotlab.framework;

import org.myrobotlab.service.interfaces.CommunicationInterface;

public abstract class MessageService {

  // FIXME NO DUPLICATE FIELDS - YOU'LL NEED TO PULL NAME OUT OF SERVICE !!!
  // private String name;

  transient protected Outbox outbox = null;

  transient protected CommunicationInterface cm = null;

  public MessageService() {
    // this.name = name;
  }

  public MessageService(String name) {
    // this.name = name;
  }
  /*
   * 
   * public String getName(){ return name; }
   * 
   * public void setName(String name){ this.name = name; }
   */

  /*
   * 
   * public void send(String name, String method) { send(name, method,
   * (Object[]) null); }
   * 
   */

  // TODO - remove or reconcile - RemoteAdapter and Service are the only ones
  // using this
  /**
   * 
   * @param name
   * @param method
   * @param data
   * @return
   */

}
