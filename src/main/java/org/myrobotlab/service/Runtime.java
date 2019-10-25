package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.myrobotlab.client.Client;
import org.myrobotlab.client.Client.Endpoint;
import org.myrobotlab.client.Client.RemoteMessageHandler;
import org.myrobotlab.client.InProcessCli;
import org.myrobotlab.codec.ApiFactory;
import org.myrobotlab.codec.ApiFactory.ApiDescription;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.HelloRequest;
import org.myrobotlab.framework.HelloResponse;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodCache;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.NameAndType;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.SystemResources;
import org.myrobotlab.framework.interfaces.MessageListener;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.lang.LangUtils;
import org.myrobotlab.logging.Appender;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.HttpRequest;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.string.StringUtil;
import org.myrobotlab.swagger.Swagger3;
import org.slf4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * FIXME - AVOID STATIC FIELDS - THE ONLY STATIC FIELD SHOULD BE THE INSTANCE
 * VAR OF RUNTIME !
 * 
 * Runtime is responsible for the creation and removal of all Services and the
 * associated static registries It maintains state information regarding
 * possible &amp; running local Services It maintains state information
 * regarding foreign Runtimes It is a singleton and should be the only service
 * of Runtime running in a process The host and registry maps are used in
 * routing communication to the appropriate service (be it local or remote) It
 * will be the first Service created It also wraps the real JVM Runtime object.
 *
 * TODO - get last args &amp; better restart (with Agent possibly?)
 *
 * RuntimeMXBean - scares me - but the stackTrace is clever RuntimeMXBean
 * runtimeMxBean = ManagementFactory.getRuntimeMXBean(); List&lt;String&gt;
 * arguments = runtimeMxBean.getInputArguments()
 *
 * final StackTraceElement[] stackTrace =
 * Thread.currentThread().getStackTrace(); final String mainClassName =
 * stackTrace[stackTrace.length - 1].getClassName();
 *
 * check for 64 bit OS and 32 bit JVM is is64bit()
 *
 */
public class Runtime extends Service implements MessageListener, RemoteMessageHandler, Gateway {
  final static private long serialVersionUID = 1L;

  // FIXME - AVOID STATIC FIELDS !!! use .getInstance() to get the singleton

  /**
   * a registry of all services regardless of which environment they came from -
   * each must have a unique name
   */
  static private final Map<String, ServiceInterface> registry = new TreeMap<>();

  /**
   * <pre>
   * The set of client connections to this mrl instance Some of the connections
   * are outbound to other webguis, others may be inbound if a webgui is
   * listening in this instance. These details and many others (such as from
   * which connection a client is from) is in the Map<String, Object> information.
   * Since different connections have different requirements, and details regarding
   * clients the only "fixed" required info to add a client is :
   * 
   * uuid - key unique identifier for the client
   * connection - name of the connection currently managing the clients connection
   * state - state of the client and/or connection
   * (lots more attributes with the Map<String, Object> to provide necessary data for the connection)
   * </pre>
   */
  static private final Map<String, Map<String, Object>> connections = new HashMap<>();

  // idToconnections ?? - FIXME - make default route !
  // id to Connection .. where id is like ip.. might need priority
  // like routeTable <String, List<RouteEntry>>
  // currently its <id, list<uuid>>
  static private final Map<String, Set<String>> routeTable = new HashMap<>();

  static private String defaultRoute = null;

  /**
   * map to hide methods we are not interested in
   */
  static private Set<String> hideMethods = new HashSet<>();

  static private boolean needsRestart = false;

  static private String runtimeName;

  /**
   * Pid file of this process
   */
  File pidFile = null;

  /**
   * user specified data which prefixes built id
   */
  // static String customId;

  final static String PID_DIR = "pids";

  static private boolean autoAcceptLicense = true; // at the moment

  /**
   * number of services created by this runtime
   */
  Integer creationCount = 0;

  /**
   * current list of cli sessions and where they are from
   */
  List<String> cliSessions = new ArrayList<String>();

  /**
   * the local repo of this machine - it should not be static as other foreign
   * repos will come in with other Runtimes from other machines.
   */
  private Repo repo = null;

  private ServiceData serviceData = ServiceData.getLocalInstance();

  /**
   * command line options
   */
  static CmdOptions options;

  /**
   * the platform (local instance) for this runtime. It must be a non-static as
   * multiple runtimes will have different platforms
   */
  Platform platform = null;

  SystemResources resources = new SystemResources();

  private static long uniqueID = new Random(System.currentTimeMillis()).nextLong();

  public final static Logger log = LoggerFactory.getLogger(Runtime.class);

  /**
   * Object used to synchronize initializing this singleton.
   */
  transient private static final Object instanceLockObject = new Object();

  /**
   * The singleton of this class.
   */
  transient private static Runtime runtime = null;

  transient private static Security security = null;

  private List<String> jvmArgs;

  private List<String> args;

  /**
   * 
   */
  String cliCwd = "/";

  String remoteId = null;

  /**
   *
   * initially I thought that is would be a good idea to dynamically load
   * Services and append their definitions to the class path. This would
   * "theoretically" be done with ivy to get/download the appropriate dependent
   * jars from the repo. Then use a custom ClassLoader to load the new service.
   *
   * Ivy works for downloading the appropriate jars &amp; artifacts However, the
   * ClassLoader became very problematic
   *
   * There is much mis-information around ClassLoaders. The most knowledgeable
   * article I have found has been this one :
   * http://blogs.oracle.com/sundararajan
   * /entry/understanding_java_class_loading
   *
   * Overall it became a huge PITA with really very little reward. The
   * consequence is all Services' dependencies and categories are defined here
   * rather than the appropriate Service class.
   *
   */

  /**
   * global startingArgs - whatever came into main each runtime will have its
   * individual copy
   */
  // FIXME - remove static !!!
  static String[] globalArgs;

  static Set<String> networkPeers = null;

  Locale locale;

  /*
   * Returns the number of processors available to the Java virtual machine.
   *
   */
  public static final int availableProcessors() {
    return java.lang.Runtime.getRuntime().availableProcessors();
  }

  /**
   * function to test if internet connectivity is available
   * 
   * @return
   */
  static public String getPublicGateway() {
    try {

      URL url = new URL("http://checkip.amazonaws.com/");
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("GET");

      int status = con.getResponseCode();
      log.info("status " + status);

      String gateway = FileIO.toString(con.getInputStream());
      return gateway;

    } catch (Exception e) {
      log.warn("internet not available");
    }
    return null;
  }

  static public synchronized ServiceInterface create(String name, String type) {
    String fullTypeName;
    if (name.contains("/")) {
      throw new IllegalArgumentException(String.format("can not have forward slash / in name %s", name));
    }

    if (name.contains("@")) {
      throw new IllegalArgumentException(String.format("can not have @ in name %s", name));
    }

    if (type.indexOf(".") == -1) {
      fullTypeName = String.format("org.myrobotlab.service.%s", type);
    } else {
      fullTypeName = type;
    }
    return createService(name, fullTypeName);
  }

  /**
   * This helper method will create, load then start a service
   * 
   * @param name
   *          - name of instance
   * @param type
   *          - type
   * @return returns the service in the form of a ServiceInterface
   */
  static public ServiceInterface loadAndStart(String name, String type) {
    ServiceInterface s = create(name, type);
    s.load();
    s.startService();
    return s;
  }

  static public ServiceInterface createAndStart(String name, String type) {
    ServiceInterface s = null;
    // framework level catch of all startServices
    // we will catch it here and log it with a stack trace
    // its not a good idea to let exceptions propegate higher - because
    // logging format can get challenging (Python trace-back) or they may
    // be completely lost - this is the last level the error can be handled
    // before
    // going into the unknown - so we catch it !
    try {
      s = create(name, type);
      s.startService();
    } catch (Exception e) {
      String error = String.format("createAndStart(%s, %s) %s", name, type, e.getClass().getCanonicalName());
      Runtime.getInstance().error(error);
      log.error(error, e);
    }
    return s;
  }

  /**
   * creates and starts service from a cmd line object
   *
   * @param services
   *          - services to be created
   */
  public final static void createAndStartServices(List<String> services) {

    if (services == null) {
      log.error("createAndStartServices(null)");
      return;
    }

    log.info("services {}", Arrays.toString(services.toArray()));

    if (services.size() % 2 == 0) {

      for (int i = 0; i < services.size(); i += 2) {
        String name = services.get(i);
        String type = services.get(i + 1);

        log.info("attempting to invoke : {} of type {}", name, type);

        ServiceInterface s = Runtime.create(name, type);

        if (s != null) {
          try {
            s.startService();
          } catch (Exception e) {
            runtime.error(e.getMessage());
            Logging.logError(e);
          }
        } else {
          runtime.error(String.format("could not create service %1$s %2$s", name, type));
        }

      }
      return;
    }
    Runtime.mainHelp();
    shutdown();
  }

  static public synchronized ServiceInterface createService(String name, String fullTypeName) {
    log.info("Runtime.createService {}", name);
    if (name == null || name.length() == 0 || fullTypeName == null || fullTypeName.length() == 0) {
      log.error("{} not a type or {} not defined ", fullTypeName, name);
      return null;
    }

    ServiceInterface sw = Runtime.getService(name);
    if (sw != null) {
      log.info("service {} already exists", name);
      return sw;
    }

    try {

      if (log.isDebugEnabled()) {
        // TODO - determine if there have been new classes added from
        // ivy --> Boot Classloader --> Ext ClassLoader --> System
        // ClassLoader
        // http://blog.jamesdbloom.com/JVMInternals.html
        log.debug("ABOUT TO LOAD CLASS");
        log.debug("loader for this class " + Runtime.class.getClassLoader().getClass().getCanonicalName());
        log.debug("parent " + Runtime.class.getClassLoader().getParent().getClass().getCanonicalName());
        log.debug("system class loader " + ClassLoader.getSystemClassLoader());
        log.debug("parent should be null" + ClassLoader.getSystemClassLoader().getParent().getClass().getCanonicalName());
        log.debug("thread context " + Thread.currentThread().getContextClassLoader().getClass().getCanonicalName());
        log.debug("thread context parent " + Thread.currentThread().getContextClassLoader().getParent().getClass().getCanonicalName());
      }

      Repo repo = Runtime.getInstance().getRepo();
      if (!repo.isServiceTypeInstalled(fullTypeName)) {
        log.error("{} is not installed", fullTypeName);
        if (autoAcceptLicense) {
          repo.install(fullTypeName);
        }
      }

      // create an instance
      Object newService = Instantiator.getThrowableNewInstance(null, fullTypeName, name);
      log.debug("returning {}", fullTypeName);
      ServiceInterface si = (ServiceInterface) newService;
      si.setVirtual(Platform.isVirtual());
      Runtime.getInstance().creationCount++;
      si.setOrder(Runtime.getInstance().creationCount);
      return (Service) newService;
    } catch (Exception e) {
      log.error("createService failed", e);
    }
    return null;
  }

