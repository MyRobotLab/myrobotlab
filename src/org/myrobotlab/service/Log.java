/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.slf4j.Logger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;

/**
 * 
 * Log - This service should allow you to record and play back messages. for
 * testing purposes only.
 *
 */

// TODO - add non Root log level changing ability - Service.setLogLevel

public class Log extends Service implements Appender<ILoggingEvent>, NameProvider, CommunicationInterface {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Log.class);

	/**
	 * onLogEvent subscriber private queue :)
	 */
	public HashMap<String, ArrayList<MRLListener>> publishLogEventNotifyList = new HashMap<String, ArrayList<MRLListener>>();

	boolean isLogging = false;
	String logLevel = "info";

	/*
	 * TODO - allow options to record and playback message log - serialize to
	 * disk etc
	 */

	// TODO - do in Service

	public Log(String n) {
		super(n);
	}

	public void addListener(MRLListener listener) {
		addListener(listener.topicMethod, listener.callbackName, listener.callbackMethod);
	}

	public void addListener(String topicMethod, String callbackName, String callbackMethod) {
		if ("publishLogEvent".equals(topicMethod)) {
			log.info("private subscription {} {} {}", topicMethod, callbackName, callbackMethod);
			MRLListener listener = new MRLListener(topicMethod, callbackName, callbackMethod);
			if (publishLogEventNotifyList.containsKey(listener.topicMethod.toString())) {
				// iterate through all looking for duplicate
				boolean found = false;
				ArrayList<MRLListener> nes = publishLogEventNotifyList.get(listener.topicMethod.toString());
				for (int i = 0; i < nes.size(); ++i) {
					MRLListener entry = nes.get(i);
					if (entry.equals(listener)) {
						log.warn(String.format("attempting to add duplicate MRLListener %s", listener));
						found = true;
						break;
					}
				}
				if (!found) {
					log.info(String.format("adding addListener from %s.%s to %s.%s", this.getName(), listener.topicMethod, listener.callbackName, listener.callbackMethod));
					nes.add(listener);
				}
			} else {
				ArrayList<MRLListener> notifyList = new ArrayList<MRLListener>();
				notifyList.add(listener);
				log.info(String.format("adding addListener from %s.%s to %s.%s", this.getName(), listener.topicMethod, listener.callbackName, listener.callbackMethod));
				publishLogEventNotifyList.put(listener.topicMethod.toString(), notifyList);
			}
		} else {
			super.addListener(topicMethod, callbackName, callbackMethod);
		}
	}

	public String publishLogEvent(String entry) {
		return entry;
	}

	public Message log(Message m) {
		log.info("log message from " + m.sender + "." + m.data);
		return m;
	}

	@Override
	public boolean preProcessHook(Message m) {
		if (m.method.equals("log")) {
			invoke("log", m);
			return false;
		}
		return true;
	}

	public void startService() {
		super.startService();
		startLogging();
	}

	@Override
	public boolean isStarted() {
		return true;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void addError(String msg) {
	}

	@Override
	public void addError(String arg0, Throwable arg1) {
	}

	@Override
	public void addInfo(String info) {
		invoke("publishLogEvent", info);
	}

	@Override
	public void addInfo(String info, Throwable arg1) {
		invoke("publishLogEvent", info);
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
	public void addFilter(ch.qos.logback.core.filter.Filter arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearAllFilters() {
		// TODO Auto-generated method stub

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

	/**
	 * Main interface through which slf4j sends logging.
	 * This method in turn publishes the events to a MRL publishLogEvent topic.
	 */
	@Override
	public void doAppend(ILoggingEvent event) throws LogbackException {
		// event.getFormattedMessage();
		Message msg = Message.createMessage(this, null, "onLogEvent", new Object[] { String.format("[%s] %s", event.getThreadName(), event.toString()) });
		msg.sendingMethod = "publishLogEvent";
		msg.sender = getName();
		Object[] param = new Object[] { msg };

		// Object[] param = new Object[] { String.format("[%s] %s",
		// arg0.getThreadName(), arg0.toString()) };

		if (publishLogEventNotifyList.size() != 0) {
			// get the value for the source method
			ArrayList<MRLListener> subList = publishLogEventNotifyList.get("publishLogEvent");
			for (int i = 0; i < subList.size(); ++i) {
				MRLListener listener = subList.get(i);

				ServiceInterface si = Runtime.getService(listener.callbackName);
				Class<?> c = si.getClass();
				try {
					Method meth = c.getMethod(listener.callbackMethod, new Class<?>[] { Message.class });
					// TODO: what to do with this returned object?
					// Object retobj = meth.invoke(si, param);
					meth.invoke(si, param);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// send(msg);

				// must make new for internal queues
				// otherwise you'll change the name on
				// existing enqueued messages
				// msg = new Message(msg);
			}
		}
	}

	public void add(Message msg) throws InterruptedException {
		LinkedList<Message> msgBox = getOutbox().getMsgBox();
		synchronized (msgBox) {
			while (msgBox.size() > getOutbox().getMaxQueueSize()) {
				msgBox.wait(); // Limit the size
			}
			msgBox.addFirst(msg);
			msgBox.notifyAll(); // must own the lock
		}
	}

	public void setRootLogLevel(String level) {

		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

		if (level == null || level.length() == 0) {
			stopLogging();
			return;
		} else if ("debug".equalsIgnoreCase(level)) {
			root.setLevel(ch.qos.logback.classic.Level.DEBUG);
			logLevel = "debug";
		} else if ("info".equalsIgnoreCase(level)) {
			root.setLevel(ch.qos.logback.classic.Level.INFO);
			logLevel = "info";
		} else if ("warn".equalsIgnoreCase(level)) {
			root.setLevel(ch.qos.logback.classic.Level.WARN);
			logLevel = "warn";
		} else if ("error".equalsIgnoreCase(level)) {
			root.setLevel(ch.qos.logback.classic.Level.ERROR);
			logLevel = "error";
		} else {
			log.error("unknown logging level {}", level);
		}

		if (!isLogging) {
			root.addAppender(this);
		}

		broadcastState();
	}

	public void startLogging() {
		// LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(ch.qos.logback.classic.Level.INFO);
		root.addAppender(this);
		isLogging = true;
	}

	public void stopLogging() {
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.detachAppender(this);
		isLogging = false;
	}

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		try {

			// Log4jLoggerAdapter blah;

			Runtime.start("log", "Log");
			Runtime.start("python", "Python");
			Runtime.start("webgui", "WebGui");
			log.info("this is an info test");
			log.warn("this is an warn test");
			log.error("this is an error test");
			// Runtime.start("gui", "SwingGui");

		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	@Override
	public void addRemote(URI mrlHost, URI protocolKey) {
		// not a gateway - don't need to worry about this
	}

	@Override
	public void send(Message msg) {
		ServiceInterface sw = Runtime.getService(msg.getName());
		if (sw == null) {
			ServiceInterface sender = Runtime.getService(msg.sender);
			if (sender != null) {
				sender.removeListener(msg.sendingMethod, msg.getName(), msg.method);
			}
			return;
		}

		URI host = sw.getInstanceId();
		if (host == null) {
			sw.in(msg);
		}
	}

	@Override
	public void send(URI uri, Message msg) {
		// no remote sending enabled
	}

	static public String[] getCategories() {
		return new String[] { "testing" };
	}

	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(Log.class.getCanonicalName());
		meta.addDescription("Logging Service helpful in diagnostics");
		meta.addCategory("framework");

		return meta;
	}

}
