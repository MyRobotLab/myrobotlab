package org.myrobotlab.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.sensor.EncoderPublisher;
import org.myrobotlab.serial.CRC;
import org.myrobotlab.service.Pid.PidData;
import org.myrobotlab.service.abstracts.AbstractMotorController;
import org.myrobotlab.service.config.AbstractMotorControllerConfig;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.PortConnector;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

/**
 * 
 * RoboClaw - RoboClaw service for the roboclaw motor controller command
 * 
 * <pre>
 * 
 * RoboClaw - http://downloads.ionmc.com/docs/roboclaw_user_manual.pdf
 * 
 * The basic command structures consist of an address byte, command byte, data
 * bytes and a CRC16 16bit checksum. The amount of data each command will send
 * or receive can vary.
 * 
 * | --------ADDRESS------ |COMMAND|DATA |0x80 (128) - 0x87 (135)|
 * 
 * Big Endian -
 * 
 * BAUD OPTIONS 2400 9600 19200 38400 57600 115200 230400 460800
 * 
 * 
 * PACKET ACK = 0xFF
 * </pre>
 * 
 * Packet PseudoCode Putc(address); Putc(0); Putc(speed); Putc((address + 0 +
 * speed) &amp; 0b01111111);
 * 
 * @author GroG
 * 
 *         FIXME - crc validation check - ie do not set any local fields unless
 *         this value IS correct
 * 
 */
public class RoboClaw extends AbstractMotorController<AbstractMotorControllerConfig> implements EncoderPublisher, PortConnector, MotorController, SerialDataListener {

  private static final long serialVersionUID = 1L;

  public final static int PACKETIZED_SERIAL_MODEx = 4;

  public static final int PINMODE = 4;

  public final static Logger log = LoggerFactory.getLogger(RoboClaw.class);

  transient Serial serial;

  Integer address = 128;

  List<String> ports = new ArrayList<String>();

  String firmwareVersion;

  Integer logicBatteryVoltageLevel;

  Integer mainBatteryVoltage;

  Integer boardTemp;

  int buffer = 1;

  final MotorData m1 = new MotorData();
  final MotorData m2 = new MotorData();

  Integer pwmMode;

  final Poller poller = new Poller();

  public RoboClaw(String n, String id) {
    super(n, id);
    // add motor ports the roboclaw supports
    ports.add("m1");
    ports.add("m2");
    map(-1.0, 1.0, -127, 127);
  }

  public static class MotorData implements Serializable {

    private static final long serialVersionUID = 1L;

    PidData pid;
    Long qpps;
    Long rawSpeed;
    Long encoderSpeed;
    Integer pwm;
    EncoderData encoder;
  }

  // FIXME separate the methods into mrl & roboclaw native
  @Override
  public void connect(String port) throws Exception {
    connect(port, 38400, 8, 1, 0);
  }

  @Override
  public void disconnect() throws IOException {
    if (serial != null) {
      serial.close();
    }
  }

  @Override
  public boolean motorDetach(String name) {
    motors.remove(name);
    return true;
  }

  @Override
  public void motorMove(String name) {
    motorMove((MotorControl) Runtime.getService(name));
  }

  @Override
  public void motorMoveTo(String name, Integer position) {
    error("not implemented");
  }

