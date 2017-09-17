package org.myrobotlab.service;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import io.nodyn.NoOpExitHandler;
import io.nodyn.Nodyn;
import io.nodyn.runtime.NodynConfig;
import io.nodyn.runtime.RuntimeFactory;

public class Node extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Node.class);

  public Node(String n) {
    super(n);
  }

  private static final String SCRIPT = "" + "var main = require('./project/main.js');" + "main.run();";
  transient final ScriptEngineManager manager = new ScriptEngineManager();

  public void runMain(String... args) throws InterruptedException {

    for (final ScriptEngineFactory scriptEngine : manager.getEngineFactories()) {
      System.out.println(scriptEngine.getEngineName() + " (" + scriptEngine.getEngineVersion() + ")");
      System.out.println("\tLanguage: " + scriptEngine.getLanguageName() + "(" + scriptEngine.getLanguageVersion() + ")");
      System.out.println("\tCommon Names/Aliases: ");
      for (final String engineAlias : scriptEngine.getNames()) {
        System.out.println(engineAlias + " ");
      }
    }

    // Use DynJS runtime
    RuntimeFactory factory = RuntimeFactory.init(Node.class.getClassLoader(), RuntimeFactory.RuntimeType.DYNJS);

    // Set config to run main.js
    NodynConfig config = new NodynConfig(new String[] { "-e", SCRIPT });

    // Create a new Nodyn and run it
    Nodyn nodyn = factory.newRuntime(config);
    nodyn.setExitHandler(new NoOpExitHandler());

    try {
      int exitCode = nodyn.run();
      if (exitCode != 0) {
        error("exitCode != 0");
      }
    } catch (Throwable t) {
      Logging.logError(t);
    }
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Node.class.getCanonicalName());
    meta.addDescription("embedded node js");
    // add dependency if necessary
    meta.setAvailable(false); // not ready for prime-time
    meta.addDependency("org.node", "0.1.1");
    meta.addCategory("programming");
    return meta;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Node node = (Node) Runtime.start("node", "Node");
      // Runtime.start("gui", "SwingGui");
      node.runMain((String[])null);

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
