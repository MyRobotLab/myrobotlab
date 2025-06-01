/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.framework;

// java or mrl imports only - no dependencies !
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.config.ConfigUtils;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.Broadcaster;
import org.myrobotlab.framework.interfaces.ConfigurableService;
import org.myrobotlab.framework.interfaces.FutureInvoker;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.image.Util;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.ServiceConfig.Listener;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.interfaces.AuthorizationProvider;
import org.myrobotlab.service.interfaces.QueueReporter;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.myrobotlab.string.StringUtil;
import org.slf4j.Logger;

/**
 * 
 * Service is the base of the MyRobotLab Service Oriented Architecture. All
 * meaningful Services derive from the Service class. There is a
 * _TemplateService.java in the org.myrobotlab.service package. This can be used
 * as a very fast template for creating new Services. Each Service begins with
 * two threads One is for the "OutBox" this delivers messages out of the
 * Service. The other is the "InBox" thread which processes all incoming
 * messages.
 * 
 */
public abstract class Service<T extends ServiceConfig> implements Runnable, Serializable, ServiceInterface, Broadcaster,
    QueueReporter, FutureInvoker, ConfigurableService<T> {

  // FIXME upgrade to ScheduledExecutorService
  // http://howtodoinjava.com/2015/03/25/task-scheduling-with-executors-scheduledthreadpoolexecutor-example/

  /**
   * contains all the meta data about the service - pulled from the static
   * method getMetaData() each instance will call the method and populate the
   * data for an instance
   * 
   */
  protected MetaData serviceType;

  /**
   * Config member - configuration of type {ServiceType}Config Runtime applys
   * either the default config or a saved config during service creation
   */
  protected T config;

  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(Service.class);

  /**
   * key into Runtime's hosts of ServiceEnvironments mrlscheme://[gateway
   * name]/scheme://key for gateway mrl://gateway/xmpp://incubator incubator if
   * host == null the service is local
   */
  private URI instanceId = null;

  /**
   * unique name of the service (eqv. hostname)
   */
  private String name;

  /**
   * unique id - (eqv. domain suffix)
   */
  protected String id;

  /**
   * simpleName used in serialization
   */
  protected String simpleName;

  /**
   * full class name used in serialization
   */
  protected String typeKey;

  private boolean isRunning = false;

  transient protected Thread thisThread = null;

  final transient protected Inbox inbox;

  final protected Outbox outbox;

  protected String serviceVersion = null;

  /**
   * default en.properties - if there is one
   */
  protected Properties defaultLocalization = null;

  /**
   * map of keys to localizations -
   * 
   * <pre>
   *  Match Service with current Locale of the Runtime service
   *  Match Service with Default (English) Locale
   *  Match Runtime with current Locale of the Runtime service.
   *  Match Runtime with Default (English) Locale
   * </pre>
   * 
   * service specific - then runtime
   */
  protected transient Properties localization = null;

  /**
   * for promoting portability and good pathing
   */
  final transient protected static String fs = File.separator;

  /**
   * for promoting portability and good pathing
   */
  final transient protected String ps = File.pathSeparator;

  /**
   * a more capable task handler
   */
  transient Map<String, Timer> tasks = new HashMap<String, Timer>();

  /**
   * used as a static cache for quick method name testing FIXME - if you make
   * this static it borks things - not sure why this should be static info and
   * should not be a member variable !
   */
  transient protected Set<String> methodSet;

  /**
   * This is the map of interfaces - its really "static" information, since its
   * a definition. However, since serialization will not process statics - we
   * are making it a member variable
   */
  // FIXME - this should be a map
  protected Map<String, String> interfaceSet;

  /**
   * order which this service was created
   */
  int creationOrder = 0;

  // FIXME SecurityProvider
  protected transient AuthorizationProvider authProvider = null;

  protected Status lastError = null;
  protected Long lastErrorTs = null;
  protected Status lastStatus = null;
  protected Long lastStatusTs = null;
  protected long statusBroadcastLimitMs = 1000;

  /**
   * variable for services to virtualize some of their dependencies - defaults
   * to be the same as Runtime's unless explicitly set
   */
  protected boolean isVirtual = false;

  /**
   * overload this if your service needs other environmental or dependencies to
   * be ready
   */
  protected boolean ready = true;

  /**
   * Locale for the service - defaults to be the same as Runtime's unless
   * explicitly set
   */
  protected Locale locale;

  /**
   * copyShallowFrom is used to help maintain state information with
   * 
   * @param target
   *               t
   * @param source
   *               s
   * @return o
   */
  public static Object copyShallowFrom(Object target, Object source) {
    if (target == source) { // data is myself - operating on local copy
      return target;
    }
    Set<Class<?>> ancestry = new HashSet<Class<?>>();
    Class<?> targetClass = source.getClass();

    ancestry.add(targetClass);

    // if we are a org.myrobotlab object climb up the ancestry to
    // copy all super-type fields ...
    // GroG says: I wasn't comfortable copying of "Service" - because its never
    // been tested before - so we copy all definitions from
    // other superclasses e.g. - org.myrobotlab.service.abstracts
    // it might be safe in the future to copy all the way up without stopping...
    while (targetClass.getCanonicalName().startsWith("org.myrobotlab")
        && !targetClass.getCanonicalName().startsWith("org.myrobotlab.framework")) {
      ancestry.add(targetClass);
      targetClass = targetClass.getSuperclass();
    }

    for (Class<?> sourceClass : ancestry) {

      Field[] fields = sourceClass.getDeclaredFields();
      for (Field field : fields) {
        try {

          int modifiers = field.getModifiers();

          // if (Modifier.isPublic(mod)
          // !(Modifier.isPublic(f.getModifiers())
          // Hmmm JSON mappers do hacks to get by
          // IllegalAccessExceptions.... Hmmmmm

          // GROG - recent change from this
          // if ((!Modifier.isPublic(modifiers)
          // to this
          String fname = field.getName();
          /*
           * if (fname.equals("desktops") || fname.equals("useLocalResources")
           * ){ log.info("here"); }
           */

          if (Modifier.isPrivate(modifiers) || fname.equals("log") || Modifier.isTransient(modifiers)
              || Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
            log.debug("skipping {}", field.getName());
            continue;
          } else {
            log.debug("copying {}", field.getName());
          }
          Type t = field.getType();

          // log.info(String.format("setting %s", f.getName()));
          /*
           * if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
           * continue; }
           */

          // GroG - this is new 1/26/2017 - needed to get webgui data to
          // load
          field.setAccessible(true);
          Field targetField = sourceClass.getDeclaredField(field.getName());
          targetField.setAccessible(true);

          if (t.equals(Boolean.TYPE)) {
            targetField.setBoolean(target, field.getBoolean(source));
          } else if (t.equals(Character.TYPE)) {
            targetField.setChar(target, field.getChar(source));
          } else if (t.equals(Byte.TYPE)) {
            targetField.setByte(target, field.getByte(source));
          } else if (t.equals(Short.TYPE)) {
            targetField.setShort(target, field.getShort(source));
          } else if (t.equals(Integer.TYPE)) {
            targetField.setInt(target, field.getInt(source));
          } else if (t.equals(Long.TYPE)) {
            targetField.setLong(target, field.getLong(source));
          } else if (t.equals(Float.TYPE)) {
            targetField.setFloat(target, field.getFloat(source));
          } else if (t.equals(Double.TYPE)) {
            targetField.setDouble(target, field.getDouble(source));
          } else {
            // log.debug(String.format("setting reference to remote
            // object %s", f.getName()));
            targetField.set(target, field.get(source));
          }
        } catch (Exception e) {
          log.error("copy failed source {} to a {}", source, target, e);
        }
      } // for each field in class
    } // for each in ancestry
    return target;
  }

  public static String getHostName(final String inHost) {
    if (inHost != null)
      return inHost;

    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      log.error("could not find host, host is null or empty !");
    }

    return "localhost"; // no network - still can't be null // chumby
  }

  static public void logTimeEnable(Boolean b) {
    Logging.logTimeEnable(b);
  }

  public boolean setSecurityProvider(AuthorizationProvider provider) {
    if (authProvider != null) {
      log.error("security provider is already set - it can not be unset .. THAT IS THE LAW !!!");
      return false;
    }

    authProvider = provider;
    return true;
  }

  /**
   * sleep without the throw
   * 
   * @param millis
   *               the time in milliseconds
   * 
   */
  public static void sleep(int millis) {
    sleep((long) millis);
  }

  public static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
    }
  }

  public static String stackToString(final Throwable e) {
    StringWriter sw;
    try {
      sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
    } catch (Exception e2) {
      return "bad stackToString";
    }
    return sw.toString();
  }

  public String getRootDataDir() {
    return Runtime.DATA_DIR;
  }

  public String getHomeDir() {
    return System.getProperty("user.home");
  }

  static public String getDataDir(String typeName) {
    String dataDir = Runtime.DATA_DIR + fs + typeName;
    File f = new File(dataDir);
    if (!f.exists()) {
      if (!f.mkdirs()) {
        log.error("Cannot create data directory: %s", dataDir);
      }
    }
    return dataDir;
  }

  public String getDataDir() {
    return getDataDir(getClass().getSimpleName());
  }

  public String getDataInstanceDir() {
    String dataDir = Runtime.DATA_DIR + fs + getClass().getSimpleName() + fs + getName();
    File f = new File(dataDir);
    if (!f.exists()) {
      if (!f.mkdirs()) {
        error("Cannot create data directory: %s", dataDir);
      }
    }
    return dataDir;
  }

  // ============== resources begin ======================================

  /**
   * Non-static getResourceDir() will return /resource/{service type name} e.g.
   * /resource/Arduino
   * 
   * @return the resource directory
   * 
   */
  public String getResourceDir() {
    return getResourceDir(getClass());
  }

  /**
   * Static getResourceDir(Class clazz) will return the appropriate resource
   * directory, typically it will be /resource/{MetaData} but depending if run
   * in the presence of other developing directories.
   * 
   * @param clazz
   *              the class name
   * @return the resource dir
   * 
   */
  static public String getResourceDir(Class<?> clazz) {
    return getResourceDir(clazz.getSimpleName(), null);
  }

  static public String getResourceDir(Class<?> clazz, String additionalPath) {
    return getResourceDir(clazz.getSimpleName(), additionalPath);
  }

  /**
   * getResourceDir gets the appropriate resource path for any resource supplied
   * in additionalPath. This is a private method, if you need a resource, use
   * getResource or getResourceAsString
   * 
   * <pre>
   * Order of increasing precedence is:
   *     1. resource
   *     2. src/resource/{MetaData} or
   *     3. ../{MetaData}/resource/{MetaData}
   * </pre>
   * 
   * @param serviceType
   *                       the type of service
   * @param additionalPath
   *                       to glue together
   * @return the full resolved path
   * 
   *         FIXME - DO NOT USE STATIC !!!! all instances of services should be
   *         able to get the resource directory If its static and "configurable"
   *         then it needs an instance of Runtime which is not available.
   * 
   */
  static public String getResourceDir(String serviceType, String additionalPath) {

    // setting resource directory
    String resource = ConfigUtils.getResourceRoot() + fs + serviceType;

    if (additionalPath != null) {
      resource = FileIO.gluePaths(resource, additionalPath);
    }
    return resource;
  }

  /**
   * non static get resource path return the path to a resource - since the root
   * can change depending if in debug or runtime - it gets the appropriate root
   * and adds the additionalPath..
   * 
   * @param additionalPath
   *                       additional paths to add to the resource path
   * @return the combined file path
   * 
   */
  public String getResourcePath(String additionalPath) {
    return FileIO.gluePaths(getResourceDir(), additionalPath);
  }

  /**
   * All resource access should be using this method. Util.getResource... should
   * be deprecated. This should be the one source which determines the location
   * and resolves the priority of setting this configuration
   * 
   * @return the root folder for the resource dir
   * 
   */

  static public String getResourceRoot() {
    return ConfigUtils.getResourceRoot();
  }

  /**
   * 
   * @return list of resources for this service top level
   * 
   */
  public File[] getResourceDirList() {
    return getResourceDirList(null);
  }

  /**
   * Get a resource, first parameter is serviceType
   * 
   * @param serviceType
   *                     - the type of service
   * @param resourceName
   *                     - the path of the resource
   * @return the bytes of the resource
   */
  static public byte[] getResource(String serviceType, String resourceName) {
    String filename = getResourceDir(serviceType, resourceName);
    File f = new File(filename);
    if (!f.exists()) {
      log.error("resource {} does not exist", f);
      return null;
    }
    byte[] content = null;
    try {
      content = Files.readAllBytes(Paths.get(filename));
    } catch (IOException e) {
      log.error("getResource threw", e);
    }
    return content;
  }

  public byte[] getResource(String resourceName) {
    return getResource(getClass(), resourceName);
  }

  /**
   * static getResource(Class, resourceName) to access a different services
   * resources
   * 
   * @param clazz
   *                     the class
   * @param resourceName
   *                     the resource name
   * @return bytes of the resource
   * 
   */
  static public byte[] getResource(Class<?> clazz, String resourceName) {
    return getResource(clazz.getSimpleName(), resourceName);
  }

  /**
   * Get a resource as a string. This will follow the conventions of finding the
   * appropriate resource dir
   * 
   * @param resourceName
   *                     the name of the resource
   * @return the string of the bytes , assuming utf-8
   * 
   */
  public String getResourceAsString(String resourceName) {
    byte[] data = getResource(resourceName);
    if (data != null) {
      try {
        return new String(data, "UTF-8");
      } catch (Exception e) {
        log.error("getResourceAsString threw", e);
      }
    }
    return null;
  }

  static public String getResourceAsString(Class<?> clazz, String resourceName) {
    return getResourceAsString(clazz.getSimpleName(), resourceName);
  }

  static public String getResourceAsString(String serviceType, String resourceName) {
    byte[] data = getResource(serviceType, resourceName);
    if (data != null) {
      try {
        return new String(data, "UTF-8");
      } catch (Exception e) {
        log.error("getResourceAsString threw", e);
      }
    }
    return null;
  }

  /**
   * Constructor of service, reservedkey typically is a services name and inId
   * will be its process id
   * 
   * @param reservedKey
   *                    the service name
   * @param inId
   *                    process id
   * 
   */
  public Service(String reservedKey, String inId) {
    log.info("constructing {}", reservedKey);
    name = reservedKey;

    // necessary for serialized transport\
    if (inId == null) {
      id = ConfigUtils.getId();
      log.debug("creating local service for id {}", id);
    } else {
      id = inId;
      log.debug("creating remote proxy service for id {}", id);
    }

    typeKey = this.getClass().getCanonicalName();
    simpleName = this.getClass().getSimpleName();
    MethodCache cache = MethodCache.getInstance();
    cache.cacheMethodEntries(this.getClass());

    serviceType = MetaData.get(getClass().getSimpleName());

    // FIXME - this is 'sort-of' static :P
    if (methodSet == null) {
      methodSet = getMessageSet();
    }

    interfaceSet = getInterfaceSet();

    if (locale == null) {
      if (!Runtime.isRuntime(this)) {
        locale = Runtime.getInstance().getLocale();
      } else {
        // is runtime
        locale = Locale.getDefault();
      }
    }

    // load appropriate localization properties based on current local language
    loadLocalizations();

    this.inbox = new Inbox(getFullName());
    this.outbox = new Outbox(getFullName());

    File versionFile = new File(getResourceDir() + fs + "version.txt");
    if (versionFile.exists()) {
      try {
        String version = FileIO.toString(versionFile);
        if (version != null) {
          serviceVersion = version.trim();
        }
      } catch (Exception e) {
        log.error("extracting service version info threw", e);
      }
    }

    // register this service if local - if we are a foreign service, we probably
    // are being created in a
    // registration already
    if (id.equals(ConfigUtils.getId())) {
      Registration registration = new Registration(this);
      Runtime.register(registration);
    }
  }

  /**
   * 
   * @param additionalPath
   *                       get a list of resource files in a resource path
   * @return list of files
   * 
   */
  public File[] getResourceDirList(String additionalPath) {
    String resDir = getResourceDir(getClass(), additionalPath);
    File f = new File(resDir);
    return f.listFiles();
  }

  /**
   * new overload - mqtt uses this for json encoded MrlListener to process
   * subscriptions
   * 
   * @param data
   *             - listener callback info
   */
  public void addListener(Map data) {
    // {topicMethod=pulse, callbackName=mqtt01, callbackMethod=onPulse}
    if (!data.containsKey("topicMethod")) {
      error("addListener topicMethod missing");
    }
    if (!data.containsKey("callbackName")) {
      error("addListener callbackName missing");
    }
    if (!data.containsKey("callbackMethod")) {
      error("addListener callbackMethod missing");
    }
    addListener(data.get("topicMethod").toString(), data.get("callbackName").toString(),
        data.get("callbackMethod").toString());
  }

  public void addListener(MRLListener listener) {
    addListener(listener.topicMethod, listener.callbackName, listener.callbackMethod);
  }

  @Override
  public void addListener(String localMethod, String remoteName) {
    addListener(localMethod, remoteName, CodecUtils.getCallbackTopicName(localMethod));
  }

  /**
   * adds a MRL message listener to this service this is the result of a
   * "subscribe" from a different service FIXME !! - implement with HashMap or
   * HashSet .. WHY ArrayList ???
   * 
   * @param localMethod
   *                     - method when called, it's return will be sent to the
   *                     remoteName.remoteMethod
   * @param remoteName
   *                     - name of the service to send return message to
   * @param remoteMethod
   *                     - name of the method to send return data to
   */
  @Override
  public void addListener(String localMethod, String remoteName, String remoteMethod) {
    remoteName = CodecUtils.getFullName(remoteName);
    MRLListener listener = new MRLListener(localMethod, remoteName, remoteMethod);
    if (outbox.notifyList.containsKey(listener.topicMethod)) {
      // iterate through all looking for duplicate
      boolean found = false;
      List<MRLListener> nes = outbox.notifyList.get(listener.topicMethod);
      for (int i = 0; i < nes.size(); ++i) {
        MRLListener entry = nes.get(i);
        if (entry.equals(listener)) {
          log.debug("attempting to add duplicate MRLListener {}", listener);
          found = true;
          break;
        }
      }
      if (!found) {
        log.debug("adding addListener from {}.{} to {}.{}", this.getName(), listener.topicMethod, listener.callbackName,
            listener.callbackMethod);
        nes.add(listener);
      }
    } else {
      List<MRLListener> notifyList = new CopyOnWriteArrayList<MRLListener>();
      notifyList.add(listener);
      log.debug("adding addListener from {}.{} to {}.{}", this.getName(), listener.topicMethod, listener.callbackName,
          listener.callbackMethod);
      outbox.notifyList.put(listener.topicMethod, notifyList);
    }
  }

  public boolean hasSubscribed(String listener, String topicMethod) {
    List<MRLListener> nes = outbox.notifyList.get(topicMethod);
    for (MRLListener ne : nes) {
      if (ne.callbackName.contentEquals(listener)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void addTask(long intervalMs, String method) {
    addTask(intervalMs, method, new Object[] {});
  }

  @Override
  public void addTask(long intervalMs, String method, Object... params) {
    addTask(method, intervalMs, 0, method, params);
  }

  @Override
  public void addTaskOneShot(long delayMs, String method, Object... params) {
    addTask(method, true, 0, delayMs, method, params);
  }

  @Override
  synchronized public void addTask(String taskName, long intervalMs, long delayMs, String method, Object... params) {
    addTask(taskName, false, intervalMs, delayMs, method, params);
  }

  /**
   * a stronger bigger better task handler !
   * 
   * @param taskName
   *                   task name
   * @param intervalMs
   *                   how frequent in milliseconds
   * @param delayMs
   *                   the delay
   * @param method
   *                   the method
   * @param params
   *                   the params to pass
   */
  @Override
  synchronized public void addTask(String taskName, boolean oneShot, long intervalMs, long delayMs, String method,
      Object... params) {
    if (tasks.containsKey(taskName)) {
      log.info("already have active task \"{}\"", taskName);
      return;
    }
    Timer timer = new Timer(String.format("%s.timer", String.format("%s.%s", getName(), taskName)));
    Message msg = Message.createMessage(getFullName(), getFullName(), method, params);
    Task task = new Task(this, oneShot, taskName, intervalMs, msg);
    timer.schedule(task, delayMs);
    tasks.put(taskName, timer);
  }

  @Override
  public Map<String, Timer> getTasks() {
    return tasks;
  }

  @Override
  public boolean containsTask(String taskName) {
    return tasks.containsKey(taskName);
  }

  @Override
  final public void invokeFuture(String method, long delayMs) {
    invokeFuture(method, delayMs, (Object[]) null);
  }

  /**
   * creates a one timed task that executes in the future delayMs milliseconds
   */
  @Override
  final public void invokeFuture(String method, long delayMs, Object... params) {
    addTask(String.format("%s-%d", method, System.currentTimeMillis()), true, 0, delayMs, method, params);
  }

  @Override
  synchronized public void purgeTask(String taskName) {
    if (tasks.containsKey(taskName)) {
      log.debug("remove task {}", taskName);
      Timer timer = tasks.get(taskName);
      if (timer != null) {
        try {
          timer.cancel();
          timer.purge();
          timer = null;
        } catch (Exception e) {
          log.info(e.getMessage());
        }
      }
    } else {
      log.debug("purgeTask - task {} does not exist", taskName);
    }
    tasks.remove(taskName);
  }

  @Override
  public void purgeTasks() {
    for (String taskName : tasks.keySet()) {
      Timer timer = tasks.get(taskName);
      if (timer != null) {
        try {
          timer.purge();
          timer.cancel();
          timer = null;
        } catch (Exception e) {
          log.info(e.getMessage());
        }
      }
    }
    tasks.clear();
  }

  @Override
  public Service broadcastState() {
    invoke("publishState");
    return this;
  }

  @Override
  @Deprecated /* use publishStatus */
  public void broadcastStatus(Status status) {
    invoke("publishStatus", status);
  }

  @Override
  public String clearLastError() {
    String le = null;
    if (lastError != null) {
      le = lastError.toString();
    }

    lastError = null;
    return le;
  }

  public void close(Writer w) {
    if (w == null) {
      return;
    }
    try {
      w.flush();
    } catch (Exception e) {
      Logging.logError(e);
    } finally {
      try {
        w.close();
      } catch (Exception e) {
        // don't really care
      }
    }
  }

  @Override
  public String[] getDeclaredMethodNames() {
    Method[] methods = getDeclaredMethods();
    String[] ret = new String[methods.length];

    log.info("getDeclaredMethodNames loading {} non-sub-routable methods", methods.length);
    for (int i = 0; i < methods.length; ++i) {
      ret[i] = methods[i].getName();
    }
    Arrays.sort(ret);
    return ret;
  }

  @Override
  public Method[] getDeclaredMethods() {
    return this.getClass().getDeclaredMethods();
  }

  @Override
  public Inbox getInbox() {
    return inbox;
  }

  @Override
  public URI getInstanceId() {
    return instanceId;
  }

  public String getIntanceName() {
    return name;
  }

  @Override
  public Status getLastError() {
    return lastError;
  }

  // FIXME - use the method cache
  public Set<String> getMessageSet() {
    Set<String> ret = new TreeSet<>();
    Method[] methods = getMethods();
    log.debug("getMessageSet loading {} non-sub-routable methods", methods.length);
    for (Method method : methods) {
      ret.add(method.getName());
    }
    return ret;
  }

  // FIXME - should be a "Set" not an array !
  @Override
  public String[] getMethodNames() {
    Method[] methods = getMethods();
    /*
     * Set<String> m = new TreeSet<String>(); m.addAll(methods);
     */
    String[] ret = new String[methods.length];

    log.info("getMethodNames loading {} non-sub-routable methods", methods.length);
    for (int i = 0; i < methods.length; ++i) {
      ret[i] = methods[i].getName();
    }

    Arrays.sort(ret);

    return ret;
  }

  @Override
  public Method[] getMethods() {
    return this.getClass().getMethods();
  }

  /**
   * Returns a map containing all interface names from the class hierarchy and
   * the interface hierarchy of the current class.
   *
   * @return A map containing all interface names.
   */
  public Map<String, String> getInterfaceSet() {
    Map<String, String> ret = new TreeMap<>();
    Set<Class<?>> visitedClasses = new HashSet<>();
    getAllInterfacesHelper(getClass(), ret, visitedClasses);
    return ret;
  }

  /**
   * Recursively traverses the class hierarchy and the interface hierarchy to
   * add all interface names to the specified map.
   *
   * @param c
   *                       The class to start the traversal from.
   * @param ret
   *                       The map to store the interface names.
   * @param visitedClasses
   *                       A set to keep track of visited classes to avoid
   *                       infinite loops.
   */
  private void getAllInterfacesHelper(Class<?> c, Map<String, String> ret, Set<Class<?>> visitedClasses) {
    if (c != null && !visitedClasses.contains(c)) {
      // Add interfaces from the current class
      Class<?>[] interfaces = c.getInterfaces();
      for (Class<?> interfaze : interfaces) {
        ret.put(interfaze.getName(), interfaze.getName());
      }

      // Add interfaces from interfaces implemented by the current class
      for (Class<?> interfaze : interfaces) {
        getAllInterfacesHelper(interfaze, ret, visitedClasses);
      }

      // Recursively traverse the superclass hierarchy
      visitedClasses.add(c);
      getAllInterfacesHelper(c.getSuperclass(), ret, visitedClasses);
    }
  }

  public Message getMsg() throws InterruptedException {
    return inbox.getMsg();
  }

  /**
   * 
   */
  @Override
  public List<MRLListener> getNotifyList(String key) {
    if (getOutbox() == null) {
      // this is remote system - it has a null outbox, because its
      // been serialized with a transient outbox
      // and your in a skeleton
      // use the runtime to send a message
      // FIXME - parameters !
      try {
        return (ArrayList<MRLListener>) Runtime.getInstance().sendBlocking(getName(), "getNotifyList",
            new Object[] { key });
      } catch (Exception e) {
        log.error("remote getNotifyList threw", e);
        return null;
      }

    } else {
      return getOutbox().notifyList.get(key);
    }
  }

  @Override
  public ArrayList<String> getNotifyListKeySet() {
    if (getOutbox() == null) {
      // this is remote system - it has a null outbox, because its
      // been serialized with a transient outbox
      // and your in a skeleton
      // use the runtime to send a message

      try {
        return (ArrayList<String>) Runtime.getInstance().sendBlocking(getFullName(), "getNotifyListKeySet");
      } catch (Exception e) {
        log.error("remote getNotifyList threw", e);
        return null;
      }

    } else {
      return new ArrayList<>(getOutbox().notifyList.keySet());
    }
  }

  @Override
  public Outbox getOutbox() {
    return outbox;
  }

  @Override
  public String getSimpleName() {
    return simpleName;
  }

  public Thread getThisThread() {
    return thisThread;
  }

  @Override
  public String getTypeKey() {
    return typeKey;
  }

  @Override
  public boolean hasError() {
    return lastError != null;
  }

  @Override
  public Map<String, Peer> getPeers() {
    if (getConfig() == null) {
      return null;
    }
    return getConfig().getPeers();
  }

  /**
   * returns the peer key if a name is supplied and matches a peer name
   * 
   * @param name
   *             - name of service
   * @return - key of peer if it exists
   */
  public String getPeerKey(String name) {
    Map<String, Peer> peers = getPeers();
    if (peers != null) {
      for (String peerKey : peers.keySet()) {
        Peer peer = peers.get(peerKey);
        if (name.equals(peer.name)) {
          return peerKey;
        }
      }
    }
    return null;
  }

  @Override
  public Set<String> getPeerKeys() {
    if (getConfig() == null || getConfig().peers == null) {
      return new HashSet<>();
    }
    return getConfig().peers.keySet();
  }

  public String help(String format, String level) {
    StringBuilder sb = new StringBuilder();
    Method[] methods = this.getClass().getDeclaredMethods();
    TreeMap<String, Method> sorted = new TreeMap<>();

    for (Method m : methods) {
      sorted.put(m.getName(), m);
    }
    for (String key : sorted.keySet()) {
      Method m = sorted.get(key);
      sb.append("/").append(getName()).append("/").append(m.getName());
      Class<?>[] types = m.getParameterTypes();
      for (Class<?> c : types) {
        sb.append("/").append(c.getSimpleName());
      }
      sb.append("\n");
    }

    sb.append("\n");
    return sb.toString();
  }

  @Override
  public void in(Message msg) {
    inbox.add(msg);
  }

  /**
   * This is where all messages are routed to and processed
   */
  @Override
  final public Object invoke(Message msg) {
    Object retobj = null;

    if (log.isDebugEnabled()) {
      log.debug("--invoking {}.{}({}) {} --", name, msg.method, CodecUtils.getParameterSignature(msg.data), msg.msgId);
    }

    // recently added - to support "nameless" messages - concept you may get
    // a message at this point
    // which does not belong to you - but is for a service in the same
    // Process
    // this is to support nameless Runtime messages but theoretically it
    // could
    // happen in other situations...
    if (Runtime.getInstance().isLocal(msg) && !name.equals(msg.getName())) {
      // wrong Service - get the correct one
      if (Runtime.getService(msg.getName()) == null) {
        error("cannot get service %s", msg.getName());
        return null;
      }
      return Runtime.getService(msg.getName()).invoke(msg);
    }

    String blockingKey = String.format("%s.%s", msg.getFullName(), msg.getMethod());
    if (inbox.blockingList.containsKey(blockingKey)) {
      Object[] returnContainer = inbox.blockingList.get(blockingKey);
      if (msg.getData() == null) {
        returnContainer[0] = null;
      } else {
        // transferring data
        returnContainer[0] = msg.getData()[0];
      }

      synchronized (returnContainer) {
        inbox.blockingList.remove(blockingKey);
        returnContainer.notifyAll(); // addListener sender
      }

      return null;
    }

    retobj = invokeOn(false, this, msg.method, msg.data);

    return retobj;
  }

  @Override
  final public Object invoke(String method) {
    return invokeOn(false, this, method, (Object[]) null);
  }

  @Override
  final public Object invoke(String method, Object... params) {
    return invokeOn(false, this, method, params);
  }

  /**
   * Broadcast publishes messages synchronously without queuing ! Messages will
   * be processed on the same thread which calls broadcast. This is unlike
   * invoke, which will queue/buffer the message and wait for inbox thread to
   * pick it up.
   */
  @Override
  final public Object broadcast(String method) {
    return invokeOn(true, this, method, (Object[]) null);
  }

  /**
   * Broadcast publishes messages synchronously without queuing ! Messages will
   * be processed on the same thread which calls broadcast. This is unlike
   * invoke, which will queue/buffer the message and wait for inbox thread to
   * pick it up.
   */
  @Override
  final public Object broadcast(String method, Object... params) {
    return invokeOn(true, this, method, params);
  }

  /**
   * thread blocking invoke call on different service in the same process
   * 
   * @param serviceName
   *                    the service to invoke on
   * @param methodName
   *                    the method to invoke
   * @param params
   *                    var args of the params to pass
   * @return the returned value from invoking
   * 
   */
  final public Object invokeOn(String serviceName, String methodName, Object... params) {
    return invokeOn(false, Runtime.getService(serviceName), methodName, params);
  }

  /**
   * the core working invoke method
   * 
   * @param obj
   *                   - the object
   * @param methodName
   *                   - the method to invoke on that object
   * @param params
   *                   - the list of args to pass to the method
   * @return return object
   */
  @Override
  final public Object invokeOn(boolean blockLocally, Object obj, String methodName, Object... params) {
    Object retobj = null;
    try {
      MethodCache cache = MethodCache.getInstance();
      if (obj == null) {
        log.error("cannot invoke on a null object ! {}({})", methodName, MethodCache.formatParams(params));
        return null;
      }
      Method method = cache.getMethod(obj.getClass(), methodName, params);
      if (method == null) {
        error("could not find method %s.%s(%s)", obj.getClass().getSimpleName(), methodName,
            MethodCache.formatParams(params));
        return null; // should this be allowed to throw to a higher level ?
      }
      retobj = method.invoke(obj, params);
      if (blockLocally) {
        Outbox outbox = null;
        if (obj instanceof ServiceInterface) {
          outbox = ((ServiceInterface) obj).getOutbox();
        } else {
          return retobj;
        }

        List<MRLListener> subList = outbox.notifyList.get(methodName);
        // correct? get local (default?) gateway
        Runtime runtime = Runtime.getInstance();
        if (subList != null) {
          for (MRLListener listener : subList) {
            Message msg = Message.createMessage(getFullName(), listener.callbackName, listener.callbackMethod, retobj);
            msg.sendingMethod = methodName;
            if (runtime.isLocal(msg)) {
              ServiceInterface si = Runtime.getService(listener.callbackName);
              if (si == null) {
                log.debug("{} cannot callback to listener {} does not exist for {} ", getName(), listener.callbackName,
                    listener.callbackMethod);
              } else {
                Method m = cache.getMethod(si.getClass(), listener.callbackMethod, retobj);
                if (m == null) {

                  // attempt to get defaultInvokeMethod
                  m = cache.getDefaultInvokeMethod(si.getClass().getCanonicalName());
                  if (m != null) {
                    m.invoke(si, listener.callbackMethod, new Object[] { retobj });
                  } else {
                    log.warn("Null Method as a result of cache lookup. {} {} {}", si.getClass(),
                        listener.callbackMethod, retobj);
                  }
                } else {
                  try {
                    m.invoke(si, retobj);
                  } catch (Throwable e) {
                    // we attempted to invoke this , it blew up. Catch it here,
                    // continue
                    // through the rest of the listeners instead of bombing out.
                    log.error("Invoke blew up! on: {} calling method {} ", si.getName(), m, e);
                  }
                }
              }
            } else {
              send(msg);
            }
          }
        }
      } else {
        out(methodName, retobj);
      }
    } catch (Exception e) {
      // error(e);
      // e.getCause()
      error("could not invoke %s.%s (%s) %s - check logs for details", getName(), methodName, params, e.getCause());
      log.error("could not invoke {}.{} ({})", getName(), methodName, params, e);
    }
    return retobj;
  }

  @Override
  public boolean isLocal() {
    return instanceId == null;
  }

  @Override
  public boolean isRuntime() {
    return Runtime.class == this.getClass();
  }

  @Override
  public boolean isReady() {
    return ready;
  }

  protected void setReady(Boolean ready) {
    if (!ready.equals(this.ready)) {
      this.ready = ready;
      broadcastState();
    }
  }

  @Override
  public boolean isRunning() {
    return isRunning;
  }

  /**
   * getConfig returns current config of the service. This default super method
   * will also filter webgui subscriptions out, in addition for any local
   * subscriptions it will remove the instance "id" from any service. The reason
   * it removes the webgui subscriptions is to avoid overwelming the user when
   * modifying config. UI subscriptions tend to be very numerous and not very
   * useful to the user. The reason it removes the instance id from local
   * subscriptions is to allow the config to be used with any instance. Unless
   * the user is controlling instance id, its random every restart.
   */
  public T getConfig() {
    return config;
  }

  public ServiceConfig getPeerConfig(String peerKey) {
    return getPeerConfig(peerKey, new StaticType<ServiceConfig>() {
    });
  }

  /**
   * Get a service's peer's configuration. This method is used to get the
   * configuration of a peer service regarless if it is currently running or
   * not. If the peer is running the configuration is pulled from the active
   * peer service, if it is not currently running the configuration is read from
   * the current config set's service configuration file, if that does not exist
   * the default configuration for this peer is used.
   * 
   * @param peerKey
   *                - key of the peer service. e.g. "opencv" in the case of
   *                i01."opencv"
   * @return
   */
  public <P extends ServiceConfig> P getPeerConfig(String peerKey, StaticType<P> type) {
    String peerName = getPeerName(peerKey);
    if (peerName == null) {
      error("peer name not found for peer key %s", peerKey);
      return null;
    }

    // Java generics don't let us create a new StaticType using
    // P here because the type variable is erased, so we have to cast anyway for
    // now
    ConfigurableService<P> si = (ConfigurableService<P>) Runtime.getService(peerName);
    if (si != null) {
      // peer is currently running - get its config
      P c = si.getConfig();
      if (type.asClass().isAssignableFrom(c.getClass())) {
        return c;
      }
    }

    // peer is not currently running attempt to read from config
    Runtime runtime = Runtime.getInstance();
    // read current service config for this peer service
    P sc = runtime.readServiceConfig(peerName, type);
    if (sc == null) {
      error("peer service %s is defined, but %s.yml not available on filesystem", peerKey, peerName);
      return null;
    }
    return sc;
  }

  public void setPeerConfigValue(String peerKey, String fieldname, Object value)
      throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    ServiceConfig sc = getPeerConfig(peerKey, new StaticType<ServiceConfig>() {
    });
    if (sc == null) {
      error("invalid config for peer key %s field name %s", peerKey, fieldname);
      return;
    }
    Field field = sc.getClass().getDeclaredField(fieldname);
    field.set(sc, value);
    savePeerConfig(peerKey, sc);
    String peerName = getPeerName(peerKey);
    var cs = Runtime.getConfigurableService(peerName, new StaticType<Service<ServiceConfig>>() {
    });
    if (cs != null) {
      cs.apply(sc); // TODO - look for applies if its read from the file system
                    // it needs to update Runtime.plan
    }

    // broadcast change
    invoke("getPeerConfig", peerKey);
    Runtime runtime = Runtime.getInstance();
    runtime.broadcastState();
  }

  /**
   * Super class apply using template type. The default assigns config of the
   * templated type, and also add listeners from subscriptions found on the base
   * class ServiceConfig.listeners
   */
  public T apply(T c) {
    config = c;
    addConfigListeners(c);
    return config;
  }

  /**
   * The basic ServiceConfig has a list of listeners. These are definitions of
   * other subscribers subscribing for data from this service. This method
   * processes those listeners and adds them to the outbox notifyList.
   */
  public ServiceConfig addConfigListeners(ServiceConfig config) {
    if (config != null && config.listeners != null) {
      for (Listener listener : config.listeners) {
        addListener(listener.method, listener.listener, listener.callback);
      }
    }
    return config;
  }

  /**
   * Default filtered config, used when saving, can be overriden by concrete
   * class
   */
  @Override
  public ServiceConfig getFilteredConfig() {
    // Make a copy, because we don't want to modify the original
    ServiceConfig sc = CodecUtils.fromYaml(CodecUtils.toYaml(getConfig()), config.getClass());
    Map<String, List<MRLListener>> listeners = getOutbox().notifyList;
    List<Listener> newListeners = new ArrayList<>();

    // TODO - perhaps a switch for "remote" things ?
    for (String method : listeners.keySet()) {
      List<MRLListener> list = listeners.get(method);
      for (MRLListener listener : list) {
        if (!listener.callbackName.endsWith("@webgui-client")) {
          // Removes the `@runtime-id` so configs still work with local IDs
          // The StringUtils.removeEnd() call is a no-op when the ID is not our
          // local ID,
          // so doesn't conflict with remote routes
          Listener newConfigListener = new Listener(listener.topicMethod, StringUtil.removeEnd(listener.callbackName, '@' + Runtime.getInstance().getId()),
              listener.callbackMethod);
          newListeners.add(newConfigListener);
        }
      }
    }

    if (newListeners.size() > 0) {
      sc.listeners = newListeners;
    }

    if (sc.listeners != null) {
      Collections.sort(sc.listeners, new MrlListenerComparator());
    }

    return sc;
  }

  @Override
  public void setConfigValue(String fieldname, Object value)
      throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
    log.info("setting field name fieldname {} to {}", fieldname, value);

    Field field = getConfig().getClass().getDeclaredField(fieldname);
    // field.setAccessible(true); should not need this - it "should" be public
    field.set(getConfig(), value);
    save();
  }

  @Override
  @Deprecated /*
               * this is being used wrongly - Runtime knows how to load services
               * don't - what is desired here is apply()
               */
  public ServiceConfig load() throws IOException {
    Plan plan = Runtime.load(getName(), getClass().getSimpleName());
    return plan.get(getName());
  }

  @Override
  public void out(Message msg) {
    outbox.add(msg);
  }

  /**
   * Creating a message function call - without specifying the recipients -
   * static routes will be applied this is good for Motor drivers - you can swap
   * motor drivers by creating a different static route The motor is not "Aware"
   * of the driver - only that it wants to method="write" data to the driver
   */
  @Override
  public void out(String method, Object o) {
    Message m = Message.createMessage(getFullName(), null, method, o);

    if (m.sender.length() == 0) {
      m.sender = this.getFullName();
    }
    if (m.sendingMethod.length() == 0) {
      m.sendingMethod = method;
    }
    if (outbox == null) {
      log.info("******************OUTBOX IS NULL*************************");
      return;
    }
    outbox.add(m);
  }

  // override for extended functionality
  public boolean preProcessHook(Message m) {
    return true;
  }

  // override for extended functionality
  public boolean preRoutingHook(Message m) {
    return true;
  }

  /**
   * framework diagnostic publishing method for examining load, capacity, and
   * throughput of Inbox &amp; Outbox queues
   * 
   * @param stats
   *              s
   * @return the stats
   */
  public QueueStats publishQueueStats(QueueStats stats) {
    return stats;
  }

  /**
   * publishing point for the whole service the entire Service is published
   * 
   * @return the service
   */
  @Override
  public Service<T> publishState() {
    return this;
  }

  /**
   * Releases resources, and unregisters service from the runtime
   */
  @Override
  synchronized public void releaseService() {
    // auto release children and unregister
    Runtime.releaseServiceInternal(getName());
  }

  /**
   * 
   */
  public void removeAllListeners() {
    outbox.notifyList.clear();
  }

  @Override
  public void removeListener(String topicMethod, String callbackName) {
    removeListener(topicMethod, callbackName, CodecUtils.getCallbackTopicName(topicMethod));
  }

  @Override
  public void removeListener(String outMethod, String serviceName, String inMethod) {
    String fullName = CodecUtils.getFullName(serviceName);
    if (outbox.notifyList.containsKey(outMethod)) {
      List<MRLListener> nel = outbox.notifyList.get(outMethod);
      nel.removeIf(listener -> {
        if (listener == null) {
          log.info("Removing null listener for method {}", outMethod);
          return true;
        }

        // Previously we were not checking inMethod, which meant if a service
        // had multiple
        // subscriptions to the same topic (one to many mapping), the first in
        // the list would be removed
        // instead of the requested one.
        if (listener.callbackMethod.equals(inMethod)
            && CodecUtils.checkServiceNameEquality(listener.callbackName, fullName)) {
          log.info("removeListener requested {}.{} to be removed", fullName, outMethod);
          return true;
        }
        return false;
      });
    } else {
      log.info("removeListener requested {}.{} to be removed - but does not exist", fullName, outMethod);
    }
  }

  // ---------------- logging end ---------------------------

  @Override
  public boolean requiresSecurity() {
    return authProvider != null;
  }

  @Override
  final public void run() {
    isRunning = true;

    try {
      while (isRunning) {
        // TODO should this declaration be outside the while loop? if
        // so, make sure to release prior to continue
        Message m = getMsg();

        if (!preRoutingHook(m)) {
          continue;
        }

        // nameless Runtime messages
        if (m.getName() == null) {
          // don't know if this is "correct"
          // but we are substituting the Runtime name as soon as we
          // see that its a null
          // name message
          m.setName(Runtime.getInstance().getFullName());
        }

        // route if necessary
        if (!m.getName().equals(this.getName())) // && RELAY
        {
          outbox.add(m); // RELAYING
          continue; // sweet - that was a long time coming fix !
        }

        if (!preProcessHook(m)) {
          // if preProcessHook returns false
          // the message does not need to continue
          // processing
          continue;
        }

        Object ret = invoke(m);

      }
    } catch (InterruptedException edown) {
      info("shutting down");
    } catch (Exception e) {
      error(e);
    }
  }

  /**
   * method of serializing default will be simple xml to name file
   */
  @Override
  public boolean save() {
    Runtime runtime = Runtime.getInstance();
    return runtime.saveService(runtime.getConfigName(), getName(), null);
  }

  /**
   * Save a service's peer's config to current config set
   * 
   * @param peerKey
   */
  public void savePeerConfig(String peerKey, ServiceConfig config) {
    try {
      Runtime runtime = Runtime.getInstance();
      String peerName = getPeerName(peerKey);
      String data = CodecUtils.toYaml(config);
      String ymlFileName = runtime.getConfigPath() + fs + CodecUtils.getShortName(peerName) + ".yml";
      FileIO.toFile(ymlFileName, data.getBytes());
      info("saved %s", ymlFileName);
    } catch (Exception e) {
      error(e);
    }
  }

  public ServiceInterface getPeer(String peerKey) {
    String actualName = getPeerName(peerKey);
    return Runtime.getService(actualName);
  }

  @Override
  public void send(String name, String method) {
    send(name, method, (Object[]) null);
  }

  public void sendToPeer(String peerName, String method) {
    send(getPeerName(peerName), method);
  }

  public Object sendToPeerBlocking(String peerName, String method) throws InterruptedException, TimeoutException {
    return sendBlocking(getPeerName(peerName), method);
  }

  public Object invokePeer(String peerName, String method) {
    return invokeOn(false, getPeer(peerName), method, (Object[]) null);
  }

  @Deprecated /* peers are dead */
  public Object invokePeer(String peerName, String method, Object... data) {
    return invokeOn(false, getPeer(peerName), method, data);
  }

  public void sendToPeer(String peerName, String method, Object... data) {
    String name = getPeerName(peerName);
    Message msg = Message.createMessage(getFullName(), name, method, data);
    send(msg);
  }

  public Object sendToPeerBlocking(String peerName, String method, Object... data)
      throws InterruptedException, TimeoutException {
    return sendBlocking(getPeerName(peerName), method, data);
  }

  @Override
  public void send(String name, String method, Object... data) {
    if (name == null) {
      log.debug("{}.send null, {} address", getName(), method);
      return;
    }
    // if you know the service is local - use same thread
    // to call directly
    ServiceInterface si = Runtime.getService(name);
    if (si != null && CodecUtils.isLocal(name)) {
      invokeOn(true, si, method, data);
      return;
    }

    // if unknown assume remote - fire and forget on outbox
    Message msg = Message.createMessage(getFullName(), name, method, data);
    // All methods which are invoked will
    // get the correct sendingMethod
    // here its hardcoded
    msg.sendingMethod = "send";
    // log.info(CodecUtils.toJson(msg));
    send(msg);
  }

  @Override
  public void send(Message msg) {
    outbox.add(msg);
  }

  public void sendAsync(String name, String method, Object... data) {
    // if unknown assume remote - fire and forget on outbox
    Message msg = Message.createMessage(getFullName(), name, method, data);
    // All methods which are invoked will
    // get the correct sendingMethod
    // here its hardcoded
    msg.sendingMethod = "send";
    // log.info(CodecUtils.toJson(msg));
    send(msg);

    outbox.add(msg);
  }

  @Override
  public Object sendBlocking(String name, Integer timeout, String method, Object... data)
      throws InterruptedException, TimeoutException {
    Message msg = Message.createMessage(getFullName(), name, method, data);
    msg.msgId = Runtime.getUniqueID();

    return sendBlocking(msg, timeout);
  }

  /**
   * In theory the only reason this should need to use synchronized wait/notify
   * is when the msg destination is in another remote process. sendBlocking
   * should either invoke directly or use a gateway's sendBlockingRemote. To use
   * a gateways sendBlockingRemote - the msg must have a remote src
   * 
   * <pre>
   * after attach:
   * stdin (remote) --&gt; gateway sendBlockingRemote --&gt; invoke
   *                &lt;--                            &lt;--
   * </pre>
   * 
   */
  @Override
  public Object sendBlocking(Message msg, Integer timeout) throws InterruptedException, TimeoutException {
    if (Runtime.getInstance().isLocal(msg)) {
      return invoke(msg);
    } else {
      return waitOn(msg.getFullName(), msg.getMethod(), timeout, msg);
    }
  }

  /**
   * This method waits on a remote topic by sending a subscription and waiting
   * for a message to come back. It is used both by sendBlocking and waitFor to
   * normalize the code - they are equivalent. The only difference between
   * sendBlocking and waitFor is sendBlocking sends an activating msg to the
   * remote topic. If timeout occurs before a return message, a TimeoutException
   * is thrown. This is important to distinguish between a timeout and a valid
   * null return.
   * 
   * @param fullName
   *                 - service name
   * @param method
   *                 - method name
   * @param timeout
   *                 - max time to wait in ms
   * @param sendMsg
   *                 - optional message to send to the remote topic
   * @return the returned object
   * @throws InterruptedException
   *                              boom
   * @throws TimeoutException
   *                              boom
   */
  protected Object waitOn(String fullName, String method, Integer timeout, Message sendMsg)
      throws InterruptedException, TimeoutException {

    String subscriber = null;
    if (sendMsg != null) {
      // InProcCli proxies - so the subscription needs to be from the sender NOT
      // from runtime !
      subscriber = sendMsg.getSrcFullName();
    } else {
      subscriber = getFullName();
    }

    // put in-process lock in map
    String callbackMethod = CodecUtils.getCallbackTopicName(method);
    String blockingKey = String.format("%s.%s", subscriber, callbackMethod);
    Object[] blockingLockContainer = null;
    if (!inbox.blockingList.containsKey(blockingKey)) {
      blockingLockContainer = new Object[1];
      inbox.blockingList.put(blockingKey, blockingLockContainer);
    } else {
      // if it already exists - other threads are already waiting for the
      // same callback ...
      blockingLockContainer = inbox.blockingList.get(blockingKey);
    }

    // send subscription
    subscribe(fullName, method, subscriber, CodecUtils.getCallbackTopicName(method));

    if (sendMsg != null) {
      // possible race condition - counting on the delay of
      // starting a thread for the program counter to reach the
      // wait before the msg is sent
      new Thread("blocking-msg") {
        @Override
        public void run() {
          Runtime.getInstance().send(sendMsg);
        }
      }.start();
    }

    synchronized (blockingLockContainer) {
      if (timeout == null) {
        blockingLockContainer.wait();
      } else {
        long startTs = System.currentTimeMillis();
        blockingLockContainer.wait(timeout);
        if (System.currentTimeMillis() - startTs >= timeout) {
          throw new TimeoutException("timeout of %d for %s.%s exceeded", timeout, fullName, method);
        }
      }
    }

    // cleanup
    unsubscribe(fullName, method, subscriber, CodecUtils.getCallbackTopicName(method));

    return blockingLockContainer[0];

  }

  // equivalent to sendBlocking without the sending a message
  @Override
  public Object waitFor(String fullName, String method, Integer timeout) throws InterruptedException, TimeoutException {
    return waitOn(fullName, method, timeout, null);
  }

  // BOXING - End --------------------------------------
  @Override
  public Object sendBlocking(String name, String method) throws InterruptedException, TimeoutException {
    return sendBlocking(name, method, (Object[]) null);
  }

  @Override
  public Object sendBlocking(String name, String method, Object... data) throws InterruptedException, TimeoutException {
    // default 1 second timeout - FIXME CONFIGURABLE
    return sendBlocking(name, 1000, method, data);
  }

  @Override
  public void setInstanceId(URI uri) {
    instanceId = uri;
  }

  /**
   * rarely should this be used. Gateways use it to provide x-route natting
   * services by re-writing names with prefixes
   */

  @Override
  public String getName() {
    return name;
  }

  public Service setState(Service s) {
    return (Service) copyShallowFrom(this, s);
  }

  public void setThisThread(Thread thisThread) {
    this.thisThread = thisThread;
  }

  @Override
  synchronized public ServiceInterface startPeer(String peerKey) {
    if (peerKey == null) {
      log.warn("peerKey is null");
      return null;
    }

    peerKey = peerKey.trim();

    // get current definition of config and peer
    Peer peer = getConfig().getPeer(peerKey);

    if (peer == null) {
      error("startPeer could not find peerKey of %s in %s", peerKey, getName());
      return null;
    }

    // start peer requested
    broadcastState();
    return Runtime.start(peer.name);
  }

  @Override
  synchronized public void startPeers(String[] peerKeys) {

    if (peerKeys == null) {
      return;
    }

    for (String peerKey : peerKeys) {
      try {
        startPeer(peerKey);
      } catch (Exception e) {
        error(e);
      }
    }
  }

  /**
   * Release a peer by peerKey. There can be advantages to refer to a peer with
   * a peer key instead of a typed reference. This allows more modularity and
   * the ability to plug in different types of peers, even with different
   * instance names. The peerKey is an internal key the service uses to perform
   * operations on its peers. This one will release a peer.
   * 
   * @param peerKey
   */
  @Override
  synchronized public void releasePeer(String peerKey) {

    if (getConfig() != null && getConfig().getPeer(peerKey) != null) {
      ServiceConfig sc = null;
      String peerName = getPeerName(peerKey);
      ServiceInterface si = Runtime.getService(peerName);
      if (si != null) {
        sc = si.getConfig();
      }

      // peer recursive
      if (sc != null && sc.getPeers() != null) {
        for (String subPeerKey : sc.getPeers().keySet()) {
          Peer subpeer = sc.getPeer(subPeerKey);
          if (subpeer.autoStart) {
            Runtime.release(subpeer.name);
          }
        }
      }
      Runtime.release(peerName);
      broadcastState();
    } else {
      error("%s.releasePeer(%s) does not exist", getName(), peerKey);
    }
  }

  /**
   * Release a set of peers in the order they are provided.
   */
  @Override
  synchronized public void releasePeers(String[] peerKeys) {
    if (peerKeys == null) {
      return;
    }

    for (String peerKey : peerKeys) {
      try {
        releasePeer(peerKey);
      } catch (Exception e) {
        error(e);
      }

    }
  }

  @Override
  synchronized public void startService() {
    if (!isRunning()) {
      log.info("starting {}", getName());
      outbox.start();
      if (thisThread == null) {
        thisThread = new Thread(this, name);
      }
      thisThread.start();
      isRunning = true;
      send("runtime", "started", getName());

    } else {
      log.debug("startService request: service {} is already running", name);
    }
  }

  /**
   * Stops the service. Stops threads.
   */
  @Override
  synchronized public void stopService() {
    isRunning = false;
    outbox.stop();
    if (thisThread != null) {
      thisThread.interrupt();
    }
    thisThread = null;

    Runtime runtime = Runtime.getInstance();
    runtime.invoke("stopped", getFullName());
  }

  // -------------- Messaging Begins -----------------------
  @Override
  public void subscribe(NameProvider topicName, String topicMethod) {
    String callbackMethod = CodecUtils.getCallbackTopicName(topicMethod);
    subscribe(topicName.getName(), topicMethod, getFullName(), callbackMethod);
  }

  @Override
  public void subscribe(String topicName, String topicMethod) {
    String callbackMethod = CodecUtils.getCallbackTopicName(topicMethod);
    subscribe(topicName, topicMethod, getFullName(), callbackMethod);
  }

  @Override
  public void subscribe(String service, String method, String callback) {
    subscribe(service, method, getFullName(), callback);
  }

  public void subscribeTo(String service, String method) {
    subscribe(service, method, getFullName(), CodecUtils.getCallbackTopicName(method));
  }

  public void subscribeToRuntime(String method) {
    subscribe(Runtime.getInstance().getFullName(), method, getFullName(), CodecUtils.getCallbackTopicName(method));
  }

  public void unsubscribeTo(String service, String method) {
    unsubscribe(service, method, getFullName(), CodecUtils.getCallbackTopicName(method));
  }

  public void unsubscribeToRuntime(String method) {
    unsubscribe(Runtime.getInstance().getFullName(), method, getFullName(), CodecUtils.getCallbackTopicName(method));
  }

  // TODO make protected or private
  public void subscribe(String topicName, String topicMethod, String callbackName, String callbackMethod) {
    topicName = CodecUtils.getFullName(topicName);
    callbackName = CodecUtils.getFullName(callbackName);
    log.info("subscribe [{}/{} ---> {}/{}]", topicName, topicMethod, callbackName, callbackMethod);
    // TODO - do regex matching
    if (topicName.contains("*")) { // FIXME "any regex expression
      List<String> tnames = Runtime.getServiceNames(topicName);
      for (String serviceName : tnames) {
        MRLListener listener = new MRLListener(topicMethod, callbackName, callbackMethod);
        send(Message.createMessage(getFullName(), serviceName, "addListener", listener));
      }
    } else {
      if (topicMethod.contains("*")) { // FIXME "any regex expression
        Set<String> tnames = Runtime.getMethodMap(topicName).keySet();
        for (String method : tnames) {
          MRLListener listener = new MRLListener(method, callbackName, callbackMethod);
          send(Message.createMessage(getFullName(), topicName, "addListener", listener));
        }
      } else {
        MRLListener listener = new MRLListener(topicMethod, callbackName, callbackMethod);
        send(Message.createMessage(getFullName(), topicName, "addListener", listener));
      }
    }
  }

  @Override
  public void unsubscribe(NameProvider topicName, String topicMethod) {
    String callbackMethod = CodecUtils.getCallbackTopicName(topicMethod);
    unsubscribe(topicName.getName(), topicMethod, getFullName(), callbackMethod);
  }

  @Override
  public void unsubscribe(String topicName, String topicMethod) {
    String callbackMethod = CodecUtils.getCallbackTopicName(topicMethod);
    unsubscribe(topicName, topicMethod, getFullName(), callbackMethod);
  }

  @Override
  public void unsubscribe(String topicName, String topicMethod, String callback) {
    unsubscribe(topicName, topicMethod, getFullName(), callback);
  }

  // TODO make protected or private
  public void unsubscribe(String topicName, String topicMethod, String callbackName, String callbackMethod) {
    topicName = CodecUtils.getFullName(topicName);
    callbackName = CodecUtils.getFullName(callbackName);
    log.info("unsubscribe [{}/{} ---> {}/{}]", topicName, topicMethod, callbackName, callbackMethod);
    send(Message.createMessage(getFullName(), topicName, "removeListener",
        new Object[] { topicMethod, callbackName, callbackMethod }));
  }

  // -------------- Messaging Ends -----------------------
  // ---------------- Status processing begin ------------------
  @Override
  public Status error(Exception e) {
    log.error("status:", e);
    Status status = Status.error(e);
    status.name = getName();
    log.error(status.toString());
    invoke("publishStatus", status);
    return status;
  }

  @Override
  public Status error(String format, Object... args) {
    Status ret;
    ret = Status.error(String.format(Objects.requireNonNullElse(format, ""), args));
    ret.name = getName();
    log.error(ret.toString());
    lastError = ret;
    invoke("publishStatus", ret);
    return ret;
  }

  public Status error(String msg) {
    Status status = Status.error(msg);
    status.name = getName();
    log.error(status.toString());
    lastError = status;
    invoke("publishStatus", status);
    return status;
  }

  public Status warn(String msg) {
    Status status = Status.warn(msg);
    status.name = getName();
    log.warn(status.toString());
    invoke("publishStatus", status);
    return status;
  }

  @Override
  public Status warn(String format, Object... args) {
    String msg = String.format(Objects.requireNonNullElse(format, ""), args);

    return warn(msg);
  }

  /**
   * set status broadcasts an info string to any subscribers
   * 
   * @param msg
   *            m
   * @return string
   */
  public Status info(String msg) {
    return info(msg, (Object[]) null);
  }

  /**
   * set status broadcasts an formatted info string to any subscribers
   */
  @Override
  public Status info(String format, Object... args) {
    Status status = Status.info(format, args);
    status.name = getName();
    log.info(status.toString());
    invoke("publishStatus", status);
    return status;
  }

  /**
   * error only channel publishing point versus publishStatus which handles
   * info, warn &amp; error
   * 
   * @param status
   *               status
   * @return the status
   */
  public Status publishError(Status status) {
    return status;
  }

  public Status publishWarn(Status status) {
    return status;
  }

  @Override
  public Status publishStatus(Status status) {
    // demux over different channels
    if (status.isError()) {
      invoke("publishError", status);
    } else if (status.isWarn()) {
      invoke("publishWarn", status);
    }
    return status;
  }

  @Override
  public String toString() {
    return getName();
  }

  // interesting this is not just in memory
  @Override
  public Map<String, MethodEntry> getMethodMap() {
    return Runtime.getMethodMap(getName());
  }

  @Override
  public void updateStats(QueueStats stats) {
    invoke("publishStats", stats);
  }

  @Override
  public QueueStats publishStats(QueueStats stats) {
    // log.error(String.format("===stats - dequeued total %d - %d bytes in
    // %d ms %d Kbps",
    // stats.total, stats.interval, stats.ts - stats.lastTS, 8 *
    // stats.interval/ (stats.delta)));
    return stats;
  }

  @Override
  public String getDescription() {
    return serviceType.getDescription();
  }

  /**
   * Attachable.detach(serviceName) - routes to reference parameter
   * Attachable.detach(Attachable)
   * 
   * FIXME - the "string" attach/detach(string) method should be in the
   * implementation.. and this abstract should implement the
   * attach/detach(Attachable) .. because if a string was used as the base
   * implementation - it would always work when serialized (and not registered)
   * 
   */
  @Override
  public void detach(String serviceName) {
    detach(Runtime.getService(serviceName));
  }

  /**
   * Detaches ALL listeners/subscribers from this service if services have
   * special requirements, they can override this WARNING - if used this will
   * remove all UI and other perhaps necessary subscriptions
   */
  @Override
  @Deprecated /*
               * dangerous method, not to be used as lazy detach when you don't
               * know the controller name
               */
  public void detach() {
    outbox.reset();
  }

  /**
   * Attachable.attach(serviceName) - routes to reference parameter
   * Attachable.attach(Attachable)
   */
  @Override
  public void attach(String serviceName) throws Exception {
    attach(Runtime.getService(serviceName));
  }

  /**
   * is Attached - means there is a subscriber with that (full name)
   */
  @Override
  public boolean isAttached(String serviceName) {
    return getAttached().contains(CodecUtils.getFullName(serviceName));
  }

  /**
   * This detach when overriden "routes" to the appropriately typed
   * parameterized detach within a service.
   * 
   * When overriden, the first thing it should do is check to see if the
   * referenced service is already detached. If it is already detached it should
   * simply return.
   * 
   * If its detached to this service, it should first detach itself, modifying
   * its own data if necessary. The last thing it should do is call the
   * parameterized service's detach. This gives the other service an opportunity
   * to detach. e.g.
   * 
   * <pre>
   * 
   * public void detach(Attachable service) {
   *    if (ServoControl.class.isAssignableFrom(service.getClass())) {
   *        detachServoControl((ServoControl) service);
   *        return;
   *    }
   *    
   *    ...  route to more detach functions   ....
   *    
   *    error("%s doesn't know how to detach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
   *  }
   *  
   *  And within detachServoControl :
   *  
   *  public void detachServoControl(ServoControl service) {
   *       // guard
   *       if (!isAttached(service)){
   *           return;
   *       }
   *       
   *       ... detach logic ....
   * 
   *       // call to detaching service
   *       service.detach(this);  
   * }
   * </pre>
   * 
   * @param service
   *                - the service to detach from this service
   * 
   * 
   *                FIXME !!! - although this is a nice pub/sub function to clear
   *                out
   *                pubs - it will often have to be overriden and therefore will
   *                be
   *                extremely easy to forget to call super a "framework" method
   *                should
   *                replace this - so that a service.detachOutbox() calls -&gt; a
   *                detach that can be overidden !
   * 
   */
  @Override
  public void detach(Attachable service) {
    outbox.detach(service.getName());
  }

  /**
   * the "routing" isAttached - when overridden by a service this "routes" to
   * the appropriate typed isAttached
   */
  @Override
  public boolean isAttached(Attachable instance) {
    return isAttached(instance.getName());
  }

  /**
   * returns all currently attached services
   */
  @Override
  public Set<String> getAttached() {
    // return all attached
    return outbox.getAttached(null);
  }

  /**
   * returns all currently attached services to a specific publishing point
   */
  @Override
  public Set<String> getAttached(String publishPoint) {
    return outbox.getAttached(publishPoint);
  }

  /**
   * Attaches takes instance then calls the derived service attach(name) to
   * route appropriately
   */
  @Override
  public void attach(Attachable service) throws Exception {
    attach(service.getName());
  }

  @Override
  public boolean setVirtual(boolean b) {
    this.isVirtual = b;
    return isVirtual;
  }

  @Override
  public boolean isVirtual() {
    return isVirtual;
  }

  /**
   * a convenience method for a Service which always attempts to find a file
   * with the same ordered precedence
   * 
   * 1. check data/{MetaData} first (users data directory) 2. check
   * resource/{MetaData} (mrl's static resource directory) 3. check absolute
   * path
   * 
   * @param filename
   *                 - file name to get
   * @return the file to returned or null if does not exist
   */
  public File getFile(String filename) {
    File file = new File(getDataDir() + fs + filename);
    if (file.exists()) {
      log.info("found file in data directory - {}", file.getAbsolutePath());
      return file;
    }
    file = new File(getResourceDir() + fs + filename);
    if (file.exists()) {
      log.info("found file in resource directory - {}", file.getAbsolutePath());
      return file;
    }

    file = new File(filename);

    if (file.exists()) {
      log.info("found file - {}", file.getAbsolutePath());
      return file;
    }

    error("could not find file {}", file.getAbsolutePath());
    return file;
  }

  /**
   * Called by Runtime when system is shutting down a service can use this
   * method when it has to do some "ordered" cleanup.
   */
  @Override
  public void preShutdown() {
  }

  /**
   * determines if current process has internet access - moved to Service
   * recently because it may become Service specific
   * 
   * @return - true if internet is available
   */
  public static boolean hasInternet() {
    return Runtime.getPublicGateway() != null;
  }

  /**
   * true when no display is available - moved from Runtime to Service because
   * it may become Service specific
   * 
   * @return - true when no display is available
   */
  static public boolean isHeadless() {
    return java.awt.GraphicsEnvironment.isHeadless();
  }

  @Override
  public void setOrder(int creationCount) {
    this.creationOrder = creationCount;
  }

  @Deprecated
  public String getSwagger() {
    return null;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getFullName() {
    return String.format("%s@%s", name, id);
  }

  public void copyResource(String src, String dest) throws IOException {
    FileIO.copy(getResourceDir() + File.separator + src, dest);
  }

  public void setId(String id) {
    this.id = id;
  }

  /**
   * non parameter version for use within a Service
   * 
   * @return bytes of png image
   * 
   */
  public byte[] getServiceIcon() {
    return getServiceIcon(getClass().getSimpleName());
  }

  /**
   * static class version for use when class is available "preferred"
   * 
   * @param serviceType
   *                    the type of service
   * @return the bytes representing it's icon (png)
   * 
   */
  public static byte[] getServiceIcon(Class<?> serviceType) {
    return getServiceIcon(serviceType.getSimpleName());
  }

  /**
   * One place to get the ServiceIcons so that we can avoid a lot of strings
   * with "resource/Servo.png"
   * 
   * @param serviceType
   *                    name of the service type
   * @return byte array of the icon image (png)
   * 
   */
  public static byte[] getServiceIcon(String serviceType) {
    try {
      // this is bad (making a string with resource root
      // - but at least its only
      String path = getResourceRoot() + fs + serviceType + ".png";
      return Files.readAllBytes(Paths.get(path));
    } catch (Exception e) {
      log.warn("getServiceIcon threw", e);
    }
    return null;
  }

  public static String getServiceScript(Class<?> clazz) {
    return getServiceScript(clazz.getSimpleName());
  }

  public static String getServiceScript(String serviceType) {
    return getResourceAsString(serviceType, String.format("%s.py", serviceType));
  }

  public String getServiceScript() {
    return getServiceScript(getClass());
  }

  public String getResourceImage(String imageFile) {
    String path = FileIO.gluePaths(getResourceDir(), imageFile);
    return Util.getImageAsBase64(path);
  }

  /**
   * Determine if the service is operating in dev mode. isJar() is no longer
   * appropriate - as some services are modular and can be operating outside in
   * develop mode in a different repo with a "runtime" myrobotlab.jar.
   * 
   * @return true if running inside an IDE
   * 
   */
  public boolean isDev() {
    // 2 folders to check
    // src/resource/{MetaData} for services still bundled with myrobotlab.jar
    // and
    // ../{MetaData}/resource/{MetaData} for services in their own repo
    File check = new File(FileIO.gluePaths("src/resource", simpleName));
    if (check.exists()) {
      return true;
    }
    check = new File(FileIO.gluePaths(String.format("../%s/resource", simpleName), simpleName));
    if (check.exists()) {
      return true;
    }
    return false;

  }

  /**
   * localize a key - details are
   * http://myrobotlab.org/content/localization-myrobotlab-and-inmoov-languagepacks
   * 
   * @param key
   *            key to lookup in localize
   * @return localized string for key
   * 
   */
  public String localize(String key) {
    return localize(key, (Object[]) null);
  }

  /**
   * String format template processing localization
   * 
   * @param key
   *             lookup key
   * @param args
   *             var args
   * @return localized string for key
   * 
   */
  public String localize(String key, Object... args) {

    log.debug("{} current locale is {}", getName(), getLocale());
    log.debug("{} localization size {}", getName(), localization.size());

    if (key == null) {
      log.error("localize(null) not allowed");
      return null;
    }
    key = key.toUpperCase();
    Object prop = localization.get(key);

    if (prop == null) {
      prop = defaultLocalization.get(key);
    }

    if (prop == null) {
      Runtime runtime = Runtime.getInstance();
      // tried to resolve local to this service and failed
      if (this != runtime) {
        // if we are not runtime - we ask runtime
        prop = runtime.localize(key, args);
      } else {
        // if we are runtime - we try default en
        prop = runtime.localizeDefault(key);
      }
    }
    if (prop == null) {
      log.error("please help us get a good translation for {} in {}", key, Runtime.getInstance().getLocale().getTag());
      return null;
    }
    if (args == null) {
      return prop.toString();
    } else {
      return String.format(prop.toString(), args);
    }
  }

  @Override
  @Deprecated /*
               * this system should be removed in favor of a ProgramAB instance
               * with ability to translate
               */
  public void loadLocalizations() {

    if (defaultLocalization == null) {
      // default is always english :P
      defaultLocalization = Locale.loadLocalizations(FileIO.gluePaths(getResourceDir(), "localization/en.properties"));
    }

    localization = Locale
        .loadLocalizations(FileIO.gluePaths(getResourceDir(), "localization/" + locale.getLanguage() + ".properties"));
  }

  /**
   * set the current locale for this service - initial locale would have been
   * set by Runtimes locale
   * 
   */
  @Override
  public void setLocale(String code) {
    locale = new Locale(code);
    log.info("{} new locale is {}", getName(), code);
    loadLocalizations();
    broadcastState();
  }

  /**
   * @return get country tag of current locale
   */
  public String getCountry() {
    return locale.getCountry();
  }

  /**
   * Java does regions string codes differently than other systems en_US vs
   * en-US ... seems like there has been a lot of confusion on which delimiter
   * to use This function is used to simplify all of that - since we are
   * primarily interested in language and do not usually need the distinction
   * between regions in this context
   * 
   * @return the language from the locale
   * 
   */
  public String getLanguage() {
    return locale.getLanguage();
  }

  /**
   * @return the current locale
   */
  public Locale getLocale() {
    return locale;
  }

  /**
   * @return get country name of current locale
   */
  public String getDisplayLanguage() {
    return locale.getDisplayLanguage();
  }

  /**
   * @return get current locale tag - this is of the form en-BR en-US including
   *         region
   */
  public String getLocaleTag() {
    return locale.getTag();
  }

  @Override
  public boolean hasInterface(String interfaze) {
    // probably a bad idea - but nice for lazy people
    if (!interfaze.contains(".")) {
      interfaze = String.format("org.myrobotlab.service.interfaces.%s", interfaze);
    }
    return interfaceSet.containsKey(interfaze);
  }

  @Override
  public boolean hasInterface(Class<?> interfaze) {
    return hasInterface(interfaze.getCanonicalName());
  }

  public Set<String> getInterfaceNames() {
    Set<String> fn = new TreeSet<>();
    Class<?>[] faces = getClass().getInterfaces();
    for (Class<?> c : faces) {
      fn.add(c.getCanonicalName());
    }
    return fn;
  }

  // a "helper" strongly typed Java function
  @Override
  public boolean isType(Class<?> clazz) {
    return isType(clazz.getCanonicalName());
  }

  /**
   * This function does a type comparison of the service and a string passed in.
   * It's important that this does not use getClass() to resolve the type,
   * instead to support polyglot proxies - it should be using a string to
   * compare types
   */
  @Override
  public boolean isType(String clazz) {
    // probably a bad idea - but nice for lazy people
    if (!clazz.contains(".")) {
      clazz = String.format("org.myrobotlab.service.%s", clazz);
    }
    return typeKey.equals(clazz);
  }

  @Override
  public int compareTo(ServiceInterface o) {
    if (this.creationOrder == o.getCreationOrder()) {
      return 0;
    }
    if (this.creationOrder < o.getCreationOrder()) {
      return -1;
    }
    return 1;
  }

  @Override
  public int getCreationOrder() {
    return creationOrder;
  }

  /**
   * Return the service name of a peer from its peerKey
   * 
   * @param peerKey
   * @return - name of peer service
   */
  public String getPeerName(String peerKey) {

    if (getConfig() == null) {
      return null;
    }
    return getConfig().getPeerName(peerKey);
  }

  /**
   * returns if the peer is currently started from its peerkey value e.g.
   * isPeerStarted("head")
   * 
   * @param peerKey
   * @return
   */
  public boolean isPeerStarted(String peerKey) {
    return Runtime.isStarted(getPeerName(peerKey));
  }

  protected void registerForInterfaceChange(Class<?> clazz) {
    Runtime.getInstance().registerForInterfaceChange(getClass().getCanonicalName(), clazz);
  }

  final public Plan getDefault() {
    return ServiceConfig.getDefault(new Plan("runtime"), getName(), this.getClass().getSimpleName());
  }

  @Override
  public MetaData getMetaData() {
    return serviceType;
  }

  /**
   * apply the current config path config file for this service directly
   */
  public void apply() {
    Runtime runtime = Runtime.getInstance();
    String configName = runtime.getConfigName();
    ServiceConfig sc = runtime.readServiceConfig(configName, name);

    if (sc == null) {
      error("config file %s not found", Runtime.getConfigRoot() + fs + configName + fs + name + ".yml");
      return;
    }

    // applying config to self
    apply((T) sc);
  }

  public void applyPeerConfig(String peerKey, ServiceConfig config) {
    applyPeerConfig(peerKey, config, new StaticType<>() {
    });
  }

  /**
   * Apply the config to a peer, regardless if the peer is currently running or
   * not
   * 
   * @param peerKey
   * @param config
   */
  public <P extends ServiceConfig> void applyPeerConfig(String peerKey, P config,
      StaticType<Service<P>> configServiceType) {
    String peerName = getPeerName(peerKey);

    // meh - templating is not very helpful here
    ConfigurableService<P> si = Runtime.getService(peerName, configServiceType);
    if (si != null) {
      si.apply(config);
    }
  }

  /**
   * Set a peer's name to a new service name. e.g. i01.setPeerName("mouth",
   * "mouth") will change the InMoov2 peer "mouth" to be simply "mouth" instead
   * of "i01.mouth"
   * 
   * @param key
   * @param fullName
   */
  public void setPeerName(String key, String fullName) {
    Peer peer = getConfig().getPeer(key);
    String oldName = peer.name;
    peer.name = fullName;
    // update plan ?
    ServiceConfig.getDefault(new Plan("runtime"), peer.name, peer.type);
    // FIXME - determine if only updating the Plan in memory is enough,
    // should we also make or update a config file - if the config path is set?
    info("updated %s name to %s", oldName, peer.name);
  }

  /**
   * get all the subscriptions to this service
   */
  public Map<String, List<MRLListener>> getNotifyList() {
    return getOutbox().getNotifyList();
  }

  /**
   * Update a peer's type. First its done in the current Plan, and it will also
   * modify the config file if a configpath is set.
   * 
   * @param key
   *                 - peerKey of the service .. e.g. "head" for InMoov's head
   *                 peer
   * @param peerType
   *                 - desired shortname of the type
   */
  public void updatePeerType(String key, String peerType) {

    Peer peer = getConfig().getPeer(key);
    peer.type = peerType;

    ServiceConfig.getDefault(new Plan("runtime"), peer.name, peerType);
    Runtime runtime = Runtime.getInstance();
    String configName = runtime.getConfigName();
    // Seems a bit invasive - but yml file overrides everything
    // if one exists we need to replace it with the new peer type
    if (configName != null) {
      String configFile = configName + fs + peer.name + ".yml";
      File staleFile = new File(configFile);
      if (staleFile.exists()) {
        log.info("removing old config file {}", configFile);
        staleFile.delete();
        // save new default in its place
        runtime.saveDefault(configName, peer.name, peer.type, false);
      }
    }
    info("updated %s to type %s", peer.name, peerType);
  }

}
