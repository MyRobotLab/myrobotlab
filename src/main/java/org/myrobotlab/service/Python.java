package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.FindFile;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.PythonConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.Script;
import org.myrobotlab.service.interfaces.ServiceLifeCycleListener;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyList;
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
public class Python extends Service implements ServiceLifeCycleListener {

  /**
   * this thread handles all callbacks to Python process all input and sets msg
   * handles
   * 
   */
  public class InputQueue implements Runnable {
    transient protected Python python;
    protected volatile boolean running = false;
    transient protected Thread myThread = null;

    public InputQueue(Python python) {
      this.python = python;
    }

    @Override
    public void run() {
      try {
        running = true;
        while (running) {

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
            StringBuffer msgHandle = new StringBuffer().append("msg_").append(CodecUtils.getSafeReferenceName(msg.sender)).append("_").append(msg.sendingMethod);
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

            interp.exec(compiledObject);

          } catch (Exception e) {
            log.error("InputQueueThread threw msg: {}", msg, e);
            python.error(String.format("%s %s", e.getClass().getSimpleName(), e.getMessage()));
          }
        }
      } catch (Exception e) {
        if (e instanceof InterruptedException) {
          info("shutting down %s", getName());
        } else {
          log.error("InputQueueThread while loop threw", e);
        }
      }
      log.info("shutting down python queue");
    }

    synchronized public void stop() {
      if (myThread != null) {
        running = false;
        myThread.interrupt();
        myThread = null;
      }
    }

