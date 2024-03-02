package org.myrobotlab.service;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.process.Terminal;
import org.myrobotlab.service.Log.LogEntry;
import org.myrobotlab.service.config.TerminalManagerConfig;
import org.slf4j.Logger;

public class TerminalManager extends Service<TerminalManagerConfig> {

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

  /**
   * Process a command against a named terminal
   * 
   * @param name
   * @param command
   */
  public void processCommand(String name, String command) {
    if (!terminals.containsKey(name)) {
      error("could not find terminal %s to process command %s", name, command);
      return;
    }
    Terminal terminal = terminals.get(name);
    terminal.processCommand(command);
  }

  /**
   * Start a generalized simple terminal
   * 
   * @param name
   *          terminal name
   */
  public void startTerminal(String name) {
    startTerminal(name, null);
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

  public void deleteTerminal(String name) {
    log.info("deleting terminal {}", name);
    if (terminals.containsKey(name)) {
      terminals.remove(name);
    } else {
      info("%s terminal does not exist", name);
    }
  }

  public LogEntry publishStdOut(String name, String msg) {
    LogEntry entry = new LogEntry();
    entry.src = name;
    entry.level = "INFO";
    entry.className = this.getClass().getCanonicalName();
    entry.body = msg;
    return entry;
  }

  public void startTerminal(String name, String type) {
    log.info("starting terminal {} {}", name, type);

    Terminal terminal = null;
    String fullType = null;

    if (type == null) {
      type = "";
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
    terminal.start();
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      TerminalManager manager = (TerminalManager) Runtime.start("manager", "TerminalManager");
      Runtime.start("webgui", "WebGui");
      manager.startTerminal("basic");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
