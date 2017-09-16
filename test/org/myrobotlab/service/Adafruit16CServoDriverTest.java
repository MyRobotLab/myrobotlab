package org.myrobotlab.service;

import static org.myrobotlab.service.Adafruit16CServoDriver.SERVOMAX;
import static org.myrobotlab.service.Adafruit16CServoDriver.SERVOMIN;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.FileIO.FileComparisonException;
import org.myrobotlab.service.interfaces.SerialDevice;

public class Adafruit16CServoDriverTest {

	static Adafruit16CServoDriver driver = null;
	static Arduino arduino = null;
	static SerialDevice serial = null;
	static VirtualDevice virtual = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		driver = (Adafruit16CServoDriver) Runtime.start("driver", "Adafruit16CServoDriver");
		// arduino = driver.getArduino();
		arduino = (Arduino) Runtime.start("arduino", "Arduino");
		serial = arduino.getSerial();
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
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetDescription() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testStartService() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetPeers() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testMain() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testAdafruit16CServoDriver() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testAttachArduino() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testAttachServoInteger() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testBegin() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testConnect() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetArduino() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetPinList() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testIsAttached() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testServoAttach() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testServoDetach() {
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
	public final void testSetPWM() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetPWMFreq() {
		// fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetServo() {
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

	public final void test() throws IOException, FileComparisonException {
		// virtual.create
		virtual.createVirtualSerial("v1");
		// FIXME - make virtual UART

		Serial uart = virtual.getUart("v1");
		uart.open("v1");
		uart.record();
		arduino.connect("v0");

		driver.setServo(0, SERVOMIN);
		driver.setServo(0, SERVOMAX);
		driver.setServo(0, SERVOMIN);
		driver.setServo(0, SERVOMAX);
		driver.setServo(0, SERVOMIN);
		driver.setServo(0, SERVOMAX);
		driver.setServo(0, SERVOMIN);
		driver.setServo(0, SERVOMAX);
		driver.setServo(0, SERVOMIN);
		driver.setServo(0, SERVOMAX);
		driver.setServo(0, SERVOMIN);
		driver.setServo(0, SERVOMAX);

		// begin();
		driver.setPWMFreq(0, 60);

		for (int i = SERVOMIN; i < SERVOMAX; ++i) {
			driver.setPWM(0, 0, i);
		}

		driver.setPWM(0, 0, 0);

		driver.setPWM(0, 0, SERVOMIN);
		driver.setPWM(0, 0, SERVOMAX);
		driver.setPWM(0, 0, SERVOMIN);
		driver.setPWM(0, 0, SERVOMAX);
		driver.setPWM(0, 0, SERVOMIN);
		driver.setPWM(0, 0, SERVOMAX);
		driver.setPWM(0, 0, SERVOMAX);

		// need to allow time :P
		// to flush serial thread
		// sleep(1000);
		// disconnect / close arduino port
		// flush cable
		// stop recording
		arduino.disconnect();
		// cable.close();
		uart.stopRecording();

		FileIO.compareFiles("test/Adafruit16CServoDriver/test.rx", "test/Adafruit16CServoDriver/control/test.rx");

	}

}
