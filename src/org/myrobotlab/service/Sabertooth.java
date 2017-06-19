package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.myrobotlab.arduino.BoardType;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.PortConnector;
import org.myrobotlab.service.interfaces.SerialDevice;
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
public class Sabertooth extends Service implements PortConnector, MotorController {

  private static final long serialVersionUID = 1L;

  public final static int PACKETIZED_SERIAL_MODE = 4;

  int mode = PACKETIZED_SERIAL_MODE;

  public static final int PINMODE = 4;

  public final static Logger log = LoggerFactory.getLogger(Sabertooth.class);

  transient Serial serial;

  // range mapping

  private Integer address = 128;

  public static final int INPUT = 0x0;

  public static final int OUTPUT = 0x1;

  boolean setSaberToothBaud = false;

  HashMap<String, MotorPort> motors = new HashMap<String, MotorPort>();

  public final static int MOTOR1_FORWARD = 0;

  public final static int MOTOR1_BACKWARD = 1;

  public final static int SET_MIN_VOLTAGE = 2;

  public final static int SET_MAX_VOLTAGE = 3;

  public final static int MOTOR2_FORWARD = 4;

  public final static int MOTOR2_BACKWARD = 5;

  public Sabertooth(String n) {
    super(n);
  }

  public void connect(String port) throws IOException {
    connect(port, 9600, 8, 1, 0);
  }

  public void disconnect() {
    if (serial != null) {
      serial.disconnect();
    }
  }

  public void driveBackwardsMotor1(int speed) {
    if (speed < 0 || speed > 127) {
      error("invalid speed", speed);
      return;
    }
    sendPacket(MOTOR1_BACKWARD, speed);
  }

  public void driveBackwardsMotor2(int speed) {
    if (speed < 0 || speed > 127) {
      error("invalid speed", speed);
      return;
    }
    sendPacket(MOTOR2_BACKWARD, speed);
  }

  public void driveForwardMotor1(int speed) {
    if (speed < 0 || speed > 127) {
      error("invalid speed", speed);
      return;
    }
    sendPacket(MOTOR1_FORWARD, speed);
  }

  public void driveForwardMotor2(int speed) {
    if (speed < 0 || speed > 127) {
      error("invalid speed %s", speed);
      return;
    }
    sendPacket(MOTOR2_FORWARD, speed);
  }

  public boolean motorDetach(String name) {
    if (motors.containsKey(name)) {
      motors.remove(name);
      return true;
    }
    return false;
  }

  public void motorMove(String name) {
    motorMove((MotorControl) Runtime.getService(name));
  }

  public void motorMoveTo(String name, Integer position) {
    error("not implemented");
  }

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
    // serial.addByteListener(this); - there is no feedback .. I think
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
  public void motorMove(MotorControl mc) {

    if (!motors.containsKey(mc.getName())) {
      error("%s not attached to %s", mc.getName(), getName());
      return;
    }

    MotorPort motor = motors.get(mc.getName());
    int portNumber = motor.getPortNumber();
    double pwr = motor.getPowerLevel();
    int power = (int) (pwr * 127);

    if (portNumber == 1) {
      if (pwr >= 0) {
        driveForwardMotor1(power);
      } else {
        driveBackwardsMotor1(Math.abs(power));
      }
    } else if (portNumber == 2) {
      if (pwr >= 0) {
        driveForwardMotor2(power);
      } else {
        driveBackwardsMotor2(Math.abs(power));
      }
    } else {
      error("invalid port number %d", portNumber);
    }

  }

  @Override
  public void motorMoveTo(MotorControl motor) {
    // TODO Auto-generated method stub

  }

  @Override
  public void motorStop(MotorControl motor) {
    motor.move(0);
  }

  @Override
  public boolean isConnected() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void motorReset(MotorControl motor) {
    // TODO Auto-generated method stub

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

    ServiceType meta = new ServiceType(Sabertooth.class.getCanonicalName());
    meta.addDescription("Interface for the powerful Sabertooth motor controller");
    meta.addCategory("motor", "control");
    meta.addPeer("serial", "Serial", "Serial Port");

    return meta;
  }

