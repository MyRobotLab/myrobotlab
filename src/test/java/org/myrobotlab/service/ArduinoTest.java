package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.Msg;
import org.myrobotlab.arduino.virtual.Device;
import org.myrobotlab.arduino.virtual.MrlServo;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Arduino.Sketch;
import org.myrobotlab.service.data.DeviceMapping;
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
 * FIXME - test all types of Arduino's and thier pin definitions uno, mega, nano, pico, decillia
 * FIXME - make sure the primitives defined in the PinArrayControl are accessable through invoking ... (as per all primitive signature interfaces)
 * FIXME - test the othe PinArrayControllers .. Pcf8574, Mpr121, RasPi
 * FIXME - test webgui oscope
 *
 */

public class ArduinoTest extends AbstractTest implements PinArrayListener {

  static Arduino arduino01 = null;
  static Arduino arduino02 = null;
  public final static Logger log = LoggerFactory.getLogger(ArduinoTest.class);

  static String port01 = "COM1";
  static String port02 = "COM2";

  static Serial serial01 = null;
  static Serial serial02 = null;

  static SerialDevice uart01 = null;

  static VirtualArduino virtual01 = null;

  String analogPin = "A1";
  String digitalPin = "D0";

  Map<String, PinData> pinData = new HashMap<String, PinData>();

  int servoPin01 = 7;
  int servoPin02 = 8;

  private void assertVirtualPinValue(int address, int value) {
    if (virtual01 != null) {
      assertTrue(virtual01.readBlocking(address, 50) == value);
      virtual01.clearPinQueue(address);
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
      pinData.put(pindata[i].pin, pindata[i]);
    }
  }

  @Before
  public void setUp() throws Exception {

    if (isVirtual()) {
      virtual01 = (VirtualArduino) Runtime.start("virtual01", "VirtualArduino");
      uart01 = virtual01.getSerial();
      uart01.setTimeout(100); // don't want to hang when decoding results...
      virtual01.connect(port01);
    }

    // TODO: Initialize the arduino under test. (potentially do this in each
    // test method vs passing the same one around ..)
    arduino01 = (Arduino) Runtime.start("arduinoTest", "Arduino");
    // TODO: have a separate unit test for testing serial. we probably don't
    // want to intermingle that testing here (if we can avoid it.)
    serial01 = arduino01.getSerial();
    arduino01.connect(port01);

    /**
     * Arduino's expected state before each test is 'connected' with no devices,
     * no pins enabled
     */
  }

