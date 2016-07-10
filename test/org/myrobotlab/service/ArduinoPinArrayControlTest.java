package org.myrobotlab.service;

import static org.junit.Assert.fail;

import java.util.List;

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
import org.myrobotlab.service.interfaces.PinDefinition;
import org.slf4j.Logger;

public class ArduinoPinArrayControlTest {

	public final static Logger log = LoggerFactory.getLogger(ArduinoPinArrayControlTest.class);
	
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
	public void testReadInt() {
		fail("Not yet implemented");
	}

	@Test
	public void testReadString() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetModeIntString() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetModeStringString() {
		fail("Not yet implemented");
	}

	@Test
	public void testWriteIntInteger() {
		fail("Not yet implemented");
	}

	@Test
	public void testWriteStringInteger() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetPinList() {
		Arduino arduino = (Arduino)Runtime.start("arduino", "Arduino");
		List<PinDefinition> pins = arduino.getPinList();
		log.info("Arduino %s has %d pins", arduino.getBoardType(), pins.size());
		
		arduino.setBoardMega();
		pins = arduino.getPinList();
		log.info("Arduino %s has %d pins", arduino.getBoardType(), pins.size());
	
		arduino.setBoardMega();
		pins = arduino.getPinList();
		log.info("Arduino %s has %d pins", arduino.getBoardType(), pins.size());
	
		
	}

	@Test
	public void testPinArrayGetPinList() {
		fail("Not yet implemented");
	}

	@Test
	public void testPinArrayReadInt() {
		fail("Not yet implemented");
	}

	@Test
	public void testPinArrayReadString() {
		fail("Not yet implemented");
	}

	@Test
	public void testPinArraySetModeIntString() {
		fail("Not yet implemented");
	}

	@Test
	public void testPinArraySetModeStringString() {
		fail("Not yet implemented");
	}

	@Test
	public void testPinArrayWriteIntInteger() {
		fail("Not yet implemented");
	}

	@Test
	public void testPinArrayWriteStringInteger() {
		fail("Not yet implemented");
	}

	public static void main(String[] args) {
		try {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			ArduinoPinArrayControlTest.setUpBeforeClass();
			ArduinoPinArrayControlTest test = new ArduinoPinArrayControlTest();
			test.setUp();

			// structured testing begins
			test.testGetPinList();

			JUnitCore junit = new JUnitCore();
			Result result = junit.run(ArduinoPinArrayControlTest.class);
			log.info("Result: {}", result);

			// Runtime.dump();

		} catch (Exception e) {
			log.error("test threw", e);
		}
	}
}
