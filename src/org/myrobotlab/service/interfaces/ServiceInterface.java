package org.myrobotlab.service.interfaces;

import java.net.URI;
import java.util.ArrayList;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Status;
public interface ServiceInterface {
	
	// FIXME !!!! - refactor - split into 2 interfaces ServiceInterface - service related methods & Messaging
	
	// getTestEnvironment() - hasServo hasArduino ???
	
	// getLastError()
	
	// hasError() - publish subscribe - getError().getSourceName()
	
	// can't be statics :( 
	// but is good enough - probably a good idea to keep in mind
	// is the ability to do "non destructive" tests on a "live" Service at any time
	public Status test(Object... data) throws Exception;
	public Status test() throws Exception;
	
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
	
	public void info(String format, Object... args);
	public String error(String format, Object... args);
	public void warn(String format, Object... args);
	
	/**
	 * recursive release - releases all peers and their peers etc.
	 * then releases this service
	 */
	public void releasePeers();

	public ArrayList<String> getNotifyListKeySet();

	public ArrayList<MRLListener> getNotifyList(String key);

	public String getSimpleName();
	
	public String getType();

	public String getDescription();
	
	public boolean save();
	
	public boolean load();
	
	// FIXME - split 2 interfaces ServiceInterface & Messaging
	// public void send(String name, String method, )
	
	public void subscribe(String outMethod, String publisherName, String inMethod, Class<?>... parameterType);
	
	public void unsubscribe(String outMethod, String publisherName, String inMethod, Class<?>... parameterType);
	
	public void addListener(String outMethod, String namedInstance, String inMethod, Class<?>... paramTypes);
	
	public void removeListener(String outMethod, String serviceName, String inMethod, Class<?>... paramTypes);
	
	public Object invoke(String method);

	public Object invoke(String method, Object...params);
	
	public boolean hasDisplay();
	
	public boolean hasPeers();
	
	
	/**
	 * asked by the framework - to determine if the service needs to be secure
	 * @return
	 */
	public boolean requiresSecurity();

	public boolean isLocal();
}
