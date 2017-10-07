package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.framework.repo.GitHub;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.FindFile;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Script;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.modules.thread.thread;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;

/**
 * 
 * Python - This service provides python scripting support. It uses the jython
 * integration and provides python 2.7 syntax compliance.
 * 
 * More Info at : https://www.python.org/ http://www.jython.org/
 * 
 * @author GroG
 * 
 */
public class Python extends Service {

	/**
	 * this thread handles all callbacks to Python process all input and sets
	 * msg handles
	 * 
	 */
	public class InputQueueThread extends Thread {
		private Python python;

		public InputQueueThread(Python python) {
			super(String.format("%s.input", python.getName()));
			this.python = python;
		}

		@Override
		public void run() {
			try {
				while (isRunning()) {

					Message msg = inputQueue.take();

					try {
						// FIXME - remove all msg_ .. its the old way .. :P

						// serious bad bug in it which I think I fixed - the
						// msgHandle is really the data coming from a callback
						// it can originate from the same calling function such
						// as Sphinx.send - but we want the callback to
						// call a different method - this means the data needs
						// to go to a data structure which is keyed by only the
						// sending method, but must call the appropriate method
						// in Sphinx
						StringBuffer msgHandle = new StringBuffer().append("msg_").append(getSafeReferenceName(msg.sender)).append("_").append(msg.sendingMethod);

						// StringBuffer methodSignature ???

						PyObject compiledObject = null;

						// TODO - getCompiledMethod(msg.method SHOULD BE
						// getCompiledMethod(methodSignature
						// without it - no overloading is possible

						if (msg.data == null || msg.data.length == 0) {
							compiledObject = getCompiledMethod(msg.method, String.format("%s()", msg.method), interp);
						} else {
							StringBuffer methodWithParams = new StringBuffer();
							methodWithParams.append(String.format("%s(", msg.method));
							for (int i = 0; i < msg.data.length; ++i) {
								String paramHandle = String.format("%s_p%d", msgHandle, i);
								interp.set(paramHandle.toString(), msg.data[i]);
								methodWithParams.append(paramHandle);
								if (i < msg.data.length - 1) {
									methodWithParams.append(",");
								}
							}
							methodWithParams.append(")");
							compiledObject = getCompiledMethod(msg.method, methodWithParams.toString(), interp);
						}
						/*
						 * if (compiledObject == null){ // NEVER NULL - object
						 * cache - builds cache if not there
						 * log.error(String.format( "%s() NOT FOUND",
						 * msg.method)); }
						 */

						// commented out recently - no longer using msg handle
						// for
						// call-backs :)
						// log.info(String.format("setting data %s",
						// msgHandle));
						// interp.set(msgHandle.toString(), msg);
						interp.exec(compiledObject);

					} catch (Exception e) {
						Logging.logError(e);
						python.error(String.format("%s %s", e.getClass().getSimpleName(), e.getMessage()));
					}

				}
			} catch (Exception e) {
				if (e instanceof InterruptedException) {
					info("shutting down %s", getName());
				} else {
					Logging.logError(e);
				}
			}
		}
	}

	class PIThread extends Thread {
		private String code;
		private PyObject compiledCode;
		public boolean executing = false;

		PIThread(String name, PyObject compiledCode) {
			super(name);
			this.compiledCode = compiledCode;
		}

		PIThread(String name, String code) {
			super(name);
			this.code = code;
		}

		@Override
		public void run() {
			try {
				executing = true;
				if (compiledCode != null) {
					interp.exec(compiledCode);
				} else {
					interp.exec(code);
				}
			} catch (Exception e) {
				String error = Logging.stackToString(e);

				invoke("publishStatus", Status.error(e));

				String filtered = error;
				filtered = filtered.replace("'", "");
				filtered = filtered.replace("\"", "");
				filtered = filtered.replace("\n", "");
				filtered = filtered.replace("\r", "");
				filtered = filtered.replace("<", "");
				filtered = filtered.replace(">", "");
				if (interp != null) {
					interp.exec(String.format("print '%s'", filtered));
				}
				Logging.logError(e);
				if (filtered.length() > 40) {
					filtered = filtered.substring(0, 40);
				}

			} finally {
				executing = false;
				invoke("finishedExecutingScript");
			}

		}
	}


