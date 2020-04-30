package org.myrobotlab.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.Msg;
import org.myrobotlab.arduino.VirtualMsg;
import org.myrobotlab.framework.QueueStats;
import org.myrobotlab.framework.Service;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.data.SerialRelayData;
import org.myrobotlab.service.interfaces.MrlCommPublisher;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.SerialDataListener;

// @Ignore
public class VirtualArduinoTest extends AbstractServiceTest implements MrlCommPublisher, SerialDataListener, Runnable {

  private Msg msg = new Msg(this, null);
  private Thread portReaderThread = null;
  String testPort = "testPort";
  Serial serial = (Serial)Runtime.start("dteSerial", "Serial");

  @Override
  public Service createService() {
    Runtime.setLogLevel("info");

    // Runtime.start("gui", "SwingGui");
    // First thing, set up our serial port before we start the virtual arduino
    // TODO: there is a race condition here.. we need to listen before the virtual arduino starts.
    portReaderThread = new Thread(this, "VirtualArduinoTest.portReaderThread");
    portReaderThread.start();

    // TODO Auto-generated method stub
    VirtualArduino service = (VirtualArduino)Runtime.start("virtualArduino", "VirtualArduino");
    return service;
  }

  @Override
  public void testService() throws Exception {
    VirtualArduino va = (VirtualArduino)service;
    // attach to the serial port for callbacks to this test.
    // Ok.. now what ?  i mean.. what the heck can a virtual arduino do?  
    // It should be able to read and write bytes to a uart.  (com port)
    log.info("About to connect");
    // the virtual arduino service should respond to some bytes being written to it's uart.
    // but first thing is to test.. if we "connect" to the virtual arduino.. does the uart respond with hello.
    // connect the virtual arduino to the uart port.
    va.connect(testPort);
    
    // Let's exercise a few things on the virtual arduino service.
    List<PinDefinition> pins = va.getPinList();
    assertTrue(pins.size() > 0);
    
    va.disconnect();
    
    assertFalse(va.isConnected());
    
    // see that it can't connect to a null port
    va.connect(null);
    assertFalse(va.isConnected());
    // make sure after all that we can still connect to the virtual port.
    va.connect(testPort);
    assertTrue(va.isConnected());
    
    // connect our local serial port to the test port
    serial.connect(testPort);
    // we should be able to do a simple test that writes data to the uart.. and see it show up in the MrlCommIno script.
    // At this point what do we have.
//     Thread.sleep(1000);
    log.info("Writing a messge to attach a servo!");
    byte[] data = msg.servoAttach(0, 1, 0, 0, "s1");
    serial.write(data);
    // i'd like to see an ack come back!
  //  Thread.sleep(1000);
    // TODO: this ack received needs to come back from the arduino service currently..
    // but it should be pushe down into the internals of the msg class
    // msg.ackReceived(0);
    serial.write(msg.servoMoveToMicroseconds(0, 2000));
    System.out.println("Waiting... for what I have no idea.");
    Thread.sleep(1000);
  }

  // These are all of the messages that the MrlComm/MrlCommIno can publish back to the arduino service.
  // none of these will get called unlesss this test gets the onBytes called that passes the returned stream down to the Msg.java onBytes.

  @Override
  public BoardInfo publishBoardInfo(Integer version, Integer boardTypeId, Integer microsPerLoop, Integer sram, Integer activePins, int[] deviceSummary) {
    return null;
  }

  @Override
  public void publishAck(Integer function) {
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
    // TODO Auto-generated method stub

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
    // TODO relay the bytes back to the Arduino side real Msg.java ? Is this right?
    log.info("On Bytes Virtual Arduino Test : {}", data);
    msg.onBytes(data);
  }

  @Override
  public QueueStats publishStats(QueueStats stats) {
    // TODO Auto-generated method stub
    // NoOp in the unit test for now.
    return null;
  }

  @Override
  public void updateStats(QueueStats stats) {
    // TODO Auto-generated method stub
    // NoOp in the unit test for now.    
  }

  @Override
  public void onConnect(String portName) {
    // TODO : here is where we should initialize the Msg.java parser.
    // cascade the message down to our parser.
    // log.info("ON CONNECT IN THE TEST!");
    // msg.onConnect(portName);

  }

  @Override
  public void onDisconnect(String portName) {
    // TODO add onDisconnect to the Msg.java class.
    // msg.onDisconnect(portName);
  }

  @Override
  public void run() {
    // TODO Auto-generated method stub
    log.info("Starting the port reader thread.");

    try {
      serial.connect(testPort);
      // TODO: figure out why we don't get our clear to send setup.
      // msg.clearToSend = true;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      log.error("Failed to connect to a virtual serila port!?", e);
      return;
    }

    while (true) {
      byte[] data = null;
      try {
        data = serial.readBytes();
        //log.info("Called read bytes on serial. port:{} data:{}", serial.getPortName(), data);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if (data != null) {
        onBytes(data);
      } else {
        // TODO: avoid cpu burn here.
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

  }

  @Override
  public void ackTimeout() {
    // TODO: validate something...
    log.warn("Ack Timeout was seen!");
    
  }

}
