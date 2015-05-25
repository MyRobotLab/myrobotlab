package org.myrobotlab.framework;


import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.logging.Appender;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.TestCatcher;
import org.myrobotlab.service.TestThrower;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.slf4j.Logger;

public class MessageTest {
	public final static Logger log = LoggerFactory.getLogger(MessageTest.class);

	static TestCatcher catcher;
	static TestThrower thrower;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		catcher = (TestCatcher)Runtime.start("catcher", "TestCatcher");
		thrower = (TestThrower)Runtime.start("thrower", "TestThrower");
	}


	@Test
	public void simpleSubscribeAndThrow() throws Exception {

		catcher.clear();
		catcher.subscribe("thrower", "pitch", "onPitch");
		// ROUTE MUST STABALIZE - BEFORE MSGS - otherwise they will be missed
		Service.sleep(100);
		
		thrower.pitchInt(1000);
		BlockingQueue<Message> balls = catcher.waitForMsgs(1000);

		log.warn(String.format("caught %d balls", balls.size()));
		log.warn(String.format("left balls %d ", catcher.msgs.size()));
	}

	@Test
	public void broadcastMessage() throws Exception {
		catcher.clear();
		catcher.subscribe("thrower", "pitch", "onPitch");
		
		Message msg = thrower.createMessage(null, "getServiceNames", null);
		CommunicationInterface comm = thrower.getComm();
		comm.send(msg);
		
		String[] ret = (String[])thrower.invoke(msg);
		log.info(String.format("got %s", Arrays.toString(ret)));
		assertNotNull(ret);
	}
	
	@Test
	public void clearRoutes() throws Exception {
		catcher.clear();
		catcher.subscribe("thrower", "pitch", "onPitch");
		
		Service.sleep(100);
		
		thrower.pitchInt(1000);
		BlockingQueue<Message> balls = catcher.waitForMsgs(1000);
		
		

		log.warn(String.format("caught %d balls", balls.size()));
		log.warn(String.format("left balls %d ", catcher.msgs.size()));
		
		Runtime.removeAllSubscriptions();
		
		Message msg = thrower.createMessage(null, "getServiceNames", null);
		CommunicationInterface comm = thrower.getComm();
		comm.send(msg);
		
		String[] ret = (String[])thrower.invoke(msg);
		log.info(String.format("got %s", Arrays.toString(ret)));
		assertNotNull(ret);
	}
	
	@Test
	public void invokeStringNotation() throws Exception {
		catcher.clear();
		catcher.subscribe("thrower/pitch", "onPitch");
		
		Runtime runtime = Runtime.getInstance();
		
		Message msg = thrower.createMessage(null, "getServiceNames", null);
		CommunicationInterface comm = thrower.getComm();
		comm.send(msg);
		
		String[] ret = (String[])thrower.invoke(msg);
		log.info(String.format("got %s", Arrays.toString(ret)));
		assertNotNull(ret);
	}
	
	public static void main(String[] args) {
		try {
			
			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.DEBUG);
			Logging logging = LoggingFactory.getInstance();
			logging.addAppender(Appender.FILE);
			
			setUpBeforeClass();
		

		} catch(Exception e){
			Logging.logError(e);
		}
		
		System.exit(0);
	}
}
