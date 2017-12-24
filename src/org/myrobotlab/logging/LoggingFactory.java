package org.myrobotlab.logging;

import org.myrobotlab.framework.Instantiator;

public class LoggingFactory {

  static String logFileName = "myrobotlab.log";

  public static Logging getInstance() {
    try {
      // Logging logging = (Logging)
      // Service.getNewInstance("org.myrobotlab.logging.LoggingLog4J");
      Logging logging = (Logging) Instantiator.getNewInstance("org.myrobotlab.logging.LoggingSLF4J");
      return logging;
    } catch (Exception e) {
      System.out.println(e.getMessage());
      Logging.logError(e); //
      e.printStackTrace();
    }

    return null;
  }

  public static void init() {
    init(null);
  }

  public static void init(String level) {
    Logging logging = getInstance();
    logging.configure();
    if (level != null) {
      logging.setLevel(level);
    }
  }
  
  public static void setLevel(String level){
    Logging logging = getInstance();
    logging.configure();
    if (level != null) {
      logging.setLevel(level);
    }
  }

  /**
   * at the moment "myrobotlab.log" - although it could be based on runtime name
   * - if its different from default and multiple processes are running from the
   * same directory or "myrobotlab.{ts}.log"
   * @return the log filename
   */
  public static String getLogFileName() {
    return logFileName;
  }
  
  public static void setLogFile(String filename){
    logFileName = filename;
  }

}
