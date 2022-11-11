package org.myrobotlab.framework.interfaces;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.myrobotlab.framework.Inbox;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public interface ServiceInterface extends ServiceQueue, LoggingSink, NameTypeProvider, MessageSubscriber, MessageSender, StateSaver, Invoker, StatePublisher, StatusPublisher,
    ServiceStatus, TaskManager, Attachable, Comparable<ServiceInterface> {

  // does this work ?
  public final static Logger log = LoggerFactory.getLogger(Service.class);

  /**
   * When set service will attempt to provide services with no hardware
   * dependencies. Some services have the capablity to mock hardware such as the
   * Serial and Arduino services.
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

  @Override
  public String getSimpleName();

  /**
   * equivalent to getClass().getCanonicalName()
   * 
   * @return
   */
  public String getType();

  /**
   * Does the meta data of this service define peers
   * 
   * @return
   */
  public boolean hasPeers();

  /**
   * Service life-cycle method: releaseService will call stopService, release
   * its peers, do any derived business logic to release resources, then
   * un-register itself
   */
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

  /**
   * Service life cycle method - calls create, and starts any necessary
   * resources to function
   */
  public void startService();

  /**
   * @return get a services current config
   *
   */
  public ServiceConfig getConfig();

  /**
   * sets config - just before apply
   * 
   * @param config
   */
  public void setConfig(ServiceConfig config);

  /**
   * Configure a service by merging in configuration
   * 
   * @param config
   *          the config to load
   * @return the loaded config.
   */
  public ServiceConfig apply(ServiceConfig config);

  /**
   * loads json config and starts the service
   */
  public void loadAndStart();

  /**
   * Service life-cycle method, stops the inbox and outbox threads - typically
   * does not release "custom" resources. It's purpose primarily is to stop
   * messaging from flowing in or out of this service - which is handled in the
   * base Service class. Most times this method will not need to be overriden
   */
  public void stopService();

  public String clearLastError();

  public boolean hasError();

  public void out(String method, Object retobj);

  public boolean isRuntime();

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

  /***
   * When this service is started and has peers auto started peers are added on
   * starting. Shared peers will be already started and not added to this set.
   * When the service is released, peers are automatically released, except for
   * the ones not started by this service.
   * 
   * @param actualPeerName
   */
  public void addAutoStartedPeer(String actualPeerName);

  /**
   * When this service is releasing it will only remove the peers it started
   * this method allows that check.
   * 
   * @param actualPeerName
   * @return
   */
  public boolean autoStartedPeersContains(String actualPeerName);

  public MetaData getMetaData();

}
