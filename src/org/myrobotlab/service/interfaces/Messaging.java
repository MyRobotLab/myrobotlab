package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Message;

public interface Messaging {
	
	/**
	 * put message in inbox, so it will be processed by this service
	 * 
	 * @param msg
	 */
	public void in(Message msg);

	public void out(Message msg);
	
	public Object invoke(String method);

	public Object invoke(String method, Object... params);

	public boolean isLocal();

	public void send(String name, String method);

	public void send(String name, String method, Object... data);

	public Object sendBlocking(String name, String method);

	public Object sendBlocking(String name, String method, Object... data);
	
	public Object sendBlocking(String name, Integer timeout, String method, Object... data);

	public void subscribe(NameProvider topicName, String topicKey);
	
	public void subscribe(String topicName, String topicKey);
	
	public void subscribe(String topicName, String topicMethod, String callbackName, String callbackMethod);

	public void unsubscribe(NameProvider topicName, String topicKey);

	public void unsubscribe(String topicName, String topicKey);
	
	public void unsubscribe(String topicName, String topicMethod, String callbackName, String callbackMethod);

}
