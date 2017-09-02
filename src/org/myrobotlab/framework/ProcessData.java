package org.myrobotlab.framework;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.myrobotlab.cmdline.CmdLine;
import org.myrobotlab.framework.interfaces.Invoker;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Agent;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

/**
 * Simple class representing an operating system mrl process
 * 
 * @author GroG
 *
 */
public class ProcessData implements Serializable {
	public final static Logger log = LoggerFactory.getLogger(ProcessData.class);

	private static final long serialVersionUID = 1L;

	public static final String STATE_RUNNING = "running";
	public static final String STATE_STOPPED = "stopped";
	public static final String STATE_RESTARTING = "restarting";
	public static final String STATE_UNKNOWN = "unknown";

	// TODO - need to start using id
	// public Integer id;
	public String id;
	public String branch;
	public String name;
	public String version;
	public Long startTs = null;
	public Long stopTs = null;
	public String jarPath = null;

	public String javaExe = null;

	// TODO - default more memory ? also mebbe have a jvmAppend flag vs replace ?
	public String jvm[] = new String[]{"-Djava.library.path=libraries/native","-Djna.library.path=libraries/native","-Dfile.encoding=UTF-8"};

	boolean userDefinedServices = false;

	public String state = STATE_STOPPED;

	transient public Process process;
	transient public Monitor monitor;
	static transient public Invoker service;
	CmdLine inCmdLine = null;
	static long nextPid = 0;
	
	String fromAgent = null;

	ArrayList<String> in = null;

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
					// don't wait if there is no agent
					if (service != null) {
						data.process.waitFor();
					}
				}
			} catch (Exception e) {
			}

			data.state = STATE_STOPPED;
			data.state = "stopped";

			if (ProcessData.service != null) {
				ProcessData.service.invoke("publishTerminated", data.id);
			}

		}

	}

	public ProcessData(Agent service, String id, String branch, String version, String name, Process process) {
		this.id = id;
		ProcessData.service = service;
		this.name = name;
		this.branch = branch;
		this.version = version;
		this.process = process;
		this.fromAgent = Runtime.getId();
	}

	/**
	 * copy of a ProcessData - threaded data will not be copied
	 * @param pd the process data
	 */
	public ProcessData(ProcessData pd) {
		this.id = pd.id;
		this.name = pd.name;
		this.branch = pd.branch;
		this.version = pd.version;

		this.javaExe = pd.javaExe;
		this.version = pd.version;
		
		this.jarPath = pd.jarPath;
		
		this.jvm = pd.jvm;

		this.userDefinedServices = pd.userDefinedServices;

		if (pd.in != null) {
			this.in = new ArrayList<String>();
			for (int i = 0; i < pd.in.size(); ++i) {
				this.in.add(pd.in.get(i));
			}
		}

		// this.process = pd.process;
		// this.startTs = System.currentTimeMillis();
		// monitor = new Monitor(this);
		// monitor.start();
	}

	/*
	 * FIXME - is too much catering to mrl execution ...
	 * 
	 * convert an String[] into a valid ProcessData
	 * 
	 * @param inCmdLine
	 * @param defaultBranch
	 * @param defaultVersion
	 */
	public ProcessData(Agent service, String jarPath, String[] inArgs, String defaultBranch,
			String defaultVersion) {
	  inCmdLine = new CmdLine(inArgs);
		ProcessData.service = service;
		this.jarPath = jarPath;

		// String protectedDomain =
		// URLDecoder.decode(Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(),
		// "UTF-8");
		// log.info("protected domain {}", protectedDomain);

		// convert to ArrayList to process
		in = new ArrayList<String>();
    
		for (int i = 0; i < inArgs.length; ++i) {
			String cmd = inArgs[i];
			
			if (cmd.equals("-runtimeName")) {
				name = inArgs[i + 1];
				continue;
			}
			
	    // if (inCmdLine.containsKey("-jvm") && inCmdLine.getArgumentCount("-jvm") > 0) {
			if (cmd.equals("-jvm")) {
	      String tmp = inCmdLine.getArgument("-jvm", 0);
	      jvm = tmp.split(" ");
	      continue;
	    }

			if (cmd.equals("-branch")) {
				branch = inArgs[i + 1];
				continue;
			}

			if (cmd.equals("-service")) {
				userDefinedServices = true;
			}

			// additional parameters
			in.add(inArgs[i]);
		}

		name = (name == null) ? "runtime" : name;
		branch = (branch == null) ? defaultBranch : branch;
		version = (version == null) ? defaultVersion : version;

		// step 1 - get current env data
		// String ps = File.pathSeparator;
		String fs = File.separator;

		Platform platform = Platform.getLocalInstance();
		String exeName = platform.isWindows() ? "javaw" : "java";

		javaExe = String.format("%s%sbin%s%s", System.getProperty("java.home"), fs, fs, exeName);

	}

	public boolean isRunning() {
		return STATE_RUNNING.equals(state);
	}

	public String[] buildCmdLine() {
		ArrayList<String> cmd = new ArrayList<String>();

		cmd.add(javaExe);
		
		if (jvm != null){
		  for (int i = 0; i < jvm.length; ++i){
		    cmd.add(jvm[i]);
		  }
		}
		
		cmd.add("-cp");

		// step 1 - get current env data
		String ps = File.pathSeparator;
		// bogus jython.jar added as a hack to support - jython's 'more' fragile
		// 2.7.0 interface :(
		// http://www.jython.org/archive/21/docs/registry.html
		// http://bugs.jython.org/issue2355
		String classpath = String.format("%s%s./libraries/jar/jython.jar%s./libraries/jar/*%s./bin%s./build/classes",
				jarPath, ps, ps, ps, ps);
		cmd.add(classpath);

		cmd.add("org.myrobotlab.service.Runtime");

		if (!userDefinedServices) {
			cmd.add("-service");
			// cmd.add("webgui");
			// cmd.add("WebGui");
			cmd.add("log");
			cmd.add("Log");
			cmd.add("cli");
			cmd.add("Cli");
			cmd.add("gui");
			cmd.add("SwingGui");
			cmd.add("python");
			cmd.add("Python");
		}

		cmd.add("-fromAgent");
		cmd.add(Runtime.getId());
		
		// if an id is not supplied for this process
		// create a new "Unique" one
		if (inCmdLine != null || !inCmdLine.hasSwitch("-id")){
		  cmd.add("-id");
		  SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmmssSSS");
		  this.id = String.format("runtime.%s.%s", formatter.format(new Date()), nextPid);
		  // this.id = String.format("%s.%d", org.myrobotlab.service.Runtime.getId(), nextPid);
		  ++nextPid;
		  cmd.add(this.id);
		} else {
		  this.id = inCmdLine.getArgument("-id", 0);
		}

		if (in != null) {
			for (int i = 0; i < in.size(); ++i) {
				cmd.add(in.get(i));
			}
		}
		return cmd.toArray(new String[cmd.size()]);
	}
	
	public void setRestarting(){
		state = STATE_RESTARTING;
	}

	public boolean isRestarting() {
		return state.equals(STATE_RESTARTING);
	}

}
