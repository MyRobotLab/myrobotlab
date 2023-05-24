package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Invoker;
import org.myrobotlab.framework.interfaces.NameProvider;

/**
 * 
 * @author GroG
 *
 *         A LogPublisher can publish its own logging messages
 * 
 */
public interface LogPublisher extends NameProvider, Invoker {

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
  String publishLog(String msg);
}
