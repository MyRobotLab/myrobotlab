package org.myrobotlab.service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.JavaScriptConfig;
import org.slf4j.Logger;

public class JavaScript extends Service<JavaScriptConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(JavaScript.class);

  // TODO -
  // https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Embedding_tutorial

  transient ScriptEngine engine;

  transient ScriptEngineManager manager = new ScriptEngineManager();

  @Override
  public void startService() {
    for (final ScriptEngineFactory scriptEngine : manager.getEngineFactories()) {
      System.out.println(scriptEngine.getEngineName() + " (" + scriptEngine.getEngineVersion() + ")");
      System.out.println("\tLanguage: " + scriptEngine.getLanguageName() + "(" + scriptEngine.getLanguageVersion() + ")");
      System.out.println("\tCommon Names/Aliases: ");
      for (final String engineAlias : scriptEngine.getNames()) {
        System.out.println(engineAlias + " ");
      }
    }

    engine = manager.getEngineByName("js");
  }

  public void exec(String script) {

    try {
      // engine.put("inputNumber", numberToWriteInExponentialForm);
      // engine.put("decimalPlaces", numberDecimalPlaces);
      // engine.eval("var outputNumber =
      // inputNumber.toExponential(decimalPlaces);");
      // final String exponentialNumber = (String)
      // engine.get("outputNumber");
      // System.out.println("Number: " + exponentialNumber);
      engine.eval(script);
    } catch (Exception e) {
      error(e);
    }
  }

  public void put(String varName, Object obj) {
    engine.put(varName, obj);
  }

  public JavaScript(String n, String id) {
    super(n, id);
  }

  public Object get(String varName) {
    return engine.get(varName);
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      JavaScript javascript = (JavaScript) Runtime.start("javascript", "JavaScript");
      javascript.exec("java.lang.System.out.println(\"hello world\");");
      javascript.exec("var x = 3; ++x; java.lang.System.out.println(x);");
      // Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
