package org.myrobotlab.service;

import java.io.IOException;
import java.util.HashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.data.JoystickData;
import org.myrobotlab.service.interfaces.JoystickListener;
import org.myrobotlab.service.interfaces.KeyListener;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

/**
 * EddieControlBoard - This service will communicate with the parallax
 * EddieControlBoard It can publish sensor data , control motors and more!
 *
 */
public class EddieControlBoard extends Service implements KeyListener, SerialDataListener, JoystickListener {

  class SensorPoller extends Thread {

    boolean isPolling = false;

    @Override
    public void run() {
      isPolling = true;
      while (isPolling) {
        try {
          String dataString = getAnalogValues();
          if (dataString.length() == 32) {
            invoke("publishSensors", dataString);
          } else {
            error("invalid data string %s", dataString);
          }
          sleep(sensorPollIntervalMS);
        } catch (Exception e) {
          Logging.logError(e);
        }
      }

    }
  }

  class Simulator extends Thread {
    @Override
    public void run() {
      while (isRunning()) {
        // how to auto correct & read the various parts
        // you know how to do this - ORIGINAL InputStream API Argggg !

      }
    }
  }

  private static final long serialVersionUID = 1L;

  // Peers
  transient Serial serial;

  transient Keyboard keyboard;
  transient WebGui webgui;
  transient Joystick joystick;
  transient RemoteAdapter remote;

  transient Python python;
  transient SpeechSynthesis mouth;

  HashMap<String, Float> lastSensorValues = new HashMap<String, Float>();
  int sampleCount = 0;
  Mapper mapper = new Mapper(-1.0f, 1.0f, -127.0f, 127.0f);
  float leftMotorPower = 0.0f;

  float rightMotorPower = 0.0f;

  int timeout = 500;// 500 ms serial timeout

  transient SensorPoller sensorPoller = null;

  int sensorPollIntervalMS = 100; // 10 times a second

  public final static Logger log = LoggerFactory.getLogger(EddieControlBoard.class);

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Runtime.start("ecb", "EddieControlBoard");

      // 129 -> 81
      // 128 -> 80 (full reverse)
      // 127 -> 7F (full forward)
      // 255 -> (little reverse)

      // 81 FF 0 1 7F
      // 128 --- 255 0 1 --- 127
      /*
       * float i = 0.94f;
       * 
       * Map mapper = new Map(-1.0f, 1.0f, -127.0f, 127.0f); int x =
       * mapper.calc(i); if (x > 127) { x = 128 - x; }
       * 
       * log.info("{}", Integer.toHexString(x & 0xFF));
       * 
       * String hex = Integer.toHexString(256 & 0xFF);
       * log.info(hex.toUpperCase()); hex = Integer.toHexString(255 & 0xFF);
       * log.info(hex.toUpperCase()); hex = Integer.toHexString(230 & 0xFF); //
       * slow reverse log.info(hex.toUpperCase());
       */

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public EddieControlBoard(String n) {
    super(n);
  }

  public void connect(String port) throws IOException {
	  serial.open(port, 115200, 8, 1, 0);
  }

  public String getAnalogValues() throws Exception {
    // serial.clear();
    serial.write("ADC\r");
    String ret = serial.readString(32);
    return ret;
  }

  public Float getBatteryLevel() {
    startSensors();
    sleep(1000);
    stopSensors();
    return lastSensorValues.get("BATTERY");
  }

  public String getGPIOHighValues() throws Exception {
    serial.write("HIGHS\r");
    String ret = serial.readString(9);
    return ret;
  }

  public String getGPIOInputs() throws Exception {
    serial.write("INS\r");
    String ret = serial.readString(9);
    return ret;
  }

  public String getGPIOLowValues() throws Exception {
    serial.write("LOWS\r");
    String ret = serial.readString(9);
    return ret;
  }

  public String getGPIOOutputs() throws Exception {
    serial.write("OUTS\r");
    String ret = serial.readString(9);
    return ret;
  }

  // read commands begin ---
  public String getHwVersion() throws Exception {
    serial.write("HWVER\r");
    String ret = serial.readString(5);
    return ret;
  }

