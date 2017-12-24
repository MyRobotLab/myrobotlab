package org.myrobotlab.logging;

import java.nio.charset.StandardCharsets;

import org.apache.log4j.DailyRollingFileAppender;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.util.StatusPrinter;

public class LoggingSLF4J extends Logging {

  @Override
  public void addAppender(Object type) {
    // BIG NOOP
  }

  @Override
  public void addAppender(String type) {
    addAppender(type, null);
  }
  
  // http://stackoverflow.com/questions/7824620/logback-set-log-file-name-programmatically

  @Override
  public void addAppender(String type, String filename) {

    // OutputStreamAppender<E>
    // ConsoleAppender, FileAppender

    // OutputStreamAppender<ILoggingEvent> appender = null;

    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    PatternLayoutEncoder ple = new PatternLayoutEncoder();

    // %date
    ple.setCharset(StandardCharsets.UTF_8);
    ple.setPattern("%date{HH:mm:ss.SSS} [%thread] %level %logger{10} [%file:%line] %msg%n");
    ple.setContext(lc);
    ple.start();

    // allows you to add appenders to different logging locations
    Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    logger.setAdditive(false);

    // TODO - do layout ???

    if (Appender.CONSOLE.equalsIgnoreCase(type)) {
      ConsoleAppender<ILoggingEvent> console = new ConsoleAppender<ILoggingEvent>();
      console.setName(type);
      // console.setLayout(layout); ???
      console.setEncoder(ple);
      console.setContext(lc);
      console.start();
      logger.addAppender(console);
    } else if (Appender.FILE.equalsIgnoreCase(type)) {
      FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
      fileAppender.setName(type);
      fileAppender.setFile(LoggingFactory.getLogFileName());
      fileAppender.setEncoder(ple);
      fileAppender.setContext(lc);
      fileAppender.setAppend(false);
      fileAppender.start();
      logger.addAppender(fileAppender);
    } else if (Appender.IS_AGENT.equalsIgnoreCase(type)) {
      // FROM_AGENT has only console - Agent has both console & file
      // appender
      /*
       * appender = new ConsoleAppender(layout); appender = new
       * RollingFileAppender(layout, String.format("%s%smyrobotlab.log",
       * System.getProperty("user.dir"), File.separator), false);
       * appender.setName(type); appenders.add(Appender.IS_AGENT);
       */

      // console
      ConsoleAppender<ILoggingEvent> console = new ConsoleAppender<ILoggingEvent>();
      console.setName(String.format("%s.%s", Appender.IS_AGENT, Appender.CONSOLE));
      // console.setLayout(layout); ???
      console.setEncoder(ple);
      console.setContext(lc);
      logger.addAppender(console);
      
      // grr DailyRollingFileAppender f = new DailyRollingFileAppender();

      FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
      fileAppender.setName(String.format("%s.%s", Appender.IS_AGENT, Appender.FILE));
      // fileAppender.setFile(String.format("%s%smyrobotlabz.log", System.getProperty("user.dir"), File.separator));
      fileAppender.setFile(LoggingFactory.getLogFileName());
      fileAppender.setEncoder(ple);
      fileAppender.setAppend(false);
      fileAppender.setContext(lc);
      fileAppender.start();

      // logger.addAppender(console); // THIS IS NEW ! PREVIOUSLY A BUG
      // PREVENTED THIS ..
      // going to keep it shutdown - so as not to not do too much logging
      logger.addAppender(fileAppender);

    } else if (Appender.FROM_AGENT.equalsIgnoreCase(type)) {
      // only has console because the console is relayed to the Agent
      // shorter layout than Agent - since everything will be
      // prepended to Agent's log prefix
      // layout = new PatternLayout("[%t] %-5p %c %x - %m%n");
      // TODO - add Pid or runtime Name ! process index ?
      // layout = new PatternLayout("[%t] %-5p %c %x - %m%n"); SHORT
      // PATTERN ???
      // appender = new RollingFileAppender(layout,
      // String.format("%s%agent.log", System.getProperty("user.dir"),
      // File.separator), false);

      // console
      ConsoleAppender<ILoggingEvent> console = new ConsoleAppender<ILoggingEvent>();
      console.setName(String.format("%s.%s", Appender.FROM_AGENT, Appender.CONSOLE));
      // console.setLayout(layout); ???
      console.setEncoder(ple);
      console.setContext(lc);
      logger.addAppender(console);

    } else {
      log.error(String.format("attempting to add unkown type of Appender %1$s", type));
      return;
    }

  }

  @Override
  public void configure() {
    // LoggerFactory.getILoggerFactory();
    // BasicConfigurator.configureDefaultContext();
    // http://www.mkyong.com/logging/logback-duplicate-log-messages/
    // BasicConfigurator.configureDefaultContext();
    // LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    // BasicConfigurator.configure(lc);
    // BasicConfigurator.configure(null);

    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(context);
    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
  }

  @Override
  public String getLevel() {
    /*
     * Map<String,String> levels=newTreeMap(); LoggerContext
     * context=(LoggerContext)LoggerFactory.getILoggerFactory(); for ( Logger
     * logger : context.getLoggerList()) { if (logger.getLevel() != null) {
     * levels.put(logger.getName(),logger.getLevel().toString()); } }
     */

    Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    Level level = logger.getLevel();
    if (level.equals(Level.DEBUG)) {
      return "DEBUG";
    } else if (level.equals(Level.INFO)) {
      return "INFO";
    } else if (level.equals(Level.WARN)) {
      return "WARN";
    } else if (level.equals(Level.ERROR)) {
      return "ERROR";
    }
    return "UNKNOWN";
  }

  @Override
  public void removeAllAppenders() {
    Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    logger.detachAndStopAllAppenders();
  }

  @Override
  public void removeAppender(Object console) {
    // Another big NOOP

  }

  @Override
  public void removeAppender(String name) {

    Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    logger.detachAppender(name); // does this stop it too ?
  }

  @Override
  public void setLevel(String level) {
    setLevel(null, level);
  }

  @Override
  public void setLevel(String clazz, String level) {
    if (clazz == null || clazz.length() == 0) {
      clazz = Logger.ROOT_LOGGER_NAME;
    }

    Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    if ("DEBUG".equalsIgnoreCase(level)) { // && log4j {
      logger.setLevel(Level.DEBUG);
    } else if ("TRACE".equalsIgnoreCase(level)) { // && log4j {
      logger.setLevel(Level.TRACE);
    } else if ("WARN".equalsIgnoreCase(level)) { // && log4j {
      logger.setLevel(Level.WARN);
    } else if ("ERROR".equalsIgnoreCase(level)) { // && log4j {
      logger.setLevel(Level.ERROR);
      // } else if ("FATAL".equalsIgnoreCase(level)) { // && log4j {
      // logger.setLevel(Level.FATAL);
    } else { // && log4j {
      logger.setLevel(Level.INFO);
    }
  }

}
