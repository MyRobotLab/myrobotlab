package org.myrobotlab.service.interfaces;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.framework.Status;

public interface ServiceInterface
		extends ServiceQueue, LoggingSink, NameTypeProvider, MessageSubscriber, MessageSender, StateSaver, Invoker {

	

  /**
	 * this is a local method which adds a request from some foreign service
	 * with address information (otherService/callback) for a topic callback
	 * Adds an entry on the notify list
	 * 
	 * @param localTopic
	 * @param otherService
	 * @param callback
	 */
	public void addListener(String localTopic, String otherService, String callback);

	public void removeListener(String localTopic, String otherService, String callback);

	public String[] getDeclaredMethodNames();

	public Method[] getDeclaredMethods();

	public URI getInstanceId();

	public String[] getMethodNames();

	public Method[] getMethods();

	public ArrayList<MRLListener> getNotifyList(String key);

	public ArrayList<String> getNotifyListKeySet();

	public Outbox getOutbox();

	// Deprecate - just use class
	public String getSimpleName();
	
	// Deprecate ?? What is this??
	public String getType();

	public boolean hasPeers();
	
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

	public void setInstanceId(URI uri);

	public void setName(String prefix);

	public void startService();

	public void stopService();

	public String clearLastError();

	public boolean hasError();

	public Status getLastError();

	public void broadcastState();

	// public Object invoke(Message msg);

	public void out(String method, Object retobj);

	public boolean isRuntime();

	// FIXME - meta data needs to be infused into instance
	public String getDescription();

	public Map<String, MethodEntry> getMethodMap();

	/**
	 * the "routing" attach - routes to a specific strongly typed attach of the
	 * service if it exists
	 * 
	 * @param name
	 */
	/*
	 * HEH - this did not work - trying to generalize that which should not be
	 * generalized :P public void attach(String name) throws Exception;
	 * 
	 * public void attach(ServiceInterface instance) throws Exception;
	 */
	
	default public void attach(ServiceInterface service) throws Exception {
	  /*
	  if (isAttached(service)){
	    return;
	  }
	  service.attach(this);
	  */
	  error("don't know how to attach a %s", service.getClass().getSimpleName());
	}
	/*
	default public boolean isAttached(ServiceInterface instance){
	  return true;
	}
	*/
	
	default public void attach(String instance) throws Exception {
    return;
  }  
  
}
