package org.myrobotlab.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.Msg;
import org.myrobotlab.arduino.VirtualMsg;
import org.myrobotlab.arduino.virtual.Device;
import org.myrobotlab.arduino.virtual.MrlComm;
import org.myrobotlab.arduino.virtual.MrlCommIno;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PortConnector;
import org.myrobotlab.service.interfaces.PortListener;
import org.myrobotlab.service.interfaces.PortPublisher;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

/**
 * Virtual Arduino Simulator... It emulates the Arduino, but we also try to
 * maintain the internal state the Arduino would (at least on a software
 * level)...
 * 
 * @author GroG
 *
 */
public class VirtualArduino extends Service implements PortPublisher, PortListener, PortConnector, SerialDataListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(VirtualArduino.class);

  /**
   * The Java port of the MrlComm.ino script
   */
  transient MrlCommIno ino;

  transient MrlComm mrlComm;

  /**
   * Blender, JMonkey, other ...
   */
  //transient Simulator simulator;

  /**
   * our emulated electronic UART
   */

  transient Serial uart;

  /**
   * the unique board type key
   */
  String board;
  String aref;

  /**
   * address index of pinList
   */
  Map<Integer, PinDefinition> pinIndex = null;

  /**
   * name index of pinList
   */
  Map<String, PinDefinition> pinMap = null;

  String portName = "COM42";

  transient VirtualMsg virtualMsg;

  /**
   * should be ui widgetized
   */
  BoardInfo boardInfo;

  /**
   * thread to run the script
   */
  transient InoScriptRunner runner;

  transient FileOutputStream record = null;

  /**
   * This class is a thread which runs a (port) of MrlComm.ino. It does what the
   * Arduino "OS" does .. It runs the "loop()" method forever..
   * 
   * @author GroG
   *
   */
  public static class InoScriptRunner implements Runnable {
    boolean isRunning = false;
    VirtualArduino virtual;
    MrlCommIno ino;
    Thread myThread = null;

    InoScriptRunner(VirtualArduino virtual, MrlCommIno ino) {
      this.virtual = virtual;
      this.ino = ino;
    }
    
    synchronized public void start() {
      if (myThread == null) {
        myThread = new Thread(this, String.format("%s.mrlcomm", virtual.getName()));
        myThread.start();
        log.info("start called ");
      }
    }
    
    synchronized public void stop() {
      log.info("stop called for mrlcomm ino script.");
      if (myThread != null) {
        isRunning = false;
        myThread.interrupt();
        myThread = null;
      }
    }

    public void run() {
      isRunning = true;
      // prior to running reset, MrlComm would be reset,
      // this is also what happens if you press the reset button on 
      // the actual arduino.  (alternatively, we could create a new MrlComm instance.. 
      // and not rely on calling softReset()...
      ino.getMrlComm().softReset();
      ino.setup();
      while (isRunning) {
        try {
          if (isRunning)
            ino.loop();
            try {
              // a small delay that can be interrupted
              Thread.sleep(1);
            } catch(InterruptedException e1) {
              // we were interrupted.. we need to shut down.
              isRunning = false;
            }
        } catch (Exception e) {
          log.error("mrlcomm threw", e);
          isRunning = false;          
        }

      }
      log.info("leaving InoScriptRunner");
    }
    
    public boolean isRunning() {
      return isRunning;
    }
      
  }

  public VirtualArduino(String n, String id) {
    super(n, id);
  }

  public void connect(String portName) throws IOException {
    if (portName == null) {
      log.warn("{}.connect(null) not valid", getName());
      return;
    }
    if (uart != null && uart.isConnected() && (portName + ".UART").equals(uart.getPortName())) {
      log.info("already connected");
      return;
    }
    if (uart != null && uart.isConnected()) {
      uart.disconnect();
    }
    uart = Serial.connectVirtualUart(uart, portName, portName + ".UART");
  }

  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(VirtualArduino.class);
    meta.addDescription("virtual hardware of for the Arduino!");
    meta.setAvailable(true);
    meta.addPeer("uart", "Serial", "serial device for this Arduino");
    meta.addCategory("simulator");
    return meta;
  }

  public String setBoard(String board) {
    log.info("setting board to type {}", board);
    this.board = board;
    mrlComm.boardType = Arduino.getBoardTypeId(board);
    // Zxcv npinDefs = Arduino.getPinList(board);

    broadcastState();
    return board;
  }

  public void start() {    
    runner.start();
  }

  public void stop() {
    runner.stop();
  }

  /*
   * easy way to set to get a 54 pin arduino
   *
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
    
    if (board == null) {
      board = "uno";
    }

    // start the serial service that talks to our uart.
    // TODO: we really shouldn't have a serial service here.. we should really have just the uart.
    // TODO: we need the DCE end of the uart!  not the DTE end!
    // 
    try {
      // This is the virtual DCE side of the serial port.
      // create our uart serial port service.
      uart = (Serial) startPeer("uart");
      // create the virtual port for our port name and connect it.
      uart = Serial.connectVirtualUart(uart, portName, portName + ".UART");
      
    } catch (IOException e) {
      // TODO Auto-generated catch block
      log.error("Failed to create virtual uart port!", e);
      return;
    }
    // TODO: the virtual arduino service listens for bytes from the uart.
    uart.addByteListener(this.getName());
    // TODO: this seems wrong.  the uart listens to it's own connect sort of messages?
    uart.addPortListener(uart.getName());
    log.info("uart {}", uart);
    
    ino = new MrlCommIno(this);
    
    mrlComm = ino.getMrlComm();
    virtualMsg = mrlComm.getMsg();
    virtualMsg.setInvoke(false);
    boardInfo = mrlComm.boardInfo;
    // boardInfo.setType(Arduino.BOARD_TYPE_ID_UNO);
    setBoard(Arduino.BOARD_TYPE_UNO);
    
    if (runner == null) {
      runner = new InoScriptRunner(this, ino);
    }

    start();
  }

  public void releaseService() {
    if (runner != null) {
      runner.stop();
    }
    releasePeers();
    super.releaseService();
  }

  public Serial getSerial() {
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
/*
  public Simulator getSimulator() {
    return simulator;
  }

  
  public void attachSimulator(Simulator simulator) {
    this.simulator = simulator;
  }
  */

  public MrlComm getMrlComm() {
    return mrlComm;
  }

  @Override
  public String publishConnect(String portName) {
    log.info("Virtual Arduino Publish Connect for port {}", portName);
    return portName;
  }

  // chaining Serial's connect event
  @Override
  public void onConnect(String portName) {
    log.info("ON CONNECT CALLED IN VIRTUAL ARDUINO!!!!!!!!!!!!!!!!! PORT NAME:{}", portName);
    // Ok.. so we've been told to connect.. the uart is connected.. we need to make sure we reset the mrlcommino that is running.
    if (runner.isRunning()) {
      // Stop the mrlcomm runner.. and restart it..  we just got a serial port connect.
      log.info("Stopping mrlcomm runner.");
      stop();
    }
    log.info("starting MRL comm runner.");
    // TODO: is there a race condition here?
    // ?!?
    this.mrlComm.softReset();
    this.mrlComm.getMsg().publishMrlCommBegin(Msg.MRLCOMM_VERSION);
    virtualMsg.onConnect(portName);
    // wire the data back to mrlcomm?!
    
    // uart.addByteListener(mrlComm);
    start();
    
    // we should make sure we are a byte listener?
    
    // TODO: pass this down to virtualmsg?
    // invoke("publishConnect", portName);
  }

  @Override
  public String publishDisconnect(String portName) {
    return portName;
  }

  // chaining Serial's disconnect event ..
  @Override
  public void onDisconnect(String portName) {
    // TODO: pass the disconnect message down to virtualmsg?
    // msg.onDisconnect(portName);
    // invoke("publishDisconnect", portName);
  }

  @Override
  public boolean isConnected() {
    if (uart == null) {      
      return false;
    }
    return uart.isConnected();
  }

  @Override
  public String getPortName() {
    return uart.getPortName();
  }

  @Override
  public List<String> getPortNames() {
    if (uart == null) {
      return new ArrayList<String>();
    }
    return uart.getPortNames();
  }

  // implements PinArrayControl ?
  // @Override
  public List<PinDefinition> getPinList() {
    // 2 board types have been identified (perhaps this is based on processor?)
    // mega-like & uno like

    // if no change - just return the values
    if ((pinMap != null && board.contains("mega") && pinMap.size() == 70) || (pinMap != null && pinMap.size() == 20)) {
      return new ArrayList<PinDefinition>(pinIndex.values());
    }

    // create 2 indexes for fast retrieval
    // based on "name" or "address"
    pinMap = new HashMap<String, PinDefinition>();
    pinIndex = new HashMap<Integer, PinDefinition>();
    List<PinDefinition> pinList = new ArrayList<PinDefinition>();

    if (board.contains("mega")) {
      for (int i = 0; i < 70; ++i) {
        PinDefinition pindef = new PinDefinition(getName(), i);

        // begin wacky pin def logic
        String pinName = null;
        if (i == 0) {
          pindef.setRx(true);
        }
        if (i == 1) {
          pindef.setTx(true);
        }
        if (i < 1 || (i > 13 && i < 54)) {
          pinName = String.format("D%d", i);
          pindef.setDigital(true);
        } else if (i > 53) {
          pinName = String.format("A%d", i - 54);
          pindef.setAnalog(true);
          pindef.setDigital(false);
          pindef.canWrite(false);
        } else {
          pinName = String.format("D%d", i);
          pindef.setPwm(true);
        }
        pindef.setPinName(pinName);
        pindef.setAddress(i);
        pinIndex.put(i, pindef);
        pinMap.put(pinName, pindef);
        pinList.add(pindef);
      }
    } else {
      for (int i = 0; i < 20; ++i) {
        PinDefinition pindef = new PinDefinition(getName(), i);
        String pinName = null;
        if (i == 0) {
          pindef.setRx(true);
        }
        if (i == 1) {
          pindef.setTx(true);
        }
        if (i < 14) {
          pinName = String.format("D%d", i);
          pindef.setDigital(true);
        } else {
          pindef.setAnalog(true);
          pindef.canWrite(false);
          pindef.setDigital(false);
          pinName = String.format("A%d", i - 14);
        }
        if (i == 3 || i == 5 || i == 6 || i == 9 || i == 10 || i == 11) {
          pindef.setPwm(true);
          pinName = String.format("D%d", i);
        }
        pindef.setPinName(pinName);
        pindef.setAddress(i);
        pinIndex.put(i, pindef);
        pinMap.put(pinName, pindef);
        pinList.add(pindef);
      }
    }
    return pinList;
  }

  @Override
  public void connect(String port, int rate, int databits, int stopbits, int parity) throws Exception {
    uart.connect(port, rate, databits, stopbits, parity);
  }

  @Override
  public void disconnect() {
    uart.disconnect();
  }

  /*
   * public void setAref(String aref) { int arefInt = 1; switch (aref) { case
   * "EXTERNAL": arefInt = 0; break; case "DEFAULT": arefInt = 1; break; case
   * "INTERNAL1V1": arefInt = 2; break; case "INTERNAL": arefInt = 3; break;
   * case "INTERNAL2V56": arefInt = 3; break; default: error("Aref " +
   * aref.toUpperCase() + " is unknown"); } log.info("set aref to " + aref);
   * this.aref = aref; msg.setAref(arefInt); }
   */

  public String getAref() {
    return aref;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init("INFO");

      // WOW GOOD TEST !!!
      // Service.reserveRootAs("virtual.uart", "newName");
      // Service.buildDna("Tracking");

      // Runtime.start("webgui", "WebGui");

      // Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      VirtualArduino virtual = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      virtual.connect("COM99");
      arduino.connect("COM99");

      Runtime.start("gui", "SwingGui");

      // arduino.enablePin("D7");
      // String port = "COM5";
      // connect the virtual uart
      // varduino.setPortName(port);
      // connect the arduino to the other end
      // varduino.connect(port);
      // arduino.enablePin(54);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
  
  public void stopService() {
    super.stopService();
    stop();
  }

  @Override
  public void connect(String port, int rate) throws Exception {
    connect(port);
  }

  @Override
  public void onBytes(byte[] bytes) {
    // if we get bytes from the uart (DCE side) of the port.. we need to push them down to the virtual message.
    log.info("VIRTUAL ARDUINO ON BYTES {} !!!!!!!!!!!!!!!!!!!!!!!!!!", bytes);
    virtualMsg.onBytes(bytes);
  }

}