  public String getPingValues() throws Exception {
    // depends
    serial.write("PING\r");
    String ret = serial.readString(5);
    return ret;
  }

  public String getVersion() throws Exception {
    serial.write("VER\r");
    String ret = serial.readString(5);
    return ret;
  }

  public void go(float left, float right) throws Exception {
    log.info(String.format("go %f %f", left, right));
    int l = mapper.calcOutputInt(left);
    if (l > 127) {
      l = 128 - l;
    }
    int r = mapper.calcOutputInt(right);
    if (r > 127) {
      r = 128 - r;
    }
    String cmd = String.format("GO %s %s\r", Integer.toHexString(l & 0xFF), Integer.toHexString(r & 0xFF)).toUpperCase();
    info("%s", cmd);
    serial.write(cmd);
  }

  @Override
  public void onJoystickInput(JoystickData input) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void onKey(String cmd) throws Exception {
    if (cmd.equals("NumPad-7")) {
      leftMotorPower += 0.01;
      if (leftMotorPower > 1.0) {
        leftMotorPower = 1.0f;
      }
      go(leftMotorPower, rightMotorPower);

    } else if (cmd.equals("NumPad-4")) {
      leftMotorPower -= 0.01;
      if (leftMotorPower < -1.0) {
        leftMotorPower = -1.0f;
      }
      rightMotorPower += 0.01;
      if (rightMotorPower > 1.0) {
        rightMotorPower = 1.0f;
      }
      go(leftMotorPower, rightMotorPower);

    } else if (cmd.equals("NumPad-1")) {
      leftMotorPower -= 0.01;
      if (leftMotorPower < -1.0) {
        leftMotorPower = -1.0f;
      }
      go(leftMotorPower, rightMotorPower);

    }
    // left end ---

    // right begin --
    else if (cmd.equals("NumPad-9")) {
      rightMotorPower += 0.01;
      if (rightMotorPower > 1.0) {
        rightMotorPower = 1.0f;
      }
      go(leftMotorPower, rightMotorPower);

    } else if (cmd.equals("NumPad-6")) {
      rightMotorPower -= 0.01;
      if (rightMotorPower < -1.0) {
        rightMotorPower = -1.0f;
      }
      leftMotorPower += 0.01;
      if (leftMotorPower > 1.0) {
        leftMotorPower = 1.0f;
      }
      go(leftMotorPower, rightMotorPower);

    } else if (cmd.equals("NumPad-3")) {
      rightMotorPower -= 0.01;
      if (rightMotorPower < -1.0) {
        rightMotorPower = -1.0f;
      }
      go(leftMotorPower, rightMotorPower);

    }
    // right end --

    // center
    else if (cmd.equals("NumPad-8")) {
      leftMotorPower += 0.01;
      if (leftMotorPower > 1.0) {
        leftMotorPower = 1.0f;
      }
      rightMotorPower += 0.01;
      if (rightMotorPower > 1.0) {
        rightMotorPower = 1.0f;
      }
      go(leftMotorPower, rightMotorPower);

    }

    else if (cmd.equals("NumPad-2")) {
      leftMotorPower -= 0.01;
      if (leftMotorPower > 1.0) {
        leftMotorPower = 1.0f;
      }
      rightMotorPower -= 0.01;
      if (rightMotorPower > 1.0) {
        rightMotorPower = 1.0f;
      }
      go(rightMotorPower, rightMotorPower);
    }

    // stop all
    else if (cmd.equals("NumPad-5") || cmd.equals("Space")) {
      leftMotorPower = 0.0f;
      rightMotorPower = 0.0f;
      go(rightMotorPower, rightMotorPower);
    }

    else {
      warn("key command - [%s] - not defined", cmd);

    }

  }

  public void onRY(Float ry) throws Exception {
    leftMotorPower = ry * -1;
    go(rightMotorPower, leftMotorPower);
  }

  public void onY(Float y) throws Exception {
    rightMotorPower = y * -1;
    go(rightMotorPower, leftMotorPower);
  }

