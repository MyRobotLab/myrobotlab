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

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.StopWatch;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class TestCatcher extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(TestCatcher.class.getCanonicalName());
	public ArrayList<Integer> catchList = new ArrayList<Integer>();
	public ArrayList<Integer> lowCatchList = new ArrayList<Integer>();
	public ArrayList<String> stringCatchList = new ArrayList<String>();
	public String data = null;
	
	// static in Java are not overloaded but overwritten - there is no polymorphism for statics
	public static Peers getPeers(String name)
	{
		Peers peers = new Peers(name);
		peers.put("testThrower", "TestThrower", "a test peer thrower");
		return peers;
	}
	
	
	public TestCatcher(String n) {
		super(n);
	}

	public void catchNothing() {
		data = "***CATCH*** catchNothing ";
		log.info("***CATCH*** catchNothing ");
		Integer c = 1;
		synchronized (catchList) {
			catchList.add(c);
			catchList.notify();
		}
		
		broadcastState();

	}

	public Object returnNull() {
		return null;
	}

	public Integer catchInteger(Integer count) {
		log.info("***CATCH*** catchInteger " + count);
		synchronized (catchList) {
			catchList.add(count);
			catchList.notify();
		}
		return count;
	}

	public Integer lowCatchInteger(Integer count) {
		log.info("***CATCH*** lowCatchInteger " + count);
		synchronized (lowCatchList) {
			lowCatchList.add(count);
			lowCatchList.notify();
		}
		return count;

	}

	public Integer bothHandsCatchInteger(Integer firstBall, Integer secondBall) {
		log.info("***CATCH*** bothHandsCatchInteger " + firstBall + "," + secondBall);
		log.info("catchList.size={}",catchList.size());

		synchronized (catchList) {
			catchList.add(firstBall);
			catchList.add(secondBall);
			catchList.notify();
		}

		log.info("bothHandsCatchInteger " + firstBall + "," + secondBall);
		log.info("bothHandsCatchInteger size " + catchList.size());

		return catchList.size();
	}

	public Integer twoHandedPrimitiveCatchInt(int firstBall, int secondBall) {
		log.info("***CATCH*** twoHandedPrimitiveCatchInt " + firstBall + "," + secondBall);
		synchronized (catchList) {
			catchList.add(firstBall);
			catchList.add(secondBall);
			catchList.notify();
		}
		return lowCatchList.size();

	}

	public Integer throwBack(Integer count) {
		log.info("throwBack " + count);
		return count;
	}

	public String catchString(String data) {
		log.info("***CATCH*** string " + data);
		stringCatchList.add(data);
		return data;
	}

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

	public int waitForStringCatches(int numberOfCatches, int maxWaitTimeMilli) {
		log.info(getName() + ".waitForCatches waiting for " + numberOfCatches + " currently " + stringCatchList.size());

		StopWatch stopwatch = new StopWatch();
		synchronized (stringCatchList) {
			if (stringCatchList.size() < numberOfCatches) {
				try {
					stopwatch.start(); // starting clock
					while (stringCatchList.size() < numberOfCatches) {
						stringCatchList.wait(maxWaitTimeMilli); // wait up to
																// the max time
						stopwatch.end(); // sample time -
						if (stopwatch.elapsedMillis() > maxWaitTimeMilli) {
							log.error("waited for " + maxWaitTimeMilli + "ms and still only " + stringCatchList.size() + " out of " + numberOfCatches);
							return stringCatchList.size();
						}
					}

					log.info("caught " + stringCatchList.size() + " out of " + numberOfCatches);
					return numberOfCatches;

				} catch (InterruptedException e) {
					log.error("waitForCatches " + numberOfCatches + " interrupted");
					// logException(e); - removed for Android
				}
			}
		}
		return stringCatchList.size();
	}

	public void waitForLowCatches(int numberOfCatches, int maxWaitTimeMilli) {
		log.info(getName() + ".waitForLowCatches waiting for " + numberOfCatches + " currently " + lowCatchList.size());
		synchronized (lowCatchList) {
			while (lowCatchList.size() < numberOfCatches) {
				try {
					lowCatchList.wait(maxWaitTimeMilli);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					log.error("testObject1List " + numberOfCatches + " interrupted");
					// logException(e);
				}
			}
		}

	}

	@Override
	public String getDescription() {
		return "<html>service for junit tests</html>";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		TestCatcher catcher01 = new TestCatcher("catcher01");
		catcher01.startService();
		
		TestThrower thrower = new TestThrower("thrower");
		thrower.startService();
		
		catcher01.subscribe("throwInteger", thrower.getName(), "catchInteger", Integer.class);
		
		for (int i = 0; i < 1000; ++i)
		{
			thrower.invoke("throwInteger", i);
			if (i%100 == 0)
			{
				thrower.sendBlocking(catcher01.getName(), "catchInteger");
			}
		}
		
		//thrower.throwInteger(count);
		
	}
}
