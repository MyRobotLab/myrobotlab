package org.myrobotlab.framework.interfaces;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import org.myrobotlab.framework.Inbox;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.Outbox;

public interface ServiceInterface
    extends ServiceQueue, LoggingSink, NameTypeProvider, MessageSubscriber, MessageSender, StateSaver, Invoker, StatePublisher, StatusPublisher, ServiceStatus, Attachable {

  /**
   * this is a local method which adds a request from some foreign service with
   * address information (otherService/callback) for a topic callback Adds an
   * entry on the notify list
   * 
   * @param localTopic
   *          l
   * @param otherService
   *          o
   * @param callback
   *          c
   * 
   */
  
  /**
   * virtualize the service, in this mode the service should not use any "real" hardware
   * @param b
   * @return 
   */
  public boolean setVirtual(boolean b);
  
  /**
   * check to see if the service is running in a virtual mode
   * @return
   */
  public boolean isVirtual();
  
  // FIXME - remove callback
  public void addListener(String localTopic, String otherService, String callback);

  // FIXME - remove callback
  public void removeListener(String localTopic, String otherService, String callback);

  public String[] getDeclaredMethodNames();

  public Method[] getDeclaredMethods();

  public URI getInstanceId();

  public String[] getMethodNames();

  public Method[] getMethods();

  public ArrayList<MRLListener> getNotifyList(String key);

  public ArrayList<String> getNotifyListKeySet();

  public Inbox getInbox();

  public Outbox getOutbox();

  public String getSimpleName();

  // important to maintain a method to return canonical type
  // important in the future when other services are expressed differently
  // e.g.(node js services)
  public String getType();

  public boolean hasPeers();

  /**
   * recursive release - releases all peers and their peers etc. then releases
   * this service
   */
  public void releasePeers();

  public void releaseService();

  /**
   * called by runtime when system is shutting down a service can use this
   * method when it has to do some "ordered" cleanup
   */
  public void preShutdown();

  /**
   * asked by the framework - to determine if the service needs to be secure
   * 
   * @return true/false
   */
  public boolean requiresSecurity();

  public void setInstanceId(URI uri);

  public void setName(String prefix);

  public void startService();

  /**
   * loads json config and starts the service
   */
  public void loadAndStart();

  public void stopService();

  public String clearLastError();

  public boolean hasError();

  public void out(String method, Object retobj);

  public boolean isRuntime();

  // FIXME - meta data needs to be infused into instance
  public String getDescription();

  public Map<String, MethodEntry> getMethodMap();

  public boolean isReady();

  public boolean isRunning();

  /**
   * the order this service was created in relation to the other service
   * @param creationCount
   */
  public void setOrder(int creationCount);
  
  public String getId();

  public String getFullName();

}
