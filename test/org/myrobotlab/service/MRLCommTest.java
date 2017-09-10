package org.myrobotlab.service;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class MRLCommTest {
	
	public final static Logger log = LoggerFactory.getLogger(MRLCommTest.class);

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
	public void testConnectString() {
		// fail("Not yet implemented");
	}

	@Test
	public void testConnectStringIntegerIntIntInt() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetPinList() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetVersion() {
		// fail("Not yet implemented");
	}

	@Test
	public void testIsConnected() {
		// fail("Not yet implemented");
	}

	@Test
	public void testPublishSensorData() {
		// fail("Not yet implemented");
	}
	
  //	 public static void main(String[] args) {
  //		    try {
  //
  //		      LoggingFactory.getInstance().configure();
  //		      LoggingFactory.getInstance().setLevel(Level.INFO);
  //
  //		      ArduinoTest.setUpBeforeClass();
  //		      ArduinoTest test = new ArduinoTest();
  //		      test.testConnect();
  //
  //		      JUnitCore junit = new JUnitCore();
  //		      Result result = junit.run(ArduinoTest.class);
  //		      log.info("Result was: {}", result);
  //		      // WebGui gui = (WebGui) Runtime.start("webgui", "WebGui");
  //		      // ServiceInterface gui = Runtime.start("gui", "SwingGui");
  //
  //		      Runtime.dump();
  //
  //		      log.info("here");
  //		      // serial.removeByteListener(gui.getName());
  //		      // uart.removeByteListener(gui.getName());
  //
  //		      Runtime.dump();
  //
  //		    } catch (Exception e) {
  //		      Logging.logError(e);
  //		    }
  //		  }
  //
}
