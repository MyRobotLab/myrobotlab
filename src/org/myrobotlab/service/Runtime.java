package org.myrobotlab.service;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
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
import java.util.Vector;

import org.apache.ivy.core.report.ResolveReport;
import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.BootstrapFactory;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MessageListener;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.PreLogger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.framework.repo.UpdateReport;
import org.myrobotlab.framework.repo.Updates;
import org.myrobotlab.logging.Appender;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.HTTPRequest;
import org.myrobotlab.service.interfaces.Bootstrap;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.string.StringUtil;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

/**
 * 
 * Runtime is responsible for the creation and removal of all Services and the
 * associated static registries It maintains state information regarding
 * possible & running local Services It maintains state information regarding
 * foreign Runtimes It is a singleton and should be the only service of Runtime
 * running in a process The host and registry maps are used in routing
 * communication to the appropriate service (be it local or remote) It will be
 * the first Service created It also wraps the real JVM Runtime object.
 * 
 * TODO - get last args & better restart (with Agent possibly?)
 * 
 * RuntimeMXBean - scares me - but the stackTrace is clever RuntimeMXBean
 * runtimeMxBean = ManagementFactory.getRuntimeMXBean(); List<String> arguments
 * = runtimeMxBean.getInputArguments();
 * 
 * final StackTraceElement[] stackTrace =
 * Thread.currentThread().getStackTrace(); final String mainClassName =
 * stackTrace[stackTrace.length - 1].getClassName();
 * 
 * 
 */
@Root
public class Runtime extends Service implements MessageListener {
	final static private long serialVersionUID = 1L;

	// ---- rte members begin ----------------------------
	static private final HashMap<URI, ServiceEnvironment> hosts = new HashMap<URI, ServiceEnvironment>();
	static private final HashMap<String, ServiceInterface> registry = new HashMap<String, ServiceInterface>();

	/**
	 * map to hide methods we are not interested in
	 */
	// FIXME - REMOVE ALL STATICS !!!
	static private HashSet<String> hideMethods = new HashSet<String>();

	static private boolean needsRestart = false;

	static private String runtimeName;

	static private Date startDate = new Date();

	@Element
	private boolean checkForUpdatesOnStart = true;

	@Element
	private boolean autoRestartAfterUpdate = false;

	// FYI - can't be transient - "should" be preserved in
	// network transport - it's the "instances" repo
	// FIXME - non static member variables should be initialized in constructor
	private Repo repo = null;

	private Platform platform = Platform.getLocalInstance();

	// ---- rte members end ------------------------------
	private static long uniqueID = new Random(System.currentTimeMillis()).nextLong();

	// ---- Runtime members end -----------------

	public final static Logger log = LoggerFactory.getLogger(Runtime.class);

	/**
	 * Object used to synchronize initializing this singleton.
	 */
	private static final Object instanceLockObject = new Object();

	/**
	 * The singleton of this class.
	 */
	private static Runtime runtime = null;

	private List<String> jvmArgs;
	private List<String> args;

	private boolean shutdownAfterUpdate = false;

	/**
	 * global startingArgs - whatever came into main each runtime will have its
	 * individual copy
	 */
	private static String[] globalArgs;

	public static String getVersion() {
		return FileIO.resourceToString("version.txt");
	}

	public static String getUptime() {
		Date now = new Date();
		long diff = now.getTime() - startDate.getTime();

		long diffSeconds = diff / 1000 % 60;
		long diffMinutes = diff / (60 * 1000) % 60;
		long diffHours = diff / (60 * 60 * 1000) % 24;
		long diffDays = diff / (24 * 60 * 60 * 1000);

		StringBuffer sb = new StringBuffer();
		sb.append(diffDays).append(" days ").append(diffHours).append(" hours ").append(diffMinutes).append(" minutes ").append(diffSeconds).append(" seconds ");
		return sb.toString();
	}

	public static void setRuntimeName(String inName) {
		runtimeName = inName;
	}

	public Runtime(String n) {
		super(n);

		synchronized (instanceLockObject) {
			if (runtime == null) {
				runtime = this;
			}
		}

		String libararyPath = System.getProperty("java.library.path");
		String userDir = System.getProperty("user.dir");
		String userHome = System.getProperty("user.home");

		String vmName = System.getProperty("java.vm.name");
		// TODO this should be a single log statement
		// http://developer.android.com/reference/java/lang/System.html

		Date now = new Date();
		String format = "yyyy/MM/dd HH:mm:ss";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		SimpleDateFormat gmtf = new SimpleDateFormat(format);
		gmtf.setTimeZone(TimeZone.getTimeZone("UTC"));
		log.info("============== args begin ==============");
		StringBuffer sb = new StringBuffer();
		Bootstrap bootstrap = BootstrapFactory.getInstance();
		jvmArgs = bootstrap.getJVMArgs();
		args = new ArrayList<String>();
		if (globalArgs != null) {
			for (int i = 0; i < globalArgs.length; ++i) {
				sb.append(globalArgs[i]);
				args.add(globalArgs[i]);
			}
		}
		log.info(String.format("jvmArgs %s", Arrays.toString(jvmArgs.toArray())));
		log.info(String.format("args %s", Arrays.toString(args.toArray())));
		log.info("============== args end ==============");
		log.info("============== prelog begin ==============");
		try {
			// when having issues next time just close the prelogger... duh
			// can't use java.nio.file on Androi
			// Path prelog = Paths.get(PreLogger.PRELOG_FILENAME);

			File prelog = new File(PreLogger.PRELOG_FILENAME);

			if (prelog.exists()) {
				log.info(FileIO.fileToString(PreLogger.PRELOG_FILENAME));
				log.info(String.format("deleting %s", PreLogger.PRELOG_FILENAME));
				prelog.delete();
				// Files.delete(prelog);
				log.info(String.format("deleted %s", PreLogger.PRELOG_FILENAME));
			} else {
				log.info("prelog does not exist");
			}

		} catch (Exception e) {
			Logging.logException(e);
		}
		log.info("============== prelog end ==============");
		log.info("============== env begin ==============");
		Map<String, String> env = System.getenv();
		for (Map.Entry<String, String> entry : env.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			log.info(String.format("%s=%s", key, value));
		}
		log.info("============== env end ==============");

		// Platform platform = Platform.getLocalInstance();
		log.info("============== normalized ==============");
		log.info("{} - GMT - {}", sdf.format(now), gmtf.format(now));
		log.info(String.format("ivy [runtime,%s.%d.%s]", platform.getArch(), platform.getBitness(), platform.getOS()));
		log.info(String.format("os.name [%s] getOS [%s]", System.getProperty("os.name"), platform.getOS()));
		log.info(String.format("os.arch [%s] getArch [%s]", System.getProperty("os.arch"), platform.getArch()));
		log.info(String.format("getBitness [%d]", platform.getBitness()));
		log.info(String.format("java.vm.name [%s] getVMName [%s]", vmName, platform.getVMName()));
		log.info(String.format("version [%s]", FileIO.resourceToString("version.txt")));
		log.info(String.format("/resource [%s]", FileIO.getResouceLocation()));
		log.info(String.format("jar path [%s]", FileIO.getResourceJarPath()));
		log.info(String.format("sun.arch.data.model [%s]", System.getProperty("sun.arch.data.model")));

		log.info("============== non-normalized ==============");
		log.info(String.format("java.vm.name [%s]", vmName));
		log.info(String.format("java.vm.version [%s]", System.getProperty("java.vm.version")));
		log.info(String.format("java.vm.vendor [%s]", System.getProperty("java.vm.vendor")));
		log.info(String.format("java.vm.version [%s]", System.getProperty("java.vm.version")));
		log.info(String.format("java.vm.vendor [%s]", System.getProperty("java.runtime.version")));

		// System.getProperty("pi4j.armhf")
		log.info(String.format("os.version [%s]", System.getProperty("os.version")));
		log.info(String.format("os.version [%s]", System.getProperty("os.version")));

		log.info(String.format("java.home [%s]", System.getProperty("java.home")));
		log.info(String.format("java.class.path [%s]", System.getProperty("java.class.path")));
		log.info(String.format("java.library.path [%s]", libararyPath));
		log.info(String.format("user.dir [%s]", userDir));

		log.info(String.format("user.home [%s]", userHome));
		log.info(String.format("total mem [%d] Mb", Runtime.getTotalMemory() / 1048576));
		log.info(String.format("total free [%d] Mb", Runtime.getFreeMemory() / 1048576));

		log.info("getting local repo");
		repo = new Repo(n);

		hideMethods.add("main");
		hideMethods.add("loadDefaultConfiguration");
		hideMethods.add("getDescription");
		hideMethods.add("run");
		hideMethods.add("access$0");

		// TODO - check for updates on startup ???
		// repo = new Repo();

		// starting this
		startService();
	}