	public final static transient Logger log = LoggerFactory.getLogger(Python.class);
	// TODO this needs to be moved into an actual cache if it is to be used
	// Cache of compile python code
	private static final transient HashMap<String, PyObject> objectCache;

	/**
	 * current working directory root there are multiple filesystems we can load
	 * scripts from github urls | jar:file /resources | /resource exploded |
	 * .myrobotlab directory | workind directory | root of file system this
	 * variable is to tell which root to begin with
	 */

	private static final long serialVersionUID = 1L;

	static {
		objectCache = new HashMap<String, PyObject>();
	}

	/**
	 * Get a compiled version of the python call.
	 * 
	 * @param name
	 * @param code
	 * @param interp
	 * @return
	 */
	private static synchronized PyObject getCompiledMethod(String name, String code, PythonInterpreter interp) {
		// TODO change this from a synchronized method to a few blocks to
		// improve concurrent performance
		if (objectCache.containsKey(name)) {
			return objectCache.get(name);
		}

		PyObject compiled = interp.compile(code);
		if (objectCache.size() > 25) {
			// keep the size to 6
			objectCache.remove(objectCache.keySet().iterator().next());
		}
		objectCache.put(name, compiled);
		return compiled;
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

		ServiceType meta = new ServiceType(Python.class.getCanonicalName());
		meta.addDescription("the Jython script engine compatible with pure Python 2.7 scripts");
		meta.addCategory("programming", "control");
		return meta;
	}

	public static final String getSafeReferenceName(String name) {
		return name.replaceAll("[/ .-]", "_");
	}

	/**
	 * pyrobotlab python service urls - created for referencing script
	 */
	Map<String, String> exampleUrls = new TreeMap<String, String>();

	transient LinkedBlockingQueue<Message> inputQueue = new LinkedBlockingQueue<Message>();
	transient InputQueueThread inputQueueThread;
	transient PythonInterpreter interp = null;
	transient PIThread interpThread = null;

	int interpreterThreadCount = 0;

	/**
	 * local current directory of python script any new python script will get
	 * localScriptDir prefix
	 */
	String localScriptDir = new File(getCfgDir()).getAbsolutePath();

	/**
	 * local pthon files of current script directory
	 */
	List<String> localPythonFiles = new ArrayList<String>();

	/**
	 * default location for python modules
	 */
	String modulesDir = "pythonModules";

	boolean pythonConsoleInitialized = false;

	/**
	 * opened scripts
	 */
	HashMap<String, Script> openedScripts = new HashMap<String, Script>();

	public Python(String n) {
		super(n);

		log.info(String.format("creating python %s", getName()));
		// get all currently registered services and add appropriate python
		// handles
		Map<String, ServiceInterface> svcs = Runtime.getRegistry();
		StringBuffer initScript = new StringBuffer();
		initScript.append("from time import sleep\n");
		initScript.append("from org.myrobotlab.service import Runtime\n");
		Iterator<String> it = svcs.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceInterface sw = svcs.get(serviceName);

			initScript.append(String.format("from org.myrobotlab.service import %s\n", sw.getSimpleName()));

			String serviceScript = String.format("%s = Runtime.getService(\"%s\")\n", getSafeReferenceName(serviceName), serviceName);

			// get a handle on running service
			initScript.append(serviceScript);
		}

		exec(initScript.toString(), false); // FIXME - shouldn't be done in the

		subscribe(Runtime.getInstance().getName(), "registered");
		log.info(String.format("created python %s", getName()));

		log.info("creating module directory pythonModules");
		new File("pythonModules").mkdir();