    synchronized public void start() {
      if (myThread == null) {
        myThread = new Thread(this, String.format("python.%s.input", python.getName()));
        myThread.start();
      } else {
        log.warn("python input queue already running");
      }
    }
  }

  class PIThread extends Thread {
    private String code;
    public boolean executing = false;

    PIThread(String name, String code) {
      super(name);
      this.code = code;
    }

    @Override
    public void run() {
      try {
        if (interp == null) {
          log.warn("cannot run script - python interpreter is null - not initialized yet ?");
          return;
        }

        executing = true;
        interp.exec(code);

      } catch (Exception e) {
        log.error("python exec threw", e);
        String error = Logging.stackToString(e);
        if (error.contains("KeyboardInterrupt")) {
          warn("Python process killed !");
        } else {
          error(e);
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
          log.error("following script errored \n{}", code);
          log.error("interp.exec threw", e);
          if (filtered.length() > 40) {
            filtered = filtered.substring(0, 40);
          }
        }

      } finally {
        executing = false;
        log.info("script completed");
        invoke("finishedExecutingScript");
      }
    }
  }

  public final static transient Logger log = LoggerFactory.getLogger(Python.class);
  // TODO this needs to be moved into an actual cache if it is to be used
  // Cache of compile python code
  private static final transient HashMap<String, PyObject> objectCache = new HashMap<String, PyObject>();

  private static final long serialVersionUID = 1L;

  protected int newScriptCnt = 0;

  final List<String> startScripts = new ArrayList<>();

  final List<String> stopScripts = new ArrayList<>();

  /**
   * Any script executed is put in a openedScripts map... Helpful in IDE
   * displays
   */
  protected boolean openOnExecute = true;

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
   * Set a Python variable with a value from Java e.g. python.set("my_var", 5)
   * 
   * @param pythonRefName
   *          python variable name
   * @param o
   *          value to set
   */
  public void set(String pythonRefName, Object o) {
    interp.set(pythonRefName, o);
  }

  /**
   * Get a Python value from Python into Java return type is PyObject wrapper
   * around the value
   * 
   * @param pythonRefName
   *          - name of variable
   * @return the PyObject wrapper
   */
  public PyObject getPyObject(String pythonRefName) {
    return interp.get(pythonRefName);
  }

  /**
   * Get the value of the Python variable e.g. Integer x =
   * (Integer)python.getValue("my_var")
   * 
   * @param pythonRefName
   *          the reference name for the python object
   * @return the python object
   */
  public Object get(String pythonRefName) {
    PyObject o = getPyObject(pythonRefName);
    if (o == null) {
      return null;
    }
    if (o instanceof PyString) {
      return o.toString();
    } else if (o instanceof PyFloat) {
      return ((PyFloat) o).getValue();
    } else if (o instanceof PyInteger) {
      return ((PyInteger) o).getValue();
    } else if (o instanceof PyList) {
      return ((PyList) o).getArray();
    }
    return o;
  }

  /**
   * FIXME - buildtime package in resources pyrobotlab python service urls -
   * created for referencing script
   */
  Map<String, String> exampleFiles = new TreeMap<String, String>();

  transient LinkedBlockingQueue<Message> inputQueue = new LinkedBlockingQueue<Message>();
  final transient InputQueue inputQueueThread;
  transient PythonInterpreter interp = null;
  transient Map<String, PIThread> interpThreads = new HashMap<String, PIThread>();

  int interpreterThreadCount = 0;

  /**
   * local current directory of python script any new python script will get
   * localScriptDir prefix
   */
  String localScriptDir = new File(FileIO.getCfgDir()).getAbsolutePath();

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

  String activeScript = null;

  public Python(String n, String id) {
    super(n, id);

    log.info("created python {}", getName());

    log.info("creating module directory pythonModules");
    new File("pythonModules").mkdir();

    inputQueueThread = new InputQueue(this);

    // I love ServiceData !
    ServiceData sd = ServiceData.getLocalInstance();
    // I love Platform !
    Platform p = Platform.getLocalInstance();
    List<MetaData> sdt = sd.getAvailableServiceTypes();
    for (int i = 0; i < sdt.size(); ++i) {
      MetaData st = sdt.get(i);
      // FIXME - cache in "data" dir Or perhaps it should be pulled into
      // resource directory during build time and packaged with jar
      String file = String.format("%s/%s.py", st.getSimpleName(), st.getSimpleName());
      exampleFiles.put(st.getSimpleName(), file);
    }

    localPythonFiles = getFileListing();

    createPythonInterpreter();
    attachPythonConsole();

    String selfReferenceScript = "from time import sleep\nfrom org.myrobotlab.framework import Platform\n" + "from org.myrobotlab.service import Runtime\n"
        + "from org.myrobotlab.framework import Service\n" + "from org.myrobotlab.service import Python\n"
        + String.format("%s = Runtime.getService(\"%s\")\n\n", CodecUtils.getSafeReferenceName(getName()), getName()) + "Runtime = Runtime.getInstance()\n\n"
        + String.format("runtime = Runtime.getInstance()\n") + String.format("myService = Runtime.getService(\"%s\")\n", getName());
    // FIXME !!! myService is SO WRONG it will collide on more than 1 python
    // service :(
    PyObject compiled = getCompiledMethod("initializePython", selfReferenceScript, interp);
    interp.exec(compiled);

    // initialize all the pre-existing service before python was created
    Map<String, ServiceInterface> services = Runtime.getLocalServices();
    for (ServiceInterface service : services.values()) {
      if (service.isRunning()) {
        onStarted(service.getName());
      }
    }

    log.info("starting python {}", getName());
    inputQueueThread.start();
    log.info("started python {}", getName());
  }

  public void newScript() {
    if (!openedScripts.containsKey("script.py")) {
      openScript("script.py", "");
    }
  }

  public void openScript(String scriptName, String code) {
    activeScript = scriptName;
    openedScripts.put(scriptName, new Script(scriptName, code));
    broadcastState();
  }

  public void closeScript(String scriptName) {
    openedScripts.remove(scriptName);
    broadcastState();
  }

  /**
   * append more Python to the current script
   * 
   * @param data
   *          the code to append
   * @return the resulting concatenation
   */
  public Script appendScript(String data) {
    return new Script("append", data);
  }

  /**
   * runs the pythonConsole.py script which creates a Python Console object and
   * redirect stdout &amp; stderr to published data - these are hooked by the
   * SwingGui
   */
  public void attachPythonConsole() {
    if (!pythonConsoleInitialized) {
      // FIXME - this console script has hardcoded globals to
      // reference this service that will break with more than on python service
      // !
      String consoleScript = getResourceAsString("pythonConsole.py");
      exec(consoleScript, false);
      pythonConsoleInitialized = true;
    }
  }

  /**
   * 
   */
  synchronized public void createPythonInterpreter() {
    if (interp != null) {
      log.info("interpreter already created");
      return;
    }
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
     * don't respect java accessibility, so that we can access protected members
     * on subclasses - NO ! - future versions of java will not allow this !
     * removing (GroG 20210404)
     */
    // props.put("python.security.respectJavaAccessibility", "false");
    props.put("python.import.site", "false");

    Properties preprops = System.getProperties();

    PythonInterpreter.initialize(preprops, props, new String[0]);

    interp = new PythonInterpreter();

    PySystemState sys = Py.getSystemState();

    if (modulesDir != null) {
      sys.path.append(new PyString(modulesDir));
    }

    sys.path.append(new PyString(Runtime.getInstance().getConfigDir()));

    log.info("Python System Path: {}", sys.path);

  }

  public String eval(String method) {
    String jsonMethod = String.format("%s()", method);
    PyObject o = interp.eval(jsonMethod);
    String ret = o.toString();
    return ret;
  }

  /**
   * execute code (blocking)
   * 
   * @param code
   *          string of code to run
   * @return true/false
   */
  public boolean exec(String code) {
    return exec(code, true);
  }

  /**
   * FIXME - isn't "blocking" exec == eval ???
   * 
   * This method will execute a string that represents a python script. When
   * called with blocking=false, the return code will likely return true even if
   * there is a syntax error because it doesn't wait for the response.
   * 
   * @param code
   *          - the script to execute
   * @param blocking
   *          - if true, this method will wait until all of the code has been
   *          evaluated.
   * @return - returns true if execution of the code was successful. returns
   *         false if there was an exception.
   */
  public boolean exec(String code, boolean blocking) {
    log.info("exec(String) \n{}", code);

    try {
      if (!blocking) {
        String name = String.format("%s.interpreter.%d", getName(), ++interpreterThreadCount);
        PIThread interpThread = new PIThread(name, code);
        interpThread.start();
        interpThreads.put(name, interpThread);
      } else {
        interp.exec(code);
      }
      return true;
    } catch (PyException pe) {
      // something specific with a python error
      error(pe.toString());
      invoke("publishStdError", pe.toString());
    } catch (Exception e) {
      error(e);
    } finally {
      if (blocking) {
        invoke("finishedExecutingScript");
      }
    }
    return false;
  }

  /**
   * This method will execute and block a string that represents a python
   * script. Python return statement as return
   * 
   * @param code
   *          - the script to execute
   * @return - returns String of python return statement
   */
  public String evalAndWait(String code) {
    // moz4r : eval() no worky for what I want, don't want to mod it & break
    // things
    String pyOutput = null;
    log.info("eval(String) \n{}", code);
    if (interp == null) {
      createPythonInterpreter();
    }
    try {
      pyOutput = interp.eval(code).toString();
    } catch (PyException pe) {
      // something specific with a python error
      error(pe.toString());
      log.error("evalAndWait threw python exception", pe);
    } catch (Exception e) {
      // more general error handling.
      error(e.getMessage());
      // dump stack trace to log
      log.error("evalAndWait threw", e);
    }
    return pyOutput;
  }

  public void execAndWait(String code) {
    exec(code, true);
  }

  /**
   * executes an external Python file
   * 
   * @param filename
   *          the full path name of the python file to execute
   * @return true/false
   * @throws IOException
   *           boom
   */
  public boolean execFile(String filename) throws IOException {
    return execFile(filename, true);
  }

  /**
   * executes an external Python file
   * 
   * @param filename
   *          file to exec
   * @param block
   *          true if blocking exec
   * @return true/false
   * @throws IOException
   *           boom
   * 
   */
  public boolean execFile(String filename, boolean block) throws IOException {
    String script = FileIO.toString(filename);
    if (openOnExecute) {
      openScript(filename, script);
    }
    return exec(script);
  }

  /**
   * execute an "already" defined python method directly
   * 
   * @param method
   *          - the name of the method
   */
  public void execMethod(String method) {
    execMethod(method, (Object[]) null);
  }

  public void execMethod(String method, Object... parms) {
    Message msg = Message.createMessage(getName(), getName(), method, parms);
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
    log.info("finishedExecutingScript");
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

  /**
   * load a official "service" script maintained in myrobotlab
   * 
   * @param serviceType
   *          the type of service
   */
  public void loadServiceScript(String serviceType) {
    String filename = getResourceRoot() + fs + serviceType + fs + String.format("%s.py", serviceType);
    String serviceScript = null;
    try {
      serviceScript = FileIO.toString(filename);
    } catch (Exception e) {
      error("%s.py not  found", serviceType);
      log.error("getting service file script example threw {}", e);
    }
    openScript(filename, serviceScript);
  }

  @Deprecated
  public void loadPyRobotLabServiceScript(String serviceType) {
    loadServiceScript(serviceType);
  }

  /*
   * this method can be used to load a Python script from the Python's local
   * file system, which may not be the SwingGui's local system. Because it can
   * be done programatically on a different machine we want to broadcast our
   * changed state to other listeners (possibly the SwingGui)
   * 
   * @param filename - name of file to load
   */
  public void openScriptFromFile(String filename) throws IOException {
    log.info("loadScriptFromFile {}", filename);
    String data = FileIO.toString(filename);
    openScript(filename, data);
  }

  @Override
  public void onStarted(String serviceName) {
    ServiceInterface s = Runtime.getService(serviceName);
    if (s == null) {
      error("%s got started event from %s yet does not exist in registry", getName(), serviceName);
      return;
    }

    String registerScript = "from org.myrobotlab.framework import Platform\n" + "from org.myrobotlab.service import Runtime\n" + "from org.myrobotlab.framework import Service\n";

    // load the import
    // RIXME - RuntimeGlobals & static values for unknown
    if (!"unknown".equals(s.getSimpleName())) {

      registerScript += String.format("from org.myrobotlab.service import %s\n", s.getSimpleName());
    }

    registerScript += String.format("%s = Runtime.getService(\"%s\")\n", CodecUtils.getSafeReferenceName(s.getName()), s.getName());
    exec(registerScript, false);
  }

  @Override
  public void onReleased(String serviceName) {
    String registerScript = String.format("%s = None\n", CodecUtils.getSafeReferenceName(serviceName));
    exec(registerScript, false);
  }

  /**
   * preProcessHook is used to intercept messages and process or route them
   * before being processed/invoked in the Service.
   * 
   * Here all messages allowed to go and effect the Python service will be let
   * through. However, all messages not found in this filter will go "into" they
   * Python script. There they can be handled in the scripted users code.
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

  public String publishStdError(String data) {
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

  // @Override /* FIXME - make interface for it */
  public void defaultInvokeMethod(String method, Object... params) {
    if (interp == null) {
      createPythonInterpreter();
    }
    Message msg = Message.createMessage(getName(), getName(), method, params);
    // handling call-back input needs to be
    // done by another thread - in case its doing blocking
    // or is executing long tasks - the inbox thread needs to
    // be freed of such tasks - it has to do all the inbound routing
    inputQueue.add(msg);
  }

  @Override
  synchronized public void startService() {
    super.startService();
    Map<String, ServiceInterface> services = Runtime.getLocalServices();
    for (ServiceInterface s : services.values()) {
      onStarted(s.getName());
    }
    // register runtime life cycle events for other services
    Runtime.getInstance().attachServiceLifeCycleListener(getName());

    // run start scripts if there are any
    for (String script : startScripts) {
      // i think in this context its safer to block
      try {
        execFile(script, true);
      } catch (IOException e) {
        log.error("starting scripts threw",e);
      }
    }
  }

  @Override
  public void releaseService() {
    super.releaseService();
    stop();
    if (interp != null) {
      // PySystemState.exit(); // the big hammar' throws like Thor
      interp.cleanup();
      interp = null;
    }

    inputQueueThread.stop();
    thread.interruptAllThreads();
    Py.getSystemState()._systemRestart = true;
  }

  /**
   * stop all scripts (not sure the pros/cons of this management vs
   * thread.interruptAllThreads())
   * 
   * @return false
   */
  public boolean stop() {
    log.info("stopping all scripts");
    for (PIThread pt : interpThreads.values()) {
      if (pt.isAlive()) {
        pt.interrupt();
      }
    }
    interpThreads.clear();
    return false;
  }

  /**
   * stops threads releases interpreter
   */
  @Override
  public void stopService() {
    // run any stop scripts 
    for (String script : stopScripts) {
      // i think in this context its safer to block
      try {
        execFile(script, true);
      } catch (IOException e) {
        log.error("stopping scripts threw", e);
      }
    }
    // shutdown inbox/outbox
    super.stopService();
    // release the interpeter
    stop();
  }

  public boolean isOpenOnExecute() {
    return openOnExecute;
  }

  public void setOpenOnExecute(boolean openOnExecute) {
    this.openOnExecute = openOnExecute;
  }

  public static void main(String[] args) {
    try {
      Runtime.main(new String[] { "--id", "admin", "--from-launcher" });
      LoggingFactory.init("INFO");

      // Runtime.start("i01.head.rothead", "Servo");
      // Runtime.start("i01.head.neck", "Servo");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      Python python = (Python) Runtime.start("python", "Python");
      // python.execFile("data/adafruit.py");

      // Runtime.start("i01", "InMoov2");

    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

  @Override
  public void onCreated(String name) {

  }

  @Override
  public void onRegistered(Registration registration) {

  }

  @Override
  public void onStopped(String fullname) {

  }

  @Override
  public ServiceConfig getConfig() {
    PythonConfig config = new PythonConfig();
    config.startScripts = startScripts;
    return config;
  }

  public ServiceConfig apply(ServiceConfig c) {
    PythonConfig config = (PythonConfig) c;
    if (config.startScripts != null && config.startScripts.size() > 0) {
      startScripts.clear();
      startScripts.addAll(config.startScripts);
      // if were already running and told to load
      // we run the scripts - if this service has only been created
      // the startService method will run the start scripts
      if (isRunning()) {
        for (String script : startScripts) {
          try {
            execFile(script);
          } catch(Exception e) {
            error(e);
          }
        }
      }
    }
    
    if (config.stopScripts != null && config.stopScripts.size() > 0) {
      stopScripts.clear();
      stopScripts.addAll(config.stopScripts);
    }
        
    return c;
  }

}
