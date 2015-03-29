package org.myrobotlab.service.interfaces;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Status;

public interface ServiceInterface extends Messaging, LoggingSink {

	// FIXME !!!! - refactor - split into 2 interfaces ServiceInterface -
	// service related methods & Messaging

	// getTestEnvironment() - hasServo hasArduino ???

	// hasError() - publish subscribe - getError().getSourceName()


	// ErrorSink
	//public String error(String format, Object... args);

	public String[] getCategories();

	public String[] getDeclaredMethodNames();

	public Method[] getDeclaredMethods();

	public String getDescription();

	public URI getInstanceId();

	public String[] getMethodNames();

	public Method[] getMethods();

	public String getName();

	public ArrayList<MRLListener> getNotifyList(String key);

	public ArrayList<String> getNotifyListKeySet();

	// Deprecate - just use class
	public String getSimpleName();

	// Deprecate ?? What is this??
	public String getType();

	public boolean hasDisplay();

	public boolean hasPeers();

	public boolean load();

	/**
	 * recursive release - releases all peers and their peers etc. then releases
	 * this service
	 */
	public void releasePeers();

	public void releaseService();


	/**
	 * asked by the framework - to determine if the service needs to be secure
	 * 
	 * @return
	 */
	public boolean requiresSecurity();

	public boolean save();

	public void setInstanceId(URI uri);

	public void setPrefix(String prefix);

	public void startService();

	public void stopService();

	// can't be statics :(
	// but is good enough - probably a good idea to keep in mind
	// is the ability to do "non destructive" tests on a "live" Service at any
	// time
	public Status test() throws Exception;
	
	public String clearLastError();

	public boolean hasError();

	public Status getLastError();

	public void broadcastState();
}
