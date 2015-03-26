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

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.slf4j.Logger;

/**
 * test catcher is a class to be used to exercise and verify publish, subscribe
 * and other forms of message sending
 * 
 * @author GroG
 *
 */
public class TestCatcher extends Service implements SerialDataListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(TestCatcher.class);

	/**
	 * data to hold the incoming messages
	 */
	transient BlockingQueue<Message> msgs = new LinkedBlockingQueue<Message>();
	transient BlockingQueue<Object> data = new LinkedBlockingQueue<Object>();
	
	ArrayList<Status> errorList = new ArrayList<Status>();

	boolean isLocal = true;

	/**
	 * awesome override to simulate remote services - e.g. in
	 * Serial.addByteListener
	 */
	public boolean isLocal() {
		return isLocal;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		try {
			TestCatcher catcher01 = new TestCatcher("catcher01");
			catcher01.startService();

			TestThrower thrower = new TestThrower("thrower");
			thrower.startService();

			catcher01.subscribe("throwInteger", thrower.getName(), "catchInteger", Integer.class);

			for (int i = 0; i < 1000; ++i) {
				thrower.invoke("throwInteger", i);
				if (i % 100 == 0) {
					thrower.sendBlocking(catcher01.getName(), "catchInteger");
				}
			}

			// thrower.throwInteger(count);

		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	public Status onError(Status error){
		errorList.add(error);
		return error;
	}
	
	public TestCatcher(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "testing", "framework" };
	}

	/**
	 * some pub/sub interfaces do not use the Message queue to post their data -
	 * but use a callback thread from the other service as an optimization
	 * onByte is one of those methods
	 */
	@Override
	public Integer onByte(Integer b) {
		addData("onByte", b);
		return b;
	}

	/**
	 * preProcessHook is used to intercept messages and process or route them
	 * before being processed/invoked in the Service.
	 * 
	 * @throws
	 * 
	 * @see org.myrobotlab.framework.Service#preProcessHook(org.myrobotlab.framework.Message)
	 */
	@Override
	public boolean preProcessHook(Message msg) {
		try {
			msgs.put(msg);
			log.info(String.format("%d msg %s ", msgs.size(), msg));
		} catch (Exception e) {
			Logging.logError(e);
		}
		return false;
	}

	public void clear() {
		data.clear();
		msgs.clear();
	}

	public BlockingQueue<Message> getMsgs() {
		return msgs;
	}

	@Override
	public String getDescription() {
		return "service for junit tests";
	}

	public Message getMsg(long timeout) throws InterruptedException {
		Message msg = msgs.poll(timeout, TimeUnit.MILLISECONDS);
		return msg;
	}

	public Object getData(long timeout) throws InterruptedException {
		Object obj = data.poll(timeout, TimeUnit.MILLISECONDS);
		return obj;
	}

	public ArrayList<Message> waitForMsgs(int count) throws InterruptedException, IOException {
		return waitForMsgs(count, 1000, 100);
	}

	public ArrayList<Message> waitForMsgs(int count, int timeout) throws InterruptedException, IOException {
		return waitForMsgs(count, timeout, 100);
	}

	public ArrayList<Message> waitForMsgs(int count, int timeout, int pollInterval) throws InterruptedException, IOException {
		ArrayList<Message> ret = new ArrayList<Message>();
		long start = System.currentTimeMillis();
		long now = start;

		while (ret.size() < count) {
			now = System.currentTimeMillis();
			Message msg = msgs.poll(pollInterval, TimeUnit.MILLISECONDS);
			if (msg != null) {
				ret.add(msg);
			}
			if (now - start > timeout) {
				String error = String.format("waited %d ms received %d messages expecting %d in less than %d ms", now - start, ret.size(), count, timeout);
				log.error(error);
				throw new IOException(error);
			}
		}

		log.info(String.format("returned %d msgs in %s ms", ret.size(), now - start));
		return ret;
	}

	public ArrayList<?> waitForData(int count) throws InterruptedException, IOException {
		return waitForData(count, 1000, 100);
	}

	public ArrayList<?> waitForData(int count, int timeout) throws InterruptedException, IOException {
		return waitForData(count, timeout, 100);
	}

	public ArrayList<?> waitForData(int count, int timeout, int pollInterval) throws InterruptedException, IOException {
		int msgCount = 0;
		ArrayList<Object> ret = new ArrayList<Object>();
		long start = System.currentTimeMillis();
		long now = start;

		while (msgCount < count) {
			Object msg = data.poll(pollInterval, TimeUnit.MILLISECONDS);
			if (msg != null) {
				ret.add(msg);
			}
			now = System.currentTimeMillis();
			if (now - start > timeout) {
				throw new IOException(String.format("waited %d ms received %d messages expecting %d in less than %d ms", now - start, ret.size(), count, timeout));
			}
		}

		log.info("returned %d data in %s ms", ret.size(), now - start);
		return ret;
	}

	public int getMsgCount() {
		return msgs.size();
	}

	@Override
	public String onConnect(String portName) {
		info("connected to %s", portName);
		addData("onConnect", portName);
		return portName;
	}

	@Override
	public String onDisconnect(String portName) {
		info("disconnect to %s", portName);
		addData("onDisconnect", portName);
		return portName;
	}

	public void checkMsg(String method) throws InterruptedException, IOException {
		checkMsg(1000, method, (Object[]) null);
	}

	public void checkMsg(String method, Object... checkParms) throws InterruptedException, IOException {
		checkMsg(1000, method, checkParms);
	}

	public void checkMsg(long timeout, String method, Object... checkParms) throws InterruptedException, IOException {
		Message msg = getMsg(timeout);
		if (msg == null) {
			log.error(String.format("%s", msg));
			throw new IOException(String.format("reached timeout of %d waiting for message", timeout));
		}
		if (checkParms != null && checkParms.length != msg.data.length) {
			log.error(String.format("%s", msg));
			throw new IOException(String.format("incorrect number of expected parameters - expected %d got %d", checkParms.length, msg.data.length));
		}

		if (checkParms == null && msg.data != null) {
			log.error(String.format("%s", msg));
			throw new IOException(String.format("expected null parameters - got non-null"));
		}

		if (checkParms != null && msg.data == null) {
			log.error(String.format("%s", msg));
			throw new IOException(String.format("expected non null parameters - got null"));
		}

		if (!method.equals(msg.method)) {
			log.error(String.format("%s", msg));
			throw new IOException(String.format("unlike methods - expected %s got %s", method, msg.method));
		}

		for (int i = 0; i < checkParms.length; ++i) {
			Object expected = checkParms[i];
			Object got = msg.data[i];
			if (!expected.equals(got)) {
				throw new IOException(String.format("unlike methods - expected %s got %s", method, msg.method));
			}
		}

	}

	/**
	 * "unified"? way of testing direct callbacks. reconstruct the message that
	 * "would have" been created to make this direct callback
	 * 
	 * @param method
	 * @param parms
	 * @throws InterruptedException
	 */
	public void addData(String method, Object... parms) {
		try {
			Message msg = new Message();
			msg.method = method;
			msg.data = parms;
			msgs.put(msg);
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

}
