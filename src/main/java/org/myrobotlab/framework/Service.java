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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.Broadcaster;
import org.myrobotlab.framework.interfaces.Invoker;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.image.Util;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.InMoov2Config;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.interfaces.AuthorizationProvider;
import org.myrobotlab.service.interfaces.QueueReporter;
import org.myrobotlab.service.meta.abstracts.MetaData;
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
public abstract class Service implements Runnable, Serializable, ServiceInterface, Invoker, Broadcaster, QueueReporter {

  // FIXME upgrade to ScheduledExecutorService
  // http://howtodoinjava.com/2015/03/25/task-scheduling-with-executors-scheduledthreadpoolexecutor-example/

  /**
   * contains all the meta data about the service - pulled from the static
   * method getMetaData() each instance will call the method and populate the
   * data for an instance
   * 
   */
  protected MetaData serviceType;

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
  protected String serviceClass;

  private boolean isRunning = false;

  transient protected Thread thisThread = null;

  transient protected Inbox inbox = null;
  
  transient protected Outbox outbox = null;

  protected String serviceVersion = null;

  /**
   * default en.properties - if there is one
   */
  protected Properties defaultLocalization = null;
  
  /**
   * the last config applied to this service
   */
  protected ServiceConfig config; 

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
   * a definition. However, since gson will not process statics - we are making
   * it a member variable
   */
  protected Map<String, String> interfaceSet;
  
  /**
   * plan which was used to build this service
   */
  protected Plan buildPlanx = null;

  /**
   * order which this service was created
   */
  int creationOrder = 0;

  // FIXME SecurityProvider
  protected AuthorizationProvider authProvider = null;

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
   *          t
   * @param source
   *          s
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
    while (targetClass.getCanonicalName().startsWith("org.myrobotlab") && !targetClass.getCanonicalName().startsWith("org.myrobotlab.framework")) {
      ancestry.add(targetClass);
      targetClass = targetClass.getSuperclass();
    }

