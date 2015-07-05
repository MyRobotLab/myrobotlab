package org.myrobotlab.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.fileLib.FindFile;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.modules.thread.thread;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         a Service to access Python interpreter.
 * 
 * 
 */
public class Python extends Service {

	int interpreterThreadCount = 0;

	/**
	 * this thread handles all callbacks to Python process all input and sets
	 * msg handles
	 * 
	 */
	public class InputQueueThread extends Thread {
		private Python python;

		public InputQueueThread(Python python) {
			super(String.format("%s_input", python.getName()));
			this.python = python;
		}

		@Override
		public void run() {
			try {
				while (isRunning()) {

					Message msg = inputQueue.take();

					try {
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
							compiledObject = getCompiledMethod(msg.method, String.format("ret_val = %s()", msg.method), interp);
						} else {
							StringBuffer methodWithParams = new StringBuffer();
							methodWithParams.append(String.format("ret_val = %s(", msg.method));
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
						 * log.error(String.format("%s() NOT FOUND",
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
						python.error(e.getMessage());
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
		public boolean executing = false;
		private String code;
		private PyObject compiledCode;

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
				error = error.replace("'", "");
				error = error.replace("\"", "");
				error = error.replace("\n", "");
				error = error.replace("\r", "");
				error = error.replace("<", "");
				error = error.replace(">", "");
				if (interp != null) {
					interp.exec(String.format("print '%s'", error));
				}
				Logging.logError(e);
				if (error.length() > 40) {
					error = error.substring(0, 40);
				}
				error("Python error - %s", error);
			} finally {
				executing = false;
				invoke("finishedExecutingScript");
			}

		}
	}

	public static class Script implements Serializable {
		private static final long serialVersionUID = 1L;
		private String name;
		private String code;

		public Script(String name, String script) {
			this.name = name;
			// DOS2UNIX line endings.
			// This seems to get triggered when people use editors that don't do
			// the cr/lf thing very well..
			// TODO:This will break python quoted text with the """ syntax in
			// python.
			if (script != null) {
				script = script.replaceAll("(\r)+\n", "\n");
			}
			this.code = script;
		}

		public String getCode() {
			return code;
		}

		public String getName() {
			return name;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	private static final long serialVersionUID = 1L;

	public final static transient Logger log = LoggerFactory.getLogger(Python.class);

	transient PythonInterpreter interp = null;

	transient PIThread interpThread = null;

	// FIXME - this is messy !
	transient HashMap<String, Script> scripts = new HashMap<String, Script>();

	transient LinkedBlockingQueue<Message> inputQueue = new LinkedBlockingQueue<Message>();

	transient InputQueueThread inputQueueThread;
	// TODO this needs to be moved into an actual cache if it is to be used
	// Cache of compile python code
	private static final transient HashMap<String, PyObject> objectCache;
	static {
		objectCache = new HashMap<String, PyObject>();
	}
	String inputScript = null;
	String setupScript = null;
	String msgHandlerScript = null;

	private Script currentScript = new Script("untitled.py", "");
	boolean pythonConsoleInitialized = false;

	String initialServiceScript = "";

	String rootPath = null;

	String modulesDir = "pythonModules";

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

	public static final String getSafeReferenceName(String name) {
		return name.replaceAll("[/ .]", "_");
	}

	public static String makeSafeName(String name) {
		return name.replaceAll("[\\-/ .]", "");
	}

	/**
	 * 
	 * @param instanceName
	 */
	public Python(String n) {
		super(n);

		log.info(String.format("creating python %s", getName()));
		// get all currently registered services and add appropriate python
		// handles
		HashMap<String, ServiceInterface> svcs = Runtime.getRegistry();
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

		initialServiceScript = initScript.toString();
		exec(initialServiceScript, false); // FIXME - shouldn't be done in the
											// constructor - e.g.
											// "initServicesScripts()"
		// register for addition of new services

		subscribe("registered", Runtime.getInstance().getName(), "registered", ServiceInterface.class);
		log.info(String.format("created python %s", getName()));
	}

	// PyObject interp.eval(String s) - for verifying?

	/**
	 * append more Python to the current script
	 * 
	 * @param data
	 *            the code to append
	 * @return the resulting concatenation
	 */
	public String appendScript(String data) {
		currentScript.setCode(String.format("%s\n%s", currentScript.getCode(), data));
		return data;
	}

	/**
	 * runs the pythonConsole.py script which creates a Python Console object
	 * and redirect stdout & stderr to published data - these are hooked by the
	 * GUIService
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
		// TODO - check if exists - destroy / de-initialize if necessary
		PySystemState.initialize();
		interp = new PythonInterpreter();

		PySystemState sys = Py.getSystemState();
		if (rootPath != null) {
			sys.path.append(new PyString(rootPath));
		}
		if (modulesDir != null) {
			sys.path.append(new PyString(modulesDir));
		}

		// add self reference
		// Python scripts can refer to this service as 'python' regardless
		// of the actual name
		/*
		 * String selfReferenceScript =
		 * String.format("from org.myrobotlab.service import Runtime\n" +
		 * "from org.myrobotlab.service import Python\n" +
		 * "python = Runtime.getService(\"%s\")\n\n", getName()); // TODO - //
		 * deprecate //+ "runtime = Runtime.getInstance()\n\n" +
		 * "myService = Runtime.create(\"%1$s\",\"Python\")\n",
		 * getSafeReferenceName(this.getName())); PyObject compiled =
		 * getCompiledMethod("initializePython", selfReferenceScript, interp);
		 * interp.exec(compiled);
		 */

		/*
		 * String self = String.format("myService = Runtime.getService(\"%s\")",
		 * getName()); PyObject compiled = getCompiledMethod("initializePython",
		 * self, interp); interp.exec(compiled);
		 */

		String selfReferenceScript = "from org.myrobotlab.service import Runtime\n" + "from org.myrobotlab.service import Python\n"
				+ String.format("%s = Runtime.getService(\"%s\")\n\n", getSafeReferenceName(getName()), getName()) + "Runtime = Runtime.getInstance()\n\n"
				+ String.format("myService = Runtime.getService(\"%s\")\n", getName());
		PyObject compiled = getCompiledMethod("initializePython", selfReferenceScript, interp);
		interp.exec(compiled);
	}

	public void exec() {
		exec(currentScript.getCode(), false);
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

	/**
	 * replaces and executes current Python script
	 * 
	 * @param code
	 */
	public void exec(String code) {
		exec(code, true);
	}

	/**
	 * non blocking exec
	 * 
	 * @param code
	 * @param replace
	 */
	public void exec(String code, boolean replace) {
		exec(code, replace, false);
	}

	/**
	 * replaces and executes current Python script if replace = false - will not
	 * replace "script" variable can be useful if ancillary scripts are needed
	 * e.g. monitors & consoles
	 * 
	 * @param code
	 *            the code to execute
	 * @param replace
	 *            replace the current script with code
	 */
	public void exec(String code, boolean replace, boolean blocking) {
		log.info(String.format("exec(String) \n%s\n", code));

		if (interp == null) {
			createPythonInterpreter();
		}
		if (replace) {
			currentScript.setCode(code);
		}
		try {
			if (!blocking) {
				interpThread = new PIThread(String.format("%s.interpreter.%d", getName(), ++interpreterThreadCount), code);
				interpThread.start();
			} else {
				interp.exec(code);
			}
		} catch(PyException pe){
			error(pe.toString());
		} catch (Exception e) {
			// PyException - very nice - but we'll handle it all 
			// the same way at the moment
			// broadcast msg only
			error(e.getMessage());
			// dump stack trace to log
			Logging.logError(e);
		}
	}

	public void execAndWait(String code) {
		exec(code, true, true);
	}

	public void execAndWait() {
		exec(currentScript.code, true, true);
	}

	/**
	 * executes an external Python file
	 * 
	 * @param filename
	 *            the full path name of the python file to execute
	 * @throws IOException
	 */
	public void execFile(String filename) throws IOException {
		String script = FileIO.fileToString(filename);
		exec(script);
	}

	/**
	 * execute an "already" defined python method directly
	 * 
	 * @param methodName
	 */
	public void execMethod(String method) {
		Message msg = createMessage(getName(), method, null);
		inputQueue.add(msg);
	}

	/**
	 * FIXME - better method Converter !!! can't do this now
	 */
	/*
	 * public void execMethod(String method, Object[]...args) { Message msg =
	 * createMessage(getName(), method, args); inputQueue.add(msg); }
	 */

	public void execMethod(String method, String param1) {
		Message msg = createMessage(getName(), method, new Object[] { param1 });
		inputQueue.add(msg);
	}
	
	public String eval(String method) {
		String jsonMethod = String.format("%s()", method);
		PyObject o = interp.eval(jsonMethod);
		String ret = o.toString();
		/*
		interp.exec(
		interp.get("ret_val");
		interp.exec(compiledObject);
		interp.eval();
		    Object x = engine.get("x");
		    System.out.println("x: " + x);
		*/
		return ret;
	}
	
	

	public void execResource(String filename) {
		String script = FileIO.resourceToString(filename);
		exec(script);
	}

	/**
	 * event method when script has finished executing
	 */
	public void finishedExecutingScript() {
	}

	@Override
	public String[] getCategories() {
		return new String[] { "programming", "control" };
	}

	@Override
	public String getDescription() {
		return "Python IDE";
	}

	/**
	 * gets the listing of current example python scripts in the myrobotlab.jar
	 * under /Python/examples
	 * 
	 * @return list of python examples
	 */
	public ArrayList<File> getExampleListing() {
		ArrayList<File> r = FileIO.listResourceContents("/Python/examples");
		return r;
	}

	/**
	 * list files from user directory user directory is located where MRL was
	 * unzipped (dot) .myrobotlab directory these are typically hidden on Linux
	 * systems
	 * 
	 * @return returns list of files with .py extension
	 */
	public ArrayList<String> getFileListing() {
		try {
			// FileIO.listResourceContents(path);
			List<File> files = FindFile.findByExtension(getCFGDir(), "py");
			ArrayList<String> ret = new ArrayList<String>();
			for (int i = 0; i < files.size(); ++i) {
				ret.add(files.get(i).getName());
			}
			return ret;
		} catch (Exception e) {
			Logging.logError(e);
		}
		return null;
	}

	/**
	 * Get the current script.
	 * 
	 * @return
	 */
	public Script getScript() {
		return currentScript;
	}

	public boolean loadAndExec(String filename) throws IOException {
		boolean ret = loadScriptFromFile(filename);
		exec();
		return ret;
	}

	/**
	 * this method can be used to load a Python script from the Python's local
	 * file system, which may not be the GUIService's local system. Because it
	 * can be done programatically on a different machine we want to broadcast
	 * our changed state to other listeners (possibly the GUIService)
	 * 
	 * @param filename
	 *            - name of file to load
	 * @return - success if loaded
	 * @throws IOException
	 */
	public boolean loadScriptFromFile(String filename) throws IOException {
		log.info(String.format("loadScriptFromFile %s", filename));
		String data = FileIO.fileToString(filename);
		return loadScript(filename, data);
	}

	public boolean loadScript(String scriptName, String newCode) {
		if (newCode != null && !newCode.isEmpty()) {
			log.info(String.format("replacing current script with %1s", scriptName));

			currentScript = new Script(scriptName, newCode);

			// tell other listeners we have changed
			// our current script
			// invoke("getScript");
			// invoke("publishLoadedScript", currentScript);
			broadcastState();
			return true;
		} else {
			warn(String.format("%1s a not valid script", scriptName));
			return false;
		}
	}

	// FIXME - need to replace "script" with Hashmap<filename, script> to
	// support and IDE muti-file view

	/**
	 * load a script from the myrobotlab.jar - location of example scripts are
	 * /resource/Python/examples
	 * 
	 * @param filename
	 *            name of file to load
	 * @return true if successfully loaded
	 */
	public boolean loadScriptFromResource(String filename) {
		log.info(String.format("loadScriptFromResource %s", filename));
		String newCode = FileIO.resourceToString(filename);
		return loadScript(filename, newCode);
	}

	/**
	 * Loads script from the users .myrobotlab directory - maintain the only
	 * non-absolute filename
	 * 
	 * @param filename
	 * @return true if successfully loaded
	 * @throws IOException
	 */
	public boolean loadUserScript(String filename) throws IOException {
		String newCode = FileIO.fileToString(getCFGDir() + File.separator + filename);
		if (newCode != null && !newCode.isEmpty()) {
			log.info(String.format("replacing current script with %s", filename));

			currentScript = new Script(filename, newCode);

			// tell other listeners we have changed
			// our current script
			// broadcastState();
			return true;
		} else {
			log.warn(String.format("%1s a not valid script", filename));
			return false;
		}
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

	/**
	 * 
	 * @param data
	 * @return
	 */
	public String publishStdOut(String data) {
		return data;
	}

	public void registered(ServiceInterface s) {

		String registerScript = "";

		// load the import
		// RIXME - RuntimeGlobals & static values for unknown
		if (!"unknown".equals(s.getSimpleName())) {
			registerScript = String.format("from org.myrobotlab.service import %s\n", s.getSimpleName());
		}

		registerScript += String.format("%s = Runtime.getService(\"%s\")\n", getSafeReferenceName(s.getName()), s.getName());
		exec(registerScript, false);
	}

	public boolean saveAndReplaceCurrentScript(String name, String code) {
		currentScript.name = name;
		currentScript.code = code;
		return saveCurrentScript();
	}

	public boolean saveCurrentScript() {
		try {
			FileOutputStream out = new FileOutputStream(getCFGDir() + File.separator + currentScript.name);
			out.write(currentScript.code.getBytes());
			out.close();
			return true;
		} catch (Exception e) {
			Logging.logError(e);
		}
		return false;
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
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Runtime.start("gui", "GUIService");
		 Runtime.start("webgui", "WebGUI");
		 Runtime.start("python", "Python");
		// String f = "C:\\Program Files\\blah.1.py";
		// log.info(getName(f));
		/*
		Python python = (Python) Runtime.start("python", "Python");
		python.error("this is an error");
		python.loadScriptFromResource("VirtualDevice/Arduino.py");
		python.execAndWait();
		// python.releaseService();

		Runtime.createAndStart("gui", "GUIService");
		
		*/

	}

}
