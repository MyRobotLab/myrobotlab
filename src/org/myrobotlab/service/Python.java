package org.myrobotlab.service;

import java.io.File;
import java.io.FileOutputStream;
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
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         a Service to access Python interpreter.
 * 
 *         references : http://wiki.python.org/python/InstallationInstructions
 *         http://www.python.org/javadoc/org/python/util/PythonInterpreter.html
 *         http
 *         ://etutorials.org/Programming/Python+tutorial/Part+V+Extending+and
 *         +Embedding
 *         /Chapter+25.+Extending+and+Embedding+Python/25.2+Embedding+Python
 *         +in+Java/ http://wiki.python.org/moin/PythonEditors - list of editors
 *         http://java-source.net/open-source/scripting-languages
 *         http://java.sun.com/products/jfc/tsc/articles/text/editor_kit/ -
 *         syntax highlighting text editor
 *         http://download.oracle.com/javase/tutorial
 *         /uiswing/components/generaltext.html#editorkits
 *         http://download.oracle
 *         .com/javase/tutorial/uiswing/components/editorpane.html
 *         http://stackoverflow
 *         .com/questions/2441525/how-to-use-netbeans-platform
 *         -syntax-highlight-with-jeditorpane
 *         http://book.javanb.com/jfc-swing-tutorial
 *         -the-a-guide-to-constructing-guis-2nd/ch03lev2sec6.html
 * 
 *         http://ostermiller.org/syntax/editor.html Text Editor Tutorial - with
 *         syntax highlighting
 *         http://stackoverflow.com/questions/4151950/syntax-
 *         highlighting-in-jeditorpane-in-java - example of non-tokenized
 *         highlighting
 *         http://saveabend.blogspot.com/2008/06/java-syntax-highlighting
 *         -with.html
 * 
 *         swing components http://fifesoft.com/rsyntaxtextarea/ <- AMAZING
 *         PROJECT
 *         http://www.pushing-pixels.org/2008/06/27/syntax-coloring-for-the
 *         -swing-editor-pane.html
 * 
 *         Java Python integration
 *         http://pythonpodcast.hostjava.net/pythonbook/en
 *         /1.0/PythonAndJavaIntegration
 *         .html#using-python-within-java-applications
 * 
 *         Redirecting std out
 *         http://bytes.com/topic/python/answers/40880-redirect
 *         -standard-output-python-jtextarea
 *         http://stefaanlippens.net/redirect_python_print
 *         http://stackoverflow.com
 *         /questions/1000360/python-print-on-stdout-on-a-terminal
 *         http://coreygoldberg
 *         .blogspot.com/2009/05/python-redirect-or-turn-off-stdout-and.html
 *         https
 *         ://www.ibm.com/developerworks/mydeveloperworks/blogs/PythonSwing/
 *         ?lang=en
 * 
 */
public class Python extends Service {

	private static final long serialVersionUID = 1L;

	public final static transient Logger log = LoggerFactory.getLogger(Python.class.getCanonicalName());

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

	@Element
	String inputScript = null;
	@Element
	String setupScript = null;
	@Element
	String msgHandlerScript = null;
	@Element
	private Script currentScript = new Script("untitled.py", "");
	boolean pythonConsoleInitialized = false;
	@Element
	String initialServiceScript = "";

	String rootPath = null;
	String modulesDir = "pythonModules";

	public static class Script implements Serializable {
		private static final long serialVersionUID = 1L;
		private String name;
		private String code;

		public Script() {
		}

		public Script(String name, String script) {
			this.name = name;
			this.code = script;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}

	class PIThread extends Thread {
		public boolean executing = false;
		private String code;
		private PyObject compiledCode;

		PIThread(String code) {
			this.code = code;
		}

