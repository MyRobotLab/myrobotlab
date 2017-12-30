package org.myrobotlab.service;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class EddieControlBoardTest {

  EddieControlBoard ecb = (EddieControlBoard) Runtime.start("ecb", "EddieControlBoard");

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
  public final void testEddieControlBoard() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testConnect() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetAnalogValues() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetBatteryLevel() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetGPIOHighValues() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetGPIOInputs() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetGPIOLowValues() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetGPIOOutputs() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetHwVersion() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetPingValues() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetVersion() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGo() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testOnButton() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testOnKey() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testOnRY() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testOnY() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testPublishSensors() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testRead() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSayBatterLevel() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSendCmd() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSendCommand() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetMotorSpeed() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testStartJoystick() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testStartRemoteAdapter() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testStartSensors() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testStartWebGUI() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testStop() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testStopSensors() {
    // fail("Not yet implemented"); // TODO
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
  public final void test() throws IOException {

    // Runtime.start("gui", "SwingGui");
    /*
     * need virtual Arduino Serial uart = ecb.serial.createVirtualUART();
     * uart.write("011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 00A CCE\r"); ecb.startSensors(); //
     * ecb.connect(port) uart.write("011 011 011 004 004 004 004 CBB\r");
     * uart.write("011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); uart.write(
     * "011 011 011 004 004 004 004 CBB\r"); // ecb.go(1, 1);
     * 
     */

  }

}
