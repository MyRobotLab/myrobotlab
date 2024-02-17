package org.myrobotlab.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.arduino.virtual.Device;
import org.myrobotlab.arduino.virtual.MrlComm;
import org.myrobotlab.arduino.virtual.MrlCommIno;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.VirtualArduinoConfig;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PortConnector;
import org.myrobotlab.service.interfaces.PortListener;
import org.myrobotlab.service.interfaces.PortPublisher;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.slf4j.Logger;

/**
 * Virtual Arduino Simulator... It emulates the Arduino, but we also try to
 * maintain the internal state the Arduino would (at least on a software
 * level)...
 * 
 * @author GroG
 *
 */
public class VirtualArduino extends Service<VirtualArduinoConfig> implements PortPublisher,PortListener,PortConnector,SerialDataListener
{

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(VirtualArduino.class);

  /**
   * The Java port of the MrlComm.ino script
   */
  transient MrlCommIno ino;
  transient MrlComm mrlComm;
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
        log.info("start called in virtual arduino.");
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

    @Override
    public void run() {
      // prior to running reset, MrlComm would be reset,
      // this is also what happens if you press the reset button on
      // the actual arduino. (alternatively, we could create a new MrlComm
      // instance..
      // and not rely on calling softReset()...
      // make sure our serial port is ready for us.
      if (virtual.getSerial() == null) {
        log.warn("VIRTUAL UART WAS NULL!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        return;
      }
      log.info("Starting up virtual arduino thread.");
      ino.getMrlComm().softReset();
      // add our byte listener... TODO: push this up farther?
      virtual.getSerial().addByteListener(virtual);
      ino.setup();
      // make sure the serial port is up and running .. perhaps we should add a
      // small sleep in here?
      // TODO: poll the serial device to make sure it's started & connected?
      log.info("Starting loop");
      isRunning = true;
      while (isRunning) {
        if (isRunning) {
          ino.loop();
        }
        try {
          // a small delay that can be interrupted
          Thread.sleep(1);
        } catch (InterruptedException e1) {
          // we were interrupted.. we need to shut down.
          isRunning = false;
          log.info("MrlCommIno runner thread interrupted.");
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

  /**
   * Connect to a serial port to the uart/DCE side of a virtual serial port.
   */
  @Override
  public void connect(String portName) throws IOException {
    if (portName == null) {
      log.warn("{}.connect(null) not valid", getName());
      return;
    }
    if (uart != null && uart.isConnected() && (portName + ".UART").equals(uart.getPortName())) {
      log.info("already connected");
      return;
    }
    // create our ino script.
    ino = new MrlCommIno(this);
    // grab a handle to the mrlcomm
    mrlComm = ino.getMrlComm();
    if (board == null) {
      setBoardUno();
    }
    // update our board info
    if (runner == null) {
      runner = new InoScriptRunner(this, ino);
      // runner.start();
    }

    // FIXME - THIS MAKES NO SENSE - NEXT LINE ASSIGNS IT !?!?! uart =
    // Serial.connectVirtualUart WTF?
    uart.addByteListener(this);

    // connect the DCE/uart port side
    uart = Serial.connectVirtualUart(uart, portName, portName + ".UART");
    // register our selves to listen for the bytes
    // create a new mrlcommino runner..
    start();
    // There is a small race condition. so wait for the runner to actually be
    // started.
    // TODO: remove this sleep statement, replace with a better lock.
    while (!runner.isRunning()) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        log.warn("Interrupted virtual arduino.", e);
        break;
      }
    }
    // at this point we should also register ourselves as the byteListener for
    // this uart.
    // TODO: after this is a good place to start the mrlCommIno runner.. isn't
    // it?
  }

  public String setBoard(String board) {
    // TODO: if the board type changes we need to reinit the pin map.
    log.info("setting board to type {}", board);
    this.board = board;
    mrlComm.boardType = Arduino.getBoardTypeId(board);
    broadcastState();
    return board;
  }

  public void start() {
    runner.start();
  }

  public void stop() {
    if (runner != null) {
      runner.stop();
    }
    if (uart != null) {
      uart.disconnect();
    }
  }

  /*
   * easy way to set to get a 54 pin arduino
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
    // first thing's first. let's create a virtual serial port for the uart/dce
    // side
    // create our uart serial port service.
    uart = (Serial) startPeer("uart");
  }

  @Override
  public void releaseService() {
    super.releaseService();
    if (runner != null) {
      runner.stop();
    }
    if (uart != null) {
      uart.releaseService();
    }
    // sleep(300);
    disconnect();
  }

  public Serial getSerial() {
    return uart;
  }

  public MrlComm MrlComm() {
    return ino.getMrlComm();
  }

  public Device getDevice(int deviceId) {
    return mrlComm.getDevice(deviceId);
  }

  @Deprecated
  public int readBlocking(int address, int i) {
    // TODO This method doesn't do anything and is only referenced in a unit
    // test.
    return 0;
  }

  public void clearPinQueue(int address) {
    mrlComm.pinList.clear();
  }

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
    // TODO: consider something like restarting the MrlCommIno runner..
    // chain the connect
    invoke("publishConnect", portName);
  }

  @Override
  public String publishDisconnect(String portName) {
    return portName;
  }

  // chaining Serial's disconnect event ..
  @Override
  public void onDisconnect(String portName) {
    // pass the disconnect message down to mrlcomm
    mrlComm.onDisconnect(portName);
    // chain
    invoke("publishDisconnect", portName);
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

  @Override
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
    // It's the responsibility of VirtualArduino to relay the bytes down to
    // mrlComm.
    mrlComm.onBytes(bytes);
  }

}
