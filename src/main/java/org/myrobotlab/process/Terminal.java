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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.generics.SlidingWindowList;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.TerminalManager;
import org.slf4j.Logger;

public class Terminal {

  public final static Logger log = LoggerFactory.getLogger(Terminal.class);

  public boolean isRunning = false;

  protected final String BOUNDARY_MARKER = "----------------terminal-cmd-boundary-7MA4YWxkTrZu0gW----------------";

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
  // protected List<String> output  = new SlidingWindowList<>(300);
  protected StringBuilder output = new StringBuilder();  

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
   * For synchronous output
   */
  private transient BlockingQueue<String> blockingOutputQueue = new LinkedBlockingQueue<>();

  /**
   * The directory where the interactive shell will do its work, where the
   * process will start
   */
  protected String workspace = ".";

  /**
   * last command processed
   */
  protected String lastCmd = null;
  
  
  public static class TerminalCmd {
    public long ts = System.currentTimeMillis();
    public String src;
    public String terminal;
    public String cmd;
  }
  

  public Terminal(TerminalManager service, String name) {
    // can increase to handle more input
    this.executorService = Executors.newFixedThreadPool(3);
    this.service = service;
    this.name = name;
  }

  public void clearOutput() {
    // output = new SlidingWindowList<>(300);
    output = new StringBuilder();
  }

  private String determineShellCommand() {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win")) {
      return "cmd";
    } else {
      // return "/bin/sh"; // Works for Unix/Linux/Mac
      String bashPath = "/bin/bash";
      File bashFile = new File(bashPath);
      if (bashFile.exists()) {
          return bashPath;
      } else {
          // Fallback to sh if Bash is not found (less ideal)
          return "/bin/sh";
      }
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
    processCommand(input, false);
  }

  public void processCommand(String input, boolean addBoundary) {
    synchronized (lock) {
      try {
        if (input == null) {
          input = "";
        }
        if (process == null) {
          service.error("cannot process a command when the terminal isn't started");
          return;
        }
        String cmd = null;
        if (addBoundary) {
          // windows/mac echo vs linux
          cmd = String.format("%s\necho %s\n", input, BOUNDARY_MARKER);
        } else {
          cmd = String.format("%s\n", input);
        }
        lastCmd = cmd;
        TerminalCmd terminalCmd = new TerminalCmd();
        terminalCmd.src = service.getName();
        terminalCmd.terminal = name;
        terminalCmd.cmd = cmd;
        service.invoke("publishCmd", terminalCmd);
        OutputStream outputStream = process.getOutputStream();
        outputStream.write(cmd.getBytes());
        outputStream.flush();
      } catch (Exception e) {
        service.error(e);
      }
    }
  }

  // FIXME - should be synchronized with
  public String processBlockingCommand(String input) {
    synchronized (lock) {
      blockingOutputQueue.clear();
      processCommand(input, true);
      String ret = null;
      try {
        while (isRunning && ret == null) {
          ret = blockingOutputQueue.poll(100, TimeUnit.MILLISECONDS);
        }
      } catch (InterruptedException e) {
        service.error(e);
      }
      return ret;
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
        byte[] buffer = new byte[8192]; // Adjust size as needed
        int length;
        StringBuilder dynamicBuffer = new StringBuilder();
        while ((length = inputStream.read(buffer)) != -1) {
          String text = new String(buffer, 0, length);
          // asynchronous publishing of all stdout
          service.invoke("publishLog", name, text);
          service.invoke("publishStdOut", text);
          output.append(text);
          dynamicBuffer.append(text);
          System.out.print(text);
          if (dynamicBuffer.toString().contains(BOUNDARY_MARKER)) {
            // Boundary marker found, handle command completion here
            System.out.println("Command execution completed.");
            // Remove the boundary marker from the output buffer
            int index = dynamicBuffer.indexOf(BOUNDARY_MARKER);
            dynamicBuffer.delete(index, index + BOUNDARY_MARKER.length());
            blockingOutputQueue.add(dynamicBuffer.toString());
            dynamicBuffer = new StringBuilder();
          }
        }
      } catch (IOException e) {
        service.error(e);
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

}
