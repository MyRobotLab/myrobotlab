package org.myrobotlab.framework.interfaces;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.myrobotlab.framework.Inbox;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.ServiceLifeCycleListener;

public interface ServiceInterface extends ServiceLifeCycleListener, ServiceQueue, LoggingSink, NameTypeProvider, MessageSubscriber, MessageSender, StateSaver, Invoker,
    StatePublisher, StatusPublisher, ServiceStatus, Attachable, Comparable<ServiceInterface> {

  /**
   * this is a local method which adds a request from some foreign service with
   * address information (otherService/callback) for a topic callback Adds an
   * entry on the notify list
   * 
   * 
   * virtualize the service, in this mode the service should not use any "real"
   * hardware
   * 
   * @param b
   *          true to set the virtual mode
   * @return the value
   * 
   */
  public boolean setVirtual(boolean b);

  /**
   * check to see if the service is running in a virtual mode
   * 
   * @return true if in virtual mode.
   * 
   */
  public boolean isVirtual();

  public String[] getDeclaredMethodNames();

  public Method[] getDeclaredMethods();

  public URI getInstanceId();

  public String[] getMethodNames();

  public Method[] getMethods();

  public List<MRLListener> getNotifyList(String key);

  public List<String> getNotifyListKeySet();

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
   * @return get a services current config
   *
   */
  public ServiceConfig getConfig();

  /**
   * Configure a service by merging in configuration
   * 
   * @param config
   *          the config to load
   * @return the loaded config.
   */
  public ServiceConfig load(ServiceConfig config);

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
   * @param creationCount
   *          the order this service was created in relation to the other
   *          service
   */
  public void setOrder(int creationCount);

  public String getId();

  public String getFullName();

  public void loadLocalizations();

  public void setLocale(String code);

  public int getCreationOrder();
  
  public HashMap<String, Timer> getTasks();

  public void purgeTask(String taskName);

}