  static public Map<String, Map<String, List<MRLListener>>> getNotifyEntries() {
    Map<String, Map<String, List<MRLListener>>> ret = new TreeMap<String, Map<String, List<MRLListener>>>();
    Map<String, ServiceInterface> sorted = getLocalServices();
    for (Map.Entry<String, ServiceInterface> entry : sorted.entrySet()) {
      log.info(entry.getKey() + "/" + entry.getValue());
      ArrayList<String> flks = entry.getValue().getNotifyListKeySet();
      Map<String, List<MRLListener>> subret = new TreeMap<String, List<MRLListener>>();
      for (String sn : flks) {
        List<MRLListener> mrllistners = entry.getValue().getNotifyList(sn);
        subret.put(sn, mrllistners);
      }
      ret.put(entry.getKey(), subret);
    }
    return ret;
  }

  public static String dump() {
    try {
      FileOutputStream dump = new FileOutputStream("registry.json");
      String reg = CodecUtils.toJson(registry);
      dump.write(reg.getBytes());
      dump.close();
      return reg;
    } catch (Exception e) {
      log.error("dump threw", e);
    }
    return null;
  }

  /**
   * Runs the garbage collector.
   */
  public static final void gc() {
    java.lang.Runtime.getRuntime().gc();
  }

  /**
   * although "fragile" since it relies on a external source - its useful to
   * find the external ip address of NAT'd systems
   *
   * @return external or routers ip
   * @throws Exception
   *           e
   */
  public static String getExternalIp() throws Exception {
    URL whatismyip = new URL("http://checkip.amazonaws.com");
    BufferedReader in = null;
    try {
      in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
      String ip = in.readLine();
      return ip;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /*
   * Returns the amount of free memory in the Java Virtual Machine. Calling the
   * gc method may result in increasing the value returned by freeMemory.
   */
  public static final long getFreeMemory() {
    return java.lang.Runtime.getRuntime().freeMemory();
  }

  /**
   * Get a handle to the Runtime singleton.
   *
   * @return the Runtime
   */
  public static Runtime getInstance() {
    if (runtime == null) {
      synchronized (instanceLockObject) {
        if (runtime == null) {

          // taking away capability of having a different runtime name
          runtimeName = "runtime";
          runtime = new Runtime(runtimeName);

          // setting the singleton security
          security = Security.getInstance();
          runtime.getRepo().addStatusPublisher(runtime);
          extract(); // FIXME - too overkill - do by checking version of re
        }
      }
    }
    return runtime;
  }

  static public boolean extract() {
    // FIXME - check to see if this extract only once - it should !
    // FIXME - make static function extract() and "force" it to overwrite
    // FIXME - put in command line to -extract similar to -install
    // FIXME - divide up resources so each service has its appropriate
    // dependencies
    // OR - bundle them as dependency resources into artifactory
    try {
      // Zip.extractFromSelf("resource", "resource");
      FileIO.extractResources(false);
      return true;
    } catch (Exception e) {
      log.error("extraction threw", e);
    }
    return false;
  }

  static public List<String> getJvmArgs() {
    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    return runtimeMxBean.getInputArguments();
  }

  /**
   * gets all non-loopback, active, non-virtual ip addresses
   *
   * @return list of local ipv4 IP addresses
   */
  static public List<String> getLocalAddresses() {
    log.info("getLocalAddresses");
    ArrayList<String> ret = new ArrayList<String>();

    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface current = interfaces.nextElement();
        // log.info(current);
        if (!current.isUp() || current.isLoopback() || current.isVirtual()) {
          log.info("skipping interface is down, a loopback or virtual");
          continue;
        }
        Enumeration<InetAddress> addresses = current.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress currentAddress = addresses.nextElement();

          if (!(currentAddress instanceof Inet4Address)) {
            log.info("not ipv4 skipping");
            continue;
          }

          if (currentAddress.isLoopbackAddress()) {
            log.info("skipping loopback address");
            continue;
          }
          log.info(currentAddress.getHostAddress());
          ret.add(currentAddress.getHostAddress());
        }
      }
    } catch (Exception e) {
      Logging.logError(e);
    }

