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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.myrobotlab.codec.ClassUtil;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.codec.CodecUtils.ApiDescription;
import org.myrobotlab.codec.ForeignProcessUtils;
import org.myrobotlab.framework.CmdConfig;
import org.myrobotlab.framework.CmdOptions;
import org.myrobotlab.framework.DescribeQuery;
import org.myrobotlab.framework.DescribeResults;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodCache;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.NameGenerator;
import org.myrobotlab.framework.Peer;
import org.myrobotlab.framework.Plan;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ProxyFactory;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceReservation;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.ConfigurableService;
import org.myrobotlab.framework.interfaces.MessageListener;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.framework.repo.IvyWrapper;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.framework.repo.ServiceDependency;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.AppenderType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Connection;
import org.myrobotlab.net.Host;
import org.myrobotlab.net.Http;
import org.myrobotlab.net.HttpRequest;
import org.myrobotlab.net.Pinger;
import org.myrobotlab.net.RouteTable;
import org.myrobotlab.net.WsClient;
import org.myrobotlab.process.InProcessCli;
import org.myrobotlab.process.Launcher;
import org.myrobotlab.service.config.RuntimeConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.data.ServiceTypeNameResults;
import org.myrobotlab.service.interfaces.ConnectionManager;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.LocaleProvider;
import org.myrobotlab.service.interfaces.RemoteMessageHandler;
import org.myrobotlab.service.interfaces.ServiceLifeCyclePublisher;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.myrobotlab.string.StringUtil;
import org.slf4j.Logger;

import picocli.CommandLine;

/**
 * Runtime is responsible for the creation and removal of all Services and the
 * associated static registries. It maintains state information regarding
 * possible &amp; running local Services; it also maintains state information
 * regarding foreign Runtimes. It is a singleton and should be the only service
 * of Runtime running in a process. The host and registry maps are used in
 * routing communication to the appropriate service (be it local or remote) It
 * will be the first Service created. It also wraps the real JVM Runtime object.
 * <p>
 *
 * RuntimeMXBean - scares me - but the stackTrace is clever RuntimeMXBean
 * runtimeMxBean = ManagementFactory.getRuntimeMXBean(); List&lt;String&gt;
 * arguments = runtimeMxBean.getInputArguments()
 * <p>
 * final StackTraceElement[] stackTrace =
 * Thread.currentThread().getStackTrace(); final String mainClassName =
 * stackTrace[stackTrace.length - 1].getClassName();
 * <p>
 * check for 64 bit OS and 32 bit JVM is is64bit()
 * <p>
 * FIXME - AVOID STATIC FIELDS - THE ONLY STATIC FIELD SHOULD BE THE INSTANCE *
 * VAR OF RUNTIME !
 *
 */
public class Runtime extends Service<RuntimeConfig> implements MessageListener, ServiceLifeCyclePublisher, RemoteMessageHandler, ConnectionManager, Gateway, LocaleProvider {
  final static private long serialVersionUID = 1L;

  // FIXME - AVOID STATIC FIELDS !!! use .getInstance() to get the singleton

  /**
   * a registry of all services regardless of which environment they came from -
   * each must have a unique name
   */
  static private final Map<String, ServiceInterface> registry = new TreeMap<>();

  /**
   * A plan is a request to runtime to change the system. Typically its to ask
   * to start and configure new services. The master plan is an accumulation of
   * all these requests.
   */
  @Deprecated /* use the filesystem only no memory plan */
  transient final Plan masterPlan = new Plan("runtime");

  /**
   * thread for non-blocking install of services
   */
  static private transient Thread installerThread = null;

  /**
   * services which want to know if another service with an interface they are
   * interested in registers or is released
   *
   * requestor type &gt; interface &gt; set of applicable service names
   */
  protected final Map<String, Set<String>> interfaceToNames = new HashMap<>();

  protected final Map<String, Set<String>> typeToNames = new HashMap<>();

  protected final Map<String, Set<String>> interfaceToType = new HashMap<>();

  protected final Map<String, Set<String>> typeToInterface = new HashMap<>();

  /**
   * FILTERED_INTERFACES are the set of low level interfaces which we are
   * interested in filtering out if we want to maintain a data structure which
   * has "interfaces of interest"
   */
  protected final static Set<String> FILTERED_INTERFACES = new HashSet<>(Arrays.asList("org.myrobotlab.framework.interfaces.Broadcaster",
      "org.myrobotlab.service.interfaces.QueueReporter", "org.myrobotlab.framework.interfaces.ServiceQueue", "org.myrobotlab.framework.interfaces.MessageSubscriber",
      "org.myrobotlab.framework.interfaces.Invoker", "java.lang.Runnable", "org.myrobotlab.framework.interfaces.ServiceStatus", "org.atmosphere.nettosphere.Handler",
      "org.myrobotlab.framework.interfaces.NameProvider", "org.myrobotlab.framework.interfaces.NameTypeProvider", "org.myrobotlab.framework.interfaces.ServiceInterface",
      "org.myrobotlab.framework.interfaces.TaskManager", "org.myrobotlab.framework.interfaces.LoggingSink", "org.myrobotlab.framework.interfaces.StatusPublisher",
      "org.myrobotlab.framework.interfaces.TypeProvider", "java.io.Serializable", "org.myrobotlab.framework.interfaces.Attachable",
      "org.myrobotlab.framework.interfaces.StateSaver", "org.myrobotlab.framework.interfaces.MessageSender", "java.lang.Comparable",
      "org.myrobotlab.service.interfaces.ServiceLifeCycleListener", "org.myrobotlab.framework.interfaces.StatePublisher"));

  protected final Set<String> serviceTypes = new HashSet<>();
  
  /**
   * The directory name currently being used for config. This is NOT full path
   * name. It cannot be null, it cannot have "/" or "\" in the name - it has to
   * be a valid file name for the OS. It's defaulted to "default". Changed often
   */
  @Deprecated /* use startyml.config */
  protected String configName = "default";

  /**
   * default parent path of configPath - rarely changed FIXME - don't make it
   * static !
   */
  @Deprecated /* use startYml.configRoot */
  static protected String CONFIG_ROOT = "data" + fs + "config";

  /**
   * State variable reporting if runtime is currently starting services from
   * config. If true you can find which config from runtime.getConfigName()
   */
  boolean processingConfig = false;

  /**
   * The one config directory where all config is managed the {default} is the
   * current configuration set
   */
  // protected String configDir = "data" + fs + "config";

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

  static private final String RUNTIME_NAME = "runtime";

  static public final String DATA_DIR = "data";

  static private boolean autoAcceptLicense = true; // at the moment

  /**
   * number of services created by this runtime
   */
  protected Integer creationCount = 0;

  /**
   * the local repo.json manifest of this machine, which is a list of all
   * libraries ivy installed
   */
  transient private IvyWrapper repo = null; // was transient abstract Repo

  transient private ServiceData serviceData = ServiceData.getLocalInstance();

  /**
   * command line options
   */
  static CmdOptions options = new CmdOptions();

  /**
   * command line configuration
   */
  static CmdConfig startYml = new CmdConfig();

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

  /**
   * The name of the folder used to store native library dependencies during
   * installation and runtime.
   */
  private static final String LIBRARIES = "libraries";

  String stdCliUuid = null;

  InProcessCli cli = null;

  /**
   * available Locales
   */
  transient protected Map<String, Locale> locales;

  protected List<String> configList;

  /***
   * runtime, security, webgui, perhaps python - we don't want to remove when
   * releasing config
   */
  protected Set<String> startingServices = new HashSet<>();

  /**
   * Wraps {@link java.lang.Runtime#availableProcessors()}.
   *
   * @return the number of processors available to the Java virtual machine.
   * @see java.lang.Runtime#availableProcessors()
   *
   */
  public static final int availableProcessors() {
    return java.lang.Runtime.getRuntime().availableProcessors();
  }

  /**
   * Function to test if internet connectivity is available. If it is, will
   * return the public gateway address of this computer by sending a request to
   * an external server. If there is no internet, returns null.
   * 
   * @return The public IP address or null if no internet available
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

  /**
   * Create which only has name (no type). This is only possible, if there is an
   * appropriately named service config in the Plan (in memory) or (more
   * commonly) on the filesystem. Since ServiceConfig comes with type
   * information, a name is all that is needed to start the service.
   * 
   * @param name
   * @return
   */
  static public ServiceInterface create(String name) {
    return create(name, null);
  }

  /**
   * Create create(name, type) goes through the full service lifecycle of:
   *
   * <pre>
   * clear - clearing the plan for construction of service(s) needed 
   * load  - loading the plan for desired services 
   * check - checking all planned service have met appropriate licensing and dependency checks create -
   * </pre>
   *
   * @param name
   *          - Required, cannot be null
   * @param type
   *          - Can be null if a service file exists for named service
   * @return the service
   */
  static public synchronized ServiceInterface create(String name, String type) {

    try {
      Runtime runtime = Runtime.getInstance();
      ServiceInterface si = Runtime.getService(name);
      if (si != null) {
        return si;
      }

      // FIXME remove configName from loadService
      Runtime.load(name, type);
      Runtime.check(name, type);
      // at this point - the plan should be loaded, now its time to create the
      // children peers
      // and parent service
      createServicesFromPlan(Runtime.getPlan(), null, name);
      return Runtime.getService(name);
    } catch (Exception e) {
      runtime.error(e);
    }
    return null;
  }

  /**
   * Creates all services necessary for this service - "all peers" and the
   * parent service too. At this point all type information and configuration
   * should be defined in the plan.
   * 
   * FIXME - should Plan be passed in as param ?
   *
   * @param name
   * @return
   */
  synchronized private static Map<String, ServiceInterface> createServicesFromPlan(Plan plan, Map<String, ServiceInterface> createdServices, String name) {

    if (createdServices == null) {
      createdServices = new LinkedHashMap<>();
    }

    // Plan's config
    RuntimeConfig plansRtConfig = (RuntimeConfig) plan.get("runtime");
    // current Runtime config
    RuntimeConfig currentConfig = (RuntimeConfig) Runtime.getInstance().config;

    for (String service : plansRtConfig.getRegistry()) {
      // FIXME - determine if you want to return a complete merge of activated
      // or just "recent"
      if (Runtime.getService(service) != null) {
        continue;
      }
      ServiceConfig sc = plan.get(service);
      if (sc == null) {
        runtime.error("could not get %s from plan", service);
        continue;
      }
      sc.state = "CREATING";
      ServiceInterface si = createService(service, sc.type, null);
      sc.state = "CREATED";
      // process  the base listeners/subscription of ServiceConfig
      si.addConfigListeners(sc);
      if (si instanceof ConfigurableService) {
        ((ConfigurableService)si).apply(sc);
      }
      createdServices.put(service, si);
      currentConfig.add(service);
    }

    return createdServices;
  }

  public String getServiceExample(String serviceType) {
    String url = "https://raw.githubusercontent.com/MyRobotLab/myrobotlab/develop/src/main/resources/resource/" + serviceType + "/" + serviceType + ".py";
    byte[] bytes = Http.get(url);
    if (bytes != null) {
      return new String(bytes);
    }
    return "";
  }

  public static String getPeerName(String peerKey, ServiceConfig config, Map<String, ServiceReservation> peers, String parentName) {

    if (peerKey == null || !peers.containsKey(peerKey)) {
      return null;
    }

    if (config != null) {

      // dynamically get config peer name
      // e.g. tilt should be a String value in config.tilt
      Field[] fs = config.getClass().getDeclaredFields();
      for (Field f : fs) {
        if (peerKey.equals(f.getName())) {
          if (f.canAccess(config)) {
            Object o;
            try {
              o = f.get(config);

              if (o == null) {
                // config "has" the field, just set to null at the moment
                // peer actual name then will be default notation
                if (parentName != null) {
                  return String.format("%s.%s", parentName, peerKey);
                }
                log.warn("config has field named {} but it's null", peerKey);
                return null;
              }

              if (o instanceof String) {
                return (String) o;
              } else {
                log.error("config has field named {} but it is not a string", peerKey);
                break;
              }
            } catch (Exception e) {
              log.error("getting access to field threw", e);
            }

          } else {
            log.error("config with field name {} but cannot access it", peerKey);
          }
        }
      }
    }
    // last ditch attempt at getting the name - will default it if parentName is
    // supplied
    if (parentName != null) {
      return String.format("%s.%s", parentName, peerKey);
    }
    return null;
  }

