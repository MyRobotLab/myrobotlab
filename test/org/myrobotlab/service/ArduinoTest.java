package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.codec.ArduinoMsgCodec;
import org.myrobotlab.codec.Codec;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino.Sketch;
import org.slf4j.Logger;

/**
 * 
 * @author GroG
 *
 */
public class ArduinoTest {

	public final static Logger log = LoggerFactory.getLogger(SerialTest.class);

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
		arduino = (Arduino) Runtime.start("arduino", "Arduino");
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
		catcher.clear();
		catcher.isLocal = true;

		uart.clear();
		uart.setTimeout(100);

		serial.clear();
		serial.setTimeout(100);

		/*
		 * arduino.clearLastError(); arduino.hasError();
		 */
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void testReleaseService() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testStartService() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testStopService() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testTest() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetPeers() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testArduino() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testAddCustomMsgListener() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testAnalogReadPollingStart() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testAnalogReadPollingStop() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testAnalogWrite() throws InterruptedException, IOException {
		log.info("testConnect - begin");

		serial.clear();

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
		arduino.connect(vport);
		assertTrue(arduino.isConnected());
		assertEquals(ArduinoMsgCodec.MRLCOMM_VERSION, arduino.getVersion().intValue());
		log.info("testConnect - end");
	}

	@Test
	public final void testCreatePinList() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testDigitalReadPollingStart() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testDigitalReadPollingStop() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testDigitalWrite() {
		arduino.digitalWrite(10, 1);
		assertEquals("digitalWrite/10/1\n", uart.decode());
		arduino.digitalWrite(10, 0);
		assertEquals("digitalWrite/10/0\n", uart.decode());
		// arduino.digitalWrite(10, 255);
		// assertEquals("digitalWrite/10/0", uart.decode());
	}

	@Test
	public final void testDisconnect() {
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
	public final void testGetBoardType() {
		//arduino.setBoardMega();
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetPinList() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetSerial() {
		assertNotNull(arduino.getSerial());
	}

	@Test
	public final void testGetSketch() {
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
		assertEquals(ArduinoMsgCodec.MRLCOMM_VERSION, arduino.getVersion().intValue());
	}

	@Test
	public final void testIsConnected() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testMotorAttachStringIntegerInteger() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testMotorAttachStringStringIntegerInteger() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testMotorAttachStringStringIntegerIntegerInteger() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testMotorDetach() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testMotorMove() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testMotorMoveTo() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testOnByte() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testOnCustomMsg() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPinModeIntString() {
		arduino.pinMode(8, "OUTPUT");
		assertEquals("pinMode/8/1\n",uart.decode());
	}

	@Test
	public final void testPinModeIntegerInteger() {
		arduino.pinMode(8, Arduino.OUTPUT);
		assertEquals("pinMode/8/1\n",uart.decode());
	}

	@Test
	public final void testPublishCustomMsg() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPublishLoadTimingEvent() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPublishMRLCommError() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPublishPin() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPublishPulse() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPublishServoEvent() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPublishSesorData() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPublishStepperEvent() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPublishTrigger() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPublishVersion() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPulseInIntInt() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPulseInIntIntIntInt() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPulseInIntIntIntString() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPulseInIntIntInteger() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSendMsg() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSensorAttachString() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSensorAttachUltrasonicSensor() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSensorPollingStart() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSensorPollingStop() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testServoAttachServoInteger() {
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
		
		//re-entrant test
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
	public final void testServoAttachStringInteger() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testServoDetachServo() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testServoDetachString() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testServoSweepStart() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testServoSweepStop() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testServoWrite() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testServoWriteMicroseconds() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetBoard() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetDebounce() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetDigitalTriggerOnly() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetLoadTimingEnabled() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetPWMFrequency() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetSampleRate() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetSerialRate() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetServoEventsEnabled() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetServoSpeed() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetSketch() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetStepperSpeed() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetTriggerIntInt() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetTriggerIntIntInt() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSoftReset() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testStepperAttachStepperControl() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testStepperAttachString() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testStepperDetach() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testStepperMove() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testStepperReset() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testStepperStepStringInteger() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testStepperStepStringIntegerInteger() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testStepperStop() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testConnectVirtualUART() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testOnConnect() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testOnDisconnect() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testMain() {
		// fail("Not yet implemented"); // TODO
	}

}
