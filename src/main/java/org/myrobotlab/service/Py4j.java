package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.generics.SlidingWindowList;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.StreamGobbler;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Connection;
import org.myrobotlab.service.config.Py4jConfig;
import org.myrobotlab.service.data.Script;
import org.myrobotlab.service.interfaces.Executor;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.Processor;
import org.slf4j.Logger;

import py4j.GatewayServer;
import py4j.GatewayServerListener;
import py4j.Py4JServerConnection;

/**
 * 
 * 
 * A bridge between a native proces of Python running and MRL. Should support
 * any version of Python.
 * 
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
public class Py4j extends Service<Py4jConfig> implements GatewayServerListener, Gateway, Processor {

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

  /** DepthAI's wheel is large; a Pi on a slow link can take many minutes. */
  private static final long PIP_TIMEOUT_MS = 20L * 60L * 1000L;

  private static final class CommandResult {
    final int exit;
    final String output;

    CommandResult(int exit, String output) {
      this.exit = exit;
      this.output = output;
    }
  }

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
   * a sliding window of logs
   */
  protected List<String> logs = new SlidingWindowList<>(300);

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
    // ready when connected
    ready = false;
  }

  /**
   * Add a new script to Py4j default location will be in
   * data/Py4j/{serviceName}
   * 
   * @param scriptName
   *                   - name of the script
   * @param code
   *                   - code block
   */
  public void addScript(String scriptName, String code) {
    Py4jConfig c = (Py4jConfig) config;
    File script = new File(c.scriptRootDir + fs + scriptName);

    if (script.exists()) {
      error("script %s already exists", scriptName);
      return;
    }

    openedScripts.put(scriptName, new Script(scriptName, code));
    broadcastState();
  }

  /**
   * If autostartPython is true, Py4j will start a process on starting and
   * connect the stdout/stdin streams to be redirected to the UI
   * 
   * @param b
   * @return
   */
  public boolean autostartPython(boolean b) {
    config.autostartPython = b;
    if (config.autostartPython && pythonProcess == null) {
      startPythonProcess();
    }
    return b;
  }

  /**
   * removes script from memory of openScripts
   * 
   * @param scriptName
   *                   The name of the script to close.
   */
  public void closeScript(String scriptName) {
    openedScripts.remove(scriptName);
    broadcastState();
  }

  @Override
  public void connectionError(Exception e) {
    error(e);
  }

  @Override /* TODO add a one shot addTask to call handler.setName(name) */
  public void connectionStarted(Py4JServerConnection gatewayConnection) {
    try {
      log.info("connectionStarted {}", gatewayConnection.toString());
      clients.put(getClientKey(gatewayConnection), new Py4jClient());
      ready = true;
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
   *             The Python code to execute in the interpreter.
   */
  @Override
  public boolean exec(String code) {
    log.info(String.format("exec %s", code));
    try {
      if (handler != null) {
        handler.exec(code);
        return true;
      } else {
        error("handler is null");
      }
    } catch (Exception e) {
      error(e);
    }
    return false;
  }

  private String getClientKey(Py4JServerConnection gatewayConnection) {
    return String.format("%s:%d", gatewayConnection.getSocket().getInetAddress(),
        gatewayConnection.getSocket().getPort());
  }

  /**
   * get listing of filesystem files location will be data/Py4j/{serviceName}
   * 
   * @return
   * @throws IOException
   */
  public List<String> getScriptList() throws IOException {
    List<String> sorted = new ArrayList<>();
    log.debug("Py4j script list for {}", CodecUtils.toJson(config));
    Py4jConfig c = (Py4jConfig) config;
    List<File> files = FileIO.getFileList(c.scriptRootDir, true);
    for (File file : files) {
      if (file.toString().endsWith(".py")) {
        sorted.add(file.toString().substring(c.scriptRootDir.length() + 1));
      }
    }
    Collections.sort(sorted);
    return sorted;
  }

  /**
   * Sink for standard output from Py4j-related subprocesses. This method
   * immediately publishes the output on {@link #publishStdOut(String)}.
   *
   * @param msg
   *            The output from a py4j related subprocess.
   */
  public void handleStdOut(String msg) {
    if (!"\n".equals(msg)) {
      logs.add(msg);
      invoke("publishStdOut", msg);
    }
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

  public String onPythonMessage(Message msg) {
    // create wrapper to tunnel incoming message - include original sender?
    Message tunnelMsg = Message.createMessage(msg.sender, getName(), "onPythonMessage", msg);
    String json = CodecUtils.toJson(tunnelMsg);
    handler.send(json);
    return json;
  }

  /**
   * Opens an example "service" script maintained in myrobotlab
   * 
   * @param serviceType
   *                    the type of service
   * @throws IOException
   */
  public void openExampleScript(String serviceType) throws IOException {
    String filename = getResourceRoot() + fs + serviceType + fs + String.format("%s.py", serviceType);
    String serviceScript = null;
    try {
      serviceScript = FileIO.toString(filename);
    } catch (Exception e) {
      error("%s.py not  found", serviceType);
      log.error("getting service file script example threw", e);
    }
    addScript(serviceType + ".py", serviceScript);
  }

  /**
   * Opens an existing script. All file operations will be relative to the
   * data/Py4j/{serviceName} directory.
   * 
   * @param scriptName
   *                   - name of the script file relatie to scriptRootDir
   *                   data/Py4j/{serviceName}/
   * @throws IOException
   */
  public void openScript(String scriptName) throws IOException {
    Py4jConfig c = (Py4jConfig) config;
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
        // afaik - Py4j does some kind of magical encoding to get a JavaObject
        // back to the Python process, but:
        // 1. its useless for users - no way to access the content ?
        // 2. you can't do anything with it
        // So, I've chosen to json encode it here, and the Py4j.py
        // MessageHandler will
        // decode it into a Python dictionary \o/
        // we do single encoding including the parameter array - there is no
        // header needed
        // with method and other details, as the invoke here is invoking
        // directly in the
        // Py4j.py script

        String json = CodecUtils.toJson(msg);
        // handler.invoke(msg.method, json);
        log.info(String.format("handler %s", json));
        handler.send(json);
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
    if (scriptName != null && !scriptName.toLowerCase().endsWith(".py")) {
      scriptName = scriptName + ".py";
    }
    FileIO.toFile(config.scriptRootDir + fs + scriptName, code);
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

        // sleep(100);
        // String[] services = Runtime.getServiceNames();
        // sendRemote(Message.createMessage(getName(), "runtime",
        // "onServiceNames", services));

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
      if (isPythonRunning()) {
        return;
      }
      String venvPython;
      try {
        venvPython = ensureVenv();
      } catch (IOException e) {
        error(e.getMessage());
        return;
      }

      String pythonScript = new File(getResourceDir() + fs + "Py4j.py").getAbsolutePath();
      String py4jImport = moduleImportError(venvPython, new String[] { "py4j" }, 30_000);
      if (py4jImport != null) {
        error("py4j is not installed properly in the virtual environment. %s", py4jImport);
        return;
      }

      ProcessBuilder processBuilder = new ProcessBuilder(venvPython, pythonScript);
      processBuilder.redirectErrorStream(true);
      processBuilder.command().add(getFullName());
      pythonProcess = new Py4jClient(this, processBuilder.start());
    } catch (Exception e) {
      error(e);
    }
  }

  public boolean isPythonRunning() {
    return pythonProcess != null && pythonProcess.process != null && pythonProcess.process.isAlive();
  }

  /**
   * Finds the Python executable and returns its path. 
   * Checks both "python3" and "python" commands.
   */
  private String findSystemPythonExecutable() {
      try {
          ProcessBuilder processBuilder = new ProcessBuilder("python3", "--version");
          Process process = processBuilder.start();
          int exitCode = process.waitFor();
          if (exitCode == 0) return "python3";
      } catch (Exception ignored) {}

      try {
          ProcessBuilder processBuilder = new ProcessBuilder("python", "--version");
          Process process = processBuilder.start();
          int exitCode = process.waitFor();
          if (exitCode == 0) return "python";
      } catch (Exception ignored) {}

      return null; // Python not found
  }
  
  
  private String venvPythonPath() {
    String venv = getDataDir() + fs + "venv";
    String venvPython = Platform.getLocalInstance().isWindows()
        ? venv + fs + "Scripts" + fs + "python.exe"
        : venv + fs + "bin" + fs + "python";
    File check = new File(venvPython);
    if (!check.exists()) {
      return null;
    }
    return venvPython;
  }

  /**
   * @return true when each module imports in the venv, with no pip and no network
   */
  public boolean venvCanImport(String... modules) {
    return venvImportError(modules) == null;
  }

  /**
   * @return null when every module imports, otherwise the interpreter error
   *         (or a timeout). Does not run pip.
   */
  public String venvImportError(String... modules) {
    if (modules == null || modules.length == 0) {
      return null;
    }
    String python = venvPythonPath();
    if (python == null) {
      return "Py4j venv Python was not found under " + getDataDir() + fs + "venv";
    }
    return moduleImportError(python, modules, 45_000);
  }

  /**
   * Create {@code data/Py4j/venv} when it is missing and make sure pip and
   * py4j import. On Debian and Raspberry Pi OS, {@code python3 -m venv} fails
   * until the separate {@code python3-venv} package is installed; this tries
   * that install once with {@code sudo -n} so a password prompt cannot hang.
   */
  public String ensureVenv() throws IOException {
    String systemPython = findSystemPythonExecutable();
    if (systemPython == null) {
      throw new IOException(
          "Python is not on PATH (tried python3 and python). On Raspberry Pi OS: sudo apt install python3 python3-venv python3-pip");
    }
    if (venvPythonPath() == null) {
      createVenv(systemPython);
    }
    String venvPython = venvPythonPath();
    if (venvPython == null) {
      throw new IOException("Py4j venv Python was not found under " + getDataDir() + fs + "venv");
    }
    CommandResult version = runCommand(List.of(venvPython, "--version"), null, 20_000);
    if (version.exit != 0) {
      throw new IOException("Py4j venv Python failed to start (" + venvPython
          + "). Delete data/Py4j/venv and start again. " + tailLines(version.output, 20));
    }
    info("Py4j venv %s", oneLine(version.output));
    if (!pipWorks(venvPython)) {
      CommandResult ensured = runCommand(List.of(venvPython, "-m", "ensurepip", "--upgrade"), pipEnv(), 180_000);
      if (ensured.exit != 0 || !pipWorks(venvPython)) {
        throw new IOException("pip is missing in the Py4j venv. " + tailLines(ensured.output, 30)
            + " On Raspberry Pi OS / Debian: sudo apt install python3-venv python3-pip");
      }
    }
    String py4jError = moduleImportError(venvPython, new String[] { "py4j" }, 30_000);
    if (py4jError != null) {
      CommandResult pip = runCommand(List.of(venvPython, "-m", "pip", "install", "py4j"), pipEnv(), PIP_TIMEOUT_MS);
      if (pip.exit != 0) {
        throw new IOException("Failed to install py4j in the virtual environment. " + tailLines(pip.output, 40));
      }
    }
    return venvPython;
  }

  /**
   * Best-effort pip upgrade. DepthAI's wheel tags need a current pip; a failed
   * upgrade does not abort the depthai install.
   */
  public void upgradePip() {
    try {
      String python = ensureVenv();
      info("Upgrading pip in the Py4j venv");
      CommandResult upgraded = runCommand(List.of(python, "-m", "pip", "install", "--upgrade", "pip"), pipEnv(),
          PIP_TIMEOUT_MS);
      if (upgraded.exit != 0) {
        warn("pip upgrade failed; continuing with the current pip. %s", tailLines(upgraded.output, 20));
      }
    } catch (IOException e) {
      warn("pip upgrade failed; continuing. %s", e.getMessage());
    }
  }

  /**
   * {@code platform.machine()} of the venv interpreter, for example
   * {@code aarch64} or {@code armv7l}.
   */
  public String pythonMachine() {
    String python = venvPythonPath();
    if (python == null) {
      python = findSystemPythonExecutable();
    }
    if (python == null) {
      return null;
    }
    try {
      CommandResult result = runCommand(List.of(python, "-c", "import platform; print(platform.machine())"), null,
          20_000);
      if (result.exit != 0) {
        return null;
      }
      String[] lines = result.output.split("\\R");
      for (int i = lines.length - 1; i >= 0; i--) {
        if (!lines[i].trim().isEmpty()) {
          return lines[i].trim();
        }
      }
      return null;
    } catch (IOException e) {
      log.info("python machine probe failed: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Non-interactive apt install. Returns false when this is not Linux, sudo
   * needs a password, or apt fails. Does not prompt.
   */
  public boolean tryAptInstall(String... packages) {
    if (packages == null || packages.length == 0 || !Platform.getLocalInstance().isLinux()) {
      return false;
    }
    List<String> install = aptCommand("install", "-y");
    Collections.addAll(install, packages);
    try {
      CommandResult result = runCommand(install, aptEnv(), 8L * 60L * 1000L);
      if (result.exit == 0) {
        return true;
      }
      String out = result.output == null ? "" : result.output.toLowerCase();
      if (out.contains("unable to locate") || out.contains("failed to fetch") || out.contains("404")) {
        CommandResult update = runCommand(aptCommand("update"), aptEnv(), 5L * 60L * 1000L);
        if (update.exit != 0) {
          warn("apt-get update failed. %s", tailLines(update.output, 15));
          return false;
        }
        result = runCommand(install, aptEnv(), 8L * 60L * 1000L);
        return result.exit == 0;
      }
      warn("apt install failed (%s). %s", String.join(" ", packages), tailLines(result.output, 20));
      return false;
    } catch (IOException e) {
      warn("apt install failed: %s", e.getMessage());
      return false;
    }
  }

  /**
   * Debian/Ubuntu venv failures that are fixed by the {@code python3-venv}
   * package. Raspberry Pi OS Trixie does not always install that package.
   */
  public static boolean venvNeedsDebianPackage(String output) {
    if (output == null) {
      return false;
    }
    String text = output.toLowerCase();
    return text.contains("ensurepip") || text.contains("python3-venv") || text.contains("no module named venv");
  }




  /**
   * Install a list of packages into the environment Py4j is running in. Py4j
   * does not need to be running/connected to call this method as it spawns a
   * new subprocess to invoke Pip. Output from pip is echoed via
   * {@link #handleStdOut(String)}.
   * 
   * @param packages
   *                 The list of packages to install. Must be findable by Pip
   * @throws IOException
   *                     If an I/O error occurs running Pip.
   */
  public void installPipPackages(List<String> packages) throws IOException {
    installPipPackages(packages, Collections.emptyList());
  }

  /**
   * Install packages with extra pip flags (inserted after {@code pip install}).
   * Example: {@code --only-binary=depthai} to refuse source builds that fail on
   * newer CPython.
   *
   * @param packages
   *          requirement specifiers
   * @param pipOptions
   *          extra pip arguments, or empty
   */
  public void installPipPackages(List<String> packages, List<String> pipOptions) throws IOException {
    String python = venvPythonPath();
    if (python == null) {
      python = ensureVenv();
    }
    List<String> commandArgs = new ArrayList<>();
    commandArgs.add(python);
    commandArgs.add("-m");
    commandArgs.add("pip");
    commandArgs.add("install");
    if (pipOptions != null) {
      commandArgs.addAll(pipOptions);
    }
    if (packages != null) {
      commandArgs.addAll(packages);
    }
    CommandResult result = runCommand(commandArgs, pipEnv(), PIP_TIMEOUT_MS);
    if (result.exit != 0) {
      String msg = "Could not install packages (pip exit " + result.exit + "). " + tailLines(result.output, 40);
      error(msg);
      throw new IOException(msg);
    }
  }

  private void createVenv(String systemPython) throws IOException {
    String venv = getDataDir() + fs + "venv";
    new File(getDataDir()).mkdirs();
    info("Creating Py4j venv with %s at %s", systemPython, venv);
    CommandResult created = runCommand(List.of(systemPython, "-m", "venv", venv), null, 180_000);
    if (created.exit != 0 && venvNeedsDebianPackage(created.output)
        && tryAptInstall("python3-venv", "python3-pip")) {
      info("Retrying venv creation after installing python3-venv");
      created = runCommand(List.of(systemPython, "-m", "venv", venv), null, 180_000);
    }
    if (created.exit != 0 || venvPythonPath() == null) {
      String hint = venvNeedsDebianPackage(created.output)
          ? " On Raspberry Pi OS / Debian: sudo apt install python3-venv python3-pip"
          : "";
      throw new IOException(
          "Failed to create virtual environment at " + venv + ". " + tailLines(created.output, 40) + hint);
    }
    CommandResult pip = runCommand(List.of(venvPythonPath(), "-m", "pip", "install", "py4j"), pipEnv(), PIP_TIMEOUT_MS);
    if (pip.exit != 0) {
      throw new IOException("Failed to install py4j in the virtual environment. " + tailLines(pip.output, 40));
    }
  }

  private boolean pipWorks(String python) {
    try {
      CommandResult result = runCommand(List.of(python, "-m", "pip", "--version"), null, 30_000);
      return result.exit == 0;
    } catch (IOException e) {
      return false;
    }
  }

  private String moduleImportError(String python, String[] modules, long timeoutMs) {
    StringBuilder code = new StringBuilder();
    for (String module : modules) {
      if (module == null || !module.matches("[A-Za-z_][A-Za-z0-9_]*")) {
        return "refusing to import invalid module name " + module;
      }
      code.append("import ").append(module).append('\n');
    }
    try {
      // A reader thread plus waitFor, so a hung native import (depthai probing
      // USB) cannot block forever on a full stdout pipe.
      CommandResult result = runCommand(List.of(python, "-c", code.toString()), null, timeoutMs);
      if (result.exit == 0) {
        return null;
      }
      return tailLines(result.output, 30);
    } catch (IOException e) {
      return e.getMessage();
    }
  }

  /**
   * Raspberry Pi OS Trixie mounts {@code /tmp} as a small tmpfs. Pip unpacks
   * the depthai wheel there and fails with "No space left on device".
   */
  private Map<String, String> pipEnv() {
    File tmp = new File(getDataDir() + fs + "pip-tmp");
    tmp.mkdirs();
    Map<String, String> env = new HashMap<>();
    String path = tmp.getAbsolutePath();
    env.put("TMPDIR", path);
    env.put("TEMP", path);
    env.put("TMP", path);
    env.put("PIP_DISABLE_PIP_VERSION_CHECK", "1");
    env.put("PIP_DEFAULT_TIMEOUT", "60");
    return env;
  }

  private List<String> aptCommand(String... args) {
    List<String> command = new ArrayList<>();
    if (!"root".equals(System.getProperty("user.name"))) {
      command.add("sudo");
      command.add("-n");
    }
    command.add("apt-get");
    if (args != null) {
      Collections.addAll(command, args);
    }
    return command;
  }

  private static Map<String, String> aptEnv() {
    Map<String, String> env = new HashMap<>();
    env.put("DEBIAN_FRONTEND", "noninteractive");
    return env;
  }

  private CommandResult runCommand(List<String> command, Map<String, String> extraEnv, long timeoutMs) throws IOException {
    info("run: %s", String.join(" ", command));
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);
    if (extraEnv != null) {
      pb.environment().putAll(extraEnv);
    }
    Process proc = pb.start();
    StringBuilder output = new StringBuilder();
    Thread reader = new Thread(() -> {
      try (BufferedReader stdOutput = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
        String line;
        while ((line = stdOutput.readLine()) != null) {
          synchronized (output) {
            output.append(line).append('\n');
          }
          handleStdOut(line);
        }
      } catch (IOException e) {
        log.info("command output closed: {}", e.getMessage());
      }
    }, getName() + "-cmd");
    reader.setDaemon(true);
    reader.start();
    boolean finished;
    try {
      finished = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      proc.destroyForcibly();
      joinReader(reader);
      throw new IOException("command interrupted: " + command, e);
    }
    if (!finished) {
      proc.destroyForcibly();
      joinReader(reader);
      throw new IOException(
          "command timed out after " + timeoutMs + "ms: " + command + "\n" + tailLines(copyOutput(output), 30));
    }
    joinReader(reader);
    return new CommandResult(proc.exitValue(), copyOutput(output));
  }

  private static String copyOutput(StringBuilder output) {
    synchronized (output) {
      return output.toString();
    }
  }

  private static String oneLine(String text) {
    if (text == null) {
      return "";
    }
    return text.replace('\n', ' ').replace('\r', ' ').trim();
  }

  private void joinReader(Thread reader) {
    try {
      reader.join(8000);
      if (reader.isAlive()) {
        reader.interrupt();
        reader.join(2000);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  static String tailLines(String text, int maxLines) {
    if (text == null || text.isEmpty()) {
      return "(no pip output captured)";
    }
    String[] lines = text.split("\\R");
    int from = Math.max(0, lines.length - Math.max(1, maxLines));
    StringBuilder sb = new StringBuilder();
    for (int i = from; i < lines.length; i++) {
      if (sb.length() > 0) {
        sb.append('\n');
      }
      sb.append(lines[i]);
    }
    return sb.toString();
  }

  @Override
  public void startService() {
    super.startService();
    if (config.scriptRootDir == null) {
      config.scriptRootDir = new File(getDataInstanceDir()).getAbsolutePath();
    }
    File dataDir = new File(config.scriptRootDir);
    dataDir.mkdirs();
    // start the py4j socket server
    start();
    sleep(300);
    // start the python process which starts the Py4j.py MessageHandler
    if (config.autostartPython) {
      startPythonProcess();
    }
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
      pythonProcess.gobbler.interrupt();
      pythonProcess = null;
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
      Runtime.start("py4j", "Py4j");
      // Runtime.start("python", "Python");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public void connect(String uri) throws Exception {
    // host:port of python process running py4j ???

  }

  /**
   * Remote in this context is the remote python process
   */
  @Override
  public void sendRemote(Message msg) throws Exception {
    log.info("sendRemote");
    String jsonMsg = CodecUtils.toJson(msg);
    log.info(String.format("sendRemote %s"));
    handler.send(jsonMsg);
  }

  @Override
  public boolean isLocal(Message msg) {
    return Runtime.getInstance().isLocal(msg);
  }

  @Override
  public List<String> getClientIds() {
    return Runtime.getInstance().getConnectionUuids(getName());
  }

  @Override
  public Map<String, Connection> getClients() {
    return Runtime.getInstance().getConnections(getName());
  }

  public void clear() {
    logs = new SlidingWindowList<>(300);
  }

}
