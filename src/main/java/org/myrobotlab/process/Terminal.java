package org.myrobotlab.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.TerminalManager;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class Terminal {

  public final static Logger log = LoggerFactory.getLogger(Terminal.class);

  public boolean isRunning = false;

  /**
   * executor service for managing streams
   */
  private transient ExecutorService executorService;

  /**
   * lock for synchonizing
   */
  protected transient Object lock = new Object();

  /**
   * name of this shell
   */
  protected String name;

  /**
   * output buffer
   */
  protected StringBuilder outputCapture = new StringBuilder();

  /**
   * The pid of the sub process
   */
  protected Long pid;

  /**
   * list of pids for this shell
   */
  protected Set<Long> pids = new HashSet<>();

  /**
   * process handler
   */
  private transient Process process;

  /**
   * reference to mrl service
   */
  protected transient TerminalManager service;

  /**
   * The initial command that started the shell
   */
  protected String shellCommand = null;

  /**
   * The directory where the interactive shell will do its work, where the
   * process will start
   */
  protected String workspace = ".";

  public Terminal(TerminalManager service, String name) {
    // can increase to handle more input
    this.executorService = Executors.newFixedThreadPool(3);
    this.service = service;
    this.name = name;
  }

  public void clearOutput() {
    outputCapture = new StringBuilder();
  }

  private String determineShellCommand() {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win")) {
      return "cmd";
    } else {
      return "/bin/sh"; // Works for Unix/Linux/Mac
    }
  }

  public boolean doesExecutableExist(String name) {
    return false;
  }

  /**
   * <pre>
   *  FIXME - finish !
    
   public void processAndWait(String command) throws IOException {
     String completionMarker = "Command completed -- unique marker " + System.currentTimeMillis();
     processCommand(command + "\n");
     processCommand("echo \"" + completionMarker + "\"\n");
     
     StringBuilder commandOutput = new StringBuilder();
     String line;
     while ((line = readLineWithTimeout()) != null) { // Implement readLineWithTimeout according to your input handling
         if (line.contains(completionMarker)) {
             break;
         }
         commandOutput.append(line).append("\n");
     }
     // Now commandOutput contains the output from the command, and you know the command finished.
  }
   * </pre>
   */

  public String getCapturedOutput() {
    synchronized (outputCapture) {
      return outputCapture.toString();
    }
  }

  public Set<Long> getPids() {
    Set<Long> scanPids = new HashSet<>();
    if (process.isAlive()) {
      process.descendants().forEach(processHandle -> {
        scanPids.add(processHandle.pid());
      });
    }
    pids = scanPids;
    return pids;
  }

  /**
   * cmd for executing a script
   * 
   * @param scriptPath
   * @return
   */
  public String getScriptCmd(String scriptPath) {
    if (isWindows()) {
      return ("cmd /c \"" + scriptPath + "\"\n");
    } else {
      return ("/bin/sh \"" + scriptPath + "\"\n");
    }
  }

  public String getTemplate(String templateName) {
    try {
      byte[] bytes = Files.readAllBytes(getTemplatePath(templateName));
      if (bytes != null) {
        return new String(bytes);
      }
    } catch (IOException e) {
      service.error(e);
    }
    return null;
  }

  // private void startStreamGobbler(InputStream inputStream, String streamName)
  // {
  // executorService.submit(() -> {
  // new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(line
  // -> {
  // System.out.println(line); // Print the line
  // synchronized (outputCapture) {
  // outputCapture.append(line).append("\n"); // Capture the line
  // }
  // });
  // });
  // }

  public Path getTemplatePath(String templateName) {
    Path scriptPath = Paths.get(service.getResourceDir() + File.separator + "templates" + File.separator, templateName + (isWindows() ? ".bat" : ".sh"));
    return scriptPath;
  }

  public String getVersion() {
    return "0.0.0";
  }

  public boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }

  public void processCommand(String input) {
    try {
      if (input == null) {
        input = "";
      }
      if (process == null) {
        service.error("cannot process a command when the terminal isn't started");
        return;
      }
      OutputStream outputStream = process.getOutputStream();
      outputStream.write(String.format("%s\n", input).getBytes());
      outputStream.flush();
    } catch (Exception e) {
      service.error(e);
    }
  }

  // New method to process a list of commands
  public void processCommands(List<String> commands) throws IOException {
    for (String command : commands) {
      processCommand(command + "\n");
    }
  }

  private void shutdownExecutor() {
    executorService.shutdownNow();
  }

  public void start() {
    start(workspace);
  }

  /**
   * Start an interactive shell in a workspace directory
   * 
   * @param workspace
   */
  public void start(String workspace) {
    if (!isRunning) {
      synchronized (lock) {
        try {
          shellCommand = determineShellCommand();
          ProcessBuilder processBuilder = new ProcessBuilder(shellCommand.split(" "));
          processBuilder.redirectErrorStream(true); // Merge stdout and stderr

          if (workspace != null && !workspace.isEmpty()) {
            this.workspace = workspace;
            processBuilder.directory(new File(workspace)); // Set the CWD for
                                                           // the
                                                           // process
          }

          process = processBuilder.start();
          pid = process.pid();
          isRunning = true;

          startStreamGobbler(process.getInputStream(), "OUTPUT");
          // FIXME option to attach to stdIn
          // should
          // startUserInputForwarder();
        } catch (Exception e) {
          isRunning = false;
          service.error(e);
        }
        service.broadcastState();
      }
    } else {
      log.info("{} already started", name);
    }
  }

  private void startStreamGobbler(InputStream inputStream, String streamName) {
    executorService.submit(() -> {
      try {
        byte[] buffer = new byte[1024]; // Adjust size as needed
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
          String text = new String(buffer, 0, length);
          // Synchronize writing to the outputCapture to ensure thread safety
          synchronized (outputCapture) {
            System.out.print(text); // Print the text as it comes without
                                    // waiting for a new line
            outputCapture.append(text); // Append the text to the output capture
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  private void startUserInputForwarder() {
    executorService.submit(() -> {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
        String inputLine;
        while ((inputLine = reader.readLine()) != null) {
          processCommand(inputLine);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  public void terminate() throws IOException {
    synchronized (lock) {
      // Optionally send a quit command to the shell if it supports graceful
      // exit.
      // Example for Unix/Linux/Mac: sendInput("exit\n");
      // For Windows, it might be different or not necessary.
      if (process != null) {
        process.descendants().forEach(processHandle -> {
          log.info("Terminating PID: " + processHandle.pid());
          processHandle.destroyForcibly(); // Attempts to terminate the process
        });
        // destroying parent
        process.destroyForcibly();
        process = null;
        shutdownExecutor(); // Shutdown the executor service
      }
      isRunning = false;
    }
    service.broadcastState();
  }

  public int waitForCompletion() throws InterruptedException {
    process.waitFor();
    shutdownExecutor();
    return process.exitValue();
  }

  public static void main(String[] args) {
    try {
      TerminalManager processor = (TerminalManager) Runtime.start("processor", "ManagedProcess");
      Terminal shell = new Terminal(processor, "basic tty");
      shell.start();
      // Example usage of the new method if you want to process a list of
      // commands
      List<String> commands = Arrays.asList("echo Hello", "ls");
      shell.processCommands(commands);
      int exitCode = shell.waitForCompletion();
      System.out.println("Shell exited with code: " + exitCode);
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

}