	/**
	 * returns the platform type of a remote system
	 * 
	 * @param uri
	 *            - the access uri of the remote system
	 * @return Platform description
	 */
	public Platform getPlatform(URI uri) {
		ServiceEnvironment local = hosts.get(uri);
		if (local != null) {
			return local.platform;
		}

		error("can't get local platform in service environment");

		return null;
	}

	/**
	 * returns version string of MyRobotLab
	 * 
	 * @return
	 */
	public String getLocalVersion() {
		return getVersion(null);
	}

	/**
	 * returns version string of MyRobotLab instance based on uri e.g : uri
	 * mrl://10.5.3.1:7777 may be a remote instance null uri is local
	 * 
	 * @param uri
	 *            - key of ServiceEnvironment
	 * @return version string
	 */
	public String getVersion(URI uri) {
		ServiceEnvironment local = hosts.get(uri);
		if (local != null) {
			return local.version;
		}

		error("can't get local version in service environment");

		return null;
	}

	/**
	 * check repo for updates - this will collect "all" update information both
	 * service dependency resolution & jar version information
	 * 
	 * @return
	 */
	static public Updates checkForUpdates() {
		runtime.invoke("checkingForUpdates");
		Updates updates = runtime.repo.checkForUpdates();
		runtime.invoke("publishUpdates", updates);
		return updates;
	}

	/**
	 * publishing event - since checkForUpdates may take a while
	 */
	public void checkingForUpdates() {
		log.info("checking for updates");
	}

	/**
	 * publish the results of a query of updates - to be presented to the user
	 * for selections
	 * 
	 * @param updates
	 * @return
	 */
	public Updates publishUpdates(Updates updates) {
		return updates;
	}

	// GAH !!! retrieveAll / updateAll / UpdateReport / ResolveReport :P
	public UpdateReport updateAll() {

		UpdateReport report = new UpdateReport();
		report.updates = new Updates(runtimeName);
		report.updates.isValid = true; // forcing since this is direct request
		report.updates.serviceTypesToUpdate = Arrays.asList(getServiceTypeNames());

		invoke("updatesBegin", report.updates);
		// :D optimized !
		report.reports = repo.retrieveAll();
		invoke("updatesFinished", report.reports);
		return report;
	}

	/**
	 * command line update update myrobotlab.jar only no user interaction is
	 * required - NO RESTART ONLY SHUTDOWN !!! no endless loop of bootstrap
	 * getting -update param after update :P
	 * 
	 * @return
	 */
	public UpdateReport update() {
		// we are going to force a shutdown after the update
		shutdownAfterUpdate = true;
		return runtime.applyUpdate();
	}

	/**
	 * FIXME - if true - service data xml needs to be pulled from repo this
	 * method is called by the user (or system) when a specific service needs to
	 * be installed (or updated) - it should resolve all the dependencies for
	 * that service
	 * 
	 * @param fullServiceTypeName
	 *            - full service type for dependency resolution
	 * @return
	 */
	public UpdateReport update(String fullServiceTypeName) {
		Updates updates = new Updates(runtimeName);
		updates.isValid = true; // forcing since this is direct request
		updates.serviceTypesToUpdate.add(fullServiceTypeName);
		return applyUpdates(updates);
	}

	/**
	 * headless call - no user intervention needed / no "publishUpdates"
	 * 
	 * @return
	 */
	public UpdateReport applyUpdate() {
		Updates updates = checkForUpdates();
		return applyUpdates(updates);
	}

	/**
	 * all the data contained in updates is used to apply against the running
	 * system. this is where are the business logic of the merge between the
	 * current system, the repo and the users objectives are all resolved
	 * 
	 * @param updates
	 */
	synchronized public UpdateReport applyUpdates(Updates updates) {
		UpdateReport ret = new UpdateReport();
		ret.updates = updates;

		if (!updates.isValid) {
			error("can not apply updates - updates are not valid");
			return null;
		}

		invoke("updatesBegin", updates);

		ArrayList<ResolveReport> reports = new ArrayList<ResolveReport>();
		String intertoobTest = null;
		try {
			intertoobTest = repo.getVersionFromRepo();
			info("remote version %s", intertoobTest);
		} catch (Exception e) {
			error(String.format("if connection error - just bail and save us some time !", e.getMessage()));
			return null;
		}

		// FIXME support ServiceTypes with dependencies of ServiceTypes !!!
		// FIXME compress dependencies of all ServiceTypes into 1 unique
		// list/Hash

		for (int i = 0; i < updates.serviceTypesToUpdate.size(); ++i) {
			try {
				ArrayList<ResolveReport> report = repo.retrieveServiceType(updates.serviceTypesToUpdate.get(i));
				for (int j = 0; j < report.size(); ++j) {
					reports.add(report.get(j));
				}
				// FIXME - distinguish all good versus bad reports ... DUH
			} catch (Exception e) {
				Logging.logException(e);
			}
		}

		ret.reports = reports;

		// FIXME - selectively choose which parts to update !!!
		// NOT JUST update because there "is" an update - many
		// potential parts to an update

		if (updates.hasJarUpdate()) {
			info("updating myrobotlab.jar");
			if (runtime.repo.getLatestJar()) {

				if (shutdownAfterUpdate) {
					log.info("shutdownAfterUpdate = true");
					releaseAll();
					System.exit(0);
				}

				if (autoRestartAfterUpdate) {
					log.info("autoRestartAfterUpdate = true");
					// asynch call to get user or config data to determine if a
					// restart
					// is desired
					restart();
				} else {
					log.info("autoRestartAfterUpdate = false");
					log.info("needsRestart = true");
					// async call to request permission to restart
					// publish requestSpawnBootStrap
					needsRestart = true;
					// invoke("confirmRestart");
					// progressDialog confirms after download
				}

			}
		}

		invoke("updatesFinished", reports);
		return ret;
	}

	/**
	 * publishing event for the start of updates being applied
	 */
	public Updates updatesBegin(Updates updates) {
		return updates;
	}

	/**
	 * publishing event for the end of updates being applied
	 */
	public ArrayList<ResolveReport> updatesFinished(ArrayList<ResolveReport> report) {
		return report;
	}

