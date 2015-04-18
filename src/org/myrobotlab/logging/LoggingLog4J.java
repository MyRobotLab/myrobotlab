package org.myrobotlab.logging;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.SimpleTimeZone;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.net.SocketAppender;
import org.myrobotlab.framework.Service;

public class LoggingLog4J extends Logging {

	public final static Logger log = Logger.getLogger(Logging.class);

	private HashSet<String> appenders = new HashSet<String>();

	@Override
	public void addAppender(Object console) {
		Logger.getRootLogger().addAppender((AppenderSkeleton) console);
	}

	/**
	 * 
	 * @param type
	 */
	@Override
	public void addAppender(String type) {
		addAppender(type, null, null);
	}

	/**
	 * 
	 * @param type
	 * @param hostOrMultiFile
	 * @param port
	 */
	@Override
	public void addAppender(String type, String hostOrMultiFile, String port) {
		if (appenders.contains(type)) {
			log.warn(String.format("already have %s type of appender - disregarding", type));
			return;
		}
		// same format as .configure()
		PatternLayout layout = new PatternLayout("%-4r [%t] %-5p %c %x - %m%n");
		org.apache.log4j.Appender appender = null;

		// TODO the type should be an enumeration so that we can make this a
		// switch statement (unless Python dependencies don't allow for it)
		try {
			if (Appender.CONSOLE.equalsIgnoreCase(type)) {
				appender = new ConsoleAppender(layout);
				appender.setName(type);
				appenders.add(Appender.CONSOLE);
			} else if (Appender.REMOTE.equalsIgnoreCase(type)) {
				appender = new SocketAppender(hostOrMultiFile, Integer.parseInt(port));
				appender.setName(type);
				appenders.add(Appender.REMOTE);
			} else if (Appender.FILE.equalsIgnoreCase(type)) {
				if (hostOrMultiFile != null) {
					SimpleDateFormat TSFormatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
					Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
					TSFormatter.setCalendar(cal);

					appender = new RollingFileAppender(layout, String.format("%s%smyrobotlab.%s.log", System.getProperty("user.dir"), File.separator,
							TSFormatter.format(new Date())), false);
					appender.setName(type);
				} else {
					appender = new RollingFileAppender(layout, String.format("%s%smyrobotlab.log", System.getProperty("user.dir"), File.separator), false);
					appender.setName(type);

				}

				appenders.add(Appender.FILE);
			} else if (Appender.IS_AGENT.equalsIgnoreCase(type)) {
				// FROM_AGENT has only console - Agent has both console & file
				// appender
				appender = new ConsoleAppender(layout);
				appender = new RollingFileAppender(layout, String.format("%s%smyrobotlab.log", System.getProperty("user.dir"), File.separator), false);
				appender.setName(type);
				appenders.add(Appender.IS_AGENT);
			} else if (Appender.FROM_AGENT.equalsIgnoreCase(type)) {
				// only has console because the console is relayed to the Agent
				// shorter layout than Agent - since everything will be
				// prepended to Agent's log prefix
				// layout = new PatternLayout("[%t] %-5p %c %x - %m%n");
				// TODO - add PID or runtime Name ! process index ?
				// layout = new PatternLayout("[%t] %-5p %c %x - %m%n"); SHORT
				// PATTERN ???
				// appender = new RollingFileAppender(layout,
				// String.format("%s%agent.log", System.getProperty("user.dir"),
				// File.separator), false);
				appender = new ConsoleAppender(layout);
				appender.setName(type);
				appenders.add(Appender.FROM_AGENT);
			} else {
				log.error(String.format("attempting to add unkown type of Appender %1$s", type));
				return;
			}
		} catch (Exception e) {
			System.out.println(Service.stackToString(e));
		}

		if (appender != null) {
			Logger.getRootLogger().addAppender(appender);
		}

		if (type.equalsIgnoreCase(Appender.NONE)) {
			Logger.getRootLogger().removeAllAppenders();
		}

	}

	@Override
	public void configure() {
		org.apache.log4j.BasicConfigurator.configure();
	}

	@Override
	public String getLevel() {
		Logger root = Logger.getRootLogger();
		String level = root.getLevel().toString();
		// FIXME - normalize
		// if "something" return Level.INFO
		return level;
	}

	@Override
	public void removeAllAppenders() {
		Logger.getRootLogger().removeAllAppenders();
	}

	@Override
	public void removeAppender(Object console) {
		Logger.getRootLogger().removeAppender((AppenderSkeleton) console);
	}

	/**
	 * 
	 * @param name
	 */
	@Override
	public void removeAppender(String name) {
		Logger.getRootLogger().removeAppender(name);
	}

	@Override
	public void setLevel(String level) {
		setLevel(null, level);
	}

	@Override
	public void setLevel(String clazz, String level) {

		Logger logger = null;
		level = level.toUpperCase();

		try {
			if (clazz != null) {
				logger = org.apache.log4j.Logger.getLogger(clazz);
			}
		} catch (Exception e) {
		}

		if (logger == null) {
			logger = org.apache.log4j.Logger.getRootLogger();
		}

		if ("DEBUG".equalsIgnoreCase(level)) { // && log4j {
			logger.setLevel(org.apache.log4j.Level.DEBUG);
		} else if ("TRACE".equalsIgnoreCase(level)) { // && log4j {
			logger.setLevel(org.apache.log4j.Level.TRACE);
		} else if ("WARN".equalsIgnoreCase(level)) { // && log4j {
			logger.setLevel(org.apache.log4j.Level.WARN);
		} else if ("ERROR".equalsIgnoreCase(level)) { // && log4j {
			logger.setLevel(org.apache.log4j.Level.ERROR);
		} else if ("FATAL".equalsIgnoreCase(level)) { // && log4j {
			logger.setLevel(org.apache.log4j.Level.FATAL);
		} else { // && log4j {
			logger.setLevel(org.apache.log4j.Level.INFO);
		}
	}

}
