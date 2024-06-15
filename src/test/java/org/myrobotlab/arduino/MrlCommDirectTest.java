package org.myrobotlab.arduino;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.myrobotlab.framework.QueueStats;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.serial.PortJSSC;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.data.SerialRelayData;
import org.myrobotlab.service.interfaces.MrlCommPublisher;
import org.myrobotlab.service.interfaces.PortListener;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.string.StringUtil;
import org.slf4j.Logger;

// @Ignore
public class MrlCommDirectTest implements SerialDataListener, MrlCommPublisher, PortListener {

  transient public final static Logger log = LoggerFactory.getLogger(MrlCommDirectTest.class);
  public Msg msg = null;
  private int numAcks = 0;

  String portName = "COM4";
  int rate = 115200;
  int dataBits = 8;
  int stopBits = 1;
  int parity = 0;

  @Test
  public void testMrlCommBegin() throws Exception {
    msg = new Msg(this, null);
    msg.setInvoke(false);
    assertFalse(msg.isClearToSend());
    // now we want to just see how it responds when i send it various byte
    // sequences.
    // byte[] testBytes = new byte[] {-86,14,1};
    byte[] testBytes = createTestBytes("170,2," + Msg.PUBLISH_MRL_COMM_BEGIN + ",63");
    msg.onBytes(testBytes);
    // msg.waitForBegin();
    // now what?
    assertTrue(msg.isClearToSend());
    // Thread.sleep(1000);
  }

  // @Test
  public void testRealWorldError() {
    String testMsg1 = "170,9,3,63,1,0,27,25,77,0,0,0,1,0,27,25,77,0,0,170,9,3,63,1,0,27,25,77,0,0,170,9,3,63,1,0,27,25,77,0,0,170,9,3,63,1,0,27,25,77,0,0,170,9,3,63,1,0,27,25,77,0,0,170,9,3,63,1,0,27,25,77,0,0,170,9,3,63,1,0,27,25,77,0,0,170,9,3,63,1,0,27,25,77,0,0,170,9,3,63,1,0,27,25,77,0,0,170,9,3,63,1,0,27,25,77,0,0,170,9,3,63,1,0,27,25,77,0,0,170,9,3,63,1,0,27,25,77,0,0,170,14";
    String testMsg2 = "170,2,55,63";

    msg = new Msg(this, null);
    msg.setInvoke(false);
    assertFalse(msg.isClearToSend());
    byte[] testBytes = createTestBytes(testMsg1);
    msg.onBytes(testBytes);
    // and the next batch up..

    msg.onBytes(createTestBytes(testMsg2));
    // msg.waitForBegin();
    // now what?
    assertTrue(msg.isClearToSend());

  }

  @Test
  public void testRealWorldError2() {
    String testMsg1 = "170,14,1,12,101,255";
    String testMsg2 = "170,2," + Msg.PUBLISH_MRL_COMM_BEGIN + ",63";
    msg = new Msg(this, null);
    msg.setInvoke(false);
    assertFalse(msg.isClearToSend());
    msg.onBytes(createTestBytes(testMsg1));
    msg.onBytes(createTestBytes(testMsg2));

    msg.onBytes(createTestBytes("0,0,0,0,0,0"));
    assertTrue(msg.isClearToSend());
  }

  private static byte[] createTestBytes(String intString) {
    // we are assuming an input string of integers like "170,2,55,63" for
    // example..
    // and we'll return the representative byte array
    String[] parts = intString.split(",");
    int i = 0;
    byte[] testBytes = new byte[parts.length];
    for (String p : parts) {
      Integer val = Integer.valueOf(p);
      testBytes[i] = val.byteValue();
      i++;
    }
    return testBytes;
  }

  // @Test
  public void testMrlCommReconnect2() throws Exception {
    msg = new Msg(this, null);
    msg.setInvoke(false);
    PortJSSC port = new PortJSSC(portName, rate, dataBits, stopBits, parity);
    port.listen(this);
    for (int i = 0; i < 100; i++) {
      port.open();
      onConnect(portName);
      port.close();
      onDisconnect(portName);
    }
  }