		PIThread(PyObject compiledCode) {
			this.compiledCode = compiledCode;
		}

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
				interp.exec(String.format("print '%s'", error));
				Logging.logException(e);
				error("Python error");
			} finally {
				executing = false;
				invoke("finishedExecutingScript");
			}

		}
	}

	/**
	 * this thread handles all callbacks to Python process all input and sets
	 * msg handles
	 * 
	 */
	public class InputQueueThread extends Thread {
		private Python python;

		public InputQueueThread(Python python) {
			super(String.format("%s_input", getSafeReferenceName(python.getName())));
			this.python = python;
		}

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

						PyObject compiledObject = getCompiledMethod(msg.method, String.format("%s()", msg.method), interp);
						log.info(String.format("setting data %s", msgHandle));
						interp.set(msgHandle.toString(), msg);
						interp.exec(compiledObject);
					} catch (Exception e) {
						Logging.logException(e);
						python.error(e.getMessage());
					}

				}
			} catch (InterruptedException e) {
				Logging.logException(e);
			}
		}
	}

	public static final String getSafeReferenceName(String name) {
		return name.replaceAll("[/ .]", "_");
	}

	/**
	 * 
	 * @param instanceName
	 */
	public Python(String n) {
		super(n);

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
	}

	public static String makeSafeName(String name) {
		return name.replaceAll("[\\-/ .]", "");
	}

	public void registered(ServiceInterface s) {

		String registerScript = "";

		// load the import
		// RIXME - RuntimeGlobals & static values for unknown
		if (!"unknown".equals(s.getSimpleName())) 
		{
			registerScript = String.format("from org.myrobotlab.service import %s\n", s.getSimpleName());
		}

		registerScript += String.format("%s = Runtime.getService(\"%s\")\n", getSafeReferenceName(s.getName()), s.getName());
		exec(registerScript, false);
	}

	/**
	 * runs the pythonConsole.py script which creates a Python Console object
	 * and redirect stdout & stderr to published data - these are hooked by the
	 * GUIService
	 */
	public void attachPythonConsole() {
		if (!pythonConsoleInitialized) {
			/** REMOVE IF FLAKEY BUGS APPEAR !! */
			String consoleScript = getServiceResourceFile("examples/pythonConsole.py");
			exec(consoleScript, false);
		}
	}

	// PyObject interp.eval(String s) - for verifying?

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
		String selfReferenceScript = String.format("from org.myrobotlab.service import Runtime\n" + "from org.myrobotlab.service import Python\n"
				+ "python = Runtime.create(\"%1$s\",\"Python\")\n\n" // TODO -
																		// deprecate
				+ "runtime = Runtime.getInstance()\n\n" + "myService = Runtime.create(\"%1$s\",\"Python\")\n", getSafeReferenceName(this.getName()));
		PyObject compiled = getCompiledMethod("initializePython", selfReferenceScript, interp);
		interp.exec(compiled);
	}

	/**
	 * replaces and executes current Python script
	 * 
	 * @param code
	 */
	public void exec(String code) {
		exec(code, true);
	}

	public void exec() {
		exec(currentScript.getCode(), false);
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
	public void exec(String code, boolean replace) {
		log.info(String.format("exec %s", code));
		
	//	code = code.replaceAll("\r\n", "\n"); // DOS2UNIX
		
		if (interp == null) {
			createPythonInterpreter();
		}
		if (replace) {
			currentScript.setCode(code);
		}
		try {
			interpThread = new PIThread(code);
			interpThread.start();

			// interp.exec(code);

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public void exec(PyObject code) {
		log.info(String.format("exec %s", code));
		if (interp == null) {
			createPythonInterpreter();
		}

		try {
			interpThread = new PIThread(code);
			interpThread.start();

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	/**
	 * event method when script has finished executing
	 */
	public void finishedExecutingScript() {
	}

	/**
	 * Get the current script.
	 * 
	 * @return
	 */
	public Script getScript() {
		return currentScript;
	}

	@Override
	public String getDescription() {
		return "Python IDE";
	}

	/**
	 * 
	 * @param data
	 * @return
	 */
	public String publishStdOut(String data) {
		return data;
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
	 * executes an external Python file
	 * 
	 * @param filename
	 *            the full path name of the python file to execute
	 */
	public void execFile(String filename) {
		String script = FileIO.fileToString(filename);
		exec(script);
	}

	public void execResource(String filename) {
		String script = FileIO.resourceToString(filename);
		exec(script);
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
		if (objectCache.size() > 5) {
			// keep the size to 6
			objectCache.remove(objectCache.keySet().iterator().next());
		}
		objectCache.put(name, compiled);
		return compiled;
	}

	/**
	 * Get rid of the interpreter.
	 */
	public void stop() {
		if (interp != null) {
			if (interpThread != null) {
				interpThread.interrupt();
				interpThread = null;
			}
			// PySystemState.exit(); // the big hammar' throws like Thor
			interp.cleanup();
			interp = null;
		}

		inputQueueThread.interrupt();
	}

	public void startService() {
		super.startService();
		inputQueueThread = new InputQueueThread(this);
		inputQueueThread.start();
	}

	/**
	 * stops threads releases interpreter
	 */
	public void stopService() {
		super.stopService();
		stop();// release the interpeter
	}

	public boolean loadAndExec(String filename) {
		boolean ret = loadScript(filename);
		exec();
		return ret;
	}

	// FIXME - need to replace "script" with Hashmap<filename, script> to
	// support and IDE muti-file view

	public boolean saveCurrentScript() {
		try {
			FileOutputStream out = new FileOutputStream(getCFGDir() + File.separator + currentScript.name);
			out.write(currentScript.code.getBytes());
			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}

	public boolean saveAndReplaceCurrentScript(String name, String code) {
		currentScript.name = name;
		currentScript.code = code;
		return saveCurrentScript();
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
	 */
	public boolean loadScript(String filename) {
		String newCode = FileIO.fileToString(filename);
		if (newCode != null && !newCode.isEmpty()) {
			log.info(String.format("replacing current script with %1s", filename));

			currentScript = new Script(filename, newCode);

			// tell other listeners we have changed
			// our current script
			broadcastState();
			return true;
		} else {
			log.warn(String.format("%1s a not valid script", filename));
			return false;
		}
	}

	/**
	 * Loads script from the users .myrobotlab directory - maintain the only
	 * non-absolute filename
	 * 
	 * @param filename
	 * @return true if successfully loaded
	 */
	public boolean loadUserScript(String filename) {
		String newCode = FileIO.fileToString(getCFGDir() + File.separator + filename);
		if (newCode != null && !newCode.isEmpty()) {
			log.info(String.format("replacing current script with %1s", filename));

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
	 * gets the listing of current example python scripts in the myrobotlab.jar
	 * under /Python/examples
	 * 
	 * @return list of python examples
	 */
	public ArrayList<String> getExampleListing() {
		ArrayList<String> r = FileIO.listResourceContents("/Python/examples");
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
			Logging.logException(e);
		}
		return null;
	}

	/**
	 * load a script from the myrobotlab.jar - location of example scripts are
	 * /resource/Python/examples
	 * 
	 * @param filename
	 *            name of file to load
	 * @return true if successfully loaded
	 */
	public boolean loadScriptFromResource(String filename) {
		log.debug(String.format("loadScriptFromResource scripts/%1s", filename));
		String newCode = getServiceResourceFile(String.format("examples/%1s", filename));

		log.info(String.format("loaded new scripts/%1s size %d", filename, newCode.length()));
		if (newCode != null && !newCode.isEmpty()) {
			log.info(String.format("replacing current script with %1s", filename));

			currentScript = new Script(filename, newCode);

			// tell other listeners we have changed
			// our current script
			// broadcastState();
			invoke("getScript");
			return true;
		} else {
			log.warn(String.format("%1s a not valid script", filename));
			return false;
		}
	}

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

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		// String f = "C:\\Program Files\\blah.1.py";
		// log.info(getName(f));

		Runtime.createAndStart("python", "Python");
		Runtime.createAndStart("gui", "GUIService");

	}

}
