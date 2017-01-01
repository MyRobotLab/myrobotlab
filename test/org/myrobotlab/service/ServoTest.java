package org.myrobotlab.service;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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
		// fail("Not yet implemented");
	}

	@Test
	public void testServo() {
		// fail("Not yet implemented");
	}

	@Test
	public void testAddServoEventListener() {
		// fail("Not yet implemented");
	}

	@Test
	public void testAttach() throws Exception {

		// FIXME - test state change - mrl gets restarted arduino doesn't what happens - how to handle gracefully
		// FIXME - test enabled Events
		// FIXME - make abstract class from interfaces to attempt to do Java 8 interfaces with default
		
		// creation ...
		Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
		Runtime.start("gui", "WebGui");
		Adafruit16CServoDriver afdriver = (Adafruit16CServoDriver) Runtime.start("afdriver", "Adafruit16CServoDriver");
		Servo servo01 = (Servo) Runtime.start("servo01", "Servo");
		Servo servo02 = (Servo) Runtime.start("servo02", "Servo");

		Serial serial = arduino.getSerial();
		// really I have to call refresh first ? :P
		serial.refresh();
		List<String> ports = serial.getPortNames();
		for (String port : ports) {
			log.info(port);
		}

		// User code begin ...
		// should be clear & easy !!

		// microcontroller connect ...
		arduino.connect("COM15");
		// arduino.setDebug(true);

		// ServoControl Methods begin --------------
		// are both these valid ?
		// gut feeling says no - they should not be
		// servo01.attach(arduino, 8);
		servo01.moveTo(30);
		servo01.attach(arduino, 8, 40.0);
		servo01.attach(arduino, 8, 30);
		
		servo02.attach(arduino, 7, 40);
		servo01.eventsEnabled(true);
		// FIXME is attach re-entrant ???
		servo01.broadcastState();
		servo02.broadcastState();

		/*
		servo01.setSpeed(0.02);
		servo02.setSpeed(0.02);
		*/
		
		/*
		servo02.setSpeed(1.0);
		servo01.setSpeed(1.0);
		*/
		
		// sub speed single move
		servo01.moveTo(30);
		servo01.moveTo(31);
		servo01.moveTo(30);
		servo01.moveTo(31);
		servo01.moveTo(30);
		
		servo01.moveTo(130);
		servo02.moveTo(130);
		servo01.moveTo(30);
		servo02.moveTo(30);
		
		arduino.setDebug(true);
		
		// detaching the device
		servo01.detach(arduino); // test servo02.detach(arduino); error ?
		// servo02.detach(afdriver); // TEST CASE - THIS FAILED - THEN RE-ATTACHED DID SPLIT BRAIN FIXME
		servo02.detach(arduino);

		// errors / boundary cases
		// servo01.attach(arduino, 8, 40);
		servo02.attach(arduino, 8, 40); // same pin?
		servo01.attach(arduino, 7, 40); // already attached ?
		
		
		servo01.moveTo(130);
		servo02.moveTo(130);
		servo01.moveTo(30);
		servo02.moveTo(30);
		
		servo01.broadcastState();
		servo02.broadcastState();
		
		servo01.setSpeed(0.2);
		servo02.setSpeed(0.2);
		servo01.moveTo(130);
		servo02.moveTo(130);
		servo01.moveTo(30);
		servo02.moveTo(30);
		servo01.moveTo(130);
		servo01.setSpeed(1.0);
		servo01.moveTo(30);
		servo01.moveTo(130);
		servo01.moveTo(30);
		servo01.moveTo(130);
		
		servo01.detach();

		// no move after detach test
		servo01.moveTo(30);
		servo01.moveTo(130);
		servo01.moveTo(30);
		servo01.moveTo(130);
		
		// move after detach/re-attach
		servo01.attach();
		servo01.moveTo(30);
		servo01.moveTo(130);
		servo01.moveTo(30);
		servo01.moveTo(130);
		
		

		//servo02.attach(afdriver, 8);

		// this is valid
		// FIXME --- THIS IS NOT RE-ENTRANT !!!
		// servo01.attach(arduino, 8, 40); // this attaches the device, calls
										// Servo.attach(8), then Servo.write(40)
		// FIXME --- THIS IS NOT RE-ENTRANT !!!
		//servo02.attach(afdriver, 8, 40);
		// IS IT Equivalent to this ?

		// energize to different pin
		// servo01.attach(7);
		arduino.setDebug(true);
		
		
		servo01.moveTo(130);
		servo01.moveTo(30);
		// servo02.attach(7);

		// servo move methods
		servo02.moveTo(30);
		servo02.moveTo(130);
		
		servo02.detach();
		servo02.moveTo(30);
		servo02.moveTo(130);
		servo02.moveTo(30);
		servo02.moveTo(130);
	
		servo02.attach();
		servo02.moveTo(30);
		servo02.moveTo(130);
		servo02.moveTo(30);
		servo02.moveTo(130);
		servo02.moveTo(30);
		servo02.moveTo(130);

		/*
		servo01.moveTo(30);
		servo02.moveTo(30);
		servo01.moveTo(130);
		servo02.moveTo(130);
		servo01.moveTo(30);
		servo02.moveTo(30);
		servo01.moveTo(130);
		servo02.moveTo(130);
		*/

		// servo detach
		servo01.detach();
		servo02.detach();

		// should re-attach
		// with the same pin & pos
		servo01.attach();
		servo02.attach();

	
		
		servo02.moveTo(30);
		servo02.moveTo(130);
		servo02.moveTo(30);
		servo02.moveTo(130);

	}

	@Test
	public void testDetach() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetController() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetLastActivityTime() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetMax() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetMaxInput() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetMaxOutput() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetMin() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetMinInput() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetMinOutput() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetPos() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetRest() {
		// fail("Not yet implemented");
	}

	@Test
	public void testIsAttached() {
		// fail("Not yet implemented");
	}

	@Test
	public void testIsInverted() {
		// fail("Not yet implemented");
	}

	@Test
	public void testIsSweeping() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMap() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMoveTo() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishServoEvent() {
		// fail("Not yet implemented");
	}

	@Test
	public void testRefreshControllers() {
		// fail("Not yet implemented");
	}

	@Test
	public void testRest() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetController() {
		// fail("Not yet implemented");
	}

	@Test
	public void testEventsEnabled() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetInverted() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetMinMax() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetRest() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetSpeed() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetSpeedControlOnUC() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetSweepDelay() {
		// fail("Not yet implemented");
	}

	@Test
	public void testStop() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSweep() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSweepIntInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSweepIntIntIntInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSweepIntIntIntIntBoolean() {
		// fail("Not yet implemented");
	}

	@Test
	public void testWriteMicroseconds() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMain() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetMetaData() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetDeviceType() {
		// fail("Not yet implemented");
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

			/* START JUNIT
			JUnitCore junit = new JUnitCore();
			Result result = junit.run(ServoTest.class);
			log.info("Result: {}", result);
			*/

			// Runtime.dump();

		} catch (Exception e) {
			log.error("test threw", e);
		}
	}

}
