package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.Msg;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino.Sketch;
import org.myrobotlab.service.VirtualArduino.MrlServo;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.slf4j.Logger;

/**
 * 
 * @author GroG
 *
 */

public class ArduinoTest implements PinArrayListener {

	public final static Logger log = LoggerFactory.getLogger(ArduinoTest.class);

	static boolean useVirtualHardware = true;
	static String port = "COM5";

	// things to test
	static Arduino arduino = null;
	static Serial serial = null;

	// virtual hardware
	static VirtualArduino virtual = null;
	static Serial uart = null;

	int servoPin = 7;
	String enablePin = "A1";
	int writeAddress = 6;
	
	Map<Integer, PinData> pinData = new HashMap<Integer, PinData>();

	// FIXME - test for re-entrant !!!!
	// FIXME - single switch for virtual versus "real" hardware

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		log.info("setUpBeforeClass");

		arduino = (Arduino) Runtime.start("arduino", "Arduino");
		serial = arduino.getSerial();

		// FIXME - needs a seemless switch
		if (useVirtualHardware) {
			virtual = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
			uart = virtual.getSerial();
			uart.setTimeout(100); // don't want to hang when decoding results...
			virtual.connect(port);
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {

		/**
		 * Arduino's expected state before each test is
		 * 'connected' with no devices, no pins enabled
		 */

		arduino.connect(port);
		arduino.reset();

		serial.clear();
		serial.setTimeout(100);

		uart.clear();
		uart.setTimeout(100);
		
		pinData.clear();
	}

	@Test
	public void testReleaseService() {
		arduino.releaseService();
		// better re-start it
		arduino = (Arduino)Runtime.start("arduino", "Arduino");
	}


	@Test
	public final void testAnalogWrite() throws InterruptedException, IOException {
		log.info("testAnalogWrite");

		arduino.analogWrite(10, 0);
		assertVirtualPinValue(10, 0);

		arduino.analogWrite(10, 127);
		assertVirtualPinValue(10, 127);

		arduino.analogWrite(10, 128);
		assertVirtualPinValue(10, 128);

		arduino.analogWrite(10, 255);
		assertVirtualPinValue(10, 255);
		
		arduino.error("test");
	}

	private void assertVirtualPinValue(int address, int value) {
		if (virtual != null){
			assertTrue(virtual.readBlocking(address, 50) == value);
			virtual.clearPinQueue(address);
		}
	}

	@Test
	public void testAttachPinArrayListener() {
		// fail("Not yet implemented");
	}

	@Test
	public void testAttachPinListenerInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testAttachStringInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testConnectArduinoString() {
		// fail("Not yet implemented");
	}

	@Test
	public void testConnectString() {
		for (int i = 0; i < 20; ++i) {
			// arduino.connect(port);
			// arduino.enableAck(true);
			arduino.echo(90.57F, 129, 30.123F);
			/*
			arduino.echo(30003030L + i);
			arduino.echo(2L);
			arduino.echo(-1L);
			*/
			// arduino.disconnect();
		}
		
		log.info("here");

	}

	@Test
	public void testConnectStringIntIntIntInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testControllerAttach() {
		// fail("Not yet implemented");
	}

	@Test
	public void testCreatePinList() {
		// fail("Not yet implemented");
	}

	@Test
	public void testCustomMsg() {
		// fail("Not yet implemented");
	}

	@Test
	public void testDeviceDetach() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testDigitalWrite() {
		log.info("testDigitalWrite");
		arduino.digitalWrite(10, 1);
		// assertEquals("digitalWrite/10/1\n", uart.decode());
		arduino.digitalWrite(10, 0);
		// assertEquals("digitalWrite/10/0\n", uart.decode());
		// arduino.digitalWrite(10, 255);
		// assertEquals("digitalWrite/10/0", uart.decode());
	}

	@Test
	public void testDisablePin() {
		// fail("Not yet implemented");
	}

	@Test
	public void testDisablePins() {
		// enable 2 pins
		
		// verify we're not getting data
	}

	@Test
	public final void testDisconnect() throws IOException {
		log.info("testDisconnect");
		// shutdown mrlcomm

		// disconnect
		arduino.disconnect();

		// clear
		serial.clear();
		uart.clear();

		// test disconnected
		assertTrue(!arduino.isConnected());

		// test no data - no exception ?
		arduino.digitalWrite(10, 1);

		// reconnect
		arduino.connect(port);

		// test we are connected
		assertTrue(arduino.isConnected());

		// assert basic re-connect worky
		arduino.digitalWrite(10, 1);

	}

	public void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testEnableBoardStatus() {

		org.myrobotlab.service.Test test = (org.myrobotlab.service.Test) Runtime.start("test", "Test");
		test.subscribe(arduino.getName(), "publishBoardStatus");
		arduino.enableBoardStatus(true);
		// FIXME notify with timeout

		arduino.enableBoardStatus(false);
	}

	@Test
	public void testEnableBoardStatusInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testEnabledHeartbeat() {
		// fail("Not yet implemented");
	}

	@Test
	public void testEnablePinInt() {
		// set board type
		arduino.enablePin(enablePin);
		arduino.attach(this);
		sleep(50);
		assertTrue(pinData.containsKey(arduino.getAddress(enablePin)));
		arduino.disablePin(enablePin);
	}

	@Test
	public void testGetBoardInfo() {
		arduino.connect(port);
		BoardInfo boardInfo = arduino.getBoardInfo();
		assertTrue(boardInfo.isValid());
		assertTrue(boardInfo.getVersion().intValue() == Msg.MRLCOMM_VERSION);
	}

	@Test
	public void testGetController() {
		arduino.connect(port);
		DeviceController d = arduino.getController();
		assertNotNull(d);
	}

	@Test
	public void testGetDeviceIdDeviceControl() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetDeviceIdString() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetMrlPinType() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetPinList() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetPortName() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetSerial() {
		// fail("Not yet implemented");
	}

	@Test
	public void testHeartbeat() {
		// fail("Not yet implemented");
	}

	@Test
	public void testI2cRead() {
		// fail("Not yet implemented");
	}

	@Test
	public void testI2cReturnData() {
		// fail("Not yet implemented");
	}

	@Test
	public void testI2cWrite() {
		// fail("Not yet implemented");
	}

	@Test
	public void testI2cWriteRead() {
		// fail("Not yet implemented");
	}

	@Test
	public void testIsAttached() {
		// fail("Not yet implemented");
	}

	@Test
	public void testIsConnected() {
		// fail("Not yet implemented");
	}

	@Test
	public void testIsRecording() {
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
	public void testMotorReset() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMotorStop() {
		// fail("Not yet implemented");
	}

	@Test
	public void testMsgRoute() {
		// fail("Not yet implemented");
	}

	@Test
	public void testNeoPixelSetAnimation() {
		// fail("Not yet implemented");
	}

	@Test
	public void testNeoPixelWriteMatrix() {
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
	public void testOnCustomMsg() {
		// fail("Not yet implemented");
	}

	@Test
	public void testOnDisconnect() {
		// fail("Not yet implemented");
	}

	@Test
	public void testOnSensorData() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPinModeIntInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPinModeStringString() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPinNameToAddress() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishAttachedDevice() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishBoardInfo() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishBoardStatus() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishCustomMsg() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishDebug() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishMessageAck() {
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
	public void testPublishPinArray() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishPinDefinition() {
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
	public void testPublishSensorData() {
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
	public void testReadInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testReadString() {
		// fail("Not yet implemented");
	}

	@Test
	public void testRecord() {
		// fail("Not yet implemented");
	}

	@Test
	public void testRefresh() {
		// fail("Not yet implemented");
	}

	@Test
	public void testReleaseI2cDevice() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSendMsgIntIntArray() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSendMsgIntListOfInteger() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSendMsgMrlMsg() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSensorActivate() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSensorDeactivate() {
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
	public void testServoAttach() {
		// fail("Not yet implemented");
	}

	@Test
	public void testServoDetach() {
		// fail("Not yet implemented");
	}

	@Test
	public void testServoSetMaxVelocity() {
		// fail("Not yet implemented");
	}

	@Test
	public void testServoSetVelocity() {
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
		arduino.connect(port);
		arduino.write(writeAddress, 1);
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
	public void testSetBoardMegaADK() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetController() {
		// fail("Not yet implemented");
	}

	@Test
	public void testUnsetController() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetDebounce() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetDebug() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetDigitalTriggerOnly() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetPWMFrequency() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetSerialRate() {
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
	public void testReset() {
		// fail("Not yet implemented");
	}

	@Test
	public void testStopRecording() {
		// fail("Not yet implemented");
	}

	@Test
	public void testUploadSketchString() {
		// fail("Not yet implemented");
	}

	@Test
	public void testUploadSketchStringString() {
		// fail("Not yet implemented");
	}

	@Test
	public void testUploadSketchStringStringString() {
		// fail("Not yet implemented");
	}

	@Test
	public void testWrite() {
		// fail("Not yet implemented");
	}

	//////// end generated ///////////////////////

	@Test
	public final void testConnect() throws IOException {
		log.info("testConnect - begin");
		arduino.disconnect();
		arduino.connect(port);
		sleep(10);
		assertTrue(arduino.isConnected());
		assertEquals(Msg.MRLCOMM_VERSION, arduino.getBoardInfo().getVersion().intValue());
		log.info("testConnect - end");
	}

	@Test
	public void testGetBoardType() {
		// arduino.setBoardMega()
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
		arduino.connect(port);
		assertEquals(Msg.MRLCOMM_VERSION, arduino.getBoardInfo().getVersion().intValue());
	}

	@Test
	public final void testPinModeIntString() {
		log.info("testPinModeIntString");
		arduino.pinMode(8, "OUTPUT");
		// assertEquals("pinMode/8/1\n", uart.decode());
	}

	@Test
	public final void testPinModeIntegerInteger() {
		log.info("testPinModeIntegerInteger");
		arduino.pinMode(8, Arduino.OUTPUT);
		// assertEquals("pinMode/8/1\n", uart.decode());
	}

	@Test
	public final void testServoAttachServoInteger() throws Exception {
		log.info("testServoAttachServoInteger");
		Servo servo = null;

		// make sure we're connected
		arduino.connect(port);
		assertTrue(arduino.isConnected());
		assertTrue(arduino.getBoardInfo().isValid());

		// reentrancy make code strong !
		// for (int i = 0; i < 3; ++i) {

		// create a servo
		servo = (Servo) Runtime.start("servo", "Servo");

		// attach it
		servo.attach(arduino, servoPin);
		
		// verify its attached
		assertTrue(servo.isAttached());
		assertTrue(servo.isAttached(arduino));
		assertTrue(arduino.getDeviceNames().contains(servo.getName()));
		
		// detach it
		arduino.detach(servo);
		
		// verify its detached
		assertFalse(arduino.getDeviceNames().contains(servo.getName()));
		assertFalse(servo.isAttached());
		assertFalse(servo.isAttached(arduino));
		
		// attach it the other way
		arduino.attach(servo, servoPin);

		// verify its attached
		assertTrue(servo.isAttached());
		assertTrue(servo.isAttached(arduino));
		assertTrue(arduino.getDeviceNames().contains(servo.getName()));
		
		// servo should have the correct pin
		assertTrue(servoPin == servo.getPin());

		// get its device id
		int deviceId = arduino.getDeviceId(servo.getName());

		// get mrlcom's device id
		// virtualized tests
		MrlServo mrlServo = null;
		if (virtual != null) {
			Thread.sleep(100);
			mrlServo = (MrlServo) virtual.getDevice(deviceId);
			// verify
			assertTrue(deviceId == mrlServo.id);
		}

		// can we attach to a different pin?
		servo.attach(servoPin + 1);		
		if (virtual != null) {
			sleep(100);
			assertTrue(mrlServo.pin == servoPin + 1);
			assertTrue(mrlServo.pin == servo.getPin());
		}

		int velocity = 50;
		// degree per second
		servo.setVelocity(velocity);
		if (virtual != null) {
			sleep(100);
			assertTrue(mrlServo.velocity == velocity);
		}

		// attach to the correct pin again
		servo.attach(servoPin);
		servo.moveTo(30);
		servo.moveTo(130);
		servo.moveTo(30);
		// assertEquals(virtual.servoMoveTo(130));
		servo.rest();

		assertTrue(servo.isAttached());
		assertEquals(arduino.getName(), servo.getController().getName());

		servo.moveTo(0);
		// assertEquals(virtual.servoMoveTo(0));
		servo.moveTo(90);
		// assertEquals("servoWrite/7/90\n", uart.decode());
		servo.moveTo(180);
		// assertEquals("servoWrite/7/180\n", uart.decode());
		servo.moveTo(0);
		// assertEquals("servoWrite/7/0\n", uart.decode());

		// detach
		servo.detach();
		// assertEquals("servoDetach/7/0\n", uart.decode());

		servo.moveTo(10);

		// re-attach
		servo.attach();
		// assertEquals("servoAttach/7/9/5/115/101/114/118/111\n",
		// uart.decode());
		assertTrue(servo.isAttached());
		// // assertEquals(servoPin, servo.getPin().intValue());
		assertEquals(arduino.getName(), servo.getController().getName());

		servo.moveTo(90);
		// assertEquals("servoWrite/7/90\n", uart.decode());

		arduino.enableBoardStatus(true);

		servo.startService();

		servo.moveTo(90);
	
		
		// when we release a service - it should 
		// notify and process releasing itself from attached 
		// services
		servo.releaseService();
		assertFalse(arduino.getDeviceNames().contains(servo.getName()));
		assertFalse(servo.isAttached());
		assertFalse(servo.isAttached(arduino));
	
	}

	@Test
	public void testSetBoardMega() {
		log.info("testSetBoardMega");
		String boardType = arduino.getBoardType();

		arduino.setBoardMega();

		assertEquals(Arduino.BOARD_TYPE_MEGA, arduino.getBoardType());

		List<PinDefinition> pins = arduino.getPinList();
		assertEquals(70, pins.size());

		arduino.setBoard(boardType);
	}

	@Test
	public void testSetBoardUno() {
		log.info("testSetBoardUno");
		String boardType = arduino.getBoardType();

		arduino.setBoardUno();

		assertEquals(Arduino.BOARD_TYPE_UNO, arduino.getBoardType());

		List<PinDefinition> pins = arduino.getPinList();
		assertEquals(20, pins.size());

		arduino.setBoard(boardType);
	}

	public static class JUnitListener extends RunListener {

		public void testAssumptionFailure(Failure failure) {
			log.info("testAssumptionFailure");
		}

		public void testFailure(Failure failure) {
			log.info("testFailure");
		}

		public void testFinished(Description description) {
			log.info("testFinished");
		}

		public void testIgnored(Description description) {
			log.info("testIgnored");
		}

		public void testRunFinished(Result result) {
			log.info("testRunFinished");
		}

		public void testRunStarted(Description description) {
			log.info("testRunStarted");
		}

		public void testStarted(Description description) {
			log.info("testStarted");
		}
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	@Override
	public String getName() {
		return "arduinoTest";
	}

	@Override
	public void onPinArray(PinData[] pindata) {
		log.info("onPinArray size {}", pindata.length);
		for (int i = 0; i < pindata.length; ++i){
			pinData.put(pindata[i].getAddress(),pindata[i]);
		}
	}

	public static void main(String[] args) {
		try {
			LoggingFactory.init("INFO");
			
			// Runtime.start("webgui", "WebGui");
			// Runtime.start("gui", "GUIService");

			// test a "real" arduino
			useVirtualHardware = false;
			port = "COM5";
			// port = "COM4";
			// port = "COM99";

			ArduinoTest test = new ArduinoTest();
			ArduinoTest.setUpBeforeClass();
			
			// arduino.record();

			if (virtual != null) {
				virtual.connect(port);
			}
			arduino.connect(port);
			
			arduino.setDebug(true);
			//arduino.enableAck(false);
			
			test.testConnectString();
			
			Servo servo01 = (Servo)Runtime.start("servo", "Servo");
			
		   boolean b = true;
	      if (b) {
	        return;
	      }

			test.testGetVersion();
			test.testServoAttachServoInteger();
			test.testEnableBoardStatus();
			test.testEnablePinInt();

	

			// test specific method
			test.testServoAttachServoInteger();

			// run junit as java app
			JUnitCore junit = new JUnitCore();
			Result result = junit.run(ArduinoTest.class);
			log.info("Result was: {}", result);

			// Runtime.dump();

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

}
