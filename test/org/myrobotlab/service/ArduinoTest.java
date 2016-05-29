package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.codec.serial.ArduinoMsgCodec;
import org.myrobotlab.codec.serial.Codec;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino.Sketch;
import org.myrobotlab.service.data.Pin;
import org.slf4j.Logger;

/**
 * 
 * @author GroG
 *
 */

public class ArduinoTest {

	public final static Logger log = LoggerFactory.getLogger(ArduinoTest.class);

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
		log.info("setUpBeforeClass");

		// Runtime.start("gui", "GUIService");

		arduino = (Arduino) Runtime.start("arduino", "Arduino");
		serial = arduino.getSerial();

		catcher = (TestCatcher) Runtime.start("catcher", "TestCatcher");
		virtual = (VirtualDevice) Runtime.start("virtual", "VirtualDevice");
		virtual.createVirtualArduino(vport);
		logic = virtual.getLogic();

		catcher.subscribe(arduino.getName(), "publishError");

		uart = virtual.getUart(vport);
		uart.setCodec("arduino");
		Codec codec = uart.getRXCodec();
		codec.setTimeout(1000);
		uart.setTimeout(100); // don't want to hang when decoding results...

		arduino.setBoardMega();
		arduino.connect(vport);

		// serial.removeListener("onByte", serviceName, inMethod);

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
		catcher.clear();
		catcher.isLocal = true;

		serial.clear();
		serial.setTimeout(100);
		
		uart.clear();
		uart.setTimeout(100);

		/*
		 * arduino.clearLastError(); arduino.hasError();
		 */
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testReleaseService() {
		// fail("Not yet implemented");
	}

	@Test
	public void testStartService() {
		// fail("Not yet implemented");
	}

	@Test
	public void testStopService() {
		// fail("Not yet implemented");
	}

	/*
	 * not a good test
	 * 
	 * @Test public void testArduino() { // fail("Not yet implemented"); }
	 */

	@Test
	public void testAddCustomMsgListener() {
		// fail("Not yet implemented");
	}

	@Test
	public void testAnalogReadPollingStart() {
		// fail("Not yet implemented");
	}

	@Test
	public void testAnalogReadPollingStop() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testAnalogWrite() throws InterruptedException, IOException {
		log.info("testAnalogWrite");

		arduino.analogWrite(10, 0);
		String decoded = uart.decode();
		assertEquals("analogWrite/10/0\n", decoded);

		arduino.analogWrite(10, 127);
		decoded = uart.decode();
		assertEquals("analogWrite/10/127\n", decoded);

		arduino.analogWrite(10, 128);
		decoded = uart.decode();
		assertEquals("analogWrite/10/128\n", decoded);

		arduino.analogWrite(10, 255);
		decoded = uart.decode();
		assertEquals("analogWrite/10/255\n", decoded);

		arduino.error("test");

		log.info(String.format("errors %b", catcher.hasError()));

		// Runtime.clearErrors();
		/*
		 * if (Runtime.hasErrors()){ ArrayList<Status> errors =
		 * Runtime.getErrors(); //throw new IOException("problem with errors");
		 * }
		 */

		/*
		 * uart.decode(); codec.decode(newByte)
		 * 
		 * catcher.checkMsg("bla");
		 */
	}

	@Test
	public final void testConnect() {
		log.info("testConnect - begin");
		arduino.disconnect();
		arduino.connect(vport);
		assertTrue(arduino.isConnected());
		assertEquals(ArduinoMsgCodec.MRLCOMM_VERSION, arduino.getVersion().intValue());
		log.info("testConnect - end");
	}

	@Test
	public void testConnectVirtualUART() {
		// fail("Not yet implemented");
	}

	@Test
	public void testCreatePinList() {
		// fail("Not yet implemented");
	}

	@Test
	public void testDigitalReadPollingStart() {
		// fail("Not yet implemented");
	}

	@Test
	public void testDigitalReadPollingStop() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testDigitalWrite() {
		log.info("testDigitalWrite");
		arduino.digitalWrite(10, 1);
		assertEquals("digitalWrite/10/1\n", uart.decode());
		arduino.digitalWrite(10, 0);
		assertEquals("digitalWrite/10/0\n", uart.decode());
		// arduino.digitalWrite(10, 255);
		// assertEquals("digitalWrite/10/0", uart.decode());
	}

	@Test
	public final void testDisconnect() {
		log.info("testDisconnect");
		arduino.disconnect();
		assertTrue(!arduino.isConnected());
		arduino.digitalWrite(10, 1);
		assertEquals(0, uart.available());
		arduino.connect(vport);
		assertTrue(arduino.isConnected());
		uart.clear();
		arduino.digitalWrite(10, 1);
		assertEquals("digitalWrite/10/1\n", uart.decode());
	}

	@Test
	public void testGetBoardType() {
		// arduino.setBoardMega()
	}

	@Test
	public void testGetPinList() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetSerial() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testGetSketch() {
		log.info("testGetSketch");
		Sketch sketch = arduino.getSketch();
		assertNotNull(sketch);
		assertTrue(sketch.data.length() > 0);
		arduino.setSketch(null);
		assertNull(arduino.getSketch());
		arduino.setSketch(sketch);
		assertEquals(sketch, arduino.getSketch());
	}

	@Test
	public final void testGetVersion() {
		log.info("testGetVersion");
		assertEquals(ArduinoMsgCodec.MRLCOMM_VERSION, arduino.getVersion().intValue());
	}

	@Test
	public void testIsConnected() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMotorAttach() {
		// fail("Not yet implemented");
	}

	@Test
	public void testAttach() {
		// fail("Not yet implemented");
	}

