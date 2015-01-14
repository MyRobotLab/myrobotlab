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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
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
	public final static Logger log = LoggerFactory.getLogger(TestCatcher.class.getCanonicalName());

	/**
	 * data to hold the incoming messages
	 */
	// transient BlockingQueue<Object> blockingData = new
	// LinkedBlockingQueue<Object>();
	transient BlockingQueue<Message> data = new LinkedBlockingQueue<Message>();

	public TestCatcher(String n) {
		super(n);
	}

	/**
	 * preProcessHook is used to intercept messages and process or route them
	 * before being processed/invoked in the Service.
	 * 
	 * @throws
	 * 
	 * @see org.myrobotlab.framework.Service#preProcessHook(org.myrobotlab.framework.Message)
	 */
	public boolean preProcessHook(Message msg) {
		// let the messages for this service
		// get processed normally
		/*
		 * if (methodSet.contains(msg.method)) { return true; }
		 */
		try {
			data.put(msg);
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}
	
	public BlockingQueue<Message>  getData(){
		return data;
	}
	
	public Message getMsg(long timeout) throws InterruptedException {
		Message msg = data.poll(timeout, TimeUnit.MILLISECONDS);
		return msg;
	}
	
	public ArrayList<Message> getMsgs(long timeout)throws InterruptedException {
		ArrayList<Message> msgs = new ArrayList<Message>();
		long start = System.currentTimeMillis();
		boolean done = false;
		while (!done){
			Message msg = data.poll(timeout, TimeUnit.MILLISECONDS);
			if (msg == null){
				break;
			} else {
				msgs.add(msg);
			}
		}		
		return msgs;
	}

	/*
	public int waitForCatches(int numberOfCatches, int maxWaitTimeMilli) {
		log.info(getName() + ".waitForCatches waiting for " + numberOfCatches + " currently " + catchList.size());

		StopWatch stopwatch = new StopWatch();
		synchronized (catchList) {
			if (catchList.size() < numberOfCatches) {
				try {
					stopwatch.start(); // starting clock
					while (catchList.size() < numberOfCatches) {
						catchList.wait(maxWaitTimeMilli); // wait up to the max
															// time
						stopwatch.end(); // sample time -
						if (stopwatch.elapsedMillis() > maxWaitTimeMilli) {
							log.error("waited for " + maxWaitTimeMilli + "ms and still only " + catchList.size() + " out of " + numberOfCatches);
							return catchList.size();
						}
					}

					log.info("caught " + catchList.size() + " out of " + numberOfCatches);
					return numberOfCatches;

				} catch (InterruptedException e) {
					log.error("waitForCatches " + numberOfCatches + " interrupted");
					// logException(e); - removed for Android
				}
			}
		}
		return catchList.size();
	}
	*/
	
	@Override
	public String getDescription() {
		return "service for junit tests";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

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

	}

	@Override
	public void onByte(Integer b) {
		
	}
}
