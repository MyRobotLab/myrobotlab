package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
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

import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Decoder;
import org.atmosphere.wasync.Encoder;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.myrobotlab.codec.ApiFactory;
import org.myrobotlab.codec.ApiFactory.ApiDescription;
import org.myrobotlab.codec.CodecJson;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.NameAndType;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
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
public class Runtime extends Service implements MessageListener {
  final static private long serialVersionUID = 1L;

  static public boolean is64bit() {
    return Platform.getLocalInstance().getBitness() == 64;
  }

  // FIXME - AVOID STATIC FIELDS !!! use .getInstance() to get the singleton

  /**
   * instances of MRL - keyed with an instance key URI format is
   * mrl://gateway/(protocol key)
   */

  /**
   * environments of running mrl instances - the null environment is the current
   * local
   */
  static private final Map<URI, ServiceEnvironment> environments = new HashMap<URI, ServiceEnvironment>();

  /**
   * a registry of all services regardless of which environment they came from -
   * each must have a unique name
   */
  static private final TreeMap<String, ServiceInterface> registry = new TreeMap<String, ServiceInterface>();

  /**
   * map to hide methods we are not interested in
   */
  static private HashSet<String> hideMethods = new HashSet<String>();

  static private boolean needsRestart = false;

  static private String runtimeName;

  /**
   * the id of the agent which spawned us
   */
  // static String fromAgent = null;

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

  // private boolean shutdownAfterUpdate = false;

  static transient Cli cli;

  /**
   * global startingArgs - whatever came into main each runtime will have its
   * individual copy
   */
  // FIXME - remove static !!!
  static String[] globalArgs;

  private static String gateway;

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

