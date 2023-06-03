package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.StreamGobbler;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.Py4jConfig;
import org.myrobotlab.service.data.Script;
import org.myrobotlab.service.interfaces.Executor;
import org.slf4j.Logger;

import py4j.GatewayServer;
import py4j.GatewayServerListener;
import py4j.Py4JServerConnection;

/**
 * 
 * 
 * A bridge between a native proces of Python running and MRL.
 * Should support any version of Python. 
 * <pre>
 *  requirements: 
 * 
 *  1.  some version of python is installed and the python
 *      executable is available in the PATH.  Py4j service will start the
 *      default python and run a small script resource/Py4j/Py4j.py
 *      
 *  2.  pip install py4j.
 *  
 *  TODO:
 *  1.  Support multiple instances of Py4j running - this requires more management 
 *  of the service ports
 *  2. Perhaps asynchronous calling of handler ?
 *
 * </pre>
 * 
 * @author GroG
 */
public class Py4j extends Service implements GatewayServerListener {

  /**
   * POJO class to tie all the data elements of a external python process
   * together. Including the process handler, the std out, std err streams and
   * termination signal thread.
   * 
   * @author GroG
   *
   */
  class Py4jClient {
    // py4j connection
    public transient Py4JServerConnection connection;
    public transient StreamGobbler gobbler;
    public transient Process process;
    public transient Py4j py4j;
    public transient Thread waitFor;

    /* TODO figure out a way to connect the process to the connection */
    public Py4jClient() {
    }

    public Py4jClient(Py4j py4j, Process process) {
      this.process = process;
      this.py4j = py4j;
      this.gobbler = new StreamGobbler(String.format("%s-gobbler", getName()), process.getInputStream());
      this.gobbler.start();
      this.waitFor = new WaitForProcess(py4j, process);
      this.waitFor.start();

      log.info("process started {}", process);
    }
  }

  /**
   * A class to wait on a process signal and notify client is disconnected
   * 
   * @author GroG
   *
   */
  public class WaitForProcess extends Thread {
    public Integer exitCode;
    public Process process;
    public Py4j py4j;

    public WaitForProcess(Py4j py4j, Process process) {
      super(String.format("%s-process-signal", py4j.getName()));
      this.process = process;
      this.py4j = py4j;
    }

    @Override
    public void run() {
      try {
        exitCode = process.waitFor();
      } catch (InterruptedException e) {
      }
      warn("process %s terminated with exit code %d", process.toString(), exitCode);
    }
  }

  public final static Logger log = LoggerFactory.getLogger(Py4j.class);

  private static final long serialVersionUID = 1L;

  /**
   * py4j clients currently attached to this service
   */
  protected Map<String, Py4jClient> clients = new HashMap<>();

  /**
   * Java server side gateway for the python process to attach default port
   * 25333
   */
  private transient GatewayServer gateway = null;

  /**
   * the all important interface to the Python MessageHandler This defines what
   * Java can call that will rpc'd into Python's MessageHandler
   */
  private transient Executor handler = null;

  /**
   * Opened scripts are scripts opened in memory, from there they can be
   * executed or saved to the file system, or updatd in memory which the js
   * client does
   */
  protected HashMap<String, Script> openedScripts = new HashMap<String, Script>();

  /**
   * client process and connectivity reference
   */
  protected Py4jClient pythonProcess = null;

  public Py4j(String n, String id) {
    super(n, id);
  }

  /**
   * Add a new script to Py4j default location will be in
   * data/Py4j/{serviceName}
   * 
   * @param scriptName
   *          - name of the script
   * @param code
   *          - code block
   * @throws IOException
   */
  public void addScript(String scriptName, String code) throws IOException {
    Py4jConfig c = (Py4jConfig)config;
    File script = new File(c.scriptRootDir + fs + scriptName);

    if (script.exists()) {
      error("script %s already exists", scriptName);
      return;
    }

    openedScripts.put(scriptName, new Script(scriptName, code));
    broadcastState();
  }

