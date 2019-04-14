package org.myrobotlab.service;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

//TODO: re-enable this test when we figure out why it fails from the
// command line ant build...

@Ignore
public class UltrasonicSensorTest extends AbstractTest {

  static PinArrayControl arduino = null;
  static TestCatcher catcher = null;

  // VirtualArduino setup
  static int echoPin = 7;
  public final static Logger log = LoggerFactory.getLogger(UltrasonicSensor.class);
  static String port = "COM4";

  static UltrasonicSensor sensor = null;
  static SerialDevice serial = null;
  static int trigPin = 8;
  static Serial uart = null;
  static boolean useVirtualHardware = true; // base class for this and

  static VirtualArduino virtual = null;

  // FIXME - test for re-entrant !!!!
  // FIXME - single switch for virtual versus "real" hardware

  static public void main(String[] args) {

    try {
      // // LoggingFactory.init();
      // FIXME - base class static method .webGui() & .gui()
      // Runtime.start("webgui", "WebGui");
      // Runtime.start("gui", "SwingGui");

      // test a "real" arduino
      useVirtualHardware = true;

      UltrasonicSensorTest test = new UltrasonicSensorTest();
      UltrasonicSensorTest.setUpBeforeClass();

      // arduino.record();

      if (virtual != null) {
        virtual.connect(port);
      }

      test.test();

      // run junit as java app
      JUnitCore junit = new JUnitCore();
      Result result = junit.run(UltrasonicSensorTest.class);
      log.info("Result was: {}", result);

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // // LoggingFactory.init(Level.INFO);

    log.info("setUpBeforeClass");
    sensor = (UltrasonicSensor) Runtime.start("arduino", "UltrasonicSensor");
    virtual = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
    if (useVirtualHardware) {
      virtual.connect(port);
    }

    catcher = (TestCatcher) Runtime.start("catcher", "TestCatcher");
    uart = (Serial) virtual.getSerial();
    // uart.setTimeout(100); // don't want to hang when decoding results...
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

  // TODO - Virtual Serial test - do a record of tx & rx on a real sensor
  // then send the data - IT MUST BE INTERLEAVED
  @Test
  public final void test() throws Exception {

    TestCatcher catcher = (TestCatcher) Runtime.start("catcher", "TestCatcher");
    sensor.addRangeListener(catcher);
    sensor.attach(port, trigPin, echoPin);
    sensor.startRanging();
    log.info("here");
    sensor.stopRanging();

    uart.stopRecording();

    sensor.startRanging();
    log.info("here");

    sensor.stopRanging();

    sensor.startRanging();

    sensor.startRanging();

    sensor.stopRanging();
  }


}
