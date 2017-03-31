package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.codec.serial.HexCodec;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.Port;
import org.slf4j.Logger;

public class SerialTest {

  // TODO - https://github.com/junit-team/junit/wiki/Parameterized-tests
  // -
  // http://www.javaworld.com/article/2076265/testing-debugging/junit-best-practices.html
  public final static Logger log = LoggerFactory.getLogger(SerialTest.class);

  static Serial serial = null;
  static TestCatcher catcher = null;

  static VirtualDevice virtual = null;
  static Serial uart = null;
  static Python logic = null;
  static String vport = "vport";

  static Set<Thread> startThreads;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    LoggingFactory.init(Level.INFO);

    log.info("setUpBeforeClass");

    // Runtime.start("gui", "SwingGui");
    serial = (Serial) Runtime.start("serial", "Serial");
    catcher = (TestCatcher) Runtime.start("catcher", "TestCatcher");
    virtual = (VirtualDevice) Runtime.start("virtual", "VirtualDevice");
    virtual.createVirtualSerial(vport);

    uart = (Serial)virtual.getUart(vport);
    uart.setTimeout(300);
    Thread.sleep(100);
    serial.open(vport);
    Thread.sleep(300);

    startThreads = Runtime.getThreads();
  }

  public static Set<Thread> getDeadThreads() {
    Set<Thread> dead = new HashSet<Thread>();
    Set<Thread> current = Runtime.getThreads();
    for (Thread thread : startThreads) {
      if (!current.contains(thread)) {
        log.info(String.format("thread %s is dead", thread.getName()));
        dead.add(thread);
      }
    }
    return dead;
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {

    catcher.clear();
    catcher.isLocal = true;

    uart.clear();
    uart.setTimeout(300);

    serial.clear();
    serial.setTimeout(300);

    if (!serial.isConnected()) {
      serial.open(vport);
    }


    serial.addByteListener(catcher);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testGetCategories() {
    // assertTrue(serial.getCategories().length > 0);
  }

  @Test
  public final void testGetDescription() {
    assertTrue(serial.getDescription().length() > 0);
  }

  @Test
  public final void testStopService() {
    // fail("Not yet implemented"); // TODO
    // TODO thread count
  }

  // FIXME - remove
  @Test
  public final void testTest() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testBytesToInt() {
    log.info("testBytesToInt");

    int x = 0;

    // signed "biggest"
    // x = Serial.bytesToInt(new int[]{127, 255, 255, 255}, 0, 4);
    x = Serial.bytesToInt(new int[] { 127, 255, 255, 255 }, 0, 4);
    assertEquals(Integer.MAX_VALUE, x);

    x = Serial.bytesToInt(new int[] { 0, 0, 0, 255 }, 0, 4);
    log.info(String.format("%d", x));
    assertEquals(255, x);

    x = Serial.bytesToInt(new int[] { 0, 0, 0, 0 }, 0, 4);
    assertEquals(0, x);

    // Java is signed :P - good and bad
    x = Serial.bytesToInt(new int[] { 255, 255, 255, 255 }, 0, 4);
    assertEquals(-1, x);

    x = Serial.bytesToInt(new int[] { 0, 0, 1, 0 }, 0, 4);
    assertEquals(256, x);

    x = Serial.bytesToInt(new int[] { 0, 1, 0, 0 }, 0, 4);
    assertEquals(65536, x);

    x = Serial.bytesToInt(new int[] { 1, 0, 0, 0 }, 0, 4);
    assertEquals(16777216, x);

    /*
     * TODO DO RANGE TESTS :P x = Serial.bytesToInt(new int[]{1, 0, 1, 0}, 1,
     * 3); assertEquals(1, x);
     */

  }

  @Test
  public final void testAvailable() throws Exception, InterruptedException {
    log.info("testAvailable");

    serial.write(0);
    serial.write(127);
    serial.write(128);
    serial.write(255);

    Thread.sleep(100);

    assertEquals(4, uart.available());

    assertEquals(0, uart.read());
    assertEquals(127, uart.read());
    assertEquals(128, uart.read());
    assertEquals(255, uart.read());

    Set<Thread> names = getDeadThreads();
    log.info(names.size() + "");
  }

  @Test
  public final void testClear() throws Exception, InterruptedException {
    log.info("testClear");

    serial.write(0);
    serial.write(127);
    serial.write(128);
    serial.write(255);

    Thread.sleep(100);

    assertEquals(4, uart.available());
    uart.clear();
    assertEquals(0, uart.available());
  }

  @Test
  public final void testConnectString() throws InterruptedException, Exception {
    log.info("testConnectString");

    // ========== remote pub/sub connect / onByte testing ==========
    log.info("testing connect & disconnect for remote service");

    serial.addByteListener(catcher);

    if (serial.isConnected()) {
      serial.disconnect();
      catcher.checkMsg("onDisconnect", vport);
    }

    catcher.isLocal = false;

    serial.open(vport);
    catcher.checkMsg("onConnect", vport);

    testReadAndWrite();

    catcher.clear();
    serial.disconnect();

    serial.write(255);
    log.info("testing timeout");

    boolean expectedFailure = false;
    try {
      // timeout makes it throw
      uart.read();
    } catch (Exception e) {
      log.info("expected failure on timeout");
      expectedFailure = true;
    }

    assertTrue(expectedFailure);

    catcher.checkMsg("onDisconnect", vport);
    serial.removeByteListener(catcher);

    // ========== local pub/sub connect / onByte testing ==========
    log.info("testing connect & disconnect for local service");
    catcher.isLocal = true;

    serial.addByteListener(catcher);
    serial.open(vport);
    catcher.checkMsg("onConnect", vport);

    testReadAndWrite();

    serial.disconnect();
    catcher.checkMsg("onDisconnect", vport);
    serial.removeByteListener(catcher);
    serial.open(vport);
  }

  @Test
  public final void testReadAndWrite() throws Exception, InterruptedException {
    log.info("testReadAndWrite");

    // Set<Thread> names = getDeadThreads();

    logThreads();
    // serial.removeAllListeners();

    // serial --> uart
    serial.write(0);
    serial.write(127);
    serial.write(128);
    serial.write(255);

    Thread.sleep(300);
    assertEquals(0, uart.read());
    assertEquals(127, uart.read());
    assertEquals(128, uart.read());
    assertEquals(255, uart.read());

    // serial <-- uart
    uart.write(0);
    uart.write(127);
    uart.write(128);
    uart.write(255);

    Thread.sleep(300);
    assertEquals(0, serial.read());
    assertEquals(127, serial.read());
    assertEquals(128, serial.read());
    assertEquals(255, serial.read());

    catcher.clear();

  }

  public final void logThreads() {
    Set<Thread> current = Runtime.getThreads();
    String[] t = new String[current.size()];
    int i = 0;
    for (Thread thread : current) {
      t[i] = thread.getName();
      ++i;
    }

    Arrays.sort(t);
    for (i = 0; i < t.length; ++i) {
      log.info(String.format("thread %s", t[i]));
    }
  }

  @Test
  public final void testConnectVirtualNullModem() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testConnectVirtualUART() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testToString() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testBytesToLong() {
    int[] test;
    long x;

    test = new int[] { 0x00, 0x00, 0x00, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };
    x = Serial.bytesToLong(test, 0, 4);
    assertEquals(3, x);

    test = new int[] { 0xFF, 0x00, 0x00, 0x00, 0xFF, 0x00, 0x00, 0x00, 0x00 };
    x = Serial.bytesToLong(test, 3, 3);
    assertEquals(65280, x);

    test = new int[] { 0x00, 0x00, 0x00, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };
    x = Serial.bytesToLong(test, 0, 8);
    assertEquals(12952339975L, x);

    test = new int[] { 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF };
    x = Serial.bytesToLong(test, 0, 8);
    assertEquals(-1, x);

    /*
     * WTH? test = new int[]{0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
     * 0xFE}; x = Serial.bytesToLong(test, 0, 8); assertEquals(-1, x);
     */

    log.info("here");
  }

  @Test
  public final void testSerial() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testAddByteListenerSerialDataListener() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testAddByteListenerString() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetParams() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testConnectStringIntIntIntInt() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testConnectStringSerialDataListener() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testConnectFilePlayer() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testConnectLoopback() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testConnectPort() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testConnectTCP() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testCreateHardwarePort() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testCreateTCPPort() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testCreateVirtualPort() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testCreateVirtualUART() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testDisconnect() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetHardwareLibrary() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetListeners() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetPort() {
    Port port = serial.getPort();
    assertFalse(port.isHardware());
  }

  @Test
  public final void testGetPortName() throws IOException {
    log.info("testGetPortName");
    String portName = serial.getPortName();
    log.info(String.format("port name is %s", portName));
    assertEquals(vport, portName);
    serial.disconnect();
    portName = serial.getPortName();
    assertEquals(null, portName);
    serial.open(vport);
    portName = serial.getPortName();
    assertEquals(vport, portName);
  }

  @Test
  public final void testGetPortNames() {
    List<String> ports = serial.getPortNames();
    log.info(String.format("number of ports %d", ports.size()));
    // should only be 2 ports - 1 virtual & 1 virtual uart
    assertEquals(2, ports.size());
  }

  @Test
  public final void testGetPortSource() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetQueue() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetRXCodec() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetRXCodecKey() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetRXCount() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetTimeout() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetTXCodec() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetTXCodecKey() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testIsConnected() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testIsRecording() throws Exception {
    serial.record();
    assertTrue(serial.isRecording());
    int x = 65;
    serial.write(65);
    serial.stopRecording();
    assertFalse(serial.isRecording());

    String data = FileIO.toString("serial.tx.hex");
    HexCodec hex = new HexCodec(null);
    // DecimalCodec dec = new DecimalCodec(null);
    assertEquals(Integer.parseInt(hex.decode(x).trim()), Integer.parseInt(data.trim()));
  }

  @Test
  public final void testOnByte() {
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
  public final void testPublishConnect() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testPublishDisconnect() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testPublishPortNames() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testPublishRX() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testPublishTX() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testRead() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testReadByteArray() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testReadInt() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testReadIntArray() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testReadLine() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testReadLineChar() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testReadString() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testReadStringChar() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testReadStringInt() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testReadToDelimiter() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testRecord() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testRecordString() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testRecordRX() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testRecordTX() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testRefresh() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testRemoveByteListenerSerialDataListener() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testRemoveByteListenerString() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testReset() {
    serial.reset();
    assertEquals(0, serial.available());
  }

  @Test
  public final void testSetBufferSize() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetCodec() throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException,
      IllegalArgumentException, InvocationTargetException, Exception, InterruptedException {
    log.info("testSetCodec");

    boolean notready = true;
    if (notready) {
      return;
    }

 

    testReadAndWrite();

    // ==== decimal codec test ===
    // serial.setCodec("decimal");

  
    testReadAndWrite();

    // ==== hex codec test ===
 

    testReadAndWrite();

    // ==== ascii codec test ===
   

    testReadAndWrite();
  }

  @Test
  public final void testSetDTR() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetHardwareLibrary() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetRXCodec() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetTimeout() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetTXCodec() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testStopRecording() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testWriteByteArray() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testWriteInt() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testWriteIntArray() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testWriteString() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testWriteFile() {
    // fail("Not yet implemented"); // TODO
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.DEBUG);

      SerialTest.setUpBeforeClass();
      SerialTest test = new SerialTest();
      test.testBytesToLong();
      test.testConnectString();

      JUnitCore junit = new JUnitCore();
      Result result = junit.run(SerialTest.class);
      log.info("Result: {}", result);
      // WebGui gui = (WebGui) Runtime.start("webgui", "WebGui");
      // ServiceInterface gui = Runtime.start("gui", "SwingGui");

      Runtime.dump();

      log.info("here");
      // serial.removeByteListener(gui.getName());
      // uart.removeByteListener(gui.getName());

      Runtime.dump();

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
