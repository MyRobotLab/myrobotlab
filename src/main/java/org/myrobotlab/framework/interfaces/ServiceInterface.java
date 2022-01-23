package org.myrobotlab.framework.interfaces;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.framework.Inbox;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.ServiceLifeCycleListener;
import org.slf4j.Logger;

public interface ServiceInterface extends ServiceLifeCycleListener, ServiceQueue, LoggingSink, NameTypeProvider, MessageSubscriber, MessageSender, StateSaver, Invoker,
    StatePublisher, StatusPublisher, ServiceStatus, TaskManager, Attachable, Comparable<ServiceInterface> {

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
   * recursive release - releases all peers and their peers etc. then releases
   * this service
   */
  public void releasePeers();

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

  static public LinkedHashMap<String, ServiceConfig> getDefault(String name, String type) {
    LinkedHashMap<String, ServiceConfig> config = new LinkedHashMap<>();
    try {

      String configClass = "org.myrobotlab.service." + type;

      Class<?> clazz = Class.forName(configClass);
      Method method = clazz.getMethod("getDefault", String.class);

      // create new instance
      // Constructor<?> ctor = clazz.getConstructor();
      // Object configObject = ctor.newInstance();

      // I chose "non"-static method for getDefault - because Java has
      // an irritating rule of not allowing static overloads and abstracts
      config = (LinkedHashMap<String, ServiceConfig>) method.invoke(null, name);

      if (config == null || config.keySet().size() == 0) {
        log.warn("{} does not currently have any default configurations", configClass);
      }
    } catch (NoSuchMethodException em) {

      try {

        log.info("{} of type {} does not have a getDefault", name, type);

        String configClass = "org.myrobotlab.service.config." + type + "Config";

        Class<?> clazz = Class.forName(configClass);

        // create new instance
        Constructor<?> ctor = clazz.getConstructor();
        ServiceConfig configObject = (ServiceConfig) ctor.newInstance();

        // I chose "non"-static method for getDefault - because Java has
        // an irritating rule of not allowing static overloads and abstracts
        config.put(name, configObject);

      } catch (Exception e) {
        log.warn("{} of type {} does not have a Config object - creating default service config", name, type, e);
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.type = type;
        config.put(name, serviceConfig);
      }

    } catch (Exception e) {
      log.error("ServiceConfig.getDefault({},{}) threw", name, type, e);
    }
    return config;
  }

}
