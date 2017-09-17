package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.slf4j.Logger;

public class Lidar extends Service implements SerialDataListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Lidar.class.getCanonicalName());
  public static final String MODEL_SICK_LMS200 = "SICK LMS200";
  public String serialName;
  public transient Serial serial;
  public transient ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  String model;
  // states
  public static final String STATE_PRE_INITIALIZATION = "state pre initialization";
  public static final String STATE_INITIALIZATION_STAGE_1 = "state initialization stage 1";
  public static final String STATE_INITIALIZATION_STAGE_2 = "state initialization stage 2";
  public static final String STATE_INITIALIZATION_STAGE_3 = "state initialization stage 3";
  public static final String STATE_INITIALIZATION_STAGE_4 = "state initialization stage 4";
  public static final String STATE_SINGLE_SCAN = "taking a single scan";
  public static final String STATE_MODE_CHANGE = "changing mode";
  public static final String STATE_NOMINAL = "waiting on user to tell me what to do";
  public int dataMessageSize = 213; // default size for a SICK LMS-200
  String state = STATE_PRE_INITIALIZATION;
  // String state = STATE_SINGLE_SCAN;//STATE_PRE_INITIALIZATION; // for
  // testing with SEAR Simulator
  int index = 0;
  private int LIDARbaudRate = 9600; // by default
  private String serialPort;
  private byte[] message;
  // private boolean dataAvailable = false;
  String info; // used for outputting log.info messages

  public static void main(String[] args) {
    LoggingFactory.init(Level.WARN);

    try {

      Lidar template = new Lidar("Lidar");
      template.startService();

      // Lidar lidar01 = (Lidar) Runtime.createAndStart("lidar01",
      // "Lidar");
      // creates and runs a serial service
      // lidar01.connect("dev/lidar01");
      // send a command
      // this sets the mode to a spread of 180 degrees with readings every
      // 0.5
      // degrees
      // lidar01.setMode(180, 0.5f);
      // this setMode command catches the reply from the LMS in the
      // listener
      // within the
      // Lidar service and returns a bool stating if it was successful or
      // not.

      // an array of floats holding ranges (after the LDIAR service strips
      // and
      // parses the data.
      // lidar01.singleScan();

      Python python = new Python("python");
      python.startService();

      Runtime.createAndStart("gui", "SwingGui");
      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Lidar(String n) {
    super(n);
    reserve(String.format("%s_serial", n), "Serial", "serial port for Lidar");
  }

  @Override
  public Integer onByte(Integer b) throws IOException {
    index++;

    if (log.isDebugEnabled()) {
      log.debug(String.format("byteReceived Index = %d expected message size = %d data = %02x", index, dataMessageSize, b));
    }

    buffer.write(b);
    // so a byte was appended
    // now depending on what model it was and
    // what stage of initialization we do that funky stuff
    if (MODEL_SICK_LMS200.equals(model) && STATE_MODE_CHANGE.equals(state) && buffer.size() == 14) {
      // These modes always have 14 bytes replies
      // log.info(buffer.toString());
      message = buffer.toByteArray();
      // dataAvailable = true;
      if (message[5] == 1 || message[6] == 1) {
        log.info("Mode change was a Success!!!");
      }
      if (message[5] == 0 || message[6] == 0) {
        log.error("Sorry dude, but I failed to change mode. Please try again.");
      }
      state = STATE_NOMINAL;
    }
    if (MODEL_SICK_LMS200.equals(model) && STATE_SINGLE_SCAN.equals(state) && index == dataMessageSize) {
      if (log.isDebugEnabled()) {
        log.debug(String.format("Buffer size =  %s  Buffer =  %s", +buffer.size(), buffer.toString()));
      }
      // WTF do I do with this data now?
      buffer.flush(); // flush entire buffer so I can convert it to a byte
      // array
      message = buffer.toByteArray();
      info = String.format("size of message = %s", message.length);
      log.info(info);
      // dataAvailable = true;
      invoke("publishLidarData");
      state = STATE_NOMINAL;
      index = 0;

    }
    return b;
  }

  public void connect(String port) throws IOException {
    serial = getSerial();
    serialPort = port;
    serial.open(port, LIDARbaudRate, 8, 1, 0);
  }

  public boolean connect(String port, int baud) throws IOException {
    serial = getSerial();
    serialPort = port;
    LIDARbaudRate = baud;
    serial.open(port, baud, 8, 1, 0);

    return serial.isConnected();
  }

  public boolean disconnect() {
    serial = getSerial();
    serial.disconnect();
    return serial.isConnected();
  }

  public Serial getSerial() {
    if (serialName == null) {
      serialName = String.format("%s_serial", getName());
    }
    serial = (Serial) Runtime.create(serialName, "Serial");
    return serial;
  }

  public int[] publishLidarData() {

    int[] intData = new int[(message.length - 11) / 2];
    log.info("publishLidarData has been called. Message length = " + message.length + "        We should have = " + intData.length + "data readings");
    StringBuilder data = new StringBuilder("");
    for (int i = 8; i < (message.length - 3); i = i + 2) // excluding the
    // header and
    // the footer,
    // taking every
    // other byte as
    // the LSB of
    // the new
    // number
    {
      // Do some bitwise stuff to get our integer distance in cm(default)
      // or mm if you changed the mode
      // data = MSB << 8 | LSB
      ByteBuffer bb = ByteBuffer.wrap(new byte[] { 0, 0, message[i + 1], message[i] });

      intData[(i - 8) / 2] = bb.getInt();

      log.info("IntData index = " + (i - 8) / 2 + " i = " + i + " message = " + String.format(" %02x %02x", message[i], message[i + 1])
          + String.format("  Integer = %02x", intData[(i - 8) / 2]));
      data.append(intData[(i - 8) / 2]).append(", ");

    } // end for loop
      // log.info("Data = "+data.toString());
    return intData; // This should return data to the python code if the
    // user has subscribed to it
  }// end dataToString

  public boolean reconnectSerial() throws IOException {
    serial = getSerial();
    serial.disconnect();
    serial.open(serialPort, LIDARbaudRate, 8, 1, 0);
    return serial.isConnected();
  }

  public void setBaud(int baudRate) throws Exception {

    state = STATE_SINGLE_SCAN;

    LIDARbaudRate = baudRate;

    index = 0;
    buffer.reset();

    /*
     * 9600 is default, but just in case you ever need it... PC sends : 02 00 02
     * 00 20 42 52 08 LMS replies: 06 02 81 03 00 A0 00 10 36 1A (success)
     */
    if (baudRate == 9600) {
      serial.write(new byte[] { 0x02, 0x00, 0x02, 0x00, 0x20, 0x42, 0x52, 0x08 });
    } /*
       * 19200 PC sends : 02 00 02 00 20 41 51 08 LMS replies: 06 02 81 03 00 A0
       * 00 10 36 1A (success)
       */else if (baudRate == 19200) {
      serial.write(new byte[] { 0x02, 0x00, 0x02, 0x00, 0x20, 0x41, 0x52, 0x08 });
    } /*
       * 38400 PC sends : 02 00 02 00 20 41 51 08 LMS replies: 06 02 81 03 00 A0
       * 00 10 36 1A (success)
       */else if (baudRate == 38400) {
      serial.write(new byte[] { 0x02, 0x00, 0x02, 0x00, 0x20, 0x40, 0x52, 0x08 });
    } else {
      log.error("You've specified an unsupported baud rate");
    }
  }

  public void setModel(String m) {
    model = m;
  }

  public void setScanMode(int spread, float angularResolution) throws Exception {
    state = STATE_MODE_CHANGE;
    buffer.reset();
    index = 0;
    if (spread == 100) {
      if (angularResolution == 1) {
        serial.write(new byte[] { 0x02, 0x00, 0x05, 0x00, 0x3B, 0x64, 0x00, 0x64, 0x00, 0x1D, 0x0F });
        // Start bytes and header = 8 bytes, 202 data bytes, 1 status
        // and 2 bytes for checksum
        dataMessageSize = 213;
      } else if (angularResolution == 0.5) {
        serial.write(new byte[] { 0x02, 0x00, 0x05, 0x00, 0x3B, 0x64, 0x00, 0x32, 0x00, (byte) 0xb1, 0x59 });
        // Start bytes and header = 8 bytes, 402 data bytes, 1 status
        // and 2 bytes for checksum
        dataMessageSize = 413;
      } else if (angularResolution == 0.25) {
        serial.write(new byte[] { 0x02, 0x00, 0x05, 0x00, 0x3B, 0x64, 0x00, 0x19, 0x00, (byte) 0xe7, 0x72 });
        // Start bytes and header = 8 bytes, 802 data bytes, 1 status
        // and 2 bytes for checksum
        dataMessageSize = 813;
      } else {
        log.error("You've defined an unsupported Mode");
      }
    } // end if spread = 100
    if (spread == 180) {
      if (angularResolution == 1) {
        serial.write(new byte[] { 0x02, 0x00, 0x05, 0x00, 0x3B, (byte) 0xB4, 0x00, 0x64, 0x00, (byte) 0x97, 0x49 });
        // Start bytes and header = 8 bytes, 362 data bytes, 1 status
        // and 2 bytes for checksum
        dataMessageSize = 373;
      } else if (angularResolution == 0.5) {
        serial.write(new byte[] { 0x02, 0x00, 0x05, 0x00, 0x3B, (byte) 0xB4, 0x00, 0x32, 0x00, 0x3B, 0x1F });
        // Start bytes and header = 8 bytes, 722 data bytes, 1 status
        // and 2 bytes for checksum
        dataMessageSize = 733;
      } else {
        log.error("You've defined an unsupported Mode");
      }
    } // end if spread = 180
  }// end of setMode

  /*
   * Set Lidar to use centimeters
   */
  public boolean setToCM() {
    return true;
    // if (true) {
    // return true;
    // } else {
    // return false;
    // }
  }

  /*
   * Set Lidar to use millimeters
   */
  public boolean setToMM() {
    return true;
    // if (true) {
    // return true;
    // } else {
    // return false;
    // }
  }

  public void singleScan() throws Exception {
    state = STATE_SINGLE_SCAN;
    serial.write(new byte[] { 0x02, 0x00, 0x02, 0x00, 0x30, 0x01, 0x31, 0x18 });
    index = 0;
    buffer.reset();
  }// end singleScan

  @Override
  public void startService() {
    super.startService();

    try {
      serial = (Serial) startPeer("serial", "Serial");
      serial.addByteListener(this);
      // setting callback / message route
      serial.addListener("publishByte", getName(), "byteReceived");
      serial.startService();
      if (model == null) {
        model = MODEL_SICK_LMS200;
      }

      // start Lidar hardware initialization here
      // data coming back from the hardware will be in byteRecieved
      if (MODEL_SICK_LMS200.equals(model)) {
        serial.write(new byte[] { 1, 38, 32, 43 });
      }
      state = STATE_INITIALIZATION_STAGE_1;
    } catch (Exception e) {
      error(e.getMessage());
    }
  }

  public void write(byte[] command) throws Exception {
    // iterate through the byte array sending each one to the serial port.
    for (int i = 0; i < command.length; i++) {
      serial.write(command[i]);
    }
  }

  @Override
  public void onConnect(String portName) {
    info("%s connected to %s", getName(), portName);
  }

  @Override
  public void onDisconnect(String portName) {
    info("%s disconnected from %s", getName(), portName);
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

    ServiceType meta = new ServiceType(Lidar.class.getCanonicalName());
    meta.addDescription("The Lidar Service - Light Detection And Ranging");
    meta.addCategory("sensor");

    return meta;
  }
}