  public static void check(String name, String type) {
    log.info("check - implement - dependencies and licensing");
    // iterate through plan - check dependencies and licensing
  }

  /**
   * Use {@link #start(String, String)} instead.
   *
   * @param name
   *          Name of service
   * @param type
   *          Type of service
   * @return Created service
   */
  @Deprecated /* use start */
  static public ServiceInterface createAndStart(String name, String type) {
    return start(name, type);
  }

  /**
   * creates and starts service from a cmd line object
   *
   * @param services
   *          - services to be created
   */
  @Deprecated /* who uses this ? */
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
  @Override
  public boolean setVirtual(boolean b) {
    boolean changed = isVirtual != b;
    setAllVirtual(b);
    if (changed) {
      broadcastState();
    }
    return b;
  }

  /**
   * Sets all services' virtual state to {@code b}. This allows a single call to
   * enable or disable virtualization across all services.
   *
   * @param b
   *          Whether all services should be virtual or not
   * @return b
   */
  static public boolean setAllVirtual(boolean b) {
    Platform.setVirtual(b);
    for (ServiceInterface si : getServices()) {
      if (!si.isRuntime()) {
        si.setVirtual(b);
      }
    }
    Runtime.getInstance().isVirtual = b;
    Runtime.getInstance().broadcastState();
    return b;
  }

  /**
   * Sets the enable value in start.yml. start.yml is a file which can control
   * the automatic loading of config. In general when its on, and a config is
   * selected and saved, the next time Runtime starts it will attempt to load
   * the last saved config and get the user back to their last state.
   * 
   * @param autoStart
   * @throws IOException
   *           - thrown if cannot write file to filesystem
   */
  public void setAutoStart(boolean autoStart) throws IOException {
    log.debug("setAutoStart {}", autoStart);
    startYml.id = getId();
    startYml.enable = autoStart;
    startYml.config = configName;
    startYml.configRoot = CONFIG_ROOT;
    FileIO.toFile("start.yml", CodecUtils.toYaml(startYml));
    invoke("getStartYml");
  }

  /**
   * Framework owned method - core of creating a new service. This method will
   * create a service with the given name and of the given type. If the type
   * does not contain any dots, it will be assumed to be in the
   * {@code org.myrobotlab.service} package. This method can currently only
   * instantiate Java services, but in the future it could be enhanced to call
   * native service runtimes.
   * <p>
   * The name parameter must not contain '/' or '@'. Thus, a full name must be
   * split into its first and second part, passing the first in as the name and
   * the second as the inId. This method will log an error and return null if
   * name contains either of those two characters.
   * <p>
   * The {@code inId} is used to determine whether the service is a local one or
   * a remote proxy. It should equal the Runtime ID of the MyRobotLab instance
   * the service was originally instantiated under.
   * 
   * @param name
   *          May not contain '/' or '@', i.e. cannot be a full name
   * @param type
   *          The type of the new service
   * @param inId
   *          The ID of the runtime the service is linked to.
   * @return An existing service if the requested name and type match, otherwise
   *         a newly created service. If the name is null, or it contains '@' or
   *         '/', or a service with the same name exists but has a different
   *         type, will return null instead.
   */
  static private synchronized ServiceInterface createService(String name, String type, String inId) {
    log.info("Runtime.createService {}", name);

    if (name == null) {
      runtime.error("service name cannot be null");

      return null;
    }

    if (name.contains("@") || name.contains("/")) {
      runtime.error("service name cannot contain '@' or '/': {}", name);

      return null;
    }

    String fullName;
    if (inId == null || inId.equals(""))
      fullName = getFullName(name);
    else
      fullName = String.format("%s@%s", name, inId);

    if (type == null) {
      ServiceConfig sc = runtime.masterPlan.get(name);
      if (sc != null) {
        log.info("found type for {} in plan", name);
        type = sc.type;
      } else {
        runtime.error("createService type not specified and could not get type for {} from plan", name);
        return null;
      }
    }

    if (type == null) {
      runtime.error("cannot create service {} no type in plan or yml file", name);
      return null;
    }

    String fullTypeName;
    if (type.contains(".")) {
      fullTypeName = type;
    } else {
      fullTypeName = String.format("org.myrobotlab.service.%s", type);
    }

    ServiceInterface si = Runtime.getService(fullName);
    if (si != null) {
      if (!si.getTypeKey().equals(fullTypeName)) {
        runtime.error("Service with name {} already exists but is of type {} while requested type is ", name, si.getTypeKey(), type);
        return null;
      }
      return si;
    }

    // DO NOT LOAD HERE !!! - doing so would violate the service life cycle !
    // only try to resolve type by the plan - if not then error out

    String id = (inId == null) ? Platform.getLocalInstance().getId() : inId;
    if (name.length() == 0 || fullTypeName == null || fullTypeName.length() == 0) {
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

      // FIXME - error if deps are missing - prompt license
      // require restart !
      // FIXME - this should happen after inspecting the "loaded" "plan" not
      // during the create/start/apply !

      // create an instance
      Object newService = Instantiator.getThrowableNewInstance(null, fullTypeName, name, id);
      log.debug("returning {}", fullTypeName);
      si = (ServiceInterface) newService;

      // si.setId(id);
      if (Platform.getLocalInstance().getId().equals(id)) {
        si.setVirtual(Platform.isVirtual());
        Runtime.getInstance().creationCount++;
        si.setOrder(Runtime.getInstance().creationCount);
      }

      if (runtime != null) {

        runtime.invoke("created", getFullName(name));

        // add all the service life cycle subscriptions
        // runtime.addListener("registered", name);
        // runtime.addListener("created", name);
        // runtime.addListener("started", name);
        // runtime.addListener("stopped", name);
        // runtime.addListener("released", name);
      }

      return (Service) newService;
    } catch (Exception e) {
      log.error("createService failed for {}@{} of type {}", name, id, fullTypeName, e);
    }
    return null;
  }

  static public Map<String, Map<String, List<MRLListener>>> getNotifyEntries() {
    return getNotifyEntries(null);
  }

  static public Map<String, Map<String, List<MRLListener>>> getNotifyEntries(String service) {
    Map<String, Map<String, List<MRLListener>>> ret = new TreeMap<String, Map<String, List<MRLListener>>>();
    Map<String, ServiceInterface> sorted = null;
    if (service == null) {
      sorted = getLocalServices();
    } else {
      sorted = new HashMap<String, ServiceInterface>();
      ServiceInterface si = Runtime.getService(service);
      if (si != null) {
        sorted.put(service, si);
      }
    }
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

  /**
   * Dumps {@link #registry} to a file called {@code registry.json} in JSON
   * form.
   *
   * @return The registry in JSON form or null if an error occurred.
   */
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
   * Wraps {@link java.lang.Runtime#gc()}.
   *
   * Runs the garbage collector.
   */
  public static final void gc() {
    java.lang.Runtime.getRuntime().gc();
  }

  /**
   * Although "fragile" since it relies on a external source - its useful to
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
   * Wraps {@link java.lang.Runtime#freeMemory()}.
   *
   * @return the amount of free memory in the Java Virtual Machine. Calling the
   *         gc method may result in increasing the value returned by
   *         freeMemory.
   *
   *
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

          // all though this is appropriate it cannot be done
          // because you need runtime to correctly load/start/etc the plan
          // so it needs to be bootstrapped
          // load("runtime", "Runtime");

          // just create Runtime
          runtime = (Runtime) createService(RUNTIME_NAME, "Runtime", Platform.getLocalInstance().getId());
        }
        try {
          // a bit backwards - it loads after it been created
          // but its necessary because you need an runtime instance before you
          // load
          
          File cfgRoot = new File(CONFIG_ROOT);
          cfgRoot.mkdirs();
          if (startYml.enable) {
            Runtime.load("runtime", "Runtime");
          }
          ((RuntimeConfig) runtime.config).add("runtime");

          runtime.startService();
          // platform virtual is higher priority than service virtual
          Runtime.setAllVirtual(Platform.isVirtual());

          // setting the singleton security
          Security.getInstance();
          runtime.getRepo().addStatusPublisher(runtime);
          FileIO.extractResources();
          // protected services we don't want to remove when releasing a config
          runtime.startingServices.add("runtime");
          runtime.startingServices.add("security");
          runtime.startingServices.add("webgui");
          runtime.startingServices.add("python");

          runtime.startInteractiveMode();

          try {
            if (options.config != null) {
              Runtime.startConfig(options.config);
            } else if (startYml != null && startYml.config != null && startYml.enable) {
              Runtime.startConfig(startYml.config);
            }
          } catch (Exception e) {
            log.info("runtime will not be loading config");
          }

        } catch (Exception e) {
          log.error("runtime will not be loading config", e);
        }

      } // synchronized lock
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

  // What's the purpose of this? It doesn't return anything
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

  /**
   * Gets a Map between service names and the service object of all services
   * local to this MRL instance.
   * 
   * @return A Map between service names and service objects
   */
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
   * FIXME - return
   *
   * @return filtering/query requests
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
   * <p>
   * This will be part of the getHelloRequest - and the first listing from a
   * process of what services are available.
   * <p>
   * TODO - future work would be to supply a query to the getServiceList(query)
   * such that interfaces, types, or processes ids, can selectively be queried
   * out of it
   *
   * @return list of registrations
   */
  synchronized public List<Registration> getServiceList() {
    return registry.values().stream().map(si -> new Registration(si.getId(), si.getName(), si.getTypeKey())).collect(Collectors.toList());
  }

  // FIXME - scary function - returns private data
  public static Map<String, ServiceInterface> getRegistry() {
    return registry;// FIXME should return copy
  }

  /**
   * Gets a running service with the specified name. If the name is null or
   * there's no such service with the specified name, returns null instead.
   *
   * @param inName
   *          The name of the service
   * @return The service if it exists, or null
   */
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
   * @return all service names in an array form
   * 
   *
   */
  static public String[] getServiceNames() {
    List<ServiceInterface> si = getServices();
    String[] ret = new String[si.size()];
    for (int i = 0; i < ret.length; ++i) {
      ServiceInterface s = si.get(i);

      if (isLocal(s.getFullName())) {
        ret[i] = s.getName();
      } else {
        ret[i] = s.getFullName();
      }

      // ret[i] = s.getFullName();
    }
    return ret;
  }

  // Is it a good idea to modify all regex inputs? For example, if the pattern
  // already contains ".?" then the replacement will result in "..?"
  // If POSIX-style globs are desired there are different
  // pattern matching engines designed for that
  public static boolean match(String text, String pattern) {
    return text.matches(pattern.replace("?", ".?").replace("*", ".*?"));
  }

  public static List<String> getServiceNames(String pattern) {
    return getServices().stream().map(NameProvider::getName).filter(serviceName -> match(serviceName, pattern)).collect(Collectors.toList());
  }

  /**
   * @param interfaze
   *          the interface
   * @return a list of service names that implement the interface
   * @throws ClassNotFoundException
   *           if the class for the requested interface is not found.
   *
   */
  public static List<String> getServiceNamesFromInterface(String interfaze) throws ClassNotFoundException {
    if (!interfaze.contains(".")) {
      interfaze = "org.myrobotlab.service.interfaces." + interfaze;
    }

    return getServiceNamesFromInterface(Class.forName(interfaze));
  }

  /**
   * @param interfaze
   *          interface
   * @return list of service names
   * 
   */
  public static List<String> getServiceNamesFromInterface(Class<?> interfaze) {
    return getServicesFromInterface(interfaze).stream().map(ServiceInterface::getFullName).collect(Collectors.toList());
  }

  /**
   * Get all currently-running services
   *
   * @return A list of all currently-running services
   */
  public static List<ServiceInterface> getServices() {
    return getServices(null);
  }

  /**
   * Get all services that belong to an MRL instance with the given ID.
   * 
   * @param id
   *          The ID of the MRL instance
   * @return A list of the services that belong to the given MRL instance
   */
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
   * @param interfaze
   *          interface
   * @return results
   *
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

        Set<Class<?>> ancestry = new HashSet<>();
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
      error("could not find interfaces for %s - %s %s", interfaze, e.getClass().getSimpleName(), e.getMessage());
      log.error("getting class", e);
    }

    return results;
  }

