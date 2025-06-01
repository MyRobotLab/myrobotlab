package org.myrobotlab.logging;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * Initially we wanted some abstraction between slf4j and mrl library (to
 * support Android) Now its no longer needed.
 * 
 * @author GroG
 *
 */
public class LoggerFactory {

  public static Logger getLogger(Class<?> clazz) {
    return getLogger(clazz.toString());
  }

  public static Logger getLogger(String name) {
    return org.slf4j.LoggerFactory.getLogger(name);
  }

  public static ILoggerFactory getILoggerFactory() {
    return org.slf4j.LoggerFactory.getILoggerFactory();
  }

}
