package org.myrobotlab.service;

import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.codec.serial.ArduinoMsgCodec;
import org.myrobotlab.codec.serial.Codec;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class UltrasonicSensorTest {
	
	public final static Logger log = LoggerFactory.getLogger(UltrasonicSensor.class);
	static UltrasonicSensor sensor = null;


	static Arduino arduino = null;
	static Serial serial = null;
	static TestCatcher catcher = null;

	static VirtualDevice virtual = null;
	static Python logic = null;
	static String vport = "vport";
	static Serial uart = null;

	int servoPin = 9;

	static ArduinoMsgCodec codec = new ArduinoMsgCodec();

	// FIXME - test for re-entrant !!!!
	// FIXME - single switch for virtual versus "real" hardware

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		log.info("setUpBeforeClass");

		//Runtime.start("gui", "GUIService");
		sensor = (UltrasonicSensor) Runtime.start("arduino", "UltrasonicSensor");
		arduino = sensor.getArduino();
		serial = arduino.getSerial();

		catcher = (TestCatcher) Runtime.start("catcher", "TestCatcher");
		virtual = (VirtualDevice) Runtime.start("virtual", "VirtualDevice");
		virtual.createVirtualArduino(vport);
		logic = virtual.getLogic();

		catcher.subscribe(arduino, "publishError", "onError");

		uart = virtual.getUART();
		uart.setCodec("arduino");
		Codec codec = uart.getRXCodec();
		codec.setTimeout(1000);
		uart.setTimeout(100); // don't want to hang when decoding results...

		arduino.setBoard(Arduino.BOARD_TYPE_ATMEGA2560);
		arduino.connect(vport);

		Service.sleep(500);
		// nice to be able to check messages
		// uart.addByteListener(catcher);
		log.info("here");

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
	public final void testGetCategories() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testGetDescription() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testStartService() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testGetPeers() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testMain() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testUltrasonicSensor() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testAddRangeListener() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testAttach() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testGetArduino() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testGetEchoPin() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testGetLastRange() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testGetTriggerPin() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testOnRange() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testPing() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testPingInt() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testPublishRange() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testRange() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testSetType() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testStartRanging() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testStartRangingInt() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testStopRanging() {
		// fail("Not yet implemented");
	}
	
	// TODO - Virtual Serial test - do a record of tx & rx on a real sensor
	// then send the data - IT MUST BE INTERLEAVED
	@Test
	public final void test() throws IOException {
		
		
			// FIXME - there has to be a properties method to configure
			// localized
			// testing
			int triggerPin = 7;
			int echoPin = 8;

			String port = "COM15";

			sensor.attach(port, triggerPin, echoPin);
			// arduino.re
			// TODO - VIRTUAL NULL MODEM WITH TEST DATA !!!!
			// RECORD FROM ACTUAL SENSOR !!!

			// sensor.arduino.setLoadTimingEnabled(true);
			TestCatcher catcher = (TestCatcher)Runtime.start("catcher", "TestCatcher");
			sensor.addRangeListener(catcher);

			sensor.startRanging();
			log.info("here");
			sensor.stopRanging();

			uart.stopRecording();

			arduino.setLoadTimingEnabled(true);
			arduino.setLoadTimingEnabled(false);

	
			sensor.startRanging();
			log.info("here");
			
			sensor.stopRanging();

			sensor.startRanging(5);
			sensor.startRanging(10);

			sensor.startRanging();

			sensor.stopRanging();
		
	}


}