  /**
   * Sabertooth uses numeric identifiers for ports. This just does a type
   * conversion of the portNumber for attaching with the MotorConfigPort data
   * 
   * @param motor
   * @param portNumber
   * @throws Exception
   */
  public void attach(MotorControl motor, Integer portNumber) throws Exception {
    attach(motor, String.format("%d", portNumber));
  }

  /**
   * Because many motor controllers have String labeled ports .e.g. "PortA,
   * PortB, etc" the universal attach for MotorConfigPort uses a String.
   * 
   * @param motor
   * @param portNumber
   * @throws Exception
   */
  public void attach(MotorControl motor, String portNumber) throws Exception {
    if (isAttached(motor)) {
      log.info("{}.attach({}) already attached", getName(), motor.getName());
      return;
    }
    motors.put(motor.getName(), new MotorPort(portNumber));
    motor.attachMotorController(this);
  }

  /**
   * Sabertooth is a serial device, so it has a PortConnector interface.
   */
  @Override
  public void connect(String port, int rate, int databits, int stopbits, int parity) throws IOException {
    serial.open(port, rate, databits, stopbits, parity);
  }

  @Override
  public void detach(DeviceControl device) {
    motors.remove(device);
  }

  @Override
  public int getAttachedCount() {
    return motors.size();
  }

  @Override
  public Set<String> getAttachedNames() {
    return motors.keySet();
  }

  // @Override - lame - should be override
  static public List<BoardType> getBoardTypes() {
    // currently only know of 1
    return new ArrayList<BoardType>();
  }

  // @Override
  public boolean isAttached(MotorControl control) {
    return motors.containsKey(control.getName());
  }

  public static void main(String[] args) {
    LoggingFactory.init("INFO");

    try {

      String port = "COM19";

      // ---- Virtual Begin -----
      VirtualDevice virtual = (VirtualDevice) Runtime.start("virtual", "VirtualDevice");
      virtual.createVirtualSerial(port);
      // virtual.getUART(); uart.setTimeout(300);
      // ---- Virtual End -----

      // Runtime.start("webgui", "WebGui");
      // Runtime.start("python", "Python");
      // Joystick joystick = (Joystick)Runtime.start("joystick",
      // "Joystick");
      Runtime.start("joystick", "Joystick");

      Sabertooth saber = (Sabertooth) Runtime.start("saber", "Sabertooth");
      saber.connect(port);

      // be "true" to the interface
      MotorController mc = (MotorController) saber;
      
      MotorPort motor01 = (MotorPort) Runtime.start("motor01", "MotorPort");
      MotorPort motor02 = (MotorPort) Runtime.start("motor02", "MotorPort");
      motor01.setPortNumber(1);
      motor01.setPortNumber(2);

      motor01.attachMotorController(mc);
      motor02.attachMotorController(mc);

      motor01.move(0);
      motor01.move(0.15);
      motor01.move(0.30);
      motor01.move(0.40);

      motor01.stop();

      motor01.move(0.15);
      motor01.stopAndLock();
      motor01.move(0.40);
      motor01.unlock();

      saber.driveForwardMotor1(20);
      saber.driveForwardMotor1(30);
      saber.driveForwardMotor1(60);
      saber.driveForwardMotor1(110);
      saber.driveForwardMotor1(0);

      saber.driveForwardMotor2(20);
      saber.driveForwardMotor2(30);
      saber.driveForwardMotor2(60);
      saber.driveForwardMotor2(110);
      saber.driveForwardMotor2(0);

      // Motor m1 = (Motor) Runtime.start("m1", "Motor");

      // Motor m2 = (Motor) Runtime.createAndStart("m2", "Motor");

      // Runtime.start("gui", "SwingGui");
      Runtime.start("webgui", "WebGui");
      Runtime.start("motor", "Motor");

      saber.driveForwardMotor1(100);

      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
