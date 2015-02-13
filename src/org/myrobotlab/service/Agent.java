package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ProcessData;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

/**
 * @author GroG
 *
 *         Agent Smith is responsible for all Processes Just as Runtime is
 *         responsible for all Services Agent starts and prepares all processes
 *         for running MRL This includes environment variables, jvm arguments
 *         and cmdline arguments
 * 
 *         Agent can not use any JNI JNA or typically other Services with
 *         dependencies as the expectation is this remain a thin wrapper which
 *         controls other / more complex processes.
 * 
 *         java -jar myrobotlab.jar will start the one (and probably only) Agent
 *         process
 * 
 *         Although Runtime primarily processes command line parameters there
 *         are a few specific for Process directives
 * 
 * 
 *         Since -test needs a specialized cleaned environment its handled here
 * 
 *         Several modes exist - normal = set env and keep process in map, with
 *         re-directed stdin stdout & stderr streams envOnly = set the correct
 *         environment then terminate
 * 
 * 
 *         default is start a new process with relayed cmdline and redirect
 *         stdin stout & stderr streams, terminate if no subprocesses exist
 * 
 *         =================================================================== *
 *         References :
 *
 *         http://www.excelsior-usa.com/articles/java-to-exe.html
 *
 *         possible small wrappers mac / linux / windows
 *         http://mypomodoro.googlecode
 *         .com/svn-history/r89/trunk/src/main/java/org
 *         /mypomodoro/util/Restart.java
 *
 *         http://java.dzone.com/articles/programmatically-restart-java
 *         http://stackoverflow
 *         .com/questions/3468987/executing-another-application-from-java
 *
 *
 *         TODO - ARMV 6 7 8 ??? -
 *         http://www.binarytides.com/linux-cpu-information/ - lscpu
 *
 *         Architecture: armv7l Byte Order: Little Endian CPU(s): 4 On-line
 *         CPU(s) list: 0-3 Thread(s) per core: 1 Core(s) per socket: 1
 *         Socket(s): 4
 *
 *
 *         TODO - soft floating point vs hard floating point readelf -A
 *         /proc/self/exe | grep Tag_ABI_VFP_args soft = nothing hard =
 *         Tag_ABI_VFP_args: VFP registers
 *
 *         PACKAGING jsmooth - windows only javafx - 1.76u - more dependencies ?
 *         http://stackoverflow.com/questions/1967549/java-packaging-tools-
 *         alternatives-for-jsmooth-launch4j-onejar
 *
 *         TODO classpath order - for quick bleeding edge updates? rsync
 *         exploded classpath
 *
 *         TODO - check for Java 1.7 or > addShutdownHook check for network
 *         connectivity TODO - proxy -Dhttp.proxyHost=webproxy
 *         -Dhttp.proxyPort=80 -Dhttps.proxyHost=webproxy -Dhttps.proxyPort=80
 *         -Dhttp.proxyUserName="myusername" -Dhttp.proxyPassword="mypassword"
 * 
 *         TODO? how to get vm args http:*
 *         stackoverflow.com/questions/1490869/how-to-get
 *         -vm-arguments-from-inside-of-java-application http:*
 *         java.dzone.com/articles/programmatically-restart-java http:*
 *         stackoverflow.com
 *         /questions/9911686/getresource-some-jar-returns-null-although
 *         -some-jar-exists-in-geturls RuntimeMXBean runtimeMxBean =
 *         ManagementFactory.getRuntimeMXBean(); List<String> arguments =
 *         runtimeMxBean.getInputArguments();
 *         
 *         TODO - on java -jar myrobotlab.jar | make a copy if agent.jar does not exist..
 *         if it does then spawn the Agent there ... it would make upgrading myrobotlab.jar "trivial" !!!
 * 
 */
