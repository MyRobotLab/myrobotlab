package org.myrobotlab.service;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.process.Terminal;
import org.myrobotlab.process.Terminal.TerminalCmd;
import org.myrobotlab.service.config.TerminalManagerConfig;
import org.slf4j.Logger;

public class TerminalManager extends Service<TerminalManagerConfig> {

  public class TerminalLogEntry {
    public String msg = null;
    public String src = null;
    // FIXME - STDERR at some point
    public String stream = "stdout";
    public String terminal = null;
    public long ts = System.currentTimeMillis();
  }

  public static class TerminalStartupConfig {
    public String type = null; // Python Node Ros

  }

  public final static Logger log = LoggerFactory.getLogger(TerminalManager.class);

  private static final long serialVersionUID = 1L;

  /**
   * Thread safe map of all the terminals
   */
  protected Map<String, Terminal> terminals = new ConcurrentSkipListMap<>();

  public TerminalManager(String n, String id) {
    super(n, id);
  }

  public void deleteTerminal(String name) {
    log.info("deleting terminal {}", name);
    if (terminals.containsKey(name)) {
      terminals.remove(name);
    } else {
      info("%s terminal does not exist", name);
    }
  }

  /**
   * Process blocking command in default terminal
   * 
   * @param cmd
   * @return
   */
  public String processBlockingCommand(String cmd) {
    return processBlockingCommand("default", cmd);
  }
  
  /**
   * Publishes the current command from a terminal
   * @param cmd
   * @return
   */
  public TerminalCmd publishCmd(TerminalCmd cmd) {
    return cmd;
  }

  /**
   * Synchronously process a command in the terminal
   * 
   * @param name
   * @param cmd
   */
  public String processBlockingCommand(String name, String cmd) {
    if (!terminals.containsKey(name)) {
      error("could not find terminal %s to process command %s", name, cmd);
      return null;
    }
    Terminal terminal = terminals.get(name);
    return terminal.processBlockingCommand(cmd);
  }

  /**
   * Asynchronously process command in default terminal
   * 
   * @param cmd
   */
  public void processCommand(String cmd) {
    processCommand("default", cmd);
  }

  /**
   * Process a command against a named terminal
   * 
   * @param name
   * @param cmd
   */
  public void processCommand(String name, String cmd) {
    if (!terminals.containsKey(name)) {
      error("could not find terminal %s to process command %s", name, cmd);
      return;
    }
    Terminal terminal = terminals.get(name);
    terminal.processCommand(cmd);
  }

  /**
   * Structured log publishing
   * 
   * @param name
   * @param msg
   * @return
   */
  public TerminalLogEntry publishLog(String name, String msg) {
    TerminalLogEntry entry = new TerminalLogEntry();
    entry.src = getName();
    entry.terminal = name;
    entry.msg = msg;
    entry.stream = "stdout";
    return entry;
  }

  /**
   * All stdout/stderr from all terminals is published here
   * 
   * @param msg
   * @return
   */
  public String publishStdOut(String msg) {
    return msg;
  }

  /**
   * Save configuration of the terminal including if its currently running
   * 
   * @param name
   *          terminal name
   */
  public void saveTerminal(String name) {
    log.info("saving terminal {}", name);
    // TODO - get terminal startup info and
    // save it to config
  }

  public void startTerminal() {
    startTerminal("default");
  }

  /**
   * Start a generalized simple terminal
   * 
   * @param name
   *          terminal name
   */
  public void startTerminal(String name) {
    startTerminal(name, null, null);
  }

  public void startTerminal(String name, String workspace, String type) {
    log.info("starting terminal {} {}", name, type);

    Terminal terminal = null;
    String fullType = null;

    if (type == null) {
      type = "";
    }

    if (workspace == null) {
      workspace = ".";
    }

    if (!type.contains(".")) {
      fullType = "org.myrobotlab.process." + type + "Terminal";
    } else {
      fullType = type;
    }

    if (terminals.containsKey(name)) {
      terminal = terminals.get(name);
    } else {
      terminal = (Terminal) Instantiator.getNewInstance(fullType, this, name);
      terminals.put(name, terminal);
    }
    terminal.start(workspace);
  }

  /**
   * Terminates the terminal
   * 
   * @param name
   *          terminal name
   */
  public void terminateTerminal(String name) {
    log.info("terminating terminal {}", name);
    if (terminals.containsKey(name)) {
      try {
        Terminal terminal = terminals.get(name);
        terminal.terminate();
      } catch (Exception e) {
        error(e);
      }
    } else {
      info("%s terminal does not exist", name);
    }
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      TerminalManager manager = (TerminalManager) Runtime.start("manager", "TerminalManager");
      Runtime.start("webgui", "WebGui");
      manager.startTerminal();

//      for (int i = 0; i < 100; ++i) {
//        String ls = manager.processBlockingCommand("ls");
//        manager.processCommand("ls");
//      }
      
//      List<String> commands = Arrays.asList("echo Hello", "ls");
//      manager.processCommands(commands);


    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
