package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.Msg;
import org.myrobotlab.arduino.VirtualMsg;
import org.myrobotlab.arduino.virtual.Device;
import org.myrobotlab.framework.QueueStats;
import org.myrobotlab.framework.Service;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.data.SerialRelayData;
import org.myrobotlab.service.interfaces.MrlCommPublisher;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.SerialDataListener;

// @Ignore
public class VirtualArduinoTest extends AbstractServiceTest implements MrlCommPublisher, SerialDataListener {

  private Msg msg = new Msg(this, null);
  String testPort = "testPort";
  Serial serial = (Serial)Runtime.start("dteSerial", "Serial");
  private int numAcks = 0;

  @Override
  public Service createService() {
    Runtime.setLogLevel("info");
    VirtualArduino service = (VirtualArduino)Runtime.start("virtualArduino", "VirtualArduino");
    return service;
  }

  @Override
  public void testService() throws Exception {

    // for unit testing,
    msg.setInvoke(false);
    
    
    VirtualArduino va = (VirtualArduino)service;
    // there's a race condition... the virtual arduino service is starting up.. 
    // if we call connect too quickly.. it fails with a null pointer because the peer service for serial isn't started.
    Thread.sleep(1000);
    va.connect(testPort);

    // so at this point, we expect that we could do something with the virtual arduino.. like open a serial port to the non-uart side of the 
    // serial port and read some data coming back.
    Serial serial = (Serial)Runtime.start("serial", "Serial");
    serial.addByteListener(this);
    serial.connect(testPort);
    // Ok.. now we have a serial port..
    
    // wait for the msg to get the clear to send.
    Thread.sleep(2000);
    
    // lets flush an add servo message down the serial port
    
    
    serial.write(msg.servoAttach(0, 7, 180, -1, "s1"));
    
    Thread.sleep(2000);
    
    
    Device d = va.getDevice(0);
    // we should see some on bytes messages i would thinkg
    Thread.sleep(100000);
  }

  
  public void testServiceA() throws Exception {
    // our msg class shouldn't be invoking for the unit test
    msg.setInvoke(false);
    // our test service
    VirtualArduino va = (VirtualArduino)service;
    // for unit tests.. skip invoking. call directly.
    va.mrlComm.getMsg().setInvoke(false);
//    va.virtualMsg.setInvoke(false);
    // attach to the serial port for callbacks to this test.
    // connect the virtual arduino to the uart (DCE) port.
    va.connect(testPort);
    // Let's exercise a few things on the virtual arduino service.
    List<PinDefinition> pins = va.getPinList();
    assertTrue(pins.size() == 20);
    // This doesn't seem to work as expected.
    // how about we change the board?
    // va.setBoardMega();
    // pins = va.getPinList();
    // assertTrue(pins.size() == 70);
    va.disconnect();
    assertFalse(va.isConnected());
    // see that it can't connect to a null port
    va.connect(null);
    assertFalse(va.isConnected());
    // now get our local serial port to start reading data from the virtual arduinio
    serial.addByteListener(this);
    // connect our local serial port to the test port
    serial.connect(testPort);    
    // make sure after all that we can still connect to the virtual port.
    va.connect(testPort);
    assertTrue(va.isConnected());
    // we should be able to do a simple test that writes data to the uart.. and see it show up in the MrlCommIno script.
    // At this point what do we have.
    // Thread.sleep(1000);
    log.info("Writing a messge to attach a servo!");
    byte[] data = msg.servoAttach(0, 1, 0, 0, "s1");
    serial.write(data);
    serial.flush();
    // we probably have a race condition here.. but lets see.
    
    // the serial port needs to write to the port queue.. and the mrlcomm needs to be reading those bytes.
    
    Thread.sleep(1000);
    Device d = va.getDevice(0);
    assertNotNull(d);
    // i'd like to see an ack come back!
    
    
    // other stuff like..
    serial.write(msg.servoMoveToMicroseconds(0, 1976));
    // TODO: meaningful assert
    serial.write(msg.servoSetVelocity(0, 42));
    // TODO: meaningful assert
    serial.write(msg.servoMoveToMicroseconds(0, 2020));
    // TODO: meaningful assert

    //  Thread.sleep(1000);
    // TODO: this ack received needs to come back from the arduino service currently..
    // but it should be pushe down into the internals of the msg class
    // msg.ackReceived(0);
    serial.write(msg.servoMoveToMicroseconds(0, 2000));
    // TODO: Test various messages
    Thread.sleep(10000);
  }

  // These are all of the messages that the MrlComm/MrlCommIno can publish back to the arduino service.
  // none of these will get called unlesss this test gets the onBytes called that passes the returned stream down to the Msg.java onBytes.

  @Override
  public BoardInfo publishBoardInfo(Integer version, Integer boardTypeId, Integer microsPerLoop, Integer sram, Integer activePins, int[] deviceSummary) {
    return null;
  }

  @Override
  public void publishAck(Integer function) {
    numAcks ++;
    log.info("Publish Ack for function {}", VirtualMsg.methodToString(function));
    // TODO Auto-generated method stub
    // we got an ack from the virtual arduino .. acknoledge that in the mirror real msg parser.
   //  msg.ackReceived(function);
  }

  @Override
  public int[] publishCustomMsg(int[] msg) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String publishDebug(String debugMsg) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void publishEcho(float myFloat, int myByte, float secondFloat) {
    // TODO Auto-generated method stub

  }

  @Override
  public EncoderData publishEncoderData(Integer deviceId, Integer position) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void publishI2cData(Integer deviceId, int[] data) {
    // TODO Auto-generated method stub

  }

  @Override
  public SerialRelayData publishSerialData(Integer deviceId, int[] data) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Integer publishServoEvent(Integer deviceId, Integer eventType, Integer currentPos, Integer targetPos) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void publishMrlCommBegin(Integer version) {
    assertEquals(VirtualMsg.MRLCOMM_VERSION, version.intValue());
  }

  @Override
  public String publishMRLCommError(String errorMsg) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PinData[] publishPinArray(int[] data) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Integer publishUltrasonicSensorData(Integer deviceId, Integer echoTime) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void onBytes(byte[] data) {
    // It's the responsibility of the MrlCommPublisher to relay serial bytes  / events to the msg.java class
    log.info("VIRTUAL ARDUINO TEST ON BYTES!!!! {}", data);
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
    // TODO: should we test this?  it's the responsibility of the MrlCommPublisher to pass serial events to the msg class.
    msg.onConnect(portName);
  }

  @Override
  public void onDisconnect(String portName) {
    // TODO: should we test this? it's the responsibility of the MrlCommPublisher to pass serial events to the msg class.
    msg.onDisconnect(portName);
  }

  @Override
  public void ackTimeout() {
    // TODO: validate something...
    log.warn("Ack Timeout was seen!");
  }

  @Override
  public Object invoke(String method, Object... params) {
    log.warn("Don't invoke in a unit test!!!!!!");
    return null;
  }

}