      gateway = FileIO.toString(con.getInputStream());
      return gateway;

    } catch (Exception e) {
      log.warn("internet not available");
    }
    return null;
  }

  static public synchronized ServiceInterface create(String name, String type) {
    String fullTypeName;
    if (name.indexOf("/") != -1) {
      throw new IllegalArgumentException(String.format("can not have forward slash / in name %s", name));
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
    ServiceEnvironment se = getLocalServices();
    Map<String, ServiceInterface> sorted = new TreeMap<String, ServiceInterface>(se.serviceDirectory);
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

      FileOutputStream dump = new FileOutputStream("environments.json");
      dump.write(CodecUtils.toJson(environments).getBytes());
      dump.close();

      dump = new FileOutputStream("registry.json");
      dump.write(CodecUtils.toJson(registry).getBytes());
      dump.close();

      StringBuffer sb = new StringBuffer().append("\ninstances:\n");
      Map<URI, ServiceEnvironment> sorted = environments;
      Iterator<URI> hkeys = sorted.keySet().iterator();
      URI url;
      ServiceEnvironment se;
      Iterator<String> it2;
      String serviceName;
      ServiceInterface sw;
      while (hkeys.hasNext()) {
        url = hkeys.next();
        se = environments.get(url);
        sb.append("\t").append(url);

        // Service Environment
        Map<String, ServiceInterface> sorted2 = new TreeMap<String, ServiceInterface>(se.serviceDirectory);
        it2 = sorted2.keySet().iterator();
        while (it2.hasNext()) {
          serviceName = it2.next();
          sw = sorted2.get(serviceName);
          sb.append("\t\t").append(serviceName);
          sb.append("\n");
        }
      }

      sb.append("\nregistry:");

      Map<String, ServiceInterface> sorted3 = new TreeMap<String, ServiceInterface>(registry);
      Iterator<String> rkeys = sorted3.keySet().iterator();
      while (rkeys.hasNext()) {
        serviceName = rkeys.next();
        sw = sorted3.get(serviceName);
        sb.append("\n").append(serviceName).append(" ").append(sw.getInstanceId());
      }

      sb.toString();

      FileIO.toFile(String.format("serviceRegistry.%s.txt", runtime.getName()), sb.toString());

      return sb.toString();
    } catch (Exception e) {
      Logging.logError(e);
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
   * get the one and only Cli
   * 
   * @return - command line interpreter
   */
  public static Cli getCli() {
    if (cli == null) {
      cli = startCli();
    }
    return cli;
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

  public static ServiceEnvironment getLocalServices() {
    if (!environments.containsKey(null)) {
      runtime.error("local (null) ServiceEnvironment does not exist");
      return null;
    }

    return environments.get(null);
  }

  /*
   * getLocalServicesForExport returns a filtered map of Service references to
   * export to another instance of MRL. The objective of filtering may help
   * resolve functionality, security, or technical issues. For example, the
   * Dalvik JVM can only run certain Services. It would be error prone to export
   * a SwingGui to a jvm which does not support swing.
   *
   * Since the map of Services is made for export - it is NOT a copy but
   * references
   *
   * The filtering is done by Service Type.. although in the future it could be
   * extended to Service.getName()
   *
   */
  public static ServiceEnvironment getLocalServicesForExport() {
    if (!environments.containsKey(null)) {
      runtime.error("local (null) ServiceEnvironment does not exist");
      return null;
    }

    ServiceEnvironment local = environments.get(null);

    // URI is null but the "acceptor" will fill in the correct URI/ID
    ServiceEnvironment export = new ServiceEnvironment(null);

    Iterator<String> it = local.serviceDirectory.keySet().iterator();
    String name;
    ServiceInterface sw;
    while (it.hasNext()) {
      name = it.next();
      sw = local.serviceDirectory.get(name);
      if (security == null || security.allowExport(name)) {
        // log.info(String.format("exporting service: %s of type %s",
        // name, sw.getServiceType()));
        export.serviceDirectory.put(name, sw); // FIXME !! make note
        // when Xmpp or Remote
        // Adapter pull it -
        // they have to reset
        // this !!
      } else {
        log.info("security prevents export of {}", name);
        continue;
      }
    }

    return export;
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
      me.parameterTypes = m.getParameterTypes();
      me.returnType = m.getReturnType();
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

  /*
   * Return the named service - - if name is not null, but service is not found
   * - return null (for re-entrant Service creation) - if the name IS null,
   * return Runtime - to support api/getServiceNames - if the is not null, and
   * service is found - return the Service
   */
  public static ServiceInterface getService(String name) {

    if (name == null || name.length() == 0) {
      return Runtime.getInstance();
    }
    if (!registry.containsKey(name)) {
      return null;
    } else {
      return registry.get(name);
    }
  }

  public static ServiceEnvironment getEnvironment(URI url) {
    if (environments.containsKey(url)) {
      return environments.get(url); // FIXME should return copy
    }
    return null;
  }

  /*
   * get all environments
   */
  public static HashMap<URI, ServiceEnvironment> getEnvironments() {
    return new HashMap<URI, ServiceEnvironment>(environments);
  }

  // Reference - cpu utilization
  // http://www.javaworld.com/javaworld/javaqa/2002-11/01-qa-1108-cpu.html

  /*
   * list of currently created services
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
    List<ServiceInterface> list = new ArrayList<ServiceInterface>(registry.values());
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
    return getDiffTime(now.getTime() - platform.getStartTime().getTime());
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
    ServiceEnvironment se = getLocalServices();
    Iterator<String> it = se.serviceDirectory.keySet().iterator();
    String serviceName;
    while (it.hasNext()) {
      serviceName = it.next();
      ServiceInterface sw = se.serviceDirectory.get(serviceName);
      ret &= sw.load();
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
          CodecJson codec = new CodecJson();
          options = (CmdOptions) codec.decode(FileIO.toString(options.cfg), CmdOptions.class);
        } catch (Exception e) {
          log.error("config file {} was specified but could not be read", options.cfg);
        }
      }

      try {
        Files.write(Paths.get(dataDir + File.separator + "lastOptions.json"), CodecJson.encodePretty(options).getBytes());
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

    } catch (Exception e) {
      Runtime.mainHelp();
      shutdown();
      log.error("main threw", e);
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
    ServiceEnvironment se = environments.get(updatedService.getInstanceId());
    if (se != null) {
      se.serviceDirectory.put(updatedService.getName(), updatedService);
    } else {
      error("onState ServiceEnvironment null");
    }
  }

  /*
   * register - this method enters the service into the registery of services
   *
   */
  public final static synchronized ServiceInterface register(ServiceInterface s, URI url) {
    ServiceEnvironment se = null;
    if (!environments.containsKey(url)) {
      se = new ServiceEnvironment(url);
      environments.put(url, se);
    } else {
      se = environments.get(url);
    }

    /**
     * register with null data is how initial communication starts between 2 mrl
     * instances (1st msg)
     */
    if (s == null) {
      return null;
    }

    String name = s.getName();

    // REMOTE BROADCAST to all foreign environments
    // FIXME - Security determines what to export
    // for each gateway

    // RELAY - xforwarder name grows remote1.remote2.remote3 ....

    List<String> remoteGateways = getServiceNamesFromInterface(Gateway.class);
    for (int ri = 0; ri < remoteGateways.size(); ++ri) {
      String n = remoteGateways.get(ri);
      // Communicator gateway = (Communicator)registry.get(n);
      ServiceInterface gateway = registry.get(n);

      // for each JVM this gateway is attached too
      for (Map.Entry<URI, ServiceEnvironment> o : environments.entrySet()) {
        URI uri = o.getKey();
        // if its a foreign JVM & the gateway responsible for the
        // remote
        // connection and
        // the foreign JVM is not the host which this service
        // originated
        // from - send it....
        if (uri != null && gateway.getName().equals(uri.getHost()) && !uri.equals(s.getInstanceId())) {
          log.info("gateway {} sending registration of {} remote to {}", gateway.getName(), name, uri);
          // FIXME - Security determines what to export
          Message msg = Message.createMessage(runtime, null, "register", s);
          // ((Communicator) gateway).sendRemote(uri, msg);
          // //mrl://remote2/tcp://127.0.0.1:50488 <-- wrong
          // sendingRemote is wrong
          // FIXME - optimize gateway.send(msg) && URI TO URI MAP
          // IN
          // RUNTIME !!!
          gateway.in(msg);
        }
      }
    }

    // ServiceInterface sw = new ServiceInterface(s, se.accessURL);
    se.serviceDirectory.put(name, s);
    // WARNING - SHOULDN'T THIS BE DONE FIRST AVOID DEADLOCK / RACE
    // CONDITION ????
    registry.put(name, s); // FIXME FIXME FIXME FIXME !!!!!!
    // pre-pend
    // URI if not NULL !!!
    if (runtime != null) {
      runtime.invoke("registered", s);
    }

    // new --------
    // we want to subscribe to state changes
    if (!s.isLocal()) {
      runtime.subscribe(name, "publishState");
    }
    // end new ----

    return s;

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

    // remove from environments
    ServiceEnvironment se = environments.get(sw.getInstanceId());
    se.serviceDirectory.remove(name);

    log.info("released {}", name);
  }

  public static boolean release(URI url) /* release process environment */
  {
    boolean ret = true;
    ServiceEnvironment se = environments.get(url);
    if (se == null) {
      log.warn("attempt to release {} not successful - it does not exist", url);
      return false;
    }
    log.info(String.format("releasing url %1$s", url));
    String[] services = se.serviceDirectory.keySet().toArray(new String[se.serviceDirectory.keySet().size()]);
    String runtimeName = null;
    ServiceInterface service;
    for (int i = 0; i < services.length; ++i) {
      service = registry.get(services[i]);
      if (service != null && "Runtime".equals(service.getSimpleName())) {
        runtimeName = service.getName();
        log.info(String.format("delaying release of Runtime %1$s", runtimeName));
        continue;
      }
      ret &= release(services[i]);
    }

    if (runtimeName != null) {
      ret &= release(runtimeName);
    }

    return ret;
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

    ServiceEnvironment se = environments.get(null); // local services only
    if (se == null) {
      log.info("releaseAll called when everything is released, all done here");
      return;
    }
    Iterator<String> seit = se.serviceDirectory.keySet().iterator();
    String serviceName;
    ServiceInterface sw;

    seit = se.serviceDirectory.keySet().iterator();
    while (seit.hasNext()) {
      serviceName = seit.next();
      sw = se.serviceDirectory.get(serviceName);

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

    log.info("clearing hosts environments");
    environments.clear();

    log.info("clearing registry");
    registry.clear();

    // exit () ?
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

  /*
   * save all configuration from all local services
   */
  static public boolean saveAll() {
    boolean ret = true;
    ServiceEnvironment se = getLocalServices();
    Iterator<String> it = se.serviceDirectory.keySet().iterator();
    String serviceName;
    while (it.hasNext()) {
      serviceName = it.next();
      ServiceInterface sw = se.serviceDirectory.get(serviceName);
      ret &= sw.save();
    }

    return ret;
  }

  static public void connectTo(String url) throws IOException {
    connectTo(url, null);
  }

  static public void connectTo(String url, String body) throws IOException {

    // connect via websocket
    Client<?, ?, ?> client = ClientFactory.getDefault().newClient();

    RequestBuilder<?> request = client.newRequestBuilder().method(Request.METHOD.GET).uri(url).encoder(new Encoder<String, Reader>() { // Stream
                                                                                                                                       // the
                                                                                                                                       // request
                                                                                                                                       // body
      @Override
      public Reader encode(String s) {
        return new StringReader(s);
      }
    }).decoder(new Decoder<String, Reader>() {
      @Override
      public Reader decode(Event type, String s) {
        return new StringReader(s);
      }
    }).transport(Request.TRANSPORT.WEBSOCKET) // Try WebSocket
        .transport(Request.TRANSPORT.LONG_POLLING); // Fallback to Long-Polling

    Socket socket = client.create();
    socket.on(new Function<Reader>() {
      @Override
      public void on(Reader r) {
        // Read the response
        log.info("on(Reader) here");
        // r.read(cbuf, off, len);
      }
    }).on(new Function<IOException>() {

      @Override
      public void on(IOException ioe) {
        // Some IOException occurred
        log.info("on(IOException) here");
      }

    }).open(request.build()).fire("echo").fire("bong");

    // client.w
  }

  public static void setRuntimeName(String inName) {
    runtimeName = inName;
  }

  static public ServiceInterface start(String name, String type) {
    return createAndStart(name, type);
  }

  static public Cli startCli() {
    cli = (Cli) start("cli", "Cli");
    return cli;
  }

  static public void stopCli() {
    if (cli != null) {
      release(cli.getName());
    }
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
    public String webgui = "localhost";

    // FIXME - implement
    // AGENT INFO
    @Option(names = { "-u", "--update-agent" }, description = "updates agent with the latest versions of the current branch")
    public boolean updateAgent = false;

    // FIXME - does this get executed by another CommandLine ?
    // AGENT INFO
    @Option(names = { "-g",
        "--agent" }, description = "command line options for the agent must be in quotes e.g. --agent \"--service pyadmin Python --invoke pyadmin execFile myadminfile.py\"")
    public String agent;

    // FIXME -rename to daemon
    // AGENT INFO
    @Option(names = { "-f", "--fork" }, description = "forks the agent, otherwise the agent will terminate self if all processes terminate")
    public boolean fork = false;

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

    @Option(names = { "--libraries" }, description = "sets the location of the libraries directory")
    public String libraries = "libraries";

    // FIXME - get version vs force version - perhaps just always print version
    // in help
    @Option(names = { "-v", "--version" }, arity = "0..1", description = "requested version or if left blank return version")
    public String version;

    @Option(names = { "-s", "--service",
        "--services" }, arity = "1..*", description = "services requested on startup, the services must be {name} {Type} paired, e.g. gui SwingGui webgui WebGui servo Servo ...")
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

    // 3 states
    // isAgent == make default directory (with pid) if custom not supplied
    // fromAgent == needs agentId
    // neither ... == normal pid file !isAgent & !fromAgent

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
    log.info("============== env end ==============");

    // Platform platform = Platform.getLocalInstance();
    log.info("============== normalized ==============");
    long startTime = platform.getStartTime().getTime();
    log.info("{} - GMT - {}", sdf.format(startTime), gmtf.format(startTime));
    log.info("pid {}", platform.getPid());
    log.info("hostname {}", platform.getHostname());
    log.info("ivy [runtime,{}.{}.{}]", platform.getArch(), platform.getBitness(), platform.getOS());
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

  /*
   * returns version string of MyRobotLab
   */
  public String getLocalVersion() {
    return getVersion(null);
  }

  // FIXME - you don't need that many "typed" messages - resolve,
  // resolveError, ... etc
  // just use & parse "message"

  public static Platform getPlatform() {
    return getInstance().platform;
  }

  /**
   * returns the platform type of a remote system
   *
   * @param uri
   *          - the access uri of the remote system
   * @return Platform description
   */
  public Platform getPlatform(URI uri) {
    ServiceEnvironment local = environments.get(uri);
    if (local != null) {
      return local.platform;
    }

    error("can't get local platform in service environment");

    return null;
  }

  public Repo getRepo() {
    return repo;
  }

  /**
   * Gets the current total number of services registered services. This is the
   * number of services in all Service Environments
   *
   * @return total number of services
   */
  public int getServiceCount() {
    int cnt = 0;
    Iterator<URI> it = environments.keySet().iterator();
    ServiceEnvironment se;
    Iterator<String> it2;
    while (it.hasNext()) {
      se = environments.get(it.next());
      it2 = se.serviceDirectory.keySet().iterator();
      while (it2.hasNext()) {
        ++cnt;
        it2.next();
      }
    }
    return cnt;
  }

  // ============== configuration begin ==============

  static public String getId() {
    return Platform.getLocalInstance().getId();
  }

  public int getEnvironmentCount() {
    return environments.size();
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

  /**
   * returns version string of MyRobotLab instance based on uri e.g : uri
   * mrl://10.5.3.1:7777 may be a remote instance null uri is local
   *
   * @param uri
   *          - key of ServiceEnvironment
   * @return version string
   */
  public String getVersion(URI uri) {
    ServiceEnvironment local = environments.get(uri);
    if (local != null) {
      return local.platform.getVersion();
    }

    error("can't get local version in service environment");

    return null;
  }

  /*
   * event fired when a new artifact is download
   */
  public String newArtifactsDownloaded(String module) {
    return module;
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
  public void onMessage(Message msg) {
    // TODO: what do we do when we get a message?
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

      Message msg = Message.createMessage(this, "agent", "restart", platform.getId());
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

  public static void clearErrors() {
    ServiceEnvironment se = getLocalServices();
    for (String name : se.serviceDirectory.keySet()) {
      se.serviceDirectory.get(name).clearLastError();
    }
  }

  public static boolean hasErrors() {
    ServiceEnvironment se = getLocalServices();

    for (String name : se.serviceDirectory.keySet()) {
      if (se.serviceDirectory.get(name).hasError()) {
        return true;
      }
    }
    return false;
  }

  /**
   * remove all subscriptions from all local Services
   */
  static public void removeAllSubscriptions() {
    ServiceEnvironment se = getEnvironment(null);
    Set<String> keys = se.serviceDirectory.keySet();
    for (String name : keys) {
      ServiceInterface si = getService(name);
      ArrayList<String> nlks = si.getNotifyListKeySet();
      for (int i = 0; i < nlks.size(); ++i) {
        si.getOutbox().notifyList.clear();
      }
    }
  }

  public static ArrayList<Status> getErrors() {
    ArrayList<Status> stati = new ArrayList<Status>();
    ServiceEnvironment se = getLocalServices();
    for (String name : se.serviceDirectory.keySet()) {
      Status status = se.serviceDirectory.get(name).getLastError();
      if (status != null && status.isError()) {
        log.info(status.toString());
        stati.add(status);
      }
    }
    return stati;
  }

  public static void broadcastStates() {
    ServiceEnvironment se = getLocalServices();

    for (String name : se.serviceDirectory.keySet()) {
      se.serviceDirectory.get(name).broadcastState();
    }
  }

  public static Runtime get() {
    return Runtime.getInstance();
  }

  public static String getRuntimeName() {
    return Runtime.getInstance().getName();
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

    // from which gateway ?
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

}