    if (ret.size() == 0) {
      // if we don't have a "real" ip address - we always have home
      ret.add("127.0.0.1");
    }
    return ret;
  }

  static public void getNetInfo() {
    try {
      List<String> local = getLocalAddresses();
      String gateway = getPublicGateway();
      getNetworkPeers();
    } catch (Exception e) {
      log.error("getNetInfo threw", e);
    }

  }

  // TODO - add network to search
  static public Set<String> getNetworkPeers() throws UnknownHostException {
    networkPeers = new TreeSet<>();
    // String myip = InetAddress.getLocalHost().getHostAddress();
    List<String> myips = getLocalAddresses(); // TODO - if nothing else -
                                              // 127.0.0.1
    for (String myip : myips) {
      if (myip.equals("127.0.0.1")) {
        log.info("This PC is not connected to any network!");
      } else {
        String testIp = null;
        for (int i = myip.length() - 1; i >= 0; --i) {
          if (myip.charAt(i) == '.') {
            testIp = myip.substring(0, i + 1);
            break;
          }
        }

        log.info("My Device IP: " + myip + "\n");
        log.info("Search log:");

        for (int i = 1; i <= 254; ++i) {
          try {

            InetAddress addr = InetAddress.getByName(testIp + new Integer(i).toString());

            if (addr.isReachable(1000)) {
              log.info("Available: " + addr.getHostAddress());
              networkPeers.add(addr.getHostAddress());
            } else {
              log.info("Not available: " + addr.getHostAddress());
            }

            // TODO - check default port 8888 8887

          } catch (IOException ioex) {
          }
        }

        log.info("found {} devices", networkPeers.size());

        for (String device : networkPeers) {
          log.info(device);
        }
      }
    }
    return networkPeers;
  }

  public static List<ApiDescription> getApis() {
    return ApiFactory.getApis();
  }

  // @TargetApi(9)
  static public List<String> getLocalHardwareAddresses() {
    log.info("getLocalHardwareAddresses");
    ArrayList<String> ret = new ArrayList<String>();
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface current = interfaces.nextElement();
        byte[] mac = current.getHardwareAddress();

        if (mac == null || mac.length == 0) {
          continue;
        }

        String m = StringUtil.bytesToHex(mac);
        log.info("mac address : {}", m);

        /*
         * StringBuilder sb = new StringBuilder(); for (int i = 0; i <
         * mac.length; i++) { sb.append(String.format("%02X%s", mac[i], (i <
         * mac.length - 1) ? "-" : "")); }
         */

        ret.add(m);
        log.info("added mac");
      }
    } catch (Exception e) {
      Logging.logError(e);
    }

    log.info("done");
    return ret;
  }

  public static Map<String, ServiceInterface> getLocalServices() {
    Map<String, ServiceInterface> local = new HashMap<>();
    for (String serviceName : registry.keySet()) {
      if (!serviceName.contains("@") || serviceName.endsWith(String.format("@%s", Platform.getLocalInstance().getId()))) { // FIXME
                                                                                                                           // -
                                                                                                                           // @
                                                                                                                           // should
                                                                                                                           // be
                                                                                                                           // a
                                                                                                                           // requirement
                                                                                                                           // of
                                                                                                                           // "all"
                                                                                                                           // entries
                                                                                                                           // for
                                                                                                                           // consistency
        local.put(serviceName, registry.get(serviceName));
      }
    }
    return local;
  }

  /**
   * FIXME - return filtering/query requests
   * 
   * @return
   */
  public static Map<String, ServiceInterface> getLocalServicesForExport() {
    return registry;
  }

  /*
   * FIXME - DEPRECATE - THIS IS NOT "instance" specific info - its Class
   * definition info - Runtime should return based on ClassName
   */
  public static Map<String, MethodEntry> getMethodMap(String serviceName) {
    if (!registry.containsKey(serviceName)) {
      runtime.error(String.format("%1$s not in registry - can not return method map", serviceName));
      return null;
    }

    Map<String, MethodEntry> ret = new TreeMap<String, MethodEntry>();
    ServiceInterface sw = registry.get(serviceName);

    Class<?> c = sw.getClass();
    Method[] methods = c.getDeclaredMethods();

    Method m;
    MethodEntry me;
    String s;
    for (int i = 0; i < methods.length; ++i) {
      m = methods[i];

      if (hideMethods.contains(m.getName())) {
        continue;
      }
      me = new MethodEntry(m);
      s = me.getSignature();
      ret.put(s, me);
    }

    return ret;
  }

  // FIXME - max complexity method
  static public Map<String, Object> getSwagger(String name, String type) {
    Swagger3 swagger = new Swagger3();
    List<NameAndType> nameAndTypes = new ArrayList<>();
    nameAndTypes.add(new NameAndType(name, type));
    return swagger.getSwagger(nameAndTypes);
  }

  public static Map<String, ServiceInterface> getRegistry() {
    return registry;// FIXME should return copy
  }

  public static ServiceInterface getService(String name) {

    // FIXME - don't do this by default - an API will get the "default" service
    // if that is desired
    if (name == null || name.length() == 0) {
      return Runtime.getInstance();
    }
    if (!registry.containsKey(name)) {
      return null;
    } else {
      return registry.get(name);
    }
  }

  /**
   * return all service names in a list form
   * 
   * @return
   */
  static public String[] getServiceNames() {
    List<ServiceInterface> si = getServices();
    String[] ret = new String[si.size()];
    for (int i = 0; i < ret.length; ++i) {
      ret[i] = si.get(i).getName();
    }
    return ret;
  }

  public static boolean match(String text, String pattern) {
    return text.matches(pattern.replace("?", ".?").replace("*", ".*?"));
  }

  public static List<String> getServiceNames(String pattern) {
    List<ServiceInterface> sis = getServices();
    List<String> ret = new ArrayList<String>();
    for (ServiceInterface si : sis) {
      String serviceName = si.getName();

      if (match(serviceName, pattern)) {
        ret.add(serviceName);
      }

    }
    return ret;
  }

  public static List<String> getServiceNamesFromInterface(String interfaze) throws ClassNotFoundException {
    if (!interfaze.contains(".")) {
      interfaze = "org.myrobotlab.service.interfaces." + interfaze;
    }
    return getServiceNamesFromInterface(Class.forName(interfaze));
  }

  /*
   * param interfaceName
   * 
   * @return service names which match
   */
  public static List<String> getServiceNamesFromInterface(Class<?> interfaze) {
    ArrayList<String> ret = new ArrayList<String>();
    ArrayList<ServiceInterface> services = getServicesFromInterface(interfaze);
    for (int i = 0; i < services.size(); ++i) {
      ret.add(services.get(i).getName());
    }
    return ret;
  }

  public static List<ServiceInterface> getServices() {
    return getServices(null);
  }

  public static List<ServiceInterface> getServices(String id) {
    if (id == null) {
      return new ArrayList<ServiceInterface>(registry.values());
    }

    List<ServiceInterface> list = new ArrayList<>();
    // otherwise we are getting services of an instance
    for (String serviceName : registry.keySet()) {
      if (serviceName.indexOf("@") != -1) {
        String sid = serviceName.substring(serviceName.indexOf("@") + 1);
        if (id.equals(sid)) {
          list.add(registry.get(serviceName));
        }
      }
    }
    return list;
  }

  /*
   * @return services which match
   */
  public static synchronized ArrayList<ServiceInterface> getServicesFromInterface(Class<?> interfaze) {
    ArrayList<ServiceInterface> ret = new ArrayList<ServiceInterface>();

    Iterator<String> it = registry.keySet().iterator();
    String serviceName;
    ServiceInterface sw;
    Class<?> c;
    Class<?>[] interfaces;
    Class<?> m;
    while (it.hasNext()) {
      serviceName = it.next();
      sw = registry.get(serviceName);
      c = sw.getClass();
      interfaces = c.getInterfaces();
      for (int i = 0; i < interfaces.length; ++i) {
        m = interfaces[i];

        if (m.equals(interfaze)) {
          ret.add(sw);
        }
      }
    }

    return ret;
  }

  static public Set<Thread> getThreads() {
    return Thread.getAllStackTraces().keySet();
  }

  /*
   * dorky pass-throughs to the real JVM Runtime
   */
  public static final long getTotalMemory() {

    return java.lang.Runtime.getRuntime().totalMemory();
  }

  /**
   * unique id's are need for sendBlocking - to uniquely identify the message
   * this is a method to support that - it is unique within a process, but not
   * across processes
   *
   * @return a unique id
   */
  public static final synchronized long getUniqueID() {
    ++uniqueID;
    return uniqueID;
  }

  public static String getUptime() {
    Date now = new Date();
    Platform platform = Platform.getLocalInstance();
    String uptime = getDiffTime(now.getTime() - platform.getStartTime().getTime());
    log.info("up for {}", uptime);
    return uptime;
  }

  public static String getDiffTime(long diff) {

    long diffSeconds = diff / 1000 % 60;
    long diffMinutes = diff / (60 * 1000) % 60;
    long diffHours = diff / (60 * 60 * 1000) % 24;
    long diffDays = diff / (24 * 60 * 60 * 1000);

    StringBuffer sb = new StringBuffer();
    sb.append(diffDays).append(" days ").append(diffHours).append(" hours ").append(diffMinutes).append(" minutes ").append(diffSeconds).append(" seconds");
    return sb.toString();

  }

  /**
   * Get version returns the current version of mrl. It must be done this way,
   * because the version may be queried on the command line without the desire
   * to start a "Runtime"
   * 
   * @return
   */
  public static String getVersion() {
    return Platform.getLocalInstance().getVersion();
  }

  // FIXME - shouldn't this be in platform ???
  public static String getBranch() {
    return Platform.getLocalInstance().getBranch();
  }

  static public void install() throws ParseException, IOException {
    getInstance().getRepo().install();
  }

  /**
   * Installs a single Service type. This "should" work even if there is no
   * Runtime. It can be invoked on the command line without starting a MRL
   * instance. If a runtime exits it will broadcast events of installation
   * progress
   *
   */
  static public void install(String serviceType) throws ParseException, IOException {
    getInstance().getRepo().install(serviceType);
  }

  static public void invokeCommands(String[] invoke) {

    if (invoke.length < 2) {
      log.error("invalid invoke request, minimally 2 parameters are required: --invoke service method ...");
      return;
    }

    String name = invoke[0];
    String method = invoke[1];

    // params
    Object[] data = new Object[invoke.length - 2];
    for (int i = 2; i < invoke.length; ++i) {
      data[i - 2] = invoke[i];
    }

    log.info("attempting to invoke : {}.{}({})\n", name, method, Arrays.toString(data));
    getInstance().send(name, method, data);
  }

  public static boolean isLocal(String serviceName) {
    ServiceInterface sw = getService(serviceName);
    return sw.isLocal();
  }

  /*
   * check if class is a Runtime class
   *
   * @return true if class == Runtime.class
   */
  public static boolean isRuntime(Service newService) {
    return newService.getClass().equals(Runtime.class);
  }

  /**
   * load all configuration from all local services
   * 
   * @return true / false
   */
  static public boolean loadAll() {
    boolean ret = true;
    Map<String, ServiceInterface> local = getLocalServices();
    for (ServiceInterface si : local.values()) {
      ret &= si.load();
    }
    return ret;
  }

  /*
   * Main starting method of MyRobotLab Parses command line options
   *
   * -h help -v version -list jvm args -Dhttp.proxyHost=webproxy
   * f-Dhttp.proxyPort=80 -Dhttps.proxyHost=webproxy -Dhttps.proxyPort=80
   *
   */
  public static void main(String[] args) {

    try {
      options = new CmdOptions();

      // for Callable execution ...
      // int exitCode = new CommandLine(options).execute(args);
      new CommandLine(options).parseArgs(args);

      // fix paths
      Platform platform = Platform.getLocalInstance();
      if (options.id != null) {
        platform.setId(options.id);
      }
      options.dataDir = (platform.isWindows()) ? options.dataDir.replace("/", "\\") : options.dataDir.replace("\\", "/");
      options.libraries = (platform.isWindows()) ? options.libraries.replace("/", "\\") : options.libraries.replace("\\", "/");
      options.resourceDir = (platform.isWindows()) ? options.resourceDir.replace("/", "\\") : options.resourceDir.replace("\\", "/");

      // save an output of our cmd options
      File dataDir = new File(Runtime.getOptions().dataDir);
      if (!dataDir.exists()) {
        dataDir.mkdirs();
      }

      // if a you specify a config file it becomes the "base" of configuration
      // inline flags will still override values
      if (options.cfg != null) {
        /*
         * YOU SHOULD NOT OVERRIDE - file has highest precedence CodecJson codec
         * = new CodecJson(); CmdOptions fileOptions =
         * codec.decode(FileIO.toString(options.cfg), CmdOptions.class); new
         * CommandLine(fileOptions).parseArgs(args);
         */
        try {
          options = (CmdOptions) CodecUtils.fromJson(FileIO.toString(options.cfg), CmdOptions.class);
        } catch (Exception e) {
          log.error("config file {} was specified but could not be read", options.cfg);
        }
      }

      try {
        Files.write(Paths.get(dataDir + File.separator + "lastOptions.json"), CodecUtils.toPrettyJson(options).getBytes());
      } catch (Exception e) {
        log.error("writing lastOption.json failed", e);
      }

      globalArgs = args;
      Logging logging = LoggingFactory.getInstance();

      // TODO - replace with commons-cli -l
      logging.setLevel(options.logLevel);

      if (options.virtual) {
        Platform.setVirtual(true);
      }

      // Runtime runtime = Runtime.getInstance();

      // if (options.logToConsole) {
      // logging.addAppender(Appender.CONSOLE); // this is default of logger
      // setup :P - the parameter is not needed
      // } else {
      logging.addAppender(Appender.FILE, String.format("%s.log", runtimeName));
      // }

      if (options.help) {
        Runtime.mainHelp();
        shutdown();
        return;
      }

      // FIXME willSpawn() if not willSpawn - then shutdown with this option
      // (and others like it)
      if (options.addKeys != null) {
        if (options.addKeys.length < 2) {
          Runtime.mainHelp();
          shutdown();
        }
        Security security = Runtime.getSecurity();
        for (int i = 0; i < options.addKeys.length; i += 2) {
          security.setKey(options.addKeys[i], options.addKeys[i + 1]);
          log.info("encrypted key : {} XXXXXXXXXXXXXXXXXXXXXXX added to {}", options.addKeys[i], security.getStoreFileName());
        }
        // TODO - save all the crazy logic to the end with a single shutdown,
        // which handles all cases when it should and should not be shutdown
        if (options.services.size() == 0) {
          shutdown();
        }
      }

      // FIXME TEST THIS !! 0 length, single service, multiple !
      if (options.install != null) {
        // we start the runtime so there is a status publisher which will
        // display status updates from the repo install
        Repo repo = getInstance().getRepo();
        if (options.install.length == 0) {
          repo.install(options.libraries, (String) null);
        } else {
          for (String service : options.install) {
            repo.install(options.libraries, service);
          }
        }
        shutdown();
        return;
      }

      createAndStartServices(options.services);

      if (options.invoke != null) {
        invokeCommands(options.invoke);
      }

      if (options.interactive || !options.spawnedFromAgent) {
        log.info("====interactive mode==== -> interactive {} spawnedFromAgent {}", options.interactive, options.spawnedFromAgent);
        Runtime runtime = Runtime.getInstance();
        runtime.startInteractiveMode();
      }

    } catch (Exception e) {
      log.error("runtime exception", e);
      Runtime.mainHelp();
      shutdown();
      log.error("main threw", e);
    }
  }

  public void startInteractiveMode() {
    if (stdInClient == null) {
      stdInClient = new InProcessCli(getId(), getName(), System.in, System.out); // ????
    }
    stdInClient.start();
  }

  public void stopInteractiveMode() {
    stdInClient.stop();
  }

  /**
   * prints help to the console
   */
  static void mainHelp() {
    new CommandLine(new CmdOptions()).usage(System.out);
  }

  public static String message(String msg) {
    getInstance().invoke("publishMessage", msg);
    log.info(msg);
    return msg;
  }

  /**
   * needsRestart is set by the update process - or some othe method when a
   * restart is needed. Used by other Services to prepare for restart
   *
   * @return needsRestart
   */
  public static boolean needsRestart() {
    return needsRestart;
  }

  public void onState(ServiceInterface updatedService) {
    log.info("runtime updating registry info for remote service {}", updatedService.getName());
    registry.put(updatedService.getName(), updatedService);
  }

  /**
   * registers a service - internal to this mrl process
   * 
   * @param service
   * @return
   */
  public final static synchronized ServiceInterface register(ServiceInterface service) {
    registry.put(service.getName(), service);
    if (runtime != null) {
      runtime.invoke("registered", service);
    }
    if (!service.isLocal()) {
      runtime.subscribe(service.getName(), "publishState");
    }
    return service;
  }

  /**
   * releases a service - stops the service, its threads, releases its
   * resources, and removes registry entries
   *
   * @param name
   *          of the service to be released
   * @return whether or not it successfully released the service
   */

  // FIXME - clean up subscriptions from released
  public synchronized static boolean release(String name) {
    log.info("releasing service {}", name);
    Runtime rt = getInstance();

    if (!registry.containsKey(name)) {
      rt.info("%s already released", name);
      return false;
    }

    // get reference from registry
    ServiceInterface sw = registry.get(name);
    if (sw == null) {
      log.warn("cannot release {} - not in registry");
      return false;
    }

    // FIXME - TODO invoke and or blocking on preRelease - Future

    // send msg to service to self terminate
    if (sw.isLocal()) {
      sw.releaseService();
    } else {
      rt.send(name, "releaseService");
    }

    unregister(name);

    return true;
  }

  synchronized public static void unregister(String name) {
    log.info("unregister {}", name);
    Runtime rt = getInstance();

    // get reference from registry
    ServiceInterface sw = registry.get(name);
    if (sw == null) {
      log.info("{} already unregistered", name);
      return;
    }

    // you have to send released before removing from registry
    rt.invoke("released", sw);

    // remove from registry
    registry.remove(name);

    log.info("released {}", name);
  }

  public List<ServiceInterface> getRemoteServices() {
    return getRemoteServices(null);
  }

  public List<ServiceInterface> getRemoteServices(String id) {
    List<ServiceInterface> list = new ArrayList<>();
    for (String serviceName : registry.keySet()) {
      if (serviceName.contains("@")) {
        String sid = serviceName.substring(serviceName.indexOf("@") + 1);
        if (id == null || sid.equals(id)) {
          list.add(registry.get(serviceName));
        }
      }
    }
    return list;
  }

  /**
   * This does not EXIT(1) !!! releasing just releases all services
   *
   * FIXME FIXME FIXME - just call release on each - possibly saving runtime for
   * last .. send prepareForRelease before releasing
   *
   * release all local services
   *
   * FIXME - there "should" be an order to releasing the correct way would be to
   * save the Runtime for last and broadcast all the services being released
   *
   * FIXME - send SHUTDOWN event to all running services with a timeout period -
   * end with System.exit() FIXME normalize with releaseAllLocal and
   * releaseAllExcept
   */
  public static void releaseAll() /* local only? YES !!! LOCAL ONLY !! */
  {
    log.debug("releaseAll");

    Map<String, ServiceInterface> local = getLocalServices();

    for (String serviceName : local.keySet()) {
      ServiceInterface sw = local.get(serviceName);

      if (sw == Runtime.getInstance()) {
        // skipping runtime
        continue;
      }

      log.info("stopping service {}", serviceName);

      if (sw == null) {
        log.warn("unknown type and/or remote service");
        continue;
      }
      // runtime.invoke("released", se.serviceDirectory.get(serviceName));
      // FIXME DO THIS
      try {
        sw.stopService();
        // sw.releaseService(); // FIXED ! - releaseService will mod the
        // maps :P
        runtime.invoke("released", sw);
      } catch (Exception e) {
        runtime.error(String.format("%s threw while stopping", e));
        Logging.logError(e);
      }
    }

    runtime.stopService();
    log.info("clearing registry");
    registry.clear();
  }

  /**
   * sets task to shutdown in (n) seconds
   * 
   * @param seconds
   */
  public static void shutdown(Integer seconds) {
    log.info("shutting down in {} seconds", seconds);
    if (seconds > 0) {
      runtime.addTaskOneShot(seconds * 1000, "shutdown", (Object[]) null);
      runtime.invoke("publishShutdown", seconds);
    } else {
      shutdown();
    }
  }

  /**
   * shutdown terminates the currently running Java virtual machine by
   * initiating its shutdown sequence. This method never returns normally. The
   * argument serves as a status code; by convention, a nonzero status code
   * indicates abnormal termination
   *
   */
  public static void shutdown() {
    log.info("mrl shutdown");

    for (ServiceInterface service : getServices()) {
      service.preShutdown();
    }

    for (ServiceInterface service : getServices()) {
      service.save();
    }

    try {
      releaseAll();
    } catch (Exception e) {
      log.error("releaseAll threw - continuing to shutdown", e);
    }

    // In unusual situations, System.exit(int) might not actually stop the
    // program.
    // Runtime.getRuntime().halt(int) on the other hand, always does.
    System.exit(-1); // really returned ? or jvm bug ?
    java.lang.Runtime.getRuntime().halt(-1);
  }

  public Integer publishShutdown(Integer seconds) {
    return seconds;
  }

  // ---------------- callback events end -------------

  // ---------------- Runtime begin --------------

  public static void releaseAllServicesExcept(HashSet<String> saveMe) {
    log.info("releaseAllServicesExcept");
    List<ServiceInterface> list = Runtime.getServices();
    for (int i = 0; i < list.size(); ++i) {
      ServiceInterface si = list.get(i);
      if (saveMe != null && saveMe.contains(si.getName())) {
        log.info("leaving {}", si.getName());
        continue;
      } else {
        si.releaseService();
      }
    }

  }

  /*
   * @param name - name of Service to be removed and whos resources will be
   * released
   */
  static public void releaseService(String name) {
    Runtime.release(name);
  }

  /**
   * save all configuration from all local services
   */
  static public boolean saveAll() {
    boolean ret = true;
    Map<String, ServiceInterface> local = getLocalServices();
    for (ServiceInterface sw : local.values()) {
      ret &= sw.save();
    }
    return ret;
  }

  // FIXME - should be a map of remotes ?
  static Client clientRemote = new Client();
  static InProcessCli stdInClient = null;

  public void connect() throws IOException {
    connect("admin", "ws://localhost:8887/api/messages");
  }

  /**
   * jump to another process using the cli
   * 
   * @param id
   * @return
   */
  public String jump(String id) {
    String route = getRoute(id);
    if (route == null) {
      // log.error("cannot attach - no routing information for {}", id);
      return "cannot attach - no routing information for " + id;
    }
    stdInClient.setRemote(id);
    return id;
  }

  public String exit() {
    stdInClient.setRemote(getId());
    return getId();
  }

  @Override
  public void connect(String url) throws IOException {
    connect(url, url);
  }

  // FIXME -
  // step 1 - first bind the uuids (1 local and 1 remote)
  // step 2 - Clients will contain attribute
  public void connect(String id, String url) throws IOException {

    clientRemote.addResponseHandler(this); // FIXME - only needs to be done once
                                           // on client creation?

    UUID uuid = java.util.UUID.randomUUID();
    Endpoint endpoint = clientRemote.connect(uuid.toString(), url);
    // Message msg = Message.createMessage("runtime", "runtime",
    // "getHelloResponse", new Object[] { "runtime-client-uuid",
    // CodecUtils.toJson(new HelloRequest(getId(), uuid.toString())) });
    Message msg = getDefaultMsg(uuid.toString());
    // put as many attribs as possible in
    Map<String, Object> attributes = new HashMap<String, Object>();

    // required data
    // attributes.put("id", getId());
    attributes.put("gateway", "runtime");
    attributes.put("uuid", uuid);

    // connection specific
    attributes.put("c-type", "wasync");
    attributes.put("c-endpoint", endpoint);

    // cli specific
    attributes.put("cwd", "/");
    attributes.put("url", url);
    attributes.put("uri", url); // not really correct
    attributes.put("user", "root");
    attributes.put("host", "local");

    // addendum
    attributes.put("User-Agent", "runtime-client");
    // addConnection(id, uuid.toString(), attributes);
    Runtime.getInstance().addConnection(uuid.toString(), attributes);

    // send getHelloResponse
    clientRemote.send(uuid.toString(), CodecUtils.toJson(msg));
  }

  /**
   * FIXME - this is a gateway callback - probably should be in the gateway
   * interface - this is a "specific" gateway that supports typeless json or
   * websockets
   * 
   * callback - from clientRemote - all client connections will recieve here
   * TODO - get clients directional api - an api per direction incoming and
   * outgoing
   */
  @Override // uuid
  public void onRemoteMessage(String uuid, String data) {
    try {

      log.info("connection {} responded with {}", uuid, data);
      // get api - decode msg - process it
      Map<String, Object> connection = getConnection(uuid);
      if (connection == null) {
        error("no connection with uuid %s", uuid);
        return;
      }

      // ================= begin messages2 api =======================

      if (log.isDebugEnabled()) {
        log.debug("data - [{}]", data);
      }

      // decoding message envelope
      Message msg = CodecUtils.fromJson(data, Message.class);
      msg.setProperty("uuid", uuid);

      // if were blocking -
      Message retMsg = null;
      Object ret = null;

      if (isLocal(msg)) {

        String serviceName = msg.getName();
        // to decode fully we need class name, method name, and an array of json
        // encoded parameters
        MethodCache cache = MethodCache.getInstance();
        Class<?> clazz = Runtime.getClass(serviceName);
        if (clazz == null) {
          log.error("local msg but no Class for requested service {}", serviceName);
          return;
        }
        Object[] params = cache.getDecodedJsonParameters(clazz, msg.method, msg.data);

        Method method = cache.getMethod(clazz, msg.method, params);
        ServiceInterface si = Runtime.getService(serviceName);
        if (method == null) {
          log.error("cannot find {}", cache.makeKey(clazz, msg.method, cache.getParamTypes(params)));
          return;
        }
        if (si == null) {
          log.error("si null for serviceName {}", serviceName);
          return;
        }
        // higher level protocol - ordered steps to establish routing
        // must add meta data of connection to system
        if ("runtime".equals(serviceName) && "getHelloResponse".equals(msg.method)) {
          params[0] = uuid;
        }
        ret = method.invoke(si, params);

        // propagate return data to subscribers
        si.out(msg.method, ret);

        // sender is important - this "might" be right ;)
        String sender = String.format("%s@%s", si.getName(), getId());

        // Tri-Input broadcast
        // if it was a blocking call return a serialized message back - must
        // switch return address original sender
        // with name
        // TODO - at some point we want the option of "not trusting the sender's
        // return address"
        retMsg = Message.createMessage(sender, msg.sender, CodecUtils.getCallbackTopicName(method.getName()), ret);

      } else {
        // RELAYING !! in and out of process - don't decode !
        // FIXME GET GATEWAY
        // FIXME - send or sendBlocking

        if (msg.isBlocking()) {
          ret = sendBlockingRemote(msg, 3000);
          // FIXME !!! Implement - we probably want the "whole" msg back from
          // the inbox !
          retMsg = null;
        } else {
          send(msg);
        }
      }

      // handle the response back
      if (msg.isBlocking()) {
        retMsg.msgId = msg.msgId;
        String retUuid = Runtime.getRoute(msg.getId()); //
        Map<String, Object> retCon = Runtime.getConnection(retUuid);
        // verify I'm the appropriate gateway
        Endpoint endpoint = (Endpoint) retCon.get("c-endpoint");
        // FIXME - double encode parameters ?
        endpoint.socket.fire(CodecUtils.toJson(retMsg));
      }
    } catch (Exception e) {
      log.error("processing msg threw", e);
    } // this, apiKey, uuid, endpoint, data);
  }

  public static void setRuntimeName(String inName) {
    runtimeName = inName;
  }

  static public ServiceInterface start(String name, String type) {
    return createAndStart(name, type);
  }

  /**
   * <pre>
   * Command options for picocli library. This encapsulates all the available
   * command line flags and their details. arity attribute is for specifying in
   * an array or list the number of expected attributes after the flag. Short
   * versions of flags e.g. -i must be unique and have only a single character.
   * 
   * FIXME - make it callable so it does a callback and does some post proccessing .. i think that's why its callable ?
   * FIXME - have it capable of toString or buildCmdLine that in turn can be used as input to generate the CmdOptions again, ie.
   *         test serialization
   * </pre>
   */
  @Command(name = "java -jar myrobotlab.jar ")
  static public class CmdOptions {

    // copy constructor for people who don't like continued maintenance ;) -
    // potentially dangerous for arrays and containers
    public CmdOptions(CmdOptions other) throws IllegalArgumentException, IllegalAccessException {
      Field[] fields = this.getClass().getDeclaredFields();
      for (Field field : fields) {
        // Field field = object.getClass().getDeclaredField(fieldName);
        field.set(this, field.get(other));
      }
    }

    public CmdOptions() {
      // TODO Auto-generated constructor stub
    }

    // AGENT INFO
    @Option(names = { "-a", "--auto-update" }, description = "auto updating - this feature allows mrl instances to be automatically updated when a new version is available")
    public boolean autoUpdate = false;

    // FIXME HOW DO YOU "nullify" values !?!?!? does this need --noWebGui ???
    // FIXME - when instances connect via ws - default will become true
    // AGENT ONLY INFO
    @Option(names = { "-w",
        "--webgui" }, arity = "0..1", description = "starts webgui for the agent - this starts a server on port 127.0.0.1:8887 that accepts websockets from spawned clients. --webgui {address}:{port}")
    public String webgui = "127.0.0.1:8887";

    // FIXME - implement
    // AGENT INFO
    @Option(names = { "-u", "--update-agent" }, description = "updates agent with the latest versions of the current branch")
    public boolean updateAgent = false;

    // FIXME - does this get executed by another CommandLine ?
    // AGENT INFO
    @Option(names = { "-g",
        "--agent" }, description = "command line options for the agent must be in quotes e.g. --agent \"--service pyadmin Python --invoke pyadmin execFile myadminfile.py\"")
    public String agent;

    // AGENT INFO
    @Option(names = { "--proxy" }, description = "proxy config e.g. --proxy \"http://webproxy:8080\"")
    public String proxy;

    // FIXME -rename to daemon
    // AGENT INFO
    @Option(names = { "-f", "--fork" }, description = "forks the agent, otherwise the agent will terminate self if all processes terminate")
    public boolean fork = false;

    @Option(names = { "--interactive" }, description = "starts in interactive mode - reading from stdin")
    public boolean interactive = false;

    @Option(names = { "--spawned-from-agent" }, description = "starts in interactive mode - reading from stdin")
    public boolean spawnedFromAgent = false;

    @Option(names = { "--from-agent" }, description = "signals if the current process has been started by an Agent")
    public boolean fromAgent = false;

    @Option(names = { "-h", "-?", "--?", "--help" }, description = "shows help")
    public boolean help = false;

    @Option(names = { "-I",
        "--invoke" }, arity = "0..*", description = "invokes a method on a service --invoke {serviceName} {method} {param0} {param1} ... : --invoke python execFile myFile.py")
    public String invoke[];

    // FIXME - should work with a startup ...
    @Option(names = { "-k", "--add-key" }, arity = "2..*", description = "adds a key to the key store\n"
        + "@bold,italic java -jar myrobotlab.jar -k amazon.polly.user.key ABCDEFGHIJKLM amazon.polly.user.secret Fidj93e9d9fd88gsakjg9d93")
    public String addKeys[];

    @Option(names = { "-j", "--jvm" }, arity = "0..*", description = "jvm parameters for the instance of mrl")
    public String jvm;

    @Option(names = { "-n", "--id" }, description = "process identifier to be mdns or network overlay name for this instance - one is created at random if not assigned")
    public String id;

    @Option(names = { "-c", "--cfg", "--config" }, description = "Configuration file. If specified all configuration from the file will be used as a \"base\" of configuration. "
        + "All configuration of last run is saved to {data-dir}/lastOptions.json. This file can be used as a starter config for subsequent --cfg config.json. "
        + "If this value is set, all other configuration flags are ignored.")
    public String cfg = null;

    // FIXME - how does this work ??? if specified is it "true" ?
    @Option(names = { "-B", "--no-banner" }, description = "prevents banner from showing")
    public boolean noBanner = false;

    /**
     * <pre>
     * &#64;Option(names = { "-nc", "--no-cli" }, description = "no command line interface")
     * public boolean noCli = false;
     * </pre>
     */

    // FIXME - highlight or italics for examples !!
    @Option(names = { "-m", "--memory" }, description = "adjust memory can e.g. -m 2g \n -m 128m")
    public String memory = null;

    @Option(names = { "-l", "--log-level" }, description = "log level - helpful for troubleshooting " + " [debug info warn error]")
    public String logLevel = "info";

    @Option(names = { "-i",
        "--install" }, arity = "0..*", description = "installs all dependencies for all services, --install {ServiceType} installs dependencies for a specific service")
    public String install[];

    @Option(names = { "-V", "--virtual" }, description = "sets global environment as virtual - all services which support virtual hardware will create virtual hardware")
    public boolean virtual = false;

    // AGENT !!! FIXME - implement
    @Option(names = { "-L", "--list-versions" }, description = "list all possible versions for this branch")
    public boolean listVersions = false;

    @Option(names = { "-b", "--branch" }, description = "requested branch")
    public String branch;

    // installation root of libraries - jars will be installed under
    // {libraries}/jar natives under {libraries}/native
    // @Option(names = { "--libraries" }, description = "sets the location of
    // the libraries directory")
    // CHANGING THIS IS NOT READY FOR PRIME TIME ! - not displaying it as a
    // viable flag
    public String libraries = "libraries";

    // FIXME - get version vs force version - perhaps just always print version
    // in help
    @Option(names = { "-v", "--version" }, arity = "0..1", description = "requested version or if left blank return version")
    public String version;

    @Option(names = { "-s", "--service",
        "--services" }, arity = "0..*", description = "services requested on startup, the services must be {name} {Type} paired, e.g. gui SwingGui webgui WebGui servo Servo ...")
    public List<String> services = new ArrayList<>();

    // FIXME - implement !
    @Option(names = {
        "--client" }, arity = "0..1", description = "starts a command line interface and optionally connects to a remote instance - default with no host param connects to agent process --client [host]")
    public String client[];

    // for AGENT used to sync to the latest via source and build
    @Option(names = { "--src" }, arity = "0..1", description = "use latest source")
    public String src;

    @Option(names = { "--data-dir" }, description = "sets the location of the data directory")
    public String dataDir = "data";

    @Option(names = { "--resource-dir" }, description = "sets the location of the resource directory")
    public String resourceDir = "resource";

  }

  public Runtime(String n) {
    super(n);

    synchronized (instanceLockObject) {
      if (runtime == null) {
        runtime = this;
        // if main(argv) args did not create options we must create
        // a new one with defaults
        if (options == null) {
          options = new CmdOptions();
        }

        repo = Repo.getInstance(options.libraries, "IvyWrapper");
        if (options == null) {
          options = new CmdOptions();
        }

      }
    }

    locale = Locale.getDefault();

    if (runtime.platform == null) {
      runtime.platform = Platform.getLocalInstance();
    }

    // setting the id and the platform
    platform = Platform.getLocalInstance();

    String libararyPath = System.getProperty("java.library.path");
    String userDir = System.getProperty("user.dir");
    String userHome = System.getProperty("user.home");

    // TODO this should be a single log statement
    // http://developer.android.com/reference/java/lang/System.html

    String format = "yyyy/MM/dd HH:mm:ss";
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    SimpleDateFormat gmtf = new SimpleDateFormat(format);
    gmtf.setTimeZone(TimeZone.getTimeZone("UTC"));
    log.info("============== args begin ==============");
    StringBuffer sb = new StringBuffer();

    jvmArgs = getJvmArgs();
    args = new ArrayList<String>();
    if (globalArgs != null) {
      for (int i = 0; i < globalArgs.length; ++i) {
        sb.append(globalArgs[i]);
        args.add(globalArgs[i]);
      }
    }
    if (jvmArgs != null) {
      log.info("jvmArgs {}", Arrays.toString(jvmArgs.toArray()));
    }
    log.info("file.encoding {}", System.getProperty("file.encoding"));
    log.info("args {}", Arrays.toString(args.toArray()));

    log.info("============== args end ==============");

    log.info("============== env begin ==============");

    Map<String, String> env = System.getenv();
    if (env.containsKey("PATH")) {
      log.info("PATH={}", env.get("PATH"));
    } else {
      log.info("PATH not defined");
    }
    if (env.containsKey("JAVA_HOME")) {
      log.info("JAVA_HOME={}", env.get("JAVA_HOME"));
    } else {
      log.info("JAVA_HOME not defined");
    }

    // also look at bitness detection in framework.Platform 
    String procArch = env.get("PROCESSOR_ARCHITECTURE");
    String procArchWow64 = env.get("PROCESSOR_ARCHITEW6432");
    if (procArch != null) {
      log.info("PROCESSOR_ARCHITECTURE={}", procArch);
    } else {
      log.info("PROCESSOR_ARCHITECTURE not defined");
    }
    if (procArchWow64 != null) {
      log.info("PROCESSOR_ARCHITEW6432={}", procArchWow64);
    } else {
      log.info("PROCESSOR_ARCHITEW6432 not defined");
    }
    log.info("============== env end ==============");

    // Platform platform = Platform.getLocalInstance();
    log.info("============== normalized ==============");
    long startTime = platform.getStartTime().getTime();
    log.info("{} - GMT - {}", sdf.format(startTime), gmtf.format(startTime));
    log.info("pid {}", platform.getPid());
    log.info("hostname {}", platform.getHostname());
    log.info("ivy [runtime,{}.{}.{}]", platform.getArch(), platform.getJvmBitness(), platform.getOS());
    log.info("version {} branch {} commit {} build {}", platform.getVersion(), platform.getBranch(), platform.getCommit(), platform.getBuild());
    log.info("platform [{}}]", platform);
    log.info("version [{}]", Runtime.getVersion());
    log.info("root [{}]", FileIO.getRoot());
    log.info("cfg dir [{}]", FileIO.getCfgDir());
    log.info("sun.arch.data.model [{}]", System.getProperty("sun.arch.data.model"));

    log.info("============== non-normalized ==============");
    log.info("os.name [{}] getOS [{}]", System.getProperty("os.name"), platform.getOS());
    log.info("os.arch [{}] getArch [{}]", System.getProperty("os.arch"), platform.getArch());
    log.info("os.version [{}]", System.getProperty("os.version"));

    log.info("java.vm.name [{}]", System.getProperty("java.vm.name"));
    log.info("java.vm.vendor [{}]", System.getProperty("java.vm.vendor"));
    log.info("java.specification.version [{}]", System.getProperty("java.specification.version"));
    // test ( force encoding )
    // System.setProperty("file.encoding","UTF-8" );
    log.info("file.encoding [{}]", System.getProperty("file.encoding"));
    log.info("Charset.defaultCharset() [{}]", Charset.defaultCharset());
    log.info("user.language [{}]", System.getProperty("user.language"));
    log.info("user.country [{}]", System.getProperty("user.country"));
    log.info("user.variant [{}]", System.getProperty("user.variant"));

    // System.getProperty("pi4j.armhf")

    log.info("java.home [{}]", System.getProperty("java.home"));
    log.debug("java.class.path [{}]", System.getProperty("java.class.path"));
    log.info("java.library.path [{}]", libararyPath);
    log.info("user.dir [{}]", userDir);

    log.info("user.home [{}]", userHome);
    log.info("total mem [{}] Mb", Runtime.getTotalMemory() / 1048576);
    log.info("total free [{}] Mb", Runtime.getFreeMemory() / 1048576);
    // Access restriction - log.info("total physical mem [{}] Mb",
    // Runtime.getTotalPhysicalMemory() / 1048576);
    
    if (platform.isWindows()) {
      log.info("guessed os bitness [{}]", platform.getOsBitness());
      // try to compare os bitness with jvm bitness
      if (platform.getOsBitness() != platform.getJvmBitness()) {
        log.warn("detected possible bitness mismatch between os & jvm");
      }
    }

    log.info("getting local repo");

    repo.addStatusPublisher(this);

    hideMethods.add("main");
    hideMethods.add("loadDefaultConfiguration");
    hideMethods.add("getDescription");
    hideMethods.add("run");
    hideMethods.add("access$0");

    // TODO - good idea for future use - but must have a way to
    // purge tasks on Junit test or it gets hung in Travis
    // addTask(1000, "getSystemResources");
    // TODO - check for updates on startup ???

    // starting this
    try {
      startService();
    } catch (Exception e) {
      error("OMG Runtime won't start GAME OVER ! :( %s", e.getMessage());
      Logging.logError(e);
    }
  }

  /**
   * publishing event - since checkForUpdates may take a while
   */
  public void checkingForUpdates() {
    log.info("checking for updates");
  }

  public Locale getLocale() {
    return locale;
  }

  static public String getInputAsString(InputStream is) {
    try (java.util.Scanner s = new java.util.Scanner(is)) {
      return s.useDelimiter("\\A").hasNext() ? s.next() : "";
    }
  }

  // start cli commands ----
  /**
   * change working directory to new path
   * 
   * @param path
   */
  public void cd(String path) {
    cliCwd = path.trim();
  }

  /**
   * print working directory
   * 
   * @return
   */
  public String pwd() {
    return cliCwd;
  }

  /**
   * list the contents of the current working directory
   * 
   * @return
   */
  public Object ls() {
    return ls(cliCwd);
  }

  public String attachCli(String remoteId) {
    this.remoteId = remoteId;
    return remoteId;
  }

  /**
   * list the contents of a specific path
   * 
   * @param inPath
   * @return
   */
  public Object ls(String inPath) {
    String absPath = null;

    if (inPath.startsWith("/")) {
      absPath = inPath;
    } else {
      absPath = cliCwd + inPath;
    }

    String[] parts = absPath.split("/");

    String ret = null;
    if (absPath.equals("/")) {
      return Runtime.getServiceNames();
    } else if (parts.length == 2 && !absPath.endsWith("/")) {
      return Runtime.getService(parts[1]);
    } else if (parts.length == 2 && absPath.endsWith("/")) {
      ServiceInterface si = Runtime.getService(parts[1]);
      return si.getDeclaredMethodNames();
    }
    return ret;
  }

  /**
   * serviceName at id
   * 
   * @return
   */
  public String whoami() {
    return "runtime@" + getId();
  }

  /**
   * list connections - current connection names to this mrl runtime
   * 
   * @return
   */
  public List<String> lc() {
    return Runtime.getConnectionNames();
  }
  // end cli commands ----

  // ---------- Java Runtime wrapper functions begin --------
  /*
   * Executes the specified command and arguments in a separate process. Returns
   * the exit value for the subprocess.
   */
  static public String exec(String program) {
    return execute(program, null, null, null, null);
  }

  /*
   * publishing point of Ivy sub system - sends event failedDependency when the
   * retrieve report for a Service fails
   */
  public String failedDependency(String dep) {
    return dep;
  }

  // FIXME - you don't need that many "typed" messages - resolve,
  // resolveError, ... etc
  // just use & parse "message"

  public static Platform getPlatform() {
    return getInstance().platform;
  }

  public boolean is64bit() {
    return getInstance().platform.getJvmBitness() == 64;
  }

  public Repo getRepo() {
    return repo;
  }

  public String getId() {
    return Platform.getLocalInstance().getId();
  }

  /*
   * Returns an array of all the simple type names of all the possible services.
   * The data originates from the repo's serviceData.xml file https:/
   * /code.google.com/p/myrobotlab/source/browse/trunk/myrobotlab/thirdParty
   * /repo/serviceData.xml
   *
   * There is a local one distributed with the install zip When a "update" is
   * forced, MRL will try to download the latest copy from the repo.
   *
   * The serviceData.xml lists all service types, dependencies, categories and
   * other relevant information regarding service creation
   */
  public String[] getServiceTypeNames() {
    return getServiceTypeNames("all");
  }

  // ============== configuration end ==============

  /*
   * publishing event to get the possible services currently available
   *
   */
  public String[] getServiceTypeNames(String filter) {
    return serviceData.getServiceTypeNames(filter);
  }

  // FIXME THIS IS NOT NORMALIZED !!!
  static public Status noWorky(String userId) {
    Status status = null;
    try {
      String retStr = HttpRequest.postFile("http://myrobotlab.org/myrobotlab_log/postLogFile.php", userId, "file", new File(LoggingFactory.getLogFileName()));
      if (retStr.contains("Upload:")) {
        log.info("noWorky successfully sent - our crack team of experts will check it out !");
        status = Status.info("no worky sent");
      } else {
        status = Status.error("could not send");
      }
    } catch (Exception e) {
      log.error("the noWorky didn't worky !");
      status = Status.error(e);
    }

    // this makes the 'static' of this method pointless
    // perhaps the webgui should invoke rather than call directly .. :P
    Runtime.getInstance().invoke("publishNoWorky", status);
    return status;
  }

  /*
   * published results of sending a noWorky
   */
  static public Status publishNoWorky(Status status) {
    return status;
  }

  // -------- network begin ------------------------

  /*
   * this method is an event notifier that there were updates found
   */
  public ServiceData proposedUpdates(ServiceData si) {
    return si;
  }

  public String publishMessage(String msg) {
    return msg;
  }

  // -------- network end ------------------------

  // http://stackoverflow.com/questions/16610525/how-to-determine-if-graphicsenvironment-exists

  // FIXME - this is important in the future
  @Override
  @Deprecated /* use onResponse ??? */
  public void onMessage(Message msg) {
    // TODO: what do we do when we get a message?
    log.info("onMessage()");
  }

  // ---------------- callback events begin -------------
  /*
   * registration event
   *
   * @param sw - the name of the Service which was successfully registered
   */
  public ServiceInterface registered(ServiceInterface sw) {
    return sw;
  }

  /*
   * release event
   *
   * @param sw - the name of the Service which was successfully released
   */
  public ServiceInterface released(ServiceInterface sw) {
    return sw;
  }

  /*
   * published events
   *
   */
  public String resolveBegin(String className) {
    return className;
  }

  public void resolveEnd() {
  }

  public List<String> resolveError(List<String> errors) {
    return errors;
  }

  public String resolveSuccess(String className) {
    return className;
  }

  /**
   * FIXME - need to extend - communication to Agent ??? process request restart
   * ???
   *
   * restart occurs after applying updates - user or config data needs to be
   * examined and see if its an appropriate time to restart - if it is the
   * spawnBootstrap method will be called and bootstrap.jar will go through its
   * sequence to update myrobotlab.jar
   */
  public void restart() {
    try {
      info("restarting");

      // FIXME - send original command line ..
      // FIXME - SEND ***ID*** !!!!
      // just send a restart msg to the Agent process
      // FIXME - perhaps a "rename" is more safe .. since the file is complete
      // ...
      // FIXME - perhaps an idea worth investigating - inter-process file-system
      // queues
      // each process must have its own directory or file name type
      // id.ts.{serviceName}.json
      // 14604@ctnal0043108539.agent.1500211865673.json
      // ctnal0043108539.14604.agent.1500211865673.json
      // in this case however - the spawned process does not know the agents id
      // agent.1500211865673.json

      Message msg = Message.createMessage(getName(), "agent", "restart", platform.getId());
      FileIO.toFile(String.format("msgs/agent.%d.part", msg.msgId), CodecUtils.toJson(msg));
      File partFile = new File(String.format("msgs/agent.%d.part", msg.msgId));
      File json = new File(String.format("msgs/agent.%d.json", msg.msgId));
      partFile.renameTo(json);

      // TODO - timeout release .releaseAll nice ? - check or re-implement
      // Runtime.releaseAll();
      // Bootstrap.spawn(args.toArray(new String[args.size()]));
      // System.exit(0);

      // i've sent a message to the agent which controls me to kill me
      // and start again.. if the agent does not kill me i can only assume
      // the agent is lost, so i will commit suicide in 3 seconds
      sleep(3000);
      log.error("the agent did not kill me ! .. but I will take my cyanide pill .. goodbye ...");
      shutdown();
      // shutdown / exit
    } catch (Exception e) {
      log.error("shutdown threw", e);
    }
  }

  static public Map<String, String> getManifest() {
    return Platform.getManifest();
  }

  /**
   * Runtime's setLogLevel will set the root log level if its called from a
   * service - it will only set that Service type's log level
   * 
   * @param level
   *          - DEBUG | INFO | WARN | ERROR
   * @return the level which was set
   */
  static public String setLogLevel(String level) {
    log.info("setLogLevel {}", level);
    Logging logging = LoggingFactory.getInstance();
    logging.setLevel(level);
    log.info("setLogLevel {}", level);
    return level;
  }

  static public String getLogLevel() {
    Logging logging = LoggingFactory.getInstance();
    return logging.getLevel();
  }

  static public String setLogFile(String file) {
    log.info("setLogFile {}", file);
    Logging logging = LoggingFactory.getInstance();
    logging.removeAllAppenders();
    LoggingFactory.setLogFile(file);
    logging.addAppender(Appender.FILE);
    return file;
  }

  static public void disableLogging() {
    Logging logging = LoggingFactory.getInstance();
    logging.removeAllAppenders();
  }

  /**
   * Stops all service-related running items. This releases the singleton
   * referenced by this class, but it does not guarantee that the old service
   * will be GC'd. FYI - if stopServices does not remove INSTANCE - it is not
   * re-entrant in junit tests
   */
  @Override
  public void stopService() {
    super.stopService();
    runtime = null;
  }

  // FYI - the way to call "all" service methods !
  public void clearErrors() {
    for (String serviceName : registry.keySet()) {
      send(serviceName, "clearLastError");
    }
  }

  public static boolean hasErrors() {
    for (ServiceInterface si : registry.values()) {
      if (si.hasError()) {
        return true;
      }
    }
    return false;
  }

  /**
   * remove all subscriptions from all local Services
   */
  static public void removeAllSubscriptions() {
    for (ServiceInterface si : getLocalServices().values()) {
      ArrayList<String> nlks = si.getNotifyListKeySet();
      for (int i = 0; i < nlks.size(); ++i) {
        si.getOutbox().notifyList.clear();
      }
    }
  }

  public static ArrayList<Status> getErrors() {
    ArrayList<Status> stati = new ArrayList<Status>();
    for (ServiceInterface si : getLocalServices().values()) {
      Status status = si.getLastError();
      if (status != null && status.isError()) {
        log.info(status.toString());
        stati.add(status);
      }
    }
    return stati;
  }

  public static void broadcastStates() {
    for (ServiceInterface si : getLocalServices().values()) {
      si.broadcastState();
    }
  }

  public static Runtime get() {
    return Runtime.getInstance();
  }

  static public String execute(String... args) {
    if (args == null || args.length == 0) {
      log.error("execute invalid number of args");
      return null;
    }
    String program = args[0];
    List<String> list = null;

    if (args.length > 1) {
      list = new ArrayList<String>();
      for (int i = 1; i < args.length; ++i) {
        list.add(args[i]);
      }
    }

    return execute(program, list, null, null, null);
  }

  /*
   * FIXME - return a POJO - because there are lots of downstream which would
   * want different parts of the results FIXME - stream gobbler reconciled -
   * threaded stream consumption FIXME - watchdog - a good idea FIXME - most
   * common use case would be returning a string i would think FIXME -
   * ProcessData &amp; ProcessData2 reconciled
   *
   */
  static public String execute(String program, List<String> args, String workingDir, Map<String, String> additionalEnv, Boolean block) {

    log.info("execToString(\"{} {}\")", program, args);

    ArrayList<String> command = new ArrayList<String>();
    command.add(program);
    if (args != null) {
      for (String arg : args) {
        command.add(arg);
      }
    }

    Integer exitValue = null;

    ProcessBuilder builder = new ProcessBuilder(command);

    Map<String, String> environment = builder.environment();
    if (additionalEnv != null) {
      environment.putAll(additionalEnv);
    }
    StringBuilder outputBuilder;

    try {
      Process handle = builder.start();

      InputStream stdErr = handle.getErrorStream();
      InputStream stdOut = handle.getInputStream();

      // TODO: we likely don't need this
      // OutputStream stdIn = handle.getOutputStream();

      outputBuilder = new StringBuilder();
      byte[] buff = new byte[4096];

      // TODO: should we read both of these streams?
      // if we break out of the first loop is the process terminated?

      // read stderr
      for (int n; (n = stdErr.read(buff)) != -1;) {
        outputBuilder.append(new String(buff, 0, n));
      }
      // read stdout
      for (int n; (n = stdOut.read(buff)) != -1;) {
        outputBuilder.append(new String(buff, 0, n));
      }

      stdOut.close();
      stdErr.close();

      // TODO: stdin if we use it.
      // stdIn.close();

      // the process should be closed by now?

      handle.waitFor();

      handle.destroy();

      exitValue = handle.exitValue();
      // print the output from the command
      System.out.println(outputBuilder.toString());
      System.out.println("Exit Value : " + exitValue);
      outputBuilder.append("Exit Value : " + exitValue);

      return outputBuilder.toString();
    } catch (Exception e) {
      log.error("execute threw", e);
      exitValue = 5;
      return e.getMessage();
    }
  }

  public static Double getBatteryLevel() {
    Platform platform = Platform.getLocalInstance();
    Double r = 100.0;
    try {
      if (platform.isWindows()) {
        // String ret = Runtime.execute("cmd.exe", "/C", "WMIC.exe", "PATH",
        // "Win32_Battery", "Get", "EstimatedChargeRemaining");
        String ret = Runtime.execute("WMIC.exe", "PATH", "Win32_Battery", "Get", "EstimatedChargeRemaining");
        int pos0 = ret.indexOf("\n");
        if (pos0 != -1) {
          pos0 = pos0 + 1;
          int pos1 = ret.indexOf("\n", pos0);
          String dble = ret.substring(pos0, pos1).trim();
          try {
            r = Double.parseDouble(dble);
          } catch (Exception e) {
            log.error("no Battery detected by system");
          }

          return r;
        }

      } else if (platform.isLinux()) {
        String ret = Runtime.execute("acpitool");
        int pos0 = ret.indexOf("Charging, ");
        if (pos0 != -1) {
          pos0 = pos0 + 10;
          int pos1 = ret.indexOf("%", pos0);
          String dble = ret.substring(pos0, pos1).trim();
          try {
            r = Double.parseDouble(dble);
          } catch (Exception e) {
            log.error("no Battery detected by system");
          }
          return r;
        }
        log.info(ret);
      } else if (platform.isMac()) {
        String ret = Runtime.execute("pmset -g batt");
        int pos0 = ret.indexOf("Battery-0");
        if (pos0 != -1) {
          pos0 = pos0 + 10;
          int pos1 = ret.indexOf("%", pos0);
          String dble = ret.substring(pos0, pos1).trim();
          try {
            r = Double.parseDouble(dble);
          } catch (Exception e) {
            log.error("no Battery detected by system");
          }
          return r;
        }
        log.info(ret);
      }

    } catch (Exception e) {
      log.info("execToString threw", e);
    }
    return r;
  }

  public static void setLanguage(String language) {
    Runtime runtime = Runtime.getInstance();
    runtime.setLocale(new Locale(language));
  }

  public void setLocale(String language) {
    setLocale(new Locale(language));
  }

  public void setLocale(String language, String country) {
    setLocale(new Locale(language, country));
  }

  public void setLocale(String language, String country, String variant) {
    setLocale(new Locale(language, country, variant));
  }

  public void setLocale(Locale locale) {
    // the local field is used for display & serialization
    this.locale = locale;
    Locale.setDefault(locale);
    /*
     * I don't believe these are necessary System.setProperty("user.language",
     * language); System.setProperty("user.country", country);
     * System.setProperty("user.variant", variant);
     */
  }

  public String getLanguage() {
    return locale.getLanguage();
  }

  public String getCountry() {
    return locale.getCountry();
  }

  public Platform login(Platform platform) {
    info("runtime %s says \"hello\" %s", platform.getId(), platform);

    // from which connection ?
    // runtime "mqtt" --mqtt01--> --mqtt02--> runtime"watchdog"

    // return to its runtime a register ..

    return platform;
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   *
   * @return ServiceType - returns all the data
   *
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Runtime.class.getCanonicalName());
    meta.addDescription("is a singleton service responsible for the creation, starting, stopping, releasing and registration of all other services");
    meta.addCategory("framework");
    meta.addPeer("cli", "Cli", "command line interpreter for the runtime");

    meta.includeServiceInOneJar(true);
    // apache 2.0 license
    meta.addDependency("com.google.code.gson", "gson", "2.8.5");
    // apache 2.0 license
    meta.addDependency("org.apache.ivy", "ivy", "2.4.0-4");
    // apache 2.0 license
    meta.addDependency("org.apache.httpcomponents", "httpclient", "4.5.2");
    // apache 2.0 license
    meta.addDependency("org.atmosphere", "wasync", "2.1.5");
    // apache 2.0 license
    meta.addDependency("info.picocli", "picocli", "4.0.0-beta-2");

    // EDL (new-style BSD) licensed
    meta.addDependency("org.eclipse.jgit", "org.eclipse.jgit", "5.4.0.201906121030-r");

    // all your logging needs
    meta.addDependency("org.slf4j", "slf4j-api", "1.7.21");
    meta.addDependency("ch.qos.logback", "logback-classic", "1.0.13");

    // meta.addDependency("org.apache.maven", "maven-embedder", "3.1.1");
    // meta.addDependency("ch.qos.logback", "logback-classic", "1.2.3");

    return meta;
  }

  public ServiceData getServiceData() {
    return serviceData;
  }

  public SystemResources getSystemResources() {
    return new SystemResources();
  }

  public String getDisplayLanguage() {
    return locale.getDisplayLanguage();
  }

  /**
   * Return supported system languages
   */
  public HashMap<String, String> getLanguages() {

    Locale[] locales = Locale.getAvailableLocales();

    HashMap<String, String> languagesList = new HashMap<String, String>();
    for (int i = 0; i < locales.length; i++) {
      log.info(locales[i].toLanguageTag());
      languagesList.put(locales[i].toLanguageTag(), locales[i].getDisplayLanguage());
    }
    return languagesList;
  }

  /**
   * get the Security singleton
   * 
   * @return
   */
  static public Security getSecurity() {
    return Runtime.getInstance().security;
  }

  public String getLocaleTag() {
    return locale.toLanguageTag();
  }

  public static Process exec(String... cmd) throws IOException {
    // FIXME - can't return a process - it will explode in serialization
    // but we might want to keep it and put it on a transient map
    log.info("Runtime exec {}", Arrays.toString(cmd));
    Process p = java.lang.Runtime.getRuntime().exec(cmd);
    return p;
  }

  // FIXME - parameters String filname, String lang (python|java) - default with
  // timestamp ?
  // TODO - auto-restore (on startup)/ auto-backup
  public static void backup() {

    try {

      // registry

      // save json inline in python form
      // https://stackoverflow.com/questions/42444130/python-multi-line-json-and-variables
      // - dictionary

      // backup potentially could be very few lines of code - if the very few
      // lines of code did a very large amount of
      // data manipulation, the proportion of data to code would be high

      // 0 - replace option - tear down everything except runtime ??? ie -
      // replace system vs add

      // 1 - export all services

      // 1.5 - and their inline data

      // 2 - export all subscriptions/ notifications

      // 3 - look for exceptions - see if any services have their own special
      // jython export implementation

      // FIXME - remove messageMap and interfaceMap and depedencies - anything
      // which is static as the class definition

      // TODO - if language.equals("python")
      StringBuilder sb = new StringBuilder();

      // TODO - add header

      sb.append("import json\n");

      String[] services = getServiceNames();

      for (String name : services) {
        ServiceInterface si = Runtime.getService(name);
        String safeName = CodecUtils.getSafeReferenceName(name);
        sb.append(String.format("%s = Runtime.start(\"%s\",\"%s\")\n", name, name, si.getType()));
      }

      sb.append("\n############ loading ############\n");
      sb.append("print(\"loading ...\")\n");

      for (String name : services) {
        ServiceInterface si = Runtime.getService(name);
        if (si.getName().equals("runtime")) {
          continue;
        }
        String json = CodecUtils.toJson(si);

        // data load in "json" form vs python dictionary - it could be in json
        // dictionary
        sb.append(String.format("%sJson = \"\"\"%s\n\"\"\"\n", name, json));

        sb.append(String.format("%s.load(%s)\n", name + "Json", si.getType()));
      }

      Files.write(Paths.get("backup.py"), sb.toString().getBytes());

      // Map<String, Map<String, List<MRLListener>>> routeMap =
      // getNotifyEntries();
      /*
       * StringBuffer routes = new StringBuffer(); for (String name : services)
       * { ServiceInterface si = Runtime.getService(name);
       * si.getOutbox().notifyList; }
       */

      // TODO - notifications / subscriptions
      // iterate through all services
      // iterate through all notifications
      // load subscripts - make shorthand syntax // perhaps subscribe ??
      Files.write(Paths.get("backup-routes.py"), CodecUtils.toJson(getNotifyEntries()).getBytes());

      log.info("finished...");

    } catch (Exception e) {
      log.error("backup threw", e);
    }
  }

  public static String export(String filename, String names) throws IOException {
    String python = LangUtils.toPython(names);
    Files.write(Paths.get(filename), python.toString().getBytes());
    return python;
  }

  public static String exportAll(String filename) throws IOException {
    // currently only support python - maybe in future we'll support js too
    String python = LangUtils.toPython();
    Files.write(Paths.get(filename), python.toString().getBytes());
    return python;
  }

  public static Runtime getInstance(String[] args2) {
    Runtime.main(args2);
    return Runtime.getInstance();
  }

  public static CmdOptions getOptions() {
    return options;
  }

  // FIXME - a way to reach in a messages meta-data ?? is this a reference
  // through the thread storage?
  // FIXME - needs to be paired with its client which has already been added
  // its "meta-data"
  public HelloResponse getHelloResponse(String uuid, HelloRequest hello) {
    HelloResponse response = new HelloResponse();
    Map<String, Object> connection = getConnection(uuid);
    if (uuid == null) {
      log.error("uuid could not be found in known connections {}", uuid);
      // return null;
    }
    response.id = getId();
    // this.uuid = uuid;
    response.request = hello;
    response.platform = Platform.getLocalInstance();
    response.services = Runtime.getServiceTypes();
    connection.put("request", hello);
    // addClientAttribute(uuid, "request", request);
    updateRoute(hello.id, uuid);
    Runtime.getConnection(uuid).put("id", hello.id);
    return response;
  }

  public void onHelloResponse(HelloResponse response) {
    log.info("onHelloResponse {}", response);
  }

  static public Map<String, Set<String>> route() {
    return routeTable;
  }

  public static Map<String, String> getServiceTypes() {
    Map<String, String> ret = new HashMap<>();
    for (String name : registry.keySet()) {
      ret.put(name, registry.get(name).getClass().getCanonicalName());
    }
    return ret;
  }

  /**
   * When a new connection is formed we have "connectivity" but not msg routing
   * ability - The new connection will become the "new" default route, but we
   * don't really have any information about the details of what id it has until
   * we get a HelloResponse/HelloRequest message. When we have the new
   * information we'll move the connections to the new id and this will be
   * maintained in the routeTable
   * 
   * null key in the routeTable for id is "unknown" - ie its a connection which
   * has not sent a getHelloResponse(request) msg
   * 
   * @param id
   * @param uuid
   */
  static public void updateRoute(String id, String uuid) {

    if (id == null) {
      id = uuid;
    }

    if (routeTable.containsKey(id)) {
      routeTable.get(id).add(uuid);
    } else {
      Set<String> uuids = new HashSet<>();
      uuids.add(uuid);
      routeTable.put(id, uuids);
    }
    // is this always the case - latest always wins?
    defaultRoute = uuid;
  }

  static public void removeRoute(String uuid) {
    if (routeTable.containsKey(uuid)) {
      routeTable.remove(uuid);
    }

    Set<String> removeIds = new HashSet<String>();
    for (String id : routeTable.keySet()) {
      Set<String> routes = routeTable.get(id);
      if (routes.contains(uuid)) {
        routes.remove(uuid);
      }
      if (routes.size() == 0) {
        // adding for delayed removal
        removeIds.add(id);
      }
    }

    for (String clean : removeIds) {
      routeTable.remove(clean);
    }

    if (uuid.equals(defaultRoute)) {
      if (routeTable.size() > 0) {
        // if it was our default route - change it to next available
        defaultRoute = routeTable.keySet().iterator().next();
      } else {
        defaultRoute = null;
      }
    }
  }

  public void deleteRoute(String id) {
    routeTable.remove(id);
  }

  public void addConnection(String uuid, Map<String, Object> attributes) {
    Map<String, Object> attr = null;
    if (!connections.containsKey(uuid)) {
      attr = attributes;
      invoke("publishConnect", attributes);
    } else {
      attr = connections.get(uuid);
      attr.putAll(attributes);
    }
    connections.put(uuid, attr);

    // recently removed - no connections in route table
    // updateRoute(null, uuid);
  }

  @Override
  public Message getDefaultMsg(String connId) {
    Message msg = Message.createMessage(String.format("%s@%s", getName(), getId()), "runtime", "getHelloResponse",
        new Object[] { "fill-uuid", CodecUtils.toJson(new HelloRequest(Platform.getLocalInstance().getId(), connId)) });
    msg.setBlocking();
    return msg;
  }

  public void removeConection(String uuid) {
    if (connections.remove(uuid) != null) {
      invoke("publishDisconnect", uuid);
    }
    for (String id : routeTable.keySet()) {
      Set<String> conn = routeTable.get(id);
      if (conn != null) {
        conn.remove(uuid);
      }
    }
  }

  public String publishDisconnect(String uuid) {
    return uuid;
  }

  // FIXME - filter only serializable objects ?
  public Map<String, Object> publishConnect(Map<String, Object> attributes) {
    return attributes;
  }

  static public void removeConnection(String uuid) {
    connections.remove(uuid);
  }

  /**
   * globally get all client
   * 
   * @return
   */
  static public Map<String, Map<String, Object>> getConnections() {
    return connections;
  }

  /**
   * separated by connection - send connection name and get filter results back
   * for a specific connections connected clients
   * 
   * @param gatwayName
   * @return
   */
  static public Map<String, Map<String, Object>> getConnections(String gatwayName) {
    Map<String, Map<String, Object>> ret = new HashMap<>();
    for (String uuid : connections.keySet()) {
      Map<String, Object> c = connections.get(uuid);
      String gateway = (String) c.get("gateway");
      if (gatwayName == null || gateway.equals(gatwayName)) {
        ret.put(uuid, c);
      }
    }
    return ret;
  }

  static String formatConnection(Map<String, Object> c) {
    return String.format("%s %s %s", c.get("id"), c.get("uri"), c.get("uuid"));
  }

  static public String getConnectionName(String uuid) {
    Map<String, Object> c = Runtime.getConnection(uuid);
    if (c != null)
      return formatConnection(c);
    else
      return null;
  }

  // FIXME - if Connection was an abstract this could be promoted or abstracted
  // for
  // every service display properties
  static public List<String> getConnectionNames() {
    List<String> ret = new ArrayList<>();
    Map<String, Map<String, Object>> connections = Runtime.getConnections();
    for (String uuid : connections.keySet()) {
      Map<String, Object> c = connections.get(uuid);
      ret.add(formatConnection(c));
    }
    return ret;
  }

  /**
   * get a specific clients data
   * 
   * @param uuid
   * @return
   */
  static public Map<String, Object> getConnection(String uuid) {
    return connections.get(uuid);
  }

  /**
   * Globally get all connection ids
   * 
   * @return
   */
  static public List<String> getConnectionIds() {
    return getConnectionIds(null);
  }

  static public boolean connectionExists(String uuid) {
    return connections.containsKey(uuid);
  }

  /**
   * Get connection ids that belong to a specific connection
   * 
   * @param name
   * @return
   */
  static public List<String> getConnectionIds(String name) {
    List<String> ret = new ArrayList<>();
    for (String uuid : connections.keySet()) {
      Map<String, Object> c = connections.get(uuid);
      String gateway = (String) c.get("gateway");
      if (name == null || gateway.equals(name)) {
        ret.add(uuid);
      }
    }
    return ret;
  }

  public static Class<?> getClass(String name) {
    ServiceInterface si = registry.get(name);
    if (si == null) {
      return null;
    }
    return si.getClass();
  }

  /**
   * takes an id returns a connection uuid
   * 
   * @param id
   * @return
   */
  public static String getRoute(String id) {
    Set<String> ret = routeTable.get(id);
    if (ret != null) {
      return ret.iterator().next();
    }
    return defaultRoute;
  }

  /**
   * get gateway based on remote address of a msg e.g. msg.getRemoteId()
   * 
   * @param remoteId
   * @return
   */
  public static Gateway getGatway(String remoteId) {
    // get a route from the remote id
    String uuid = getRoute(remoteId);

    // get a connection from the route
    Map<String, Object> conn = getConnection(uuid);

    // find the gateway managing the connection
    return (Gateway) getService((String) conn.get("gateway"));
  }

  // TODO - sendRemote
  public Object sendBlockingRemote(Message msg, Integer timeout) throws IOException {
    if (isLocal(msg)) {
      log.error("msg NOT REMOTE yet sendBlockingRemote is called {}", msg);
      return null;
    }

    if (msg.sender == null) {
      // msg.sender = getName();
      log.error("blocking remote msg must have a return sender address");
      return null;
    }

    // get the appropriate connection - Runtime has a r ? wasync connection
    // get a route from the remote id
    String uuid = getRoute(msg.getId());

    // get a connection from the route
    Map<String, Object> conn = getConnection(uuid);

    // make sure msg is blocking
    // FIXME - should be enum !!!
    msg.setBlocking();

    Object ret = null;

    String type = (String) conn.get("c-type");
    if ("wasync".equals(type)) {

      // block the thread and wait for the return -
      // if you have an msgId it should be stored a new one generated - and wait
      // for its
      // return - then replace with original and send back
      // "send back" locally or - remotely remote --> gateway (process) gateway
      // --> remote
      // FIXME implement !
      Endpoint endpoint = (Endpoint) conn.get("c-endpoint");
      endpoint.socket.fire(CodecUtils.toJson(msg));

      Object[] returnContainer = new Object[1];

      inbox.blockingList.put(msg.msgId, returnContainer);

      try {
        // block until message comes back
        synchronized (returnContainer) {
          outbox.add(msg);
          returnContainer.wait(timeout);
        }
      } catch (InterruptedException e) {
        log.error("interrupted", e);
      }
      ret = returnContainer[0];
    } else if ("cli".equals(type)) {
      
      if (msg.getId() == null || msg.getSrcId().equals(String.format("%s-cli", getId()))) {

        // runtime is a gateway for cli - so change the name - invoke it
        // and send it back
        msg.name = msg.getName(); // this makes it "local"

        ret = invoke(msg);
      } else {
        log.error("local id does not match cli msg {}", msg.getFullName());
      }

    } else {
      log.error("do not know how to handle connection type " + type);
    }

    return ret;
  }

  @Override
  public List<String> getClientIds() {
    return Runtime.getConnectionIds(getName());
  }

  @Override
  public Map<String, Map<String, Object>> getClients() {
    return Runtime.getConnections(getName());
  }

  // FIXME - remove if not using ...
  @Override
  public void sendRemote(Message msg) throws IOException {
    if (isLocal(msg)) {
      log.error("msg NOT REMOTE yet sendBlockingRemote is called {}", msg);
      return;
    }

    if (msg.sender == null) {
      // msg.sender = getName();
      log.error("blocking remote msg must have a return sender address");
      return;
    }

    // get the appropriate connection - Runtime has a r ? wasync connection
    // get a route from the remote id
    String uuid = getRoute(msg.getId());

    // get a connection from the route
    Map<String, Object> conn = getConnection(uuid);

    if (stdInClient.isLocal(msg)) {
      if (msg.data == null) {
        System.out.println("null");
        return;
      }
      // should really "always" be a single item in the array since its a return
      // msg
      // but just in case ...
      for (Object o : msg.data) {
        System.out.println(CodecUtils.toPrettyJson(o));
      }

      System.out.println(stdInClient.getPrompt(uuid));

    } else {
      Endpoint endpoint = (Endpoint) conn.get("c-endpoint");
      endpoint.socket.fire(CodecUtils.toJson(msg));
    }
  }

  /**
   * DONT MODIFY NAME - JUST work on is Local - and InvokeOn should handle it
   * 
   * if the incoming Message's remote Id is the (same as ours) OR (it can't be
   * found it our route table) - peel it off and treat it as local.
   * 
   * if we have an @{id/connection} but do not have the connection - we'll peel
   * off the @{id/connection} and treat it as local if id is ours - peel it off
   * !
   */
  public boolean isLocal(Message msg) {

    if (msg.getId() == null || getId().equals(msg.getId())) {
      return true;
    }

    return false;
  }

}