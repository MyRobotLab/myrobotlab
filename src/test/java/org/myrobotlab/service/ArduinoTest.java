package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.Msg;
import org.myrobotlab.arduino.virtual.Device;
import org.myrobotlab.arduino.virtual.MrlServo;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.DeviceMapping;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PinListener;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

/**
 * 
 * @author GroG
 * 
 *         FIXME - test all types of Arduino's and thier pin definitions uno,
 *         mega, nano, pico, decillia FIXME - make sure the primitives defined
 *         in the PinArrayControl are accessable through invoking ... (as per
 *         all primitive signature interfaces) FIXME - test the othe
 *         PinArrayControllers .. Pcf8574, Mpr121, RasPi FIXME - test webgui
 *         oscope
 *
 */

public class ArduinoTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(ArduinoTest.class);

  static Arduino arduino01 = null;

  static TestCatcher catcher = null;

  static String port01 = "COM16";

  String analogPin = "A1"; // on Arduino this is address 15

  String digitalPin = "D0";

  String servoPin01 = "7";

  String[] activePins = null;

  private void assertVirtualPinValue(VirtualArduino virtual, int address, int value) {
    if (virtual != null) {
      assertTrue(virtual.readBlocking(address, 50) == value);
      virtual.clearPinQueue(address);
    }
  }

  @Override
  public String getName() {
    return "arduinoTest";
  }

  @Before
  public void setUp() throws Exception {
    arduino01 = (Arduino) Runtime.start("arduino03", "Arduino");
    catcher = (TestCatcher) Runtime.start("arduinoTestCatcher", "TestCatcher");
    arduino01.connect(port01);
    assertTrue(String.format("arduino could not connect to port %s", port01), arduino01.isConnected());
  }

  // TODO: fix this test method.
  // @Test
  public final void testAnalogWrite() throws InterruptedException, IOException {

    arduino01.analogWrite(10, 0);
    assertVirtualPinValue(arduino01.getVirtual(), 10, 0);

    arduino01.analogWrite(10, 127);
    assertVirtualPinValue(arduino01.getVirtual(), 10, 127);

    arduino01.analogWrite(10, 128);
    assertVirtualPinValue(arduino01.getVirtual(), 10, 128);

    arduino01.analogWrite(10, 255);
    assertVirtualPinValue(arduino01.getVirtual(), 10, 255);

    arduino01.error("test");
  }

  @Test
  public final void testConnect() throws IOException {
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
    catcher.clear();
    arduino01.connect(port01);
    arduino01.reset();

    if (arduino01.isVirtual()) {
      SerialDevice uart = arduino01.getVirtual().getSerial();
      uart.clear();
      uart.setTimeout(100);
    }

    catcher.clear();
  }

  @Test
  public void testConnectString() {
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
    arduino01.disconnect();
    // clear
    if (arduino01.isVirtual()) {
      arduino01.getVirtual().getSerial().clear();
    }

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

    org.myrobotlab.service.Test test = (org.myrobotlab.service.Test) Runtime.start("test", "Test");
    test.subscribe(arduino01.getName(), "publishBoardStatus");
    arduino01.enableBoardInfo(true);
    // FIXME notify with timeout
    arduino01.enableBoardInfo(false);
  }

  @Test
  public void testEnablePinInt() {
    // set board type
    arduino01.enablePin(analogPin);
    arduino01.attachPinArrayListener(catcher);
    sleep(50);
    assertTrue(catcher.containsPinArrayFromPin(analogPin));
    arduino01.disablePin(analogPin);
  }

  @Test
  public void testEnablePinString() {
    // set board type
    catcher.clear();
    arduino01.enablePin(analogPin);
    arduino01.attachPinArrayListener(catcher);
    sleep(50);
    assertTrue(catcher.containsPinArrayFromPin(analogPin));
    arduino01.disablePin(analogPin);
  }

  @Test
  public void testGetBoardInfo() {
    arduino01.connect(port01);
    BoardInfo boardInfo = arduino01.getBoardInfo();
    assertTrue(boardInfo.getVersion().intValue() == Msg.MRLCOMM_VERSION);
  }

  @Test
  public final void pinArrayTest() {
    catcher.clear();
    arduino01.connect(port01);

    activePins = new String[] { "D10", "D12", "D13" };
    arduino01.attachPinArrayListener((PinArrayListener) catcher);
    arduino01.enablePin(10);
    arduino01.enablePin(12);
    arduino01.enablePin(13);

    // Wait for pin enablement to complete
    sleep(200);
    arduino01.reset();
    // Wait for reset to complete
    sleep(200);

    assertTrue("did not get pin array data D10", catcher.containsPinArrayFromPin(arduino01.getPin(10).getPinName()));
    assertTrue("did not get pin array data D12", catcher.containsPinArrayFromPin(arduino01.getPin(12).getPinName()));
    assertTrue("did not get pin array data D13", catcher.containsPinArrayFromPin(arduino01.getPin(13).getPinName()));

  }

  @Test
  public final void testGetVersion() {
    catcher.clear();
    arduino01.connect(port01);
    assertEquals(Msg.MRLCOMM_VERSION, arduino01.getBoardInfo().getVersion().intValue());
  }

  @Test
  public final void testPinModeIntegerInteger() {
    catcher.clear();
    arduino01.pinMode(8, Arduino.OUTPUT);
    PinDefinition pd = arduino01.getPin(8);
    assertEquals(pd.getMode(), "OUTPUT");
  }

  @Test
  public final void testPinModeIntString() {
    catcher.clear();
    arduino01.pinMode(8, "OUTPUT");
    PinDefinition pd = arduino01.getPin(8);
    assertEquals(pd.getMode(), "OUTPUT");
  }

  @Test
  public void testReleaseService() {
    catcher.clear();
    arduino01.releaseService();
    // better re-start it
    arduino01 = (Arduino) Runtime.start("arduino", "Arduino");
  }

  @Test
  public final void testServo() throws Exception {
    catcher.clear();

    Servo servo = (Servo) Runtime.start("servo01", "Servo");
    arduino01.connect(port01);

    // TEST AUTO DETACH !!!!

    assertTrue("isConnected", arduino01.isConnected());

    // attach it
    servo.setPin(Integer.parseInt(servoPin01));
    servo.attach(arduino01);
    sleep(300); // wait for asynchronous creation over serial of an MrlComm
                // servo

    // get DeviceId
    DeviceMapping mapping = arduino01.deviceList.get("servo01");
    assertNotNull("verify arduino mapping exists in device list", mapping);

    MrlServo mrlservo = null;
    if (arduino01.isVirtual()) {
      Device device = arduino01.getVirtual().getDevice(mapping.getId());
      assertNotNull("verify virtual device exists", device);
      assertTrue("verify its a servo", device instanceof MrlServo);
      mrlservo = (MrlServo) device;
    }

    // verify its attached
    assertTrue("verify servo is not attached to arduino", servo.isAttached(arduino01));
    assertTrue("verify arduino is not attached to servo", arduino01.isAttached(servo));
    assertTrue("verify servo is not attached to arduino by name", servo.isAttached(arduino01.getName()));
    assertTrue("verify arduino is not attached to servo by name", arduino01.isAttached(servo.getName()));
    assertTrue("arduino is attached and contains a servo in device list", arduino01.getAttached().contains(servo.getName()));

    if (arduino01.isVirtual()) {
      assertNotNull(mrlservo);
      assertTrue("verifty virtual mrlservo is enabled", mrlservo.enabled);
    }

    // move it
    servo.moveToBlocking(30.0);
    if (arduino01.isVirtual()) {
      // FIXME -- fix blocking fix encoders
      sleep(500);
      assertNotNull(mrlservo);
      assertTrue("virtual servo moved blocking to 30", mrlservo.targetPosUs == Arduino.degreeToMicroseconds(30));
    }

    servo.moveTo(100.0);
    sleep(100);
    if (arduino01.isVirtual()) {
      assertNotNull(mrlservo);
      assertTrue("virtual servo moved to 100", mrlservo.targetPosUs == Arduino.degreeToMicroseconds(100));
    }

    // detach it
    arduino01.detach(servo);
    sleep(300); // wait for asynchronous removal of MrlServo
    assertFalse("verify servo is not attached to arduino", servo.isAttached(arduino01));
    assertFalse("verify arduino is not attached to servo", arduino01.isAttached(servo));
    assertFalse("verify servo is not attached to arduino by name", servo.isAttached(arduino01.getName()));
    assertFalse("verify arduino is not attached to servo by name", arduino01.isAttached(servo.getName()));
    if (arduino01.isVirtual()) {
      assertNull("verify device has been removed", arduino01.getVirtual().getDevice(mapping.getId()));
    }
    /*
     * assertFalse("verifty servo is disabled", servo.enabled()); if
     * (arduino01.isVirtual()) {
     * assertFalse("verifty virtual mrlservo is disabled", mrlservo.enabled); }
     */

    assertFalse(servo.isAttached(arduino01));

    // attach it the other way
    arduino01.attach(servo, Integer.parseInt(servoPin01));

    // verify its attached
    assertTrue(servo.isAttached(arduino01));
    assertTrue(arduino01.getAttached().contains(servo.getName()));

    // servo should have the correct pin
    assertTrue((servoPin01 + "").equals(servo.getPin()));

    // get its device id
    int deviceId = arduino01.getDeviceId(servo.getName());

    // get mrlcom's device id
    // virtualized tests
    MrlServo mrlServo = null;
    if (arduino01.isVirtual()) {
      Thread.sleep(100);
      mrlServo = (MrlServo) arduino01.getVirtual().getDevice(deviceId);
      // verify
      assertTrue(deviceId == mrlServo.id);
    }

    // can we enable to a different pin?
    /*
     * servo.enable(servoPin01 + 1 + ""); if (arduino01.isVirtual()) {
     * sleep(100); assertTrue(mrlServo.pin == Integer.parseInt(servoPin01 + 1));
     * assertTrue((mrlServo.pin + "").equals(servo.getPin())); }
     */

    double velocity = 50;
    // degree per second
    servo.setSpeed(velocity);
    if (arduino01.isVirtual()) {
      sleep(100);
      assertNotNull(mrlServo);
      assertTrue(mrlServo.velocity == velocity);
    }

    // attach to the correct pin again
    /*
     * servo.enable(servoPin01); servo.moveTo(30.0); servo.moveTo(130.0);
     * servo.moveTo(30.0); // assertEquals(virtual.servoMoveTo(130));
     * servo.rest();
     */

    assertTrue(servo.getController().contains(arduino01.getName()));

    servo.moveTo(0.0);
    // assertEquals(virtual.servoMoveTo(0));
    servo.moveTo(90.0);
    // assertEquals("servoWrite/7/90\n", uart.decode());
    servo.moveTo(180.0);
    // assertEquals("servoWrite/7/180\n", uart.decode());
    servo.moveTo(0.0);
    // assertEquals("servoWrite/7/0\n", uart.decode());

    // detach
    servo.detach();
    // assertNull("detach did not remove controller", servo.getController());
    // does not need to be null
    assertTrue(!servo.isAttached());

    // assertEquals("servoDetach/7/0\n", uart.decode());
    arduino01.attach(servo);
    // Service.sleep(500); // async attach
    log.info("{}", servo.getController());
    assertTrue("arduino did not attach to servo correctly", servo.getController().contains(arduino01.getName()));

    servo.moveTo(10.0);

    // re-attach
    servo.enable();
    assertTrue("servo should be enabled", servo.isEnabled());
    // assertEquals("servoAttach/7/9/5/115/101/114/118/111\n",
    // uart.decode());
    // // assertEquals(servoPin, servo.getPin().intValue());

    servo.moveTo(90.0);
    // assertEquals("servoWrite/7/90\n", uart.decode());

    arduino01.enableBoardInfo(true);

    servo.startService();

    servo.moveTo(90.0);

    // when we release a service - it should
    // notify and process releasing itself from attached
    // services
    servo.releaseService();
    Service.sleep(200); // async call removal of servo
    assertFalse(arduino01.getAttached().contains(servo.getName()));
    assertFalse(servo.isAttached(arduino01));
  }

  @Test
  public void testSetBoardMega() {
    catcher.clear();
    String boardType = arduino01.getBoard();
    arduino01.setBoardMega();
    assertEquals(Arduino.BOARD_TYPE_MEGA, arduino01.getBoard());
    List<PinDefinition> pins = arduino01.getPinList();
    assertEquals(70, pins.size());
    arduino01.setBoard(boardType);
  }

  @Test
  public void testSetBoardUno() {
    catcher.clear();
    String boardType = arduino01.getBoard();

    arduino01.setBoardUno();

    assertEquals(Arduino.BOARD_TYPE_UNO, arduino01.getBoard());

    List<PinDefinition> pins = arduino01.getPinList();
    assertEquals(20, pins.size());

    arduino01.setBoard(boardType);
  }

  @Test
  public void testPin() throws Exception {
    PinDefinition pin = arduino01.getPin(analogPin);

    catcher.clear();
    arduino01.setBoardUno();
    arduino01.isConnected();
    arduino01.connect(port01);

    catcher.setPin(analogPin);

    // arduino01.attachPinListener((PinListener) this, pin.getAddress());
    // arduino01.attach(catcher); // This will attach as a PinArrayListener
    // since it is higher priority in the Arduino
    arduino01.attachPinListener(catcher);

    arduino01.enablePin(pin.getAddress());
    sleep(200);
    arduino01.disablePin(pin.getAddress());
    assertNotNull("should not be null", catcher.pinData);
    assertNotNull("should not be null2", catcher.pinData.pin);
    assertTrue("did not receive pin data int", catcher.pinData.pin.equals(analogPin));

    catcher.pinData = null;
    catcher.setPin(analogPin);
    arduino01.attachPinListener(catcher);
    arduino01.enablePin(analogPin);
    sleep(100);
    assertTrue("did not receive pin data from pin", catcher.pinData.pin.equals(analogPin));

  }


  // FIXME - verify detach() detach(catcher) no pinData
  
}