	/**
	 * publishing the updates progress with a series of status messages.
	 * 
	 * @return
	 */
	final public Status updateProgress(final Status status) {
		if (status.isError()) {
			log.error(status.toString());
		} else {
			log.info(status.toString());
		}
		return status;
	}

	/**
	 * invoked to confirm with the user it is appropriate to restart now
	 * 
	 * @return - the current autoRestartAfterUpdate to put in dialog to (never
	 *         ask again)
	 */
	/*
	 * public boolean confirmRestart() { needsRestart = true; return
	 * autoRestartAfterUpdate; }
	 */

	/**
	 * restart occurs after applying updates - user or config data needs to be
	 * examined and see if its an appropriate time to restart - if it is the
	 * spawnBootstrap method will be called and bootstrap.jar will go through
	 * its sequence to update myrobotlab.jar
	 */
	public void restart() {
		try {

			// final java.lang.Runtime r = java.lang.Runtime.getRuntime();
			info("restarting");
			// TODO - timeout release .releaseAll nice ? - check or re-implement
			Runtime.releaseAll();

			// create bootstrap.jar
			Bootstrap bootstrap = BootstrapFactory.getInstance();
			bootstrap.createBootstrapJar();

			// WRONG - jvm args should be created and maintained in bootstrap
			// FIXME - get jvm arguments and other original args
			/*
			 * ArrayList<String> restartArgs = new ArrayList<String>(); for (int
			 * i = 0; i < jvmArgs.size(); ++i) {
			 * restartArgs.add(jvmArgs.get(i)); }
			 */
			/*
			 * for (int i = 0; i < args.size(); ++i) {
			 * restartArgs.add(args.get(i)); }
			 */
			bootstrap.spawn(args);
			System.exit(0);

			// shutdown / exit
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	/**
	 * check if class is a Runtime class
	 * 
	 * @param newService
	 * @return true if class == Runtime.class
	 */
	public static boolean isRuntime(Service newService) {
		return newService.getClass().equals(Runtime.class);
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
					if (runtimeName == null) {
						runtimeName = "runtime";
					}
					runtime = new Runtime(runtimeName);
				}
			}
		}
		return runtime;
	}

	/**
	 * Stops all service-related running items. This releases the singleton
	 * referenced by this class, but it does not guarantee that the old service
	 * will be GC'd. FYI - if stopServices does not remove INSTANCE - it is not
	 * re-entrant in junit tests
	 */
	@Override
	public void stopService() {
		/*
		 * FIXME - re-implement but only start if you have a task if (timer !=
		 * null) { timer.cancel(); // stop all scheduled jobs timer.purge(); }
		 */
		super.stopService();
		runtime = null;
	}

	/**
	 * Runtime singleton service
	 */
	@Override
	public String getDescription() {
		return "Runtime singleton service";
	}

	// ---------- Java Runtime wrapper functions begin --------
	/**
	 * Executes the specified command and arguments in a separate process.
	 * Returns the exit value for the subprocess.
	 * 
	 * @param params
	 * @return
	 */
	public int exec(String[] params) {
		java.lang.Runtime r = java.lang.Runtime.getRuntime();
		try {
			Process p = r.exec(params);
			return p.exitValue();
		} catch (IOException e) {
			logException(e);
		}

		return 0;
	}

	/**
	 * dorky pass-throughs to the real JVM Runtime
	 * 
	 * @return
	 */
	public static final long getTotalMemory() {

		return java.lang.Runtime.getRuntime().totalMemory();
	}

	/**
	 * Returns the amount of free memory in the Java Virtual Machine. Calling
	 * the gc method may result in increasing the value returned by freeMemory.
	 * 
	 * @return
	 */
	public static final long getFreeMemory() {
		return java.lang.Runtime.getRuntime().freeMemory();
	}

	// Reference - cpu utilization
	// http://www.javaworld.com/javaworld/javaqa/2002-11/01-qa-1108-cpu.html

	/**
	 * Returns the number of processors available to the Java virtual machine.
	 * 
	 * @return
	 */
	public static final int availableProcessors() {
		return java.lang.Runtime.getRuntime().availableProcessors();
	}

	/**
	 * big hammer - exits no ifs ands or butts
	 */
	public static final void exit() {
		java.lang.Runtime.getRuntime().exit(-1);
	}

	/**
	 * Terminates the currently running Java virtual machine by initiating its
	 * shutdown sequence. This method never returns normally. The argument
	 * serves as a status code; by convention, a nonzero status code indicates
	 * abnormal termination
	 * 
	 * @param status
	 */
	public static final void exit(int status) {
		java.lang.Runtime.getRuntime().exit(status);
	}

	/**
	 * Runs the garbage collector.
	 */
	public static final void gc() {
		java.lang.Runtime.getRuntime().gc();
	}

	/**
	 * 
	 * @param filename
	 */
	public static final void loadLibrary(String filename) {
		java.lang.Runtime.getRuntime().loadLibrary(filename);
	}

	// ---------- Java Runtime wrapper functions end --------
	/**
	 * 
	 * FIXME - need Platform information (reference from within service ???
	 * 
	 * @param url
	 * @param s
	 * @return
	 */
	public final static synchronized ServiceInterface register(ServiceInterface s, URI url) {
		ServiceEnvironment se = null;
		if (!hosts.containsKey(url)) {
			se = new ServiceEnvironment(url);
			hosts.put(url, se);
		} else {
			se = hosts.get(url);
		}

		if (s != null) {
			if (se.serviceDirectory.containsKey(s.getName())) {
				log.info(String.format("attempting to register %1$s which is already registered in %2$s", s.getName(), url));
				if (runtime != null) {
					runtime.invoke("collision", s.getName());
					runtime.warn("collision registering %s", s.getName());
					runtime.error(String.format(" name collision with %s", s.getName()));
				}
				return s;// <--- BUG ?!?!? WHAT ABOUT THE REMOTE GATEWAYS !!!
			}
		

		// REMOTE BROADCAST to all foreign environments
		// FIXME - Security determines what to export
		// for each gateway
		
		// NEW PART !!!

		Vector<String> remoteGateways = getServicesFromInterface(Gateway.class.getCanonicalName());
		for (int ri = 0; ri < remoteGateways.size(); ++ri) {
			String n = remoteGateways.get(ri);
			// Communicator gateway = (Communicator)registry.get(n);
			ServiceInterface gateway = registry.get(n);

			// for each JVM this gateway is is attached too
			for (Map.Entry<URI, ServiceEnvironment> o : hosts.entrySet()) {
				// Map.Entry<String,SerializableImage> pairs = o;
				URI uri = o.getKey();
				// if its a foreign JVM & the gateway responsible for the remote
				// connection and
				// the foreign JVM is not the host which this service originated
				// from - send it....
				if (uri != null && gateway.getName().equals(uri.getHost()) && !uri.equals(s.getHost())) {
					log.info(String.format("gateway %s sending registration of %s remote to %s", gateway.getName(), s.getName(), uri));
					// FIXME - Security determines what to export
					Message msg = runtime.createMessage("", "register", s);
					// ((Communicator) gateway).sendRemote(uri, msg);
					// //mrl://remote2/tcp://127.0.0.1:50488 <-- wrong
					// sendingRemote is wrong
					// FIXME - optimize gateway.send(msg) && URI TO URI MAP IN
					// RUNTIME !!!
					gateway.in(msg);
				}
			}
		}

		// ServiceInterface sw = new ServiceInterface(s, se.accessURL);
		se.serviceDirectory.put(s.getName(), s);
		// WARNING - SHOULDN'T THIS BE DONE FIRST AVOID DEADLOCK / RACE
		// CONDITION ????
		registry.put(s.getName(), s); // FIXME FIXME FIXME FIXME !!!!!! pre-pend
										// URI if not NULL !!!
		if (runtime != null) {
			runtime.invoke("registered", s);
		}

		return s;
		}
		
		return null;
	}

	/**
	 * called by remote/foreign systems to register a new service through a
	 * subscription
	 * 
	 * @param sw
	 */
	public synchronized void register(ServiceInterface sw) {
		log.debug(String.format("register(ServiceInterface %s)", sw.getName()));

		if (registry.containsKey(sw.getName())) {
			log.warn("{} already defined - will not re-register", sw.getName());
			return;
		}

		ServiceEnvironment se = hosts.get(sw.getHost());
		if (se == null) {
			error("no service environment for %s", sw.getHost());
			return;
		}

		if (se.serviceDirectory.containsKey(sw.getName())) {
			log.info("{} already registered", sw.getName());
			return;
		}
		// FIXME - does the refrence of this service wrapper need to point back
		// to the
		// service environment its referencing?
		// sw.host = se; - can't do this because its final

		se.serviceDirectory.put(sw.getName(), sw);
		registry.put(sw.getName(), sw);
		if (runtime != null) {
			runtime.invoke("registered", sw);
		}

	}

	/**
	 * registers an initial ServiceEnvironment which is a complete set of
	 * Services from a foreign instance of MRL. It returns whether changes have
	 * been made. This is necessary to determine if the register should be
	 * echoed back.
	 * 
	 * @param uri
	 * @param s
	 * @return false if it already matches
	 */
	public static synchronized boolean register(ServiceEnvironment s) {
		URI uri = s.accessURL;

		if (!hosts.containsKey(uri)) {
			log.info(String.format("adding new ServiceEnvironment {}", uri));
		} else {
			// FIXME ? - replace regardless ???
			if (areEqual(s)) {
				log.info(String.format("ServiceEnvironment {} already exists - with same count and names", uri));
				return false;
			}
			log.info(String.format("replacing ServiceEnvironment {}", uri.toString()));
		}

		hosts.put(uri, s);

		Iterator<String> it = s.serviceDirectory.keySet().iterator();
		String serviceName;
		while (it.hasNext()) {
			serviceName = it.next();
			log.info(String.format("adding %s to registry", serviceName));
			ServiceInterface si = s.serviceDirectory.get(serviceName);
			// IMPORTANT - s.accessURL == service.host !! NORMALIZE !!!
			si.setHost(uri);

			registry.put(serviceName, si);
			runtime.invoke("registered", s.serviceDirectory.get(serviceName));

			// if I find a runtime - subscribe this Runtime to remote Runtime
			if ("org.myrobotlab.service.Runtime".equals(s.getClass().getCanonicalName())) {
				log.info(String.format("found runtime %s", serviceName));
				runtime.subscribe("registered", serviceName, "register", ServiceInterface.class);
			}

		}

		return true;
	}

	/**
	 * Checks if s has the same service directory content as the environment at
	 * url.
	 * 
	 * @param s
	 * @param uri
	 * @return
	 */
	private static boolean areEqual(ServiceEnvironment s) {
		URI url = s.accessURL;
		ServiceEnvironment se = hosts.get(url);
		if (se.serviceDirectory.size() != s.serviceDirectory.size()) {
			return false;
		}

		s.serviceDirectory.keySet().iterator();
		Iterator<String> it = s.serviceDirectory.keySet().iterator();
		String serviceName;
		while (it.hasNext()) {
			serviceName = it.next();
			if (!se.serviceDirectory.containsKey(serviceName)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Gets the current total number of services registered services. This is
	 * the number of services in all Service Environments
	 * 
	 * @return total number of services
	 */
	public int getServiceCount() {
		int cnt = 0;
		Iterator<URI> it = hosts.keySet().iterator();
		ServiceEnvironment se;
		Iterator<String> it2;
		while (it.hasNext()) {
			se = hosts.get(it.next());
			it2 = se.serviceDirectory.keySet().iterator();
			while (it2.hasNext()) {
				++cnt;
				it2.next();
			}
		}
		return cnt;
	}

	/**
	 * 
	 * @return
	 */
	public int getServiceEnvironmentCount() {
		return hosts.size();
	}

	/**
	 * 
	 * @return
	 */
	public static ServiceEnvironment getLocalServices() {
		if (!hosts.containsKey(null)) {
			runtime.error("local (null) ServiceEnvironment does not exist");
			return null;
		}

		return hosts.get(null);
	}

	/**
	 * getLocalServicesForExport returns a filtered map of Service references to
	 * export to another instance of MRL. The objective of filtering may help
	 * resolve functionality, security, or technical issues. For example, the
	 * Dalvik JVM can only run certain Services. It would be error prone to
	 * export a GUIService to a jvm which does not support swing.
	 * 
	 * Since the map of Services is made for export - it is NOT a copy but
	 * references
	 * 
	 * The filtering is done by Service Type.. although in the future it could
	 * be extended to Service.getName()
	 * 
	 * @return
	 * 
	 */
	public static ServiceEnvironment getLocalServicesForExport() {
		if (!hosts.containsKey(null)) {
			runtime.error("local (null) ServiceEnvironment does not exist");
			return null;
		}

		ServiceEnvironment local = hosts.get(null);

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
														// when XMPP or Remote
														// Adapter pull it -
														// they have to reset
														// this !!
			} else {
				log.info(String.format("security prevents export of %s", name));
				continue;
			}
		}

		return export;
	}

	public static boolean isLocal(String serviceName) {
		ServiceInterface sw = getService(serviceName);
		return sw.isLocal();
	}

	/**
	 * releases a service - stops the service, its threads, releases its
	 * resources, and removes registry entries
	 * 
	 * @param name
	 *            of the service to be released
	 * @return whether or not it successfully released the service
	 */
	public synchronized static boolean release(String name) /*
															 * release local
															 * Service
															 */
	{
		log.warn("releasing service {}", name);
		Runtime rt = getInstance();
		if (!registry.containsKey(name)) {
			rt.error("release could not find %s", name);
			return false;
		}
		ServiceInterface sw = registry.remove(name);
		if (sw.isLocal()){
			sw.stopService();
		}
		ServiceEnvironment se = hosts.get(sw.getHost());
		se.serviceDirectory.remove(name);
		rt.invoke("released", sw);
		log.warn("released{}", name);
		return true;
	}

	/**
	 * 
	 * @param url
	 * @return
	 */
	public static boolean release(URI url) /* release process environment */
	{
		boolean ret = true;
		ServiceEnvironment se = hosts.get(url);
		if (se == null) {
			log.warn("attempt to release {} not successful - it does not exist", url);
			return false;
		}
		log.info(String.format("releasing url %1$s", url));
		String[] services = (String[]) se.serviceDirectory.keySet().toArray(new String[se.serviceDirectory.keySet().size()]);
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
	 * FIXME FIXME FIXME - just call release on each - possibly saving runtime
	 * for last .. send prepareForRelease before releasing
	 * 
	 * release all local services
	 * 
	 * FIXME - there "should" be an order to releasing the correct way would be
	 * to save the Runtime for last and broadcast all the services being
	 * released
	 * 
	 * FIXME - send SHUTDOWN event to all running services with a timeout period
	 * - end with System.exit() FIXME normalize with releaseAllLocal and
	 * releaseAllExcept
	 */
	public static void releaseAll() /* local only? YES !!! LOCAL ONLY !! */
	{
		log.debug("releaseAll");

		ServiceEnvironment se = hosts.get(null); // local services only
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

			log.info(String.format("stopping service %s/%s", se.accessURL, serviceName));

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
				runtime.error("%s threw while stopping");
				Logging.logException(e);
			}
		}

		runtime.stopService();

		log.info("clearing hosts environments");
		hosts.clear();

		log.info("clearing registry");
		registry.clear();

		// exit () ?
	}

	/**
	 * 
	 * @return
	 */
	public static HashMap<String, ServiceInterface> getRegistry() {
		return registry;// FIXME should return copy
	}

	/**
	 * 
	 * @param url
	 * @return
	 */
	public static ServiceEnvironment getServiceEnvironment(URI url) {
		if (hosts.containsKey(url)) {
			return hosts.get(url); // FIXME should return copy
		}
		return null;
	}

	/**
	 * 
	 * @return
	 */
	public static HashMap<URI, ServiceEnvironment> getServiceEnvironments() {
		return new HashMap<URI, ServiceEnvironment>(hosts);
	}

	/**
	 * 
	 * @param serviceName
	 * @return
	 */
	public static HashMap<String, MethodEntry> getMethodMap(String serviceName) {
		if (!registry.containsKey(serviceName)) {
			runtime.error(String.format("%1$s not in registry - can not return method map", serviceName));
			return null;
		}

		HashMap<String, MethodEntry> ret = new HashMap<String, MethodEntry>();
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
			me = new MethodEntry();
			me.name = m.getName();
			me.parameterTypes = m.getParameterTypes();
			me.returnType = m.getReturnType();
			s = MethodEntry.getSignature(me.name, me.parameterTypes, me.returnType);
			ret.put(s, me);
		}

		return ret;
	}

	public static void dumpToFile() {
		FileIO.stringToFile(String.format("serviceRegistry.%s.txt", runtime.getName()), Runtime.dump());
		FileIO.stringToFile(String.format("notifyEntries.%s.xml", runtime.getName()), Runtime.dumpNotifyEntries());
	}

	/**
	 * a method which returns a xml representation of all the listeners and
	 * routes in the runtime system
	 * 
	 * @return
	 */
	public static String dumpNotifyEntries() {
		ServiceEnvironment se = getLocalServices();

		Map<String, ServiceInterface> sorted = new TreeMap<String, ServiceInterface>(se.serviceDirectory);

		Iterator<String> it = sorted.keySet().iterator();
		String serviceName;
		ServiceInterface sw;
		String n;
		ArrayList<MRLListener> nes;
		MRLListener listener;

		StringBuffer sb = new StringBuffer().append("<NotifyEntries>");
		while (it.hasNext()) {
			serviceName = it.next();
			sw = sorted.get(serviceName);
			sb.append("<service name=\"").append(sw.getName()).append("\" serviceEnironment=\"").append(sw.getHost()).append("\">");
			ArrayList<String> nlks = sw.getNotifyListKeySet();
			if (nlks != null) {
				Iterator<String> nit = nlks.iterator();

				while (nit.hasNext()) {
					n = nit.next();
					sb.append("<addListener map=\"").append(n).append("\">");
					nes = sw.getNotifyList(n);
					for (int i = 0; i < nes.size(); ++i) {
						listener = nes.get(i);
						sb.append("<MRLListener outMethod=\"").append(listener.outMethod).append("\" name=\"").append(listener.name).append("\" inMethod=\"")
								.append(listener.outMethod).append("\" />");
					}
					sb.append("</addListener>");
				}
			}
			sb.append("</service>");

		}
		sb.append("</NotifyEntries>");

		return sb.toString();
	}

	/**
	 * WTF did you use a vector ?!?!?
	 * 
	 * @param interfaceName
	 * @return
	 */
	public static Vector<String> getServicesFromInterface(String interfaceName) {
		Vector<String> ret = new Vector<String>();

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

				if (m.getCanonicalName().equals(interfaceName)) {
					ret.add(sw.getName());
				}
			}
		}

		return ret;
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

	// ---------------- callback events begin -------------
	/**
	 * registration event
	 * 
	 * @param filename
	 *            - the name of the Service which was successfully registered
	 * @return
	 */
	public ServiceInterface registered(ServiceInterface sw) {
		return sw;
	}

	/**
	 * release event
	 * 
	 * @param filename
	 *            - the name of the Service which was successfully released
	 * @return
	 */
	public ServiceInterface released(ServiceInterface sw) {
		return sw;
	}

	/**
	 * collision event - when a registration is attempted but there is a name
	 * collision
	 * 
	 * @param name
	 *            - the name of the two Services with the same name
	 * @return
	 */
	public String collision(String name) {
		return name;
	}

	// ---------------- callback events end -------------

	// ---------------- Runtime begin --------------

	/**
	 * prints help to the console
	 */
	static void mainHelp() {
		System.out.println(String.format("Runtime %s", FileIO.resourceToString("version.txt")));
		System.out.println("-h --help			                       # help ");
		System.out.println("-v --version		                       # print version");
		System.out.println("-update   			                       # update myrobotlab");
		System.out.println("-invoke name method [param1 param2 ...]    # invoke a method of a service");
		System.out.println("-install [ServiceType1 ServiceType2 ...]   # install services - if no ServiceTypes are specified install all");
		System.out.println("-runtimeName <runtime name>                # rename the Runtime service - prevents multiple instance name collisions");
		System.out.println("-logToConsole                              # redirects logging to console");
		System.out.println("-logLevel <DEBUG | INFO | WARNING | ERROR> # log level");
		System.out.println("-service <name1 Type1 name2 Type2 ...>     # create and start list of services, e.g. -service gui GUIService");
		System.out.println("example:");
		String helpString = "java -Djava.library.path=./libraries/native/x86.32.windows org.myrobotlab.service.Runtime -service gui GUIService -logLevel INFO -logToConsole";
		System.out.println(helpString);
	}

	/**
	 * creates and starts service from a cmd line object
	 * 
	 * @param cmdline
	 *            data object from the cmd line
	 */
	public final static void createAndStartServices(CMDLine cmdline) {

		System.out.println(String.format("createAndStartServices service count %1$d", cmdline.getArgumentCount("-service") / 2));

		if (cmdline.getArgumentCount("-service") > 0 && cmdline.getArgumentCount("-service") % 2 == 0) {

			for (int i = 0; i < cmdline.getArgumentCount("-service"); i += 2) {

				log.info(String.format("attempting to invoke : org.myrobotlab.service.%1$s named %2$s", cmdline.getSafeArgument("-service", i + 1, ""),
						cmdline.getSafeArgument("-service", i, "")));

				String name = cmdline.getSafeArgument("-service", i, "");
				String type = cmdline.getSafeArgument("-service", i + 1, "");
				ServiceInterface s = Runtime.create(name, type);

				if (s != null) {
					s.startService();
				} else {
					runtime.error(String.format("could not create service %1$s %2$s", name, type));
				}

			}
			return;
		} /*
		 * LIST ???
		 * 
		 * else if (cmdline.hasSwitch("-list")) { Runtime runtime =
		 * Runtime.getInstance(); if (runtime == null) {
		 * 
		 * } else { System.out.println(getServiceTypeNames()); } return; }
		 */
		mainHelp();
	}

	/**
	 * Returns an array of all the simple type names of all the possible
	 * services. The data originates from the repo's serviceData.xml file
	 * https:/
	 * /code.google.com/p/myrobotlab/source/browse/trunk/myrobotlab/thirdParty
	 * /repo/serviceData.xml
	 * 
	 * There is a local one distributed with the install zip When a "update" is
	 * forced, MRL will try to download the latest copy from the repo.
	 * 
	 * The serviceData.xml lists all service types, dependencies, categories and
	 * other relevant information regarding service creation
	 * 
	 * @return
	 */
	public String[] getServiceTypeNames() {
		return getServiceTypeNames("all");
	}

	/**
	 * publishing event to get the possible services currently available
	 * 
	 * @param filter
	 * @return
	 */
	public String[] getServiceTypeNames(String filter) {
		return runtime.repo.getServiceDataFile().getServiceTypeNames(filter);
	}

	/**
	 * 
	 * initially I thought that is would be a good idea to dynamically load
	 * Services and append their definitions to the class path. This would
	 * "theoretically" be done with ivy to get/download the appropriate
	 * dependent jars from the repo. Then use a custom ClassLoader to load the
	 * new service.
	 * 
	 * Ivy works for downloading the appropriate jars & artifacts However, the
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
	 * @return
	 */

	/**
	 * @param name
	 *            - name of Service to be removed and whos resources will be
	 *            released
	 */
	static public void releaseService(String name) {
		Runtime.release(name);
	}

	static public void invokeCommands(CMDLine cmdline) {
		int argCount = cmdline.getArgumentCount("-invoke");
		if (argCount > 1) {

			StringBuffer params = new StringBuffer();

			ArrayList<String> invokeList = cmdline.getArgumentList("-invoke");
			Object[] data = new Object[argCount - 2];
			for (int i = 2; i < argCount; ++i) {
				data[i - 2] = invokeList.get(i);
				params.append(String.format("%s ", invokeList.get(i)));
			}

			String name = cmdline.getArgument("-invoke", 0);
			String method = cmdline.getArgument("-invoke", 1);

			log.info(String.format("attempting to invoke : %s.%s(%s)\n", name, method, params.toString()));
			getInstance().send(name, method, data);

		}

	}

	static public ServiceInterface start(String name, String type) {
		return createAndStart(name, type);
	}

	static public ServiceInterface createAndStart(String name, String type) {
		ServiceInterface s = create(name, type);
		s.startService();
		return s;
	}

	/**
	 * 
	 * @param name
	 * @param type
	 * @return
	 */
	static public synchronized ServiceInterface create(String name, String type) {
		String fullTypeName;
		if (type.indexOf(".") == -1) {
			fullTypeName = String.format("org.myrobotlab.service.%s", type);
		} else {
			fullTypeName = type;
		}
		return createService(name, fullTypeName);
	}

	/**
	 * @param name
	 *            - name of Service
	 * @param pkgName
	 *            - package of Service in case Services are created in different
	 *            packages
	 * @param type
	 *            - type of Service
	 * @return
	 */
	/*
	 * static public synchronized ServiceInterface create(String name, String
	 * pkgName, String type) { try {
	 * log.debug("Runtime.create - Class.forName"); // get String Class String
	 * typeName = pkgName + type; // Class<?> cl = Class.forName(typeName); //
	 * Class<?> cl = Class.forName(typeName, false, //
	 * ClassLoader.getSystemClassLoader()); return createService(name,
	 * typeName); } catch (Exception e) { Logging.logException(e); } return
	 * null; }
	 */

	/**
	 * @param name
	 * @param cls
	 * @return
	 */
	static public synchronized ServiceInterface createService(String name, String fullTypeName) {
		log.info(String.format("Runtime.createService %s", name));
		if (name == null || name.length() == 0 || fullTypeName == null || fullTypeName.length() == 0) {
			log.error(String.format("%s not a type or %s not defined ", fullTypeName, name));
			return null;
		}

		ServiceInterface sw = Runtime.getService(name);
		if (sw != null) {
			log.debug(String.format("service %s already exists", name));
			return sw;
		}

		try {

			if (log.isDebugEnabled()) {
				// TODO - determine if there have been new classes added from
				// ivy
				log.debug("ABOUT TO LOAD CLASS");
				log.debug("loader for this class " + Runtime.class.getClassLoader().getClass().getCanonicalName());
				log.debug("parent " + Runtime.class.getClassLoader().getParent().getClass().getCanonicalName());
				log.debug("system class loader " + ClassLoader.getSystemClassLoader());
				log.debug("parent should be null" + ClassLoader.getSystemClassLoader().getParent().getClass().getCanonicalName());
				log.debug("thread context " + Thread.currentThread().getContextClassLoader().getClass().getCanonicalName());
				log.debug("thread context parent " + Thread.currentThread().getContextClassLoader().getParent().getClass().getCanonicalName());
			}

			// want to use only local instance runtime - but cant check to see
			// if local runtime exists
			if (!fullTypeName.equals("org.myrobotlab.service.Runtime")) {

				Repo repo = Runtime.getInstance().getRepo();

				if (!repo.isServiceTypeInstalled(fullTypeName)) {
					log.error(String.format("%s is not installed - please install it", fullTypeName));
					// return null;
				}

			}

			// create an instance
			Object newService = Service.getNewInstance(fullTypeName, name);
			log.info("returning {}", fullTypeName);
			return (Service) newService;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return null;
	}

	// ---------------- Runtime end --------------

	public static String dump() {
		StringBuffer sb = new StringBuffer().append("\nhosts:\n");
		Map<URI, ServiceEnvironment> sorted = hosts;
		Iterator<URI> hkeys = sorted.keySet().iterator();
		URI url;
		ServiceEnvironment se;
		Iterator<String> it2;
		String serviceName;
		ServiceInterface sw;
		while (hkeys.hasNext()) {
			url = hkeys.next();
			se = hosts.get(url);
			sb.append("\t").append(url);

			// good check :)
			if ((se.accessURL != url) && (!url.equals(se.accessURL))) {
				sb.append(" key not equal to data ").append(se.accessURL);
			}
			sb.append("\n");

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
			sb.append("\n").append(serviceName).append(" ").append(sw.getHost());
		}

		return sb.toString();
	}

	// ============== update begin ==============

	// ============== update end ==============

	// ============== update events begin ==============

	/**
	 * publishing point of Ivy sub system - sends event failedDependency when
	 * the retrieve report for a Service fails
	 * 
	 * @param dep
	 * @return
	 */
	public String failedDependency(String dep) {
		return dep;
	}

	/**
	 * this method is an event notifier that there were updates found
	 */
	public ServiceData proposedUpdates(ServiceData si) {
		return si;
	}

	/**
	 * published events
	 * 
	 * @param className
	 * @return
	 */
	public String resolveBegin(String className) {
		return className;
	}

	/**
	 * 
	 * @param errors
	 * @return
	 */
	public List<String> resolveError(List<String> errors) {
		return errors;
	}

	/**
	 * 
	 * @param className
	 * @return
	 */
	public String resolveSuccess(String className) {
		return className;
	}

	/**
	 * event fired when a new artifact is download
	 * 
	 * @param module
	 * @return
	 */
	public String newArtifactsDownloaded(String module) {
		return module;
	}

	public void resolveEnd() {
	}

	// FIXME - you don't need that many "typed" messages - resolve,
	// resolveError, ... etc
	// just use & parse "message"

	public static String message(String msg) {
		getInstance().invoke("publishMessage", msg);
		log.info(msg);
		return msg;
	}

	public String publishMessage(String msg) {
		return msg;
	}

	// ============== update events begin ==============

	// References :
	// http://java.dzone.com/articles/programmatically-restart-java
	// better - http://java.dzone.com/articles/programmatically-restart-java
	/*
	 * static public void restart(String restartScript) {
	 * log.info("restart - restart?"); Runtime.releaseAll(); try { Platform
	 * platform = Platform.getLocalInstance(); if (restartScript == null) { if
	 * (platform.isWindows()) {
	 * java.lang.Runtime.getRuntime().exec("cmd /c start myrobotlab.bat"); }
	 * else { java.lang.Runtime.getRuntime().exec("./myrobotlab.sh"); } } else {
	 * if (platform.isWindows()) {
	 * java.lang.Runtime.getRuntime().exec(String.format
	 * ("cmd /c start scripts\\%s.cmd", restartScript)); } else { String command
	 * = String.format("./scripts/%s.sh", restartScript); File exe = new
	 * File(command); // FIXME - NORMALIZE !!!!! if (!exe.setExecutable(true)) {
	 * log.error(String.format("could not set %s to executable permissions",
	 * command)); } java.lang.Runtime.getRuntime().exec(command); } } } catch
	 * (Exception ex) { Logging.logException(ex); } System.exit(0);
	 * 
	 * }
	 */

	// http://javafrustratzone.blogspot.com/2012/01/bash-shell-in-java-cannot-run-program.html
	// http://www.javaworld.com/article/2071275/core-java/when-runtime-exec---won-t.html
	// http://stackoverflow.com/questions/14165517/processbuilder-forwarding-stdout-and-stderr-of-started-processes-without-blocki
	// http://stackoverflow.com/questions/10954194/start-cmd-by-using-processbuilder
	// startShell(new String[]{"dir"});
	public static int startShell(String[] args) throws IOException {
		Platform platform = Platform.getLocalInstance();
		String[] shellArgs;
		if (platform.isWindows()) {
			shellArgs = new String[args.length + 2];
			shellArgs[0] = "cmd.exe";
			shellArgs[1] = "/c"; // "start" ???
			for (int i = 0; i < args.length; ++i) {
				shellArgs[i + 2] = args[i];
			}
		} else {
			shellArgs = new String[args.length + 2];
			shellArgs[0] = "/bin/bash";
			for (int i = 0; i < args.length; ++i) {
				shellArgs[i + 1] = args[i];
			}
		}
		// based on platform
		return startProcess(shellArgs);
	}

	public static int startProcess(String[] args) throws IOException {
		/*
		 * 
		 * ProcessBuilder testProcess = new ProcessBuilder(); Map environmentMap
		 * = testProcess.environment(); System.out.println(environmentMap);
		 * 
		 * // usual way System.out.println(System.getenv());
		 * 
		 * ProcessBuilder dirProcess = new ProcessBuilder("cmd"); File commands
		 * = new File("C:/process/commands.bat"); File dirOut = new
		 * File("C:/process/out.txt"); File dirErr = new
		 * File("C:/process/err.txt");
		 * 
		 * dirProcess.redirectInput(commands);
		 * dirProcess.redirectOutput(dirOut); dirProcess.redirectError(dirErr);
		 * 
		 * ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
		 * pb.redirectErrorStream(true); // use this to capture messages sent to
		 * stderr Process shell = pb.start(); InputStream shellIn =
		 * shell.getInputStream(); // this captures the output from the command
		 * int shellExitStatus = shell.waitFor(); // wait for the shell to
		 * finish and get the return code InputStreamReader reader = new
		 * InputStreamReader(shellIn); BufferedReader buf = new
		 * BufferedReader(reader); // Read lines using buf
		 */

		// SHOULD BE THREADED? PROLLY
		// ANDROID DOES NOT SUPPORT inheritIO()
		//Process p = new ProcessBuilder().inheritIO().command(args).start();
		Process p = new ProcessBuilder().command(args).start();
		// p.waitFor();
		// int rc = p.exitValue();
		return 0;
	}

	static class Move implements Runnable {

		File src;
		File dst;

		Move(File src, File dst) throws FileNotFoundException {
			this.src = src;
			this.dst = dst;
		}

		Move(String src, String dst) throws FileNotFoundException {
			this(new File(src), new File(dst));
		}

		@Override
		public void run() {
			try {
				// Files.move(src.toPath(), dst.toPath(),
				// StandardCopyOption.REPLACE_EXISTING);
				// NO NIO ON ANDROID !!!
				// Files.move(src.toPath(), dst.toPath(),
				// StandardCopyOption.REPLACE_EXISTING);
				FileIO.copy(src, dst);
			} catch (IOException e) {
				Logging.logException(e);
			}
		}

	}

	/**
	 * 
	 * should probably be deprecated - currently not used
	 * 
	 * @param runBeforeRestart
	 * 
	 *            will this work on a file lock update?
	 */
	static public void restart(Runnable runBeforeRestart) {
		final java.lang.Runtime r = java.lang.Runtime.getRuntime();
		log.info("restart - restart?");
		Runtime.releaseAll();
		try {
			// java binary
			String java = System.getProperty("java.home") + "/bin/java";
			// vm arguments
			/*
			 * getRuntimeMXBean scares me List<String> vmArguments =
			 * ManagementFactory.getRuntimeMXBean().getInputArguments();
			 * StringBuffer vmArgsOneLine = new StringBuffer(); for (String arg
			 * : vmArguments) { // if it's the agent argument : we ignore it
			 * otherwise the // address of the old application and the new one
			 * will be in conflict if (!arg.contains("-agentlib")) {
			 * vmArgsOneLine.append(arg); vmArgsOneLine.append(" "); } }
			 */
			// init the command to execute, add the vm args
			final StringBuffer cmd = new StringBuffer("\"" + java + "\" ");

			// program main and program arguments
			String[] mainCommand = globalArgs;
			// program main is a jar
			if (mainCommand[0].endsWith(".jar")) {
				// if it's a jar, add -jar mainJar
				cmd.append("-jar " + new File(mainCommand[0]).getPath());
			} else {
				// else it's a .class, add the classpath and mainClass
				cmd.append("-cp \"" + System.getProperty("java.class.path") + "\" " + mainCommand[0]);
			}
			// finally add program arguments
			for (int i = 1; i < mainCommand.length; i++) {
				cmd.append(" ");
				cmd.append(mainCommand[i]);
			}
			// execute the command in a shutdown hook, to be sure that all the
			// resources have been disposed before restarting the application

			r.addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						r.exec(cmd.toString());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			// execute some custom code before restarting
			if (runBeforeRestart != null) {
				runBeforeRestart.run();
			}
			// exit
		} catch (Exception ex) {
			Logging.logException(ex);
		}
		System.exit(0);

	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public static ServiceInterface getService(String name) {

		if (!registry.containsKey(name)) {
			log.debug(String.format("service %s does not exist", name));
			return null;
		}

		return registry.get(name);
	}

	// ============== configuration begin ==============

	/**
	 * save all configuration from all local services
	 * 
	 * @return
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

	/**
	 * load all configuration from all local services
	 * 
	 * @return
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

	// ============== configuration end ==============

	public static ArrayList<String> buildMLRCommandLine(HashMap<String, String> services, String runtimeName) {

		ArrayList<String> ret = new ArrayList<String>();

		Platform platform = Platform.getLocalInstance();

		// library path
		String classpath = String.format("%s.%s.%s", platform.getArch(), platform.getBitness(), platform.getOS());
		String libraryPath = String.format("-Djava.library.path=\"./libraries/native/%s\"", classpath);
		ret.add(libraryPath);

		// class path
		String systemClassPath = System.getProperty("java.class.path");
		ret.add("-classpath");
		String classPath = String.format("./libraries/jar/*%1$s./libraries/jar/%2$s/*%1$s%3$s", platform.getClassPathSeperator(), classpath, systemClassPath);
		ret.add(classPath);

		ret.add("org.myrobotlab.service.Runtime");

		// ----- application level params --------------

		// services
		if (services.size() > 0) {
			ret.add("-service");
			for (Map.Entry<String, String> o : services.entrySet()) {
				ret.add(o.getKey());
				ret.add(o.getValue());
			}
		}

		// runtime name
		if (runtimeName != null) {
			ret.add("-runtimeName");
			ret.add(runtimeName);
		}

		// logLevel

		// logToConsole
		// ret.add("-logToConsole");

		return ret;
	}

	/**
	 * unique id's are need for sendBlocking - to uniquely identify the message
	 * this is a method to support that - it is unique within a process, but not
	 * accross processes
	 * 
	 * @return a unique id
	 */
	public static final synchronized long getUniqueID() {
		++uniqueID;
		return uniqueID;
	}

	public boolean noWorky() {
		try {
			String ret = HTTPRequest.postFile("http://myrobotlab.org/myrobotlab_log/postLogFile.php", "runtime", "file", new File("myrobotlab.log"));
			if (ret.contains("Upload:")) {
				info("noWorky successfully sent - our crack team of experts will check it out !");
				return true;
			}
		} catch (Exception e) {
			Logging.logException(e);
		}
		error("the noWorky didn't worky !");
		return false;
	}

	public static List<ServiceInterface> getServices() {
		// QUESTION - why isn't registry just a treemap ?
		TreeMap<String, ServiceInterface> sorted = new TreeMap<String, ServiceInterface>(registry);
		List<ServiceInterface> list = new ArrayList<ServiceInterface>(sorted.values());
		return list;
	}

	// -------- network begin ------------------------

	/**
	 * although "fragile" since it relies on a external source - its useful to
	 * find the external ip address of NAT'd systems
	 * 
	 * @return external or routers ip
	 * @throws Exception
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
	 * gets all non-loopback, active, non-virtual ip addresses
	 * 
	 * @return list of local ipv4 IP addresses
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	static public ArrayList<String> getLocalAddresses() {
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
			Logging.logException(e);
		}
		return ret;
	}

	//@TargetApi(9)
	static public ArrayList<String> getLocalHardwareAddresses() {
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
				log.info(String.format("mac address : %s", m));

				/*
				 * StringBuilder sb = new StringBuilder(); for (int i = 0; i <
				 * mac.length; i++) { sb.append(String.format("%02X%s", mac[i],
				 * (i < mac.length - 1) ? "-" : "")); }
				 */

				ret.add(m);
				log.info("added mac");
			}
		} catch (Exception e) {
			Logging.logException(e);
		}

		log.info("done");
		return ret;
	}

	// -------- network end ------------------------

	// http://stackoverflow.com/questions/16610525/how-to-determine-if-graphicsenvironment-exists

	static public boolean isHeadless() {
		// String awt = "java.awt.GraphicsEnvironment";
		// java.awt.GraphicsEnvironment.isHeadless()
		// String nm = System.getProperty("java.awt.headless");
		// should return true if Linux != display
		String b = System.getProperty("java.awt.headless");
		return Boolean.parseBoolean(b);
	}

	/**
	 * Runtime's setLogLevel will set the root log level if its called from a
	 * service - it will only set that Service type's log level
	 * 
	 */
	public String setLogLevel(String level) {
		Logging logging = LoggingFactory.getInstance();
		logging.setLevel(level);
		return level;
	}

	public static boolean setJSONPrettyPrinting(boolean b) {
		return Encoder.setJSONPrettyPrinting(b);
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

	public Repo getRepo() {
		return repo;
	}

	public Platform getPlatform() {
		return repo.getPlatform();
	}

	// FIXME - this is important in the future
	@Override
	public void receive(Message msg) {
		// TODO Auto-generated method stub

	}

	static public Thread[] getThreads() {
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
		return threadArray;
	}

	/**
	 * Main starting method of MyRobotLab Parses command line options
	 * 
	 * -h help -v version -list jvm args -Dhttp.proxyHost=webproxy
	 * -Dhttp.proxyPort=80 -Dhttps.proxyHost=webproxy -Dhttps.proxyPort=80
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// global for this process
		globalArgs = args;

		CMDLine cmdline = new CMDLine();
		cmdline.splitLine(args);

		Logging logging = LoggingFactory.getInstance();

		try {

			if (cmdline.containsKey("-h") || cmdline.containsKey("--help")) {
				mainHelp();
				return;
			}

			if (cmdline.containsKey("-v") || cmdline.containsKey("--version")) {
				System.out.print(FileIO.resourceToString("version.txt"));
				return;
			}
			if (cmdline.containsKey("-runtimeName")) {
				runtimeName = cmdline.getSafeArgument("-runtimeName", 0, "MRL");
			}

			if (cmdline.containsKey("-logToConsole")) {
				logging.addAppender(Appender.CONSOLE);
			} else if (cmdline.containsKey("-logToRemote")) {
				String host = cmdline.getSafeArgument("-logToRemote", 0, "localhost");
				String port = cmdline.getSafeArgument("-logToRemote", 1, "4445");
				logging.addAppender(Appender.REMOTE, host, port);
			} else {
				if (cmdline.containsKey("-multiLog")) {
					logging.addAppender(Appender.FILE, "multiLog", null);
				} else {
					logging.addAppender(Appender.FILE);
				}
			}

			logging.setLevel(cmdline.getSafeArgument("-logLevel", 0, "INFO"));
			log.info(cmdline.toString());

			// LINUX LD_LIBRARY_PATH MUST BE EXPORTED - NO OTHER SOLUTION FOUND
			// hack to reconcile the different ways os handle and expect
			// "PATH & LD_LIBRARY_PATH" to be handled
			// found here -
			// http://blog.cedarsoft.com/2010/11/setting-java-library-path-programmatically/
			// but does not work

			if (cmdline.containsKey("-install")) {
				// force all updates
				ArrayList<String> services = cmdline.getArgumentList("-install");
				Repo repo = new Repo("install");
				if (services.size() == 0) {
					repo.retrieveAll();
					return;
				} else {
					for (int i = 0; i < services.size(); ++i) {
						repo.retrieveServiceType(services.get(i));
					}
				}
			}

			if (cmdline.containsKey("-update")) {
				// update myrobotlab
				runtime = Runtime.getInstance();
				runtime.update();

			}

			if (cmdline.containsKey("-service")) {
				createAndStartServices(cmdline);
			}

			if (cmdline.containsKey("-invoke")) {
				invokeCommands(cmdline);
			}

		} catch (Exception e) {
			Logging.logException(e);
			System.out.print(Logging.stackToString(e));
			Service.sleep(2000);
		}
	}

}
