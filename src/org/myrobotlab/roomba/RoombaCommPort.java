package org.myrobotlab.roomba;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.framework.QueueStats;
import org.myrobotlab.service.Serial;
import org.myrobotlab.service.interfaces.SerialDataListener;

/**
 * The serial-port based implementation of RoombaComm. Handles both physical
 * RS-232 ports, USB adapter ports like Keyspan USA-19HS, and Bluetooth serial
 * port profiles.
 * 
 * <p>
 * Some code taken from processing.serial.Serial. Thanks guys!
 * </p>
 * 
 * The interaction model for setting the port and protocol and WaitForDSR
 * parameters is as follows.
 * <p>
 * On creation, the class initializes the parameters, then tries to read
 * .roomba_config. If it can read the config file and parse out the parameters,
 * it sets the parameters to the values in the config file. Apps can read the
 * current settings for display using methods on the class. Apps can override
 * the settings by accepting user input and setting the parameters using methods
 * on the class, or the connect() method. Parameters that are changed by the app
 * are re-written in the config file, for use as defaults next run. Command-line
 * apps can make these parameters optional, by using the defaults if the user
 * doesn't specify them.
 */
public class RoombaCommPort extends RoombaComm implements SerialDataListener {
  private int rate = 57600;

  static final int databits = 8;
  static final int parity = 0;
  static final int stopbits = 1;
  private String protocol = "SCI";

  Serial serial;

  /**
   * contains a list of all the ports keys are port names (e.g.
   * "/dev/usbserial1") values are Boolean in-use indicator
   */
  static Map<String, Boolean> ports = null;

  /**
   * The time to wait in milliseconds after sending sensors command before
   * attempting to read
   */
  public static int updateSensorsPause = 400;

  /** the serial input stream, normally you don't need access to this */
  // public InputStream input;
  /** the serial output stream, normally you don't need access to this */
  // public OutputStream output;

  private String portname = null; // "/dev/cu.KeySerial1" for instance

  /**
   * RXTX bombs when flushing output sometimes, so by default do not flush the
   * output stream. If the output is too buffered to be useful, do:
   * roombacomm.comm.flushOutput = true; before using it and see if it works.
   */
  public boolean flushOutput = false;
  /**
   * Some "virtual" serial ports like Bluetooth serial on Windows return weird
   * errors deep inside RXTX if an opened port is used before the virtual COM
   * port is ready. One way to check that it is ready is to look for the DSR
   * line going high. However, most simple, real serial ports do not do hardware
   * handshaking so never set DSR high. Thus, if using Bluetooth serial on
   * Windows, do: roombacomm.waitForDSR = true; before using it and see if it
   * works.
   */
  public boolean waitForDSR = false; // Warning: public attribute - setting

  // won't trigger config file write
  byte buffer[] = new byte[32768];
  int bufferLast;

  // int bufferSize = 26; // how big before reset or event firing
  // boolean bufferUntil;
  // int bufferUntilByte;

  /*
   * Let you check to see if a port is in use by another Rooomba before trying
   * to use it.
   */
  public static boolean isPortInUse(String pname) {
    Boolean inuse = (Boolean) ports.get(pname);
    if (inuse != null) {
      return inuse.booleanValue();
    }
    return false;
  }

  // constructor
  public RoombaCommPort() {
    super();
    serial = new Serial("serial");
    serial.startService();
    makePorts();
  }

  public RoombaCommPort(boolean autoupdate) {
    super(autoupdate);
    makePorts();
  }

  public RoombaCommPort(boolean autoupdate, int updateTime) {
    super(autoupdate, updateTime);
    makePorts();
  }

