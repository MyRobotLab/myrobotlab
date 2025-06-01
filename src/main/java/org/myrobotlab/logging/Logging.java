package org.myrobotlab.logging;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;

public abstract class Logging {

  public final static Logger log = LoggerFactory.getLogger(Logging.class);

  // performance timing
  public static long startTimeMilliseconds = 0;

  public static boolean performanceTiming = false;

  public final static String logError(final Throwable e) {
    String ret = stackToString(e);
    log.error(ret);
    return ret;
  }

  static public void logTime(String tag) {
    if (!performanceTiming) {
      return;
    }
    if ("start".equals(tag)) {
      startTimeMilliseconds = System.currentTimeMillis();
    }

    log.info("performance clock :{} ms {}", System.currentTimeMillis() - startTimeMilliseconds, tag);

  }

  static public Boolean logTimeEnable(Boolean b) {
    performanceTiming = b;
    return performanceTiming;
  }

  static public void logTimeStart() {
    logTime("start");
  }

  public final static String stackToString(final Throwable e) {
    StringWriter sw;
    try {
      sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
    } catch (Exception e2) {
      return "bad stackToString";
    }
    return sw.toString();
  }

  public abstract void addAppender(Object type);

  public abstract void addAppender(String type);

  public abstract void addAppender(String type, String filename);

  public abstract void configure(); // a basic configuration

  public abstract String getLevel();

  public abstract void removeAllAppenders();

  public abstract void removeAppender(Object console);

  public abstract void removeAppender(String name);

  public abstract void setLevel(String level);

  public abstract void setLevel(String clazz, String level);

}
