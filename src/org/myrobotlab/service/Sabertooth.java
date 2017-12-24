package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.abstracts.AbstractMotorController;
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
 * speed) &amp; 0b01111111);
 * 
 * @author GroG
 * 
 */
public class Sabertooth extends AbstractMotorController implements PortConnector, MotorController {

  private static final long serialVersionUID = 1L;

  public final static int PACKETIZED_SERIAL_MODE = 4;

  int mode = PACKETIZED_SERIAL_MODE;

  public static final int PINMODE = 4;

  public final static Logger log = LoggerFactory.getLogger(Sabertooth.class);

  transient SerialDevice serial;

  private Integer address = 128;

  public static final int INPUT = 0x0;

  public static final int OUTPUT = 0x1;

  boolean setSaberToothBaud = false;

  /**
   * attached motors <names, port>
   */
  transient HashMap<String, MotorPort> motors = new HashMap<String, MotorPort>();

  List<String> ports = new ArrayList<String>();

  public final static int MOTOR1_FORWARD = 0;

  public final static int MOTOR1_BACKWARD = 1;

  public final static int SET_MIN_VOLTAGE = 2;

  public final static int SET_MAX_VOLTAGE = 3;

  public final static int MOTOR2_FORWARD = 4;

  public final static int MOTOR2_BACKWARD = 5;

  public Sabertooth(String n) {
    super(n);
    // add motor ports the sabertooth supports
    ports.add("m1");
    ports.add("m2");
    powerMapper = new Mapper(-1.0, 1.0, -127, 127);
  }

  public void connect(String port) throws Exception {
    connect(port, 9600, 8, 1, 0);
  }

