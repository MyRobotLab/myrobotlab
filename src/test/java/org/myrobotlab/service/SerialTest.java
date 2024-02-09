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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.codec.serial.HexCodec;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.serial.Port;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class SerialTest extends AbstractTest {

  static TestCatcher catcher = null;

  // TODO - https://github.com/junit-team/junit/wiki/Parameterized-tests
  // -
  // http://www.javaworld.com/article/2076265/testing-debugging/junit-best-practices.html
  public final static Logger log = LoggerFactory.getLogger(SerialTest.class);
  static Python logic = null;

  static Serial serial = null;
  static Set<Thread> startThreads;
  static Serial uart = null;
  static String vport = "vport";

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

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // LoggingFactory.init("WARN");
    Runtime.getInstance().setVirtual(true);

    log.info("setUpBeforeClass");

    // Runtime.start("gui", "SwingGui");
    serial = (Serial) Runtime.start("serial", "Serial");
    catcher = (TestCatcher) Runtime.start("catcher", "TestCatcher");
    Thread.sleep(100);
    serial.connect(vport);
    uart = (Serial) Runtime.getService(vport + ".UART");
    uart.setTimeout(300);
    Thread.sleep(300);

    startThreads = Runtime.getThreads();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
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
    catcher.clear();

    if (serial.isConnected()) {
      serial.disconnect();
      // catcher.checkMsg("onDisconnect", vport); is invalid
      // the message travels directly - so you must see if the method
      // was called directly
      catcher.verifyCallback("onDisconnect", vport);
    }

    catcher.isLocal = false;
    catcher.clear();
    serial.open(vport);
    // catcher.checkMsg("onConnect", vport);
    catcher.verifyCallback("onConnect", vport);//

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

    // catcher.checkMsg("onDisconnect", vport);
    catcher.verifyCallback("onDisconnect", vport);
    serial.removeByteListener(catcher);

    // ========== local pub/sub connect / onByte testing ==========
    log.info("testing connect & disconnect for local service");
    catcher.isLocal = true;

    serial.addByteListener(catcher);
    serial.open(vport);
    // catcher.checkMsg("onConnect", vport);
    catcher.verifyCallback("onConnect", vport);
    
    testReadAndWrite();

    catcher.clear();
    serial.disconnect();
    //catcher.checkMsg("onDisconnect", vport);
    catcher.verifyCallback("onDisconnect", vport);
    serial.removeByteListener(catcher);
    serial.open(vport);
  }

  @Test
  public final void testGetDescription() {
    assertTrue(serial.getDescription().length() > 0);
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
    // assertGreater(3,4);
    assertTrue(2 <= ports.size());
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

  @Test
  public final void testReset() {
    serial.reset();
    assertEquals(0, serial.available());
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

}