  /**
   * return a list of services which are currently running and implement a
   * specific interface
   *
   * @param interfaze
   *          class
   * @return list of service interfaces
   *
   */
  // FIXME !!! - use single implementation that gets parents
  @Deprecated /*
               * no longer used or needed - change events are pushed no longer
               * pulled <-- Over complicated solution
               */
  public static synchronized List<ServiceInterface> getServicesFromInterface(Class<?> interfaze) {
    List<ServiceInterface> ret = new ArrayList<ServiceInterface>();

    for (String service : getServiceNames()) {
      Class<?> clazz = getService(service).getClass();
      while (clazz != null) {
        for (Class<?> inter : clazz.getInterfaces()) {
          if (inter.getName().equals(interfaze.getName())) {
            ret.add(getService(service));
            continue;
          }
        }
        clazz = clazz.getSuperclass();
      }
    }
    return ret;
  }

  /**
   * Because startYml is required to be a static variable, since it's needed
   * "before" a runtime instance exists it will be null in json serialization.
   * This method is needed so we can serialize the data appropriately.
   * 
   * @return
   */
  static public CmdConfig getStartYml() {
    return startYml;
  }

  /**
   * Gets the set of all threads currently running.
   * 
   * @return A set containing thread objects representing all running threads
   */
  static public Set<Thread> getThreads() {
    return Thread.getAllStackTraces().keySet();
  }

  /**
   * Wraps {@link java.lang.Runtime#totalMemory()}.
   *
   * @return The amount of memory available to the JVM in bytes.
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

  /**
   * Get how long this MRL instance has been running in human-readable String
   * form.
   *
   * @return The uptime of this instance.
   */
  public static String getUptime() {
    Date now = new Date();
    Platform platform = Platform.getLocalInstance();
    String uptime = getDiffTime(now.getTime() - platform.getStartTime().getTime());
    log.info("up for {}", uptime);
    return uptime;
  }

  public static String getPlatformInfo() {
    Platform platform = Platform.getLocalInstance();
    StringBuilder sb = new StringBuilder();
    sb.append(platform.getHostname());
    sb.append(" ");
    sb.append(platform.getOS());
    sb.append(" ");
    sb.append(platform.getArch());
    sb.append(".");
    sb.append(platform.getOsBitness());

    sb.append(" Java ");
    sb.append(platform.getVmVersion());
    sb.append(" ");
    sb.append(platform.getVMName());

    return sb.toString();
  }

  /**
   * Get a human-readable String form of a difference in time in milliseconds.
   *
   * @param diff
   *          The difference of time in milliseconds
   * @return The human-readable string form of the difference in time
   */
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
   * @return the version of the running platform instance
   *
   */
  public static String getVersion() {
    return Platform.getLocalInstance().getVersion();
  }

  /**
   * Get the latest version number of MRL in String form by querying the public
   * build server. If it cannot be contacted, this method returns the String
   * {@code "unknown"}.
   * 
   * @return The latest build version in String form
   */
  public static String getLatestVersion() {
    String latest = "http://build.myrobotlab.org:8080/job/myrobotlab/job/develop/lastSuccessfulBuild/buildNumber";
    byte[] b = Http.get(latest);
    String version = (b == null) ? "unknown" : "1.1." + new String(b);
    return version;
  }

  // FIXME - shouldn't this be in platform ???

  /**
   * Get the branch that this installation was built from.
   *
   * @return The branch
   * @see Platform#getBranch()
   */
  public static String getBranch() {
    return Platform.getLocalInstance().getBranch();
  }

  /**
   * Install all services
   *
   * @throws ParseException
   *           Unknown
   * @throws IOException
   *           Unknown
   */
  // TODO: Check throws list to see if these are still thrown
  static public void install() throws ParseException, IOException {
    install(null, null);
  }

  /**
   * Install specified service.
   *
   * @param serviceType
   *          Service to install
   */
  static public void install(String serviceType) {
    install(serviceType, null);
  }

  /**
   * Maximum complexity install - allows for blocking and non-blocking install.
   * During typically runtime install of services - non blocking is desired,
   * otherwise status info from the install is blocked until installation is
   * completed. For command line installation "blocking" mode would be desired
   *
   * FIXME - problematic in that Runtime.create calls this directly, and this
   * should be stepped through, because: If we need to install new components, a
   * restart is likely needed ... we don't do custom dynamic classloaders ....
   * yet
   *
   * License - should be appropriately accepted or rejected by user
   *
   * @param serviceType
   *          the service tyype to install
   * @param blocking
   *          if this should block until done.
   *
   */
  synchronized static public void install(String serviceType, Boolean blocking) {
    Runtime r = getInstance();

    if (blocking == null) {
      blocking = false;
    }

    installerThread = new Thread() {
      @Override
      public void run() {
        try {
          if (serviceType == null) {
            r.getRepo().install();
          } else {
            r.getRepo().install(serviceType);
          }
        } catch (Exception e) {
          r.error(e);
        }
      }
    };

    if (blocking) {
      installerThread.run();
    } else {
      installerThread.start();
    }

  }

  /**
   * Invoke a service method. The parameter must not be null and must have at
   * least 2 elements. The first is the service name and the second is the
   * service method. The rest of the elements are parameters to the specified
   * method.
   *
   * @param invoke
   *          The array of service name, method, and parameters
   */
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

