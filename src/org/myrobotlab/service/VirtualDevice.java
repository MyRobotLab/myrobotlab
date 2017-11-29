package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.PortQueue;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

/**
 * 
 * VirtualDevice - This is a virtual serial port device that can be used to
 * redirect serial data over a network for example. Blender service requires
 * this so the serial commands to an inmoov and be pumped over the network to
 * blender, rather than over the serial port to an actual arduino.
 *
 */
public class VirtualDevice extends Service implements SerialDataListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(VirtualDevice.class);

  /**
   * uarts - the serial endpoints for thing which need testing over
   * serial connections
   */
  transient HashMap<String, Serial> uarts = new HashMap<String, Serial>();
  
  /**
   * the logic to control all virtual devices
   */
  transient Python logic;
  
  transient BlockingQueue<Message> msgs = new LinkedBlockingQueue<Message>();

  public VirtualDevice(String n) {
    super(n);
    logic = (Python) createPeer("logic");
  }

  public void startService() {
    super.startService();
    logic = (Python) startPeer("logic");
  }

  public Python getLogic() {
    return logic;
  }

  public Serial getUart(String portName) {
    return uarts.get(portName);
  }

  public HashMap<String, Serial> getUarts() {
    return uarts;
  }

  public void createVirtualSerial(String portName) throws IOException {

    // first create and connect on the virtual UART side
    connectVirtualUart(portName, null);
    // return uart.connectVirtualNullModem(portName);
  }

  public void createVirtualArduino(String portName) throws IOException {
    createVirtualSerial(portName);
    String newCode = FileIO.resourceToString("VirtualDevice/Arduino.py");
    log.info(newCode);
    logic.openScript("Arduino.py", newCode);
    logic.exec(newCode);
  }

  /*
   * connecting to a virtual UART allows a Serial service to interface with a
   * mocked hardware. To do this a Serial service creates 2 stream ports and
   * twists the virtual cable between them.
   * 
   * A virtual port is half a virtual pipe, and if unconnected - typically is
   * not very interesting...
   * 
   */

  public SerialDevice connectVirtualUart(String myPort, String uartPort) throws IOException {

    // get port names
    if (myPort == null) {
      myPort = getName();
    }

    if (uartPort == null) {
      uartPort = String.format("%s_uart", myPort);
    }

    BlockingQueue<Integer> left = new LinkedBlockingQueue<Integer>();
    BlockingQueue<Integer> right = new LinkedBlockingQueue<Integer>();

    // create & connect virtual uart
    Serial uart = (Serial) Runtime.start(uartPort, "Serial");

    // add our virtual port
    PortQueue vPort = new PortQueue(myPort, left, right);
    Serial.ports.put(myPort, vPort);

    PortQueue uPort = new PortQueue(uartPort, right, left);
    uart.connectPort(uPort, uart);

    // add the uart connected to my port
    uarts.put(myPort, uart);

    log.info(String.format("connectToVirtualUart - creating uart %s <--> %s", myPort, uartPort));
    return uart;
  }

  public SerialDevice createVirtualUart() throws IOException {
    return connectVirtualUart(null, null);
  }

  @Override
  public Integer onByte(Integer b) throws IOException {
    log.info("{}.onByte {}", getName(), b);
    return null;
  }

  @Override
  public void onConnect(String portName) {
    log.info("{}.onConnect {}", getName(), portName);
  }

  @Override
  public void onDisconnect(String portName) {
    log.info("{}.onDisconnect {}", getName(), portName);
  }

  /*
   * preProcessHook is used to intercept messages and process or route them
   * before being processed/invoked in the Service.
   * 
   * 
   *           @see
   *           org.myrobotlab.framework.Service#preProcessHook(org.myrobotlab.
   *           framework.Message)
   */
  @Override
  public boolean preProcessHook(Message msg) {
    try {
      msgs.put(msg);
      // log.info(String.format("%d msg %s ", msgs.size(), msg));
    } catch (Exception e) {
      Logging.logError(e);
    }
    return false;
  }

  public void clear() {
    // data.clear();
    msgs.clear();
  }

  public BlockingQueue<Message> getMsgs() {
    return msgs;
  }

  public Message getMsg(long timeout) throws InterruptedException {
    Message msg = msgs.poll(timeout, TimeUnit.MILLISECONDS);
    return msg;
  }

  public ArrayList<Message> waitForMsgs(int count) throws InterruptedException, IOException {
    return waitForMsgs(count, 1000, 100);
  }

  public ArrayList<Message> waitForMsgs(int count, int timeout) throws InterruptedException, IOException {
    return waitForMsgs(count, timeout, 100);
  }

  public ArrayList<Message> waitForMsgs(int count, int timeout, int pollInterval) throws InterruptedException, IOException {
    ArrayList<Message> ret = new ArrayList<Message>();
    long start = System.currentTimeMillis();
    long now = start;

    while (ret.size() < count) {
      now = System.currentTimeMillis();
      Message msg = msgs.poll(pollInterval, TimeUnit.MILLISECONDS);
      if (msg != null) {
        ret.add(msg);
      }
      if (now - start > timeout) {
        String error = String.format("waited %d ms received %d messages expecting %d in less than %d ms", now - start, ret.size(), count, timeout);
        log.error(error);
        throw new IOException(error);
      }
    }

    log.info(String.format("returned %d msgs in %s ms", ret.size(), now - start));
    return ret;
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(VirtualDevice.class.getCanonicalName());
    meta.addDescription("a service which can create virtual serial ports and behaviors implemented in python for them");
    meta.addCategory("testing");
    // put peer definitions in
    meta.addPeer("uart", "Serial", "uart");
    meta.addPeer("logic", "Python", "logic to implement");
    
    // this is used for testing, and does not need to be tested
    meta.setAvailable(false);

    return meta;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      String portName = "vport";
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      // Serial serial = arduino.getSerial();

      VirtualDevice virtual = (VirtualDevice) Runtime.start("virtual", "VirtualDevice");
      virtual.createVirtualArduino(portName);

      arduino.setBoardMega();
      arduino.connect(portName);

      // Runtime.start("gui", "SwingGui");
      Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