  public void disconnect() throws IOException {
    if (serial != null) {
      serial.close();
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
    String port = motor.getPort();
    
    /// double pwr = motor.getPowerLevel();
    int power = (int)powerMapper.calcOutput(mc.getPowerLevel());
    // int power = (int) (pwr * 127);

    if (port.equals("m1")) {
      if (power >= 0) {
        driveForwardMotor1(power);
      } else {
        driveBackwardsMotor1(Math.abs(power));
      }
    } else if (port.equals("m2")) {
      if (power >= 0) {
        driveForwardMotor2(power);
      } else {
        driveBackwardsMotor2(Math.abs(power));
      }
    } else {
      error("invalid port number %d", port);
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
    meta.addDescription("interface for the powerful Sabertooth motor controller");
    meta.addCategory("motor", "control");
    meta.addPeer("serial", "Serial", "Serial Port");

    return meta;
  }

  /**
   * Sabertooth is a serial device, so it has a PortConnector interface.
   */
  @Override
  public void connect(String port, int rate, int databits, int stopbits, int parity) throws Exception {
    if (serial == null) {
      serial = (Serial) startPeer("serial");
    }
    log.info("{} opening serial port {}|{}|{}|{}", port, rate, databits, stopbits, parity);
    serial.open(port, rate, databits, stopbits, parity);
    // not much choice here :(
    // sabertooth is not 'readable' and connecting serial is almost always
    // an asynchronous process - since we have no way to verify the port is open
    // we sadly must 
    sleep(3000);
  }

  public void detach(MotorControl device) {
    motors.remove(device);
  }

  // FIXME - become interface for motor port shields & controllers
  public List<String> getPorts() {
    return ports;
  }

  @Override
  public Set<String> getAttached() {
    return motors.keySet();
  }

  /**
   * Routing Attach - routes ServiceInterface.attach(service) to appropriate
   * methods for this class
   */
  @Override
  public void attach(Attachable service) throws Exception {
    // check if service already attached
    if (isAttached(service)) {
      log.info("{} is attached to {}", service.getName(), getName());
      return;
    }

    if (MotorPort.class.isAssignableFrom(service.getClass())) {
      MotorPort motor = (MotorPort) service;
      String port = motor.getPort();

      if (port == null || (!ports.contains(port))) {
        throw new IOException("port number in motor must be set to m1 or m2");
      }

      motors.put(motor.getName(), motor);

      // give opportunity for motor to attach
      motor.attach(this);

      // made changes broadcast it
      broadcastState();
      return;

    } else if (SerialDevice.class.isAssignableFrom(service.getClass())) {

      serial = (SerialDevice) service;

      // here we check and warn regarding config - but
      // it "might" be right if the user has customized it
      // this works well - the user controls all config
      // but the attach can check and report on it
      if (serial.getRate() != 9600) {
        warn("default rate for Sabertooth is 9600 serial is currently at %s", serial.getRate());
      }

      // give serial an opportunity to attach to this service
      serial.attach(this);

      if (serial.isConnected()) {
        if (!setSaberToothBaud) {
          serial.write(170);
          sleep(500);
          setSaberToothBaud = true;
        }
      }

      // made changes broadcast it
      broadcastState();
      return;
    }

    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
  }

  @Override
  public boolean isAttached(Attachable service) {
    return motors.containsKey(service.getName()) || (serial != null && serial.getName().equals(service.getName()));
  }

  @Override
  public void connect(String port, int rate) throws Exception {
    connect(port, rate, 8, 1, 0);
  }

  @Override
  public void detach() {
    for (String name : motors.keySet()) {
      Motor m = (Motor) Runtime.getService(name);
      if (m != null) {
        m.detach(this);
      }
    }

    if (serial != null) {
      serial.detach(this);
    }
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init("INFO");

      boolean virtual = true;
      //////////////////////////////////////////////////////////////////////////////////
      // Sabertooth.py
      // categories: motor
      // more info @: http://myrobotlab.org/service/Sabertooth
      //////////////////////////////////////////////////////////////////////////////////
      // uncomment for virtual hardware
      // virtual = True

      String port = "COM14";
      //String port = "/dev/ttyUSB0";

      // start optional virtual serial service, used for test
      if (virtual) {
        // use static method Serial.connectVirtualUart to create
        // a virtual hardware uart for the serial service to
        // connect to
        Serial uart = Serial.connectVirtualUart(port);
        uart.logRecv(true); // dump bytes sent from sabertooth
      }
      // start the services
      Runtime.start("gui", "SwingGui");
      Sabertooth sabertooth = (Sabertooth) Runtime.start("sabertooth", "Sabertooth");
      MotorPort m1 = (MotorPort) Runtime.start("m1", "MotorPort");
      MotorPort m2 = (MotorPort) Runtime.start("m2", "MotorPort");
      Joystick joy = (Joystick) Runtime.start("joy", "Joystick");
      // Arduino arduino = (Arduino)Runtime.start("arduino","Arduino");

      // configure services
      m1.setPort("m1");
      m2.setPort("m2");
      joy.setController(5); // 0 on Linux

      // attach services
      sabertooth.attach(m1);
      sabertooth.attach(m2);
      m1.attach(joy.getAxis("y"));
      m2.attach(joy.getAxis("rz"));
      
      m1.setInverted(true);
      m2.setInverted(true);
      // m2.attach(arduino.getPin("A4"));

      // FIXME - sabertooth.attach(motor1) & sabertooth.attach(motor2)
      // FIXME - motor1.attach(joystick) !
      sabertooth.connect(port);

      // m1.stop();
      // m2.stop();

      boolean done = true;
      if (done) {
        return;
      }

      // speed up the motor
      for (int i = 0; i < 100; ++i) {
        double pwr = i * .01;
        log.info("power {}", pwr);
        m1.move(pwr);
        sleep(100);
      }

      sleep(1000);

      // slow down the motor
      for (int i = 100; i > 0; --i) {
        double pwr = i * .01;
        log.info("power {}", pwr);
        m1.move(pwr);
        sleep(100);
      }

      // move motor clockwise
      m1.move(0.3);
      sleep(1000);
      m1.stop();

      // move motor counter-clockwise
      m1.move(-0.3);
      sleep(1);
      m1.stop();

      // TODO - stopAndLock

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