  /**
   * Checks if a service is local to this MRL instance. The service must exist.
   *
   * @param serviceName
   *          The name of the service to check
   * @return Whether the specified service is local or not
   */
  public static boolean isLocal(String serviceName) {
    ServiceInterface sw = getService(serviceName);
    return Objects.equals(sw.getId(), Platform.getLocalInstance().getId());
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
   * Start interactive mode on {@link System#in} and {@link System#out}.
   *
   * @see #startInteractiveMode(InputStream, OutputStream)
   */
  public void startInteractiveMode() {
    startInteractiveMode(System.in, System.out);
  }

  /**
   * Starts an interactive CLI on the specified input and output streams. The
   * CLI command processor runs in its own thread and takes commands according
   * to the CLI API.
   * 
   * FIXME - have another shell script which starts jar as ws client with cli
   * interface Remove this std in/out - it is overly complex and different OSs
   * handle it differently Windows Java updates have broken it several times
   *
   * @param in
   *          The input stream to take commands from
   * @param out
   *          The output stream to print command output to
   * @return The constructed CLI processor
   */
  public InProcessCli startInteractiveMode(InputStream in, OutputStream out) {
    if (cli != null) {
      log.info("already in interactive mode");
      return cli;
    }

    cli = new InProcessCli(this, "runtime", in, out);
    Connection c = cli.getConnection();
    stdCliUuid = (String) c.get("uuid");

    // addRoute(".*", getName(), 100);
    addConnection(stdCliUuid, cli.getId(), c);

    return cli;
  }

  /**
   * Stops interactive mode if it's running.
   */
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

  /**
   * Logs a string message and publishes the message.
   *
   * @param msg
   *          The message to log and publish
   * @return msg
   */
  public static String message(String msg) {
    getInstance().invoke("publishMessage", msg);
    log.info(msg);
    return msg;
  }

  /**
   * Listener for state publishing, updates registry
   * 
   * @param updatedService
   *          Updated service to put in the registry
   */
  public void onState(ServiceInterface updatedService) {
    log.info("runtime updating registry info for remote service {}", updatedService.getName());
    registry.put(String.format("%s@%s", updatedService.getName(), updatedService.getId()), updatedService);
  }

  public static synchronized Registration register(String id, String name, String typeKey, ArrayList<String> interfaces) {
    Registration proxy = new Registration(id, name, typeKey, interfaces);
    register(proxy);
    return proxy;
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
   *          registration
   * @return registration
   *
   */
  public static synchronized Registration register(Registration registration) {

    try {

      // TODO - have rules on what registrations to accept - dependent on
      // security, desire, re-broadcasting configuration etc.

      String fullname = String.format("%s@%s", registration.getName(), registration.getId());
      if (registry.containsKey(fullname)) {
        log.info("{} already registered", fullname);
        return registration;
      }

      if (!ForeignProcessUtils.isValidTypeKey(registration.getTypeKey())) {
        log.error("Invalid type key being registered: " + registration.getTypeKey());
        return null;
      }

      log.info("{}@{} registering at {} of type {}", registration.getName(), registration.getId(), Platform.getLocalInstance().getId(), registration.getTypeKey());

      if (!registration.isLocal(Platform.getLocalInstance().getId())) {

        // Check if we're registering a java service
        if (ForeignProcessUtils.isValidJavaClassName(registration.getTypeKey())) {

          String fullTypeName;
          if (registration.getTypeKey().contains(".")) {
            fullTypeName = registration.getTypeKey();
          } else {
            fullTypeName = String.format("org.myrobotlab.service.%s", registration.getTypeKey());
          }

          try {
            // de-serialize, class exists
            registration.service = Runtime.createService(registration.getName(), fullTypeName, registration.getId());
            if (registration.getState() != null) {
              copyShallowFrom(registration.service, CodecUtils.fromJson(registration.getState(), Class.forName(fullTypeName)));
            }
          } catch (ClassNotFoundException classNotFoundException) {
            log.error(String.format("Unknown service class for %s@%s: %s", registration.getName(), registration.getId(), registration.getTypeKey()), classNotFoundException);
            return null;
          }
        } else {
          // We're registering a foreign process service. We don't need to check
          // ForeignProcessUtils.isForeignTypeKey() because the type key is
          // valid
          // but is not a java class name

          // Class does not exist, check if registration has empty interfaces
          // Interfaces should always include ServiceInterface if coming from
          // remote client
          if (registration.interfaces == null || registration.interfaces.isEmpty()) {
            log.error("Unknown service type being registered, registration does not contain any " + "interfaces for proxy generation: " + registration.getTypeKey());
            return null;
          }

          // Class<?>[] interfaces = registration.interfaces.stream().map(i -> {
          // try {
          // return Class.forName(i);
          // } catch (ClassNotFoundException e) {
          // throw new RuntimeException("Unable to load interface " + i + "
          // defined in remote registration " + registration, e);
          // }
          // }).toArray(Class<?>[]::new);

          // registration.service = (ServiceInterface)
          // Proxy.newProxyInstance(Runtime.class.getClassLoader(), interfaces,
          // new ProxyServiceInvocationHandler(registration.getName(),
          // registration.getId()));
          registration.service = ProxyFactory.createProxyService(registration);
          log.info("Created proxy: " + registration.service);
        }
      }

      registry.put(fullname, registration.service);

      if (runtime != null) {

        String type = registration.getTypeKey();

        // If type does not exist in typeToNames, make it an empty hash set and
        // return it
        Set<String> names = runtime.typeToNames.computeIfAbsent(type, k -> new HashSet<>());
        names.add(fullname);

        // FIXME - most of this could be static as it represents meta data of
        // class and interfaces

        // FIXME - was false - setting now to true .. because
        // 1 edge case - "can something fulfill my need of an interface - is not
        // currently
        // switching to true
        boolean updatedServiceLists = false;

        // maintaining interface type relations
        // see if this service type is new
        // PROCESS INDEXES ! - FIXME - will need this in unregister
        // ALL CLASS/TYPE PROCESSING only needs to happen once per type
        if (!runtime.serviceTypes.contains(type)) {
          // CHECK IF "CAN FULFILL"
          // add the interfaces of the new service type
          Set<String> interfaces = ClassUtil.getInterfaces(registration.service.getClass(), FILTERED_INTERFACES);
          for (String interfaze : interfaces) {
            Set<String> types = runtime.interfaceToType.get(interfaze);
            if (types == null) {
              types = new HashSet<>();
            }
            types.add(registration.getTypeKey());
            runtime.interfaceToType.put(interfaze, types);
          }

          runtime.typeToInterface.put(type, interfaces);
          runtime.serviceTypes.add(registration.getTypeKey());
          updatedServiceLists = true;
        }

        // check to see if any of our interfaces can fulfill requested ones
        Set<String> myInterfaces = runtime.typeToInterface.get(type);
        for (String inter : myInterfaces) {
          if (runtime.interfaceToNames.containsKey(inter)) {
            runtime.interfaceToNames.get(inter).add(fullname);
            updatedServiceLists = true;
          }
        }

        if (updatedServiceLists) {
          runtime.invoke("publishInterfaceToNames");
        }

        // TODO - determine rules on re-broadcasting based on configuration
        runtime.invoke("registered", registration);
      }

      // TODO - remove ? already get state from registration
      if (!registration.isLocal(Platform.getLocalInstance().getId())) {
        runtime.subscribe(registration.getFullName(), "publishState");
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
   *          name to release
   * @return true/false
   *
   */
  public synchronized static boolean releaseService(String inName) {
    if (inName == null) {
      log.debug("release (null)");
      return false;
    }

    String name = getFullName(inName);

    log.info("releasing service {}", name);

    if (!registry.containsKey(name)) {
      log.info("{} not registered", name);
      return false;
    }

    // get reference from registry
    ServiceInterface si = registry.get(name);
    if (si == null) {
      log.warn("cannot release {} - not in registry");
      return false;
    }

    // FIXME - TODO invoke and or blocking on preRelease - Future

    // send msg to service to self terminate
    if (si.isLocal()) {
      si.purgeTasks();
      si.stopService();
      Plan plan = Runtime.getPlan();
      ServiceConfig sc = plan.get(inName);
      if (sc == null) {
        log.debug("service config not available for {}", inName);
      } else {
        sc.state = "STOPPED";
      }
    } else {
      if (runtime != null) {
        runtime.send(name, "releaseService");
      }
    }
    // FOR remote this isn't correct - it should wait for
    // a message from the other runtime to say that its released
    unregister(name);
    Plan plan = Runtime.getPlan();
    ServiceConfig sc = plan.get(inName);

    if (sc != null) {
      sc.state = "RELEASED";
      // FIXME - TODO RELEASE PEERS ! which is any inName.* !!!

      // iterate through peers
      // if (sc.autoStartPeers) {
      // // get peers from meta data
      // MetaData md = MetaData.get(sc.type);
      // Map<String, ServiceReservation> peers = md.getPeers();
      // log.info("auto start peers and {} of type {} has {} peers", inName,
      // sc.type, peers.size());
      // // RECURSE ! - if we found peers and autoStartPeers is true - we start
      // // all
      // // the children up
      // for (String peer : peers.keySet()) {
      // // get actual Name
      // String actualPeerName = getPeerName(peer, sc, peers, inName);
      // if (actualPeerName != null && isStarted(actualPeerName) &&
      // si.autoStartedPeersContains(actualPeerName)) {
      // release(actualPeerName);
      // }
      // }
      // }
    }

    return true;
  }

  /**
   * Removes registration for a service. Removes the service from
   * {@link #typeToInterface} and {@link #interfaceToNames}.
   * 
   * @param inName
   *          Name of the service to unregister
   */
  synchronized public static void unregister(String inName) {
    String name = getFullName(inName);
    log.info("unregister {}", name);

    // get reference from registry
    ServiceInterface sw = registry.get(name);
    if (sw == null) {
      log.debug("{} already unregistered", name);
      return;
    }

    // you have to send released before removing from registry
    if (runtime != null) {
      runtime.invoke("released", inName); // <- DO NOT CHANGE THIS IS CORRECT
      // !!
      // it should be FULLNAME !
      // runtime.broadcast("released", inName);
      String type = sw.getTypeKey();

      boolean updatedServiceLists = false;

      // check to see if any of our interfaces can fullfill requested ones
      Set<String> myInterfaces = runtime.typeToInterface.get(type);
      if (myInterfaces != null) {
        for (String inter : myInterfaces) {
          if (runtime.interfaceToNames.containsKey(inter)) {
            runtime.interfaceToNames.get(inter).remove(name);
            updatedServiceLists = true;
          }
        }
      }

      if (updatedServiceLists) {
        runtime.invoke("publishInterfaceToNames");
      }

    }

    // FIXME - release autostarted peers ?

    // last step - remove from registry
    registry.remove(name);
    // and config
    RuntimeConfig c = (RuntimeConfig) Runtime.getInstance().config;
    c.registry.remove(CodecUtils.getShortName(name));

    log.info("released {}", name);
  }

  /**
   * Get all remote services.
   * 
   * @return List of remote services as proxies
   */
  public List<ServiceInterface> getRemoteServices() {
    return getRemoteServices(null);
  }

  /**
   * Get remote services associated with the MRL instance that has the given ID.
   * 
   * @param id
   *          The id of the target MRL instance
   * @return A list of services running on the target instance
   */
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
   * Releases all local services including Runtime asynchronously.
   *
   * @see #releaseAll(boolean, boolean)
   */
  public static void releaseAll() {
    releaseAll(true, false);
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
   *
   * local only? YES !!! LOCAL ONLY !!
   * 
   * @param releaseRuntime
   *          Whether the Runtime should also be released
   */
  public static void releaseAll(boolean releaseRuntime, boolean block) {
    // a command thread is issuing this command is most likely
    // tied to one of the services being removed
    // therefore this needs to happen asynchronously otherwise
    // the thread that issued the command will try to destroy/release itself
    // which almost always causes a deadlock
    log.debug("releaseAll");

    if (block) {
      processRelease(releaseRuntime);
    } else {

      new Thread() {
        @Override
        public void run() {
          processRelease(releaseRuntime);
        }
      }.start();

    }
  }

  /**
   * Releases all threads and can be executed in a separate thread.
   *
   * @param releaseRuntime
   *          Whether the Runtime should also be released
   */
  static private void processRelease(boolean releaseRuntime) {

    // reverse release to order of creation
    Collection<ServiceInterface> local = getLocalServices().values();
    List<ServiceInterface> ordered = new ArrayList<>(local);
    ordered.removeIf(Objects::isNull);
    Collections.sort(ordered);
    Collections.reverse(ordered);

    for (ServiceInterface sw : ordered) {

      // no longer needed now - runtime "should be" guaranteed to be last
      if (sw == Runtime.getInstance()) {
        // skipping runtime
        continue;
      }

      log.info("releasing service {}", sw.getName());

      try {
        sw.releaseService();
      } catch (Exception e) {
        runtime.error("%s threw while releasing", e);
        log.error("release", e);
      }
    }

    // clean up remote ... the contract should
    // probably be just remove their references - do not
    // ask for them to be released remotely ..
    for (String remoteService : registry.keySet()) {
      if (!remoteService.equals(runtime.getFullName())) {
        registry.remove(remoteService);
      }
    }

    if (runtime != null && releaseRuntime) {
      runtime.releaseService();
      runtime = null;
    }
  }

  /**
   * Shuts down this instance after the given number of seconds.
   *
   * @param seconds
   *          sets task to shutdown in (n) seconds
   */
  // Why is this using the wrapper type? Null can be passed in and cause NPE
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
      log.info("myrobotlab shutting down");

      if (runtime != null) {
        log.info("stopping interactive mode");
        runtime.stopInteractiveMode();
      }

      log.info("pre shutdown on all services");
      for (ServiceInterface service : getServices()) {
        service.preShutdown();
      }

      log.info("releasing all");

      // release
      releaseAll();
    } catch (Exception e) {
      log.error("something threw - continuing to shutdown", e);
    }

    // calling System.exit(0) before some specialized threads
    // are completed will actually end up in a deadlock
    Service.sleep(1000);
    System.exit(0);
  }

  public Integer publishShutdown(Integer seconds) {
    return seconds;
  }

  /**
   * publish the folders of the parent directory of configPath if the configPath
   * is null then publish directory names of data/config
   *
   * @return list of configs
   */
  public List<String> publishConfigList() {
    configList = new ArrayList<>();

    File configDirFile = new File(CONFIG_ROOT);
    if (!configDirFile.exists() || !configDirFile.isDirectory()) {
      error("%s config root does not exist", configDirFile.getAbsolutePath());
      return configList;
    }

    File[] files = configDirFile.listFiles();
    if (files == null) {
      // We checked for if directory earlier, so can only be null for IO error
      error("IO error occurred while listing config directory files");
      return configList;
    }
    for (File file : files) {
      String n = file.getName();

      if (!file.isDirectory() || file.isHidden()) {
        log.info("ignoring {} expecting directory not file", n);
        continue;
      }

      configList.add(file.getName());
    }
    Collections.sort(configList);
    return configList;
  }

  /**
   * Releases all local services except the services whose names are in the
   * given set
   * 
   * @param saveMe
   *          The set of services that should not be released
   */
  public static void releaseAllServicesExcept(HashSet<String> saveMe) {
    log.info("releaseAllServicesExcept");
    List<ServiceInterface> list = Runtime.getServices();
    for (ServiceInterface si : list) {
      if (saveMe != null && saveMe.contains(si.getName())) {
        log.info("leaving {}", si.getName());
      } else {
        si.releaseService();
      }
    }
  }

  /**
   * Release a specific service. Releasing shuts down the service and removes it
   * from registries.
   *
   * @param fullName
   *          full name The service to be released
   *
   */
  static public void release(String fullName) {
    if (fullName == null) {
      log.error("release(null)");
      return;
    }

    ServiceInterface si = Runtime.getService(fullName);
    if (si == null) {
      log.info("{} already released", fullName);
      return;
    }
    String shortName = si.getName();

    // check for peers !! check in config or check in Plan ?!?!?
    Map<String, Peer> peers = si.getPeers();
    if (peers != null) {
      for (String peerKey : peers.keySet()) {
        Peer peer = peers.get(peerKey);
        if (peer.autoStart) {
          release(peer.name);
        }
      }
    }

    // important to call service.releaseService because
    // many are derived that take care of additional thread
    // cleanup
    Plan plan = Runtime.getPlan();
    plan.remove(shortName);
    si.releaseService();
  }

  /**
   * Disconnect from remote process. FIXME - not implemented
   * 
   * @throws IOException
   *           Unknown
   */
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
   *          instance id.
   * @return string
   *
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

  /**
   * Reconnects {@link #cli} to this process.
   * 
   * @return The id of this instance
   */
  // FIXME - remove ?!?!!?
  public String exit() {
    Connection c = getConnection(stdCliUuid);
    if (c != null && c.get("cli") != null) {
      ((InProcessCli) c.get("cli")).setRemote(getId());
    }
    return getId();
  }

  /**
   * Send a command to the {@link InProcessCli}.
   *
   * @param srcFullName
   *          Unknown
   * @param cmd
   *          The command to execute
   */
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

  /**
   * Connect to the MRL instance at the given URL, auto-reconnecting if
   * specified and the connection drops.
   *
   * FIXME implement autoReconnect
   *
   * @param url
   *          The URL to connect to
   * @param autoReconnect
   *          Whether the connection should be re-established if it is dropped
   */
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

  /**
   * Connect to the MRL instance at the given URL
   * 
   * @param url
   *          Where the MRL instance being connected to is located
   */
  @Override
  public void connect(String url) {
    try {

      // TODO - do auth, ssl and unit tests for them
      // TODO - get session id
      // request default describe - on describe do registrations .. zzz

      // standardize request - TODO check for ws wss not http https
      if (!url.contains("api/messages")) {
        url += "/api/messages";
      }

      if (!url.contains("id=")) {
        url += "?id=" + getId();
      }

      WsClient client2 = new WsClient();
      client2.connect(this, url);

      // URI uri = new URI(url);
      // adding "id" as full url :P ... because we don't know it !!!
      Connection connection = new Connection(client2.getId(), getId(), getFullName());

      // connection specific
      connection.put("c-type", "Runtime");
      // attributes.put("c-endpoint", endpoint);
      connection.put("c-client", client2);

      // cli specific
      connection.put("cwd", "/");
      connection.put("url", url);
      connection.put("uri", url); // not really correct
      connection.put("user", "root");
      connection.put("host", "local");

      // addendum
      connection.put("User-Agent", "runtime-client");

      addConnection(client2.getId(), url, connection);

      // direct send - may not have and "id" so it will be too runtime vs
      // runtime@{id}
      // subscribe to "describe"
      MRLListener listener = new MRLListener("describe", getFullName(), "onDescribe");
      Message msg = Message.createMessage(getFullName(), "runtime", "addListener", listener);      
      client2.send(CodecUtils.toJsonMsg(msg));

      // send describe
      client2.send(CodecUtils.toJsonMsg(getDescribeMsg(null)));

    } catch (Exception e) {
      log.error("connect to {} giving up {}", url, e.getMessage());
    }
  }

  /**
   * FIXME - this is a gateway callback - probably should be in the gateway
   * interface - this is a "specific" gateway that supports typeless json or
   * websockets
   * <p>
   * FIXME - decoding should be done at the Connection ! - this should be
   * onRemoteMessage(msg) !
   * <p>
   * callback - from clientRemote - all client connections will recieve here
   * TODO - get clients directional api - an api per direction incoming and
   * outgoing
   *
   * @param uuid
   *          - connection for incoming data
   * @param data
   *          Incoming message in JSON String form
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

  /**
   * Add a route to the route table
   *
   * @param remoteId
   *          Id of the remote instance
   * @param uuid
   *          Unknown
   * @param metric
   *          Unknown
   * @see RouteTable#addRoute(String, String, int)
   */
  public void addRoute(String remoteId, String uuid, int metric) {
    routeTable.addRoute(remoteId, uuid, metric);
  }

  /**
   * Start Runtime with the specified config
   *
   * @param configName
   *          The name of the config file
   * @return The Runtime singleton
   */
  static public void startConfig(String configName) {
    setConfig(configName);
    Runtime runtime = Runtime.getInstance();
    runtime.processingConfig = true; // multiple inbox threads not available
    runtime.invoke("publishStartConfig", configName);
    RuntimeConfig rtConfig = (RuntimeConfig) runtime.readServiceConfig(runtime.getConfigName(), "runtime");
    if (rtConfig == null) {
      runtime.error("cannot find %s%s%s", runtime.getConfigName(), fs, "runtime.yml");
      return;
    }

    runtime.apply(rtConfig);

    // for every service listed in runtime registry - load it
    // FIXME - regex match on filesystem matches on *.yml
    for (String service : rtConfig.getRegistry()) {
      if ("runtime".equals(service) || Runtime.isStarted(service)) {
        continue;
      }

      // has to be loaded
      File file = new File(Runtime.CONFIG_ROOT + fs + runtime.getConfigName() + fs + service + ".yml");
      if (!file.exists()) {
        runtime.error("cannot read file %s - skipping", file.getPath());
        continue;
      }

      ServiceConfig sc = runtime.readServiceConfig(runtime.getConfigName(), service);
      try {
        if (sc == null) {
          continue;
        }
        runtime.loadService(Runtime.getPlan(), service, sc.type, true, 0);
      } catch (Exception e) {
        runtime.error(e);
      }
    }

    // for all newly created services start them
    Map<String, ServiceInterface> created = Runtime.createServicesFromPlan(Runtime.getPlan(), null, null);
    for (ServiceInterface si : created.values()) {
      si.startService();
    }

    runtime.processingConfig = false; // multiple inbox threads not available
    runtime.invoke("publishFinishedConfig", configName);

  }

  public String publishStartConfig(String configName) {
    log.info("publishStartConfig {}", configName);
    // Make Note: done inline, because the thread actually doing the config
    // processing
    // would need to be finished with it before this thread could be invoked
    // if multiple inbox threads were available then this would be possible
    // processingConfig = true;
    return configName;
  }

  public String publishFinishedConfig(String configName) {
    log.info("publishFinishedConfig {}", configName);
    // Make Note: done inline, because the thread actually doing the config
    // processing
    // would need to be finished with it before this thread could be invoked
    // if multiple inbox threads were available then this would be possible
    // processingConfig = false;
    return configName;
  }

  /**
   * Start a service of the specified type as the specified name.
   *
   * @param name
   *          The name of the new service
   * @param type
   *          The type of the new service
   * @return The started service
   */
  synchronized static public ServiceInterface start(String name, String type) {
    try {


      ServiceInterface requestedService = Runtime.getService(name);
      if (requestedService != null) {
        log.info("requested service already exists");
        if (requestedService.isRunning()) {
          log.info("requested service already running");
        } else {
          requestedService.startService();
        }
        return requestedService;
      }

      Runtime.load(name, type);
      // FIXME - does some order need to be maintained
      Map<String, ServiceInterface> services = createServicesFromPlan(Runtime.getPlan(), null, name);

      if (services == null) {
        Runtime.getInstance().error("cannot create instance of %s with type %s given current configuration", name, type);
        return null;
      }

      requestedService = Runtime.getService(name);

      // FIXME - does some order need to be maintained e.g. all children before
      // parent
      // breadth first, depth first, external order ordinal ?
      for (ServiceInterface service : services.values()) {
        if (service.getName().equals(name)) {
          continue;
        }
        if (!Runtime.isStarted(service.getName())) {
          service.startService();
        }
      }

      if (requestedService == null) {
        Runtime.getInstance().error("could not start %s of type %s", name, type);
        return null;
      }

      // getConfig() was problematic here for JMonkeyEngine
      ServiceConfig sc = requestedService.getConfig();
      // Map<String, Peer> peers = sc.getPeers();
      // if (peers != null) {
      // for (String p : peers.keySet()) {
      // Peer peer = peers.get(p);
      // log.info("peer {}", peer);
      // }
      // }
      // recursive - start peers of peers of peers ...
      Map<String, Peer> subPeers = sc.getPeers();
      if (sc != null && subPeers != null) {
        for (String subPeerKey : subPeers.keySet()) {
          // IF AUTOSTART !!!
          Peer subPeer = subPeers.get(subPeerKey);
          if (subPeer.autoStart) {
            Runtime.start(sc.getPeerName(subPeerKey), subPeer.type);
          }
        }
      }

      requestedService.startService();
      return requestedService;
    } catch (Exception e) {
      runtime.error(e);
    }
    return null;
  }

  /**
   * sing parameter name info supplied - potentially all information regarding
   * this service coulde be found in on the filesystem or in the plan
   * 
   * @param name
   * @return
   */
  synchronized static public ServiceInterface start(String name) {
    Runtime.load(name, null);
    Map<String, ServiceInterface> services = createServicesFromPlan(Runtime.getPlan(), null, name);
    // FIXME - order ?
    for (ServiceInterface service : services.values()) {
      service.startService();
    }
    return Runtime.getService(name);
  }

  synchronized public static Plan load(String name, String type) {
    try {
      Runtime runtime = Runtime.getInstance();
      return runtime.loadService(Runtime.getPlan(), name, type, true, 0);
    } catch (IOException e) {
      runtime.error(e);
    }
    return null;
  }

  /**
   * Construct a new Runtime with the given name and ID. The name should always
   * be "runtime" as parts of interprocess communication assume it to be so.
   *
   * TODO Check if there's a way to remove the assumptions about Runtime's name
   * 
   * @param n
   *          Name of the runtime. Should always be {@code "runtime"}
   * @param id
   *          The ID of the instance this runtime belongs to.
   */
  public Runtime(String n, String id) {
    super(n, id);

    // because you need to start with something ...
    config = new RuntimeConfig();

    synchronized (INSTANCE_LOCK) {
      if (runtime == null) {
        // fist and only time....
        runtime = this;
        repo = (IvyWrapper) Repo.getInstance(LIBRARIES, "IvyWrapper");

        // resolve serviceData MetaTypes for the repo

        for (MetaData metaData : serviceData.getServiceTypes()) {
          if (metaData.getSimpleName().equals("OpenCV")) {
            log.warn("here");
          }
          Set<ServiceDependency> deps = repo.getUnfulfilledDependencies(metaData.getType());
          if (deps.size() == 0) {
            metaData.installed = true;
          } else {
            log.warn("{} not installed", metaData.getSimpleName());
          }
        }

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

    // initialize the config list
    publishConfigList();

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
  }

  /**
   * Get the process ID of the current JVM.
   *
   * @return The process ID.
   * @see Platform#getPid()
   */
  public String getPid() {
    return Platform.getLocalInstance().getPid();
  }

  public String publishDefaultRoute(String defaultRoute) {
    return defaultRoute;
  }

  /**
   * Get the hostname of the computer this instance is running on.
   * 
   * @return The computer's hostname
   * @see Platform#getHostname()
   */
  public String getHostname() {
    return Platform.getLocalInstance().getHostname();
  }

  /**
   * publishing event - since checkForUpdates may take a while
   */
  public void checkingForUpdates() {
    log.info("checking for updates");
  }

  /**
   * Read an entire input stream as a string and return it. If the input stream
   * does not have any more tokens, returns an empty string instead.
   *
   * @param is
   *          The input stream to read from
   * @return The entire input stream read as a string
   */
  static public String getInputAsString(InputStream is) {
    try (java.util.Scanner s = new java.util.Scanner(is)) {
      return s.useDelimiter("\\A").hasNext() ? s.next() : "";
    }
  }

  /**
   * list the contents of the current working directory
   *
   * @return object
   */
  public Object ls() {
    return ls(null, null);
  }

  /**
   * List the contents of an absolute path.
   *
   * @param path
   *          The path to list
   * @return The contents of the directory
   */
  public Object ls(String path) {
    return ls(null, path);
  }

  /**
   * list the contents of a specific path
   * <p>
   * </p>
   * TODO It looks like this only returns Object because it wants to return
   * either a String array or a method entry list. It would probably be best to
   * just convert the method entry list to a string array using streams and
   * change the signature to match.
   *
   * @param contextPath
   *          c
   * @param path
   *          p
   * @return object
   *
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
      List<MethodEntry> me = cache.query(si.getTypeKey(), parts[2]);
      return me; // si.getMethodMap().get(parts[2]);
    }
    return ret;
  }

  /**
   * serviceName at id
   *
   * @return runtime name with instance id.
   *
   */
  public String whoami() {
    return "runtime@" + getId();
  }

  // end cli commands ----

  // ---------- Java Runtime wrapper functions begin --------
  /**
   * Executes the specified command and arguments in a separate process. Returns
   * the exit value for the subprocess.
   *
   * @param program
   *          The name of or path to an executable program. If given a name, the
   *          program must be on the system PATH.
   * @return The exit value of the subprocess
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
   * The data originates from the repo's serviceData.json file.
   * <p>
   * There is a local one distributed with the installation jar. When an
   * "update" is forced, MRL will try to download the latest copy from the repo.
   * <p>
   * The serviceData.json lists all service types, dependencies, categories and
   * other relevant information regarding service creation
   *
   * @return list of all service type names
   */
  public String[] getServiceTypeNames() {
    return getServiceTypeNames("all");
  }

  /**
   * getServiceTypeNames will publish service names based on some filter
   * criteria
   *
   * @param filter
   *          f
   * @return array of service types
   *
   */
  public String[] getServiceTypeNames(String filter) {
    return serviceData.getServiceTypeNames(filter);
  }

  // FIXME THIS IS NOT NORMALIZED !!!

  /**
   * Send the full log of the currently running MRL instance to the MyRobotLab
   * developers for help. The userID is the name of the MyRobotLab.org user
   * account
   * 
   * @param userId
   *          Name of the MRL website account to link the log to
   * @return Whether the log was sent successfully, info if yes and error if no.
   */
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

  // FIXME - create interface for this
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
   */
  @Override
  public Registration registered(Registration registration) {
    return registration;
  }

  /**
   * released event - when a service is successfully released from the registry
   * this event is triggered
   *
   */
  @Override
  public String released(String name) {
    return name;
  }

  /**
   * A function for runtime to "save" a service - or if the service does not
   * exists save the "default" config of that type of service
   *
   * @param name
   *          name of service to export
   * @return true/false
   * @throws IOException
   *           boom
   *
   */
  @Deprecated /* use save(name) */
  public boolean export(String name /* , String type */) throws IOException {
    return save(name);
  }

  public boolean save(String name /* , String type */) throws IOException {
    ServiceInterface si = getService(name);
    if (si != null) {
      return si.save();
    }
    error("cannot save %s - does not exist", name);
    return false;
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
      @Override
      public void run() {
        try {

          info("restarting");

          // FIXME - should we save() load() ???
          // export("last-restart");

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

          // make sure python is included
          // options.services.add("python");
          // options.services.add("Python");

          // force invoke
          // options.invoke = new String[] { "python", "execFile",
          // "lastRestart.py" };

          // create builder from Launcher daemonize ?
          log.info("re launching with commands \n{}", CmdOptions.toString(options.getOutputCmd()));
          ProcessBuilder pb = Launcher.createBuilder(options);

          // fire it off
          Process restarted = pb.start();
          // it "better" not be a requirement that a process must consume its
          // std streams
          // "hopefully" - if the OS realizes the process is dead it moves the
          // streams to /dev/null ?
          // StreamGobbler gobbler = new
          // StreamGobbler(String.format("%s-gobbler", getName()),
          // restarted.getInputStream());
          // gobbler.start();

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

  /**
   * Get the META-INF/MANIFEST.MF file from the myrobotlab.jar as String
   * key-value pairs.
   * 
   * @return key-value pairs contained in the manifest file
   * @see Platform#getManifest()
   */
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

  /**
   * Get the log level of this MRL instance
   *
   * @return The log level as a String.
   * @see Logging#getLevel()
   */
  static public String getLogLevel() {
    Logging logging = LoggingFactory.getInstance();
    return logging.getLevel();
  }

  /**
   * Set the file to output logs to. This will remove all previously-applied
   * appenders from the logging system.
   *
   * @param file
   *          The file to output logs to
   * @return file
   * @see Logging#removeAllAppenders()
   */
  static public String setLogFile(String file) {
    log.info("setLogFile {}", file);
    Logging logging = LoggingFactory.getInstance();
    logging.removeAllAppenders();
    LoggingFactory.setLogFile(file);
    logging.addAppender(AppenderType.FILE);
    return file;
  }

  /**
   * Disables logging by removing all appenders. To re-enable call
   * {@link #setLogFile(String)} or add appenders.
   *
   * @see Logging#addAppender(String)
   */
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
  public void releaseService() {
    super.releaseService();
    if (runtime != null) {
      runtime.stopInteractiveMode();

      runtime.getRepo().removeStatusPublishers();
    }
    runtime = null;
  }

  /**
   * Close all connections using this runtime as the gateway. This includes both
   * inbound and outbound connections.
   */
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

  /**
   * Clear all services' last error.
   * 
   * @see ServiceInterface#clearLastError()
   */
  public void clearErrors() {
    for (String serviceName : registry.keySet()) {
      send(serviceName, "clearLastError");
    }
  }

  /**
   * Check if any services have errors.
   *
   * @return Whether any service has an error
   * @see ServiceInterface#hasError()
   */
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

  /**
   * Get recent errors from all local services.
   * 
   * @return A list of most recent service errors
   * @see ServiceInterface#getLastError()
   */
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

  /**
   * Broadcast the states of all local services.
   */
  public static void broadcastStates() {
    for (ServiceInterface si : getLocalServices().values()) {
      si.broadcastState();
    }
  }

  /**
   * Get the Runtime singleton instance.
   * 
   * @return The singleton instance
   * @see #getInstance()
   */
  public static Runtime get() {
    return Runtime.getInstance();
  }

  /**
   * Execute an external program with arguments if specified. args must not be
   * null and the length must be greater than zero, the first element is the
   * program to be executed. If the program is just a name and not a path to the
   * executable then it must be on the operating system PATH.
   *
   * @see <a href=
   *      "https://superuser.com/questions/284342/what-are-path-and-other-environment-variables-and-how-can-i-set-or-use-them">
   *      What are PATH and other environment variables?</a>
   * @param args
   *          The program to be executed as the first element and the args to
   *          the program as the rest, if any
   * @return The program's stdout and stderr output
   */
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

  /**
   * Execute an external program with a list of arguments, a specified working
   * directory, any additional environment variables, and whether the execution
   * blocks.
   *
   * TODO Implement workingDir and block
   *
   * @param program
   *          The program to be executed
   * @param args
   *          Any arguments to the command
   * @param workingDir
   *          The directory to execute the program in
   * @param additionalEnv
   *          Any additional environment variables
   * @param block
   *          Whether this method blocks for the program to execute
   * @return The programs stderr and stdout output
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
      byte[] buff = new byte[32768];

      // TODO: should we read both of these streams?
      // if we break out of the first loop is the process terminated?

      // read stdout
      for (int n; (n = stdOut.read(buff)) != -1;) {
        outputBuilder.append(new String(buff, 0, n));
      }

      // read stderr
      for (int n; (n = stdErr.read(buff)) != -1;) {
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
      // TODO replace with logging calls
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

  /**
   * Get the current battery level of the computer this MRL instance is running
   * on.
   *
   * @return The battery level as a double from 0.0 to 100.0, expressed as a
   *         percentage.
   */
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
        // TODO This is incorrect, will not work when unplugged
        // and acpitool output is different than expected,
        // at least on Ubuntu 22.04
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

  /**
   * Get the local service data instance.
   * 
   * @return The local service data
   * @see ServiceData#getLocalInstance()
   */
  public ServiceData getServiceData() {
    return serviceData;
  }

  /**
   * Return supported system languages
   *
   * @return map of languages to locales
   */
  public Map<String, Locale> getLanguages() {
    return Locale.getAvailableLanguages();
  }

  /**
   * Get a map between locale IDs and the associated {@link Locale} instance.
   *
   * @return A map between IDs and instances.
   */
  @Override
  public Map<String, Locale> getLocales() {
    return locales;
  }

  /**
   * Set the locales by passing a list of locale IDs.
   *
   * @param codes
   *          A list of locale IDs
   * @return A map between the IDs and the Locale instances.
   */
  public Map<String, Locale> setLocales(String... codes) {
    locales = Locale.getLocaleMap(codes);
    return locales;
  }

  /**
   * @return get the Security singleton
   *
   *
   */
  static public Security getSecurity() {
    return Security.getInstance();
  }

  /**
   * Execute a program with arguments, if any. Wraps
   * {@link java.lang.Runtime#exec(String[])}.
   *
   * @param cmd
   *          A list with the program name as the first element and any
   *          arguments as the subsequent elements.
   * @return The Process spawned by the execution
   * @throws IOException
   *           if an I/O error occurs while spawning the process
   */
  public static Process exec(String... cmd) throws IOException {
    // FIXME - can't return a process - it will explode in serialization
    // but we might want to keep it and put it on a transient map
    log.info("Runtime exec {}", Arrays.toString(cmd));
    Process p = java.lang.Runtime.getRuntime().exec(cmd);
    return p;
  }

  /**
   * Re-execute {@link Runtime#main(String[])} with the specified arguments and
   * then return the Runtime singleton instance.
   *
   * @param args2
   *          An array of the arguments to be passed to main()
   * @return The Runtime singleton
   */
  public static Runtime getInstance(String[] args2) {
    Runtime.main(args2);
    return Runtime.getInstance();
  }

  /**
   * Get all the options passed on the command line when MyRobotLab is executed.
   *
   * @return The options that were passed on the command line
   */
  public static CmdOptions getOptions() {
    return options;
  }

  /**
   * TODO Unimplemented
   * 
   * @param sd
   *          ServiceData to use
   * @return sd
   */
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
   *          t
   * @param id
   *          i
   * @param remoteUuid
   *          remote id
   * @return describe results
   *
   */
  public DescribeResults describe(String type, String id, String remoteUuid) {
    DescribeQuery query = new DescribeQuery(type, remoteUuid);
    return describe(type, query);
  }

  /**
   * Get a default DescribeResults from this instance.
   *
   * @return A default description of this instance
   */
  public DescribeResults describe() {
    // default query
    return describe("platform", null);
  }

  /**
   * Describe results returns the information of a "describe" which can be
   * detailed information regarding services, theire methods and input or output
   * types.
   * <p>
   * FIXME - describe(String[] filters) where filter can be name, type, local,
   * state, etc
   * <p>
   * FIXME uuid and query are unused
   *
   * @param uuid
   *          u
   * @param query
   *          q
   * @return describe results
   *
   *
   *
   */
  public DescribeResults describe(String uuid, DescribeQuery query) {

    DescribeResults results = new DescribeResults();
    results.setStatus(Status.success("Ahoy!"));

    String fullname = null;

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
        fullname = list[i];
        ServiceInterface si = registry.get(fullname);

        Registration registration = new Registration(si);

        results.addRegistration(registration);
      }

    } catch (Exception e) {
      log.error("describe threw on {}", fullname, e);
    }

    return results;
  }

  /**
   * Describe results from remote query to describe
   *
   * @param results
   *          describe results
   *
   *
   */
  public void onDescribe(DescribeResults results) {
    List<Registration> reservations = results.getReservations();
    if (reservations != null) {
      for (Registration reservation : reservations) {
        if ("runtime".equals(reservation.getName()) && !getId().equals(reservation.getId())) {
          // If there's a reservation for a remote runtime, subscribe to its
          // registered
          // Maybe this should be done in register()?
          subscribe(reservation.getFullName(), "registered");
        }
        register(reservation);
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
   *
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

  /**
   * Listener for authentication.
   * 
   * @param response
   *          The results from a foreign instance's
   *          {@link Runtime#describe(String, DescribeQuery)}
   */
  public void onAuthenticate(DescribeResults response) {
    log.info("onAuthenticate {}", response);
  }

  /**
   * Get a list of metadata about all services local to this instance.
   * 
   * @return A list of metadata about local services
   * @see ServiceData#getServiceTypes()
   */
  public List<MetaData> getServiceTypes() {
    List<MetaData> filteredTypes = new ArrayList<>();
    for (MetaData metaData : serviceData.getServiceTypes()) {
      if (metaData.isAvailable()) {
        filteredTypes.add(metaData);
      }
    }
    return filteredTypes;
  }

  /**
   * Register a connection route from one instance to this one.
   *
   * @param uuid
   *          Unique ID for a connecting client
   * @param id
   *          Name or ID of the connecting client
   * @param connection
   *          Details of the connection
   */
  @Override
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

//  @Override
//  public ServiceConfig getFilteredConfig() {
//    RuntimeConfig sc = (RuntimeConfig) super.getFilteredConfig();
//    Set<Listener> removeList = new HashSet<>();
//    for (Listener listener : sc.listeners) {
//      if (listener.callback.equals("onReleased") || listener.callback.equals("onStarted") || listener.callback.equals("onRegistered") || listener.callback.equals("onStopped")
//          || listener.callback.equals("onCreated")) {
//        removeList.add(listener);
//      }
//    }
//    for (Listener remove : removeList) {
//      sc.listeners.remove(remove);
//    }
//    return sc;
//  }

  /**
   * Unregister all connections that a specified client has made.
   *
   * @param uuid
   *          The ID of the client
   */
  @Override
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

  /**
   * Unregister all services originating from the instance with the given ID.
   *
   * @param id
   *          The ID of the instance that is being unregistered
   */
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
   * @return connection map
   */
  public Map<String, Connection> getConnections() {
    return connections;
  }

  /**
   * separated by connection - send connection name and get filter results back
   * for a specific connections connected clients
   *
   * @param gatwayName
   *          name
   * @return map of connections
   *
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
   * @return list connections - current connection names to this mrl runtime
   *
   */
  public Map<String, Connection> lc() {
    return getConnections();
  }

  /**
   * get a specific clients data
   *
   * @param uuid
   *          uuid to get
   * @return connection for uuid
   *
   */
  public Connection getConnection(String uuid) {
    return connections.get(uuid);
  }

  /**
   * @return Globally get all connection uuids
   *
   */
  public List<String> getConnectionUuids() {
    return getConnectionUuids(null);
  }

  /**
   * Get whether a connection to the given client exists.
   *
   * @param uuid
   *          Unique ID of the client to check for
   * @return Whether a connection between this instance and the given client
   *         exists
   */
  boolean connectionExists(String uuid) {
    return connections.containsKey(uuid);
  }

  /**
   * Get connection ids that belong to a specific gateway
   *
   * @param name
   *          n
   * @return list of uuids
   *
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

  /**
   * Get the Class instance for a specific service.
   *
   * @param inName
   *          The name of the service
   * @return The Class of the service.
   * @see #getFullName(String)
   */
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
   *          id
   * @return the connection
   *
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
   *          remote
   * @return the gateway
   *
   */
  public Gateway getGatway(String remoteId) {
    // get a connection from the route
    Connection conn = getRoute(remoteId);
    if (conn == null) {
      log.debug("no connection for id {}", remoteId);
      return null;
    }
    // find the gateway managing the connection
    return (Gateway) getService((String) conn.get("gateway"));
  }

  /**
   * Get the full name of the service. A full name is defined as a "short name"
   * plus the ID of the Runtime instance it is attached to. The two components
   * are separated by an '@' character. If the given name is already a full
   * name, it is returned immediately, otherwise a full name is constructed by
   * assuming the service is local to this instance. Example:
   * 
   * <pre>
   * {
   *   &#64;code
   *   String shortName = "python";
   *
   *   // Assume the local name is "bombastic-cherry"
   *   String fullName = getFullName(shortName);
   *   // fullName is now "python@bombastic-cherry"
   *
   *   fullName = getFullName(fullName);
   *   // fullName is unchanged because it was already a full name
   *
   * }
   * </pre>
   *
   *
   * @param shortname
   *          The name to convert to a full name
   * @return shortname if it is already a full name, or a newly constructed full
   *         name
   */
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
  @Override
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
  public String created(String name) {
    return name;
  }

  @Override
  public String started(String name) {
    // if this is to be used as a callback in Python
    // users typically would want simple name ... not "fullname"

    return name;
  }

  @Override
  public String stopped(String name) {
    return name;
  }

  public static Plan getPlan() {
    Runtime runtime = Runtime.getInstance();
    return runtime.getLocalPlan();
  }

  public Plan getLocalPlan() {
    return masterPlan;
  }

  /**
   * Clear the {@link #masterPlan}.
   */
  static public void clearPlan() {
    Runtime runtime = Runtime.getInstance();
    runtime.masterPlan.clear();
    runtime.masterPlan.put("runtime", new RuntimeConfig());
    // unset config path
    runtime.configName = null;
    runtime.publishConfigList();
  }

  /**
   * Wrapper for {@link ServiceData#getMetaData(String, String)}
   * 
   * @param serviceName
   *          The name of the service
   * @param serviceType
   *          The type of the service
   * @return The metadata of the service.
   */
  public static MetaData getMetaData(String serviceName, String serviceType) {
    return ServiceData.getMetaData(serviceName, serviceType);
  }

  /**
   * Wrapper for {@link ServiceData#getMetaData(String)}
   * 
   * @param serviceType
   *          The type of the service
   * @return The metadata of the service.
   */
  public static MetaData getMetaData(String serviceType) {
    return ServiceData.getMetaData(serviceType);
  }

  /**
   * Whether the singleton has been created
   * 
   * @return Whether the singleton exists
   */
  public static boolean exists() {
    return runtime != null;
  }

  /**
   * Attempt to get the most likely valid address priority would be a lan
   * address - possibly the smallest class
   *
   * @return string address
   *
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
   *          cmd line args from agent spawn
   *
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

      File checkStart = new File("start.yml");
      if (checkStart.exists()) {
        String yml = FileIO.toString("start.yml");
        startYml = CodecUtils.fromYaml(yml, CmdConfig.class);
      }

      // id always required - precedence
      // if none supplied one will be generated
      // if in start.yml it will be used
      // if supplied by the command line it will be used
      // command line has the highest precedence
      if (options.id == null) {
        if (startYml == null || startYml.id == null || !startYml.enable) {
          options.id = NameGenerator.getName();
        } else {
          options.id = startYml.id;
        }
      }

      // String id = (options.fromLauncherx) ? options.id :
      // String.format("%s-launcher", options.id);
      String id = options.id;

      // fix paths
      Platform platform = Platform.getLocalInstance();
      platform.setId(id);

      // save an output of our cmd options
      File dataDir = new File(Runtime.DATA_DIR);
      if (!dataDir.exists()) {
        dataDir.mkdirs();
      }

      File configRoot = new File(Runtime.CONFIG_ROOT);
      if (!configRoot.exists()) {
        configRoot.mkdirs();
      }

      if (options.virtual) {
        Platform.setVirtual(true);
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

      if (options.configRoot != null || (startYml != null && startYml.configRoot != null && startYml.enable)) {
        CONFIG_ROOT = (startYml != null && startYml.configRoot != null) ? startYml.configRoot : options.configRoot;
      }

      // if a you specify a config file it becomes the "base" of configuration
      // inline flags will still override values
      if (options.config != null || (startYml != null && startYml.config != null && startYml.enable)) {
        // if this is a valid config, it will load
        Runtime.getInstance();
      }

      createAndStartServices(options.services);

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

    } catch (Exception e) {
      log.error("runtime exception", e);
      Runtime.mainHelp();
      shutdown();
      log.error("main threw", e);
    }
  }

  public static void initLog() {
    if (options != null) {
      LoggingFactory.setLogFile(options.logFile);
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
   * A gateway is responsible for creating a key to associate a unique
   * "Connection". This key should be retrievable, when a msg arrives at the
   * service which needs to be sent remotely. This key is used to get the
   * "Connection" to send the msg remotely
   *
   * @param string
   *          s
   * @param uuid
   *          u
   *
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
    ServiceInterface s = null;
    try {
      s = create(name, type);
      s.load();
      s.startService();
    } catch (Exception e) {
      log.error("loadAndStart threw", e);
    }
    return s;
  }

  /**
   * DEFAULT IF NOTHING EXISTS DO NOT DEFAULT SOMETHING THAT'S ALREADY IN PLAN
   * OVERRIDE WITH FILE
   * 
   * Load a single service entry into the plan through yml or default. This
   * method is responsible for resolving the Type and ServiceConfig for a single
   * service. Since some service Types are composites and require Peers, it can
   * potentially be recursive. The level of overrides are from highest priority
   * to lowest :
   * 
   * <pre>
   *       if a Plan definition of {name} exists, use it   - "current" plan definition !
   *       /data/config/{configName}/{service}.yml          - user's yml override
   *       /resource/config/{configName}/{service}.yml      - system yml default
   *       {ServiceConfig}.java                             - system java type default
   * 
   * 
   * </pre>
   * 
   * @param plan
   *          - plan to load
   * @param name
   *          - name of service
   * @param type
   *          - type of service
   * @param start
   *          - weather to specify in RuntimeConfig.registry to "start" this
   *          service when createFromPlan is run
   * @param level
   *          - level of the depth, services may load peers which in turn will
   *          load more, this is the depth of recursion
   * @return
   * @throws IOException
   */
  synchronized private Plan loadService(Plan plan, String name, String type, boolean start, int level) throws IOException {

    log.info("loading - {} {} {}", name, type, level);

    if (plan == null) {
      log.error("plan required to load a system");
      return null;
    }

    ServiceConfig sc = plan.get(name);

    // if there isn't some form of definition in the plan
    // get a "default" service config and put it in the plan
    if (sc == null) {
      if (type == null) {
        log.error("cannot get Java def with type == null");
      }
      log.info("getting default Java definition {} {}", name, type);

      // TODO !!!! - switch on Runtime var useDefaults = true/false
      ServiceConfig.getDefault(plan, name, type);
      sc = plan.get(name);
    }

    // HIGHEST PRIORITY - OVERRIDE WITH FILE
    String configPath = runtime.getConfigName();
    if (configPath != null) {
      log.info("priority #1 user's yml override {}", configPath + fs + name + ".yml");
      // PRIORITY #1
      // find if a current yml config file exists - highest priority
      ServiceConfig fileSc = readServiceConfig(configPath, name);
      if (fileSc != null) {
        // if definition exists in file form, it overrides current memory one
        sc = fileSc;
      }
    }

    if (sc == null && type == null) {
      log.error("no local config and unknown type");
    }

    // finalize
    if (sc != null) {
      plan.put(name, sc);
      // RECURSIVE load peers
      Map<String, Peer> peers = sc.getPeers();
      for (String peerKey : peers.keySet()) {
        Peer peer = peers.get(peerKey);
        // recursive depth load - parent and child need to be started
        runtime.loadService(plan, peer.name, peer.type, start && peer.autoStart, level + 1);
      }

      // valid service config at this point - now determine if its supposed to
      // start or not
      // if its level 0 then it was requested by user or config - so it needs to
      // start
      // if its not level 0 then it was loaded because peers were defined and
      // appropriate config loaded
      // peer.autoStart should determine if the peer starts if not explicitly
      // requested by the
      // user or config
      if (level == 0 || start) {
        plan.addRegistry(name);
      }

    } else {
      log.info("could not load {} {} {}", name, type, level);
    }

    return plan;
  }

  /**
   *
   * @param configName
   *          - filename or dir of config set
   * @param name
   *          - name of config file within that dir e.g. {name}.yml
   * @return
   */
  public ServiceConfig readServiceConfig(String configName, String name) {
    // if config path set and yaml file exists - it takes precedence

    if (configName == null) {
      configName = runtime.getConfigName();
    }

    if (configName == null) {
      log.info("config name is null cannot load {} file system", name);
      return null;
    }

    String filename = CONFIG_ROOT + fs + configName + fs + name + ".yml";
    File check = new File(filename);
    ServiceConfig sc = null;
    if (check.exists()) {
      try {
        sc = CodecUtils.readServiceConfig(filename);
      } catch (IOException e) {
        error(e);
      }
    }
    return sc;
  }

  public String publishConfigLoaded(String name) {
    return name;
  }

  // @Override
  // public ServiceConfig getConfig() {
  // RuntimeConfig config = (RuntimeConfig)super.getConfig();
  // List<org.myrobotlab.service.config.ServiceConfig.Listener> listeners = new
  // ArrayList
  // for (org.myrobotlab.service.config.ServiceConfig.Listener listener:
  // config.listeners) {
  // if (listener.equals("stopped") || listener.equals("created")||
  // listener.equals("registered")|| listener.equals("released")) {
  //
  // }
  // }
  // }

  public String setAllIds(String id) {
    Platform.getLocalInstance().setId(id);
    for (ServiceInterface si : getServices()) {
      si.setId(id);
    }
    return id;
  }

  
  public RuntimeConfig apply(RuntimeConfig c) {
    super.apply(c);
    config = (RuntimeConfig)c;
    
    setLocale(config.locale);

    if (config.id != null) {
      setAllIds(config.id);
    }

    if (config.logLevel != null) {
      setLogLevel(config.logLevel);
    }

    info("setting locale to %s", config.locale);
    if (config.virtual != null) {
      info("setting virtual to %b", config.virtual);
      setAllVirtual(config.virtual);
    }

    if (config.enableCli) {
      startInteractiveMode();
      info("enabled cli");
    } else {
      stopInteractiveMode();
      info("disabled cli");
    }

    broadcastState();
    return c;
  }

  /**
   * release the current config
   */
  static public void releaseConfig() {
    String currentConfigPath = Runtime.getInstance().getConfigName();
    if (currentConfigPath != null) {
      releaseConfigPath(currentConfigPath);
    }
  }

  /**
   * wrapper
   * 
   * @param configName
   */
  static public void releaseConfig(String configName) {
    setConfig(configName);
    releaseConfigPath(Runtime.getInstance().getConfigName());
  }

  /**
   * Release a configuration set - this depends on a runtime file - and it will
   * release all the services defined in it, with the exception of the
   * originally started services
   * 
   * @param configPath
   *          config set to release
   *
   */
  static public void releaseConfigPath(String configPath) {
    try {
      String filename = CONFIG_ROOT + fs + Runtime.getInstance().getConfigName() + fs + "runtime.yml";
      String releaseData = FileIO.toString(new File(filename));
      RuntimeConfig config = CodecUtils.fromYaml(releaseData, RuntimeConfig.class);
      List<String> registry = config.getRegistry();
      Collections.reverse(Arrays.asList(registry));
      for (String name : registry) {
        if (Runtime.getInstance().startingServices.contains(name)) {
          continue;
        }
        release(name);
      }
    } catch (Exception e) {
      Runtime.getInstance().error("could not release %s", configPath);
    }
  }

  public static String getConfigRoot() {
    return CONFIG_ROOT;
  }

  /**
   * wrapper for saveConfigPath with default prefix path supplied
   * 
   * @param configName
   * @return
   */
  static public boolean saveConfig(String configName) {
    Runtime runtime = Runtime.getInstance();
    if (configName == null) {
      runtime.error("saveConfig require a name cannot be null");
      return false;
    }
    boolean ret = runtime.saveService(configName, null, null);
    runtime.broadcastState();
    return ret;
  }

  /**
   * 
   * Saves the current runtime, all services and all configuration for each
   * service in the current "config path", if the config path does not exist
   * will error
   *
   * @param configName
   *          - config set name if null defaults to default
   * @param serviceName
   *          - service name if null defaults to saveAll
   * @param filename
   *          - if not explicitly set - will be standard yml filename
   * @return - true if all goes well
   */
  public boolean saveService(String configName, String serviceName, String filename) {
    try {

      if (configName == null) {
        error("config name cannot be null");
        return false;
      }

      setConfig(configName);

      String configPath = CONFIG_ROOT + fs + configName;

      File dir = new File(configPath);
      dir.mkdirs();

      // save running services
      Set<String> servicesToSave = new HashSet<>();

      // conditional boolean to flip and save a config name to start.yml ?
      if (startYml.enable) {
        startYml.id = getId();
        startYml.config = configName;
        startYml.configRoot = CONFIG_ROOT;
        FileIO.toFile("start.yml", CodecUtils.toYaml(startYml));
      }

      if (serviceName == null) {
        // all services
        servicesToSave = getLocalServices().keySet();
      } else {
        // single service
        servicesToSave.add(serviceName);
      }

      for (String s : servicesToSave) {
        ServiceInterface si = getService(s);
        // TODO - switch to save "NON FILTERED" config !!!!
        // get filtered clone of config for saving
        ServiceConfig config = si.getFilteredConfig();
        String data = CodecUtils.toYaml(config);
        String ymlFileName = configPath + fs + CodecUtils.getShortName(s) + ".yml";
        FileIO.toFile(ymlFileName, data.getBytes());
        info("saved %s", ymlFileName);
      }

      invoke("publishConfigList");
      return true;

    } catch (Exception e) {
      error(e);
    }
    return false;
  }

  public String setConfigName(String name) {
    if (name != null && name.contains(fs)) {
      error("invalid character " + fs + " in configuration name");
      return configName;
    }
    if (name != null) {
      configName = name.trim();
    }
    invoke("publishConfigList");
    return name;
  }

  public String getConfigName() {
    return configName;
  }

  public boolean isProcessingConfig() {
    return processingConfig;
  }

  /**
   * Sets the directory for the current config. This will be under configRoot +
   * fs + configName. Static wrapper around setConfigName - so it can be used in
   * the same way as all the other common static service methods
   *
   * @param configName
   *          - config dir name under data/config/{config}
   * @return configName
   */
  public static String setConfig(String configName) {
    Runtime runtime = Runtime.getInstance();
    runtime.setConfigName(configName);
    return configName;
  }

  /**
   * sets the root of all config, where root + fs + configName exists
   * 
   * @param root
   * @return
   */
  public static String setConfigRoot(String root) {
    CONFIG_ROOT = root;
    return root;
  }

  // FIXME - move this to service and add default (no servicename) method
  // signature
  @Deprecated /*
               * I don't think this was a good solution - to handle interface
               * lists in the js client - the js runtime should register for
               * lifecycle events, the individiual services within that js
               * runtime should only have local event handling to change attach
               * lists
               */
  public void registerForInterfaceChange(String requestor, Class<?> interestedInterface) {
    registerForInterfaceChange(interestedInterface.getCanonicalName());
  }

  /**
   * Builds the requestedAttachMatrix which is a mapping between new types and
   * their requested interfaces - interfaces they are interested in.
   *
   * This data should be published whenever new "Type" definitions are found
   *
   * @param targetedInterface
   *          - interface this add new interface to requested interfaces - add
   *          current names of services which fulfill that interface "IS ASKING"
   *
   */
  public void registerForInterfaceChange(String targetedInterface) {
    // boolean changed
    Set<String> namesForRequestedInterface = interfaceToNames.get(targetedInterface);
    if (namesForRequestedInterface == null) {
      namesForRequestedInterface = new HashSet<>();
      interfaceToNames.put(targetedInterface, namesForRequestedInterface);
    }

    // search through interfaceToType to find all types that implement this
    // interface

    if (interfaceToType.containsKey(targetedInterface)) {
      Set<String> types = interfaceToType.get(targetedInterface);
      if (types != null) {
        for (String type : types) {
          Set<String> names = typeToNames.get(type);
          namesForRequestedInterface.addAll(names);
        }
      }
    }
    invoke("publishInterfaceToNames");
  }

  /**
   * save the current "plan" to the filesystem this will give the user the
   * opportunity to tweak defaults before starting
   * 
   * @param configName
   */
  static public void savePlan(String configName) {
    Runtime runtime = Runtime.getInstance();
    Runtime.setConfig(configName);
    Runtime.getInstance().savePlanInternal(runtime.getConfigName());
  }

  private void savePlanInternal(String configName) {

    if (configName == null) {
      error("cannot save plan config name is null");
      return;
    }

    File configDirectory = new File(CONFIG_ROOT + fs + configName);
    configDirectory.mkdirs();

    for (String s : masterPlan.keySet()) {
      String filename = CONFIG_ROOT + fs + configName + fs + s + ".yml";
      String data = CodecUtils.toYaml(masterPlan.get(s));
      try {
        FileIO.toFile(filename, data.getBytes());
      } catch (Exception e) {
        error(e);
      }
      info("saved %s config to %s", getName(), filename);
    }
  }

  /**
   * Published whenever a new service type definition if found
   *
   * @return
   */
  public Map<String, Set<String>> publishInterfaceTypeMatrix() {
    return interfaceToType;
  }

  public Map<String, Set<String>> publishInterfaceToNames() {
    return interfaceToNames;
  }

  static public Plan saveDefault(String className) {
    try {
      Runtime runtime = Runtime.getInstance();
      return runtime.saveDefault(className.toLowerCase(), className);
    } catch (Exception e) {
      log.error("saving default config failed", e);
    }
    return null;
  }

  /**
   * Helper method - returns if a service is started
   *
   * @param name
   *          - name of service
   * @return - true if started
   */
  static public boolean isStarted(String name) {
    String fullname = null;
    if (name == null) {
      return false;
    }
    if (!name.contains("@")) {
      fullname = name + "@" + Runtime.getInstance().getId();
    } else {
      fullname = name;
    }
    if (registry.containsKey(fullname)) {
      ServiceInterface si = registry.get(fullname);
      return si.isRunning();
    }

    return false;
  }

  /**
   * Load all configuration files from a given directory.
   *
   * @param configPath
   *          The directory to load from
   */
  public static void loadConfigPath(String configPath) {

    Runtime.setConfig(configPath);
    Runtime runtime = Runtime.getInstance();

    String configSetDir = runtime.getConfigName() + fs + runtime.getConfigName();
    File check = new File(configSetDir);
    if (configPath == null || configPath.isEmpty() || !check.exists() || !check.isDirectory()) {
      runtime.error("config set %s does not exist or is not a directory", check.getAbsolutePath());
      return;
    }

    File[] configFiles = check.listFiles();
    runtime.info("%d config files found", configFiles.length);
    for (File f : configFiles) {
      if (!f.getName().toLowerCase().endsWith(".yml")) {
        log.info("{} - none yml file found in config set", f.getAbsolutePath());
      } else {
        runtime.loadFile(f.getAbsolutePath());
      }
    }
  }

  /**
   * Load a service from a file
   * 
   * @param path
   *          The full path of the file to load - this DOES NOT set the
   *          configPath
   */
  public void loadFile(String path) {
    try {
      File f = new File(path);
      if (!f.exists() || f.isDirectory()) {
        error("loadFile cannot load %s - it does not exist", path);
        return;
      }
      String name = f.getName().substring(0, f.getName().length() - 4);
      ServiceConfig sc = CodecUtils.readServiceConfig(path);
      loadService(Runtime.getPlan(), name, sc.type, true, 0);
      masterPlan.put(name, sc);
    } catch (Exception e) {
      error("loadFile requirese");
    }
  }

  final public Plan getDefault(String name, String type) {
    return ServiceConfig.getDefault(new Plan("runtime"), name, type);
  }

  final public Plan saveDefault(String name, String type) {
    return saveDefault(name, name, type, false);
  }

  final public Plan saveDefault(String name, String type, boolean fullPlan) {
    return saveDefault(name, name, type, fullPlan);
  }

  final public Plan saveDefault(String configName, String name, String type, boolean fullPlan) {

    Plan plan = ServiceConfig.getDefault(new Plan(name), name, type);
    String configPath = CONFIG_ROOT + fs + configName;

    if (!fullPlan) {
      try {
        String filename = configPath + fs + name + ".yml";
        ServiceConfig sc = plan.get(name);
        String yaml = CodecUtils.toYaml(sc);
        FileIO.toFile(filename, yaml);
        info("saved %s", filename);
      } catch (IOException e) {
        error(e);
      }
    } else {
      for (String service : plan.keySet()) {
        try {
          String filename = configPath + fs + service + ".yml";
          ServiceConfig sc = plan.get(service);
          String yaml = CodecUtils.toYaml(sc);
          FileIO.toFile(filename, yaml);
          info("saved %s", filename);
        } catch (IOException e) {
          error(e);
        }
      }
    }
    return plan;
  }

  public void savePlan(String name, String type) {
    saveDefault(name, type, true);
  }

  public void saveAllDefaults() {
    saveAllDefaults(new File(getResourceDir()).getParent(), false);
  }

  public void saveAllDefaults(String configPath, boolean fullPlan) {
    List<MetaData> types = serviceData.getAvailableServiceTypes();
    for (MetaData meta : types) {
      saveDefault(configPath + fs + meta.getSimpleName(), meta.getSimpleName().toLowerCase(), meta.getSimpleName(), fullPlan);
    }
  }

  public String getConfigPath() {
    if (configName == null) {
      error("config name is not set");
      return null;
    }
    return CONFIG_ROOT + fs + configName;
  }

  @Override
  public RuntimeConfig getConfig() {
    return config;
  }

}
