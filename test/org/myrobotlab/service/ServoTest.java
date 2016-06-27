package org.myrobotlab.service;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class ServoTest {

	public final static Logger log = LoggerFactory.getLogger(ServoTest.class);
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testReleaseService() {
		fail("Not yet implemented");
	}

	@Test
	public void testServo() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddServoEventListener() {
		fail("Not yet implemented");
	}

	@Test
	public void testAttach() throws Exception {
		
		Arduino arduino = (Arduino)Runtime.start("arduino", "Arduino");
		arduino.connect("COM5");
		Servo servo = (Servo)Runtime.start("servo01", "Servo");
		
		arduino.attach(servo, 8);
		servo.moveTo(30);
		servo.moveTo(130);
		servo.moveTo(30);
		servo.moveTo(130);
		
	}

	@Test
	public void testDetach() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetController() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetLastActivityTime() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMax() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMaxInput() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMaxOutput() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMin() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMinInput() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMinOutput() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetPos() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetRest() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsAttached() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsInverted() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsSweeping() {
		fail("Not yet implemented");
	}

	@Test
	public void testMap() {
		fail("Not yet implemented");
	}

	@Test
	public void testMoveTo() {
		fail("Not yet implemented");
	}

	@Test
	public void testPublishServoEvent() {
		fail("Not yet implemented");
	}

	@Test
	public void testRefreshControllers() {
		fail("Not yet implemented");
	}

	@Test
	public void testRest() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetController() {
		fail("Not yet implemented");
	}

	@Test
	public void testEventsEnabled() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetInverted() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetMinMax() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetRest() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetSpeed() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetSpeedControlOnUC() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetSweepDelay() {
		fail("Not yet implemented");
	}

	@Test
	public void testStop() {
		fail("Not yet implemented");
	}

	@Test
	public void testSweep() {
		fail("Not yet implemented");
	}

	@Test
	public void testSweepIntInt() {
		fail("Not yet implemented");
	}

	@Test
	public void testSweepIntIntIntInt() {
		fail("Not yet implemented");
	}

	@Test
	public void testSweepIntIntIntIntBoolean() {
		fail("Not yet implemented");
	}

	@Test
	public void testWriteMicroseconds() {
		fail("Not yet implemented");
	}

	@Test
	public void testMain() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMetaData() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetDeviceType() {
		fail("Not yet implemented");
	}
	
	public static void main(String[] args) {
	    try {

	      LoggingFactory.getInstance().configure();
	      LoggingFactory.getInstance().setLevel(Level.INFO);

	      ServoTest.setUpBeforeClass();
	      ServoTest test = new ServoTest();
	      test.setUp();
	      
	      // structured testing begins
	      test.testAttach();

	      JUnitCore junit = new JUnitCore();
	      Result result = junit.run(ServoTest.class);
	      log.info("Result: {}", result);
	     
	      // Runtime.dump();

	    } catch (Exception e) {
	      log.error("test threw",e);
	    }
	  }


}