    for (Class<?> sourceClass : ancestry) {

      Field fields[] = sourceClass.getDeclaredFields();
      for (int j = 0, m = fields.length; j < m; j++) {
        try {
          Field f = fields[j];

          int modifiers = f.getModifiers();

          // if (Modifier.isPublic(mod)
          // !(Modifier.isPublic(f.getModifiers())
          // Hmmm JSON mappers do hacks to get by
          // IllegalAccessExceptions.... Hmmmmm

          // GROG - recent change from this
          // if ((!Modifier.isPublic(modifiers)
          // to this
          String fname = f.getName();
          /*
           * if (fname.equals("desktops") || fname.equals("useLocalResources")
           * ){ log.info("here"); }
           */

          if (Modifier.isPrivate(modifiers) || fname.equals("log") || Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
            log.debug("skipping {}", f.getName());
            continue;
          } else {
            log.debug("copying {}", f.getName());
          }
          Type t = f.getType();

          // log.info(String.format("setting %s", f.getName()));
          /*
           * if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
           * continue; }
           */

          // GroG - this is new 1/26/2017 - needed to get webgui data to
          // load
          f.setAccessible(true);
          Field targetField = sourceClass.getDeclaredField(f.getName());
          targetField.setAccessible(true);

          if (t.equals(java.lang.Boolean.TYPE)) {
            targetField.setBoolean(target, f.getBoolean(source));
          } else if (t.equals(java.lang.Character.TYPE)) {
            targetField.setChar(target, f.getChar(source));
          } else if (t.equals(java.lang.Byte.TYPE)) {
            targetField.setByte(target, f.getByte(source));
          } else if (t.equals(java.lang.Short.TYPE)) {
            targetField.setShort(target, f.getShort(source));
          } else if (t.equals(java.lang.Integer.TYPE)) {
            targetField.setInt(target, f.getInt(source));
          } else if (t.equals(java.lang.Long.TYPE)) {
            targetField.setLong(target, f.getLong(source));
          } else if (t.equals(java.lang.Float.TYPE)) {
            targetField.setFloat(target, f.getFloat(source));
          } else if (t.equals(java.lang.Double.TYPE)) {
            targetField.setDouble(target, f.getDouble(source));
          } else {
            // log.debug(String.format("setting reference to remote
            // object %s", f.getName()));
            targetField.set(target, f.get(source));
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
   *          the time in milliseconds
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

  public final static String stackToString(final Throwable e) {
    StringWriter sw;
    try {
      sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
    } catch (Exception e2) {
      return "bad stackToString";
    }
    return "------\r\n" + sw.toString() + "------\r\n";
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
      f.mkdirs();
    }
    return Runtime.DATA_DIR + fs + typeName;
  }

  public String getDataDir() {
    return getDataDir(getClass().getSimpleName());
  }

  public String getDataInstanceDir() {
    String dataDir = Runtime.DATA_DIR + fs + getClass().getSimpleName() + fs + getName();
    File f = new File(dataDir);
    if (!f.exists()) {
      f.mkdirs();
    }
    return Runtime.DATA_DIR + fs + getClass().getSimpleName() + fs + getName();
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
   *          the class name
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
   *          the type of service
   * @param additionalPath
   *          to glue together
   * @return the full resolved path
   * 
   */
  static public String getResourceDir(String serviceType, String additionalPath) {

    // setting resource directory
    String resourceDir = "resource" + fs + serviceType;

    // overriden by src
    String override = "src" + fs + "main" + fs + "resources" + fs + "resource" + fs + serviceType;
    File test = new File(override);
    if (test.exists()) {
      log.info("found override resource dir {}", override);
      resourceDir = override;
    }

    override = ".." + fs + serviceType + fs + "resource" + fs + serviceType;
    test = new File(override);
    if (test.exists()) {
      log.info("found override repo dir {}", override);
      resourceDir = override;
    }

    if (additionalPath != null) {
      resourceDir = FileIO.gluePaths(resourceDir, additionalPath);
    }
    return resourceDir;
  }

  /**
   * non static get resource path return the path to a resource - since the root
   * can change depending if in debug or runtime - it gets the appropriate root
   * and adds the additionalPath..
   * 
   * @param additionalPath
   *          additional paths to add to the resource path
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
    // setting resource root details
    String resourceRootDir = "resource";
    // allow default to be overriden by src if it exists
    File src = new File("src");
    if (src.exists()) {
      resourceRootDir = "src" + fs + "main" + fs + "resources" + fs + "resource";
    }
    return resourceRootDir;
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
   *          - the type of service
   * @param resourceName
   *          - the path of the resource
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
   *          the class
   * @param resourceName
   *          the resource name
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
   *          the name of the resource
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
   *          the service name
   * @param inId
   *          process id
   * 
   */
  public Service(String reservedKey, String inId) {

    name = reservedKey;

    // necessary for serialized transport\
    if (inId == null) {
      id = Platform.getLocalInstance().getId();
      log.debug("creating local service for id {}", id);
    } else {
      id = inId;
      log.debug("creating remote proxy service for id {}", id);
    }

    serviceClass = this.getClass().getCanonicalName();
    simpleName = this.getClass().getSimpleName();
    MethodCache cache = MethodCache.getInstance();
    cache.cacheMethodEntries(this.getClass());

    // pull back the overrides
    serviceType = MetaData.get(getClass().getSimpleName());// ServiceData.getMetaData(name,
                                                           // getClass().getSimpleName());

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
    this.outbox = new Outbox(this);

    File versionFile = new File(getResourceDir() + fs + "version.txt");
    if (versionFile.exists()) {
      try {
        String version = FileIO.toString(versionFile);
        if (version != null) {
          version = version.trim();
          serviceVersion = version;
        }
      } catch (Exception e) {
        /* don't care */}
    }

    // register this service if local - if we are a foreign service, we probably
    // are being created in a
    // registration already
    if (id.equals(Platform.getLocalInstance().getId())) {
      Registration registration = new Registration(this);
      Runtime.register(registration);
    }
  }

  /**
   * 
   * @param additionalPath
   *          get a list of resource files in a resource path
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
   *          - listener callback info
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
    addListener(data.get("topicMethod").toString(), data.get("callbackName").toString(), data.get("callbackMethod").toString());
  }

  public void addListener(MRLListener listener) {
    addListener(listener.topicMethod, listener.callbackName, listener.callbackMethod);
  }

  public void addListener(String topicMethod, String callbackName) {
    addListener(topicMethod, callbackName, CodecUtils.getCallbackTopicName(topicMethod));
  }

  /**
   * adds a MRL message listener to this service this is the result of a
   * "subscribe" from a different service FIXME !! - implement with HashMap or
   * HashSet .. WHY ArrayList ???
   * 
   * @param topicMethod
   *          - method when called, it's return will be sent to the
   *          callbackName/calbackMethod
   * @param callbackName
   *          - name of the service to send return message to
   * @param callbackMethod
   *          - name of the method to send return data to
   */
  public void addListener(String topicMethod, String callbackName, String callbackMethod) {
    MRLListener listener = new MRLListener(topicMethod, callbackName, callbackMethod);
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
        log.debug("adding addListener from {}.{} to {}.{}", this.getName(), listener.topicMethod, listener.callbackName, listener.callbackMethod);
        nes.add(listener);
      }
    } else {
      List<MRLListener> notifyList = new CopyOnWriteArrayList<MRLListener>();
      notifyList.add(listener);
      log.debug("adding addListener from {}.{} to {}.{}", this.getName(), listener.topicMethod, listener.callbackName, listener.callbackMethod);
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
    addTask(method, 0, delayMs, method, params);
  }

  /**
   * a stronger bigger better task handler !
   * 
   * @param taskName
   *          task name
   * @param intervalMs
   *          how frequent in milliseconds
   * @param delayMs
   *          the delay
   * @param method
   *          the method
   * @param params
   *          the params to pass
   */
  @Override
  synchronized public void addTask(String taskName, long intervalMs, long delayMs, String method, Object... params) {
    if (tasks.containsKey(taskName)) {
      log.info("already have active task \"{}\"", taskName);
      return;
    }
    Timer timer = new Timer(String.format("%s.timer", String.format("%s.%s", getName(), taskName)));
    Message msg = Message.createMessage(getName(), getName(), method, params);
    Task task = new Task(this, taskName, intervalMs, msg);
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

  @Override
  final public void invokeFuture(String method, long delayMs, Object... params) {
    addTaskOneShot(delayMs, method, params);
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
  public void broadcastState() {
    invoke("publishState");
  }

  @Override
  public void broadcastStatus(Status status) {
    long now = System.currentTimeMillis();
    /*
     * if (status.equals(lastStatus) && now - lastStatusTs <
     * statusBroadcastLimitMs) { return; }
     */
    if (status.name == null) {
      status.name = getName();
    }
    if (status.level.equals(StatusLevel.ERROR)) {
      lastError = status;
      lastErrorTs = now;
      log.error(status.toString());
      invoke("publishError", status);
    } else {
      log.info(status.toString());
    }

    invoke("publishStatus", status);
    lastStatusTs = now;
    lastStatus = status;
  }

  @Override
  public String clearLastError() {
    String le = lastError.toString();
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
    Set<String> ret = new TreeSet<String>();
    Method[] methods = getMethods();
    log.debug("getMessageSet loading {} non-sub-routable methods", methods.length);
    for (int i = 0; i < methods.length; ++i) {
      ret.add(methods[i].getName());
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

  public Map<String, String> getInterfaceSet() {
    Map<String, String> ret = new TreeMap<String, String>();
    Class<?> c = getClass();
    while (c != Object.class) {

      Class<?>[] interfaces = c.getInterfaces();
      for (int i = 0; i < interfaces.length; ++i) {
        Class<?> interfaze = interfaces[i];
        // ya silly :P - but gson's default conversion of a HashSet is an
        // array
        ret.put(interfaze.getName(), interfaze.getName());
      }
      c = c.getSuperclass();
    }
    return ret;
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
      ArrayList<MRLListener> remote = null;
      try {
        remote = (ArrayList<MRLListener>) Runtime.getInstance().sendBlocking(getName(), "getNotifyList", new Object[] { key });
      } catch (Exception e) {
        log.error("remote getNotifyList threw", e);
      }

      return remote;

    } else {
      return getOutbox().notifyList.get(key);
    }
  }

  @Override
  public ArrayList<String> getNotifyListKeySet() {
    ArrayList<String> ret = new ArrayList<String>();
    if (getOutbox() == null) {
      // this is remote system - it has a null outbox, because its
      // been serialized with a transient outbox
      // and your in a skeleton
      // use the runtime to send a message

      ArrayList<String> remote = null;
      try {
        remote = (ArrayList<String>) Runtime.getInstance().sendBlocking(getName(), "getNotifyListKeySet");
      } catch (Exception e) {
        log.error("remote getNotifyList threw", e);
      }

      return remote;
    } else {
      ret.addAll(getOutbox().notifyList.keySet());
    }
    return ret;
  }

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
  public String getType() {
    return getClass().getCanonicalName();
  }

  @Override
  public boolean hasError() {
    return lastError != null;
  }

  @Override
  public boolean hasPeers() {
    try {
      Class<?> theClass = Class.forName(serviceClass);
      Method method = theClass.getMethod("getPeers", String.class);
    } catch (Exception e) {
      log.debug("{} does not have a getPeers", serviceClass);
      return false;
    }
    return true;
  }

  public String help(String format, String level) {
    StringBuffer sb = new StringBuffer();
    Method[] methods = this.getClass().getDeclaredMethods();
    TreeMap<String, Method> sorted = new TreeMap<String, Method>();

    for (int i = 0; i < methods.length; ++i) {
      Method m = methods[i];
      sorted.put(m.getName(), m);
    }
    for (String key : sorted.keySet()) {
      Method m = sorted.get(key);
      sb.append("/").append(getName()).append("/").append(m.getName());
      Class<?>[] types = m.getParameterTypes();
      if (types != null) {
        for (int j = 0; j < types.length; ++j) {
          Class<?> c = types[j];
          sb.append("/").append(c.getSimpleName());
        }
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
      return Runtime.getService(msg.getName()).invoke(msg);
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
   *          the service to invoke on
   * @param methodName
   *          the method to invoke
   * @param params
   *          var args of the params to pass
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
   *          - the object
   * @param methodName
   *          - the method to invoke on that object
   * @param params
   *          - the list of args to pass to the method
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
        error("could not find method %s.%s(%s)", obj.getClass().getSimpleName(), methodName, MethodCache.formatParams(params));
        return null; // should this be allowed to throw to a higher level ?
      }
      retobj = method.invoke(obj, params);
      if (blockLocally) {
        List<MRLListener> subList = outbox.notifyList.get(methodName);
        // correct? get local (default?) gateway
        Runtime runtime = Runtime.getInstance();
        if (subList != null) {
          for (MRLListener listener : subList) {
            Message msg = Message.createMessage(getFullName(), listener.callbackName, listener.callbackMethod, retobj);
            if (msg == null) {
              log.error("Unable to create message.. null message created");
            }
            msg.sendingMethod = methodName;
            if (runtime.isLocal(msg)) {
              ServiceInterface si = Runtime.getService(listener.callbackName);
              if (si == null) {
                log.info("{} cannot callback to listener {} does not exist for {} ", getName(), listener.callbackName, listener.callbackMethod);
              } else {
                Method m = cache.getMethod(si.getClass(), listener.callbackMethod, retobj);
                if (m == null) {

                  // attempt to get defaultInvokeMethod
                  m = cache.getDefaultInvokeMethod(si.getClass().getCanonicalName());
                  if (m != null) {
                    m.invoke(si, listener.callbackMethod, new Object[] { retobj });
                  } else {
                    log.warn("Null Method as a result of cache lookup. {} {} {}", si.getClass(), listener.callbackMethod, retobj);
                  }
                } else {
                  try {
                    m.invoke(si, retobj);
                  } catch (Throwable e) {
                    // we attempted to invoke this , it blew up. Catch it here,
                    // continue
                    // through the rest of the listeners instead of bombing out.
                    log.error("Invoke blew up! on: {} calling method {} ", si.getName(), m.toString(), e);
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
      error("could not invoke %s.%s (%s) - check logs for details", getName(), methodName, params);
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
   * Default load config method, subclasses should override this to support
   * service specific configuration in the service yaml files.
   * 
   */
  public ServiceConfig apply(ServiceConfig config) {
    log.info("Default service config loading for service: {} type: {}", getName(), getType());
    // setVirtual(config.isVirtual); "overconfigured" - user Runtimes virtual
    // setLocale(config.locale);

    // FIXME - TODO -
    // assigne a ServiceConfig config member variable the incoming config
    return config;
  }

  /**
   * Default getConfig returns name and type with null service specific config
   * 
   */
  public ServiceConfig getConfig() {
    // FIXME !!! - this should be null for services that do not have it !
//    log.info("{} of type {} does not currently define its own config", getName(), getSimpleName());
//    ServiceConfig config = new ServiceConfig();
//    config.type = getClass().getSimpleName();
    return config;
  }
  
  @Override
  public void setConfig(ServiceConfig config) {
    this.config = config;
  }

  public ServiceConfig load() throws IOException {
    ServiceConfig config = Runtime.load(getName(), getClass().getSimpleName());
    return config;
  }

  public void out(Message msg) {
    outbox.add(msg);
  }

  /**
   * Creating a message function call - without specifying the recipients -
   * static routes will be applied this is good for Motor drivers - you can swap
   * motor drivers by creating a different static route The motor is not "Aware"
   * of the driver - only that it wants to method="write" data to the driver
   */
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
   *          s
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
  public Service publishState() {
    return this;
  }


  /**
   * Releases resources, and unregisters service from the runtime
   */
  @Override
  synchronized public void releaseService() {

    purgeTasks();

    // recently added - preference over detach(Runtime.getService(getName()));
    // since this service is releasing - it should be detached from all existing
    // services
    detach();

    // note - if stopService is overwritten with extra
    // threads - releaseService will need to be overwritten too
    stopService();

    // TODO ? detach all other services currently attached
    // detach();
    // @grog is it ok for now ?

    Runtime.unregister(getName());
  }

  /**
   * 
   */
  public void removeAllListeners() {
    outbox.notifyList.clear();
  }

  public void removeListener(String topicMethod, String callbackName) {
    removeListener(topicMethod, callbackName, CodecUtils.getCallbackTopicName(topicMethod));
  }

  @Override
  public void removeListener(String outMethod, String serviceName, String inMethod) {
    if (outbox.notifyList.containsKey(outMethod)) {
      List<MRLListener> nel = outbox.notifyList.get(outMethod);
      for (int i = 0; i < nel.size(); ++i) {
        MRLListener target = nel.get(i);
        if (target.callbackName.compareTo(serviceName) == 0) {
          nel.remove(i);
          log.info("removeListener requested {}.{} to be removed", serviceName, outMethod);
        }
      }
    } else {
      log.info("removeListener requested {}.{} to be removed - but does not exist", serviceName, outMethod);
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
    return save(null, null, null);
  }

  public boolean save(String filename) {
    return save(filename, null, null);
  }

  public boolean save(String filename, Boolean allowNullFields, Boolean saveNonConfigServices) {
    try {

      if (allowNullFields == null) {
        allowNullFields = true;
      }

      if (saveNonConfigServices == null) {
        saveNonConfigServices = false;
      }

      if (filename == null) {
        filename = Runtime.getInstance().getConfigDir() + fs + Runtime.getInstance().getConfigName() + fs + getName() + ".yml";
      }

      String format = filename.substring(filename.lastIndexOf(".") + 1);
      ServiceConfig config = getConfig();

      if (config == null) {
        log.info("{} has null config - not saving", getName());
        return false;
      }

      String data = null;
      if ("json".equals(format.toLowerCase())) {
        data = CodecUtils.toJson(config);
      } else {
        data = CodecUtils.toYaml(config);
      }

      FileIO.toFile(filename, data.getBytes());

      info("saved %s config to %s", getName(), filename);
      return true;

    } catch (Exception e) {
      error(e);
    }
    return false;
  }

  @Deprecated /* peers are dead */
  public ServiceInterface getPeer(String peerKey) {
    String peerName = serviceType.getPeerActualName(peerKey);
    return Runtime.getService(peerName);
  }

  public void send(String name, String method) {
    send(name, method, (Object[]) null);
  }

  @Deprecated /* peers are dead */
  public void sendToPeer(String peerName, String method) {
    send(String.format("%s.%s", name, peerName), method, (Object[]) null);
  }

  @Deprecated /* peers are dead */
  public Object invokePeer(String peerName, String method) {
    return invokeOn(false, getPeer(peerName), method, (Object[]) null);
  }

  @Deprecated /* peers are dead */
  public Object invokePeer(String peerName, String method, Object... data) {
    return invokeOn(false, getPeer(peerName), method, data);
  }

  public void sendToPeer(String peerName, String method, Object... data) {
    send(String.format("%s.%s", name, peerName), method, data);
  }

  public void send(String name, String method, Object... data) {
    if (name == null) {
      log.debug("{}.send null, {} address", getName(), method);
      return;
    }
    // if you know the service is local - use same thread
    // to call directly
    ServiceInterface si = Runtime.getService(name);
    if (si != null) {
      invokeOn(true, si, method, data);
      return;
    }

    // if unknown assume remote - fire and forget on outbox
    Message msg = Message.createMessage(getName(), name, method, data);
    msg.sender = this.getFullName();
    // All methods which are invoked will
    // get the correct sendingMethod
    // here its hardcoded
    msg.sendingMethod = "send";
    // log.info(CodecUtils.toJson(msg));
    send(msg);
  }

  public void send(Message msg) {
    outbox.add(msg);
  }

  public void sendAsync(String name, String method, Object... data) {
    // if unknown assume remote - fire and forget on outbox
    Message msg = Message.createMessage(getName(), name, method, data);
    msg.sender = this.getFullName();
    // All methods which are invoked will
    // get the correct sendingMethod
    // here its hardcoded
    msg.sendingMethod = "send";
    // log.info(CodecUtils.toJson(msg));
    send(msg);

    outbox.add(msg);
  }

  public Object sendBlocking(String name, Integer timeout, String method, Object... data) throws InterruptedException, TimeoutException {
    Message msg = Message.createMessage(getName(), name, method, data);
    msg.sender = this.getFullName();
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
   *          - service name
   * @param method
   *          - method name
   * @param timeout
   *          - max time to wait in ms
   * @param sendMsg
   *          - optional message to send to the remote topic
   * @return the returned object
   * @throws InterruptedException
   *           boom
   * @throws TimeoutException
   *           boom
   */
  protected Object waitOn(String fullName, String method, Integer timeout, Message sendMsg) throws InterruptedException, TimeoutException {

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
  public Object waitFor(String fullName, String method, Integer timeout) throws InterruptedException, TimeoutException {
    return waitOn(fullName, method, timeout, null);
  }

  // BOXING - End --------------------------------------
  public Object sendBlocking(String name, String method) throws InterruptedException, TimeoutException {
    return sendBlocking(name, method, (Object[]) null);
  }

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

  public ServiceInterface startPeer(String reservedKey) {
   Runtime runtime = Runtime.getInstance();
   return runtime.startPeer(getName(), reservedKey);
  }
  
  public void releasePeer(String reservedKey) {    
    Runtime runtime = Runtime.getInstance();
    runtime.releasePeer(getName(), reservedKey);
  }

  @Override
  @Deprecated /* use Runtime.start() */
  public void loadAndStart() {
    try {
      load();
      startService();
    } catch (Exception e) {
      log.error("Load and Start failed.", e);
    }
  }

  @Override
  synchronized public void startService() {

    if (!isRunning()) {
      outbox.start();
      if (thisThread == null) {
        thisThread = new Thread(this, name);
      }
      thisThread.start();
      isRunning = true;
      Runtime runtime = Runtime.getInstance();
      if (runtime != null) {
        runtime.invoke("started", getName()); // getFullName()); - removed
                                              // fullname
      }

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
  public void subscribe(NameProvider topicName, String topicMethod) {
    String callbackMethod = CodecUtils.getCallbackTopicName(topicMethod);
    subscribe(topicName.getName(), topicMethod, getName(), callbackMethod);
  }

  public void subscribe(String topicName, String topicMethod) {
    String callbackMethod = CodecUtils.getCallbackTopicName(topicMethod);
    subscribe(topicName, topicMethod, getName(), callbackMethod);
  }

  public void subscribeTo(String service, String method) {
    subscribe(service, method, getName(), CodecUtils.getCallbackTopicName(method));
  }

  public void subscribeToRuntime(String method) {
    subscribe(Runtime.getInstance().getName(), method, getName(), CodecUtils.getCallbackTopicName(method));
  }

  public void unsubscribeTo(String service, String method) {
    unsubscribe(service, method, getName(), CodecUtils.getCallbackTopicName(method));
  }

  public void unsubscribeToRuntime(String method) {
    unsubscribe(Runtime.getInstance().getName(), method, getName(), CodecUtils.getCallbackTopicName(method));
  }

  public void subscribe(String topicName, String topicMethod, String callbackName, String callbackMethod) {
    log.info("subscribe [{}/{} ---> {}/{}]", topicName, topicMethod, callbackName, callbackMethod);
    // TODO - do regex matching
    if (topicName.contains("*")) { // FIXME "any regex expression
      List<String> tnames = Runtime.getServiceNames(topicName);
      for (String serviceName : tnames) {
        MRLListener listener = new MRLListener(topicMethod, callbackName, callbackMethod);
        send(Message.createMessage(getName(), serviceName, "addListener", listener));
      }
    } else {
      if (topicMethod.contains("*")) { // FIXME "any regex expression
        Set<String> tnames = Runtime.getMethodMap(topicName).keySet();
        for (String method : tnames) {
          MRLListener listener = new MRLListener(method, callbackName, callbackMethod);
          send(Message.createMessage(getName(), topicName, "addListener", listener));
        }
      } else {
        MRLListener listener = new MRLListener(topicMethod, callbackName, callbackMethod);
        send(Message.createMessage(getName(), topicName, "addListener", listener));
      }
    }
  }

  public void unsubscribe(NameProvider topicName, String topicMethod) {
    String callbackMethod = CodecUtils.getCallbackTopicName(topicMethod);
    unsubscribe(topicName.getName(), topicMethod, getName(), callbackMethod);
  }

  public void unsubscribe(String topicName, String topicMethod) {
    String callbackMethod = CodecUtils.getCallbackTopicName(topicMethod);
    unsubscribe(topicName, topicMethod, getName(), callbackMethod);
  }

  public void unsubscribe(String topicName, String topicMethod, String callbackName, String callbackMethod) {
    log.info("unsubscribe [{}/{} ---> {}/{}]", topicName, topicMethod, callbackName, callbackMethod);
    send(Message.createMessage(getName(), topicName, "removeListener", new Object[] { topicMethod, callbackName, callbackMethod }));
  }

  // -------------- Messaging Ends -----------------------
  // ---------------- Status processing begin ------------------
  public Status error(Exception e) {
    log.error("status:", e);
    Status ret = Status.error(e);
    ret.name = getName();
    log.error(ret.toString());
    invoke("publishStatus", ret);
    return ret;
  }

  @Override
  public Status error(String format, Object... args) {
    Status ret = null;
    if (format != null) {
      ret = Status.error(String.format(format, args));
    } else {
      ret = Status.error(String.format("", args));
    }
    ret.name = getName();
    log.error(ret.toString());
    invoke("publishStatus", ret);
    return ret;
  }

  public Status error(String msg) {
    return error(msg, (Object[]) null);
  }

  public Status warn(String msg) {
    return warn(msg, (Object[]) null);
  }

  @Override
  public Status warn(String format, Object... args) {
    Status status = Status.warn(format, args);
    status.name = getName();
    log.warn(status.toString());
    invoke("publishStatus", status);
    return status;
  }

  /**
   * set status broadcasts an info string to any subscribers
   * 
   * @param msg
   *          m
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
   *          status
   * @return the status
   */
  public Status publishError(Status status) {
    return status;
  }

  public Status publishStatus(Status status) {
    return status;
  }

  @Override
  public String toString() {
    return getName();
  }

  // interesting this is not just in memory
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
  public void detach(String serviceName) {
    detach(Runtime.getService(serviceName));
  }

  /**
   * Detaches ALL listeners/subscribers from this service if services have
   * special requirements, they can override this WARNING - if used this will
   * remove all UI and other perhaps necessary subscriptions
   */
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
  public void attach(String serviceName) throws Exception {
    attach(Runtime.getService(serviceName));
  }

  /**
   * is Attached - means there is a subscriber with that (full name)
   */
  public boolean isAttached(String serviceName) {
    return getAttached().contains(serviceName);
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
   *          - the service to detach from this service
   * 
   * 
   *          FIXME !!! - although this is a nice pub/sub function to clear out
   *          pubs - it will often have to be overriden and therefore will be
   *          extremely easy to forget to call super a "framework" method should
   *          replace this - so that a service.detachOutbox() calls -&gt; a
   *          detach that can be overidden !
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

  public boolean setVirtual(boolean b) {
    this.isVirtual = b;
    return isVirtual;
  }

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
   *          - file name to get
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

  public void setOrder(int creationCount) {
    this.creationOrder = creationCount;
  }

  @Deprecated
  public String getSwagger() {
    return null;
  }

  public String getId() {
    return id;
  }

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
   *          the type of service
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
   *          name of the service type
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
   *          key to lookup in localize
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
   *          lookup key
   * @param args
   *          var args
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
      } else if (this == runtime) {
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

  public void loadLocalizations() {

    if (defaultLocalization == null) {
      // default is always english :P
      defaultLocalization = Locale.loadLocalizations(FileIO.gluePaths(getResourceDir(), "localization/en.properties"));
    }

    localization = Locale.loadLocalizations(FileIO.gluePaths(getResourceDir(), "localization/" + locale.getLanguage() + ".properties"));
  }

  /**
   * set the current locale for this service - initial locale would have been
   * set by Runtimes locale
   * 
   */
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
    return serviceClass.equals(clazz);
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

  public int getCreationOrder() {
    return creationOrder;
  }

  public boolean isPeerStarted(String peerKey) {
    if (serviceType.peers != null) {
      if (!serviceType.peers.containsKey(peerKey)) {
        return false;
      }
      ServiceReservation sr = serviceType.peers.get(peerKey);
      return "started".equals(sr.state);
    }
    return false;
  }

  protected void registerForInterfaceChange(Class<?> clazz) {
    Runtime.getInstance().registerForInterfaceChange(getClass().getCanonicalName(), clazz);
  }

  final public Plan getDefault() {
    return MetaData.getDefault(getName(), this.getClass().getSimpleName(), null);
  }

  
}