		// I love ServiceData !
		ServiceData sd = ServiceData.getLocalInstance();
		// I love Platform !
		Platform p = Platform.getLocalInstance();
		List<ServiceType> sdt = sd.getAvailableServiceTypes();
		for (int i = 0; i < sdt.size(); ++i) {
			ServiceType st = sdt.get(i);
			String url = String.format("https://raw.githubusercontent.com/MyRobotLab/pyrobotlab/%s/service/%s.py", p.getBranch(), st.getSimpleName());
			exampleUrls.put(st.getSimpleName(), url);
		}

		localPythonFiles = getFileListing();
	}
	
	public void openScript(String scriptName, String code){
		openedScripts.put(scriptName, new Script(scriptName, code));
		broadcastState();
	}

	/**
	 * append more Python to the current script
	 * 
	 * @param data
	 *            the code to append
	 * @return the resulting concatenation
	 */
	public Script appendScript(String data) {		
		return new Script("append", data);
	}

	/**
	 * runs the pythonConsole.py script which creates a Python Console object
	 * and redirect stdout &amp; stderr to published data - these are hooked by the
	 * SwingGui
	 */
	public void attachPythonConsole() {
		if (!pythonConsoleInitialized) {
			/** REMOVE IF FLAKEY BUGS APPEAR !! */
			String consoleScript = getServiceResourceFile("pythonConsole.py");
			exec(consoleScript, false);
		}
	}

	/**
	 * 
	 */
	public void createPythonInterpreter() {
		// TODO: If the username on windows contains non-ascii characters
		// the Jython interpreter will blow up.
		// The APPDATA environment variable contains the username.
		// as a result, jython sees the non ascii chars and it causes a utf-8
		// decoding error.
		// overriding of the APPDATA environment variable is done in the agent
		// as a work around.

		// work around for 2.7.0
		// http://bugs.jython.org/issue2355

		// ??? - do we need to extract {jar}/Lib/site.py ???

		Properties props = new Properties();

		/*
		 * Used to prevent: console: Failed to install '':
		 * java.nio.charset.UnsupportedCharsetException: cp0.
		 */
		props.put("python.console.encoding", "UTF-8");

		/*
		 * don't respect java accessibility, so that we can access protected
		 * members on subclasses
		 */
		props.put("python.security.respectJavaAccessibility", "false");
		props.put("python.import.site", "false");

		Properties preprops = System.getProperties();

		PythonInterpreter.initialize(preprops, props, new String[0]);

		interp = new PythonInterpreter();

		PySystemState sys = Py.getSystemState();

		if (modulesDir != null) {
			sys.path.append(new PyString(modulesDir));
		}
		log.info("Python System Path: {}", sys.path);

		String selfReferenceScript = "from org.myrobotlab.service import Runtime\n" + "from org.myrobotlab.service import Python\n"
				+ String.format("%s = Runtime.getService(\"%s\")\n\n", getSafeReferenceName(getName()), getName()) + "Runtime = Runtime.getInstance()\n\n"
				+ String.format("myService = Runtime.getService(\"%s\")\n", getName());
		PyObject compiled = getCompiledMethod("initializePython", selfReferenceScript, interp);
		interp.exec(compiled);
	}

	public String eval(String method) {
		String jsonMethod = String.format("%s()", method);
		PyObject o = interp.eval(jsonMethod);
		String ret = o.toString();
		return ret;
	}

	public void exec(PyObject code) {
		log.info(String.format("exec \n%s\n", code));
		if (interp == null) {
			createPythonInterpreter();
		}

		try {
			interpThread = new PIThread(String.format("%s.interpreter.%d", getName(), ++interpreterThreadCount), code);
			interpThread.start();

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	/*
	 * replaces and executes current Python script
	 */
	public void exec(String code) {
		exec(code, true);
	}

	/*
	 * non blocking exec
	 * 
	 */
	public void exec(String code, boolean replace) {
		exec(code, replace, false);
	}

	/**
	 * This method will execute a string that represents a python script.  When called with blocking=false, the return code
	 * will likely return true even if there is a syntax error because it doesn't wait for the response.
	 * 
	 * @param code - the script to execute
	 * @param replace - not used!  (should be removed.)
	 * @param blocking - if true, this method will wait until all of the code has been evaluated.  
	 * @return - returns true if execution of the code was successful.  returns false if there was an exception.
	 */
	public boolean exec(String code, boolean replace, boolean blocking) {
	  log.info("exec(String) \n{}", code);
		boolean success = true;
		if (interp == null) {
			createPythonInterpreter();
		}
		try {
			if (!blocking) {
				interpThread = new PIThread(String.format("%s.interpreter.%d", getName(), ++interpreterThreadCount), code);
				interpThread.start();
			} else {
				interp.exec(code);
			}
		} catch (PyException pe) {
		  // something specific with a python error
		  success = false;
			error(pe.toString());
			Logging.logError(pe);
		} catch (Exception e) {
		  success = false;
			// more general error handling.
			error(e.getMessage());
			// dump stack trace to log
			Logging.logError(e);
		}
		return success;
	}

	public void execAndWait(String code) {
		exec(code, true, true);
	}

	/*
	 * executes an external Python file
	 * 
	 * @param filename
	 *            the full path name of the python file to execute
	 */
	public void execFile(String filename) throws IOException {
	  String script = FileIO.toString(filename);
		exec(script);
	}

	/**
	 * execute an "already" defined python method directly
	 * 
	 * @param method - the name of the method
	 */
	public void execMethod(String method) {
		// execMethod(getName(), method, (Object[])null);
		execMethod(method, (Object[])null);
	}

	public void execMethod(String method, Object...parms) {
		Message msg = Message.createMessage(this, getName(), method, parms);
		inputQueue.add(msg);
	}

	public void execResource(String filename) {
		String script = FileIO.resourceToString(filename);
		exec(script);
	}

	/**
	 * publishing method when a script is finished
	 */
	public void finishedExecutingScript() {
	}

	/**
	 * DEPRECATE - use online examples only ... (possibly you can package &amp;
	 * include filename listing during build process)
	 * 
	 * gets the listing of current example python scripts in the myrobotlab.jar
	 * under /Python/examples
	 * 
	 * @return list of python examples
	 */
	public List<File> getExampleListing() {
		List<File> r = null;
		try {
			// expensive method - searches through entire jar
			r = FileIO.listResourceContents("Python/examples");
		} catch (Exception e) {
			Logging.logError(e);
		}
		return r;
	}

	/**
	 * list files from user directory user directory is located where MRL was
	 * unzipped (dot) .myrobotlab directory these are typically hidden on Linux
	 * systems
	 * 
	 * @return returns list of files with .py extension
	 */
	public List<String> getFileListing() {
		try {
			// FileIO.listResourceContents(path);
			List<File> files = FindFile.findByExtension(localScriptDir, "py", false);
			localPythonFiles = new ArrayList<String>();
			for (int i = 0; i < files.size(); ++i) {
				localPythonFiles.add(files.get(i).getName());
			}
			return localPythonFiles;
		} catch (Exception e) {
			Logging.logError(e);
		}
		return null;
	}


	/*
	 * load a script from the myrobotlab.jar - location of example scripts are
	 * /resource/Python/examples
	 *            name of file to load
	 */
	public void loadPyRobotLabServiceScript(String serviceType) {
	  Platform p = Platform.getLocalInstance();
		String serviceScript = GitHub.getPyRobotLabScript(p.getBranch(), serviceType);
		openScript(String.format("%s.py", serviceType), serviceScript);
	}

	/*
	 * this method can be used to load a Python script from the Python's local
	 * file system, which may not be the SwingGui's local system. Because it
	 * can be done programatically on a different machine we want to broadcast
	 * our changed state to other listeners (possibly the SwingGui)
	 * 
	 * @param filename
	 *            - name of file to load
	 */
	public void openScriptFromFile(String filename) throws IOException {
		log.info("loadScriptFromFile {}", filename);
		String data = FileIO.toString(filename);
		openScript(filename, data);
	}

	
	public void onRegistered(ServiceInterface s) {

		String registerScript = "";

		// load the import
		// RIXME - RuntimeGlobals & static values for unknown
		if (!"unknown".equals(s.getSimpleName())) {
			registerScript = String.format("from org.myrobotlab.service import %s\n", s.getSimpleName());
		}

		registerScript += String.format("%s = Runtime.getService(\"%s\")\n", getSafeReferenceName(s.getName()), s.getName());
		exec(registerScript, false);
	}

	/**
	 * preProcessHook is used to intercept messages and process or route them
	 * before being processed/invoked in the Service.
	 * 
	 * Here all messages allowed to go and effect the Python service will be let
	 * through. However, all messsages not found in this filter will go "into"
	 * they Python script. There they can be handled in the scripted users code.
	 * 
	 * @see org.myrobotlab.framework.Service#preProcessHook(org.myrobotlab.framework.Message)
	 */
	@Override
	public boolean preProcessHook(Message msg) {
		// let the messages for this service
		// get processed normally
		if (methodSet.contains(msg.method)) {
			return true;
		}
		// otherwise its target is for the
		// scripting environment
		// set the data - and call the call-back function
		if (interp == null) {
			createPythonInterpreter();
		}

		// handling call-back input needs to be
		// done by another thread - in case its doing blocking
		// or is executing long tasks - the inbox thread needs to
		// be freed of such tasks - it has to do all the inbound routing
		inputQueue.add(msg);
		return false;
	}

	public String publishStdOut(String data) {
		return data;
	}

	public void setLocalScriptDir(String path) {
		File dir = new File(path);
		if (!dir.isDirectory()) {
			error("%s is not a directory");
		}

		localScriptDir = dir.getAbsolutePath();

		getFileListing();
		save();
		broadcastState();
	}

	@Override
	public void startService() {
		super.startService();
		log.info(String.format("starting python %s", getName()));
		if (inputQueueThread == null) {
			inputQueueThread = new InputQueueThread(this);
			inputQueueThread.start();
		}
		log.info(String.format("started python %s", getName()));
	}

	/**
	 * Get rid of the interpreter.
	 */
	public void stop() {
		if (interp != null) {
			// PySystemState.exit(); // the big hammar' throws like Thor
			interp.cleanup();
			interp = null;
		}

		if (interpThread != null) {
			interpThread.interrupt();
			interpThread = null;
		}

		if (inputQueueThread != null) {
			inputQueueThread.interrupt();
			inputQueueThread = null;
		}

		thread.interruptAllThreads();
		Py.getSystemState()._systemRestart = true;
	}

	/**
	 * stops threads releases interpreter
	 */
	@Override
	public void stopService() {
		super.stopService();
		stop();// release the interpeter
	}

	public static void main(String[] args) {		
		LoggingFactory.init(Level.INFO);

		try {
			
			File test = new File("file:/D:/local");
			File example = new File("https://raw.githubusercontent.com/MyRobotLab/pyrobotlab/develop/service/Clock.py");
			
			log.info("{}", test.toURI().toURL());
			log.info("{}", example.toURI().toURL());

			// Runtime.start("gui", "SwingGui");
			// String f = "C:\\Program Files\\blah.1.py";
			// log.info(getName(f));
			Runtime.start("python", "Python");
			// Runtime.start("webgui", "WebGui");
			Runtime.start("gui", "SwingGui");

			// python.error("this is an error");
			// python.loadScriptFromResource("VirtualDevice/Arduino.py");
			// python.execAndWait();
			// python.releaseService();

			/*
			 * python.load(); python.save();
			 * 
			 * FileOutputStream fos = new FileOutputStream("python.dat");
			 * ObjectOutputStream out = new ObjectOutputStream(fos);
			 * out.writeObject(python); out.close();
			 * 
			 * FileInputStream fis = new FileInputStream("python.dat");
			 * ObjectInputStream in = new ObjectInputStream(fis); Object x =
			 * in.readObject(); in.close();
			 * 
			 * Runtime.createAndStart("gui", "SwingGui");
			 */

		} catch (Exception e) {
			Logging.logError(e);
		}

	}

}