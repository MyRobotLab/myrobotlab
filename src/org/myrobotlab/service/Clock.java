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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.ClockEvent;
import org.slf4j.Logger;

/**
 * Clock - This is a simple clock service that can be started and stopped. It
 * generates a pulse with a timestamp on a regular interval defined by the
 * setInterval(Integer) method. Interval is in milliseconds.
 */
public class Clock extends Service {

	public class ClockThread implements Runnable {
		public Thread thread = null;
		ClockThread() {
			thread = new Thread(this, getName() + "_ticking_thread");
			thread.start();
		}

		@Override
		public void run() {
			
			try {
				
				while (isClockRunning == true) {
					Date now = new Date();
					Iterator<ClockEvent> i = events.iterator();
					while (i.hasNext()) {
						ClockEvent event = i.next();
						if (now.after(event.time)) {
							// TODO repeat - don't delete set time forward
							// interval
							send(event.name, event.method, event.data);
							
							i.remove();
						}
					}
					
				if (!NoExecutionAtFirstClockStarted){invoke("pulse", new Date());}
				Thread.sleep(interval);
				NoExecutionAtFirstClockStarted=false;
				}
			} catch (InterruptedException e) {
				log.info("ClockThread interrupt");
				isClockRunning = false;
			}
		}
	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Clock.class.getCanonicalName());
	public boolean isClockRunning;

	public int interval = 1000;

	public transient ClockThread myClock = null;

	// FIXME
	ArrayList<ClockEvent> events = new ArrayList<ClockEvent>();

	private boolean NoExecutionAtFirstClockStarted=false;

	public Clock(String n) {
		super(n);
	}

	public void addClockEvent(Date time, String name, String method, Object... data) {
		ClockEvent event = new ClockEvent(time, name, method, data);
		events.add(event);
	}

	// clock started event
	public void clockStarted() {
	}

	public void clockStopped() {
	}

	public Date pulse(Date time) {
		return time;
	}

	public void setInterval(Integer milliseconds) {
		interval = milliseconds;
		broadcastState();
	}

	public void startClock(boolean NoExecutionAtFirstClockStarted) {
		if (myClock == null) {
			this.NoExecutionAtFirstClockStarted=NoExecutionAtFirstClockStarted;
			// info("starting clock");
			isClockRunning = true;
			myClock = new ClockThread();
			invoke("clockStarted");
		} else {
			log.warn("clock already started");
		}

		broadcastState();
	}
	
	public void startClock() {
		startClock(false);
	}
	public void stopClock() {

		if (myClock != null) {
			// info("stopping clock");
			log.info("stopping " + getName() + " myClock");
			isClockRunning = false;
			myClock.thread.interrupt();
			myClock.thread = null;
			myClock = null;
			// have requestors broadcast state !
			// broadcastState();
			invoke("clockStopped");
		} else {
			log.warn("clock already stopped");
		}

		isClockRunning = false;
		broadcastState();
	}

	@Override
	public void stopService() {
		stopClock();
		super.stopService();
	}

	public static void main(String[] args) throws Exception {
		LoggingFactory.init(Level.INFO);

		String test = "tcp";

		if ("tcp".equals(test)) {
			// TCP CONNECT WORKS BEGIN ---------------------------------
			try {

				int i = 6;

				// for (int i = 1; i < 4; ++i) {
				Runtime.main(new String[] { "-runtimeName", String.format("runtime.%d", i) });

				// auto-grab the next port if can not listen???
				RemoteAdapter remote = (RemoteAdapter) Runtime.start(String.format("remote%d", i), "RemoteAdapter");
				// remote.setUDPPort(6868);
				// remote.setTCPPort(6868);
				// remote.scan();
				// remote.setDefaultPrefix("raspi");
				Runtime.start(String.format("clock%d", i), "Clock");
				Runtime.start(String.format("gui", i), "SwingGui");
				// Runtime.start(String.format("python", i), "Python");

				remote.connect("tcp://127.0.0.1:6767");
				// Runtime.start(String.format("p%d", i), "Python");
				// remote.scan();

				// remote.startListening();
				// remote.connect("tcp://127.0.0.1:6767");

				// FIXME - sholdn't this be sendRemote ??? or at least
				// in an interface
				// remote.sendRemote(uri, msg);
				// xmpp1.sendMessage("xmpp 2", "robot02 02");
				// }
			} catch (Exception e) {
				Logging.logError(e);
			}
			// TCP CONNECT WORKS END ---------------------------------

		} else if ("xmpp".equals(test)) {

			// XMPP CONNECT WORKS BEGIN ---------------------------------
			int i = 2;

			Runtime.main(new String[] { "-runtimeName", String.format("r%d", i) });
			Security security = (Security) Runtime.createAndStart("security", "Security");
			security.addUser("incubator incubator");
			security.setGroup("incubator incubator", "authenticated");
			security.allowExportByType("XMPP", false);
			security.allowExportByType("Security", false);
			security.allowExportByType("Runtime", false);
			Xmpp xmpp1 = (Xmpp) Runtime.createAndStart(String.format("xmpp%d", i), "XMPP");
			Clock clock = (Clock) Runtime.createAndStart(String.format("clock%d", i), "Clock");
			Runtime.createAndStart(String.format("gui%d", i), "SwingGui");

			xmpp1.connect("talk.google.com", 5222, "robot02@myrobotlab.org", "mrlRocks!");

			Message msg = null;

			msg = xmpp1.createMessage(null, "register", clock);
			String base64 = CodecUtils.msgToBase64(msg);
			xmpp1.sendMessage(base64, "incubator incubator");

			// XMPP CONNECT WORKS END ---------------------------------
		}
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

		ServiceType meta = new ServiceType(Clock.class.getCanonicalName());
		meta.addDescription("used to generate pulses and recurring messages");
		meta.addCategory("scheduling");

		return meta;
	}
}