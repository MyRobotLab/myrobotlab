package org.myrobotlab.service.interfaces;

import java.net.URI;

import org.myrobotlab.framework.Message;

public interface CommunicationInterface {

  public void addRemote(URI mrlHost, URI protocolKey);

  public void send(final Message msg);

  public void send(final URI uri, final Message msg);

}
