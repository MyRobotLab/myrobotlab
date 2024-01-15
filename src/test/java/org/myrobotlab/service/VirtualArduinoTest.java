package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.Msg;
import org.myrobotlab.arduino.VirtualMsg;
import org.myrobotlab.arduino.virtual.Device;
import org.myrobotlab.arduino.virtual.MrlServo;
import org.myrobotlab.framework.QueueStats;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.StaticType;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.data.SerialRelayData;
import org.myrobotlab.service.interfaces.MrlCommPublisher;
import org.myrobotlab.service.interfaces.SerialDataListener;

// @Ignore
public class VirtualArduinoTest extends AbstractServiceTest implements MrlCommPublisher, SerialDataListener {

  private Msg msg = new Msg(this, null);
  String testPort = "testPort";
  Serial serial = (Serial) Runtime.start("dteSerial", "Serial");

  @Override
  public Service createService() {
    // Runtime.setLogLevel("info");
    VirtualArduino service = (VirtualArduino) Runtime.start("virtualArduino", "VirtualArduino");
    return service;
  }

  @Override
  public void testService() throws Exception {

    // our local msg handle that parses the stream from the virtual arduino
    // skip invoking in unit tests, instead directly call the callbacks to make
    // unit tests easier.
    msg.setInvoke(false);
    // our service to test
    VirtualArduino va = (VirtualArduino) service;
    // connect to the test uart/DCE port
    va.connect(testPort);
    // virtual arduino listens on virtual port + .UART as the DCE side of the
    // interface.
    assertEquals(testPort + ".UART", va.getPortName());
    // some basic validation stuff on the va..
    assertNotNull(va.getPinList());
    assertNotNull(va.board);

    Serial serial = (Serial) Runtime.start("serial", "Serial");
    // attach our test as a byte listener for the onBytes call.
    serial.addByteListener(this);
    // connect to the actual DTE side of the virtual serial port.
    serial.connect(testPort);
    Thread.sleep(100);
    // TODO: We should probably wait for the begin message to be seen from the
    // virtual arduino here before proceeding.
    // servo attach method being written to the DTE side of the virtual port
    serial.write(msg.servoAttach(0, 7, 180, -1, "s1"));
    // TODO: there's a race condition here.. we seem to fail without a small
    // sleep here!!!!
    // we need to let the virtual arduinos catch up and actually add the device.
    Thread.sleep(50);
    // get a handle to the virtual device that we just attached.
    Device d = va.getDevice(0);
    // validate the device exists.
    assertNotNull(d);
    // make sure the device is actually a servo
    assertEquals(Msg.DEVICE_TYPE_SERVO, d.type);
    serial.write(msg.servoMoveToMicroseconds(0, 1500));
    MrlServo s = (MrlServo) d;
    // TODO: there's a race condition here. it takes a moment for the virtual
    // device to respond.
    Thread.sleep(50);
    assertEquals(s.targetPosUs, 1500);
    serial.write(msg.servoSetVelocity(0, 22));
    Thread.sleep(50);
    assertEquals(s.velocity, 22);

    // other Servo methods to test.
    serial.write(msg.servoAttachPin(0, 11));
    Thread.sleep(50);
    assertEquals(11, s.pin);

    serial.write(msg.servoDetachPin(0));
    Thread.sleep(50);
    assertFalse(s.enabled);

    va.disconnect();
    Thread.sleep(50);
    assertFalse(va.isConnected());

    // TODO: we could test other virtual devices.
    // what else can we do?

  }

  // These are all of the messages that the MrlComm/MrlCommIno can publish back
  // to the arduino service.
  // none of these will get called unlesss this test gets the onBytes called
  // that passes the returned stream down to the Msg.java onBytes.

  @Override
  public BoardInfo publishBoardInfo(Integer version, Integer boardTypeId, Integer microsPerLoop, Integer sram, Integer activePins, int[] deviceSummary) {
    return null;
  }

  @Override
  public void publishAck(Integer function) {
    log.info("Publish Ack for function {}", VirtualMsg.methodToString(function));
  }

  @Override
  public int[] publishCustomMsg(int[] msg) {
    log.info("Publish Custom Msg: {}", msg);
    return msg;
  }

  @Override
  public String publishDebug(String debugMsg) {
    log.info("Publish Debug Message {}", debugMsg);
    return debugMsg;
  }

  @Override
  public void publishEcho(float myFloat, int myByte, float secondFloat) {
    log.info("Publish Echo myFloat:{} myByte:{} secondFloat:{}", myFloat, myByte, secondFloat);
  }

  @Override
  public EncoderData publishEncoderData(Integer deviceId, Integer position) {
    // mocked out encoder data to publish.
    log.info("Device ID:{} Position:{}", deviceId, position);
    EncoderData data = new EncoderData("Test-" + deviceId, null, new Double(position), new Double(position));
    return data;
  }

  @Override
  public void publishI2cData(Integer deviceId, int[] data) {
    log.info("Publish I2C - Device ID: {} Data: {}", deviceId, data);
  }

  @Override
  public SerialRelayData publishSerialData(Integer deviceId, int[] data) {
    log.info("Publish Serial Data: {} {}", deviceId, data);
    SerialRelayData srd = new SerialRelayData(deviceId, data);
    return srd;
  }

  @Override
  public Integer publishServoEvent(Integer deviceId, Integer eventType, Integer currentPos, Integer targetPos) {
    log.info("Publish Servo Event - DeviceId:{} EventType:{} CurrentPos:{} TargetPos:{}", deviceId, eventType, currentPos, targetPos);
    return currentPos;
  }

  @Override
  public void publishMrlCommBegin(Integer version) {
    assertEquals(VirtualMsg.MRLCOMM_VERSION, version.intValue());
  }

  @Override
  public String publishMRLCommError(String errorMsg) {
    log.warn("MRL Comm Error : {}", errorMsg);
    return errorMsg;
  }

  @Override
  public PinData[] publishPinArray(int[] data) {
    log.info("Publish Pin Array: {}", data);
    // TODO: this has some complex logic in Arduino to replicate/test here.
    return null;
  }

  @Override
  public Integer publishUltrasonicSensorData(Integer deviceId, Integer echoTime) {
    log.info("Publish Ultrasonice Sensor Data: {} {}", deviceId, echoTime);
    return echoTime;
  }

  @Override
  public void onBytes(byte[] data) {
    // It's the responsibility of the MrlCommPublisher to relay serial bytes /
    // events to the msg.java class
    msg.onBytes(data);
  }

  @Override
  public QueueStats publishStats(QueueStats stats) {
    // TODO NoOp in the unit test for now.
    return stats;
  }

  @Override
  public void updateStats(QueueStats stats) {
    // TODO: NoOp in the unit test for now.
  }

  @Override
  public void onConnect(String portName) {
    // TODO: should we test this? it's the responsibility of the
    // MrlCommPublisher to pass serial events to the msg class.
    msg.onConnect(portName);
  }

  @Override
  public void onDisconnect(String portName) {
    // TODO: should we test this? it's the responsibility of the
    // MrlCommPublisher to pass serial events to the msg class.
    msg.onDisconnect(portName);
  }

  @Override
  public void ackTimeout() {
    // TODO: validate something...
    log.warn("Ack Timeout was seen!!!!");
  }

  @Override
  public <R> R invoke(String method, StaticType<R> returnType, Object... params) {
    log.warn("Don't invoke in a unit test!!!!!!");
    return null;
  }

  @Override
  public boolean isConnecting() {
    // TODO Auto-generated method stub
    return false;
  }

}