  // public void sendPacket(int command, int data) {
  public void sendPacket(int... d) {
    try {
      if (serial == null || !serial.isConnected()) {
        error("serial device not connected");
        return;
      }

      // packet[0] == address
      // packet[1] == command
      // packet[n] == data
      // packet[n+1] == crc-1
      // packet[n+2] == crc-2

      StringBuilder sb = new StringBuilder();

      byte[] packet = new byte[d.length + 2];
      for (int i = 0; i < d.length; ++i) {
        packet[i] = (byte) d[i];
        sb.append(String.format("%02X ", d[i]));
      }

      // 9600
      /*
       * if (!setRoboClawBaud) { serial.write(170); sleep(500); setRoboClawBaud
       * = true; }
       */

      long xmodemcrc16 = CRC.calculateCRC(CRC.Parameters.XMODEM, Arrays.copyOfRange(packet, 0, d.length));
      packet[d.length] = (byte) ((xmodemcrc16 >> 8) & 0xFF);
      sb.append(String.format("%02X ", (byte) ((xmodemcrc16 >> 8) & 0xFF)));
      packet[d.length + 1] = (byte) (xmodemcrc16 & 0xFF);
      sb.append(String.format("%02X ", (byte) (xmodemcrc16 & 0xFF)));

      log.debug(" sendPacket {}", sb);

      /**
       * <pre>
      
      81 00 20 28 08 
      81 00 20 28 08 
      81 00 20 28 08 
      81 05 20 d7 fd 
      81 05 20 d7 fd 
      81 05 20 d7 fd 
      81 01 20 1b 39 
      81 01 20 1b 39      
      81 01 20 1b 39
      
      aa 81 00 20 28 08 
      81 00 20 28 08 
      81 00 20 28 08
       * 
       * </pre>
       **/

      serial.write(packet);

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public void setAddress(Integer address) {
    this.address = address;
  }

  // ----------MotorController Interface End --------------

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

    sendPacket(0x87, 15, value);

    // (1) flush() does not seem to wait until transmission is complete.
    // As a result, a Serial.end() directly after this appears to
    // not always transmit completely. So, we manually add a delay.
    // (2) RoboClaw takes about 200 ms after setting the baud rate to
    // respond to commands again (it restarts).
    // So, this 500 ms delay should deal with this.
    // - FIXME - this is proportional to the rate e.g. sleep(delay/baud)
    sleep(500);
  }

  // --- MotorController interface end ----

  @Override
  public void motorMove(MotorControl mc) {

    if (!motors.contains(mc.getName())) {
      error("%s not attached to %s", mc.getName(), getName());
      return;
    }

    MotorPort motor = (MotorPort) Runtime.getService(mc.getName());
    String port = motor.getPort();

    /// double pwr = motor.getPowerLevel();
    int power = (int) motorCalcOutput(motor);
    // int power = (int) (pwr * 127);

    if (port.equals("m1")) {
      if (power >= 0) {
        driveForwardM1(power);
      } else {
        driveBackwardM1(Math.abs(power));
      }
    } else if (port.equals("m2")) {
      if (power >= 0) {
        driveForwardM2(power);
      } else {
        driveBackwardM1(Math.abs(power));
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

  public class Poller implements Runnable {

    transient Thread worker;

    boolean isRunning = false;

    public boolean readM1Encoder = false;
    public boolean readM2Encoder = false;
    public boolean readM1Motor = false;
    public boolean readM2Motor = false;

    @Override
    public void run() {
      try {
        isRunning = true;
        while (isRunning) {

          if (readM1Encoder) {
            readEncoderM1();
          }

          if (readM2Encoder) {
            readEncoderM2();
          }

          if (readM1Motor) {
            // readEncoderM1();
          }

          if (readM2Motor) {
            // readEncoderM2();
          }

          sleep(100);
        }
      } catch (Exception e) {
        log.info("stop reading data");
        isRunning = false;
        worker = null;
      }
    }

    public void start() {
      if (worker == null) {
        worker = new Thread(this, String.format("%s.poller", getName()));
        worker.start();
      }
    }

    public void stop() {
      if (worker != null) {
        isRunning = false;
        worker = null;
      }
    }

  }

  public void startPolling() {
    poller.start();
  }

  public void startEncoderM1() {
    poller.readM1Encoder = true;
    poller.start();
  }

  public void startEncoderM2() {
    poller.readM2Encoder = true;
    poller.start();
  }

  public void stopPolling() {
    poller.stop();
  }

  /**
   * RoboClaw is a serial device, so it has a PortConnector interface.
   */
  @Override
  public void connect(String port, int rate, int databits, int stopbits, int parity) throws Exception {
    if (serial == null) {
      Serial s = (Serial) startPeer("serial");
      attach(s);
    }
    log.info("{} opening serial port {}|{}|{}|{}", port, rate, databits, stopbits, parity);
    serial.open(port, rate, databits, stopbits, parity);
    // not much choice here :(
    // roboclaw is not 'readable' and connecting serial is almost always
    // an asynchronous process - since we have no way to verify the port is open
    // we sadly must
    sleep(3000);
  }

  @Override
  public void detach(MotorControl device) {
    motors.remove(device);
  }

  // FIXME - become interface for motor port shields & controllers
  @Override
  public List<String> getPorts() {
    return ports;
  }

  @Override
  public Set<String> getAttached() {
    return motors;
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

      motors.add(motor.getName());

      // give opportunity for motor to attach
      motor.attach(this);

      // made changes broadcast it
      broadcastState();
      return;

    } else if (SerialDevice.class.isAssignableFrom(service.getClass())) {
      // FIXME - use SerialDevice
      serial = (Serial) service;
      serial.setTimeout(40);

      // here we check and warn regarding config - but
      // it "might" be right if the user has customized it
      // this works well - the user controls all config
      // but the attach can check and report on it
      /*
       * if (serial.getRate() != 9600) {
       * warn("default rate for RoboClaw is 9600 serial is currently at %s",
       * serial.getRate()); }
       */

      // serial.attach(this); - not needed since the serial service is very
      // "low" level,
      // it doesn't attach to anything - but "higher" level services attach to
      // it.
      serial.addByteListener(this);

      /*
       * if (serial.isConnected()) { if (!setRoboClawBaud) { serial.write(170);
       * sleep(500); setRoboClawBaud = true; } }
       */

      // made changes broadcast it
      broadcastState();
      return;
    }

    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
  }

  @Override
  public boolean isAttached(Attachable service) {
    return motors.contains(service.getName()) || (serial != null && serial.getName().equals(service.getName()));
  }

  @Override
  public void connect(String port, int rate) throws Exception {
    connect(port, rate, 8, 1, 0);
  }

  @Override
  public void detach() {
    for (String name : motors) {
      Motor m = (Motor) Runtime.getService(name);
      if (m != null) {
        m.detach(this);
      }
    }

    if (serial != null) {
      serial.detach(this);
    }
  }

  /**
   * <pre>
   * ////////////////////////////// RoboClaw Native Methods
   * ////////////////////////////// //////////////////////////////
   * </pre>
   */

  /**
   * <pre>
      0 - Drive Forward M1
      Drive motor 1 forward. Valid data range is 0 - 127. A value of 127 = full speed forward, 64 =
      about half speed forward and 0 = full stop.
      Send: [Address, 0, Value, CRC(2 bytes)]
      Receive: [0xFF]
   * </pre>
   */
  void driveForwardM1(int value) {
    sendPacket(address, 0, value);
  }

  /**
   * <pre>
    1 - Drive Backwards M1
    Drive motor 1 backwards. Valid data range is 0 - 127. A value of 127 full speed backwards, 64 =
    about half speed backward and 0 = full stop.
    Send: [Address, 1, Value, CRC(2 bytes)]
    Receive: [0xFF]
   * </pre>
   * 
   * @param value
   *          v
   */
  public void driveBackwardM1(int value) {
    sendPacket(address, 1, value);
  }

  /**
   * <pre>
  2 - Set Minimum Main Voltage (Command 57 Preferred)
  Sets main battery (B- / B+) minimum voltage level. If the battery voltages drops below the set
  voltage level RoboClaw will stop driving the motors. The voltage is set in .2 volt increments. A
  value of 0 sets the minimum value allowed which is 6V. The valid data range is 0 - 140 (6V -
  34V). The formula for calculating the voltage is: (Desired Volts - 6) x 5 = Value. Examples of
  valid values are 6V = 0, 8V = 10 and 11V = 25.
  Send: [Address, 2, Value, CRC(2 bytes)]
  Receive: [0xFF]
  RoboClaw Series
  Brushed DC Motor Controllers
  RoboClaw Series User Manual 65
   * </pre>
   * 
   * @param value
   *          v
   */
  public void setMiniMainVoltage(int value) {
    sendPacket(address, 2, value);
  }

  /**
   * <pre>
  3 - Set Maximum Main Voltage (Command 57 Preferred)
  Sets main battery (B- / B+) maximum voltage level. The valid data range is 30 - 175 (6V -
  34V). During regenerative breaking a back voltage is applied to charge the battery. When using
  a power supply, by setting the maximum voltage level, RoboClaw will, before exceeding it, go
  into hard braking mode until the voltage drops below the maximum value set. This will prevent
  overvoltage conditions when using power supplies. The formula for calculating the voltage is:
  Desired Volts x 5.12 = Value. Examples of valid values are 12V = 62, 16V = 82 and 24V = 123.
  Send: [Address, 3, Value, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param value
   *          v
   */
  public void setMaxMainVoltage(int value) {
    sendPacket(address, 3, value);
  }

  /**
   * <pre>
  4 - Drive Forward M2
  Drive motor 2 forward. Valid data range is 0 - 127. A value of 127 full speed forward, 64 = about
  half speed forward and 0 = full stop.
  Send: [Address, 4, Value, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param value
   *          v
   */
  public void driveForwardM2(int value) {
    sendPacket(address, 4, value);
  }

  /**
   * <pre>
  5 - Drive Backwards M2
  Drive motor 2 backwards. Valid data range is 0 - 127. A value of 127 full speed backwards, 64 =
  about half speed backward and 0 = full stop.
  Send: [Address, 5, Value, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param value
   *          v
   */
  public void driveBackwardM2(int value) {
    sendPacket(address, 5, value);
  }

  /**
   * <pre>
  6 - Drive M1 (7 Bit)
  Drive motor 1 forward or reverse. Valid data range is 0 - 127. A value of 0 = full speed reverse,
  64 = stop and 127 = full speed forward.
  Send: [Address, 6, Value, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param value
   *          v
   */
  public void driveM1(int value) {
    sendPacket(address, 6, value);
  }

  /**
   * <pre>
  7 - Drive M2 (7 Bit)
  Drive motor 2 forward or reverse. Valid data range is 0 - 127. A value of 0 = full speed reverse,
  64 = stop and 127 = full speed forward.
  Send: [Address, 7, Value, CRC(2 bytes)]
  Receive: [0xFF]
  RoboClaw Series
  Brushed DC Motor Controllers
  RoboClaw Series User Manual 66
  Commands 8 - 13 Mixed Mode Compatibility Commands
  The following commands are mix mode commands used to control speed and turn for differential
  steering. Before a command is executed, both valid drive and turn data packets are required
  Once RoboClaw begins to operate the motors turn and speed can be updated independently.
   * </pre>
   * 
   * @param value
   *          v
   */
  public void driveM2(int value) {
    sendPacket(address, 7, value);
  }

  /**
   * <pre>
  8 - Drive Forward
  Drive forward in mix mode. Valid data range is 0 - 127. A value of 0 = full stop and 127 = full
  forward.
  Send: [Address, 8, Value, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param value
   *          v
   */
  public void driveForward(int value) {
    sendPacket(address, 8, value);
  }

  /**
   * <pre>
  9 - Drive Backwards
  Drive backwards in mix mode. Valid data range is 0 - 127. A value of 0 = full stop and 127 = full
  reverse.
  Send: [Address, 9, Value, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param value
   *          v
   */
  public void driveBackward(int value) {
    sendPacket(address, 9, value);
  }

  /**
   * <pre>
  10 - Turn right
  Turn right in mix mode. Valid data range is 0 - 127. A value of 0 = stop turn and 127 = full
  speed turn.
  Send: [Address, 10, Value, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param value
   *          v
   */
  public void turnRight(int value) {
    sendPacket(address, 10, value);
  }

  /**
   * <pre>
  11 - Turn left
  Turn left in mix mode. Valid data range is 0 - 127. A value of 0 = stop turn and 127 = full speed
  turn.
  Send: [Address, 11, Value, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param value
   *          v
   */
  public void turnLeft(int value) {
    sendPacket(address, 11, value);
  }

  /**
   * <pre>
  12 - Drive Forward or Backward (7 Bit)
  Drive forward or backwards. Valid data range is 0 - 127. A value of 0 = full backward, 64 = stop
  and 127 = full forward.
  Send: [Address, 12, Value, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param value
   *          v
   */
  public void driveForwardOrBackward(int value) {
    sendPacket(address, 12, value);
  }

  /**
   * <pre>
  13 - Turn Left or Right (7 Bit)
  Turn left or right. Valid data range is 0 - 127. A value of 0 = full left, 0 = stop turn and 127 = full
  right.
  Send: [Address, 13, Value, CRC(2 bytes)]
  Receive: [0xFF
   * </pre>
   * 
   * @param value
   *          v
   */
  public void turnLeftOrRight(int value) {
    sendPacket(address, 13, value);
  }

  /**
   * <pre>
  21 - Read Firmware Version
  Read RoboClaw firmware version. Returns up to 48 bytes(depending on the Roboclaw model) and
  is terminated by a line feed character and a null character.
  Send: [Address, 21]
  Receive: [“RoboClaw 10.2A v4.1.11”,10,0, CRC(2 bytes)]
  The command will return up to 48 bytes. The return string includes the product name and
  firmware version. The return string is terminated with a line feed (10) and null (0) character.
   * </pre>
   * 
   * @return the firmware version
   */
  public String readFirmwareVersion() {
    sendPacket(address, 21);
    // GAH !! "up to" 48 bytes :(
    // TODO -
    return firmwareVersion;
  }

  /**
   * <pre>
  24 - Read Main Battery Voltage Level
  Read the main battery voltage level connected to B+ and B- terminals. The voltage is returned in
  10ths of a volt(eg 300 = 30v).
  Send: [Address, 24]
  Receive: [Value(2 bytes), CRC(2 bytes)]
   * </pre>
   * 
   * @return battery voltage
   */
  public Integer readMainBatteryVoltage() {

    byte[] data = sendReadPacket(4, address, 24);
    if (data != null) {
      mainBatteryVoltage = bytes2ToInt(data, 0);
    }

    return mainBatteryVoltage;
  }

  /**
   * <pre>
  25 - Read Logic Battery Voltage Level
  Read a logic battery voltage level connected to LB+ and LB- terminals. The voltage is returned in
  10ths of a volt(eg 50 = 5v).
  Send: [Address, 25]
  Receive: [Value.Byte1, Value.Byte0, CRC(2 bytes)]
   * </pre>
   * 
   * @return voltage
   */
  public Integer readLogicbatteryVoltage() {
    logicBatteryVoltageLevel = null;

    byte[] data = sendReadPacket(4, address, 25);
    if (data != null) {
      logicBatteryVoltageLevel = bytes2ToInt(data, 0);
    }

    return logicBatteryVoltageLevel;
  }

  /**
   * <pre>
  26 - Set Minimum Logic Voltage Level
  Note: This command is included for backwards compatibility. We recommend you use
  command 58 instead.
  Sets logic input (LB- / LB+) minimum voltage level. RoboClaw will shut down with an error if
  the voltage is below this level. The voltage is set in .2 volt increments. A value of 0 sets the
  minimum value allowed which is 6V. The valid data range is 0 - 140 (6V - 34V). The formula for
  calculating the voltage is: (Desired Volts - 6) x 5 = Value. Examples of valid values are 6V = 0,
  8V = 10 and 11V = 25.
  Send: [Address, 26, Value, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param value
   *          v
   */
  public void setMinLogicVoltage(int value) {
    sendPacket(address, 26, value);
  }

  /**
   * <pre>
  27 - Set Maximum Logic Voltage Level
  Note: This command is included for backwards compatibility. We recommend you use
  command 58 instead.
  Sets logic input (LB- / LB+) maximum voltage level. The valid data range is 30 - 175 (6V -
  34V). RoboClaw will shutdown with an error if the voltage is above this level. The formula for
  calculating the voltage is: Desired Volts x 5.12 = Value. Examples of valid values are 12V = 62,
  16V = 82 and 24V = 123.
  Send: [Address, 27, Value, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param value
   *          v
   */

  public void setMaxLogicVoltage(int value) {
    sendPacket(address, 27, value);
  }

  /**
   * <pre>
  48 - Read Motor PWM values
  Read the current PWM output values for the motor channels. The values returned are +/-32767.
  The duty cycle percent is calculated by dividing the Value by 327.67.
  Send: [Address, 48]
  Receive: [M1 PWM(2 bytes), M2 PWM(2 bytes), CRC(2 bytes)]
   * </pre>
   */

  public void readMotorPwmValues() {
    sendPacket(address, 48);

    byte[] data = sendReadPacket(4, address, 25);
    if (data != null) {
      m1.pwm = bytes2ToInt(data, 0);
      m2.pwm = bytes2ToInt(data, 2);
    }

  }

  /**
   * <pre>
  49 - Read Motor Currents
  Read the current draw from each motor in 10ma increments. The amps value is calculated by
  dividing the value by 100.
  Send: [Address, 49]
  Receive: [M1 Current(2 bytes), M2 Currrent(2 bytes), CRC(2 bytes)]
   * </pre>
   * 
   * @param value
   *          v
   */
  public void readMotorCurrents(int value) {
    sendPacket(address, 49);
    // TODO - implement
  }

  /**
   * <pre>
  57 - Set Main Battery Voltages
  Set the Main Battery Voltage cutoffs, Min and Max. Min and Max voltages are in 10th of a volt
  increments. Multiply the voltage to set by 10.
  Send: [Address, 57, Min(2 bytes), Max(2bytes, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param min
   *          min voltage
   * @param max
   *          max voltage
   */
  public void setMainBatteryVoltages(int min, int max) {
    sendPacket(address, 57, byte1(min), byte0(min), byte1(max), byte0(max));
  }

  private int byte3(int value) {
    return (value >> 24) & 0xFF;
  }

  private int byte2(int value) {
    return (value >> 16) & 0xFF;
  }

  private int byte1(int value) {
    return (value >> 8) & 0xFF;
  }

  private int byte0(int value) {
    return value & 0xFF;
  }

  /**
   * <pre>
  58 - Set Logic Battery Voltages
  Set the Logic Battery Voltages cutoffs, Min and Max. Min and Max voltages are in 10th of a volt
  increments. Multiply the voltage to set by 10.
  Send: [Address, 58, Min(2 bytes), Max(2bytes, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param min
   *          voltage
   * @param max
   *          voltage
   */
  public void setLogicBatteryVoltages(int min, int max) {
    sendPacket(address, 58, byte1(min), byte0(min), byte1(max), byte0(max));
  }

  /**
   * <pre>
  59 - Read Main Battery Voltage Settings
  Read the Main Battery Voltage Settings. The voltage is calculated by dividing the value by 10
  Send: [Address, 59]
  Receive: [Min(2 bytes), Max(2 bytes), CRC(2 bytes)]
   * </pre>
   */
  public void readMainBatteryVoltageSettings() {
    sendPacket(address, 59);
  }

  /**
   * <pre>
  60 - Read Logic Battery Voltage Settings
  Read the Logic Battery Voltage Settings. The voltage is calculated by dividing the value by 10
  Send: [Address, 60]
  Receive: [Min(2 bytes), Max(2 bytes), CRC(2 bytes)]
   * </pre>
   */
  public void readLogicBatteryVoltageSettings() {
    sendPacket(address, 60);
  }

  /**
   * <pre>
  68 - Set M1 Default Duty Acceleration
  Set the default acceleration for M1 when using duty cycle commands(Cmds 32,33 and 34) or
  when using Standard Serial, RC and Analog PWM modes.
  Send: [Address, 68, Accel(4 bytes), CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param accel
   *          acc
   */
  public void setDefaultDutyAccelM1(int accel) {
    sendPacket(address, 68, byte3(accel), byte2(accel), byte1(accel), byte0(accel));
  }

  /**
   * <pre>
  69 - Set M2 Default Duty Acceleration
  Set the default acceleration for M2 when using duty cycle commands(Cmds 32,33 and 34) or
  when using Standard Serial, RC and Analog PWM modes.
  Send: [Address, 69, Accel(4 bytes), CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param accel
   *          acc
   */
  public void setDefaultDutyAccelM2(int accel) {
    sendPacket(address, 69, byte3(accel), byte2(accel), byte1(accel), byte0(accel));
  }

  /**
   * <pre>
  74 - Set S3, S4 and S5 Modes
  Set modes for S3,S4 and S5.
  Send: [Address, 74, S3mode, S4mode, S5mode, CRC(2 bytes)]
  Receive: [0xFF]
  Mode S3mode S4mode S5mode
  0 Default Disabled Disabled
  1 E-Stop(latching) E-Stop(latching) E-Stop(latching)
  2 E-Stop E-Stop E-Stop
  3 Voltage Clamp Voltage Clamp Voltage Clamp
  4 M1 Home M2 Home
  
  Mode Description
  Disabled: pin is inactive.
  Default: Flip switch if in RC/Analog mode or E-Stop(latching) in Serial modes.
  E-Stop(Latching): causes the Roboclaw to shutdown until the unit is power cycled.
  E-Stop: Holds the Roboclaw in shutdown until the E-Stop signal is cleared.
  Voltage Clamp: Sets the signal pin as an output to drive an external voltage clamp circuit
  Home(M1 and M2): will trigger the specific motor to stop and the encoder count to reset to 0.
   * </pre>
   * 
   * @param s3
   *          a
   * @param s4
   *          a
   * @param s5
   *          a
   */
  public void setModes(int s3, int s4, int s5) {
    sendPacket(address, 74, s3, s4, s5);
  }

  /**
   * <pre>
  75 - Get S3, S4 amd S5 Modes
  Read mode settings for S3,S4 and S5. See command 74 for mode descriptions
  Send: [Address, 75]
  Receive: [S3mode, S4mode, S5mode, CRC(2 bytes)]
  76 - Set DeadBand for RC/Analog controls
  Set RC/Analog mode control deadband percentage in 10ths of a percent. Default value is
  25(2.5%). Minimum value is 0(no DeadBand), Maximum value is 250(25%).
  Send: [Address, 76, Reverse, Forward, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   */
  public void getModes() {
    sendPacket(address, 76);
    // TODO implement lock & timeout...
  }

  /**
   * <pre>
  77 - Read DeadBand for RC/Analog controls
  Read DeadBand settings in 10ths of a percent.
  Send: [Address, 77]
  Receive: [Reverse, SForward, CRC(2 bytes)]
   * </pre>
   */
  public void readDeadBand() {
    sendPacket(address, 77);
  }

  /**
   * <pre>
  80 - Restore Defaults
  Reset Settings to factory defaults.
  Send: [Address, 80]
  Receive: [0xFF]
   * </pre>
   */

  public void restoreDefaults() {
    sendPacket(address, 80);
  }

  /**
   * <pre>
  81 - Read Default Duty Acceleration Settings
  Read M1 and M2 Duty Cycle Acceleration Settings.
  Send: [Address, 81]
  Receive: [M1Accel(4 bytes), M2Accel(4 bytes), CRC(2 bytes)]
   * </pre>
   */
  public void readDefaultDutyAccelSettings() {
    sendPacket(address, 81);
    // TODO implement
  }

  /**
   * <pre>
  82 - Read Temperature
  Read the board temperature. Value returned is in 10ths of degrees.
  Send: [Address, 82]
  Receive: [Temperature(2 bytes), CRC(2 bytes)]
   * </pre>
   * 
   * @return temp
   */
  public int readTemp() {
    sendPacket(address, 82);

    byte[] data = sendReadPacket(4, address, 30);

    boardTemp = null;

    boardTemp = bytes2ToInt(data);

    return boardTemp;
  }

  /**
   * <pre>
  83 - Read Temperature 2
  Read the second board temperature(only on supported units). Value returned is in 10ths of
  degrees.
  Send: [Address, 83]
  Receive: [Temperature(2 bytes), CRC(2 bytes)]
  RoboClaw Series
  Brushed DC Motor Controllers
  RoboClaw Series User Manual 74
   * </pre>
   */
  public void readTemp2() {
    sendPacket(address, 83);
  }

  /**
   * <pre>
  90 - Read Status
  Read the current unit status.
  Send: [Address, 90]
  Receive: [Status, CRC(2 bytes)]
  Function Status Bit Mask
  Normal 0x0000
  M1 OverCurrent Warning 0x0001
  M2 OverCurrent Warning 0x0002
  E-Stop 0x0004
  Temperature Error 0x0008
  Temperature2 Error 0x0010
  Main Battery High Error 0x0020
  Logic Battery High Error 0x0040
  Logic Battery Low Error 0x0080
  M1 Driver Fault 0x0100
  M2 Driver Fault 0x0200
  Main Battery High Warning 0x0400
  Main Battery Low Warning 0x0800
  Termperature Warning 0x1000
  Temperature2 Warning 0x2000
  M1 Home 0x4000
  M2 Home 0x8000
   * </pre>
   */
  public void readStatus() {
    sendPacket(address, 90);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  91 - Read Encoder Mode
  Read the encoder mode for both motors.
  Send: [Address, 91]
  Receive: [Enc1Mode, Enc2Mode, CRC(2 bytes)]
  Encoder Mode bits
  Bit 7 Enable RC/Analog Encoder support
  Bit 6-1 N/A
  Bit 0 Quadrature(0)/Absolute(1)
   * </pre>
   */
  public void readEncoderModes() {
    sendPacket(address, 91);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  92 - Set Motor 1 Encoder Mode
  Set the Encoder Mode for motor 1. See command 91.
  Send: [Address, 92, Mode, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param mode
   *          mode
   */
  public void setEncoderModeM1(int mode) {
    sendPacket(address, 92, mode);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  93 - Set Motor 2 Encoder Mode
  Set the Encoder Mode for motor 2. See command 91.
  Send: [Address, 93, Mode, CRC(2 bytes)]
  Receive: [0xFF]
  RoboClaw Series
  Brushed DC Motor Controllers
  RoboClaw Series User Manual 75
   * </pre>
   * 
   * @param mode
   *          m
   */
  public void setEncoderModeM2(int mode) {
    sendPacket(address, 93, mode);
  }

  /**
   * <pre>
  94 - Write Settings to EEPROM
  Writes all settings to non-volatile memory. Values will be loaded after each power up.
  Send: [Address, 94]
  Receive: [0xFF]
   * </pre>
   */
  public void writeSettingsToEeprom() {
    sendPacket(address, 94);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  95 - Read Settings from EEPROM
  Read all settings from non-volatile memory.
  Send: [Address, 95]
  Receive: [Enc1Mode, Enc2Mode, CRC(2 bytes)]
  RoboClaw Series
  Brushed DC Motor Controllers
  RoboClaw Series User Manual 76
   * </pre>
   */
  public void readSettingsFromEeprom() {
    sendPacket(address, 95);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  98 - Set Standard Config Settings
  Set config bits for standard settings.
  Send: [Address, 98, Config(2 bytes), CRC(2 bytes)]
  Receive: [0xFF]
  Function Config Bit Mask
  RC Mode 0x0000
  Analog Mode 0x0001
  Simple Serial Mode 0x0002
  Packet Serial Mode 0x0003
  Battery Mode Off 0x0000
  Battery Mode Auto 0x0004
  Battery Mode 2 Cell 0x0008
  Battery Mode 3 Cell 0x000C
  Battery Mode 4 Cell 0x0010
  Battery Mode 5 Cell 0x0014
  Battery Mode 6 Cell 0x0018
  Battery Mode 7 Cell 0x001C
  Mixing 0x0020
  Exponential 0x0040
  MCU 0x0080
  BaudRate 2400 0x0000
  BaudRate 9600 0x0020
  BaudRate 19200 0x0040
  BaudRate 38400 0x0060
  BaudRate 57600 0x0080
  BaudRate 115200 0x00A0
  BaudRate 230400 0x00C0
  BaudRate 460800 0x00E0
  FlipSwitch 0x0100
  Packet Address 0x80 0x0000
  Packet Address 0x81 0x0100
  Packet Address 0x82 0x0200
  Packet Address 0x83 0x0300
  Packet Address 0x84 0x0400
  Packet Address 0x85 0x0500
  Packet Address 0x86 0x0600
  Packet Address 0x87 0x0700
  Slave Mode 0x0800
  Relay Mode 0X1000
  Swap Encoders 0x2000
  Swap Buttons 0x4000
  Multi-Unit Mode 0x8000
  RoboClaw Series
  Brushed DC Motor Controllers
  RoboClaw Series User Manual 77
   * </pre>
   * 
   * @param config
   *          config
   */
  public void setStandardConfig(int config) {
    sendPacket(address, 98, byte1(config), byte0(config));
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  99 - Read Standard Config Settings
  Read config bits for standard settings See Command 98.
  Send: [Address, 99]
  Receive: [Config(2 bytes), CRC(2 bytes)]
   * </pre>
   */
  public void readStandardConfig() {
    sendPacket(address, 99);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  100 - Set CTRL Modes
  Set CTRL modes of CTRL1 and CTRL2 output pins(available on select models).
  Send: [Address, 20, CRC(2 bytes)]
  Receive: [0xFF]
  On select models of Roboclaw, two Open drain, high current output drivers are available, CTRL1
  and CTRL2.
  Mode Function
  0 Disable
  1 User
  2 Voltage Clamp
  3 Brake
  User Mode - The output level can be controlled by setting a value from 0(0%) to 65535(100%).
  A variable frequency PWM is generated at the specified percentage.
  Voltage Clamp Mode - The CTRL output will activate when an over voltage is detected and
  released when the overvoltage disipates. Adding an external load dump resistor from the CTRL
  pin to B+ will allow the Roboclaw to disipate over voltage energy automatically(up to the 3amp
  limit of the CTRL pin).
  Brake Mode - The CTRL pin can be used to activate an external brake(CTRL1 for Motor 1 brake
  and CTRL2 for Motor 2 brake). The signal will activate when the motor is stopped(eg 0 PWM).
  Note acceleration/default_acceleration settings should be set appropriately to allow the motor to
  slow down before the brake is activated.
   * </pre>
   */
  public void setCtrlModes() {
    sendPacket(address, 20);
  }

  /**
   * <pre>
  101 - Read CTRL Modes
  Read CTRL modes of CTRL1 and CTRL2 output pins(available on select models).
  Send: [Address, 101]
  Receive: [CTRL1Mode(1 bytes), CTRL2Mode(1 bytes), CRC(2 bytes)]
  Reads CTRL1 and CTRL2 mode setting. See 100 - Set CTRL Modes for valid values.
   * </pre>
   */
  public void readCtrlModes() {
    sendPacket(address, 101);
  }

  /**
   * <pre>
  102 - Set CTRL1
  Set CTRL1 output value(available on select models)
  Send: [Address, 102, Value(2 bytes), CRC(2 bytes)]
  Receive: [0xFF]
  Set the output state value of CTRL1. See 100 - Set CTRL Modes for valid values.
   * </pre>
   * 
   * @param value
   *          v
   */
  public void setCtrl1(int value) {
    sendPacket(address, 102, byte1(value), byte0(value));
  }

  /**
   * <pre>
  103 - Set CTRL2
  Set CTRL2 output value(available on select models)
  Send: [Address, 103, Value(2 bytes), CRC(2 bytes)]
  Receive: [0xFF]
  Set the output state value of CTRL2. See 100 - Set CTRL Modes for valid values.
   * </pre>
   * 
   * @param value
   *          v
   */
  public void setCtrl2(int value) {
    sendPacket(address, 103, byte1(value), byte0(value));
  }

  /**
   * <pre>
  104 - Read CTRL Settings
  Read CTRL1 and CTRL2 output values(available on select models)
  Send: [Address, 104]
  Receive: [CTRL1(2 bytes), CTRL2(2 bytes), CRC(2 bytes)]
  Reads currently set values for CTRL Settings. See 100 - Set CTRL Modes for valid values.
   * </pre>
   */
  public void readCtrls() {
    byte[] data = sendReadPacket(5, address, 104);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  133 - Set M1 Max Current Limit
  Set Motor 1 Maximum Current Limit. Current value is in 10ma units. To calculate multiply current
  limit by 100.
  Send: [Address, 134, MaxCurrent(4 bytes), 0, 0, 0, 0, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param maxCurrent
   *          current
   */
  public void setMaxCurrentM1(int maxCurrent) {
    sendPacket(address, 133, byte3(maxCurrent), byte2(maxCurrent), byte1(maxCurrent), byte0(maxCurrent), 0, 0, 0, 0);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  134 - Set M2 Max Current Limit
  Set Motor 2 Maximum Current Limit. Current value is in 10ma units. To calculate multiply current
  limit by 100.
  Send: [Address, 134, MaxCurrent(4 bytes), 0, 0, 0, 0, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param maxCurrent
   *          current
   */
  public void setMaxCurrentM2(int maxCurrent) {
    sendPacket(address, 134, byte3(maxCurrent), byte2(maxCurrent), byte1(maxCurrent), byte0(maxCurrent), 0, 0, 0, 0);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  135 - Read M1 Max Current Limit
  Read Motor 1 Maximum Current Limit. Current value is in 10ma units. To calculate divide value
  by 100. MinCurrent is always 0.
  Send: [Address, 135]
  Receive: [MaxCurrent(4 bytes), MinCurrent(4 bytes), CRC(2 bytes)]
   * </pre>
   */
  public void readMaxCurrentM1() {
    sendPacket(address, 135);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  136 - Read M2 Max Current Limit
  Read Motor 2 Maximum Current Limit. Current value is in 10ma units. To calculate divide value
  by 100. MinCurrent is always 0.
  Send: [Address, 136]
  Receive: [MaxCurrent(4 bytes), MinCurrent(4 bytes), CRC(2 bytes)]
   * </pre>
   */
  public void readMaxCurrentM2() {
    sendPacket(address, 136);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  148 - Set PWM Mode
  Set PWM Drive mode. Locked Antiphase(0) or Sign Magnitude(1).
  Send: [Address, 148, Mode, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param mode
   *          pwm mode
   */
  public void setPwmMode(int mode) {
    sendPacket(address, 148, mode);
  }

  /**
   * <pre>
  149 - Read PWM Mode
  Read PWM Drive mode. See Command 148.
  Send: [Address, 149]
  Receive: [PWMMode, CRC(2 bytes)]
   * </pre>
   * 
   * @return pwm mode
   */
  public Integer readPwmMode() {

    byte[] data = sendReadPacket(3, address, 149);
    if (data != null) {
      pwmMode = new Integer(data[0]);
      invoke("publishEncoderData", m1.encoder);
    }

    return pwmMode;
  }

  /**
   * <pre>
  
  16 - Read Encoder Count/Value M1
  Read M1 encoder count/position.
  Send: [Address, 16]
  Receive: [Enc1(4 bytes), Status, CRC(2 bytes)]
  Quadrature encoders have a range of 0 to 4,294,967,295. Absolute encoder values are converted
  from an analog voltage into a value from 0 to 2047 for the full 2v range.
  The status byte tracks counter underflow, direction and overflow. The byte value represents:
  Bit0 - Counter Underflow (1= Underflow Occurred, Clear After Reading)
  Bit1 - Direction (0 = Forward, 1 = Backwards)
  Bit2 - Counter Overflow (1= Underflow Occurred, Clear After Reading)
  Bit3 - Reserved
  Bit4 - Reserved
  Bit5 - Reserved
  Bit6 - Reserved
  Bit7 - Reserved
   * </pre>
   * 
   * @return encoder data from m1
   */
  public EncoderData readEncoderM1() {
    m1.encoder = null;

    byte[] data = sendReadPacket(7, address, 16);
    if (data != null) {
      m1.encoder = new EncoderData(String.format("%s/M1", getName()), address + "", new Double(bytes4ToLong(data)), new Double(bytes4ToLong(data)));
      invoke("publishEncoderData", m1.encoder);
    }

    return m1.encoder;
  }

  public static long bytes4ToLong(byte[] data) {
    return bytes4ToLong(data, 0);
  }

  public static long bytes4ToLong(byte[] data, int start) {
    return (data[start] & 0xFF) << 24 | (data[start + 1] & 0xFF) << 16 | (data[start + 2] & 0xFF) << 8 | (data[start + 3] & 0xFF);
  }

  public static int bytes2ToInt(byte[] data) {
    return bytes2ToInt(data, 0);
  }

  public static int bytes2ToInt(byte[] data, int start) {
    return (data[start] & 0xFF) << 8 | (data[start + 1] & 0xFF);
  }

  /**
   * <pre>
  17 - Read Quadrature Encoder Count/Value M2
  Read M2 encoder count/position.
  Send: [Address, 17]
  Receive: [EncCnt(4 bytes), Status, CRC(2 bytes)]
  Quadrature encoders have a range of 0 to 4,294,967,295. Absolute encoder values are
  converted from an analog voltage into a value from 0 to 2047 for the full 2v range.
  The Status byte tracks counter underflow, direction and overflow. The byte value represents:
  Bit0 - Counter Underflow (1= Underflow Occurred, Cleared After Reading)
  Bit1 - Direction (0 = Forward, 1 = Backwards)
  Bit2 - Counter Overflow (1= Underflow Occurred, Cleared After Reading)
  Bit3 - Reserved
  Bit4 - Reserved
  Bit5 - Reserved
  Bit6 - Reserved
  Bit7 - Reserved
   * </pre>
   * 
   * @return encoder data from m2
   */
  public EncoderData readEncoderM2() {

    m2.encoder = null;

    byte[] data = sendReadPacket(7, address, 17);
    if (data != null) {
      m2.encoder = new EncoderData(String.format("%s/M2", getName()), address + "", new Double(bytes4ToLong(data)), new Double(bytes4ToLong(data)));
      invoke("publishEncoderData", m2.encoder);
    }

    return m2.encoder;
  }

  /**
   * synchronous send and receive - if not enough bytes, or a timeout is reached
   * a null byte buffer will be returned. If the same number of bytes are
   * returned as requested then the byte array is returned. Synchronized on the
   * single serial resource.
   * 
   * @param bytesRequested
   *          - bytes requested
   * @param sendData
   *          - parameter values
   * @return the byte array
   */
  synchronized public byte[] sendReadPacket(int bytesRequested, int... sendData) {
    try {

      // clear buffer
      serial.clear();

      byte[] data = new byte[bytesRequested];
      sendPacket(sendData);
      // read uses timeout set globally
      int bytesRead = serial.read(data);

      if (bytesRead == bytesRequested) {
        return data;
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * <pre>
  18 - Read Encoder Speed M1
  Read M1 counter speed. Returned value is in pulses per second. RoboClaw keeps track of how
  many pulses received per second for both encoder channels.
  Send: [Address, 18]
  Receive: [Speed(4 bytes), Status, CRC(2 bytes)]
  Status indicates the direction (0 – forward, 1 - backward).
   * </pre>
   * 
   * @return encoder speed m1
   */
  public long readEncoderSpeedM1() {
    byte data[] = sendReadPacket(7, address, 18);

    m1.encoderSpeed = null;

    if (data != null) {
      if (data[4] == 1) {
        m1.encoderSpeed = -1 * bytes4ToLong(data);
      } else {
        m1.encoderSpeed = bytes4ToLong(data);
      }
    }

    return m1.encoderSpeed;
  }

  /**
   * <pre>
  19 - Read Encoder Speed M2
  Read M2 counter speed. Returned value is in pulses per second. RoboClaw keeps track of how
  many pulses received per second for both encoder channels.
  Send: [Address, 19]
  Receive: [Speed(4 bytes), Status, CRC(2 bytes)]
  Status indicates the direction (0 – forward, 1 - backward).
   * </pre>
   * 
   * @return encoder speed m2
   */
  public long readEncoderSpeedM2() {
    byte data[] = sendReadPacket(7, address, 17);

    m2.encoderSpeed = null;

    if (data != null) {
      if (data[4] == 1) {
        m2.encoderSpeed = -1 * bytes4ToLong(data);
      } else {
        m2.encoderSpeed = bytes4ToLong(data);
      }
    }

    return m2.encoderSpeed;

  }

  /**
   * <pre>
  20 - Reset Quadrature Encoder Counters
  Will reset both quadrature decoder counters to zero. This command applies to quadrature
  encoders only.
  Send: [Address, 20, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   */
  public void resetEncoders() {
    sendPacket(address, 20);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  22 - Set Quadrature Encoder 1 Value
  Set the value of the Encoder 1 register. Useful when homing motor 1. This command applies to
  quadrature encoders only.
  Send: [Address, 22, Value(4 bytes), CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param value
   *          v
   */
  public void setEncoderM1(int value) {
    sendPacket(address, 22, byte3(value), byte2(value), byte1(value), byte0(value));
  }

  /**
   * <pre>
  23 - Set Quadrature Encoder 2 Value
  Set the value of the Encoder 2 register. Useful when homing motor 2. This command applies to
  quadrature encoders only.
  Send: [Address, 23, Value(4 bytes), CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param value
   *          v
   */
  public void setEncoderM2(int value) {
    sendPacket(address, 23, byte3(value), byte2(value), byte1(value), byte0(value));
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  30 - Read Raw Speed M1
  Read the pulses counted in that last 300th of a second. This is an unfiltered version of command
  18. Command 30 can be used to make a independent PID routine. Value returned is in encoder
  counts per second.
  Send: [Address, 30]
  Receive: [Speed(4 bytes), Status, CRC(2 bytes)]
  The Status byte is direction (0 – forward, 1 - backward).
   * </pre>
   * 
   * @return speed of m1
   */
  public Long readSpeedM1() {
    byte[] data = sendReadPacket(7, address, 30);

    m1.rawSpeed = null;

    if (data != null) {
      if (data[4] == 1) {
        m1.rawSpeed = -1 * bytes4ToLong(data);
      } else {
        m1.rawSpeed = bytes4ToLong(data);
      }
    }

    return m1.rawSpeed;
  }

  /**
   * <pre>
  31 - Read Raw Speed M2
  Read the pulses counted in that last 300th of a second. This is an unfiltered version of command
  19. Command 31 can be used to make a independent PID routine. Value returned is in encoder
  counts per second.
  Send: [Address, 31]
  Receive: [Speed(4 bytes), Status, CRC(2 bytes)]
  The Status byte is direction (0 – forward, 1 - backward).
   * </pre>
   * 
   * @return speed m2
   */
  public Long readSpeedM2() {

    byte[] data = sendReadPacket(7, address, 31);

    m2.rawSpeed = null;

    if (data != null) {
      if (data[4] == 1) {
        m2.rawSpeed = -1 * bytes4ToLong(data);
      } else {
        m2.rawSpeed = bytes4ToLong(data);
      }
    }

    return m2.rawSpeed;
  }

  /**
   * <pre>
  78 - Read Encoder Counters
  Read M1 and M2 encoder counters. Quadrature encoders have a range of 0 to 4,294,967,295.
  Absolute encoder values are converted from an analog voltage into a value from 0 to 2047 for
  the full 2V analog range.
  Send: [Address, 78]
  Receive: [Enc1(4 bytes), Enc2(4 bytes), CRC(2 bytes)]
   * </pre>
   */
  public void readEncoders() {
    sendPacket(address, 78);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  79 - Read ISpeeds Counters
  Read M1 and M2 instantaneous speeds. Returns the speed in encoder counts per second for the
  last 300th of a second for both encoder channels.
  Send: [Address, 79]
  Receive: [ISpeed1(4 bytes), ISpeed2(4 bytes), CRC(2 bytes)]
   * </pre>
   */
  public void readISpeedCounters() {
    sendPacket(address, 79);
    // FIXME - implement (maybe)
  }

  /**
   * <pre>
  28 - Set Velocity PID Constants M1
  Several motor and quadrature combinations can be used with RoboClaw. In some cases the
  default PID values will need to be tuned for the systems being driven. This gives greater
  flexibility in what motor and encoder combinations can be used. The RoboClaw PID system
  consist of four constants starting with QPPS, P = Proportional, I= Integral and D= Derivative.
  The defaults values are:
  QPPS = 44000
  P = 0x00010000
  I = 0x00008000
  D = 0x00004000
  QPPS is the speed of the encoder when the motor is at 100% power. P, I, D are the default
  values used after a reset. Command syntax:
  Send: [Address, 28, D(4 bytes), P(4 bytes), I(4 bytes), QPPS(4 byte), CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param D
   *          pid
   * @param P
   *          pid
   * @param I
   *          pid
   * @param QPPS
   *          see comments
   */
  public void setPidQppsM1(int D, int P, int I, int QPPS) {
    sendPacket(address, 28, byte3(D), byte2(D), byte1(D), byte0(D), byte3(P), byte2(P), byte1(P), byte0(P), byte3(I), byte2(I), byte1(I), byte0(I), byte3(QPPS), byte2(QPPS),
        byte1(QPPS), byte0(QPPS));
  }

  /**
   * <pre>
  29 - Set Velocity PID Constants M2
  Several motor and quadrature combinations can be used with RoboClaw. In some cases the
  default PID values will need to be tuned for the systems being driven. This gives greater
  flexibility in what motor and encoder combinations can be used. The RoboClaw PID system
  consist of four constants starting with QPPS, P = Proportional, I= Integral and D= Derivative.
  The defaults values are:
  QPPS = 44000
  P = 0x00010000
  I = 0x00008000
  D = 0x00004000
  QPPS is the speed of the encoder when the motor is at 100% power. P, I, D are the default
  values used after a reset. Command syntax:
  Send: [Address, 29, D(4 bytes), P(4 bytes), I(4 bytes), QPPS(4 byte), CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param D
   *          pid
   * @param P
   *          pid
   * @param I
   *          pid
   * @param QPPS
   *          pid
   */
  public void setPidQppsM2(int D, int P, int I, int QPPS) {
    sendPacket(address, 29, byte3(D), byte2(D), byte1(D), byte0(D), byte3(P), byte2(P), byte1(P), byte0(P), byte3(I), byte2(I), byte1(I), byte0(I), byte3(QPPS), byte2(QPPS),
        byte1(QPPS), byte0(QPPS));
  }

  public void setPidQppsDeadzoneMinMaxM1(int D, int P, int I, int QPPS, int deadzone, int minPos, int maxPos) {
    setPidQppsM1(D, P, I, QPPS);
    setPidM1(D, P, I, 0, deadzone, minPos, maxPos);
  }

  public void setPidQppsDeadzoneMinMaxM2(int D, int P, int I, int QPPS, int deadzone, int minPos, int maxPos) {
    setPidQppsM2(D, P, I, QPPS);
    setPidM2(D, P, I, 0, deadzone, minPos, maxPos);
  }

  /**
   * <pre>
  32 - Drive M1 With Signed Duty Cycle
  Drive M1 using a duty cycle value. The duty cycle is used to control the speed of the motor
  without a quadrature encoder.
  Send: [Address, 32, Duty(2 Bytes), CRC(2 bytes)]
  Receive: [0xFF]
  The duty value is signed and the range is -32767 to +32767 (eg. +-100% duty).
   * </pre>
   * 
   * @param duty
   *          duty m1
   */
  public void driveDutyM1(int duty) {
    sendPacket(address, 32, byte1(duty), byte0(duty));
  }

  /**
   * <pre>
  33 - Drive M2 With Signed Duty Cycle
  Drive M2 using a duty cycle value. The duty cycle is used to control the speed of the motor
  without a quadrature encoder. The command syntax:
  Send: [Address, 33, Duty(2 Bytes), CRC(2 bytes)]
  Receive: [0xFF]
  The duty value is signed and the range is -32768 to +32767 (eg. +-100% duty).
   * </pre>
   * 
   * @param duty
   *          m2 duty
   */
  public void driveDutyM2(int duty) {
    sendPacket(address, 32, byte1(duty), byte0(duty));
  }

  /**
   * <pre>
  34 - Drive M1 / M2 With Signed Duty Cycle
  Drive both M1 and M2 using a duty cycle value. The duty cycle is used to control the speed of
  the motor without a quadrature encoder. The command syntax:
  Send: [Address, 34, DutyM1(2 Bytes), DutyM2(2 Bytes), CRC(2 bytes)]
  Receive: [0xFF]
  The duty value is signed and the range is -32768 to +32767 (eg. +-100% duty).
   * </pre>
   * 
   * @param duty1
   *          m1 duty
   * @param duty2
   *          m2 duty
   */
  public void driveDutyM1M2(int duty1, int duty2) {
    sendPacket(address, 34, byte1(duty1), byte0(duty1), byte1(duty2), byte0(duty2));
  }

  /**
   * <pre>
  35 - Drive M1 With Signed Speed
  Drive M1 using a speed value. The sign indicates which direction the motor will turn. This
  command is used to drive the motor by quad pulses per second. Different quadrature encoders
  will have different rates at which they generate the incoming pulses. The values used will differ
  from one encoder to another. Once a value is sent the motor will begin to accelerate as fast as
  possible until the defined rate is reached.
  Send: [Address, 35, Speed(4 Bytes), CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param speed
   *          s
   */
  public void driveSpeedM1(int speed) {
    sendPacket(address, 35, byte3(speed), byte2(speed), byte1(speed), byte0(speed));
  }

  /**
   * <pre>
  36 - Drive M2 With Signed Speed
  Drive M2 with a speed value. The sign indicates which direction the motor will turn. This
  command is used to drive the motor by quad pulses per second. Different quadrature encoders
  will have different rates at which they generate the incoming pulses. The values used will differ
  from one encoder to another. Once a value is sent, the motor will begin to accelerate as fast as
  possible until the rate defined is reached.
  Send: [Address, 36, Speed(4 Bytes), CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param speed
   *          s m2
   */
  public void driveSpeedM2(int speed) {
    sendPacket(address, 36, byte3(speed), byte2(speed), byte1(speed), byte0(speed));
  }

  /**
   * <pre>
  37 - Drive M1 / M2 With Signed Speed
  Drive M1 and M2 in the same command using a signed speed value. The sign indicates which
  direction the motor will turn. This command is used to drive both motors by quad pulses per
  second. Different quadrature encoders will have different rates at which they generate the
  incoming pulses. The values used will differ from one encoder to another. Once a value is sent
  the motor will begin to accelerate as fast as possible until the rate defined is reached.
  Send: [Address, 37, SpeedM1(4 Bytes), SpeedM2(4 Bytes), CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param speedM1
   *          m1
   * @param speedM2
   *          m2
   */
  public void driveSpeedM1M2(int speedM1, int speedM2) {
    sendPacket(address, 37, byte3(speedM1), byte2(speedM1), byte1(speedM1), byte0(speedM1), byte3(speedM2), byte2(speedM2), byte1(speedM2), byte0(speedM2));
  }

  /**
   * <pre>
  38 - Drive M1 With Signed Speed And Acceleration
  Drive M1 with a signed speed and acceleration value. The sign indicates which direction the
  motor will run. The acceleration values are not signed. This command is used to drive the motor
  by quad pulses per second and using an acceleration value for ramping. Different quadrature
  encoders will have different rates at which they generate the incoming pulses. The values used
  will differ from one encoder to another. Once a value is sent the motor will begin to accelerate
  incrementally until the rate defined is reached.
  Send: [Address, 38, Accel(4 Bytes), Speed(4 Bytes), CRC(2 bytes)]
  Receive: [0xFF]
  The acceleration is measured in speed increase per second. An acceleration value of 12,000
  QPPS with a speed of 12,000 QPPS would accelerate a motor from 0 to 12,000 QPPS in 1
  second. Another example would be an acceleration value of 24,000 QPPS and a speed value of
  12,000 QPPS would accelerate the motor to 12,000 QPPS in 0.5 seconds.
  RoboClaw Series
  Brushed DC Motor Controllers
  RoboClaw Series User Manual 92
   * </pre>
   * 
   * @param accel
   *          a
   * @param speed
   *          s
   */
  public void driveSpeedAccelM1(int accel, int speed) {
    sendPacket(address, 38, accel, speed);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  39 - Drive M2 With Signed Speed And Acceleration
  Drive M2 with a signed speed and acceleration value. The sign indicates which direction the
  motor will run. The acceleration value is not signed. This command is used to drive the motor
  by quad pulses per second and using an acceleration value for ramping. Different quadrature
  encoders will have different rates at which they generate the incoming pulses. The values used
  will differ from one encoder to another. Once a value is sent the motor will begin to accelerate
  incrementally until the rate defined is reached.
  Send: [Address, 39, Accel(4 Bytes), Speed(4 Bytes), CRC(2 bytes)]
  Receive: [0xFF]
  The acceleration is measured in speed increase per second. An acceleration value of 12,000
  QPPS with a speed of 12,000 QPPS would accelerate a motor from 0 to 12,000 QPPS in 1 second.
  Another example would be an acceleration value of 24,000 QPPS and a speed value of 12,000
  QPPS would accelerate the motor to 12,000 QPPS in 0.5 seconds.
   * </pre>
   * 
   * @param accel
   *          a
   * @param speed
   *          s
   */
  public void driveSpeedAccelM2(int accel, int speed) {
    sendPacket(address, 39, accel, speed);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  40 - Drive M1 / M2 With Signed Speed And Acceleration
  Drive M1 and M2 in the same command using one value for acceleration and two signed speed
  values for each motor. The sign indicates which direction the motor will run. The acceleration
  value is not signed. The motors are sync during acceleration. This command is used to drive
  the motor by quad pulses per second and using an acceleration value for ramping. Different
  quadrature encoders will have different rates at which they generate the incoming pulses. The
  values used will differ from one encoder to another. Once a value is sent the motor will begin to
  accelerate incrementally until the rate defined is reached.
  Send: [Address, 40, Accel(4 Bytes), SpeedM1(4 Bytes), SpeedM2(4 Bytes), CRC(2 bytes)]
  Receive: [0xFF]
  The acceleration is measured in speed increase per second. An acceleration value of 12,000
  QPPS with a speed of 12,000 QPPS would accelerate a motor from 0 to 12,000 QPPS in 1 second.
  Another example would be an acceleration value of 24,000 QPPS and a speed value of 12,000
  QPPS would accelerate the motor to 12,000 QPPS in 0.5 seconds.
   * </pre>
   * 
   * @param accel
   *          a
   * @param speedM1
   *          m1
   * @param speedM2
   *          m2
   */
  public void driveSpeedAccelM1M2(int accel, int speedM1, int speedM2) {

    sendPacket(address, 40, byte3(accel), byte2(accel), byte1(accel), byte0(accel), byte3(speedM1), byte2(speedM1), byte1(speedM1), byte0(speedM1), byte3(speedM2), byte2(speedM2),
        byte1(speedM2), byte0(speedM2));
  }

  /**
   * <pre>
  41 - Buffered M1 Drive With Signed Speed And Distance
  Drive M1 with a signed speed and distance value. The sign indicates which direction the motor
  will run. The distance value is not signed. This command is buffered. This command is used to
  control the top speed and total distance traveled by the motor. Each motor channel M1 and M2
  have separate buffers. This command will execute immediately if no other command for that
  channel is executing, otherwise the command will be buffered in the order it was sent. Any
  buffered or executing command can be stopped when a new command is issued by setting the
  Buffer argument. All values used are in quad pulses per second.
  Send: [Address, 41, Speed(4 Bytes), Distance(4 Bytes), Buffer, CRC(2 bytes)]
  Receive: [0xFF]
  The Buffer argument can be set to a 1 or 0. If a value of 0 is used the command will be buffered
  and executed in the order sent. If a value of 1 is used the current running command is stopped,
  any other commands in the buffer are deleted and the new command is executed.
   * </pre>
   * 
   * @param speed
   *          s
   * @param distance
   *          d
   */
  public void driveSpeedDistM1(int speed, int distance) {
    sendPacket(address, 41, byte3(speed), byte2(speed), byte1(speed), byte0(speed), byte3(distance), byte2(distance), byte1(distance), byte0(distance), buffer);
  }

  /**
   * <pre>
  42 - Buffered M2 Drive With Signed Speed And Distance
  Drive M2 with a speed and distance value. The sign indicates which direction the motor will run.
  The distance value is not signed. This command is buffered. Each motor channel M1 and M2
  have separate buffers. This command will execute immediately if no other command for that
  channel is executing, otherwise the command will be buffered in the order it was sent. Any
  buffered or executing command can be stopped when a new command is issued by setting the
  Buffer argument. All values used are in quad pulses per second.
  Send: [Address, 42, Speed(4 Bytes), Distance(4 Bytes), Buffer, CRC(2 bytes)]
  Receive: [0xFF]
  The Buffer argument can be set to a 1 or 0. If a value of 0 is used the command will be buffered
  and executed in the order sent. If a value of 1 is used the current running command is stopped,
  any other commands in the buffer are deleted and the new command is executed.
   * </pre>
   * 
   * @param speed
   *          s
   * @param distance
   *          d
   */
  public void driveSpeedDistM2(int speed, int distance) {
    sendPacket(address, 42, byte3(speed), byte2(speed), byte1(speed), byte0(speed), byte3(distance), byte2(distance), byte1(distance), byte0(distance), buffer);
  }

  /**
   * <pre>
  43 - Buffered Drive M1 / M2 With Signed Speed And Distance
  Drive M1 and M2 with a speed and distance value. The sign indicates which direction the motor
  will run. The distance value is not signed. This command is buffered. Each motor channel M1
  and M2 have separate buffers. This command will execute immediately if no other command for
  that channel is executing, otherwise the command will be buffered in the order it was sent. Any
  buffered or executing command can be stopped when a new command is issued by setting the
  Buffer argument. All values used are in quad pulses per second.
  Send: [Address, 43, SpeedM1(4 Bytes), DistanceM1(4 Bytes),
  SpeedM2(4 Bytes), DistanceM2(4 Bytes), Buffer, CRC(2 bytes)]
  Receive: [0xFF]
  The Buffer argument can be set to a 1 or 0. If a value of 0 is used the command will be buffered
  and executed in the order sent. If a value of 1 is used the current running command is stopped,
  any other commands in the buffer are deleted and the new command is executed.
   * </pre>
   * 
   * @param speedM1
   *          s
   * @param distanceM1
   *          d
   * @param speedM2
   *          s
   * @param distanceM2
   *          d
   */
  public void driveSpeedDistM1M2(int speedM1, int distanceM1, int speedM2, int distanceM2) {
    sendPacket(address, 43, byte3(speedM1), byte2(speedM1), byte1(speedM1), byte0(speedM1), byte3(distanceM1), byte2(distanceM1), byte1(distanceM1), byte0(distanceM1),
        byte3(speedM2), byte2(speedM2), byte1(speedM2), byte0(speedM2), byte3(distanceM2), byte2(distanceM2), byte1(distanceM2), byte0(distanceM2),

        buffer);

  }

  /**
   * <pre>
  44 - Buffered M1 Drive With Signed Speed, Accel And Distance
  Drive M1 with a speed, acceleration and distance value. The sign indicates which direction the
  motor will run. The acceleration and distance values are not signed. This command is used to
  control the motors top speed, total distanced traveled and at what incremental acceleration value
  to use until the top speed is reached. Each motor channel M1 and M2 have separate buffers. This
  command will execute immediately if no other command for that channel is executing, otherwise
  the command will be buffered in the order it was sent. Any buffered or executing command can
  be stopped when a new command is issued by setting the Buffer argument. All values used are
  in quad pulses per second.
  Send: [Address, 44, Accel(4 bytes), Speed(4 Bytes), Distance(4 Bytes),
  Buffer, CRC(2 bytes)]
  Receive: [0xFF]
  The Buffer argument can be set to a 1 or 0. If a value of 0 is used the command will be buffered
  and executed in the order sent. If a value of 1 is used the current running command is stopped,
  any other commands in the buffer are deleted and the new command is executed.
  RoboClaw Series
  Brushed DC Motor Controllers
  RoboClaw Series User Manual 94
   * </pre>
   * 
   * @param accel
   *          a
   * @param speed
   *          s
   * @param distance
   *          d
   */
  public void driveSpeedAccelDistM1(int accel, int speed, int distance) {
    sendPacket(address, 44, byte3(accel), byte2(accel), byte1(accel), byte0(accel), byte3(speed), byte2(speed), byte1(speed), byte0(speed), byte3(distance), byte2(distance),
        byte1(distance), byte0(distance), buffer);
  }

  /**
   * <pre>
  45 - Buffered M2 Drive With Signed Speed, Accel And Distance
  Drive M2 with a speed, acceleration and distance value. The sign indicates which direction the
  motor will run. The acceleration and distance values are not signed. This command is used to
  control the motors top speed, total distanced traveled and at what incremental acceleration
  value to use until the top speed is reached. Each motor channel M1 and M2 have separate
  buffers. This command will execute immediately if no other command for that channel is
  executing, otherwise the command will be buffered in the order it was sent. Any buffered
  or executing command can be stopped when a new command is issued by setting the Buffer
  argument. All values used are in quad pulses per second.
  Send: [Address, 45, Accel(4 bytes), Speed(4 Bytes), Distance(4 Bytes),
  Buffer, CRC(2 bytes)]
  Receive: [0xFF]
  The Buffer argument can be set to a 1 or 0. If a value of 0 is used the command will be buffered
  and executed in the order sent. If a value of 1 is used the current running command is stopped,
  any other commands in the buffer are deleted and the new command is executed.
   * </pre>
   * 
   * @param speed
   *          s
   * @param accel
   *          a
   * @param distance
   *          d
   */
  public void driveSpeedAccelDistM2(int speed, int accel, int distance) {
    sendPacket(address, 45, byte3(speed), byte2(speed), byte1(speed), byte0(speed), byte3(accel), byte2(accel), byte1(accel), byte0(accel), byte3(distance), byte2(distance),
        byte1(distance), byte0(distance), buffer);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  46 - Buffered Drive M1 / M2 With Signed Speed, Accel And Distance
  Drive M1 and M2 with a speed, acceleration and distance value. The sign indicates which
  direction the motor will run. The acceleration and distance values are not signed. This command
  is used to control both motors top speed, total distanced traveled and at what incremental
  acceleration value to use until the top speed is reached. Each motor channel M1 and M2 have
  separate buffers. This command will execute immediately if no other command for that channel
  is executing, otherwise the command will be buffered in the order it was sent. Any buffered
  or executing command can be stopped when a new command is issued by setting the Buffer
  argument. All values used are in quad pulses per second.
  Send: [Address, 46, Accel(4 Bytes), SpeedM1(4 Bytes), DistanceM1(4 Bytes),
  SpeedM2(4 bytes), DistanceM2(4 Bytes), Buffer, CRC(2 bytes)]
  Receive: [0xFF]
  The Buffer argument can be set to a 1 or 0. If a value of 0 is used the command will be buffered
  and executed in the order sent. If a value of 1 is used the current running command is stopped,
  any other commands in the buffer are deleted and the new command is executed.
   * </pre>
   * 
   * @param accel
   *          a
   * @param speedM1
   *          s
   * @param distanceM1
   *          d
   * @param speedM2
   *          s
   * @param distanceM2
   *          d
   */
  public void driveSpeedAccelDistM1M2(int accel, int speedM1, int distanceM1, int speedM2, int distanceM2) {
    sendPacket(address, 46, byte3(accel), byte2(accel), byte1(accel), byte0(accel), byte3(speedM1), byte2(speedM1), byte1(speedM1), byte0(speedM1), byte3(distanceM1),
        byte2(distanceM1), byte1(distanceM1), byte0(distanceM1), byte3(speedM2), byte2(speedM2), byte1(speedM2), byte0(speedM2), byte3(distanceM2), byte2(distanceM2),
        byte1(distanceM2), byte0(distanceM2), buffer);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  47 - Read Buffer Length
  Read both motor M1 and M2 buffer lengths. This command can be used to determine how many
  commands are waiting to execute.
  Send: [Address, 47]
  Receive: [BufferM1, BufferM2, CRC(2 bytes)]
  The return values represent how many commands per buffer are waiting to be executed. The
  maximum buffer size per motor is 64 commands(0x3F). A return value of 0x80(128) indicates
  the buffer is empty. A return value of 0 indiciates the last command sent is executing. A value of
  0x80 indicates the last command buffered has finished.
   * </pre>
   */
  public void readBufferLength() {
    sendPacket(address, 47);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  50 - Drive M1 / M2 With Signed Speed And Individual Acceleration
  Drive M1 and M2 in the same command using one value for acceleration and two signed speed
  values for each motor. The sign indicates which direction the motor will run. The acceleration
  value is not signed. The motors are sync during acceleration. This command is used to drive
  the motor by quad pulses per second and using an acceleration value for ramping. Different
  quadrature encoders will have different rates at which they generate the incoming pulses. The
  values used will differ from one encoder to another. Once a value is sent the motor will begin to
  accelerate incrementally until the rate defined is reached.
  Send: [Address, 50, AccelM1(4 Bytes), SpeedM1(4 Bytes), AccelM2(4 Bytes),
  SpeedM2(4 Bytes), CRC(2 bytes)]
  Receive: [0xFF]
  The acceleration is measured in speed increase per second. An acceleration value of 12,000
  QPPS with a speed of 12,000 QPPS would accelerate a motor from 0 to 12,000 QPPS in 1 second.
  Another example would be an acceleration value of 24,000 QPPS and a speed value of 12,000
  QPPS would accelerate the motor to 12,000 QPPS in 0.5 seconds.
   * </pre>
   * 
   * @param accelM1
   *          a
   * @param speedM1
   *          s
   * @param accelM2
   *          a
   * @param speedM2
   *          s
   */
  public void driveSpeedAndIndividualAccelM1M2(int accelM1, int speedM1, int accelM2, int speedM2) {
    sendPacket(address, 50, byte3(accelM1), byte2(accelM1), byte1(accelM1), byte0(accelM1), byte3(speedM1), byte2(speedM1), byte1(speedM1), byte0(speedM1), byte3(accelM2),
        byte2(accelM2), byte1(accelM2), byte0(accelM2), byte3(speedM2), byte2(speedM2), byte1(speedM2), byte0(speedM2));
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  51 - Buffered Drive M1 / M2 With Signed Speed, Individual Accel And Distance
  Drive M1 and M2 with a speed, acceleration and distance value. The sign indicates which
  direction the motor will run. The acceleration and distance values are not signed. This command
  is used to control both motors top speed, total distanced traveled and at what incremental
  acceleration value to use until the top speed is reached. Each motor channel M1 and M2 have
  separate buffers. This command will execute immediately if no other command for that channel
  is executing, otherwise the command will be buffered in the order it was sent. Any buffered
  or executing command can be stopped when a new command is issued by setting the Buffer
  argument. All values used are in quad pulses per second.
  Send: [Address, 51, AccelM1(4 Bytes), SpeedM1(4 Bytes), DistanceM1(4 Bytes),
  AccelM2(4 Bytes), SpeedM2(4 bytes), DistanceM2(4 Bytes), Buffer, CRC(2 bytes)]
  Receive: [0xFF]
  The Buffer argument can be set to a 1 or 0. If a value of 0 is used the command will be buffered
  and executed in the order sent. If a value of 1 is used the current running command is stopped,
  any other commands in the buffer are deleted and the new command is executed.
   * </pre>
   * 
   * @param accelM1
   *          a
   * @param speedM1
   *          s
   * @param distanceM1
   *          d
   * @param accelM2
   *          a
   * @param speedM2
   *          s
   * @param distanceM2
   *          d
   */
  public void driveSpeedAccelDistM1M2(int accelM1, int speedM1, int distanceM1, int accelM2, int speedM2, int distanceM2) {
    sendPacket(address, 51, byte3(accelM1), byte2(accelM1), byte1(accelM1), byte0(accelM1), byte3(speedM1), byte2(speedM1), byte1(speedM1), byte0(speedM1), byte3(distanceM1),
        byte2(distanceM1), byte1(distanceM1), byte0(distanceM1), byte3(accelM2), byte2(accelM2), byte1(accelM2), byte0(accelM2), byte3(speedM2), byte2(speedM2), byte1(speedM2),
        byte0(speedM2), byte3(distanceM2), byte2(distanceM2), byte1(distanceM2), byte0(distanceM2), buffer);
  }

  /**
   * <pre>
  52 - Drive M1 With Signed Duty And Acceleration
  Drive M1 with a signed duty and acceleration value. The sign indicates which direction the motor
  will run. The acceleration values are not signed. This command is used to drive the motor by
  PWM and using an acceleration value for ramping. Accel is the rate per second at which the duty
  changes from the current duty to the specified duty.
  Send: [Address, 52, Duty(2 bytes), Accel(2 Bytes), CRC(2 bytes)]
  Receive: [0xFF]
  The duty value is signed and the range is -32768 to +32767(eg. +-100% duty). The accel value
  range is 0 to 655359(eg maximum acceleration rate is -100% to 100% in 100ms).
   * </pre>
   * 
   * @param duty
   *          d
   * @param accel
   *          a
   */
  public void driveDutyAccelM1(int duty, int accel) {
    sendPacket(address, 52, byte1(duty), byte0(duty), byte1(accel), byte0(accel));
  }

  /**
   * <pre>
  53 - Drive M2 With Signed Duty And Acceleration
  Drive M1 with a signed duty and acceleration value. The sign indicates which direction the motor
  will run. The acceleration values are not signed. This command is used to drive the motor by
  PWM and using an acceleration value for ramping. Accel is the rate at which the duty changes
  from the current duty to the specified dury.
  Send: [Address, 53, Duty(2 bytes), Accel(2 Bytes), CRC(2 bytes)]
  Receive: [0xFF]
  The duty value is signed and the range is -32768 to +32767 (eg. +-100% duty). The accel value
  range is 0 to 655359 (eg maximum acceleration rate is -100% to 100% in 100ms).
   * </pre>
   * 
   * @param duty
   *          d
   * @param accel
   *          a
   */
  public void driveDutyAccelM2(int duty, int accel) {
    sendPacket(address, 53, byte1(duty), byte0(duty), byte1(accel), byte0(accel));
  }

  /**
   * <pre>
  54 - Drive M1 / M2 With Signed Duty And Acceleration
  Drive M1 and M2 in the same command using acceleration and duty values for each motor.
  The sign indicates which direction the motor will run. The acceleration value is not signed. This
  command is used to drive the motor by PWM using an acceleration value for ramping. The
  command syntax:
  Send: [Address, CMD, DutyM1(2 bytes), AccelM1(4 Bytes), DutyM2(2 bytes),
  AccelM1(4 bytes), CRC(2 bytes)]
  Receive: [0xFF]
  The duty value is signed and the range is -32768 to +32767 (eg. +-100% duty). The accel value
  range is 0 to 655359 (eg maximum acceleration rate is -100% to 100% in 100ms).
   * </pre>
   * 
   * @param cmd
   *          c
   * @param dutyM1
   *          d
   * @param accelM1
   *          a
   * @param dutyM2
   *          d
   * @param accelM2
   *          a
   */
  public void driveDutyAccelM1M2(int cmd, int dutyM1, int accelM1, int dutyM2, int accelM2) {
    sendPacket(address, 54, cmd, byte1(dutyM1), byte0(dutyM1), byte1(accelM1), byte0(accelM1), byte1(dutyM2), byte0(dutyM2), byte1(accelM2), byte0(accelM2));
  }

  /**
   * <pre>
  55 - Read Motor 1 Velocity PID and QPPS Settings
  Read the PID and QPPS Settings.
  Send: [Address, 55]
  Receive: [P(4 bytes), I(4 bytes), D(4 bytes), QPPS(4 byte), CRC(2 bytes)]
   * </pre>
   * 
   * @return the pid data
   */
  // FIXME - contain both inside a pair<>
  public PidData readPidQppsM1() {

    byte[] data = sendReadPacket(18, address, 55);
    if (data != null) {
      m1.pid.kp = bytes4ToLong(data);
      m1.pid.ki = bytes4ToLong(data, 4);
      m1.pid.kd = bytes4ToLong(data, 8);
      m1.qpps = bytes4ToLong(data, 12);
    }

    return m1.pid;
  }

  /**
   * <pre>
  56 - Read Motor 2 Velocity PID and QPPS Settings
  Read the PID and QPPS Settings.
  Send: [Address, 56]
  Receive: [P(4 bytes), I(4 bytes), D(4 bytes), QPPS(4 byte), CRC(2 bytes)]
   * </pre>
   * 
   * @return pid data
   */
  public PidData readPidQppsM2() {
    sendPacket(address, 56);

    byte[] data = sendReadPacket(18, address, 56);
    if (data != null) {
      m2.pid.kp = bytes4ToLong(data);
      m2.pid.ki = bytes4ToLong(data, 4);
      m2.pid.kd = bytes4ToLong(data, 8);
      m2.qpps = bytes4ToLong(data, 12);
    }

    return m2.pid;

  }

  /**
   * <pre>
  61 - Set Motor 1 Position PID Constants
  The RoboClaw Position PID system consist of seven constants starting with P = Proportional, I=
  Integral and D= Derivative, MaxI = Maximum Integral windup, Deadzone in encoder counts,
  MinPos = Minimum Position and MaxPos = Maximum Position. The defaults values are all zero.
  Send: [Address, 61, D(4 bytes), P(4 bytes), I(4 bytes), MaxI(4 bytes),
  Deadzone(4 bytes), MinPos(4 bytes), MaxPos(4 bytes), CRC(2 bytes)]
  Receive: [0xFF]
  Position constants are used only with the Position commands, 65,66 and 67 or when encoders
  are enabled in RC/Analog modes.
   * </pre>
   * 
   * @param D
   *          pid
   * @param P
   *          pid
   * @param I
   *          pid
   * @param maxI
   *          i
   * @param deadzone
   *          dz
   * @param minPos
   *          min
   * @param maxPos
   *          max
   */
  public void setPidM1(int D, int P, int I, int maxI, int deadzone, int minPos, int maxPos) {
    sendPacket(address, 61, byte3(D), byte2(D), byte1(D), byte0(D), byte3(P), byte2(P), byte1(P), byte0(P), byte3(I), byte2(I), byte1(I), byte0(I), byte3(maxI), byte2(maxI),
        byte1(maxI), byte0(maxI), byte3(deadzone), byte2(deadzone), byte1(deadzone), byte0(deadzone), byte3(minPos), byte2(minPos), byte1(minPos), byte0(minPos), byte3(maxPos),
        byte2(maxPos), byte1(maxPos), byte0(maxPos));

    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  62 - Set Motor 2 Position PID Constants
  The RoboClaw Position PID system consist of seven constants starting with P = Proportional, I=
  Integral and D= Derivative, MaxI = Maximum Integral windup, Deadzone in encoder counts,
  MinPos = Minimum Position and MaxPos = Maximum Position. The defaults values are all zero.
  Send: [Address, 62, D(4 bytes), P(4 bytes), I(4 bytes), MaxI(4 bytes),
  Deadzone(4 bytes), MinPos(4 bytes), MaxPos(4 bytes), CRC(2 bytes)]
  Receive: [0xFF]
  Position constants are used only with the Position commands, 65,66 and 67 or when encoders
  are enabled in RC/Analog modes.
   * </pre>
   * 
   * @param D
   *          pid
   * @param P
   *          pid
   * @param I
   *          pid
   * @param maxI
   *          max i
   * @param deadzone
   *          dz
   * @param minPos
   *          min
   * @param maxPos
   *          max
   */
  public void setPidM2(int D, int P, int I, int maxI, int deadzone, int minPos, int maxPos) {
    sendPacket(address, 62, byte3(D), byte2(D), byte1(D), byte0(D), byte3(P), byte2(P), byte1(P), byte0(P), byte3(I), byte2(I), byte1(I), byte0(I), byte3(maxI), byte2(maxI),
        byte1(maxI), byte0(maxI), byte3(deadzone), byte2(deadzone), byte1(deadzone), byte0(deadzone), byte3(minPos), byte2(minPos), byte1(minPos), byte0(minPos), byte3(minPos),
        byte2(minPos), byte1(minPos), byte0(minPos));
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  63 - Read Motor 1 Position PID Constants
  Read the Position PID Settings.
  Send: [Address, 63]
  Receive: [P(4 bytes), I(4 bytes), D(4 bytes), MaxI(4 byte), Deadzone(4 byte),
  MinPos(4 byte), MaxPos(4 byte), CRC(2 bytes)]
   * </pre>
   * 
   * @return pid data
   */
  public PidData readPidM1() {

    byte[] data = sendReadPacket(30, address, 63);
    if (data != null) {
      m1.pid = new PidData();
      m1.pid.kp = bytes4ToLong(data);
      m1.pid.ki = bytes4ToLong(data, 4);
      m1.pid.kd = bytes4ToLong(data, 8);
      // MaxI ??? m1.pid.kd = bytes4ToLong(data, 12);
      m1.pid.deadband = bytes4ToLong(data, 16);
      m1.pid.outMin = (double) bytes4ToLong(data, 20);
      m1.pid.outMax = (double) bytes4ToLong(data, 24);
      log.info("m1.pid {}", m1.pid.toString());
    }

    return m1.pid;
  }

  /**
   * <pre>
  64 - Read Motor 2 Position PID Constants
  Read the Position PID Settings.
  Send: [Address, 64]
  Receive: [P(4 bytes), I(4 bytes), D(4 bytes), MaxI(4 byte), Deadzone(4 byte),
  MinPos(4 byte), MaxPos(4 byte), CRC(2 bytes)]
   * </pre>
   * 
   * @return pid data
   */
  public PidData readPidM2() {
    byte[] data = sendReadPacket(30, address, 64);
    if (data != null) {
      m2.pid = new PidData();
      m2.pid.kp = bytes4ToLong(data);
      m2.pid.ki = bytes4ToLong(data, 4);
      m2.pid.kd = bytes4ToLong(data, 8);
      // MaxI ??? m1.pid.kd = bytes4ToLong(data, 12);
      m2.pid.deadband = bytes4ToLong(data, 16);
      m2.pid.outMin = (double) bytes4ToLong(data, 20);
      m2.pid.outMax = (double) bytes4ToLong(data, 24);
      log.info("m2.pid {}", m2.pid.toString());
    }

    return m2.pid;
  }

  /**
   * <pre>
  65 - Buffered Drive M1 with signed Speed, Accel, Deccel and Position
  Move M1 position from the current position to the specified new position and hold the new
  position. Accel sets the acceleration value and deccel the decceleration value. QSpeed sets the
  speed in quadrature pulses the motor will run at after acceleration and before decceleration.
  Send: [Address, 65, Accel(4 bytes), Speed(4 Bytes), Deccel(4 bytes),
  Position(4 Bytes), Buffer, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param accel
   *          a
   * @param speed
   *          s
   * @param deccel
   *          d
   * @param pos
   *          p
   */
  public void driveSpeedAccelDeccelPosM1(int accel, int speed, int deccel, int pos) {
    sendPacket(address, 65, byte3(accel), byte2(accel), byte1(accel), byte0(accel), byte3(speed), byte2(speed), byte1(speed), byte0(speed), byte3(deccel), byte2(deccel),
        byte1(deccel), byte0(deccel), byte3(pos), byte2(pos), byte1(pos), byte0(pos), buffer);
    // TODO lock - timeout - return value & publish
  }

  /**
   * <pre>
  66 - Buffered Drive M2 with signed Speed, Accel, Deccel and Position
  Move M2 position from the current position to the specified new position and hold the new
  position. Accel sets the acceleration value and deccel the decceleration value. QSpeed sets the
  speed in quadrature pulses the motor will run at after acceleration and before decceleration.
  Send: [Address, 66, Accel(4 bytes), Speed(4 Bytes), Deccel(4 bytes),
  Position(4 Bytes), Buffer, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param accel
   *          a
   * @param speed
   *          s
   * @param deccel
   *          d
   * @param pos
   *          p
   */
  public void driveSpeedAccelDeccelPosM2(int accel, int speed, int deccel, int pos) {
    sendPacket(address, 66, byte3(accel), byte2(accel), byte1(accel), byte0(accel), byte3(speed), byte2(speed), byte1(speed), byte0(speed), byte3(deccel), byte2(deccel),
        byte1(deccel), byte0(deccel), byte3(pos), byte2(pos), byte1(pos), byte0(pos), buffer);
  }

  /**
   * <pre>
  67 - Buffered Drive M1 and M2 with signed Speed, Accel, Deccel and Position
  Move M1 and M2 positions from their current positions to the specified new positions and hold the
  new positions. Accel sets the acceleration value and deccel the decceleration value. QSpeed sets
  the speed in quadrature pulses the motor will run at after acceleration and before decceleration.
  Send: [Address, 67, AccelM1(4 bytes), SpeedM1(4 Bytes), DeccelM1(4 bytes),
  PositionM1(4 Bytes), AccelM2(4 bytes), SpeedM2(4 Bytes), DeccelM2(4 bytes),
  PositionM2(4 Bytes), Buffer, CRC(2 bytes)]
  Receive: [0xFF]
   * </pre>
   * 
   * @param accelM1
   *          a
   * @param speedM1
   *          s
   * @param deccelM1
   *          d
   * @param posM1
   *          p
   * @param accelM2
   *          a
   * @param speedM2
   *          s
   * @param deccelM2
   *          d
   * @param posM2
   *          p
   */
  public void driveSpeedAccelDeccelPosM1M2(int accelM1, int speedM1, int deccelM1, int posM1, int accelM2, int speedM2, int deccelM2, int posM2) {

    sendPacket(address, 67, byte3(accelM1), byte2(accelM1), byte1(accelM1), byte0(accelM1), byte3(speedM1), byte2(speedM1), byte1(speedM1), byte0(speedM1), byte3(deccelM1),
        byte2(deccelM1), byte1(deccelM1), byte0(deccelM1), byte3(posM1), byte2(posM1), byte1(posM1), byte0(posM1),

        byte3(accelM2), byte2(accelM2), byte1(accelM2), byte0(accelM2), byte3(speedM2), byte2(speedM2), byte1(speedM2), byte0(speedM2), byte3(deccelM2), byte2(deccelM2),
        byte1(deccelM2), byte0(deccelM2), byte3(posM2), byte2(posM2), byte1(posM2), byte0(posM2),

        buffer);
  }

  @Override
  public void onConnect(String portName) {
    log.info("onConnect from port {}", portName);
  }

  @Override
  public void onDisconnect(String portName) {
    log.info("disconnected from port {}", portName);
  }

  @Override
  public void onBytes(byte[] bytes) {
    for (int i = 0; i < bytes.length; i++) {
      log.debug(String.format("onByte %02X", bytes[i] & 0xFF));
    }
  }

  @Override
  public EncoderData publishEncoderData(EncoderData data) {
    return data;
  }

  public void s(boolean b) {
    if (b) {
      buffer = 0;
    } else {
      buffer = 1;
    }
  }

  public static void standardMotorTests01(RoboClaw rc) throws Exception {

    MotorPort m1 = (MotorPort) Runtime.start("m1", "MotorPort");
    MotorPort m2 = (MotorPort) Runtime.start("m2", "MotorPort");

    for (int i = 0; i < 10; ++i) {
      m1.move(0.1 * i);
    }

    // Joystick joy = (Joystick) Runtime.start("joy", "Joystick");
    // Arduino arduino = (Arduino)Runtime.start("arduino","Arduino");

    // doesnt matter for usb connection
    // roboclaw.setAddress(128);
    // roboclaw.setAddress(129);

    // configure services
    m1.setPort("m1");
    m2.setPort("m2");
    // joy.setController(5); // 0 on Linux

    // attach services
    rc.attach(m1);
    rc.attach(m2);
    // m1.attach(joy.getAxis("y"));
    // m2.attach(joy.getAxis("rz"));

    // m1.setInverted(true);
    // m2.setInverted(true);
    // m2.attach(arduino.getPin("A4"));

    // FIXME - roboclaw.attach(motor1) & roboclaw.attach(motor2)
    // FIXME - motor1.attach(joystick) !

    // roboclaw.resetQuadratureEncoderCounters();
    // roboclaw.restoreDefaults();

    // roboclaw.resetQuadratureEncoderCounters();

    // m1.stop();
    // m2.stop();
    rc.readEncoderM1();
    rc.readEncoderM1();

    m1.move(0);

    rc.readEncoderM1();
    rc.readEncoderM1();

    rc.resetEncoders();

    rc.readEncoderM1();
    rc.readEncoderM1();

    // roboclaw.readEncoderCount();
    // roboclaw.read
    rc.driveSpeedAccelDeccelPosM1(500, 500, 500, 10000);
    rc.driveDutyAccelM1(255, 255);

    rc.readEncoderM1();
    rc.readEncoderM1();

    m1.move(0);

    // public void bufferedDriveM1SpeedAccelDeccelPosition(int
    // accel, int speed, int deccel, int pos, int buffer)

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

  }

  public static void positionalTest01(RoboClaw rc) throws Exception {

    boolean done = false;

    while (!done) {

      // rc.readM1VelocityPIDandQPPS();

      rc.readPidM1();
      rc.readPidM1();

      // rc.setM1PID(0, 15000, 45, 1000000, 500, 0, c);
      // rc.setM1PID(D, P, I, maxI, deadzone, minPos, maxPos);
      // rc.setPidQppsM1(0, 15001, 46, 56000);
      // rc.setPidM1(0, 15001, 46, 0, 501, 0, 4000001);

      rc.setPidQppsDeadzoneMinMaxM1(0, 15000, 50, 56000, 500, 0, 4000000);

      rc.readPidM1();
      rc.readPidM1();

      rc.resetEncoders();

      // rc.setM1PID(D, P, I, maxI, deadzone, minPos, c);

      // TOO - set pid & qpps - "auto-tune"

      // rc.driveM1SpeedAccelDeccelPosition(42000, 44000, 44000, 820000);
      int speed = 1000000;
      int accel = 1000000;
      int deccel = 1000000;
      // max pos 4000000
      int pos = 3000000;
      // int pos = 0;
      rc.driveSpeedAccelDeccelPosM1(speed, accel, deccel, pos);
      rc.driveSpeedAccelDeccelPosM1(speed, accel, deccel, 1000000);
      rc.readEncoderM1();

      log.info("encoder 1 {}", rc.readEncoderM1());

      // stop and reset
      log.info("stopping motorsx reseting encoders");
      rc.driveForwardM1(0);
      sleep(500);
      rc.resetEncoders();
      log.info("encoder {}", rc.readEncoderM1());

      log.info("running forward 90 duty for 1s");
      rc.driveForwardM1(90);
      sleep(500);
      log.info("mid raw speed {}", rc.readSpeedM1());
      sleep(500);
      log.info("stopping motor waiting 0.5s");
      rc.driveForwardM1(0);
      sleep(500);
      log.info("raw speed {} encoder {}", rc.readSpeedM1(), rc.readEncoderM1());

      // stop and reset
      /*
       * log.info("stopping motors reseting encoders"); rc.driveForwardM1(0);
       * sleep(500); rc.resetQuadratureEncoderCounters(); log.info("encoder {}",
       * rc.readEncoderM1());
       * 
       * log.info("encoder {}", rc.readEncoderM1());
       * 
       * // try drive command rc.bufferedM1DriveSpeedDistance(60900, 80000);
       * log.info("encoder m1 {}", rc.readEncoderM1());
       */

      // stop and reset
      log.info("stopping motors reseting encoders");
      rc.driveBackwardM1(0);
      sleep(500);
      // rc.resetQuadratureEncoderCounters();
      log.info("encoder {}", rc.readEncoderM1());

      log.info("running forward 90 duty for 1s");
      rc.driveBackwardM1(90);
      sleep(500);
      log.info("mid raw speed {}", rc.readSpeedM1());
      sleep(500);
      log.info("stopping motor waiting 0.5s");
      rc.driveBackwardM1(0);
      sleep(500);
      log.info("raw speed {} encoder {}", rc.readSpeedM1(), rc.readEncoderM1());
      log.info("here");
    }

  }

  public static void scriptTest01() throws IOException {
    Python python = (Python) Runtime.start("python", "Python");

    while (true) {
      python.execFile("../pyrobotlab/home/GroG/RoboClaw.py");
    }

  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init("INFO");

      // FIXME !!!
      // Serial.getPortNames(); !!!!

      String port = "COM4";
      // String port = "/dev/ttyS10";
      // String port = "/dev/ttyACM0";
      // String port = "vuart";

      boolean virtual = false;
      Serial uart = null;
      if (virtual) {
        uart = Serial.connectVirtualUart(port);
        uart.logRecv(true); // dump bytes sent from roboclaw
      }

      // scriptTest01();

      Runtime.start("gui", "SwingGui");
      Runtime.start("python", "Python");

      RoboClaw rc = (RoboClaw) Runtime.start("roboclaw", "RoboClaw");
      rc.connect(port);

      boolean done = false;
      if (done) {
        return;
      }

      positionalTest01(rc);
      // start the services
      // Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      log.error("main threw {}", e); // FIXME - remove all these !!!
    }

  }

}
