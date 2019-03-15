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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.Msg;
import org.myrobotlab.arduino.virtual.MrlServo;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Arduino.Sketch;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

/**
 * 
 * @author GroG
 *
 */

public class ArduinoTest extends AbstractTest implements PinArrayListener {

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

  // things to test
  static Arduino arduino = null;
  public final static Logger log = LoggerFactory.getLogger(ArduinoTest.class);

  static String port = "COM25";
  static Serial serial = null;

  static SerialDevice uart = null;
  // TODO: read the value of this off a property off a config file (maybe a
  // properties file for the mrl test framework.)
  static boolean useVirtualHardware = true;

  // virtual hardware
  static VirtualArduino virtual = null;
  String enablePin = "A1";
  Map<Integer, PinData> pinData = new HashMap<Integer, PinData>();
  // FIXME - test for re-entrant !!!!
  // FIXME - single switch for virtual versus "real" hardware

  int servoPin = 7;

  int writeAddress = 6;

  private void assertVirtualPinValue(int address, int value) {
    if (virtual != null) {
      assertTrue(virtual.readBlocking(address, 50) == value);
      virtual.clearPinQueue(address);
    }
  }

  @Override
  public String getName() {
    return "arduinoTest";
  }

  @Override
  public boolean isLocal() {
    return true;
  }

  @Override
  public void onPinArray(PinData[] pindata) {
    log.debug("onPinArray size {}", pindata.length);
    for (int i = 0; i < pindata.length; ++i) {
      pinData.put(pindata[i].address, pindata[i]);
    }
  }

  @Before
  public void setUp() throws Exception {
    // LoggingFactory.init("WARN");
    // setup the virtual port (if enabled)
    // FIXME - needs a seemless switch
    if (useVirtualHardware) {
      virtual = (VirtualArduino) Runtime.start("virtualTest", "VirtualArduino");
      uart = virtual.getSerial();
      uart.setTimeout(100); // don't want to hang when decoding results...
      virtual.connect(port);
    }

    // TODO: Initialize the arduino under test. (potentially do this in each
    // test method vs passing the same one around ..)
    arduino = (Arduino) Runtime.start("arduinoTest", "Arduino");
    // TODO: have a separate unit test for testing serial. we probably don't
    // want to intermingle that testing here (if we can avoid it.)
    serial = arduino.getSerial();
    arduino.connect(port);

    /**
     * Arduino's expected state before each test is 'connected' with no devices,
     * no pins enabled
     */
  }