public class Agent extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Agent.class);

	private static HashMap<String, ProcessData> processes = new HashMap<String, ProcessData>();
	
	private Set<String> clientJVMArgs = new HashSet<String>();
	private List<String> agentJVMArgs = new ArrayList<String>();
	
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("cli", "CLI", "Command line processor");
		return peers;
	}

	public Agent(String n) {
		super(n);
		log.info("Agent {} PID {} is alive", n, Runtime.getPID());
		agentJVMArgs = Runtime.getJVMArgs();
		log.info("jvmArgs {}", Arrays.toString(agentJVMArgs.toArray()));
		for(int i = 0; i < agentJVMArgs.size(); ++i){
			String agentJVMArg = agentJVMArgs.get(i);
			if (!agentJVMArg.startsWith("-agent") && !agentJVMArg.startsWith("-Dfile.encoding")){
				clientJVMArgs.add(agentJVMArg);
			}
		}
	}

	static public String formatList(ArrayList<String> args) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.size(); ++i) {
			log.info(args.get(i));
			sb.append(String.format("%s ", args.get(i)));
		}
		return sb.toString();
	}
	
	/**
	 * get a list of all the processes currently governed by this Agent
	 * @return
	 */
	public ProcessData[] getProcesses(){
		Object[] objs = processes.values().toArray();
		ProcessData[] pd = new ProcessData[objs.length];
		for (int i = 0; i < objs.length; ++i){
			pd[i] = (ProcessData)objs[i];
		}
		return pd;
	}

	/**
	 * create new "named" MRL instance
	 * 
	 * @param name
	 *            - unique runtime name
	 * @throws InterruptedException
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public Process spawn(String name) throws IOException, URISyntaxException, InterruptedException {
		return spawn(new String[] { "-runtimeName", name });
	}

	/**
	 * Responsibility - This method will always call Runtime. To start Runtime
	 * correctly environment must correctly be setup
	 */
	private synchronized Process spawn(String[] in) throws IOException, URISyntaxException, InterruptedException {
		log.info("============== spawn begin ==============");
		// get runtimeName
		CMDLine cmdline = new CMDLine(in);
		String runtimeName = cmdline.getSafeArgument("-runtimeName", 0, "runtime");

		if (processes.containsKey(runtimeName)) {
			error("%s already in processes - rejecting spawn request", runtimeName);
			return null;
		}

		// step 1 - get current env data
		String ps = File.pathSeparator;
		String fs = System.getProperty("file.separator");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd:HH:mm:ss");
		log.info(String.format("Agent starting spawn %s", formatter.format(new Date())));
		log.info("in args {}", Arrays.toString(in));

		// FIXME - details on space / %20 decoding in URI
		// http://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file
		String protectedDomain = URLDecoder.decode(Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(), "UTF-8");
		log.info("protected domain {}", protectedDomain);

		// platform id
		Platform platform = Platform.getLocalInstance();
		String platformId = platform.getPlatformId();
		log.info("platform {}", platformId);

		ArrayList<String> outArgs = new ArrayList<String>();
		String classpath = String.format("./%s./myrobotlab.jar%s./libraries/jar/*", ps, ps);

		String javaExe = platform.isWindows() ? "javaw" : "java";

		String javaPath = System.getProperty("java.home") + fs + "bin" + fs + javaExe;
		// JNI
		String jniLibraryPath = String.format("-Djava.library.path=libraries/native%slibraries/native/%s", ps, platformId);

		// String jvmMemory = "-Xmx2048m -Xms256m";
		long totalMemory = Runtime.getTotalPhysicalMemory();
		if (totalMemory == 0) {
			log.info("could not get total physical memory");
		} else {
			log.info("total physical memory returned is {} Mb", totalMemory / 1048576);
		}

		outArgs.add(javaPath);
		
		// jvm args relayed to clients
		for (String jvmArg : clientJVMArgs) {
			outArgs.add(jvmArg);
		}
		
		outArgs.add(jniLibraryPath);
		outArgs.add("-cp");
		outArgs.add(classpath);

		boolean hasService = false;
		/*
		 * BUG - when specifying an "-invoke test test
		 * org.myrobotlab.service.Clock for (int i = 0; i < in.length; ++i) {
		 * String arg = in[i]; if (arg.startsWith("org.myrobotlab.service")) {
		 * hasService = true; } }
		 */

		if (!hasService) {
			outArgs.add("org.myrobotlab.service.Runtime");
		}

		// TODO preserve/serialize command line parameters
		if (in.length > 0) {
			for (int i = 0; i < in.length; ++i) {
				outArgs.add(in[i]);
			}
		} else {
			// (default) - no parameters supplied

			outArgs.add("-service");
			outArgs.add("gui");
			outArgs.add("GUIService");
			outArgs.add("python");
			outArgs.add("Python");
		}

		// ProcessBuilder builder = new ProcessBuilder(path, "-Xmx1024m", "-cp",
		// classpath, ReSpawner.class.getName());

		// FIXME - non-normal operation .. Agent is being
		File update = new File("./update/myrobotlab.jar");

		if (update.exists()) {
			// attempt to process the update
			log.info("update exists archiving current");

			try {
				// if thrown - "file locked" then createBootstrapJar
				// IF THAT THROWS - GIVE UP !!!

				// update available - archive old file
				// Path source = Paths.get("./myrobotlab.jar");
				File archiveDir = new File("./archive");
				archiveDir.mkdirs();

				File source = new File("./myrobotlab.jar");
				File target = new File(String.format("./archive/myrobotlab.%s.jar", Runtime.getVersion()));
				FileIO.copy(source, target);

				// copy update
				log.info("moving update");
				source = new File("./update/myrobotlab.jar");
				target = new File("./myrobotlab.jar");
				FileIO.copy(source, target);
				log.info("deleting update");
				if (!source.delete()) {
					log.error("could not delete update");
				}
				log.info("completed update !");
			} catch (Exception e) {
				try {
					// FIXME FIXME - normalize the start !!!!
					log.info("file myrobotlab.jar is locked - ejecting agent.jar - {}", e.getMessage());

					File source = new File("./myrobotlab.jar");
					File target = new File("./agent.jar");
					FileIO.copy(source, target);

					ArrayList<String> bootArgs = new ArrayList<String>();

					bootArgs.add(javaPath);
					bootArgs.add("-jar");
					bootArgs.add("./agent.jar"); // -jar uses manifest
					// bootArgs.add("org.myrobotlab.framework.Bootstrap");
					for (int i = 0; i < in.length; ++i) {
						bootArgs.add(in[i]);
					}
					String cmd = formatList(bootArgs);
					log.info(String.format("agent.jar spawning -> [%s]", cmd));
					ProcessBuilder builder = new ProcessBuilder(bootArgs);
					Process process = builder.start();

					log.info(String.format("terminating - good luck new agent & update :)"));
					System.exit(0);
					return process;
				} catch (Exception ex) {
					log.error("PANIC - failed to create agent - terminating - bye :(");
					log.error(ex.getMessage());
				}
			}
		}

		String cmd = formatList(outArgs);
		log.info(String.format("spawning -> [%s]", cmd));

		ProcessBuilder builder = new ProcessBuilder(outArgs);// .inheritIO();

		// environment variables setup
		Map<String, String> env = builder.environment();
		if (platform.isLinux()) {
			String ldPath = String.format("'pwd'/libraries/native:'pwd'/libraries/native/%s:${LD_LIBRARY_PATH}", platformId);
			env.put("LD_LIBRARY_PATH", ldPath);
		} else if (platform.isMac()) {
			String dyPath = String.format("'pwd'/libraries/native:'pwd'/libraries/native/%s:${DYLD_LIBRARY_PATH}", platformId);
			env.put("DYLD_LIBRARY_PATH", dyPath);
		} else if (platform.isWindows()) {
			String path = String.format("PATH=%%CD%%\\libraries\\native;PATH=%%CD%%\\libraries\\native\\%s;%%PATH%%", platformId);
			env.put("PATH", path);
		} else {
			log.error("unkown operating system");
		}

		Process process = builder.start();
		processes.put(runtimeName, new ProcessData(this, runtimeName, process));

		// attach our cli to the latest instance
		CLI cli = Runtime.getCLI();
		if (cli != null) {
			cli.add(runtimeName, process.getInputStream(), process.getOutputStream());
			cli.attach(runtimeName);
		}

		log.info("Agent finished spawn {}", formatter.format(new Date()));
		return process;
	}

	public String publishTerminated(String name) {
		info("terminated %s", name);
		return name;
	}

	public Status test() {
		Status status = Status.info("agent test begin");

		try {
			// JUnitCore junit = new JUnitCore();
			// Result result = junit.run(testClasses);

			/*
			 * Bootstrap test = new BootstrapHotSpot(); // spawn mrl instance
			 * Process process = test.spawn(new String[]{"-service", "test",
			 * "Test"}); //Process process = test.spawn(new String[]{});
			 */

			// process.destroy();
		} catch (Exception e) {
			Logging.logException(e);
		}

		return status;
	}

	@Override
	public String getDescription() {
		return "Agent (Smith) - responsible for creating the environment and maintaining, tracking and terminating all processes";
	}

	public void terminate() {
		terminateProcesses();
		log.info("terminating self ... goodbye...");
		System.exit(0);
	}

	public void terminateSelfOnly() {
		log.info("goodbye .. cruel world");
		System.exit(0);
	}

	public void terminateProcesses() {
		for (String name : processes.keySet()) {
			terminate(name);
		}
		log.info("no survivors sir...");
	}

	public String terminate(String name) {
		if (processes.containsKey(name)) {
			info("terminating %s", name);
			processes.get(name).process.destroy();
			processes.remove(name);
			info("%s haz beeen terminated", name);
			return name;
		}

		warn("%s? no sir, I don't know that punk...", name);
		return null;
	}

	public static Status install(String fullType) {
		Status status = Status.info("install %s", fullType);
		try {
			Repo repo = new Repo();

			if (!repo.isServiceTypeInstalled(fullType)) {
				repo.install(fullType);
				if (repo.hasErrors()) {
					status.addError(repo.getErrors());
				}

			} else {
				log.info("installed {}", fullType);
			}
		} catch (Exception e) {
			status.addError(e);
		}
		return status;
	}

	public Status serviceTest() {

		String[] serviceTypeNames = Runtime.getInstance().getServiceTypeNames();

		HashSet<String> skipTest = new HashSet<String>();
		skipTest.add("org.myrobotlab.service.Agent");
		skipTest.add("org.myrobotlab.service.Runtime");
		skipTest.add("org.myrobotlab.service.Incubator");
		skipTest.add("org.myrobotlab.service.Test");
		skipTest.add("org.myrobotlab.service.CLI"); // ?? No ?

		Status status = Status.info("serviceTest will test %d services", serviceTypeNames.length);
		long startTime = System.currentTimeMillis();
		status.addNamedInfo("startTime", "%d", startTime);

		long installTime = 0;
		
		for (int i = 0; i < serviceTypeNames.length; ++i) {

			String serviceType = serviceTypeNames[i];

			if (skipTest.contains(serviceType)) {
				log.info("skipping %s", serviceType);
				continue;
			}

			try {

				// agent.serviceTest(); // WTF?
				status.addInfo("perparing clean environment for %s", serviceType);

				// clean environment
				// FIXME - optimize clean
				
				Repo repo = Runtime.getInstance().getRepo();
				// SUPER CLEAN - force .repo to clear !!
				//repo.clearRepo();
				
				// less clean but faster
				//repo.clearLibraries();
				//repo.clearServiceData();
				
				// comment all out for dirty

				// install Test dependencies
				boolean force = true;
				long installStartTime = System.currentTimeMillis();
				repo.install("org.myrobotlab.service.Test", force);
				repo.install(serviceType, force);
				installTime += System.currentTimeMillis() - installStartTime;
				// clean test.json part file

				// spawn a test - attach to cli - test 1 service end to end
				// ,"-invoke", "test","test","org.myrobotlab.service.Clock"
				Process process = spawn(new String[] { "-runtimeName", "testEnv", "-service", "test", "Test", "-logLevel", "WARN", "-invoke", "test", "test", serviceType });
				
				process.waitFor();

				// destroy - start again next service
				// wait for partFile report .. test.json
				// NOT NEEDED - foriegn process has ended
				byte[] data = FileIO.loadPartFile("test.json", 60000);
				if (data != null) {
					String test = new String(data);
					Status testResult = Encoder.gson.fromJson(test, Status.class);
					status.add(testResult);
				}
				// destroy env
				terminate("testEnv");

			} catch (Exception e) {
				status.addError("ERROR - %s", serviceType);
				status.addError(e);
				continue;
			}
		}
		
		status.addNamedInfo("installTime", "%d", installTime);
		status.addNamedInfo("testTimeMs", "%d", System.currentTimeMillis() - startTime);
		status.addNamedInfo("testTimeMinutes", "%d", TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - startTime));
		status.addNamedInfo("endTime", "%d", System.currentTimeMillis());

		try {
			FileIO.savePartFile("fullTest.json", Encoder.gson.toJson(status).getBytes());
		} catch (Exception e) {
			Logging.logException(e);
		}

		return status;
	}

	/**
	 * First method JVM executes when myrobotlab.jar is in jar form.
	 * 
	 * -agent "-logLevel DEBUG -service webgui WebGUI"
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.out.println("Agent.main starting");

			// split agent commands from runtime commands
			String[] agentArgs = new String[0];
			// -agent \"-params -service ... \" string encoded
			CMDLine runtimeArgs = new CMDLine(args);
			// -service for Runtime -process a b c d :)
			if (runtimeArgs.containsKey("-agent")) {
				// List<String> list = runtimeArgs.getArgumentList("-agent");

				String tmp = runtimeArgs.getArgument("-agent", 0);
				agentArgs = tmp.split(" ");
				/*
				 * agentArgs = new String[list.size()]; for (int i = 0; i <
				 * list.size(); ++i){ agentArgs[i] =
				 * String.format("-%s",list.get(i)); }
				 */
			}

			CMDLine agentCmd = new CMDLine(agentArgs);

			Runtime.setRuntimeName("smith");
			Runtime.main(agentArgs);
			Agent agent = (Agent) Runtime.start("agent", "Agent");

			if (agentCmd.containsKey("-test")) {
				agent.serviceTest();

			} else {
				agent.spawn(args); // <-- agent's is now in charge of first mrl
									// instance
			}

		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			// big hammer
			System.out.println("Agent.main leaving");
			// System.exit(0);
		}
	}

}
