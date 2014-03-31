package org.myrobotlab.service.interfaces;

import java.net.URI;
import java.util.ArrayList;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Status;
public interface ServiceInterface {
	
	// FIXME !!!!
	// need public Test() - unimplemented by Service !!!! :D
	
	// getTestEnvironment() - hasServo hasArduino ???
	
	// getLastError()
	
	// hasError() - publish subscribe - getError().getSourceName()
	
	public void test(Object... data);
	public void test();
	
	public URI getHost();
		
	public void setHost(URI uri);
	
	public String getName();

	/**
	 * in will put a message in the Service's inbox - it will require the Service's thread
	 * to process it.
	 * 
	 * @param msg - message to process
	 */
	public void in(Message msg);

	public void stopService();

	public void startService();

	public void releaseService();
	
	/**
	 * recursive release - releases all peers and their peers etc.
	 * then releases this service
	 */
	public void releasePeers();

	public ArrayList<String> getNotifyListKeySet();

	public ArrayList<MRLListener> getNotifyList(String key);

	public String getSimpleName();

	public String getDescription();
	
	public boolean save();
	
	public boolean load();
	
	public void subscribe(String outMethod, String publisherName, String inMethod, Class<?>... parameterType);
	
	public void unsubscribe(String outMethod, String publisherName, String inMethod, Class<?>... parameterType);
	
	public void addListener(String outMethod, String namedInstance, String inMethod, Class<?>... paramTypes);
	
	public void removeListener(String outMethod, String serviceName, String inMethod, Class<?>... paramTypes);
	
	public Object invoke(String method);

	public Object invoke(String method, Object...params);
	
	public boolean hasDisplay();
	
	public boolean hasPeers();
	
	
	/**
	 * asked by the framework - to determin if the service needs to be secure
	 * @return
	 */
	public boolean requiresSecurity();

	public boolean isLocal();
}
