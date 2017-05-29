package org.myrobotlab.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.myrobotlab.arduino.BoardType;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.PortConnector;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * 
 * SaberTooth - SaberTooth service for the sabertooth motor controller command
 * 
 * More Info: http://www.dimensionengineering.com/datasheets/Sabertooth2x25.pdf
 * 
 * Packet PseudoCode Putc(address); Putc(0); Putc(speed); Putc((address + 0 +
 * speed) & 0b01111111);
 * 
 * @author GroG
 * 
 */
public class Ssc32UsbServoController extends Service implements PortConnector, ServoController {

  class MotorData implements Serializable {
    private static final long serialVersionUID = 1L;
    transient MotorControl motor = null;
    /*
     * int PWMPin = -1; int dirPin0 = -1; int dirPin1 = -1; int motorPort = -1;
     * String port = null;
     */
    int portNumber;
  }

  private static final long serialVersionUID = 1L;

  public final static int PACKETIZED_SERIAL_MODE = 4;

  int mode = PACKETIZED_SERIAL_MODE;

  public static final int PINMODE = 4;

  public final static Logger log = LoggerFactory.getLogger(Ssc32UsbServoController.class);

  transient Serial serial;

  // range mapping

  private Integer address = 128;

  public static final int INPUT = 0x0;

  public static final int OUTPUT = 0x1;

  boolean setSaberToothBaud = false;

  // Motor name to its Data
  HashMap<String, MotorData> motors = new HashMap<String, MotorData>();

  public final static int MOTOR1_FORWARD = 0;

  public final static int MOTOR1_BACKWARD = 1;

  public final static int SET_MIN_VOLTAGE = 2;

  public final static int SET_MAX_VOLTAGE = 3;

  public final static int MOTOR2_FORWARD = 4;

  public final static int MOTOR2_BACKWARD = 5;

  // ----------Sabertooth Packetized Serial Mode Interface Begin
  // --------------

  public Ssc32UsbServoController(String n) {
    super(n);
  }

  public void connect(String port) throws IOException {
    connect(port, 115200, 8, 1, 0);
  }

  public void disconnect() {
    if (serial != null) {
      serial.disconnect();
    }
  }

  public Object[] getMotorData(String motorName) {
    return new Object[] { motors.get(motorName).portNumber };
  }

  // ----------Sabertooth Packetized Serial Mode Interface End --------------

  // ----------MotorController Interface Begin --------------

  // FIXME - this seems very Arduino specific?

  public void sendPacket(int command, int data) {
    try {
      if (serial == null || !serial.isConnected()) {
        error("serial device not connected");
        return;
      }

      // 9600

      if (!setSaberToothBaud) {
        serial.write(170);
        sleep(500);
        setSaberToothBaud = true;
      }

      serial.write(address);
      serial.write(command);
      serial.write(data);
      serial.write((address + command + data) & 0x7F);
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public void setAddress(Integer address) {
    this.address = address;
  }

  public void setMaxVoltage(int maxVolts) {
    int actualValue = (int) Math.round(maxVolts / 5.12);
    info("setting max voltage to %d volts - actual value %f", actualValue);
    sendPacket(SET_MAX_VOLTAGE, actualValue);
  }

  // ----------MotorController Interface End --------------

  public void setMinVoltage(int min) {
    int actualValue = (min - 6) * 5;
    info("setting max voltage to %d volts - actual value %d", actualValue);
    if (actualValue < 0 || actualValue > 120) {
      error("invalid value must be between 0 and 120 %d", actualValue);
      return;
    }
    sendPacket(SET_MIN_VOLTAGE, actualValue);
  }

  @Override
  public void startService() {
    super.startService();
    serial = (Serial) startPeer("serial");
    // serial.addByteListener(this);
  }

  public SerialDevice getSerial() {
    return serial;
  }

  void setBaudRate(int baudRate) {

    int value;
    switch (baudRate) {
      case 2400:
        value = 1;
        break;
      case 9600:
      default:
        value = 2;
        break;
      case 19200:
        value = 3;
        break;
      case 38400:
        value = 4;
        break;
      case 115200: // not valid ???
        value = 5;
        break;
    }

    sendPacket(15, value);

    // (1) flush() does not seem to wait until transmission is complete.
    // As a result, a Serial.end() directly after this appears to
    // not always transmit completely. So, we manually add a delay.
    // (2) Sabertooth takes about 200 ms after setting the baud rate to
    // respond to commands again (it restarts).
    // So, this 500 ms delay should deal with this.
    sleep(500);
  }

  // --- MotorController interface end ----

  @Override
  public boolean isConnected() {
    // TODO Auto-generated method stub
    return false;
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

    ServiceType meta = new ServiceType(Ssc32UsbServoController.class.getCanonicalName());
    meta.addDescription("Interface for the powerful Sabertooth motor controller");
    meta.addCategory("motor", "control");
    meta.addPeer("serial", "Serial", "Serial Port");

    return meta;
  }

  @Override
  public void connect(String port, int rate, int databits, int stopbits, int parity) throws IOException {
    serial.open(port, rate, databits, stopbits, parity);
  }

  @Override
  public void detach(DeviceControl device) {
    motors.remove(device);
  }

  @Override
  public int getDeviceCount() {
    return motors.size();
  }

  @Override
  public Set<String> getDeviceNames() {
    return motors.keySet();
  }

  // @Override - lame - should be override
  static public List<BoardType> getBoardTypes() {
    // currently only know of 1
    return new ArrayList<BoardType>();
  }

  @Override
  public void attach(ServoControl servo) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void attach(ServoControl servo, int pin) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoAttachPin(ServoControl servo, int pin) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoSweepStart(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoSweepStop(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoMoveTo(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoWriteMicroseconds(ServoControl servo, int uS) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoDetachPin(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoSetVelocity(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoSetAcceleration(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  ///////////// start new methods /////////////////

  public static void main(String[] args) {
    LoggingFactory.getInstance().configure();
    LoggingFactory.getInstance().setLevel(Level.WARN);

    try {

      String port = "COM12";

      // ---- Virtual Begin -----
      // VirtualDevice virtual = (VirtualDevice) Runtime.start("virtual",
      // "VirtualDevice");
      // virtual.createVirtualSerial(port);
      // virtual.getUART(); uart.setTimeout(300);
      // ---- Virtual End -----

      Runtime.start("gui", "SwingGui");
      Runtime.start("python", "Python");
      // Joystick joystick = (Joystick)Runtime.start("joystick",
      // "Joystick");
      // Runtime.start("joystick", "Joystick");

      Ssc32UsbServoController ssc = (Ssc32UsbServoController) Runtime.start("ssc", "Ssc32UsbServoController");
      ssc.connect(port);
      SerialDevice serial = ssc.getSerial();
      // 500 2500
      serial.write("#16P800 #17P1500 #27P1100 \r");
      serial.write("#16P500\r");
      serial.write("#16P2500\r");
      serial.write("#16P1500\r");// pos 0
      serial.write("#16P500S10\r");
      
      serial.write("#16P2000\r");

      // Runtime.start("webgui", "WebGui");
      // Runtime.start("motor", "Motor");

      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