  /**
   * removes script from memory of openScripts
   * 
   * @param scriptName
   */
  public void closeScript(String scriptName) {
    openedScripts.remove(scriptName);
    broadcastState();
  }

  @Override
  public void connectionError(Exception e) {
    error(e);
  }

  @Override /** TODO add a one shot addTask to call handler.setName(name) */
  public void connectionStarted(Py4JServerConnection gatewayConnection) {
    try {
      log.info("connectionStarted {}", gatewayConnection.toString());
      clients.put(getClientKey(gatewayConnection), new Py4jClient());

      info("connection started");
      invoke("getClients");
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public void connectionStopped(Py4JServerConnection gatewayConnection) {
    info("connection stopped");
    clients.remove(getClientKey(gatewayConnection));
    invoke("getClients");
  }

  /**
   * One of 3 methods supported on the MessageHandler() callbacks
   * 
   * @param code
   */
  public void exec(String code) {
    try {
      if (handler != null) {
        handler.exec(code);
      } else {
        error("handler is null");
      }
    } catch (Exception e) {
      error(e);
    }
  }

  private String getClientKey(Py4JServerConnection gatewayConnection) {
    return String.format("%s:%d", gatewayConnection.getSocket().getInetAddress(), gatewayConnection.getSocket().getPort());
  }

  /**
   * return a set of client connections - probably could be deprecated to a
   * single client, but was not sure
   * 
   * @return
   */
  public Set<String> getClients() {
    return clients.keySet();
  }

  /**
   * get listing of filesystem files location will be data/Py4j/{serviceName}
   * 
   * @return
   * @throws IOException
   */
  public List<String> getScriptList() throws IOException {
    List<String> sorted = new ArrayList<>();
    Py4jConfig c = (Py4jConfig)config;
    List<File> files = FileIO.getFileList(c.scriptRootDir, true);
    for (File file : files) {
      if (file.toString().endsWith(".py")) {
        sorted.add(file.toString().substring(c.scriptRootDir.length() + 1));
      }
    }
    Collections.sort(sorted);
    return sorted;
  }

  public void handleStdOut(String msg) {
    invoke("publishStdOut", msg);
  }

  /**
   * Potential entry point for python message
   * 
   * @param code
   */
  public void onPython(String code) {
    log.info("onPython {}", code);
    exec(code);
  }

  /**
   * Opens an example "service" script maintained in myrobotlab
   * 
   * @param serviceType
   *          the type of service
   * @throws IOException
   */
  public void openExampleScript(String serviceType) throws IOException {
    String filename = getResourceRoot() + fs + serviceType + fs + String.format("%s.py", serviceType);
    String serviceScript = null;
    try {
      serviceScript = FileIO.toString(filename);
    } catch (Exception e) {
      error("%s.py not  found", serviceType);
      log.error("getting service file script example threw {}", e);
    }
    addScript(serviceType + ".py", serviceScript);
  }

  /**
   * Opens an existing script. All file operations will be relative to the
   * data/Py4j/{serviceName} directory.
   * 
   * @param scriptName
   *          - name of the script file relatie to scriptRootDir
   *          data/Py4j/{serviceName}/
   * @throws IOException
   */
  public void openScript(String scriptName) throws IOException {
    Py4jConfig c = (Py4jConfig)config;
    File script = new File(c.scriptRootDir + fs + scriptName);

    if (!script.exists()) {
      error("file %s not found", script.getAbsolutePath());
      return;
    }

    openedScripts.put(scriptName, new Script(scriptName, FileIO.toString(script.getAbsoluteFile())));
    broadcastState();
  }

  @Override
  public boolean preProcessHook(Message msg) {
    // let the messages for this service
    // get processed normally
    if (methodSet.contains(msg.method)) {
      return true;
    }

    // TODO - determine clients are connected .. how many clients etc..
    try {
      if (handler != null) {
        handler.invoke(msg.method, msg.data);
      } else {
        error("preProcessHook handler is null");
      }
    } catch (Exception e) {
      error(e);
    }
    return false;
  }

  public String publishStdOut(String data) {
    return data;
  }

  /**
   * Saves a script to the file system default will be in
   * data/Py4j/{serviceName}/{scriptName}
   * 
   * @param scriptName
   * @param code
   * @throws IOException
   */
  public void saveScript(String scriptName, String code) throws IOException {
    Py4jConfig c = (Py4jConfig)config;
    FileIO.toFile(c.scriptRootDir + fs + scriptName, code);
    info("saved file %s", scriptName);
  }

  @Override
  public void serverError(Exception e) {
    error("server error");
    error(e);
  }

  @Override
  public void serverPostShutdown() {
    info("%s post shutdown", getName());
  }

  @Override
  public void serverPreShutdown() {
    info("%s pre shutdown", getName());
  }

  @Override
  public void serverStarted() {
    info("%s started", getName());
  }

  @Override
  public void serverStopped() {
    info("%s stopped", getName());
  }

  /**
   * start the gateway service listening on port
   */
  public void start() {
    try {
      if (gateway == null) {
        gateway = new GatewayServer(this);
        gateway.addListener(this);
        gateway.start();
        info("server started listening on %s:%d", gateway.getAddress(), gateway.getListeningPort());
        handler = (Executor) gateway.getPythonServerEntryPoint(new Class[] { Executor.class });
      } else {
        log.info("Py4j gateway server already started");
      }
    } catch (Exception e) {
      error(e);
    }
  }

  /**
   * function which start the python process and begins the client
   * MessageHandler and setup for runtime references to work
   */
  public void startPythonProcess() {
    try {

      // Specify the Python script path and arguments
      String pythonScript = new File(getResourceDir() + fs + "Py4j.py").getAbsolutePath();
      String[] pythonArgs = {};

      // Build the command to start the Python process
      ProcessBuilder processBuilder = new ProcessBuilder("python", pythonScript);
      processBuilder.redirectErrorStream(true);
      processBuilder.command().addAll(List.of(pythonArgs));

      // Start the Python process
      pythonProcess = new Py4jClient(this, processBuilder.start());

      // CRITICAL SLEEP ! - next call will be a handler method
      // it will take a little time for the python interpreter to start
      // the MessageHandler() code to execute and the python
      // client establish a connection ... TODO: better solution would probably
      // to be
      // do a "one shot addTask" to connection started .. it CANNOT be called
      // directly in the onConnection event
      // otherwise it will deadlock
      sleep(1500);

      // sets the name of the message handler
      // calling the handler when the connection is not established
      handler.setName(getName());

    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public void startService() {
    super.startService();
    Py4jConfig c = (Py4jConfig)config;
    if (c.scriptRootDir == null) {
        c.scriptRootDir = new File(getDataInstanceDir()).getAbsolutePath();
    }
    File dataDir = new File(c.scriptRootDir);
    dataDir.mkdirs();
    // start the py4j socket server
    start();
    sleep(300);
    // start the python process which starts the Py4j.py MessageHandler
    startPythonProcess();
  }

  /**
   * stop the gateway service and teardown of the python process
   */
  public void stop() {
    if (gateway != null) {
      log.info("stopping py4j gateway");
      gateway.shutdown();
      gateway = null;
    } else {
      log.info("Py4j gateway server already stopped");
    }

    handler = null;

    if (pythonProcess != null) {
      log.info("shutting down python process");
      pythonProcess.process.destroy();
    }
  }

  /**
   * shutdown cleanly
   */
  @Override
  public void stopService() {
    super.stopService();
    stop();
  }

  /**
   * updates a script in memory
   * 
   * @param scriptName
   * @param code
   * @return
   */
  public void updateScript(String scriptName, String code) {
    if (openedScripts.containsKey(scriptName)) {
      Script script = openedScripts.get(scriptName);
      script.code = code;
    } else {
      error("cannot find script %s to update", scriptName);
    }
  }
  
  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      // Runtime.start("servo", "Servo");
      Py4j py4j = (Py4j) Runtime.start("py4j", "Py4j");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
  
  
}

