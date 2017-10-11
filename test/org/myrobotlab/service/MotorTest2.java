package org.myrobotlab.service;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

// TODO: This test takes too long to run, ignoring it from the build.
@Ignore
public class MotorTest2 {

	public final static Logger log = LoggerFactory.getLogger(MotorTest2.class);

	static MotorDualPwm motor01 = null;
	static Arduino arduino = null;

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
	public void testMotor() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetController() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetPowerLevel() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetPowerOutput() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetPowerMap() {
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
	public void testLock() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMapEncoder() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMapPower() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMove() throws Exception {
		Runtime.start("webgui", "WebGui");
		arduino = (Arduino)Runtime.start("arduino", "Arduino");
		motor01 = (MotorDualPwm)Runtime.start("motor01", "MotorDualPwm");
		motor01.setPwmPins(3, 4);
		motor01.attachMotorController(arduino);
		
		arduino.connect("COM5"); 

		
		motor01.move(0.3);
		motor01.move(0.1);
		motor01.move(1.0);
		motor01.move(5.0);

		motor01.move(-0.1);
		motor01.move(-0.2);
		motor01.move(0.0);
		motor01.stop();
		
		motor01.save();
		motor01.load();
		
		motor01.detachMotorController(arduino);
	}

	@Test
	public void testMoveToIntDouble() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMoveToInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetController() {
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
	public void testSetPowerLevel() {
		// fail("Not yet implemented");
	}

	@Test
	public void testStop() {
		// fail("Not yet implemented");
	}

	@Test
	public void testStopAndLock() {
		// fail("Not yet implemented");
	}

	@Test
	public void testUnlock() {
		// fail("Not yet implemented");
	}

	@Test
	public void testUpdatePosition() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetMetaData() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetTargetPos() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMain() {
		// fail("Not yet implemented");
	}

	@Test
	public void testOnSensorData() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPulse() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetEncoder() {
		// fail("Not yet implemented");
	}

	@Test
	public void testDetach() {
		// fail("Not yet implemented");
	}

	@Test
	public void testAttach() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetPwmPins() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetPwrDirPins() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetConfig() {
		// fail("Not yet implemented");
	}

	public static void main(String[] args) {
		try {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			MotorTest2.setUpBeforeClass();
			MotorTest2 test = new MotorTest2();
			test.testMove();

			JUnitCore junit = new JUnitCore();
			Result result = junit.run(MotorTest2.class);
			log.info("Result was: {}", result);
			// WebGui gui = (WebGui) Runtime.start("webgui", "WebGui");
			// ServiceInterface gui = Runtime.start("gui", "SwingGui");

			Runtime.dump();

			log.info("here");
			// serial.removeByteListener(gui.getName());
			// uart.removeByteListener(gui.getName());

			Runtime.dump();

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

}
