package org.myrobotlab.service.interfaces;

import java.net.URI;

import org.myrobotlab.framework.Message;

public interface CommunicationInterface {

	public void send(final Message msg);

	public void send(final URI uri, final Message msg);

	public void setComm(final Communicator comm);

	/**
	 * gets the appropriate local service based communicator to relay 
	 * the message remotely
	 * 
	 * @param uri
	 * @return
	 */
	public Communicator getComm(final URI uri);
	
	public void addRemote(URI mrlHost, URI protoKey);

}
