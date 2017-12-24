package org.myrobotlab.framework;

import static org.myrobotlab.framework.StatusLevel.DEBUG;
import static org.myrobotlab.framework.StatusLevel.ERROR;
import static org.myrobotlab.framework.StatusLevel.INFO;
import static org.myrobotlab.framework.StatusLevel.SUCCESS;
import static org.myrobotlab.framework.StatusLevel.WARN;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * Goal is to have a very simple Pojo with only a few (native Java helper
 * methods) WARNING !!! - this class used to extend Exception or Throwable - but
 * the gson serializer would stack overflow with self reference issue
 * 
 * TODO - allow radix tree searches for "keys" ???
 * 
 */
public class Status implements Serializable {// extends Exception {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Status.class);
  public String name; // service name ???

  public String level;
  public String key;
  public String detail;

  // --- static creation of typed Status objects ----
  public static Status debug(String format, Object... args) {
    Status status = new Status(String.format(format, args));
    status.level = DEBUG;
    return status;
  }

  public static Status error(Exception e) {
    Status s = new Status(e);
    s.level = ERROR;
    return s;
  }

  public static Status error(String msg) {
    Status s = new Status(msg);
    s.level = ERROR;
    return s;
  }

  public static Status error(String format, Object... args) {
    Status status = new Status(String.format(format, args));
    status.level = ERROR;
    return status;
  }

  public static Status warn(String msg) {
    Status s = new Status(msg);
    s.level = ERROR;
    return s;
  }

  public static Status warn(String format, Object... args) {
    Status status = new Status(String.format(format, args));
    status.level = WARN;
    return status;
  }

  public static Status info(String msg) {
    Status s = new Status(msg);
    s.level = INFO;
    return s;
  }

  public static Status info(String format, Object... args) {
    String formattedInfo = String.format(format, args);
    Status status = new Status(formattedInfo);
    status.level = INFO;
    return status;
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
    return "------\r\n" + sw.toString() + "------\r\n";
  }

  public Status(Exception e) {
    this.level = ERROR;
    StringWriter sw;
    try {
      sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      detail = sw.toString();
    } catch (Exception e2) {
    }
    this.key = String.format("%s - %s", e.getClass().getSimpleName(), e.getMessage());
  }

  public Status(Status s) {
    if (s == null) {
      return;
    }
    this.name = s.name;
    this.level = s.level;
    this.key = s.key;
    this.detail = s.detail;
  }

  /**
   * for minimal amount of information error is assumed, and info is detail of
   * an ERROR
   * @param detail d
   */
  public Status(String detail) {
    this.level = ERROR;
    this.detail = detail;
  }

  public Status(String name, String level, String key, String detail) {
    this.name = name;
    this.level = level;
    this.key = key;
    this.detail = detail;
  }

  public boolean isDebug() {
    return DEBUG.equals(level);
  }

  public boolean isError() {
    return ERROR.equals(level);
  }

  public boolean isInfo() {
    return INFO.equals(level);
  }

  public boolean isWarn() {
    return WARN.equals(level);
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    if (name != null) {
      sb.append(name);
      sb.append(" ");
    }
    if (level != null) {
      sb.append(level);
      sb.append(" ");
    }
    if (key != null) {
      sb.append(key);
      sb.append(" ");
    }
    if (detail != null) {
      sb.append(detail);
    }

    return sb.toString();
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    LoggingFactory.init(Level.INFO);

    Status test = new Status("i am pessimistic");
    // Status subTest = new Status("i am sub pessimistic");

    // test.add(subTest);

    String json = CodecUtils.toJson(test);
    Status z = CodecUtils.fromJson(json, Status.class);
    log.info(json);
    log.info(z.toString());
  }

  public static Status success() {
    Status s = new Status(SUCCESS);
    s.level = SUCCESS;
    return s;
  }

  public boolean isSuccess() {
    return SUCCESS.equals(level);
  }

  public static Status success(String detail) {
    Status s = new Status(SUCCESS);
    s.level = SUCCESS;
    s.detail = detail;
    return s;
  }

}
