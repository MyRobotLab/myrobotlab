package org.myrobotlab.process;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.TerminalManager;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class PythonTerminal extends Terminal {

  /**
   * name of the venv
   */
  protected String venvName = "venv";

  public final static Logger log = LoggerFactory.getLogger(PythonTerminal.class);

  public PythonTerminal(TerminalManager service, String name) throws IOException {
    super(service, name);
  }

  @Override
  public String getVersion() {
    try {
      return processBlockingCommand(getScriptCmd("python --version"));
    } catch (Exception e) {
      service.error(e);
    }
    return null;
  }

  public void installPipPackages(List<String> packages) {
    String packagesString = String.join(" ", packages);
    String command = "pip install " + packagesString;
    processCommand(command + "\n");
  }

  public void installPipPackage(String string) {
    // TODO Auto-generated method stub

  }

  public void activateVirtualEnv() {
    if (isWindows()) {
      processCommand(venvName + "\\Scripts\\activate");
    } else {
      // source is "bash"
      // processCommand("source " + venvName + "/bin/activate");
      // the posix way
      processCommand(". " + venvName + "/bin/activate");
    }
    Service.sleep(300);
  }

  public void installVirtualEnv() {
    installVirtualEnv(venvName);
  }

  public void installVirtualEnv(String venvName) {
    this.venvName = venvName;
    // processCommand(getScriptCmd("python -m venv " + venvName));
    processCommand("python -m venv " + venvName);
    Service.sleep(300);
  }

  public static void main(String[] args) {
    try {
      TerminalManager processor = (TerminalManager) Runtime.start("processor", "ManagedProcess");
      PythonTerminal shell = new PythonTerminal(processor, "python");
      // shell.setWorkspace(".." + File.separator + "webcam");
      shell.start(".." + File.separator + "webcam");
      shell.installVirtualEnv();
      shell.activateVirtualEnv();
      // shell.installPipPackage("");
      shell.installPipPackages(Arrays.asList("aiortc aiohttp"));

      shell.processCommand("python webcam.py");
      System.out.println(shell.getPids().toString());

      shell.terminate();

      // Example usage
      String directory = "../webcam";
      String venvName = "venv";
      String packageName = "package_name";
      String pythonScript = "your_script.py";

      // shell.setupAndRunPythonEnvironment(directory, venvName, packageName,
      // pythonScript);

      // Wait for the completion or handle accordingly
      // shell.waitForCompletion();

      // Terminate the shell if necessary
      // shell.terminate();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