  // TODO: fix this test method.
  // @Test
  public final void testAnalogWrite() throws InterruptedException, IOException {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));

    arduino01.analogWrite(10, 0);
    assertVirtualPinValue(10, 0);

    arduino01.analogWrite(10, 127);
    assertVirtualPinValue(10, 127);

    arduino01.analogWrite(10, 128);
    assertVirtualPinValue(10, 128);

    arduino01.analogWrite(10, 255);
    assertVirtualPinValue(10, 255);

    arduino01.error("test");
  }

  @Test
  public final void testConnect() throws IOException {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    log.info("testConnect - begin");
    arduino01.disconnect();
    arduino01.connect(port01);
    sleep(10);
    assertTrue(arduino01.isConnected());
    assertEquals(Msg.MRLCOMM_VERSION, arduino01.getBoardInfo().getVersion().intValue());
    log.info("testConnect - end");
  }

  @Test
  public void testConnectResetAndClear() {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    arduino01.connect(port01);
    arduino01.reset();

    serial01.clear();
    serial01.setTimeout(100);

    uart01.clear();
    uart01.setTimeout(100);

    pinData.clear();
  }

  @Test
  public void testConnectString() {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    for (int i = 0; i < 20; ++i) {
      // arduino.connect(port);
      // arduino.enableAck(true);
      arduino01.echo(90.57F, 129, 30.123F);
      /*
       * arduino.echo(30003030L + i); arduino.echo(2L); arduino.echo(-1L);
       */
      // arduino.disconnect();
    }
  }

  @Test
  public final void testDigitalWrite() {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    log.info("testDigitalWrite");
    arduino01.digitalWrite(10, 1);
    // assertEquals("digitalWrite/10/1\n", uart.decode());
    arduino01.digitalWrite(10, 0);
    // assertEquals("digitalWrite/10/0\n", uart.decode());
    // arduino.digitalWrite(10, 255);
    // assertEquals("digitalWrite/10/0", uart.decode());
  }

  @Test
  public final void testDisconnect() throws IOException {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    log.info("testDisconnect");
    // shutdown mrlcomm
    // disconnect
    arduino01.disconnect();
    // clear
    serial01.clear();
    uart01.clear();
    // test disconnected
    assertTrue(!arduino01.isConnected());
    // test no data - no exception ?
    arduino01.digitalWrite(10, 1);
    // reconnect
    arduino01.connect(port01);
    // test we are connected
    assertTrue(arduino01.isConnected());
    // assert basic re-connect worky
    arduino01.digitalWrite(10, 1);
  }

  @Test
  public void testEnableBoardStatus() {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    org.myrobotlab.service.Test test = (org.myrobotlab.service.Test) Runtime.start("test", "Test");
    test.subscribe(arduino01.getName(), "publishBoardStatus");
    arduino01.enableBoardInfo(true);
    // FIXME notify with timeout
    arduino01.enableBoardInfo(false);
  }

  @Test
  public void testEnablePinInt() {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    // set board type
    arduino01.enablePin(analogPin);
    arduino01.attach(this);
    sleep(50);
    assertTrue(pinData.containsKey(arduino01.getPin(analogPin).getPinName()));
    arduino01.disablePin(analogPin);
  }

  @Test
  public void testGetBoardInfo() {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    arduino01.connect(port01);
    BoardInfo boardInfo = arduino01.getBoardInfo();
    // assertTrue(boardInfo.isValid());
    assertTrue(boardInfo.getVersion().intValue() == Msg.MRLCOMM_VERSION);
  }

  // TODO: fails in unit test in ant , not in eclipse.
  // @Test
  public final void testGetSketch() {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    Sketch sketch = arduino01.getSketch();
    assertNotNull(sketch);
    assertTrue(sketch.data.length() > 0);
    arduino01.setSketch(null);
    assertNull(arduino01.getSketch());
    arduino01.setSketch(sketch);
    assertEquals(sketch, arduino01.getSketch());
  }
  
  @Test
  public final void pinArrayTest() {
    fail("fix me - complete interface test");
  }

  @Test
  public final void testGetVersion() {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    arduino01.connect(port01);
    assertEquals(Msg.MRLCOMM_VERSION, arduino01.getBoardInfo().getVersion().intValue());
  }

  @Test
  public final void testPinModeIntegerInteger() {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    arduino01.pinMode(8, Arduino.OUTPUT);
    // assertEquals("pinMode/8/1\n", uart.decode());
  }

  // If we enable this test, it should assert something.
  // @Test
  public final void testPinModeIntString() {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    arduino01.pinMode(8, "OUTPUT");
    // assertEquals("pinMode/8/1\n", uart.decode());
    // TODO: add an assert here.
  }

  @Test
  public void testReleaseService() {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    arduino01.releaseService();
    // better re-start it
    arduino01 = (Arduino) Runtime.start("arduino", "Arduino");
  }

  @Test
  public final void testServo() throws Exception {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));

    Servo servo = (Servo) Runtime.start("servo01", "Servo");
    arduino01.connect(port01);
    
    // TEST AUTO DETACH !!!!

    assertTrue("isConnected", arduino01.isConnected());

    // attach it
    servo.attach(arduino01, servoPin01);
    sleep(300); // wait for asynchronous creation over serial of an MrlComm
                // servo

    // get DeviceId
    DeviceMapping mapping = arduino01.deviceList.get("servo01");
    assertNotNull("verify arduino mapping exists in device list", mapping);
    
    MrlServo mrlservo = null;
    if (isVirtual()) {
      Device device = virtual01.getDevice(mapping.getId());
      assertNotNull("verify virtual device exists", device);
      assertTrue("verify its a servo", device instanceof MrlServo);
      mrlservo = (MrlServo)device;
    }

    // verify its attached
    assertTrue("verify servo is not attached to arduino", servo.isAttached(arduino01));
    assertTrue("verify arduino is not attached to servo", arduino01.isAttached(servo));
    assertTrue("verify servo is not attached to arduino by name", servo.isAttached(arduino01.getName()));
    assertTrue("verify arduino is not attached to servo by name", arduino01.isAttached(servo.getName()));
    assertTrue("arduino is attached and contains a servo in device list", arduino01.getAttached().contains(servo.getName()));
    
    if (isVirtual()) {
      assertTrue("verifty virtual mrlservo is enabled", mrlservo.enabled);
    }
    
    // move it
    servo.moveToBlocking(30);    
    if (isVirtual()) {
      assertTrue("virtual servo moved blocking to 30", mrlservo.targetPosUs == Arduino.degreeToMicroseconds(30));
    }
    
    servo.moveTo(100);
    sleep(100);
    if (isVirtual()) {
      assertTrue("virtual servo moved to 100", mrlservo.targetPosUs == Arduino.degreeToMicroseconds(100));
    }
    
    // detach it
    arduino01.detach(servo);
    sleep(300); // wait for asynchronous removal of MrlServo
    assertFalse("verify servo is not attached to arduino", servo.isAttached(arduino01));
    assertFalse("verify arduino is not attached to servo", arduino01.isAttached(servo));
    assertFalse("verify servo is not attached to arduino by name", servo.isAttached(arduino01.getName()));
    assertFalse("verify arduino is not attached to servo by name", arduino01.isAttached(servo.getName()));
    assertNull("verify device has been removed", virtual01.getDevice(mapping.getId()));
    assertFalse("verifty servo is disabled", servo.enabled());
    if (isVirtual()) {
      assertFalse("verifty virtual mrlservo is disabled", mrlservo.enabled);
    }

    
    assertFalse(servo.isAttached(arduino01));

    // attach it the other way
    arduino01.attach(servo, servoPin01);

    // verify its attached
    assertTrue(servo.isAttached(arduino01));
    assertTrue(arduino01.getAttached().contains(servo.getName()));

    // servo should have the correct pin
    assertTrue(servoPin01 == servo.getPin());

    // get its device id
    int deviceId = arduino01.getDeviceId(servo.getName());

    // get mrlcom's device id
    // virtualized tests
    MrlServo mrlServo = null;
    if (virtual01 != null) {
      Thread.sleep(100);
      mrlServo = (MrlServo) virtual01.getDevice(deviceId);
      // verify
      assertTrue(deviceId == mrlServo.id);
    }

    // can we enable to a different pin?
    servo.enable(servoPin01 + 1);
    if (virtual01 != null) {
      sleep(100);
      assertTrue(mrlServo.pin == servoPin01 + 1);
      assertTrue(mrlServo.pin == servo.getPin());
    }

    int velocity = 50;
    // degree per second
    servo.setVelocity(velocity);
    if (virtual01 != null) {
      sleep(100);
      assertTrue(mrlServo.velocity == velocity);
    }

    // attach to the correct pin again
    servo.enable(servoPin01);
    servo.moveTo(30);
    servo.moveTo(130);
    servo.moveTo(30);
    // assertEquals(virtual.servoMoveTo(130));
    servo.rest();

    assertEquals(arduino01.getName(), servo.getController().getName());

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
    servo.enable();
    // assertEquals("servoAttach/7/9/5/115/101/114/118/111\n",
    // uart.decode());
    // // assertEquals(servoPin, servo.getPin().intValue());
    assertEquals(arduino01.getName(), servo.getController().getName());

    servo.moveTo(90);
    // assertEquals("servoWrite/7/90\n", uart.decode());

    arduino01.enableBoardInfo(true);

    servo.startService();

    servo.moveTo(90);

    // when we release a service - it should
    // notify and process releasing itself from attached
    // services
    servo.releaseService();
    assertFalse(arduino01.getAttached().contains(servo.getName()));
    assertFalse(servo.isAttached(arduino01));
  }

  @Test
  public void testSetBoardMega() {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    String boardType = arduino01.getBoard();
    arduino01.setBoardMega();
    assertEquals(Arduino.BOARD_TYPE_MEGA, arduino01.getBoard());
    List<PinDefinition> pins = arduino01.getPinList();
    assertEquals(70, pins.size());
    arduino01.setBoard(boardType);
  }

  @Test
  public void testSetBoardUno() {
    if (printMethods)
      System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    String boardType = arduino01.getBoard();

    arduino01.setBoardUno();

    assertEquals(Arduino.BOARD_TYPE_UNO, arduino01.getBoard());

    List<PinDefinition> pins = arduino01.getPinList();
    assertEquals(20, pins.size());

    arduino01.setBoard(boardType);
  }

}
