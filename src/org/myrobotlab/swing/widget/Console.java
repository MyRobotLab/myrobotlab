package org.myrobotlab.swing.widget;

import java.awt.Component;
import java.awt.EventQueue;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;

// http://www.javaworld.com/javaworld/jw-12-2004/jw-1220-toolbox.html?page=5
public class Console implements Appender<ILoggingEvent> {

  public JTextArea textArea = null;
  public JScrollPane scrollPane = null;
  private boolean logging = false;

  public Console() { // TODO boolean JFrame or component
    textArea = new JTextArea();
    scrollPane = new JScrollPane(textArea);
    DefaultCaret caret = (DefaultCaret) textArea.getCaret();
    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
  }

  /*
   * Format and then append the loggingEvent to the stored JTextArea.
   */
  public void append(final String msg) {

    // textarea not threadsafe, needs invokelater
    EventQueue.invokeLater(new Runnable() {
      // @Override
      public void run() {
        textArea.append(msg + "\n");
      }
    });

  }

  public JScrollPane getScrollPane() {
    return scrollPane;
  }

  public Component getTextArea() {
    return textArea;
  }

  /**
   * to begin logging call this function Log must not begin before the
   * SwingGui has finished drawing. For some reason, if log entries are
   * written to a JScrollPane before the gui has completed the whole gui will
   * tank
   * 
   * by default logging is off
   */
  public void startLogging() {
    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    // root.setLevel(ch.qos.logback.classic.Level.INFO); GAH !!! DONT DO THIS runtime sets the root
    root.addAppender(this);
    // PatternLayout layout = new PatternLayout("%-4r [%t] %-5p %c %x -
    // %m%n");
    // setLayout(layout);
    // setName("ConsoleGUI");
    // LoggingFactory.getInstance().addAppender(this);
    logging = true;
  }

  public void stopLogging() {
    LoggingFactory.getInstance().removeAppender(this);
    logging = false;
  }

  @Override
  public boolean isStarted() {
    // logging interface stuff
    return logging;
  }

  @Override
  public void start() {
    // TODO Auto-generated method stub

  }

  @Override
  public void stop() {
    // TODO Auto-generated method stub

  }

  @Override
  public void addError(String arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addError(String arg0, Throwable arg1) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addInfo(String info) {
    // TODO: should we publish this info / invoke something?!
    // invoke("publishLogEvent", info);
  }

  @Override
  public void addInfo(String info, Throwable arg1) {
    // TODO : should we invoke this publish method?
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
  public Context getContext() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setContext(Context arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addFilter(Filter<ILoggingEvent> arg0) {
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
  public FilterReply getFilterChainDecision(ILoggingEvent arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void doAppend(ILoggingEvent loggingEvent) throws LogbackException {
    // append(loggingEvent);
    if (logging) {
      final String msg = String.format("[%s] %s", loggingEvent.getThreadName(), loggingEvent.toString()).trim();

      // textarea not threadsafe, needs invokelater
      EventQueue.invokeLater(new Runnable() {
        // @Override
        public void run() {
          textArea.append(msg + "\n");
        }
      });
    }

  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setName(String arg0) {
    // TODO Auto-generated method stub

  }

  /// next

}