	@Test
	public void testDetach() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMotorDetach() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMotorMove() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMotorMoveTo() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMotorStop() {
		// fail("Not yet implemented");
	}

	@Test
	public void testOnByte() {
		// fail("Not yet implemented");
	}

	@Test
	public void testOnConnect() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetPortName() {
		// fail("Not yet implemented");
	}

	@Test
	public void testOnCustomMsg() {
		// fail("Not yet implemented");
	}

	@Test
	public void testOnDisconnect() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testPinModeIntString() {
		log.info("testPinModeIntString");
		arduino.pinMode(8, "OUTPUT");
		assertEquals("pinMode/8/1\n", uart.decode());
	}

	@Test
	public final void testPinModeIntegerInteger() {
		log.info("testPinModeIntegerInteger");
		arduino.pinMode(8, Arduino.OUTPUT);
		assertEquals("pinMode/8/1\n", uart.decode());
	}

	@Test
	public void testPublishCustomMsg() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishLoadTimingEvent() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishMRLCommError() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishPin() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishServoEvent() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishTrigger() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishVersion() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPulseInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPulseIntInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPulseIntIntInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPulseIntIntIntInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPulseStop() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishPulse() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishPulseStop() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSendMsg() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSensorAttach() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSensorPollingStart() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSensorPollingStop() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testServoAttachServoInteger() {
		log.info("testServoAttachServoInteger");
		Servo servo = (Servo) Runtime.start("servo", "Servo");

		// NOT THE WAY TO ATTACH SERVOS !!
		// isAttached will not get set
		// dont know a good fix - asside from not using it !
		// arduino.servoAttach(servo, servoPin);
		// re-entrant test
		// arduino.servoAttach(servo, servoPin);

		// common way
		servo.attach(arduino, servoPin);

		// another way
		// servo.setPin(servoPin);
		// servo.setController(arduino);

		assertTrue(servo.isAttached());

		// re-entrant test
		servo.attach(arduino, servoPin);

		assertTrue(servo.isAttached());
		assertEquals(servoPin, servo.getPin().intValue());
		assertEquals(arduino.getName(), servo.getControllerName());

		assertEquals("servoAttach/7/9/5/115/101/114/118/111\n", uart.decode());
		servo.moveTo(0);
		assertEquals("servoWrite/7/0\n", uart.decode());
		servo.moveTo(90);
		assertEquals("servoWrite/7/90\n", uart.decode());
		servo.moveTo(180);
		assertEquals("servoWrite/7/180\n", uart.decode());
		servo.moveTo(0);
		assertEquals("servoWrite/7/0\n", uart.decode());

		// detach
		servo.detach();
		assertEquals("servoDetach/7/0\n", uart.decode());

		servo.moveTo(10);
		String shouldBeNull = uart.decode();
		assertNull(shouldBeNull);

		// re-attach
		servo.attach();
		assertEquals("servoAttach/7/9/5/115/101/114/118/111\n", uart.decode());
		assertTrue(servo.isAttached());
		assertEquals(servoPin, servo.getPin().intValue());
		assertEquals(arduino.getName(), servo.getControllerName());

		servo.moveTo(90);
		assertEquals("servoWrite/7/90\n", uart.decode());

		servo.releaseService();
	}

	@Test
	public void testServoAttachServo() {
		// fail("Not yet implemented");
	}

	@Test
	public void testServoDetach() {
		// fail("Not yet implemented");
	}

	@Test
	public void testServoSweepStart() {
		// fail("Not yet implemented");
	}

	@Test
	public void testServoSweepStop() {
		// fail("Not yet implemented");
	}

	@Test
	public void testServoWrite() {
		// fail("Not yet implemented");
	}

	@Test
	public void testServoWriteMicroseconds() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetBoard() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetBoardMega() {
		log.info("testSetBoardMega");
		String boardType = arduino.getBoardType();

		arduino.setBoardMega();

		assertEquals(Arduino.BOARD_TYPE_MEGA, arduino.getBoardType());

		List<Pin> pins = arduino.getPinList();
		assertEquals(70, pins.size());

		arduino.setBoard(boardType);
	}

	@Test
	public void testSetBoardUno() {
		log.info("testSetBoardUno");
		String boardType = arduino.getBoardType();

		arduino.setBoardUno();

		assertEquals(Arduino.BOARD_TYPE_UNO, arduino.getBoardType());

		List<Pin> pins = arduino.getPinList();
		assertEquals(20, pins.size());

		arduino.setBoard(boardType);
	}

	@Test
	public void testSetDebounce() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetDigitalTriggerOnly() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetLoadTimingEnabled() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetPWMFrequency() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetSampleRate() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetSerialRate() {
		// fail("Not yet implemented");
	}

	@Test
	public void testServoEventsEnabled() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetServoSpeed() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetSketch() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetTriggerIntInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetTriggerIntIntInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSoftReset() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishSensorData() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMotorReset() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMain() {
		// fail("Not yet implemented");
	}

	@Test
	public void testUpdate() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetDataSinkType() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetSensorType() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetSensorConfig() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetMetaData() {
		// fail("Not yet implemented");
	}

	public static void main(String[] args) {
		try {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			ArduinoTest.setUpBeforeClass();
			ArduinoTest test = new ArduinoTest();
			test.testConnect();
			
			JUnitCore junit = new JUnitCore();
			Result result = junit.run(ArduinoTest.class);
			log.info("Result was: {}", result);
			// WebGui gui = (WebGui) Runtime.start("webgui", "WebGui");
			// ServiceInterface gui = Runtime.start("gui", "GUIService");

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