  public void computeSensors() {
    sensorsValid = true;
    sensorsLastUpdateTime = System.currentTimeMillis();
    computeSafetyFault();
  }

  
  public boolean connect(String portid) {
    logmsg("connecting to port '" + portid + "'");
    portname = portid;

    if (isPortInUse(portid)) {
      logmsg("port is in use");
      return false;
    }

    try {
    openPort();
    } catch(Exception e){
    	log.error("cannot connect", e);
    	return false;
    }

    if (connected) {
      // log in the global ports hash if the port is in use now or not
      ports.put(portname, new Boolean(connected));
      sensorsValid = false;
    } else {
      disconnect();
    }

    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.roomba.Z#disconnect()
   */
  @Override
  public void disconnect() {
    connected = false;

    // log in the global ports hash if the port is in use now or not
    ports.put(portname, new Boolean(connected));

    /*
     * try { // do io streams need to be closed first? if (input != null)
     * input.close(); if (output != null) output.close(); } catch (Exception e)
     * { e.printStackTrace(); } input = null; output = null;
     */

    serial.disconnect();
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getPortname() {
    return portname;
  }

  public String getProtocol() {
    return protocol;
  }

  public boolean isWaitForDSR() {
    return waitForDSR;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.roomba.Z#listPorts()
   */
  @Override
  public String[] listPorts() {
    List<String> portNames = serial.getPortNames();
    return portNames.toArray(new String[portNames.size()]);
  }

  void makePorts() {
    if (ports == null)
      ports = Collections.synchronizedMap(new TreeMap<String, Boolean>());
  }

  /*
   * pause(updateSensorsPause); // take a breather to let data come back
   * sensorsValid = false; // assume the worst, we're gothy int n = available();
   * //logmsg("updateSensors:n="+n); if( n >= 26) { // there are enough bytes to
   * read n = readBytes(sensor_bytes); if( n==26 ) { // did we get enough?
   * sensorsValid = true; // then everything's good, otherwise bad
   * computeSafetyFault(); } } else { logmsg("updateSensors:only "+n+
   * " bytes available, not updating sensors"); }
   * 
   * //logmsg("buffer contains: "+ buffer ); return sensorsValid;
   */

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.myrobotlab.roomba.Z#serialEvent(org.myrobotlab.serial.SerialDeviceEvent
   * )
   */
  @Override
  public Integer onByte(Integer newByte) {

    buffer[bufferLast++] = (byte) newByte.intValue();
    if (bufferLast == 26) {
      bufferLast = 0;
      System.arraycopy(buffer, 0, sensor_bytes, 0, 26);
      computeSensors();
    }

    return newByte;

  }

  /**
   * internal method, used by connect() FIXME: make it faile more gracefully,
   * recognize bad port
 * @throws IOException 
   */
  private void openPort() throws IOException {
	  serial.open(portname, rate, databits, stopbits, parity);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.roomba.Z#send(byte[])
   */
  // FIXME - IS THIS RIGHT ?!?!? - why would you send real Java bytes - you'd
  // have to
  // "load" them incorrectly send them
  //
  @Override
  public boolean send(byte[] bytes) {
    try {
      // BLECH - conversion to support silly send(byte[] bytes)
      int[] ints = new int[bytes.length];
      for (int i = 0; i < ints.length; ++i) {
        ints[i] = bytes[i];
      }
      serial.write(ints);
      // if( flushOutput ) port.flush(); // hmm, not sure if a good idea
    } catch (Exception e) { // null pointer or serial port dead
      e.printStackTrace();
    }
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.roomba.Z#send(int)
   */
  @Override
  public boolean send(int b) { // will also cover char or byte
    try {
      serial.write(b & 0xff); // for good measure do the &
      // if( flushOutput ) output.flush(); // hmm, not sure if a good idea
    } catch (Exception e) { // null pointer or serial port dead
      // errorMessage("send", e);
      e.printStackTrace();
    }
    return true;
  }

  public void setPortname(String p) {
    portname = p;
    logmsg("Port: " + portname);
    // writeConfigFile(portname, protocol, waitForDSR?'Y':'N'); fixme - use
    // Service.save()

  }

  public void setProtocol(String protocol) {
    if (protocol.equals("SCI")) {
      rate = 57600;
    } else if (protocol.equals("OI")) {
      rate = 115200;
    }
    this.protocol = protocol;
    logmsg("Protocol: " + protocol);
    // writeConfigFile(portname, protocol, waitForDSR?'Y':'N'); FIXME -
    // remove use Service.save() !
  }

  public void setWaitForDSR(boolean waitForDSR) {
    this.waitForDSR = waitForDSR;
  }

  // -------------------------------------------------------------
  // below only used internally to this class
  // -------------------------------------------------------------

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.roomba.Z#updateSensors()
   */
  @Override
  public boolean updateSensors() {
    sensorsValid = false;
    sensors();
    for (int i = 0; i < 20; i++) {
      if (sensorsValid) {
        logmsg("updateSensors: sensorsValid!");
        break;
      }
      logmsg("updateSensors: pausing...");
      pause(50);
    }

    return sensorsValid;
  }

  public boolean updateSensors(int packetcode) {
    sensorsValid = false;
    sensors(packetcode);
    for (int i = 0; i < 20; i++) {
      if (sensorsValid) {
        logmsg("updateSensors: sensorsValid!");
        break;
      }
      logmsg("updateSensors: pausing...");
      pause(50);
    }

    return sensorsValid;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.roomba.Z#wakeup()
   */
  @Override
  public void wakeup() {
    serial.setDTR(false);
    pause(500);
    serial.setDTR(true);
  }

  @Override
  public void onConnect(String portName) {
    log.info(String.format("%s connected to %s", getName(), portName));
  }

  @Override
  public void onDisconnect(String portName) {
    log.info(String.format("%s disconnected from %s", getName(), portName));
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
}
