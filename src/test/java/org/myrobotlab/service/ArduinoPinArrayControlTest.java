package org.myrobotlab.service;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.slf4j.Logger;

@Ignore
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
		// fail("Not yet implemented");
	}

	@Test
	public void testReadString() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetModeIntString() {
		// fail("Not yet implemented");
	}

	@Test
	public void testSetModeStringString() {
		// fail("Not yet implemented");
	}

	@Test
	public void testWriteIntInteger() {
		// fail("Not yet implemented");
	}

	@Test
	public void testWriteStringInteger() {
		// fail("Not yet implemented");
	}
	
	public void logPins(List<PinDefinition> pins){
		for (int i = 0; i < pins.size(); ++i){
			log.info(pins.get(i).toString());
		}
	}

	@Test
	public void testGetPinList() throws Exception {
		Arduino arduino = (Arduino)Runtime.start("arduino", "Arduino");
		// Runtime.start("gui", "SwingGui");
		Runtime.start("webgui", "WebGui");
		Runtime.start("python", "Python");
		
		arduino.connect("COM5");
		// arduino.setBoardMega();
		
		Servo servo01 = (Servo)Runtime.start("servo01", "Servo");
		servo01.attach(arduino, 7);
		
		servo01.info("hello 1");
		servo01.info("hello 2");
		servo01.warn("warning here 1");
		servo01.warn("warning here 2");
		servo01.error("error here 1");
		servo01.error("error here 2");
		
		// arduino.setDebug(true);
		/*
		arduino.enablePin(14);
		arduino.enablePin(16);
		arduino.enablePin(17);
		arduino.disablePin(14);
		*/
		
		boolean skip = true;
		if (skip){
			return;
		}
		
		List<PinDefinition> pins = null; 
		
		arduino.connect("COM5");
		
		arduino.enableBoardInfo(true);
		arduino.enableBoardInfo(false);
				
		/*
		arduino.disconnect();
		
		pins = arduino.getPinList();
		log.info("Arduino {} has {} pins", arduino.getBoardType(), pins.size());
		logPins(pins);
		
		arduino.setBoardMega();
		pins = arduino.getPinList();
		log.info("Arduino {} has {} pins", arduino.getBoardType(), pins.size());
		logPins(pins);
		*/
	
		arduino.setBoardUno();
		pins = arduino.getPinList();
		log.info("Arduino {} has {} pins", arduino.getBoard(), pins.size());
		logPins(pins);
		
		// arduino.connect("COM5");
		
		
		Pir pir = (Pir)Runtime.start("pir", "Pir");
		UltrasonicSensor srf04 = (UltrasonicSensor)Runtime.start("srf04", "UltrasonicSensor");
		
		// pir.attach(arduino, "A0");
		pir.attach(arduino, 14);
		
		srf04.attach(arduino, 10, 11);
		
	}

	@Test
	public void testPinArrayGetPinList() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPinArrayReadInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPinArrayReadString() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPinArraySetModeIntString() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPinArraySetModeStringString() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPinArrayWriteIntInteger() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPinArrayWriteStringInteger() {
		// fail("Not yet implemented");
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

			/*
			JUnitCore junit = new JUnitCore();
			Result result = junit.run(ArduinoPinArrayControlTest.class);
			log.info("Result: {}", result);
			*/
			
			

			// Runtime.dump();

		} catch (Exception e) {
			log.error("test threw", e);
		}
	}
}
