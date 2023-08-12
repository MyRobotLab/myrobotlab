/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;

public class Log extends Service<ServiceConfig> implements Appender<ILoggingEvent> {

  public static class LogEntry {
    public long ts;
    public String level;
    public String threadName;
    public String className;
    public String body;

    public LogEntry(ILoggingEvent event) {
      ts = event.getTimeStamp();
      level = event.getLevel().toString();
      threadName = event.getThreadName();
      className = event.getLoggerName();
      body = event.getFormattedMessage();
    }

    @Override
    public String toString() {
      return String.format("%d %s %s %s %s", ts, level, threadName, className, body);
    }
  }

  public final static Logger log = LoggerFactory.getLogger(Log.class);

  private static final long serialVersionUID = 1L;

  /**
   * log file name
   */
  public static String MYROBOTLAB_LOG = "myrobotlab.log";

  /**
   * buffer of log event - made transient because the appropriate way to
   * broadcast logging is through publishLogEvent (not broadcastState)
   */
  transient List<LogEntry> buffer = new ArrayList<>();

  /**
   * logging state
   */
  boolean isLogging = false;

  /**
   * last time events were broadcast
   */
  long lastPublishLogTimeTs = 0;

  /**
   * current log level
   */
  String logLevel = null;

  /**
   * max size of log buffer
   */
  int maxSize = 1000;

  /**
   * minimal time between log broadcasts
   */
  long minIntervalMs = 1000;

  public Log(String n, String id) {
    super(n, id);
    getLogLevel();
  }

  public String getLogLevel() {
    Logging logging = LoggingFactory.getInstance();
    logLevel = logging.getLevel();
    return logLevel;
  }

  @Override
  public void addError(String msg) {
    System.out.println("addError");
  }

  @Override
  public void addError(String arg0, Throwable arg1) {
  }

  @Override
  public void addFilter(ch.qos.logback.core.filter.Filter arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addInfo(String info) {
    System.out.println("addInfo");
    // invoke("publishLogEvent", info);
  }

  @Override
  public void addInfo(String info, Throwable arg1) {
    // invoke("publishLogEvent", info);
  }

  @Override
  public void addStatus(Status arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addWarn(String arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addWarn(String arg0, Throwable arg1) {
    // TODO Auto-generated method stub

  }

  @Override
  public void clearAllFilters() {
    // TODO Auto-generated method stub

  }

  /**
   * Main interface through which slf4j sends logging. This method in turn
   * publishes the events to a MRL publishLogEvent topic.
   */
  @Override
  public void doAppend(ILoggingEvent event) throws LogbackException {
    String name = Thread.currentThread().getName();
    buffer.add(new LogEntry(event));
    // if (buffer.size() > maxSize || System.currentTimeMillis() -
    // lastPublishLogTimeTs > minIntervalMs) {
    // // event.get
    // flush();
    // }
  }

  /**
   * flush publishes the current buffer and creates a new one. It can be called
   * by either the thread adding a log entry or by a scheduled task which
   * flushes automatically at some interval. The task is to keep from starving
   * for information when the logging is sparse
   */
  synchronized public void flush() {
    if (buffer.size() > 0) {
      invoke("publishLogEvents", buffer);
      buffer = new ArrayList<>(maxSize);
      lastPublishLogTimeTs = System.currentTimeMillis();
    }
  }

  @Override
  public Context getContext() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List getCopyOfAttachedFiltersList() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public FilterReply getFilterChainDecision(ILoggingEvent arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isStarted() {
    return true;
  }

  public List<LogEntry> publishLogEvents(List<LogEntry> entries) {
    return entries;
  }

  @Override
  public void setContext(Context arg0) {
    // TODO Auto-generated method stub

  }

  public void setRootLogLevel(String level) {

    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    if (level == null || level.length() == 0) {
      stopLogging();
      return;
    } else if ("debug".equalsIgnoreCase(level)) {
      root.setLevel(ch.qos.logback.classic.Level.DEBUG);
    } else if ("info".equalsIgnoreCase(level)) {
      root.setLevel(ch.qos.logback.classic.Level.INFO);
    } else if ("warn".equalsIgnoreCase(level)) {
      root.setLevel(ch.qos.logback.classic.Level.WARN);
    } else if ("error".equalsIgnoreCase(level)) {
      root.setLevel(ch.qos.logback.classic.Level.ERROR);
    } else {
      log.error("unknown logging level {}", level);
    }

    if (!isLogging) {
      root.addAppender(this);
    }
    // getting current level before broadcasting state
    getLogLevel();
    broadcastState();
  }

  @Override
  public void setName(String name) {
  }

  @Override
  public void start() {
  }

  public void startLogging() {
    // LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    // root.setLevel(ch.qos.logback.classic.Level.INFO);
    root.addAppender(this);
    isLogging = true;
    addTask(minIntervalMs, "flush");
  }

  @Override
  public void startService() {
    super.startService();
    startLogging();
  }

  @Override
  public void stop() {
  }

  public String getLog() throws IOException {
    return FileIO.toString(MYROBOTLAB_LOG);
  }

  public void stopLogging() {
    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.detachAppender(this);
    isLogging = false;
    purgeTasks();
  }

  @Override
  public void stopService() {
    super.stopService();
    stopLogging();
  }

  public static void main(String[] args) {

    LoggingFactory.init();

    try {

      // Log4jLoggerAdapter blah;

      Runtime.start("log", "Log");
      Runtime.start("python", "Python");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      Runtime runtime = Runtime.getInstance();
      runtime.startInteractiveMode();
      log.info("this is an info test");
      log.warn("this is an warn test");
      log.error("this is an error test");
      // Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }

  }

}