  // TODO: fix this test method.
  // @Test
  public final void testAnalogWrite() throws InterruptedException, IOException {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));

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

  @Test
  public final void testConnect() throws IOException {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    log.info("testConnect - begin");
    arduino.disconnect();
    arduino.connect(port);
    sleep(10);
    assertTrue(arduino.isConnected());
    assertEquals(Msg.MRLCOMM_VERSION, arduino.getBoardInfo().getVersion().intValue());
    log.info("testConnect - end");
  }

  @Test
  public void testConnectResetAndClear() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    arduino.connect(port);
    arduino.reset();

    serial.clear();
    serial.setTimeout(100);

    uart.clear();
    uart.setTimeout(100);

    pinData.clear();
  }

  @Test
  public void testConnectString() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    for (int i = 0; i < 20; ++i) {
      // arduino.connect(port);
      // arduino.enableAck(true);
      arduino.echo(90.57F, 129, 30.123F);
      /*
       * arduino.echo(30003030L + i); arduino.echo(2L); arduino.echo(-1L);
       */
      // arduino.disconnect();
    }
  }

  @Test
  public final void testDigitalWrite() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    log.info("testDigitalWrite");
    arduino.digitalWrite(10, 1);
    // assertEquals("digitalWrite/10/1\n", uart.decode());
    arduino.digitalWrite(10, 0);
    // assertEquals("digitalWrite/10/0\n", uart.decode());
    // arduino.digitalWrite(10, 255);
    // assertEquals("digitalWrite/10/0", uart.decode());
  }

  @Test
  public final void testDisconnect() throws IOException {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
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

  @Test
  public void testEnableBoardStatus() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    org.myrobotlab.service.Test test = (org.myrobotlab.service.Test) Runtime.start("test", "Test");
    test.subscribe(arduino.getName(), "publishBoardStatus");
    arduino.enableBoardInfo(true);
    // FIXME notify with timeout
    arduino.enableBoardInfo(false);
  }

  @Test
  public void testEnablePinInt() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    // set board type
    arduino.enablePin(enablePin);
    arduino.attach(this);
    sleep(50);
    assertTrue(pinData.containsKey(arduino.pinNameToAddress(enablePin)));
    arduino.disablePin(enablePin);
  }

  @Test
  public void testGetBoardInfo() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    arduino.connect(port);
    BoardInfo boardInfo = arduino.getBoardInfo();
    // assertTrue(boardInfo.isValid());
    assertTrue(boardInfo.getVersion().intValue() == Msg.MRLCOMM_VERSION);
  }

  // TODO: fails in unit test in ant , not in eclipse.
  // @Test
  public final void testGetSketch() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
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
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    arduino.connect(port);
    assertEquals(Msg.MRLCOMM_VERSION, arduino.getBoardInfo().getVersion().intValue());
  }

  @Test
  public final void testPinModeIntegerInteger() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    arduino.pinMode(8, Arduino.OUTPUT);
    // assertEquals("pinMode/8/1\n", uart.decode());
  }

  // If we enable this test, it should assert something.
  // @Test
  public final void testPinModeIntString() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    arduino.pinMode(8, "OUTPUT");
    // assertEquals("pinMode/8/1\n", uart.decode());
    // TODO: add an assert here.
  }

  @Test
  public void testReleaseService() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    arduino.releaseService();
    // better re-start it
    arduino = (Arduino) Runtime.start("arduino", "Arduino");
  }

  @Test
  public final void testServoAttachServoInteger() throws Exception {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    Servo servo = null;

    // make sure we're connected
    arduino.connect(port);
    assertTrue(arduino.isConnected());
    // assertTrue(arduino.getBoardInfo().isValid());

    // reentrancy make code strong !
    // for (int i = 0; i < 3; ++i) {

    // create a servo
    servo = (Servo) Runtime.start("servo", "Servo");

    // attach it
    servo.attach(arduino, servoPin);

    // verify its attached
    assertTrue(servo.isAttached());
    assertTrue(servo.isAttachedServoController(arduino));
    assertTrue(arduino.getAttached().contains(servo.getName()));

    // detach it
    arduino.detach(servo);

    // verify its detached
    assertFalse(arduino.getAttached().contains(servo.getName()));
    assertFalse(servo.enabled());
    assertFalse(servo.isAttachedServoController(arduino));

    // attach it the other way
    arduino.attach(servo, servoPin);

    // verify its attached
    assertTrue(servo.isAttached());
    assertTrue(servo.isAttachedServoController(arduino));
    assertTrue(arduino.getAttached().contains(servo.getName()));

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

    arduino.enableBoardInfo(true);

    servo.startService();

    servo.moveTo(90);

    // when we release a service - it should
    // notify and process releasing itself from attached
    // services
    servo.releaseService();
    assertFalse(arduino.getAttached().contains(servo.getName()));
    assertFalse(servo.isAttached());
    assertFalse(servo.isAttachedServoController(arduino));

  }

  @Test
  public void testSetBoardMega() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    String boardType = arduino.getBoard();

    arduino.setBoardMega();

    assertEquals(Arduino.BOARD_TYPE_MEGA, arduino.getBoard());

    List<PinDefinition> pins = arduino.getPinList();
    assertEquals(70, pins.size());

    arduino.setBoard(boardType);
  }

  @Test
  public void testSetBoardUno() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    String boardType = arduino.getBoard();

    arduino.setBoardUno();

    assertEquals(Arduino.BOARD_TYPE_UNO, arduino.getBoard());

    List<PinDefinition> pins = arduino.getPinList();
    assertEquals(20, pins.size());

    arduino.setBoard(boardType);
  }

}
