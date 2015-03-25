package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Message;

public interface Messaging {
	public void addListener(String outMethod, String namedInstance, String inMethod, Class<?>... paramTypes);

	/**
	 * in will put a message in the Service's inbox - it will require the
	 * Service's thread to process it.
	 * 
	 * @param msg
	 *            - message to process
	 */
	public void in(Message msg);

	public void out(Message msg);
	
	public Object invoke(String method);

	public Object invoke(String method, Object... params);

	public boolean isLocal();

	public void removeListener(String outMethod, String serviceName, String inMethod, Class<?>... paramTypes);

	public void send(String name, String method);

	public void send(String name, String method, Object... data);

	public Object sendBlocking(String name, Integer timeout, String method, Object... data);

	public Object sendBlocking(String name, String method);

	public Object sendBlocking(String name, String method, Object... data);

	public void subscribe(String outMethod, String publisherName, String inMethod, Class<?>... parameterType);

	public void unsubscribe(String outMethod, String publisherName, String inMethod, Class<?>... parameterType);

}
