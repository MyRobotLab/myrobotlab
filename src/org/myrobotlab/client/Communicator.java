package org.myrobotlab.client;

interface  Communicator {

	public abstract boolean register(String host, int port, Receiver client);
	public abstract boolean send(String name, String method, String sendingMethod, Object... data);	
}