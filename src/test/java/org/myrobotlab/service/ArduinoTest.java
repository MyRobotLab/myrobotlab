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
import org.myrobotlab.test.TestUtils;
import org.slf4j.Logger;

/**
 * 
 * @author GroG
 *
 */

public class ArduinoTest implements PinArrayListener {

  public final static Logger log = LoggerFactory.getLogger(ArduinoTest.class);

  // TODO: read the value of this off a property off a config file (maybe a properties file for the mrl test framework.) 
  static boolean useVirtualHardware = true;
  static String port = "COM5";

  // things to test
  static Arduino arduino = null;
  static Serial serial = null;

  // virtual hardware
  static VirtualArduino virtual = null;
  static SerialDevice uart = null;

  int servoPin = 7;
  String enablePin = "A1";
  int writeAddress = 6;

  Map<Integer, PinData> pinData = new HashMap<Integer, PinData>();
  // FIXME - test for re-entrant !!!!
  // FIXME - single switch for virtual versus "real" hardware
  
  @Before
  public void setUp() throws Exception {
    TestUtils.initEnvirionment();
    // setup the virtual port (if enabled)
    // FIXME - needs a seemless switch
    if (useVirtualHardware) {
      virtual = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
      uart = virtual.getSerial();
      uart.setTimeout(100); // don't want to hang when decoding results...
      virtual.connect(port);
    }

    
    // TODO:  Initialize the arduino under test.  (potentially do this in each test method vs passing the same one around ..)
    arduino = (Arduino) Runtime.start("arduino", "Arduino");
    // TODO: have a separate unit test for testing serial.  we probably don't want to intermingle that testing here (if we can avoid it.)
    serial = arduino.getSerial();
    arduino.connect(port);
    
    /**
     * Arduino's expected state before each test is
     * 'connected' with no devices, no pins enabled
     */    
  }

  // TODO : broken in ANT but not in eclipse!
  // @Test
  public void testConnectResetAndClear() {

    arduino.connect(port);
    arduino.reset();

    serial.clear();
    serial.setTimeout(100);

    uart.clear();
    uart.setTimeout(100);

    pinData.clear();
  }

  // TODO: broken from ant build due to file not founds (not broken in eclipse.)
  // @Test
  public void testReleaseService() {
    arduino.releaseService();
    // better re-start it
    arduino = (Arduino)Runtime.start("arduino", "Arduino");
  }


  // TODO: fix this test method.
  // @Test
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
    arduino.enableBoardInfo(true);
    // FIXME notify with timeout
    arduino.enableBoardInfo(false);
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
    assertTrue(pinData.containsKey(arduino.pinNameToAddress(enablePin)));
    arduino.disablePin(enablePin);
  }

  @Test
  public void testGetBoardInfo() {
    arduino.connect(port);
    BoardInfo boardInfo = arduino.getBoardInfo();
    // assertTrue(boardInfo.isValid());
    assertTrue(boardInfo.getVersion().intValue() == Msg.MRLCOMM_VERSION);
  }


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

  // TODO: fails in unit test in ant , not in eclipse.
  // @Test
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

  // If we enable this test, it should assert something.
  // @Test   
  public final void testPinModeIntString() {
    log.info("testPinModeIntString");
    arduino.pinMode(8, "OUTPUT");
    // assertEquals("pinMode/8/1\n", uart.decode());
    // TODO: add an assert here.
  }

  @Test
  public final void testPinModeIntegerInteger() {
    log.info("testPinModeIntegerInteger");
    arduino.pinMode(8, Arduino.OUTPUT);
    // assertEquals("pinMode/8/1\n", uart.decode());
  }

  // TODO: re-enable this when it's worky.
  // @Test
  public final void testServoAttachServoInteger() throws Exception {
    log.info("testServoAttachServoInteger");
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


  // TODO: re-enable when worky
  // @Test
  public void testSetBoardMega() {
    log.info("testSetBoardMega");
    String boardType = arduino.getBoard();

    arduino.setBoardMega();

    assertEquals(Arduino.BOARD_TYPE_MEGA, arduino.getBoard());

    List<PinDefinition> pins = arduino.getPinList();
    assertEquals(70, pins.size());

    arduino.setBoard(boardType);
  }

  @Test
  public void testSetBoardUno() {
    log.info("testSetBoardUno");
    String boardType = arduino.getBoard();

    arduino.setBoardUno();

    assertEquals(Arduino.BOARD_TYPE_UNO, arduino.getBoard());

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
      pinData.put(pindata[i].address,pindata[i]);
    }
  }

//  public static void main(String[] args) {
//    try {
//      LoggingFactory.init("INFO");
//
//      Runtime.start("webgui", "WebGui");
//      // Runtime.start("gui", "SwingGui");
//
//      // test a "real" arduino
//      useVirtualHardware = false;
//      port = "COM5";
//      // port = "COM4";
//      // port = "COM99";
//
//      ArduinoTest test = new ArduinoTest();
//      ArduinoTest.setUpBeforeClass();
//
//      Pir pir = (Pir)Runtime.start("pir","Pir");
//      pir.attach(arduino, 7);
//
//      // arduino.record();
//
//      if (virtual != null) {
//        virtual.connect(port);
//      }
//      arduino.connect(port);
//
//      arduino.setDebug(true);
//      //arduino.enableAck(false);
//
//      test.testConnectString();
//
//      Servo servo01 = (Servo)Runtime.start("servo01", "Servo");
//      Servo servo02 = (Servo)Runtime.start("servo02", "Servo");
//
//      servo01.setMinMax(10, 175);
//      servo01.setInverted(true);
//      servo01.setRest(157);
//      servo02.setRest(140);
//
//      servo01.setInverted(true);
//
//      servo01.attach(arduino, 7);
//      arduino.attach(servo01, 7);
//      arduino.attach(servo01, 8);
//
//      // arduino.disconnect();
//
//      boolean b = true;
//      if (b) {
//        return;
//      }
//
//      test.testGetVersion();
//      test.testServoAttachServoInteger();
//      test.testEnableBoardStatus();
//      test.testEnablePinInt();
//
//
//
//      // test specific method
//      test.testServoAttachServoInteger();
//
//      // run junit as java app
//      JUnitCore junit = new JUnitCore();
//      Result result = junit.run(ArduinoTest.class);
//      log.info("Result was: {}", result);
//
//      // Runtime.dump();
//
//    } catch (Exception e) {
//      Logging.logError(e);
//    }
//  }

}
