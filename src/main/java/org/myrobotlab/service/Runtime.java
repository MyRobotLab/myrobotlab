package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
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
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.codec.CodecUtils.ApiDescription;
import org.myrobotlab.framework.CmdOptions;
import org.myrobotlab.framework.DescribeQuery;
import org.myrobotlab.framework.DescribeResults;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodCache;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.Plan;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.MessageListener;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.framework.repo.IvyWrapper;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.lang.NameGenerator;
import org.myrobotlab.logging.AppenderType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Connection;
import org.myrobotlab.net.Host;
import org.myrobotlab.net.HttpRequest;
import org.myrobotlab.net.Pinger;
import org.myrobotlab.net.RouteTable;
import org.myrobotlab.net.WsClient;
import org.myrobotlab.process.InProcessCli;
import org.myrobotlab.process.Launcher;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.data.ServiceTypeNameResults;
import org.myrobotlab.service.interfaces.ConnectionManager;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.LocaleProvider;
import org.myrobotlab.service.interfaces.RemoteMessageHandler;
import org.myrobotlab.service.interfaces.ServiceLifeCycle;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.myrobotlab.string.StringUtil;
import org.slf4j.Logger;

import picocli.CommandLine;

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
public class Runtime extends Service implements MessageListener, ServiceLifeCycle, RemoteMessageHandler, ConnectionManager, Gateway, LocaleProvider {
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
   * which connection a client is from) is in the Map &lt;String, Object&gt; information.
   * Since different connections have different requirements, and details regarding
   * clients the only "fixed" required info to add a client is :
   * 
   * uuid - key unique identifier for the client
   * connection - name of the connection currently managing the clients connection
   * state - state of the client and/or connection
   * (lots more attributes with the Map&lt;String, Object&gt; to provide necessary data for the connection)
   * </pre>
   */
  protected final Map<String, Connection> connections = new HashMap<>();

  /**
   * corrected route table with (soon to be regex ids) mapped to
   * gateway/interfaces
   */
  protected final RouteTable routeTable = new RouteTable();

  /**
   * map to hide methods we are not interested in
   */
  static private Set<String> hideMethods = new HashSet<>();

  static private final String RUNTIME_NAME = "runtime";
  static public final String DATA_DIR = "data";

  static private boolean autoAcceptLicense = true; // at the moment

  /**
   * number of services created by this runtime
   */
  protected Integer creationCount = 0;

  /**
   * the local repo of this machine - it should not be static as other foreign
   * repos will come in with other Runtimes from other machines.
   */
  transient private IvyWrapper repo = null; // was transient abstract Repo

  transient private ServiceData serviceData = ServiceData.getLocalInstance();

  /**
   * command line options
   */
  static CmdOptions options = new CmdOptions();

  /**
   * the platform (local instance) for this runtime. It must be a non-static as
   * multiple runtimes will have different platforms
   */
  protected Platform platform = null;

  private static long uniqueID = new Random(System.currentTimeMillis()).nextLong();

  public final static Logger log = LoggerFactory.getLogger(Runtime.class);

  /**
   * Object used to synchronize initializing this singleton.
   */
  transient private static final Object INSTANCE_LOCK = new Object();

  /**
   * The singleton of this class.
   */
  transient private static Runtime runtime = null;

  private List<String> jvmArgs;

  /**
   * set of known hosts
   */
  private transient Map<String, Host> hosts = null;

  /**
   * global startingArgs - whatever came into main each runtime will have its
   * individual copy
   */
  // FIXME - remove static !!!
  static String[] globalArgs;

  static Set<String> networkPeers = null;

  private static final String LIBRARIES = "libraries";

  String stdCliUuid = null;

  InProcessCli cli = null;

  /**
   * available Locales
   */
  transient protected Map<String, Locale> locales;

  /**
   * Returns the number of processors available to the Java virtual machine.
   * 
   * @return
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
    return createService(name, type, null);
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

  /**
   * Setting the runtime virtual will set the platform virtual too. All
   * subsequent services will be virtual
   */
  public boolean setVirtual(boolean b) {
    setAllVirtual(b);
    return b;
  }

  static public boolean setAllVirtual(boolean b) {
    Platform.setVirtual(true);
    for (ServiceInterface si : getServices()) {
      if (!si.isRuntime()) {
        si.setVirtual(b);
      }
    }
    Runtime.getInstance().isVirtual = b;
    Runtime.getInstance().broadcastState();
    return b;
  }

  static public synchronized ServiceInterface createService(String name, String type, String inId) {
    log.info("Runtime.createService {}", name);

    if (name == null) {
      log.error("service name cannot be null");
    }

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

    String id = (inId == null) ? Platform.getLocalInstance().getId() : inId;
    if (name == null || name.length() == 0 || fullTypeName == null || fullTypeName.length() == 0) {
      log.error("{} not a type or {} not defined ", fullTypeName, name);
      return null;
    }

    // TODO - test new create of existing service
    ServiceInterface sw = Runtime.getService(String.format("%s@%s", name, id));
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
      Object newService = Instantiator.getThrowableNewInstance(null, fullTypeName, name, id);
      log.debug("returning {}", fullTypeName);
      ServiceInterface si = (ServiceInterface) newService;

      // si.setId(id);
      if (Platform.getLocalInstance().getId().equals(id)) {
        si.setVirtual(Platform.isVirtual());
        Runtime.getInstance().creationCount++;
        si.setOrder(Runtime.getInstance().creationCount);
      }

      if (runtime != null) {

        runtime.broadcast("created", getFullName(name));

        // add all the service life cycle subscriptions
        runtime.addListener("registered", name);
        runtime.addListener("created", name);
        runtime.addListener("started", name);
        runtime.addListener("stopped", name);
        runtime.addListener("released", name);
      }

      // initialization of the new service - it gets local registery events
      // for pre-existing registered? created/started - NO !!!
      // The new service is responsible for asking the registry for existing services on its creation
      // this should not be done automatically
      /*
      List<ServiceInterface> services = getServices();// getLocalServices();
      for (ServiceInterface s : services) {
        if (runtime != null && runtime.serviceData != null) {
          try {
            si.onRegistered(new Registration(s));
            runtime.send(s.getName(), "onCreated", si.getFullName());
          } catch (Exception e) {
            runtime.error(String.format("onRegistered threw processing %s.onRegistered(%s)", s.getName(), name));
          }
        }
        // don't register or create or start event self
        if (s.getName().equals(si.getName())) {
          continue;
        }        
      }
      */

      return (Service) newService;
    } catch (Exception e) {
      log.error("createService failed for {}@{} of type {}", name, id, fullTypeName, e);
    }
    return null;
  }

  static public Map<String, Map<String, List<MRLListener>>> getNotifyEntries() {
    Map<String, Map<String, List<MRLListener>>> ret = new TreeMap<String, Map<String, List<MRLListener>>>();
    Map<String, ServiceInterface> sorted = getLocalServices();
    for (Map.Entry<String, ServiceInterface> entry : sorted.entrySet()) {
      log.info(entry.getKey() + "/" + entry.getValue());
      List<String> flks = entry.getValue().getNotifyListKeySet();
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

  /**
   * Returns the amount of free memory in the Java Virtual Machine. Calling the
   * gc method may result in increasing the value returned by freeMemory.
   * 
   * @return
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
      synchronized (INSTANCE_LOCK) {
        if (runtime == null) {

          runtime = new Runtime(RUNTIME_NAME, Platform.getLocalInstance().getId());

          // setting the singleton security
          Security.getInstance();
          runtime.getRepo().addStatusPublisher(runtime);
          FileIO.extractResources();
        }
      }
    }
    return runtime;
  }

  /**
   * The jvm args which started this process
   * 
   * @return all jvm args in a list
   */
  static public List<String> getJvmArgs() {
    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    return runtimeMxBean.getInputArguments();
  }

  /**
   * gets all non-loopback, active, non-virtual ip addresses
   * 
   * @return list of local ipv4 IP addresses
   */
  static public List<String> getIpAddresses() {
    log.debug("getLocalAddresses");
    ArrayList<String> ret = new ArrayList<String>();

    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface current = interfaces.nextElement();
        // log.info(current);
        if (!current.isUp() || current.isLoopback() || current.isVirtual()) {
          log.debug("skipping interface is down, a loopback or virtual");
          continue;
        }
        Enumeration<InetAddress> addresses = current.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress currentAddress = addresses.nextElement();

          if (!(currentAddress instanceof Inet4Address)) {
            log.debug("not ipv4 skipping");
            continue;
          }

          if (currentAddress.isLoopbackAddress()) {
            log.debug("skipping loopback address");
            continue;
          }
          log.debug(currentAddress.getHostAddress());
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
      List<String> local = getIpAddresses();
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
    List<String> myips = getIpAddresses(); // TODO - if nothing else -
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

  static public List<ApiDescription> getApis() {
    return CodecUtils.getApis();
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
        ret.add(m);
        log.info("added mac");
      }
    } catch (Exception e) {
      log.error("getLocalHardwareAddresses threw", e);
    }

    log.info("done");
    return ret;
  }

  public static Map<String, ServiceInterface> getLocalServices() {
    Map<String, ServiceInterface> local = new HashMap<>();
    for (String serviceName : registry.keySet()) {
      // FIXME @ should be a requirement of "all" entries for consistency
      if (!serviceName.contains("@") || serviceName.endsWith(String.format("@%s", Platform.getLocalInstance().getId()))) {
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
   * 
   * FIXME - INPUT PARAMETER SHOULD BE TYPE NOT INSTANCE NAME !!!!
   */
  public static Map<String, MethodEntry> getMethodMap(String inName) {
    String serviceName = getFullName(inName);
    if (!registry.containsKey(serviceName)) {
      runtime.error(String.format("%1$s not in registry - can not return method map", serviceName));
      return null;
    }

    ServiceInterface sw = registry.get(serviceName);
    Class<?> c = sw.getClass();

    MethodCache cache = MethodCache.getInstance();
    return cache.getRemoteMethods(c.getTypeName());

  }

  /**
   * getServiceList returns the most important identifiers for a service which
   * are it's process id, it's name, and it's type.
   * 
   * This will be part of the getHelloRequest - and the first listing from a
   * process of what services are available.
   * 
   * TODO - future work would be to supply a query to the getServiceList(query)
   * such that interfaces, types, or processes ids, can selectively be queried
   * out of it
   * 
   * @return
   */
  synchronized public List<Registration> getServiceList() {
    List<Registration> ret = new ArrayList<>();
    for (ServiceInterface si : registry.values()) {
      // problem with
      // ret.add(new NameAndType(si.getId(), si.getName(), si.getType(),
      // CodecUtils.toJson(si)));
      ret.add(new Registration(si.getId(), si.getName(), si.getType()));
    }
    return ret;
  }

  // FIXME - scary function - returns private data
  public static Map<String, ServiceInterface> getRegistry() {
    return registry;// FIXME should return copy
  }

  public static ServiceInterface getService(String inName) {
    if (inName == null) {
      return null;
    }

    String name = getFullName(inName);

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
      ServiceInterface s = si.get(i);

      if (s.getId().contentEquals(Platform.getLocalInstance().getId())) {
        ret[i] = s.getName();
      } else {
        ret[i] = s.getFullName();
      }

      // ret[i] = s.getFullName();
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

  /**
   * 
   * @param interfaze
   * @return
   * @throws ClassNotFoundException
   */
  public static List<String> getServiceNamesFromInterface(String interfaze) throws ClassNotFoundException {
    if (!interfaze.contains(".")) {
      interfaze = "org.myrobotlab.service.interfaces." + interfaze;
    }
    return getServiceNamesFromInterface(Class.forName(interfaze));
  }

  /**
   * 
   * @param interfaze
   * @return
   */ // FIXME !!! NOT RETURNING FULL NAMES !!!
  public static List<String> getServiceNamesFromInterface(Class<?> interfaze) {
    List<String> ret = new ArrayList<String>();
    List<ServiceInterface> services = getServicesFromInterface(interfaze);
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
      ServiceInterface si = registry.get(serviceName);
      if (si.getId().equals(id)) {
        list.add(registry.get(serviceName));
      }
    }
    return list;
  }

  /**
   * 
   * @param interfaze
   * @return
   */
  public ServiceTypeNameResults getServiceTypeNamesFromInterface(String interfaze) {
    ServiceTypeNameResults results = new ServiceTypeNameResults(interfaze);
    try {

      if (!interfaze.contains(".")) {
        interfaze = "org.myrobotlab.service.interfaces." + interfaze;
      }

      ServiceData sd = ServiceData.getLocalInstance();

      List<MetaData> sts = sd.getServiceTypes();

      for (MetaData st : sts) {

        Set<Class<?>> ancestry = new HashSet<Class<?>>();
        Class<?> targetClass = Class.forName(st.getType()); // this.getClass();

        while (targetClass.getCanonicalName().startsWith("org.myrobotlab") && !targetClass.getCanonicalName().startsWith("org.myrobotlab.framework")) {
          ancestry.add(targetClass);
          targetClass = targetClass.getSuperclass();
        }

        for (Class<?> c : ancestry) {
          Class<?>[] interfaces = Class.forName(c.getName()).getInterfaces();
          for (Class<?> inter : interfaces) {
            if (interfaze.equals(inter.getName())) {
              results.serviceTypes.add(st.getType());
              break;
            }
          }
        }
      }

    } catch (Exception e) {
      error("could not find interfaces for %s", interfaze);
    }

    return results;
  }

  /**
   * return a list of services which are currently running and implement a
   * specific interface
   * 
   * @param interfaze
   * @return
   */
  public static synchronized List<ServiceInterface> getServicesFromInterface(Class<?> interfaze) {
    List<ServiceInterface> ret = new ArrayList<ServiceInterface>();

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

  public void startInteractiveMode() {
    startInteractiveMode(System.in, System.out);
  }

  public InProcessCli startInteractiveMode(InputStream in, OutputStream out) {
    stopInteractiveMode();

    cli = new InProcessCli(this, "runtime", in, out);
    Connection c = cli.getConnection();
    stdCliUuid = (String) c.get("uuid");

    // addRoute(".*", getName(), 100);
    addConnection(stdCliUuid, cli.getId(), c);

    return cli;
  }

  public void stopInteractiveMode() {
    if (cli != null) {
      cli.stop();
      cli = null;
    }
    if (stdCliUuid != null) {
      removeConnection(stdCliUuid);
      stdCliUuid = null;
    }
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

  public void onState(ServiceInterface updatedService) {
    log.info("runtime updating registry info for remote service {}", updatedService.getName());
    registry.put(String.format("%s@%s", updatedService.getName(), updatedService.getId()), updatedService);
  }

  /**
   * Registration is the process where a remote system sends detailed info
   * related to its services. It will have details on each service type, state,
   * id, and other info. The registration is serializable, with state
   * information in a serialized for so that stateless processes or other
   * non-Java instances can register or be registered.
   * 
   * Registration might setup subscriptions to support a UI.
   * 
   * Additional info which will be added in the future is a method map (a
   * swagger concept) and a list of supported interfaces
   * 
   * TODO - have rules on what registrations to accept - dependent on security,
   * desire, re-broadcasting configuration etc. TODO - determine rules on
   * re-broadcasting based on configuration
   * 
   * @param registration
   * @return
   */
  public final static synchronized Registration register(Registration registration) {

    try {

      // TODO - have rules on what registrations to accept - dependent on
      // security, desire, re-broadcasting configuration etc.

      String fullname = String.format("%s@%s", registration.getName(), registration.getId());
      if (registry.containsKey(fullname)) {
        log.info("{} already registered", fullname);
        return registration;
      }

      log.info("{}@{} registering at {} of type {}", registration.getName(), registration.getId(), Platform.getLocalInstance().getId(), registration.getTypeKey());

      if (!registration.isLocal(Platform.getLocalInstance().getId())) {
        // de-serialize
        registration.service = Runtime.createService(registration.getName(), registration.getTypeKey(), registration.getId());
        copyShallowFrom(registration.service, CodecUtils.fromJson(registration.getState(), Class.forName(registration.getTypeKey())));
      }

      registry.put(fullname, registration.service);

      if (runtime != null) {
        // TODO - determine rules on re-broadcasting based on configuration
        runtime.broadcast("registered", registration);
      }

      // TODO - remove ? already get state from registration
      if (!registration.isLocal(Platform.getLocalInstance().getId())) {
        runtime.subscribe(registration.getName(), "publishState");
      }

    } catch (Exception e) {
      log.error("registration threw for {}@{}", registration.getName(), registration.getId(), e);
      return null;
    }

    return registration;
  }

  /**
   * releases a service - stops the service, its threads, releases its
   * resources, and removes registry entries
   *
   * FIXME - clean up subscriptions from released
   * 
   * @param inName
   * @return
   */
  public synchronized static boolean release(String inName) {
    String name = getFullName(inName);

    log.info("releasing service {}", name);

    if (!registry.containsKey(name)) {
      log.info("{} not registered", name);
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
      if (runtime != null) {
        runtime.send(name, "releaseService");
      }
    }

    unregister(name);

    return true;
  }

  synchronized public static void unregister(String inName) {
    String name = getFullName(inName);
    log.info("unregister {}", name);

    // get reference from registry
    ServiceInterface sw = registry.get(name);
    if (sw == null) {
      log.info("{} already unregistered", name);
      return;
    }

    // you have to send released before removing from registry
    if (runtime != null) {
      runtime.broadcast("released", name); // <- DO NOT CHANGE THIS IS CORRECT
                                           // !!
      // it should be FULLNAME !
      // runtime.broadcast("released", inName);
    }

    // last step - remove from registry
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

      try {
        if (sw != null) {
          sw.stopService();
          runtime.invoke("released", sw.getFullName());
        }
      } catch (Exception e) {
        runtime.error("%s threw while stopping", e);
      }
    }

    if (runtime != null) {
      runtime.stopService();
    }
    log.debug("clearing registry");
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
    try {
      log.debug("mrl shutdown");

      if (runtime != null) {
        runtime.stopInteractiveMode();
      }

      for (ServiceInterface service : getServices()) {
        service.preShutdown();
      }

      for (ServiceInterface service : getServices()) {
        service.save();
      }

      releaseAll();
    } catch (Exception e) {
      log.error("something threw - continuing to shutdown", e);
    }

    System.exit(0);
  }

  public Integer publishShutdown(Integer seconds) {
    return seconds;
  }

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

  /**
   * shutdown and remove a service from the registry
   * 
   * @param name
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

  public void connect() throws IOException {
    connect(options.connect); // FIXME - 0 to many
  }

  // FIXME - implement ! also implement the callback events .. onDisconnect
  public void disconnect() throws IOException {
    // connect("admin", "ws://localhost:8887/api/messages");
    log.info("disconnect");
  }

  /**
   * FIXME - can this be renamed back to attach ? jump to another process using
   * the cli
   * 
   * @param id
   * @return
   */
  // FIXME - remove - the way to 'jump' is just to change
  // context to the correct mrl id e.g. cd /runtime@remote07
  public String jump(String id) {
    Connection c = getRoute(stdCliUuid);
    if (c != null && c.get("cli") != null) {
      ((InProcessCli) c.get("cli")).setRemote(id);
    } else {
      log.error("connection or cli is null for uuid {}", stdCliUuid);
    }

    return id;
  }

  // FIXME - remove ?!?!!?
  public String exit() {
    Connection c = getConnection(stdCliUuid);
    if (c != null && c.get("cli") != null) {
      ((InProcessCli) c.get("cli")).setRemote(getId());
    }
    return getId();
  }

  public void sendToCli(String srcFullName, String cmd) {
    Connection c = getConnection(stdCliUuid);
    if (c == null || c.get("cli") == null) {
      log.info("starting interactive mode");
      startInteractiveMode();
      sleep(1000);
    }
    c = getConnection(stdCliUuid);
    if (c != null && c.get("cli") != null) {
      ((InProcessCli) c.get("cli")).process(srcFullName, cmd);
    } else {
      log.error("could not start interactive mode");
    }
  }

  // FIXME - implement
  public void connect(String url, boolean autoReconnect) {
    if (!autoReconnect) {
      connect(url);
    } else {
      addTask(1000, "checkConnections");
    }
  }

  // FIXME - implement
  public void checkConnections() {
    for (Connection connection : connections.values()) {
      if (connection.containsKey("url")) {
        /*
         * FIXME - check on "STATE" ... means we support disconnected
         * connections .. if (connection.get("url").toString().equals(url)) { //
         * already connected continue; }
         */
      }
    }
    // could not find our connection for this "id" - need to reconnect
    // connect(url);
  }

  // FIXME -
  // step 1 - first bind the uuids (1 local and 1 remote)
  // step 2 - Clients will contain attribute
  // FIXME - RETRIES TIMEOUTS OTHER COMPLEXITIES
  // blocking connect - consider a non-blocking thread connect ... e.g.
  // autoConnect
  @Override
  public void connect(String url) {
    try {

      // get authorization through POST - username/password etc..

      // use session_id from auth & id to form upgrade GET request
      // /messages?id=x&session_id=y

      // request default describe - on describe do registrations .. zzz

      WsClient client = new WsClient();
      Connection c = client.connect(this, getFullName(), getId(), url);

      // URI uri = new URI(url);
      // adding "id" as full url :P ... because we don't know it !!!
      addConnection(client.getUuid(), url, c);

      // direct send - may not have and "id" so it will be too runtime vs
      // runtime@{id}
      // subscribe to "describe"
      MRLListener listener = new MRLListener("describe", getFullName(), "onDescribe");
      Message msg = Message.createMessage(getFullName(), "runtime", "addListener", listener);
      client.send(CodecUtils.toJson(msg));

      // send describe
      client.send(CodecUtils.toJson(getDescribeMsg(null)));

    } catch (Exception e) {
      log.error("connect to {} giving up {}", url, e.getMessage());
    }
  }

  /**
   * FIXME - this is a gateway callback - probably should be in the gateway
   * interface - this is a "specific" gateway that supports typeless json or
   * websockets
   * 
   * FIXME - decoding should be done at the Connection ! - this should be
   * onRemoteMessage(msg) !
   * 
   * callback - from clientRemote - all client connections will recieve here
   * TODO - get clients directional api - an api per direction incoming and
   * outgoing
   * 
   * uuid - connection for incoming data
   */
  @Override // uuid
  public void onRemoteMessage(String uuid, String data) {
    try {

      // log.debug("connection {} responded with {}", uuid, data);
      // get api - decode msg - process it
      Connection connection = getConnection(uuid);
      if (connection == null) {
        error("no connection with uuid %s", uuid);
        return;
      }

      if (log.isDebugEnabled()) {
        log.debug("data - [{}]", data);
      }

      // decoding message envelope
      Message msg = CodecUtils.fromJson(data, Message.class);
      log.info("==> {} --> {}.{}", msg.sender, msg.name, msg.method);
      msg.setProperty("uuid", uuid); // Properties ???? REMOVE ???

      if (msg.containsHop(getId())) {
        log.error("{} dumping duplicate hop msg to avoid cyclical from {} --to--> {}.{} | {}", getName(), msg.sender, msg.name, msg.method, msg.getHops());
        return;
      }

      addRoute(msg.getSrcId(), uuid, 10);

      // add our id - we don't want to see it again
      msg.addHop(getId());

      Object ret = null;

      // FIXME - see if same code block exists in WebGui .. normalize
      if (isLocal(msg)) {

        // log.info("--> {}.{} from {}", msg.name, msg.method, msg.sender);

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

        ret = method.invoke(si, params);

        // propagate return data to subscribers
        si.out(msg.method, ret);

      } else {
        log.info("GATEWAY {} RELAY {} --to--> {}.{}", getName(), msg.sender, msg.name, msg.method);
        send(msg);
      }

    } catch (Exception e) {
      log.error("processing msg threw", e);
    }
  }

  public void addRoute(String remoteId, String uuid, int metric) {
    routeTable.addRoute(remoteId, uuid, metric);
  }

  static public ServiceInterface start(String name, String type) {
    return createAndStart(name, type);
  }

  public Runtime(String n, String id) {
    super(n, id);

    synchronized (INSTANCE_LOCK) {
      if (runtime == null) {
        // fist and only time....
        runtime = this;
        repo = (IvyWrapper) Repo.getInstance(LIBRARIES, "IvyWrapper");
      }
    }

    setLocale(Locale.getDefault().getTag());
    locales = Locale.getDefaults();

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
    if (globalArgs != null) {
      for (int i = 0; i < globalArgs.length; ++i) {
        sb.append(globalArgs[i]);
      }
    }
    if (jvmArgs != null) {
      log.info("jvmArgs {}", Arrays.toString(jvmArgs.toArray()));
    }
    log.info("file.encoding {}", System.getProperty("file.encoding"));
    log.info("args {}", Arrays.toString(globalArgs));

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

    log.info("============== platform ==============");
    long startTime = platform.getStartTime().getTime();
    log.info("{} - GMT - {}", sdf.format(startTime), gmtf.format(startTime));
    log.info("pid {}", platform.getPid());
    log.info("hostname {}", platform.getHostname());
    log.info("ivy [runtime,{}.{}.{}]", platform.getArch(), platform.getJvmBitness(), platform.getOS());
    log.info("version {} branch {} commit {} build {}", platform.getVersion(), platform.getBranch(), platform.getCommit(), platform.getBuild());
    log.info("platform manifest {}", Platform.getManifest());
    log.info("platform [{}}]", platform);
    log.info("version [{}]", platform.getVersion());
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

    String vmVersion = System.getProperty("java.specification.version");
    vmVersion = "11";
    if ("1.8".equals(vmVersion)) {
      error("Unsupported Java %s - please remove version and install Java 1.8", vmVersion);
    }

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

    if (repo != null)/* transient */ {
      repo.addStatusPublisher(this);
    }

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
      log.error("OMG Runtime won't start GAME OVER ! :(", e);
    }
  }

  public String publishDefaultRoute(String defaultRoute) {
    return defaultRoute;
  }

  /**
   * publishing event - since checkForUpdates may take a while
   */
  public void checkingForUpdates() {
    log.info("checking for updates");
  }

  static public String getInputAsString(InputStream is) {
    try (java.util.Scanner s = new java.util.Scanner(is)) {
      return s.useDelimiter("\\A").hasNext() ? s.next() : "";
    }
  }

  /**
   * list the contents of the current working directory
   * 
   * @return
   */
  public Object ls() {
    return ls(null, null);
  }

  public Object ls(String path) {
    return ls(null, path);
  }

  /**
   * list the contents of a specific path
   * 
   * @param path
   * @return
   */
  public Object ls(String contextPath, String path) {
    String absPath = null;

    if (contextPath != null) {
      path = contextPath + path;
    }

    if (path == null) {
      path = "/";
    }

    // ALL SHOULD BE ABSOLUTE PATH AT THIS POINT
    // IE STARTING WITH /

    if (!path.startsWith("/")) {
      path = "/" + path;
    }

    absPath = path;

    String[] parts = absPath.split("/");

    String ret = null;
    if (absPath.equals("/")) {
      return Runtime.getServiceNames();
    } else if (parts.length == 2 && !absPath.endsWith("/")) {
      return Runtime.getService(parts[1]);
    } else if (parts.length == 2 && absPath.endsWith("/")) {
      ServiceInterface si = Runtime.getService(parts[1]);
      return si.getDeclaredMethodNames();
      /*
       * } else if (parts.length == 3 && !absPath.endsWith("/")) { // execute 0
       * parameter function ??? return Runtime.getService(parts[1]);
       */
    } else if (parts.length == 3) {
      ServiceInterface si = Runtime.getService(parts[1]);
      MethodCache cache = MethodCache.getInstance();
      List<MethodEntry> me = cache.query(si.getType(), parts[2]);
      return me; // si.getMethodMap().get(parts[2]);
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
   * FIXME - see if this is used anymore publishing point of Ivy sub system -
   * sends event failedDependency when the retrieve report for a Service fails
   */
  @Deprecated /* remove */
  public String failedDependency(String dep) {
    return dep;
  }

  public static Platform getPlatform() {
    return getInstance().platform;
  }

  // FIXME - should be removed - use Platform.getLocalInstance().is64bit()
  @Deprecated
  public boolean is64bit() {
    return getInstance().platform.getJvmBitness() == 64;
  }

  public Repo getRepo() {
    return repo;
  }

  /**
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

  /**
   * getServiceTypeNames will publish service names based on some filter
   * criteria
   * 
   * @param filter
   * @return
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

  static public Status publishNoWorky(Status status) {
    return status;
  }

  public String publishMessage(String msg) {
    return msg;
  }

  @Override
  @Deprecated /* use onResponse ??? */
  public void onMessage(Message msg) {
    // TODO: what do we do when we get a message?
    log.info("onMessage()");
  }

  /**
   * Publishing point when a service was successfully registered locally -
   * regardless if the service is local or not.
   * 
   * TODO - more business logic can be created here to limit broadcasting or
   * re-broadcasting published registrations
   * 
   * @param registration
   *          - contains all the information need for a registration to process
   * @return
   */
  public Registration registered(Registration registration) {
    return registration;
  }

  /**
   * released event - when a service is successfully released from the registry
   * this event is triggered
   * 
   * @param serviceName
   * @return
   */
  public String released(String serviceName) {
    return serviceName;
  }

  /**
   * restart occurs after applying updates - user or config data needs to be
   * examined and see if its an appropriate time to restart - if it is the
   * spawnBootstrap method will be called and bootstrap.jar will go through its
   * sequence to update myrobotlab.jar
   */
  public void restart() {
    // to avoid deadlock of shutting down from external messages
    // we spawn a kill thread
    new Thread("kill-thread") {
      public void run() {
        try {

          info("restarting");

          // export to file lastRestart.py
          exportAll("lastRestart.py");

          // shutdown all services process - send ready to shutdown - ask back
          // release all services
          for (ServiceInterface service : getServices()) {
            service.preShutdown();
          }

          // check if ready ???

          // release all local services
          releaseAll();

          if (runtime != null) {
            runtime.releaseService();
          }

          options.fromLauncher = true; // ???

          // make sure python is included
          options.services.add("python");
          options.services.add("Python");

          // force invoke
          options.invoke = new String[] { "python", "execFile", "lastRestart.py" };

          // create builder from Launcher daemonize ?
          log.info("re launching with commands \n{}", CmdOptions.toString(options.getOutputCmd()));
          ProcessBuilder pb = Launcher.createBuilder(options);

          // fire it off
          Process restarted = pb.start();

          // dramatic pause
          sleep(2000);

          // check if process exists
          if (restarted.isAlive()) {
            log.info("yay! we continue to live in future generations !");
          } else {
            log.error("omg! ... I killed all the services and now there is no offspring ! :(");
          }
          log.error("goodbye ...");
          shutdown();
        } catch (Exception e) {
          log.error("shutdown threw", e);
        }
      }
    }.start();
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
    logging.addAppender(AppenderType.FILE);
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
    if (runtime != null) {
      runtime.stopInteractiveMode();
    }

    super.stopService();

    // cannot close thread on this connection
    /*
     * (new Thread() { public void run() { closeConnections(); } }).start();
     */

    runtime = null;
  }

  public void closeConnections() {
    for (Connection c : connections.values()) {
      String gateway = c.getGateway();
      if (getFullName().equals(gateway)) {
        WsClient client = (WsClient) c.get("c-client");
        client.close();
      }
    }
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
      List<String> nlks = si.getNotifyListKeySet();
      for (int i = 0; i < nlks.size(); ++i) {
        si.getOutbox().notifyList.clear();
      }
    }
  }

  public static List<Status> getErrors() {
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

  public ServiceData getServiceData() {
    return serviceData;
  }

  /**
   * Return supported system languages
   */
  public Map<String, Locale> getLanguages() {
    return Locale.getAvailableLanguages();
  }

  public Map<String, Locale> getLocales() {
    return locales;
  }

  public Map<String, Locale> setLocales(String... codes) {
    locales = Locale.getLocaleMap(codes);
    return locales;
  }

  /**
   * get the Security singleton
   * 
   * @return
   */
  static public Security getSecurity() {
    return Security.getInstance();
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

      StringBuilder sb = new StringBuilder();
      sb.append("import json\n");

      String[] services = getServiceNames();

      for (String name : services) {
        ServiceInterface si = Runtime.getService(name);
        String safeName = CodecUtils.getSafeReferenceName(name);
        sb.append(String.format("%s = Runtime.start(\"%s\",\"%s\")\n", safeName, name, si.getType()));
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
      Files.write(Paths.get("backup-routes.py"), CodecUtils.toJson(getNotifyEntries()).getBytes());

      log.info("finished...");

    } catch (Exception e) {
      log.error("backup threw", e);
    }
  }

  public static Runtime getInstance(String[] args2) {
    Runtime.main(args2);
    return Runtime.getInstance();
  }

  public static CmdOptions getOptions() {
    return options;
  }

  public ServiceData setServiceTypes(ServiceData sd) {
    return sd;
  }

  /**
   * FIXME - describe will have the capability to describe many aspects of a
   * running service. Default behavior will show a list of local names, but
   * depending on input criteria it should be possible to show * interfaces *
   * service data * service methods * details of a service method * help/javadoc
   * of a service method * list of other known instances * levels of detail, or
   * lists of fields to display * meaningful default
   * 
   * FIXME - input parameters will need to change - at some point, a subscribe
   * to describe, and appropriate input parameters should replace the current
   * onRegistered system
   * 
   * @param type
   * @param id
   * @param remoteUuid
   * @return
   */
  public DescribeResults describe(String type, String id, String remoteUuid) {
    DescribeQuery query = new DescribeQuery(type, remoteUuid);
    return describe(type, query);
  }

  public DescribeResults describe() {
    // default query
    return describe("platform", null);
  }

  /**
   * Describe results returns the information of a "describe" which can be
   * detailed information regarding services, theire methods and input or output
   * types.
   * 
   * FIXME - describe(String[] filters) where filter can be name, type, local,
   * state, etc
   * 
   * 
   * @param uuidX
   * @param query
   * @return
   */
  public DescribeResults describe(String uuidXz, DescribeQuery query) {

    DescribeResults results = new DescribeResults();
    results.setStatus(Status.success("Ahoy!"));

    try {

      results.setId(getId());
      results.setPlatform(Platform.getLocalInstance());

      // broadcast completed connection information
      invoke("getConnections"); // FIXME - why isn't this done before ???

      Set<String> set = registry.keySet();
      String[] list = new String[set.size()];
      set.toArray(list);

      // TODO - filtering on what is broadcasted or re-broadcasted
      for (int i = 0; i < list.length; ++i) {
        String fullname = list[i];
        ServiceInterface si = registry.get(fullname);

        Registration registration = new Registration(si);

        results.addRegistration(registration);
      }

    } catch (Exception e) {
      log.error("describe threw", e);
    }

    return results;
  }

  /**
   * Describe results from remote query to describe
   * 
   * @param results
   */
  public void onDescribe(DescribeResults results) {
    List<Registration> reservations = results.getReservations();
    if (getId().equals("c1")) {
      log.info("here");
    }
    if (reservations != null) {
      for (int i = 0; i < reservations.size(); ++i) {
        register(reservations.get(i));
      }
    }
  }

  /**
   * IMPORTANT IMPORTANT IMPORTANT - Newly connected remote mrl processes blas a
   * list of registrations through onRegistered messages, for each service they
   * currently have in their registry. This process will send a list of
   * registrations to the newly connected remote process. If the "registered"
   * event is subscribed, any newly created service will be broadcasted thorough
   * this publishing point as well.
   * 
   * TODO - write filtering, configuration, or security which affects what can
   * be registered
   * 
   * Primarily, this is where new services are registered from remote systems
   * 
   * @param registration
   */
  public void onRegistered(Registration registration) {
    try {
      // check if registered ?

      // TODO - filtering - include/exclude

      String fullname = registration.getName() + "@" + registration.getId();
      if (!registry.containsKey(fullname)) {
        register(registration);
        if (fullname.startsWith("runtime@")) {
          // We want to TELL remote runtime if we have new registrations - we'll
          // send them
          // to it's runtime
          // subscribe(fullname, "registered");
          // subscribe(fullname, "released");
          // IMPORTANT w
          addListener("registered", fullname);
          addListener("released", fullname);
        }
      } else {
        log.info("{} onRegistered already registered", fullname);
      }
    } catch (Exception e) {
      log.error("onRegistered threw {}", registration, e);
    }
  }

  public void onAuthenticate(DescribeResults response) {
    log.info("onAuthenticate {}", response);
  }

  public List<MetaData> getServiceTypes() {
    return serviceData.getServiceTypes();
  }

  public void addConnection(String uuid, String id, Connection connection) {
    Connection attr = null;
    if (!connections.containsKey(uuid)) {
      attr = connection;
      invoke("publishConnect", connection);
    } else {
      attr = connections.get(uuid);
      attr.putAll(connection);
    }
    connections.put(uuid, attr);
    // String id = (String)attr.get("id");

    addRoute(id, uuid, 10);
  }

  @Override
  public Message getDescribeMsg(String connId) {
    // TODO support queries
    // FIXME !!! - msg.name is wrong with only "runtime" it should be
    // "runtime@id"
    // TODO - lots of options for a default "describe"
    Message msg = Message.createMessage(String.format("%s@%s", getName(), getId()), "runtime", "describe",
        new Object[] { "fill-uuid", CodecUtils.toJson(new DescribeQuery(Platform.getLocalInstance().getId(), connId)) });

    return msg;
  }

  public void removeConnection(String uuid) {

    Connection conn = connections.remove(uuid);

    if (conn != null) {
      invoke("publishDisconnect", uuid);
      invoke("getConnections");

      Set<String> remoteIds = routeTable.getAllIdsFor(uuid);
      for (String id : remoteIds) {
        unregisterId(id);
      }
      routeTable.removeRoute(uuid);
    }
  }

  public void unregisterId(String id) {
    Set<String> names = new HashSet<>(registry.keySet());
    for (String name : names) {
      if (name.endsWith("@" + id)) {
        unregister(name);
      }
    }
  }

  public String publishDisconnect(String uuid) {
    return uuid;
  }

  // FIXME - filter only serializable objects ?
  public Connection publishConnect(Connection attributes) {
    return attributes;
  }

  /**
   * globally get all client
   * 
   * @return
   */
  public Map<String, Connection> getConnections() {
    return connections;
  }

  /**
   * separated by connection - send connection name and get filter results back
   * for a specific connections connected clients
   * 
   * @param gatwayName
   * @return
   */
  public Map<String, Connection> getConnections(String gatwayName) {
    Map<String, Connection> ret = new HashMap<>();
    for (String uuid : connections.keySet()) {
      Connection c = connections.get(uuid);
      String gateway = (String) c.get("gateway");
      if (gatwayName == null || gateway.equals(gatwayName)) {
        ret.put(uuid, c);
      }
    }
    return ret;
  }

  /**
   * list connections - current connection names to this mrl runtime
   * 
   * @return
   */
  public Map<String, Connection> lc() {
    return getConnections();
  }

  /**
   * get a specific clients data
   * 
   * @param uuid
   * @return
   */
  public Connection getConnection(String uuid) {
    return connections.get(uuid);
  }

  /**
   * Globally get all connection uuids
   * 
   * @return
   */
  public List<String> getConnectionUuids() {
    return getConnectionUuids(null);
  }

  boolean connectionExists(String uuid) {
    return connections.containsKey(uuid);
  }

  /**
   * Get connection ids that belong to a specific gateway
   * 
   * @param name
   * @return
   */
  public List<String> getConnectionUuids(String name) {
    List<String> ret = new ArrayList<>();
    for (String uuid : connections.keySet()) {
      Connection c = connections.get(uuid);
      String gateway = (String) c.get("gateway");
      if (name == null || gateway.equals(name)) {
        ret.add(uuid);
      }
    }
    return ret;
  }

  public static Class<?> getClass(String inName) {
    String name = getFullName(inName);
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
  public Connection getRoute(String id) {
    return connections.get(routeTable.getRoute(id));
  }

  public RouteTable getRouteTable() {
    return routeTable;
  }

  /**
   * get gateway based on remote address of a msg e.g. msg.getRemoteId()
   * 
   * @param remoteId
   * @return
   */
  public Gateway getGatway(String remoteId) {
    // get a connection from the route
    Connection conn = getRoute(remoteId);
    if (conn == null) {
      log.error("no connection for id {}", remoteId);
      return null;
    }
    // find the gateway managing the connection
    return (Gateway) getService((String) conn.get("gateway"));
  }

  static public String getFullName(String shortname) {
    if (shortname == null) {
      return null;
    }
    if (shortname.contains("@")) {
      // already long form
      return shortname;
    }
    // if nothing is supplied assume local
    return String.format("%s@%s", shortname, Platform.getLocalInstance().getId());
  }

  @Override
  public List<String> getClientIds() {
    return getConnectionUuids(getName());
  }

  @Override
  public Map<String, Connection> getClients() {
    return getConnections(getName());
  }

  public void pollHosts() {
    runtime.addTask(20000, "getHosts");
  }

  // FIXME - remove if not using ...
  @Override
  public void sendRemote(Message msg) throws IOException {
    if (isLocal(msg)) {
      log.error("msg NOT REMOTE yet sendRemote is called {}", msg);
      return;
    }

    // get a connection from the route
    Connection conn = getRoute(msg.getId());
    if (conn == null) {
      log.error("could not get connection for {} from msg {}", msg.getId(), msg);
      return;
    }

    // two possible types of "remote" for this gateway cli & ws
    if ("Cli".equals(conn.get("c-type"))) {
      invoke("publishCli", msg);

      InProcessCli cli = ((InProcessCli) conn.get("cli"));
      cli.onMsg(msg);

    } else {
      // websocket Client !
      WsClient client = (WsClient) conn.get("c-client");
      if (client == null) {
        log.error("could not get client for connection {}", msg.getId());
        return;
      }

      /**
       * ======================================================================
       * DYNAMIC ROUTE TABLE - outbound msg hop starts now
       */

      // add our id - we don't want to see it again
      msg.addHop(getId());

      log.info("<== {}.{} <-- {}", msg.name, msg.method, msg.sender);

      /**
       * ======================================================================
       */

      client.send(CodecUtils.toJsonMsg(msg));
    }
  }

  public Object publishCli(Message msg) {
    if (msg.data == null || msg.data.length == 0) {
      return null;
    }
    return msg.data[0];
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

  public Object localizeDefault(String key) {
    key = key.toUpperCase();
    return defaultLocalization.get(key);
  }

  static public void setAllLocales(String code) {
    for (ServiceInterface si : getLocalServices().values()) {
      si.setLocale(code);
    }
  }

  @Override
  public String created(String serviceName) {
    return serviceName;
  }

  @Override
  public String started(String serviceName) {
    return serviceName;
  }

  @Override
  public String stopped(String serviceName) {
    return serviceName;
  }

  public static void setPeer(String fullKey, String actualName, String serviceType) {
    ServiceData.setPeer(fullKey, actualName, serviceType);
  }

  public static Plan getPlan(String serviceName, String serviceType) {
    return ServiceData.getPlan(serviceName, serviceType);
  }

  public static void clearPlan() {
    ServiceData.clearOverrides();
  }

  public static MetaData getMetaData(String serviceName, String serviceType) {
    return ServiceData.getMetaData(serviceName, serviceType);
  }

  public static MetaData getMetaData(String serviceType) {
    return ServiceData.getMetaData(serviceType);
  }

  public static boolean exists() {
    return runtime != null;
  }

  /**
   * Attempt to get the most likely valid address priority would be a lan
   * address - possibly the smallest class
   * 
   * @return
   */
  public String getAddress() {
    List<String> addresses = getIpAddresses();
    if (addresses.size() > 0) {

      // class priority
      for (String ip : addresses) {
        if (ip.startsWith("192.168")) {
          return ip;
        }
      }

      for (String ip : addresses) {
        if (ip.startsWith("172.")) {
          return ip;
        }
      }

      for (String ip : addresses) {
        if (ip.startsWith("10.")) {
          return ip;
        }
      }

      // give up - return first :P
      return addresses.get(0);
    }
    return null;
  }

  public List<Host> getHosts() {
    List<String> ips = getIpAddresses();
    String selectedIp = (ips.size() == 1) ? ips.get(0) : null;
    if (selectedIp == null) {
      for (String ip : ips) {
        if ((selectedIp != null) && (ip.startsWith(("192.")))) {
          selectedIp = ip;
        } else if (selectedIp == null) {
          selectedIp = ip;
        }
      }
    }
    String subnet = selectedIp.substring(0, selectedIp.lastIndexOf("."));
    return getHosts(subnet);
  }

  public List<Host> getHosts(String subnet) {

    if (hosts == null) {
      hosts = new HashMap<String, Host>();
      File check = new File(FileIO.gluePaths(getDataDir(), "hosts.json"));
      if (check.exists()) {
        try {
          Host[] hf = CodecUtils.fromJson(FileIO.toString(check), Host[].class);
          for (Host h : hf) {
            hosts.put(h.ip, h);
          }
          info("found %d saved hosts", hosts.size());
        } catch (Exception e) {
          error("could not load %s - %s", check, e.getMessage());
        }
      }
    }

    int timeout = 1500;
    try {
      for (int i = 1; i < 255; i++) {
        Thread pinger = new Thread(new Pinger(this, hosts, subnet + "." + i, timeout), "pinger-" + i);
        pinger.start();
      }
    } catch (Exception e) {
      log.error("getHosts threw", e);
    }
    List<Host> h = new ArrayList<>();
    for (Host hst : hosts.values()) {
      if (hst.lastActiveTs != null) {
        h.add(hst);
      }
    }
    return h;
  }

  public Host publishFoundHost(Host host) {
    log.info("found host {}", host);
    return host;
  }

  public Host publishFoundNewHost(Host host) {
    log.info("found new host {}", host);
    return host;
  }

  public Host publishLostHost(Host host) {
    log.info("lost host {}", host);
    return host;
  }

  public void saveHosts() throws IOException {
    FileOutputStream fos = new FileOutputStream(FileIO.gluePaths(getDataDir(), "hosts.json"));
    List<Host> h = new ArrayList<>(hosts.values());
    String json = CodecUtils.toPrettyJson(h);
    fos.write(json.getBytes());
    fos.close();
  }

  /**
   * start python interactively at the command line
   */
  public void python() {
    if (cli == null) {
      startInteractiveMode();
    }
    start("python", "Python");
    // since we've suscribed to pythons st
    cli.relay("python", "exec", "publishStdOut");
    cli.relay("python", "exec", "publishStdError");
    Logging logging = LoggingFactory.getInstance();
    logging.removeAllAppenders();
  }

  /**
   * Main entry point for the MyRobotLab Runtime Check CmdOptions for list of
   * options -h help -v version -list jvm args -Dhttp.proxyHost=webproxy
   * f-Dhttp.proxyPort=80 -Dhttps.proxyHost=webproxy -Dhttps.proxyPort=80
   * 
   * @param args
   */
  public static void main(String[] args) {

    try {

      globalArgs = args;

      new CommandLine(options).parseArgs(args);

      // initialize logging
      initLog();

      log.info("in args {}", Launcher.toString(args));
      log.info(CodecUtils.toJson(options));

      log.info("\n" + Launcher.banner);

      // help and exit
      if (options.help) {
        mainHelp();
        return;
      }

      // if a you specify a config file it becomes the "base" of configuration
      // inline flags will still override values
      if (options.config != null) {
        try {
          log.info("loading options {}", options.config);
          options = (CmdOptions) CodecUtils.fromJson(FileIO.toString(options.config), CmdOptions.class);
        } catch (Exception e) {
          log.error("config file {} was specified but could not be read", options.config);
          shutdown();
        }
      }

      // id always required
      if (options.id == null) {
        options.id = NameGenerator.getName();
      }

      String id = (options.fromLauncher) ? options.id : String.format("%s-launcher", options.id);

      // fix paths
      Platform platform = Platform.getLocalInstance();
      platform.setId(id);

      // save an output of our cmd options
      File dataDir = new File(Runtime.DATA_DIR);
      if (!dataDir.exists()) {
        dataDir.mkdirs();
      }

      try {
        Files.write(Paths.get(dataDir + File.separator + "lastOptions.json"), CodecUtils.toPrettyJson(options).getBytes());
      } catch (Exception e) {
        log.error("writing lastOption.json failed", e);
      }

      if (options.virtual) {
        Platform.setVirtual(true);
      }

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
          repo.install(LIBRARIES, (String) null);
        } else {
          for (String service : options.install) {
            repo.install(LIBRARIES, service);
          }
        }
        shutdown();
        return;
      }

      if (!options.fromLauncher) {
        // ===== I AM A LAUNCHER =====
        // spawn new instance, inherit io
        // any options need stripping ?
        // handle daemon
        // TODO handle more than one instance
        ProcessBuilder builder = Launcher.createBuilder(options);
        Process process = builder.start();
        process.waitFor();
        return;

      } else {
        // ===== I AM A SPAWNED INSTANCE =====
        // create service instances
        createAndStartServices(options.services);
        // getInstance().startInteractiveMode();

        if (options.invoke != null) {
          invokeCommands(options.invoke);
        }

        if (options.connect != null) {
          Runtime.getInstance().connect(options.connect);
        }

        if (options.autoUpdate) {
          // initialize
          // FIXME - use peer ?
          Updater.main(args);
        }

        if (options.fromLauncher) {
          Runtime.getInstance().startInteractiveMode(System.in, System.out);
        }

      }

    } catch (Exception e) {
      log.error("runtime exception", e);
      Runtime.mainHelp();
      shutdown();
      log.error("main threw", e);
    }
  }

  public static void initLog() {
    if (options != null) {
      LoggingFactory.init(options.logLevel);
    } else {
      LoggingFactory.init("info");
    }
  }

  public void test() {
    for (int statusCnt = 0; statusCnt < 500; statusCnt++) {
      statusCnt++;
      invoke("publishStatus", Status.info("this is status %d", statusCnt));
    }
  }

  public Connection getConnectionFromId(String remoteId) {
    for (Connection c : connections.values()) {
      if (c.getId().equals(remoteId)) {
        return c;
      }
    }
    return null;
  }

  /**
   * A gateway is responsible for creating a key to associate a unique "Connection".
   * This key should be retrievable, when a msg arrives at the service which needs to 
   * be sent remotely. This key is used to get the "Connection" to send the msg remotely
   * 
   * @param string
   * @param uuid
   */
  public void addLocalGatewayKey(String string, String uuid) {
    routeTable.addLocalGatewayKey(string, uuid);
  }

  public boolean containsRoute(String remoteId) {
    return routeTable.contains(remoteId);
  }

  public String getConnectionUuidFromGatewayKey(String gatewayKey) {
    return routeTable.getConnectionUuid(gatewayKey);
  }

}