  // @Test
  public void testMrlCommReconnect() throws Exception {
    msg = new Msg(this, null);
    msg.setInvoke(false);
    PortJSSC port = new PortJSSC(portName, rate, dataBits, stopBits, parity);
    port.listen(this);
    port.open();
    // TODO: wire in the onConnect as a proper synchronous callback.
    onConnect(portName);
    Thread.sleep(1100);
    for (int i = 0; i < 100; i++) {
      port.close();
      onDisconnect(portName);
      Thread.sleep(1100);
      port.open();
      // TODO: wire in the onConnect as a proper synchronous callback.
      onConnect(portName);
      Thread.sleep(100);
      for (int j = 0; j < 1000; j++) {
        port.write(msg.digitalWrite(1, j % 2));
      }
      Thread.sleep(100);

    }

  }

  //
  public void testMrlComm() throws Exception {
    msg = new Msg(this, null);
    msg.setInvoke(false);
    PortJSSC port = new PortJSSC(portName, rate, dataBits, stopBits, parity);
    port.listen(this);
    port.open();
    // TODO: wire in the onConnect as a proper synchronous callback.
    onConnect(portName);
    // msg.waitForBegin();
    System.out.println("################ Done #######################################");
    Thread.sleep(1000);
    // ok.. now after a second we'll attach a servo.
    Thread.sleep(1000);
    for (int j = 2; j < 12; j++) {
      port.write(msg.servoAttach(0, 7, 90, 100, "servo1"));
    }

    for (int j = 2; j < 12; j++) {
      port.write(msg.servoAttach(0, 7, 90, 100, "servo1"));
      for (int i = 1000; i < 2000; i++) {
        port.write(msg.servoMoveToMicroseconds(0, i));
      }
    }
    Thread.sleep(1000);

    // port.close();

    // Thread.sleep(2000);
    // ort.setDTR(false);
    // port.setDTR(true);

    // for (int i = 1; i <= 10; i++) {
    // System.err.println("\nLoop Test Number:" + i + "\n");
    // log.info("Closing port {}", i);
    // port.close();
    // Thread.sleep(1000);
    //
    //
    // log.info("Opening Port");
    // port.open();
    // // TODO: wire in the onConnect as a proper synchronous callback.
    // onConnect(portName);
    // Thread.sleep(1000);
    //
    // // send some commands to the port.
    // port.write(msg.servoAttach(0, 7, 90, 100, "servo1"));
    //
    //
    // Thread.sleep(1000);
    //
    //
    // }
    // waitForAnyKey();
    // wait for mrl to be sync'd

    log.info("MRL Begin.. we can send data now.");
    // How about now we try sending various mrl comm messages.
    // while (true) {
    //
    // for (int i = 2; i < 12; i++) {
    // port.write(msg.servoAttach(i-2, i, 0, 100, "servo"+i));
    // }
    //
    // for (int i = 2; i < 12; i++) {
    // port.write(msg.servoMoveToMicroseconds(0, 1200+i));
    // }
    // Thread.sleep(2000);
    // }
  }

  private void waitForAnyKey() throws IOException {
    System.out.println("Press the any key to continue..");
    System.out.flush();
    System.in.read();
  }

  @Override
  public String getName() {
    return "test";
  }

  @Override
  public QueueStats publishStats(QueueStats stats) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void updateStats(QueueStats stats) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onConnect(String portName) {
    // TODO Auto-generated method stub
    log.info("On connect called for port {}", portName);
    msg.onConnect(portName);

  }

  @Override
  public void onDisconnect(String portName) {
    // TODO Auto-generated method stub
    log.info("On Disconnect called for {}", portName);
    // msg.onDisconnect(portName);

  }

