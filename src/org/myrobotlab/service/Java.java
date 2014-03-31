package org.myrobotlab.service;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.java.Reflector;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;

import edu.rice.cs.dynamicjava.Options;
import edu.rice.cs.dynamicjava.interpreter.Interpreter;
import edu.rice.cs.dynamicjava.interpreter.InterpreterException;
import edu.rice.cs.plt.io.IOUtil;
import edu.rice.cs.plt.reflect.PathClassLoader;
import edu.rice.cs.plt.text.ArgumentParser;
import edu.rice.cs.plt.tuple.Option;

/**
 * @author GroG / raver1975
 * 
 *         a Service to access Java interpreter.
 * 
 * 
 */
public class Java extends Service {

	private static final long serialVersionUID = 1L;

	public final static transient Logger log = LoggerFactory
			.getLogger(Java.class.getCanonicalName());

	transient Interpreter interp = null;
	transient PIThread interpThread = null;
	// FIXME - this is messy !
	transient HashMap<String, Script> scripts = new HashMap<String, Script>();
	String rootPath = null;
	String modulesDir = "javaModules";

	private transient Reflector reflector;
	// TODO this needs to be moved into an actual cache if it is to be used

	// // Cache of compile java code
	// private static final transient HashMap<String, PyObject> objectCache;
	//
	// static {
	// objectCache = new HashMap<String, PyObject>();
	// }

	@Element
	String inputScript = null;
	@Element
	String setupScript = null;
	@Element
	String msgHandlerScript = null;
	@Element
	private Script currentScript = new Script("untitled.java", "");
	boolean javaConsoleInitialized = false;
	@Element
	String initialServiceScript = "";

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

		PIThread(String code) {
			this.code = code;
		}

