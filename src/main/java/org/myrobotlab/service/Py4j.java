package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Executor;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.StreamGobbler;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Script2;
import org.slf4j.Logger;

import py4j.GatewayServer;
import py4j.GatewayServerListener;
import py4j.Py4JServerConnection;

public class Py4j extends Service implements GatewayServerListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Py4j.class);

  private transient GatewayServer gateway = null;

  private transient Executor handler = null;

  protected HashMap<String, Script2> openedScripts = new HashMap<String, Script2>();

  /**
   * py4j clients currently attached to this service
   */
  protected Map<String, Py4jClient> clients = new HashMap<>();

  /**
   * script root directory - all script filenames will be relative to this
   */
  protected String scriptRootDir = null;

  public Py4j(String n, String id) {
    super(n, id);
  }

  /**
   * upserts a script in memory
   * 
   * @param scriptName
   * @param code
   * @return
   */
  public void updateScript(String scriptName, String code) {
    if (openedScripts.containsKey(scriptName)) {
      Script2 script = openedScripts.get(scriptName);
      script.code = code;
    } else {
      error("cannot find script %s to update", scriptName);
    }
  }

  /**
   * start the gateway service listening on port
   */
  public void start() {
    if (gateway == null) {
      gateway = new GatewayServer(this);
      gateway.addListener(this);
      gateway.start();
      info("server started listening on %s:%d", gateway.getAddress(), gateway.getListeningPort());
      handler = (Executor) gateway.getPythonServerEntryPoint(new Class[] { Executor.class });
    } else {
      log.info("Py4j gateway server already started");
    }
  }

  /**
   * Opens a new or existing script. All file operations will be relative to the
   * data/Py4j/{serviceName} directory.
   * 
   * @param scriptName
   *          - name of the script file relatie to scriptRootDir
   *          data/Py4j/{serviceName}/
   * @param code
   *          - code in that file
   * @throws IOException
   */
  public void openScript(String scriptName, String code) throws IOException {

    File script = new File(scriptRootDir + fs + scriptName);

    if (script.exists()) {
      code = FileIO.toString(script);
    }

    openedScripts.put(scriptName, new Script2(scriptName, code));
    broadcastState();
  }

  public void saveScript(String scriptName, String code) throws IOException {
    FileIO.toFile(scriptRootDir + fs + scriptName, code);
    info("saved file %s", scriptName);
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

  /**
   * One of 3 methods supported on the MessageHandler() callbacks
   * 
   * @param code
   */
  public void exec(String code) {
    if (handler != null) {
      handler.exec(code);
    }
  }

  @Override
  public boolean preProcessHook(Message msg) {
    // let the messages for this service
    // get processed normally
    if (methodSet.contains(msg.method)) {
      return true;
    }

    // will probably need to queu this
    if (handler != null) {
      // TODO - determine clients are connected .. how many clients etc..
      try {
        handler.invoke(msg.method, msg.data);
      } catch (Exception e) {
        error(e);
      }
    }
    return false;
  }

  /**
   * stop the gateway service
   */
  public void stop() {
    if (gateway != null) {
      gateway.shutdown();
      gateway = null;
    } else {
      log.info("Py4j gateway server already stopped");
    }
  }
  
  public class WaitForProcess extends Thread{
    public Process process;
    public Integer exitCode; 
    public Py4j py4j;
    
    public WaitForProcess(Py4j py4j, Process process) {
      super(String.format("%s-process-signal", py4j.getName()));
      this.process = process;
      this.py4j = py4j;
    }
    public void run() {
      try {
        exitCode = process.waitFor();
      } catch (InterruptedException e) {
      }
      warn("process %s terminated with exit code %d", process.toString(), exitCode);
    }
  }

  /**
   * POJO class to tie all the data elements of a external python process
   * together. Including the process handler, the std out, std err streams and
   * termination signal thread.
   * 
   * @author perry
   *
   */
  class Py4jClient {
    // py4j connection
    public transient Py4JServerConnection connection;
    public transient StreamGobbler gobbler;
    public transient Process process;
    public transient Thread waitFor;
    public transient Py4j py4j;

    @Deprecated /* figure out a way to connect the process to the connection */
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

  protected Py4jClient pythonProcess = null;

  public void startPythonProcess() {
    try {

      // Specify the Python script path and arguments
      String pythonScript = new File(getResourceDir() + fs + "Py4j.py").getAbsolutePath();
      String[] pythonArgs = {};

      // Build the command to start the Python process
      ProcessBuilder processBuilder = new ProcessBuilder("/usr/bin/python", pythonScript);
      processBuilder.redirectErrorStream(true);
      processBuilder.command().addAll(List.of(pythonArgs));

      // Start the Python process
      pythonProcess = new Py4jClient(this, processBuilder.start());

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // https://stackoverflow.com/questions/23157424/py4j-how-would-i-go-about-on-calling-a-python-method-in-java
  public interface PythonInterface {
    public Message onMsg(Message msg);
  }

  // TODO - now just need to set a reference of callbacks
  public void PythonCall(PythonInterface callback, Message msg) {
    callback.onMsg(msg);
    // return numbers;
  }

  @Override
  public void connectionError(Exception e) {
    error(e);
  }

  @Override
  public void connectionStarted(Py4JServerConnection gatewayConnection) {
    log.info("connectionStarted {}",gatewayConnection.toString());
    clients.put(getClientKey(gatewayConnection), new Py4jClient());
    info("connection started");
    invoke("getClients");
  }

  private String getClientKey(Py4JServerConnection gatewayConnection) {
    return String.format("%s:%d", gatewayConnection.getSocket().getInetAddress(), gatewayConnection.getSocket().getPort());
  }

  @Override
  public void connectionStopped(Py4JServerConnection gatewayConnection) {
    info("connection stopped");
    clients.remove(getClientKey(gatewayConnection));
    invoke("getClients");
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

  public void handleStdOut(String msg) {
    invoke("publishStdOut", msg);
  }

  public String publishStdOut(String data) {
    return data;
  }

  public void startService() {
    super.startService();
    scriptRootDir = new File(getDataInstanceDir()).getAbsolutePath();
    File dataDir = new File(scriptRootDir);
    dataDir.mkdirs();
    start();
    // TODO - start Python process with Runtime.exec("python"

    // wait a second for the server to start listening...
    sleep(300);

    startPythonProcess();

  }

  public void stopService() {
    super.stopService();
    stop();
  }

  /**
   * return a set of client connections
   * 
   * @return
   */
  public Set<String> getClients() {
    return clients.keySet();
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      // Runtime.start("servo", "Servo");
      Py4j py4j = (Py4j) Runtime.start("py4j", "Py4j");
      // py4j.start();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