  @Override
  public void onBytes(byte[] bytes) {
    // TODO Auto-generated method stub
    log.info("OnBytes Called: {}", StringUtil.byteArrayToIntString(bytes));
    // Here is where we need the msg object to read the bytes and process any
    // callbacks that it can.
    msg.onBytes(bytes);
    log.info("Msg processed on bytes.");

  }

  @Override
  public BoardInfo publishBoardInfo(Integer version, Integer boardTypeId, Integer microsPerLoop, Integer sram, Integer activePins, int[] deviceSummary) {
    // TODO Auto-generated method stub
    log.info("Publish Board Info");
    return null;
  }

  @Override
  public void publishAck(Integer function) {
    // if we get an ack.. the msg object isn't pending anymore.
    numAcks++;
    // msg.ackReceived(function);
    // //msg.pendingMessage = false;
    // System.err.println("Publish Ack " + numAcks + "function:" + function );
    // log.info("Publish Ack {}", function);
    // System.err.println("Publish Ack: " + function);
  }

  @Override
  public int[] publishCustomMsg(int[] msg) {
    // TODO Auto-generated method stub
    log.info("Publish Custom Message");
    return msg;
  }

  @Override
  public String publishDebug(String debugMsg) {
    // TODO Auto-generated method stub
    log.info("Publish Debug : {}", debugMsg);
    return debugMsg;
  }

  @Override
  public void publishEcho(float myFloat, int myByte, float secondFloat) {
    // TODO Auto-generated method stub
    log.info("Publish Echo");
  }

  @Override
  public EncoderData publishEncoderData(Integer deviceId, Integer position) {
    // TODO Auto-generated method stub
    log.info("Publish Encoder Data Device:{} Position:{}", deviceId, position);
    return null;
  }

  @Override
  public void publishI2cData(Integer deviceId, int[] data) {
    // TODO Auto-generated method stub
    log.info("Publish I2C data");
  }

  @Override
  public SerialRelayData publishSerialData(Integer deviceId, int[] data) {
    // TODO Auto-generated method stub
    log.info("Publish Serial Data");
    return null;
  }

  @Override
  public Integer publishServoEvent(Integer deviceId, Integer eventType, Integer currentPos, Integer targetPos) {
    // TODO Auto-generated method stub
    log.info("Publish Servo Event - Device:{} EventType:{}, CurrentPosition:{} TargetPosition:{}", deviceId, eventType, currentPos, targetPos);
    return null;
  }

  @Override
  public void publishMrlCommBegin(Integer version) {
    // on begin, we know that we have just connected to mrlcomm!
    // we need to make sure that we are sync'd.
    // but at this point.. we are clear to send messages
    // because mrlcomm has started.. so .. let's say clear to send.
    log.info("Publish MRL Comm Begin");
    // msg.clearToSend = true;
    System.err.println("\nPublish MrlBegin: " + version + "\n");
  }

  @Override
  public String publishMRLCommError(String errorMsg) {
    // TODO any error probably is grounds for recycling the serial port.
    log.info("Publish MRLComm Error: {}", errorMsg);
    return errorMsg;
  }

  @Override
  public PinData[] publishPinArray(int[] data) {
    // TODO Auto-generated method stub
    log.info("Publish Pin Array");
    return null;
  }

  @Override
  public Integer publishUltrasonicSensorData(Integer deviceId, Integer echoTime) {
    // TODO Auto-generated method stub
    log.info("Publish Ultrasonic Sensor Data");
    return null;
  }

  @Override
  public void ackTimeout() {
    // TODO Auto-generated method stub
    log.warn("Ack Timeout seen!");
  }

  @Override
  public Object invoke(String method, Object... params) {
    log.warn("Dont invoke in a unit test!!!!!!!!!!!!!!!!!!!!!!");
    return null;
  }

  @Override
  public boolean isConnecting() {
    // TODO Auto-generated method stub
    return false;
  }

}