		public void run() {
			try {
				executing = true;
				if (code != null)
					interp.interpret(code);
			} catch (Exception e) {
				Logging.logException(e);
			} finally {
				executing = false;
				invoke("finishedExecutingScript");
			}

		}
	}

	/**
	 * 
	 * @param instanceName
	 */
	public Java(String n) {
		super(n);

		// get all currently registered services and add appropriate java
		// handles
		HashMap<String, ServiceInterface> svcs = Runtime.getRegistry();
		StringBuffer initScript = new StringBuffer();
		// initScript.append("from time import sleep\n");
		initScript.append("import org.myrobotlab.service.*;\n");
		initScript.append("import java.util.*;\n");
		initScript.append("import org.myrobotlab.java.*;\n");
		initScript.append("public void reflect(Object o){((Java)java).reflect(o);}");

		Iterator<String> it = svcs.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceInterface sw = svcs.get(serviceName);

			initScript.append(String.format(
					"import org.myrobotlab.service.%s;\n", sw.getSimpleName()));

			// get a handle on running service
			initScript
					.append(String
							.format("%s =(%s) org.myrobotlab.service.Runtime.getService(\"%s\").service;\n",
									serviceName, sw.getSimpleName(),
									serviceName));
		}

		initialServiceScript = initScript.toString();
		exec(initialServiceScript, false); // FIXME - shouldn't be done in the
											// constructor - e.g.
											// "initServicesScripts()"
		// register for addition of new services

		subscribe("registered", Runtime.getInstance().getName(), "registered",
				ServiceInterface.class);
		reflector = new Reflector(this);
	}

	public void registered(ServiceInterface s) {

		String registerScript = "";

		// load the import
		if (!"unknown".equals(s.getSimpleName())) // FIXME - RuntimeGlobals &
													// static values for
													// "unknown"
		{
			registerScript = String.format(
					"import org.myrobotlab.service.%s;\n", s.getSimpleName());
		}

		registerScript += String
				.format("%s = (%s)org.myrobotlab.service.Runtime.getService(\"%s\").service;\n",
						s.getName(), s.getSimpleName(), s.getName());
		exec(registerScript, false);
	}

	/**
	 * runs the javaConsole.java script which creates a Java Console object and
	 * redirect stdout & stderr to published data - these are hooked by the GUIService
	 */
	public void attachPythonConsole() {
		if (!javaConsoleInitialized) {
			// String consoleScript =
			// FileIO.getResourceFile("java/examples/javaConsole.java");
			String consoleScript = getServiceResourceFile("examples/javaConsole.java");
			exec(consoleScript, false);
		}
	}

	// PyObject interp.eval(String s) - for verifying?

	/**
	 * 
	 */
	public void createJavaInterpreter() {
		// TODO - check if exists - destroy / de-initialize if necessary
		// PySystemState.initialize();
		ArgumentParser argParser = new ArgumentParser();
		argParser.supportOption("classpath",
				IOUtil.WORKING_DIRECTORY.toString());
		argParser.supportAlias("cp", "classpath");
		ArgumentParser.Result parsedArgs = argParser.parse(".");
		Iterable<File> cp = IOUtil.parsePath(parsedArgs
				.getUnaryOption("classpath"));
		ArrayList<String> als = new ArrayList<String>();
//		for (File f : cp) {
//			
//			try {
//				
//				System.out.println(f.getCanonicalPath());
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		DynaComp.decompiler.setClassPath(als.toArray(new String[]{}));
		interp = new Interpreter(new MyOptions(), new PathClassLoader(cp));

		// add self reference
		// Java scripts can refer to this service as 'java' regardless
		// of the actual name
		String selfReferenceScript = String
				.format(// "import org.myrobotlab.service.Runtime;\n" +
						// "import org.myrobotlab.service.Java;\n"
				"java = org.myrobotlab.service.Runtime.create(\"%1$s\",\"Java\");\n\n" // TODO
																						// -
				// deprecate
						+ "runtime = org.myrobotlab.service.Runtime.getInstance();\n\n"
						+ "myService = org.myrobotlab.service.Runtime.create(\"%1$s\",\"Java\");\n",
						this.getName());
		// PyObject compiled = getCompiledMethod("initializeJava",
		// selfReferenceScript, interp);

		try {
			interp.interpret(selfReferenceScript);
		} catch (InterpreterException e) {
			e.printStackTrace();
		}
	}

	/**
	 * replaces and executes current Java script
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
	 * replaces and executes current Java script if replace = false - will not
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
		if (interp == null) {
			createJavaInterpreter();
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
		return "Java IDE";
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
	 * Here all messages allowed to go and effect the Java service will be let
	 * through. However, all messsages not found in this filter will go "into"
	 * they Java script. There they can be handled in the scripted users code.
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
			createJavaInterpreter();
		}

		StringBuffer msgHandle = new StringBuffer().append("msg_")
				.append(msg.sender).append("_").append(msg.sendingMethod);
		log.debug(String.format("calling %1$s", msgHandle));
		// use a compiled version to make it easier on us
		// PyObject compiledObject = getCompiledMethod(msgHandle.toString(),
		// String.format("%1$s()", msg.method), interp);
		String fi = msg.sender + "." + String.format("%1$s()", msg.method)
				+ ";";
		try {
			interp.interpret(fi);
		} catch (InterpreterException e) {
			e.printStackTrace();
		}

		return false;
	}

	// /**
	// * Get a compiled version of the java call.
	// *
	// * @param msg
	// * @param interp
	// * @return
	// */
	// private static synchronized PyObject getCompiledMethod(String name,
	// String code, JavaInterpreter interp) {
	// // TODO change this from a synchronized method to a few blocks to
	// // improve concurrent performance
	// if (objectCache.containsKey(name)) {
	// return objectCache.get(name);
	// }
	// PyObject compiled = interp.compile(code);
	// if (objectCache.size() > 5) {
	// // keep the size to 6
	// objectCache.remove(objectCache.keySet().iterator().next());
	// }
	// objectCache.put(name, compiled);
	// return compiled;
	// }

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
			// interp..cleanup();
			interp = null;
		}
	}

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

	/**
	 * this method can be used to load a Java script from the Java's local file
	 * system, which may not be the GUIService's local system. Because it can be done
	 * programatically on a different machine we want to broadcast our changed
	 * state to other listeners (possibly the GUIService)
	 * 
	 * @param filename
	 *            - name of file to load
	 * @return - success if loaded
	 */
	public boolean loadScript(String filename) {
		String newCode = FileIO.fileToString(filename);
		if (newCode != null && !newCode.isEmpty()) {
			log.info(String.format("replacing current script with %1s",
					filename));

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

	public static int untitledDocuments = 0;

	/*
	 * public static String getName(String filename) { if (filename == null) {
	 * ++untitledDocuments; filename = String.format("untitled.%d",
	 * untitledDocuments);
	 * 
	 * } int end = filename.lastIndexOf(".java"); int begin =
	 * filename.lastIndexOf(File.separator); if (begin > 0) { ++begin; } else {
	 * begin = 0; } if (end < 0) { end = filename.length(); } return
	 * filename.substring(begin, end); }
	 */

	public boolean loadScriptFromResource(String filename) {
		log.debug(String.format("loadScriptFromResource scripts/%1s", filename));
		String newCode = getServiceResourceFile(String.format("examples/%1s",
				filename));

		log.info(String.format("loaded new scripts/%1s size %d", filename,
				newCode.length()));
		if (newCode != null && !newCode.isEmpty()) {
			log.info(String.format("replacing current script with %1s",
					filename));

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

	public Object interpret(String string) {
		log.info("Interpreting: " + string);
		try {
			Option o = interp.interpret(string);
			return o.unwrap(null);
		} catch (InterpreterException e) {
			e.printStackTrace();
			return e;
		}
	}

	public String appendScript(String data) {
		currentScript.setCode(String.format("%s\n%s", currentScript.getCode(),
				data));
		return data;
	}

	public void reflect(Object o) {
		reflector.toplevel(o);
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		// String f = "C:\\Program Files\\blah.1.java";
		// log.info(getName(f));

		Runtime.createAndStart("java", "Java");
		Runtime.createAndStart("gui", "GUIService");

	}

	public boolean isPrimitive(String string) {
		try {
			interp.interpret(string + ".toString()");
			return false;
		} catch (InterpreterException e) {
			return true;
		}
	}

	public boolean isArray(String string) {
		if (string==null)return false;
		try {
			interp.interpret(string + ".length");
			return true;
		} catch (InterpreterException e) {
			// e.printStackTrace();
			return false;
		}
		catch(Exception e){
			return false;
		}
	}

	class MyOptions extends Options {
		@Override
		public boolean requireVariableType() {
			return false;
		}

		@Override
		public boolean enforceAllAccess() {
			return false;
		}

		@Override
		public boolean enforcePrivateAccess() {
			return false;
		}

		@Override
		public boolean prohibitUncheckedCasts() {
			return false;
		}

		@Override
		public boolean prohibitBoxing() {
			return false;
		}

		@Override
		public boolean requireSemicolon() {
			return false;
		}
	}
}
