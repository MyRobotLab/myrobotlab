package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

/**
 * 
 * @author GroG
 *
 *         A LogPublisher can publish its own logging messages
 * 
 */
public interface LogPublisher extends NameProvider {

  /**
   * A String is currently used as the log entry - but it could be an object in
   * its constituent parts instead e.g. timestamp, formatted message, level etc.
   * - if this is desired make a public LogEntry publishLogEntry interface
   * method
   * 
   * @param msg
   *          msg to publish
   * @return string
   */
  public String publishLog(String msg);

  /**
   * a way to publish the log messages and log entries
   * 
   * @param method
   *          method
   * @param params
   *          params
   * @return returned object
   * 
   */
  public Object invoke(String method, Object... params);
}
