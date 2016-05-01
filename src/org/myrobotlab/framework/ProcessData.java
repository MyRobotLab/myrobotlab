package org.myrobotlab.framework;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.Invoker;
import org.slf4j.Logger;

/**
 * Simple class representing an operating system process
 * 
 * @author GroG
 *
 */
public class ProcessData implements Serializable {
	public final static Logger log = LoggerFactory.getLogger(ProcessData.class);

	private static final long serialVersionUID = 1L;

	public static final String STATE_RUNNING = "running";
	public static final String STATE_STOPPED = "stopped";
	public static final String STATE_UNKNOWN = "unknown";

	// TODO - need to start using id
	public Integer id;
	public String branch;
	public String name;
	public String version;
	public Long startTs = null;
	public Long stopTs = null;

	public String javaExe = null;

	// public String classpath = null;
	public String jniLibraryPath = null;
	public String jnaLibraryPath = null;
	public String Xmx = null;

	boolean userDefinedServices = false;
	// TODO - additional jvm args
	// public String jvmArgs = Runtime.getJVMArgs();

	// public boolean isRunning = false;
	public String state = STATE_STOPPED; // running | stopped | unknown
	// public List<String> cmdLine = new ArrayList<String> ();

	transient public Process process;
	transient public Monitor monitor;
	transient public Invoker service;

	ArrayList<String> in = null;

	// public boolean autoUpdate = false;

	public static class Monitor extends Thread {
		ProcessData data;

		public Monitor(ProcessData pd) {
			super(String.format("%s.monitor", pd.name));
			this.data = pd;
		}

		@Override
		public void run() {
			try {
				if (data.process != null) {
					// data.isRunning = true;
					data.state = STATE_RUNNING;
					data.state = "running";
					data.process.waitFor();
				}
			} catch (Exception e) {
			}

			// FIXME - invoke("terminatedProcess(name))
			data.service.invoke("publishTerminated", data.id);
			data.state = STATE_STOPPED;
			data.state = "stopped";
		}

	}

	public ProcessData(Invoker service, Integer id, String branch, String version, String name, Process process) {
		this.id = id;
		this.service = service;
		this.name = name;
		this.branch = branch;
		this.version = version;
		this.process = process;
	}

	/**
	 * copy of a ProcessData - threaded data will not be copied
	 * 
	 * @param pd
	 */
	public ProcessData(ProcessData pd) {
		this.id = pd.id;
		this.service = pd.service;
		this.name = pd.name;
		this.branch = pd.branch;
		this.version = pd.version;
		
		this.javaExe = pd.javaExe;
		this.jniLibraryPath = pd.jniLibraryPath;
		this.version = pd.version;
		this.jnaLibraryPath = pd.jnaLibraryPath;

		this.userDefinedServices = pd.userDefinedServices;

		if (pd.in != null){
			this.in = new ArrayList<String>();
			for (int i = 0; i < pd.in.size(); ++i){
				this.in.add(pd.in.get(i));
			}
		}
		
		// this.process = pd.process;
		// this.startTs = System.currentTimeMillis();
		// monitor = new Monitor(this);
		// monitor.start();
	}

	/**
	 * convert an String[] into a valid ProcessData
	 * 
	 * @param inCmdLine
	 * @param defaultBranch
	 * @param defaultVersion
	 */
	public ProcessData(Invoker service, String[] inCmdLine, String defaultBranch, String defaultVersion) {
		this.service = service;

		// String protectedDomain =
		// URLDecoder.decode(Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(),
		// "UTF-8");
		// log.info("protected domain {}", protectedDomain);

		// convert to ArrayList to process
		in = new ArrayList<String>();

		for (int i = 0; i < inCmdLine.length; ++i) {
			String cmd = inCmdLine[i];
			if (cmd.equals("-runtimeName")) {
				name = inCmdLine[i + 1];
				continue;
			}

			if (cmd.equals("-branch")) {
				branch = inCmdLine[i + 1];
				continue;
			}

			if (cmd.equals("-version")) {
				version = inCmdLine[i + 1];
				continue;
			}

			if (cmd.equals("-service")) {
				userDefinedServices = true;
			}

			// additional parameters
			in.add(inCmdLine[i]);
		}

		name = (name == null) ? "runtime" : name;
		branch = (branch == null) ? defaultBranch : branch;
		version = (version == null) ? defaultVersion : version;

		// step 1 - get current env data
		String ps = File.pathSeparator;
		String fs = File.separator;

		Platform platform = Platform.getLocalInstance();
		String exeName = platform.isWindows() ? "javaw" : "java";

		javaExe = String.format("%s%sbin%s%s", System.getProperty("java.home"), fs, fs, exeName);

		// jniLibraryPath =
		// String.format("-Djava.library.path=libraries/native%slibraries/native/%s",
		// ps, platform.getPlatformId());
		jniLibraryPath = "-Djava.library.path=libraries/native";
		jnaLibraryPath = "-Djna.library.path=libraries/native";

		/*
		 * TODO - relay additional JVM Args !!! or do you have to do this since
		 * additional parameters are simply appended !!!
		 * 
		 * Set<String> clientJVMArgs = new HashSet<String>();
		 * log.info("jvmArgs {}", Arrays.toString(agentJVMArgs.toArray())); for
		 * (int i = 0; i < agentJVMArgs.size(); ++i) { String agentJVMArg =
		 * agentJVMArgs.get(i); if (!agentJVMArg.startsWith("-agent") &&
		 * !agentJVMArg.startsWith("-Dfile.encoding")) {
		 * clientJVMArgs.add(agentJVMArg); } }
		 * 
		 * // jvm args relayed to clients for (String jvmArg : clientJVMArgs) {
		 * cmd.add(jvmArg); }
		 */

	}

	public boolean isRunning() {
		return STATE_RUNNING.equals(state);
	}

	public String[] buildCmdLine() {
		ArrayList<String> cmd = new ArrayList<String>();

		cmd.add(javaExe);

		cmd.add(jniLibraryPath);
		cmd.add(jnaLibraryPath);
		cmd.add("-cp");

		// step 1 - get current env data
		String ps = File.pathSeparator;
		// bogus jython.jar added as a hack to support - jython's 'more' fragile 2.7.0 interface :(
		// http://www.jython.org/archive/21/docs/registry.html
		// http://bugs.jython.org/issue2355
		String classpath = String.format("./myrobotlab.%s.jar%s./libraries/jar/jython.jar%s./libraries/jar/*%s./bin%s./build/classes", version, ps, ps, ps, ps);
		cmd.add(classpath);

		cmd.add("org.myrobotlab.service.Runtime");

		if (!userDefinedServices) {
			cmd.add("-service");
			cmd.add("gui");
			cmd.add("GUIService");
			cmd.add("python");
			cmd.add("Python");
		}

		cmd.add("-fromAgent");

		if (in != null) {
			for (int i = 0; i < in.size(); ++i) {
				cmd.add(in.get(i));
			}
		}
		return cmd.toArray(new String[cmd.size()]);
	}

}