  public HashMap<String, Float> publishSensors(String dataString) {
    log.info(dataString);
    String[] values = dataString.split(" ");
    lastSensorValues.put("LEFT_IR", new Float(Integer.parseInt(values[0].trim(), 16)));
    lastSensorValues.put("MIDDLE_IR", new Float(Integer.parseInt(values[1].trim(), 16)));
    lastSensorValues.put("RIGHT_IR", new Float(Integer.parseInt(values[2].trim(), 16)));
    lastSensorValues.put("BATTERY", new Float(0.00039f * Integer.parseInt(values[7].trim(), 16)));
    ++sampleCount;
    return lastSensorValues;
  }

  // read commands end ---

  public String read() throws Exception {
    return sendCommand("READ");
  }

  public void sayBatterLevel(Float buttonValue) {
    try {
      Float bl = getBatteryLevel();
      mouth.speak(String.format("current battery level is %d", bl.intValue()));
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public String sendCmd(String cmd, int expectedResponseLength) throws Exception {
    log.info(String.format("sendCommand %s", cmd));
    String ret = null;

    serial.write(String.format("%s\r", cmd));
    ret = serial.readString(expectedResponseLength);

    return ret;
  }

  /**
   * sending a command when expecting a string response in the context of
   * blocking for response
   * @param cmd to send
   * @return  the string response
   * @throws Exception e
   */
  public String sendCommand(String cmd) throws Exception {
    log.info(String.format("sendCommand %s", cmd));
    String ret = null;

    // serial.setBlocking(true);
    serial.write(String.format("%s\r", cmd));
    // ret = serial.readString();
    // serial.setBlocking(false);

    return ret;
  }

  public void setMotorSpeed(float left, float right) {
    // TODO: this function doesn't do anything?
    // The left and right speeds have units of positions per second and are
    // entered as
    // signed (two's complement) 16-bit hex values. The range of allowed
    // values is
    // from 8000 to 7FFF.
    // int myleft = (int) (left * 1000);
    // int myright = (int) (right * 1000);
    // String l = Integer.toHexString(myleft);
    // String r = Integer.toHexString(myright);

    // Long.parseLong("ffff8000", 16);
    // serial.write(String.format("%s\r",cmd));
  }

  public void startJoystick() throws Exception {
    joystick = (Joystick) startPeer("joystick");
    joystick.addInputListener(this);
    /*
     * joystick.addAxisListener(getName(), "onY");
     * joystick.addAxisListener(getName(), "onRY");
     */
  }

  public void startRemoteAdapter() throws Exception {
    remote = (RemoteAdapter) startPeer("remote");
    remote.startListening();
  }

  public boolean startSensors() {
    if (sensorPoller == null) {
      sensorPoller = new SensorPoller();
      sensorPoller.start();
      return true;
    }
    return false;
  }

  @Override
  public void startService() {
    super.startService();
    serial = (Serial) startPeer("serial");
    serial.addByteListener(this);
    serial.setTimeout(500);
    keyboard = (Keyboard) startPeer("keyboard");
    keyboard.addKeyListener(this);
    python = (Python) Runtime.start("python", "Python");
    mouth = (SpeechSynthesis) Runtime.start("mouth", "NaturalReaderSpeech");
  }

  public void startWebGUI() throws Exception {
    webgui = (WebGui) startPeer("webgui");
  }

  public void stop() throws Exception {
    go(0.0f, 0.0f);
  }

  public boolean stopSensors() {
    if (sensorPoller != null) {
      sensorPoller.isPolling = false;
      sensorPoller = null;
      return true;
    }
    return false;
  }

  @Override
  public final Integer onByte(Integer newByte) throws IOException {
    info("%s onByte %s", getName(), newByte);
    return newByte;
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

    ServiceType meta = new ServiceType(EddieControlBoard.class.getCanonicalName());
    meta.addDescription("microcontroller designed for robotics");
    meta.addCategory("microcontroller");
    // John Harland no longer uses this hardware
    meta.setAvailable(false);

    // put peer definitions in
    meta.addPeer("serial", "Serial", "serial");
    meta.addPeer("keyboard", "Keyboard", "serial");
    meta.addPeer("webgui", "WebGui", "webgui");
    meta.addPeer("remote", "RemoteAdapter", "remote interface");
    meta.addPeer("joystick", "Joystick", "joystick");

    return meta;
  }

}
