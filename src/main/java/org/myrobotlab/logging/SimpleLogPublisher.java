package org.myrobotlab.logging;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.service.interfaces.LogPublisher;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;

public class SimpleLogPublisher implements Appender<ILoggingEvent> {

  LogPublisher service = null;
  boolean isStarted = false;
  Set<String> includeClasses = null;

  public SimpleLogPublisher(LogPublisher service) {
    this.service = service;
  }

  public void filterClasses(String[] classNames) {
    if (includeClasses == null) {
      includeClasses = new HashSet<>();
    }
    for (String className : classNames) {
      includeClasses.add(className);
    }
  }

  @Override
  public void start() {
    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.addAppender(this);
    isStarted = true;
  }

  @Override
  public void stop() {
    isStarted = false;
  }

  @Override
  public boolean isStarted() {
    return isStarted;
  }

  @Override
  public void setContext(Context context) {
    // TODO Auto-generated method stub

  }

  @Override
  public Context getContext() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void addStatus(Status status) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addInfo(String msg) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addInfo(String msg, Throwable ex) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addWarn(String msg) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addWarn(String msg, Throwable ex) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addError(String msg) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addError(String msg, Throwable ex) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addFilter(Filter<ILoggingEvent> newFilter) {
    // TODO Auto-generated method stub

  }

  @Override
  public void clearAllFilters() {
    // TODO Auto-generated method stub

  }

  @Override
  public List<Filter<ILoggingEvent>> getCopyOfAttachedFiltersList() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public FilterReply getFilterChainDecision(ILoggingEvent event) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getName() {
    if (service != null) {
      return service.getName();
    }
    return null;
  }

  @Override
  public void doAppend(ILoggingEvent event) throws LogbackException {
    if (isStarted) {

      String msg = null;
      if (includeClasses == null || (includeClasses != null && includeClasses.contains(event.getLoggerName()))) {
        msg = event.toString().trim();
      }

      if (msg != null) {
        service.invoke("publishLog", msg);
      }
    }

  }

  @Override
  public void setName(String name) {
    // NOOP - name is service name
  }

}
