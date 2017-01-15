package org.myrobotlab.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.VirtualMsg;
import org.myrobotlab.arduino.virtual.Device;
import org.myrobotlab.arduino.virtual.MrlComm;
import org.myrobotlab.arduino.virtual.MrlCommIno;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.PortQueue;
import org.myrobotlab.service.interfaces.RecordControl;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

public class VirtualArduino extends Service implements RecordControl {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(VirtualArduino.class);

  /**
   * The Java port of the MrlComm.ino script
   */
  transient MrlCommIno ino;

  transient MrlComm mrlComm;

  transient Serial uart;
  String portName = "COM42";

  // transient int[] ioCmd = new int[MAX_MSG_SIZE];

  transient VirtualMsg msg;
  BoardInfo boardInfo;

  /**
   * thread to run the script
   */
  transient InoScriptRunner runner = null;

  transient FileOutputStream record = null;
  // for debuging & developing - need synchronized - both send & recv threads
  transient StringBuffer recordRxBuffer = new StringBuffer();
  transient StringBuffer recordTxBuffer = new StringBuffer();

  /**
   * This class is a thread which runs a (port) of MrlComm.ino. It does what the
   * Arduino "OS" does .. It runs the "loop()" method forever..
   * 
   * @author GroG
   *
   */
  public static class InoScriptRunner extends Thread {
    boolean isRunning = false;
    VirtualArduino virtual;
    MrlCommIno ino;

    InoScriptRunner(VirtualArduino virtual, MrlCommIno ino) {
      super(String.format("%s.mrlcomm", virtual.getName()));
      this.virtual = virtual;
      this.ino = ino;
    }

    public void run() {
      isRunning = true;
      ino.setup();
      while (isRunning) {
        try {

          ino.loop();

          Thread.sleep(10);
        } catch (Exception e) {
          log.error("mrlcomm threw", e);
        }
      }
    }
  }

  public VirtualArduino(String n) {
    super(n);
    uart = (Serial) createPeer("uart");
    ino = new MrlCommIno(this);
    mrlComm = ino.getMrlComm();
    msg = mrlComm.getMsg();
    msg.setInvoke(false);
    boardInfo = mrlComm.boardInfo;
    boardInfo.setType(Arduino.BOARD_TYPE_ID_UNO);
  }

  public void connect(String portName) throws IOException {
    if (uart != null && uart.isConnected()) {
      log.info("already connected");
      return;
    }
    connectVirtualUart(portName, portName + ".UART");
  }

  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(VirtualArduino.class.getCanonicalName());
    meta.addDescription("virtual hardware of for the Arduino!");
    meta.setAvailable(true); // false if you do not want it viewable in a
    // gui
    meta.addPeer("uart", "Serial", "serial device for this Arduino");
    meta.addCategory("simulator");
    return meta;
  }

  // TODO - shouldn't this be part of Serial service ?
  public SerialDevice connectVirtualUart(String myPort, String uartPort) throws IOException {

    BlockingQueue<Integer> left = new LinkedBlockingQueue<Integer>();
    BlockingQueue<Integer> right = new LinkedBlockingQueue<Integer>();

    // add our virtual port
    PortQueue vPort = new PortQueue(myPort, left, right);
    Serial.ports.put(myPort, vPort);

    PortQueue uPort = new PortQueue(uartPort, right, left);
    uart.connectPort(uPort, uart);

    log.info(String.format("connectToVirtualUart - creating uart %s <--> %s", myPort, uartPort));
    return uart;
  }

  @Override
  public void record() throws Exception {
    if (record == null) {
      record = new FileOutputStream(String.format("%s.ard", getName()));
    }
  }

  @Override
  public void stopRecording() {
    if (record != null) {
      try {
        record.close();
      } catch (Exception e) {
      }
      record = null;
    }
  }

  @Override
  public boolean isRecording() {
    return record != null;
  }

  public String setBoard(String board) {
    log.info("setting board to type {}", board);

    ino.setBoardType(board);

    // createPinList();
    broadcastState();
    return board;
  }

  /*
   * @Override public String onConnect(String portName) { return portName; }
   * 
   * @Override public String onDisconnect(String portName) { return portName; }
   */

  public void start() {
    if (runner != null) {
      log.warn("running ino script already");
      return;
    }
    runner = new InoScriptRunner(this, ino);
    runner.start();
  }

  public void stop() {
    if (runner != null) {
      runner.isRunning = false;
      runner.interrupt();
      runner = null;
    }
  }

  /**
   * easy way to set to get a 54 pin arduino
   *
   * @return
   */
  public String setBoardMega() {
    return setBoard(Arduino.BOARD_TYPE_MEGA);
  }

  public String setBoardMegaADK() {
    return setBoard(Arduino.BOARD_TYPE_MEGA_ADK);
  }

  public String setBoardUno() {
    return setBoard(Arduino.BOARD_TYPE_UNO);
  }

  public void setPortName(String portName) {
    this.portName = portName;
  }

  @Override
  public void startService() {
    super.startService();
    uart = (Serial) startPeer("uart");
    // uart.addByteListener(this);
    start();
  }

  public SerialDevice getSerial() {
    return uart;
  }

  public org.myrobotlab.arduino.virtual.MrlComm MrlComm() {
    return ino.getMrlComm();
  }

  public Device getDevice(int deviceId) {
    return mrlComm.getDevice(deviceId);
  }

  public int readBlocking(int address, int i) {
    // TODO Auto-generated method stub
    return 0;
  }

  public void clearPinQueue(int address) {
    mrlComm.pinList.clear();
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init();

      String port = "COM5";
      Runtime.start("webgui", "WebGui");

      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      VirtualArduino varduino = (VirtualArduino) Runtime.start("varduino", "VirtualArduino");
      // connect the virtual uart
      // varduino.setPortName(port);

      // connect the arduino to the other end
      arduino.connect(port);
      arduino.enablePin(54);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
