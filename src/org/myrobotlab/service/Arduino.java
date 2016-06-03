package org.myrobotlab.service;

import static org.myrobotlab.codec.serial.ArduinoMsgCodec.ANALOG_READ_POLLING_START;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.ANALOG_READ_POLLING_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.ANALOG_WRITE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DIGITAL_READ_POLLING_START;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DIGITAL_READ_POLLING_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DIGITAL_WRITE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.GET_VERSION;
// FIXME - need to add this to the generator !
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.MAGIC_NUMBER;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.MAX_MSG_SIZE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.MRLCOMM_VERSION;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PIN_MODE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_CUSTOM_MSG;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_LOAD_TIMING_EVENT;
/////// JAVA GENERATED DEFINITION BEGIN - DO NOT MODIFY //////
///// java static import definition - DO NOT MODIFY - Begin //////
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_MRLCOMM_ERROR;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_PIN;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_PULSE_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_SENSOR_DATA;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_SERVO_EVENT;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_VERSION;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PULSE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PULSE_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SENSOR_ATTACH;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SENSOR_POLLING_START;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SENSOR_POLLING_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_ATTACH;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_DETACH;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_EVENTS_ENABLED;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_SWEEP_START;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_SWEEP_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_WRITE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_WRITE_MICROSECONDS;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_DEBOUNCE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_DIGITAL_TRIGGER_ONLY;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_LOAD_TIMING_ENABLED;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_PWMFREQUENCY;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_SAMPLE_RATE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_SERIAL_RATE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_SERVO_SPEED;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_TRIGGER;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_MESSAGE_ACK;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_DEBUG;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.myrobotlab.codec.mrlcomm.MrlCommMessage;
import org.myrobotlab.codec.serial.ArduinoMsgCodec;
import org.myrobotlab.framework.MRLException;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.CustomMsgListener;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.NameProvider;
import org.myrobotlab.service.interfaces.SensorDataPublisher;
import org.myrobotlab.service.interfaces.SensorDataSink;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * Implementation of a Arduino Service connected to MRL through a serial port.
 * The protocol is basically a pass through of system calls to the Arduino
 * board. Data can be passed back from the digital or analog ports by request to
 * start polling. The serial port can be wireless (bluetooth), rf, or wired. The
 * communication protocol supported is in MRLComm.ino
 * 
 * Should support nearly all Arduino board types
 * 
 * digitalRead() works on all pins. It will just round the analog value received
 * and present it to you. If analogRead(A0) is greater than or equal to 512,
 * digitalRead(A0) will be 1, else 0. digitalWrite() works on all pins, with
 * allowed parameter 0 or 1. digitalWrite(A0,0) is the same as
 * analogWrite(A0,0), and digitalWrite(A0,1) is the same as analogWrite(A0,255)
 * analogRead() works only on analog pins. It can take any value between 0 and
 * 1023. analogWrite() works on all analog pins and all digital PWM pins. You
 * can supply it any value between 0 and 255
 * 
 * TODO - make microcontroller interface - getPins digitalWrite analogWrite
 * writeMicroseconds pinMode etc.. TODO - remove all non-microcontroller methods
 * TODO - call-back parseData() from serial service --> to MicroController - so
 * microcontoller can parse format messages to universal REST format
 * 
 * TODO - set trigger in combination of polling should be a universal
 * microcontroller function
 * 
 * 
 * // need a method to identify type of board //
 * http://forum.arduino.cc/index.php?topic=100557.0
 * 
 * public static final int STEPPER_EVENT_STOP = 1; public static final int
 * STEPPER_TYPE_POLOLU = 1; public static final int CUSTOM_MSG = 50;
 * 
 * FUTURE UPLOADS
 * https://pragprog.com/magazines/2011-04/advanced-arduino-hacking
 * 
 */

/**
 * 
 * Interface Design Mantra
 * 
 * MRL runs on a computer. An Arduino is a Mircro controller. For all the things
 * in MRL which need an Arduino - there is a physical connection. E.g Servo
 * --plugs into--> Arduino --plugs into--> Computer running MRL or Motor --plugs
 * into--> Arduino --plugs into--> Computer running MRL
 * 
 * so in short - the communication between these services Motor & Arduino or
 * Servo & Arduino can be optimized, because the services will never be remote
 * from one another.
 * 
 * The whole publish, invoke subscribe messaging system works great, is fairly
 * efficient, and can work remotely. But an optimization here might be a good
 * thing if we have to route data from Serial -> Arduino -> Motor -> WebGui ->
 * Angular UI !
 * 
 * We will use standard Java callback Listener patterns. It should enforce the
 * methods needed from appropriate interfaces.
 * 
 * Arduino will have maps of other services it currently needs to callback to.
 * It possibly will have wrapper classes around those services in order to
 * prevent serialization issues (with the actual service marked as transient)
 * 
 * If the "controller" is marked as transient in object which is attached - this
 * possibly will fix cyclical serialization issues
 *
 */

public class Arduino extends Service implements I2CControl, SensorDataPublisher, SerialDataListener, ServoController, MotorController, SensorDataSink {

  /**
   * MotorData is the combination of a Motor and any controller data needed to
   * implement all of MotorController API
   * 
   */
  class MotorData implements Serializable {
    private static final long serialVersionUID = 1L;
    Motor motor = null;
  }

  // ---------- MRLCOMM FUNCTION INTERFACE BEGIN -----------
  /**
   * ServoController data needed to run a servo
   * 
   */
  class ServoData implements Serializable {
    private static final long serialVersionUID = 1L;
    transient ServoControl servo = null;
    Integer pin = null;
    int servoIndex = -1;
  }

  public static class Sketch implements Serializable {
    private static final long serialVersionUID = 1L;
    public String data;
    public String name;

    public Sketch(String name, String data) {
      this.name = name;
      this.data = data;
    }

  }

  public Sketch sketch;

  private static final long serialVersionUID = 1L;

  public transient final static Logger log = LoggerFactory.getLogger(Arduino.class);

  public static final int DIGITAL_VALUE = 1; // normalized with PinData <---

  // direction
  public static final int ANALOG_VALUE = 3; // normalized with PinData

  public static final int SENSOR_DATA = 37;

  // SUBTYPES ...
  public static final int ARDUINO_TYPE_INT = 16;

  // servo event types
  public static final int SERVO_EVENT_STOPPED = 1;

  public static final int SERVO_EVENT_POSITION_UPDATE = 2;

  // error types
  public static final int ERROR_SERIAL = 1;
  public static final int ERROR_UNKOWN_CMD = 2;
  // sensor types
  public static final int COMMUNICATION_RESET = 252;
  public static final int NOP = 255;

  public static final int TRUE = 1;
  public static final int FALSE = 0;

  Integer mrlCommVersion = null;

  /**
   * number of ms to pause after sending a message to the Arduino
   */
  public int delay = 0;

  /**
   * FIXME ! - these processor types ! - something we are not interested in and
   * do not have to deal with - we are far more interested in NUM_DIGITAL_PINS
   * and "board pin layouts" -
   * 
   * As far as I can tell board types are in variants 1.0.5 Arduino IDE includes
   * 
   * This is the best reference I have found regarding actual pin capabilities
   * https://learn.sparkfun.com/tutorials/arduino-comparison-guide#totally-
   * tabular Uno & Duemilanove have 14 digital pins (6 PWM) & 6 analog - total
   * 20 Mini & Pro have 14 digital pins (8 PWM) & 6 analog - total 20
   * 
   * ATmega328 Boards 32kB Program Space // 1 UART // 6 PWM // 4-8 Analog Inputs
   * // 9-14 Digital I/O ATmega2560 Arduino Mega's 256kB Program Space // 4
   * UARTs // 14 PWM // 16 Analog Inputs // 54 Digital I/O -
   * 
   * So at the moment .. there is only Uno & Mega !!!
   * 
   */
  public transient static final String BOARD_TYPE_UNO = "Uno";
  public transient static final String BOARD_TYPE_MEGA = "Mega";

  /**
   * pin description of board
   */
  ArrayList<Pin> pinList = null;

  // needed to dynamically adjust PWM rate (D. only?)
  public static final int TCCR0B = 0x25; // register for pins 6,7
  public static final int TCCR1B = 0x2E; // register for pins 9,10
  public static final int TCCR2B = 0xA1; // register for pins 3,11

  // FIXME - more depending on board (mega)
  // http://playground.arduino.cc/Code/MegaServo
  // Servos[NBR_SERVOS] ; // max servos is 48 for mega, 12 for other boards
  // int pos
  // public static final int MAX_SERVOS = 12;
  public static final int MAX_SERVOS = 48;
  // imported Arduino constants
  public static final int HIGH = 0x1;

  public static final int LOW = 0x0;
  public static final int INPUT = 0x0;

  public static final int OUTPUT = 0x1;

  public static final int MOTOR_FORWARD = 1;

  public static final int MOTOR_BACKWARD = 0;

  /**
   * board type - UNO Mega etc..
   */
  public String board;

  /**
   * blocking queues to support blocking methods
   */
  // transient BlockingQueue<Integer> versionQueue = new
  // LinkedBlockingQueue<Integer>();

  // HashMap<String, Motor> motors = new HashMap<String, Motor>();
  // HashMap<Integer, Motor> motorIndex = new HashMap<Integer, Motor>();

  // encoder pin index for callbacks
  HashMap<Integer, Motor> encoderPins = new HashMap<Integer, Motor>();

  transient CustomMsgListener customEventListener = null;

  /**
   * index reference of servo
   */
  HashMap<Integer, ServoData> servoIndex = new HashMap<Integer, ServoData>();

  /**
   * As simple pojo wrapper to contain the service and its index in MRLComm.ino
   * Used for callbacks
   *
   */
  public static class SensorData {
    public SensorData(Integer index, SensorDataSink sensor) {
      this.index = index;
      this.sensor = sensor;
    }

    /**
     * the sensor
     */
    transient public SensorDataSink sensor;

    /**
     * index of the sensor on the MRLComm.ino side
     */
    public Integer index;
  }

  /**
   * sensors - name index of sensor we need 2 indexes for sensors because they
   * will be referenced by name OR by index
   */
  HashMap<String, SensorData> sensors = new HashMap<String, SensorData>();
  /**
   * index reference of sensor
   */
  HashMap<Integer, SensorData> indexToSensor = new HashMap<Integer, SensorData>();

  /**
   * Serial service - the Arduino's serial connection
   */
  Serial serial;

  int error_arduino_to_mrl_rx_cnt;
  int error_mrl_to_arduino_rx_cnt;

  int byteCount;

  int msgSize;

  transient int[] msg = new int[MAX_MSG_SIZE];

  // parameters for testing the getVersion retry stuff.
  // TODO: some way to do this more synchronously
  // perhaps when we connect to the serial port, MRLComm can just have the
  // version waiting?
  public int retryConnectMax = 3;
  public int retryConnectDelay = 1500;

  // make sure this is sync'd across threads, 
  private volatile boolean ackRecieved = false;
  private int numAck = 0;

  // ---------------------------- ServoController End -----------------------
  // ---------------------- Protocol Methods Begin ------------------

  public Arduino(String n) {
    super(n);
    serial = (Serial) createPeer("serial");
    createPinList();
    String mrlcomm = FileIO.resourceToString("Arduino/MRLComm.c");
    setSketch(new Sketch("MRLComm", mrlcomm));
    // add self as Pin Array Sensor Listener
    sensorAttach(this);
  }

  public void addCustomMsgListener(CustomMsgListener service) {
    customEventListener = service;
  }

  /**
   * start analog polling of selected pin
   * 
   * @param pin
   */
  public void analogReadPollingStart(Integer pin, Integer sampleRate) {
    // check pin type - if not analog then change PIN_MODE
    // sendMsg(PIN_MODE, pin, INPUT); DUH - not needed !
    // sendMsg(ANALOG_READ_POLLING_START, pin);
    // sendMsg(SENSOR_ATTACH, pin, );
    // sensorAttachPin = pin;
    // sensorAttach(this);

    sendMsg(ANALOG_READ_POLLING_START, pin, (sampleRate >> 8) & 0xff, sampleRate & 0xff);
  }

  public void analogReadPollingStart(Integer pin) {
    analogReadPollingStart(pin, 1);
  }

  /**
   * stop the selected pin from polling analog reads
   * 
   * @param pin
   */
  public void analogReadPollingStop(Integer pin) {
    sendMsg(ANALOG_READ_POLLING_STOP, pin);
  }

  public void analogWrite(Integer address, Integer value) {
    log.info(String.format("analogWrite(%d,%d) to %s", address, value, serial.getName()));
    // FIXME
    // if (pin.mode == INPUT) {sendMsg(PIN_MODE, OUTPUT)}
    sendMsg(ANALOG_WRITE, address, value);
  }

  public void connect(String port) {
    // call the other method here.
    connect(port, Serial.BAUD_115200, 8, 1, 0);
  }

  /**
   * default params to connect to Arduino & MRLComm.ino
   * 
   * @param port
   * @return
   * @throws IOException
   * @throws SerialDeviceException
   */
  @Override
  public void connect(String port, Integer rate, int databits, int stopbits, int parity) {

    // FIXME ! <<<-- REMOVE ,this) - patterns should be to add listener on
    // startService
    // return connect(port, 57600, 8, 1, 0); <- put this back ?
    // return serial.connect(port); // <<<-- REMOVE ,this) - patterns
    // should be to add listener on
    // startService
    boolean ret = serial.connect(port, rate, databits, stopbits, parity);
    log.info("RETRUNED VALUE FROM CONNECT: {}", ret);
    Integer version = getVersion();
    if (version == null || version != MRLCOMM_VERSION) {
      error("MRLComm expected version %d actual is %d", MRLCOMM_VERSION, version);
      return;
    }
  }

  // FIXME - DEPRECATE !!! only need createVirtual(port)
  // TODO - should be override .. ??
  /*
   * public Serial connectVirtualUART() throws IOException,
   * ClassNotFoundException, InstantiationException, IllegalAccessException,
   * NoSuchMethodException, SecurityException, IllegalArgumentException,
   * InvocationTargetException { Serial uart = serial.createVirtualUART();
   * uart.setCodec("arduino"); connect(serial.getName()); return uart; }
   * 
   * static public VirtualDevice createVirtual(String port) throws IOException{
   * // Once device to rule them all ? - I think that would work.. VirtualDevice
   * virtual = (VirtualDevice) Runtime.start("virtual", "VirtualDevice"); //
   * this call would generate the instance of virtual device needed
   * virtual.createVirtualArduino(port); return virtual; }
   */

  public ArrayList<Pin> createPinList() {
    pinList = new ArrayList<Pin>();
    int pinType = Pin.DIGITAL_VALUE;
    if (board != null && board.toLowerCase().contains("mega")) {
      for (int i = 0; i < 70; ++i) {
        if (i < 1 || (i > 13 && i < 54)) {
          pinType = Pin.DIGITAL_VALUE;
        } else if (i > 53) {
          pinType = Pin.ANALOG_VALUE;
        } else {
          pinType = Pin.PWM_VALUE;
        }
        pinList.add(new Pin(i, pinType, 0, getName()));
      }
    } else {
      for (int i = 0; i < 20; ++i) {
        if (i < 14) {
          pinType = Pin.DIGITAL_VALUE;
        } else {
          pinType = Pin.ANALOG_VALUE;
        }
        if (i == 3 || i == 5 || i == 6 || i == 9 || i == 10 || i == 11) {
          pinType = Pin.PWM_VALUE;
        }
        pinList.add(new Pin(i, pinType, 0, getName()));
      }
    }
    return pinList;
  }

  /**
   * start polling data from the selected pin
   * 
   * @param pin
   */
  public void digitalReadPollingStart(Integer pin) {
    digitalReadPollingStart(pin, 1);
  }

  public void digitalReadPollingStart(Integer pin, Integer sampleRate) {
    sendMsg(PIN_MODE, pin, INPUT);
    sendMsg(DIGITAL_READ_POLLING_START, pin, (sampleRate >> 8) & 0xff, sampleRate & 0xff);
  }

  /**
   * stop polling the selected pin
   * 
   * @param pin
   */
  public void digitalReadPollingStop(Integer pin) {
    sendMsg(DIGITAL_READ_POLLING_STOP, pin);
  }

  public void digitalWrite(Integer address, Integer value) {
    info("digitalWrite (%d,%d) to %s", address, value, serial.getName());
    sendMsg(DIGITAL_WRITE, address, value);
    pinList.get(address).value = value;
  }

  public void disconnect() {
    serial.disconnect();
  }

  public String getBoardType() {
    return board;
  }

  @Override
  public ArrayList<Pin> getPinList() {
    return pinList;
  }

  /**
   * Use the serial service for serial activities ! No reason to replicate
   * methods
   * 
   * @return
   */
  public Serial getSerial() {
    return serial;
  }

  public Sketch getSketch() {
    return sketch;
  }

  /**
   * GOOD DESIGN (for Arduino hardware Async) !! - blocking version of
   * getVersion - blocks on publishVersion method returns null if 1 second
   * timeout is reached.
   * 
   * This is a good pattern for future blocking methods.
   * 
   * @return
   */
  public Integer getVersion() {
    log.info("{} {} getVersion", getName(), serial.getPortName());
    int retry = 0;
    try {
      /**
       * We will try up to retryConnectMax times to get a version out of
       * MRLComm.c and wait 333 ms between each try. A blocking queue is not
       * needed, as this is only a single data element - and blocking is not
       * necessary. mrlCommVersion will be set by our port listener in
       * PUBLISH_VERSION if the result comes back.
       */
      while ((retry < retryConnectMax) && (mrlCommVersion == null)) {
        // versionQueue.clear();
        sendMsg(GET_VERSION);
        // mrlCommVersion = versionQueue.poll(1000, TimeUnit.MILLISECONDS);
        sleep(retryConnectDelay);
        ++retry;
        log.info("getVersion attempt # {}", retry);
      }
    } catch (Exception e) {
      Logging.logError(e);
    }
    if (mrlCommVersion == null) {
      error(String.format("%s did not get response from arduino....", serial.getPortName()));
    } else if (!mrlCommVersion.equals(MRLCOMM_VERSION)) {
      error(String.format("MRLComm.ino responded with version %s expected version is %s", mrlCommVersion, MRLCOMM_VERSION));
    } else {
      info(String.format("%s connected on %s responded version %s ... goodtimes...", serial.getName(), serial.getPortName(), mrlCommVersion));
    }
    return mrlCommVersion;
  }

  public boolean isConnected() {
    // include that we must have gotten a valid MRLComm version number.
    if (serial != null && serial.isConnected() && mrlCommVersion != null) {
      return true;
    }
    return false;
  }

  @Override
  public void motorAttach(MotorControl motor) throws MRLException {
    if (!motor.isLocal()) {
      throw new MRLException("motor is not in the same MRL instance as the motor controller");
    }

    int[] controlPins = motor.getControlPins();
    for (int i = 0; i < controlPins.length; ++i) {
      pinMode(controlPins[i], OUTPUT);
    }

    String type = motor.getType();

    if (type == null) {
      throw new IllegalArgumentException("");
    }

    // if we have a pulse step - we can do a form
    // of false encoding :P
    if (motor.getType().equals(Motor.TYPE_PULSE_STEP)) { // TODO - add other
      // "real"
      // encoders
      // the pwm pin in a pulse step motor "is" the encoder
      sensorAttach(motor);
    }

    motor.setController(this);

    /*
     * FIXME - implement "real" encoder later String encoderType =
     * motor.getEncoderType(); if (encoderType != null &&
     * !encoderType.equals(Motor.ENCODER_TYPE_NONE) || (motor.getType() != null
     * && motor.getType().equals(Motor.TYPE_PULSE_STEP))) { //
     * encoderPins.put(motor.encoderPin, motor); sensorIndex.put(motor.pwmPin,
     * new SensorData(motor.pwmPin, motor)); // FIXME - based on type - real
     * encoder... // analogReadPollingStart(motor.encoderPin);
     * sensorAttach(motor); } // FIXME else if (
     */

    motor.broadcastState();
  }

  public void attach(String name) throws MRLException {
    NameProvider si = Runtime.getService(name);

    if (si instanceof Motor) {
      motorAttach((Motor) si);
    } else if (si instanceof UltrasonicSensor) {
      servoAttach((Servo) si);
    } else if (si instanceof UltrasonicSensor) {
      sensorAttach((UltrasonicSensor) si);
    } else {
      throw new MRLException("%s don't know how to attach a %s", getName(), si.getClass().getSimpleName());
    }

    // else if instance of Servo ... yattah yattah yattah
  }

  public void detach(String name) {
    NameProvider si = Runtime.getService(name);

    if (si instanceof Motor) {
      motorDetach((Motor) si);
    }
  }

  // ================= new interface end =========================

  @Override
  public boolean motorDetach(MotorControl motor) {
    /*
     * boolean ret = motors.containsKey(motorName); if (ret) {
     * motors.remove(motorName); } return ret;
     */
    return true;
  }

  @Override
  public void motorMove(MotorControl motor) {

    double powerOutput = motor.getPowerOutput();
    String type = motor.getMotorType();

    if (Motor.TYPE_SIMPLE.equals(type)) {
      sendMsg(DIGITAL_WRITE, motor.getPin(Motor.PIN_TYPE_DIR), (powerOutput < 0) ? MOTOR_BACKWARD : MOTOR_FORWARD);
      sendMsg(ANALOG_WRITE, motor.getPin(Motor.PIN_TYPE_PWM), (int) Math.abs(powerOutput));
    } else if (Motor.TYPE_2_PWM.equals(type)) {
      if (powerOutput < 0) {
        sendMsg(ANALOG_WRITE, motor.getPin(Motor.PIN_TYPE_PWM_LEFT), 0);
        sendMsg(ANALOG_WRITE, motor.getPin(Motor.PIN_TYPE_PWM_RIGHT), (int) Math.abs(powerOutput));
      } else {
        sendMsg(ANALOG_WRITE, motor.getPin(Motor.PIN_TYPE_PWM_RIGHT), 0);
        sendMsg(ANALOG_WRITE, motor.getPin(Motor.PIN_TYPE_PWM_LEFT), (int) Math.abs(powerOutput));
      }
    } else if (Motor.TYPE_PULSE_STEP.equals(type)) {
      // sdsendMsg(ANALOG_WRITE, motor.getPin(Motor.PIN_TYPE_PWM_RIGHT),
      // 0);
      // TODO implement with a -1 for "endless" pulses or a different
      // command parameter :P
      sendMsg(PULSE, motor.getPin(Motor.PIN_TYPE_PULSE), (int) Math.abs(powerOutput));
    } else {
      error("motorMove for motor type %s not supported", type);
    }

  }

  @Override
  public void motorMoveTo(MotorControl motor) {
    // speed parameter?
    // modulo - if < 1
    // speed = 1 else
    log.info("motorMoveTo targetPos {} powerLevel {}", motor.getTargetPos(), motor.getPowerLevel());

    int feedbackRate = 1;
    // if pulser (with or without fake encoder
    // send a series of pulses !
    // with current direction
    if (motor.getType().equals(Motor.TYPE_PULSE_STEP)) {
      // check motor direction
      // send motor direction
      // TODO powerLevel = 100 * powerlevel

      // FIXME !!! - this will have to send a Long for targetPos at some
      // point !!!!
      double target = Math.abs(motor.getTargetPos());

      int b0 = (int) target & 0xff;
      int b1 = ((int) target >> 8) & 0xff;
      int b2 = ((int) target >> 16) & 0xff;
      int b3 = ((int) target >> 24) & 0xff;

      sendMsg(PULSE, sensors.get(motor.getName()).index, b3, b2, b1, b0, (int) motor.getPowerLevel(), feedbackRate);
    }

  }

  @Override
  public void motorStop(MotorControl motor) {

    if (motor.getType().equals(Motor.TYPE_PULSE_STEP)) {
      // check motor direction
      // send motor direction
      // TODO powerLevel = 100 * powerlevel

      // FIXME !!! - this will have to send a Long for targetPos at some
      // point !!!!
      sendMsg(PULSE_STOP, motor.getPin(Motor.PIN_TYPE_PWM));
    } else if (motor.getType().equals(Motor.TYPE_SIMPLE)) {
      sendMsg(ANALOG_WRITE, motor.getPin(Motor.PIN_TYPE_PWM), 0);
    } else if (motor.getType().equals(Motor.TYPE_SIMPLE)) {
      sendMsg(ANALOG_WRITE, motor.getPin(Motor.PIN_TYPE_PWM_LEFT), 0);
      sendMsg(ANALOG_WRITE, motor.getPin(Motor.PIN_TYPE_PWM_RIGHT), 0);
    }

  }

  /**
   * Callback for Serial service - local (not remote) although a
   * publish/subscribe could be created - this method is called by a thread
   * waiting on the Serial's RX BlockingQueue
   * 
   * Other services may use the same technique or subscribe to a Serial's
   * publishByte method
   * 
   * it might be worthwhile to look in optimizing reads into arrays vs single
   * byte processing .. but maybe there would be no gain
   * 
   */

  // FIXME - onByte(int[] data)
  @Override
  public Integer onByte(Integer newByte) {
    try {
      if (log.isDebugEnabled()) {
        log.debug("onByte {}", newByte);
      }
      /**
       * Archtype InputStream read - rxtxLib does not have this straightforward
       * design, but the details of how it behaves is is handled in the Serial
       * service and we are given a unified interface
       * 
       * The "read()" is data taken from a blocking queue in the Serial service.
       * If we want to support blocking functions in Arduino then we'll
       * "publish" to our local queues
       */
      // TODO: consider reading more than 1 byte at a time ,and make this
      // callback onBytes or something like that.
      ++byteCount;
      if (byteCount == 1) {
        if (newByte != MAGIC_NUMBER) {
          byteCount = 0;
          msgSize = 0;
          warn(String.format("Arduino->MRL error - bad magic number %d - %d rx errors", newByte, ++error_arduino_to_mrl_rx_cnt));
          // dump.setLength(0);
        }
        return newByte;
      } else if (byteCount == 2) {
        // get the size of message
        if (newByte > 64) {
          byteCount = 0;
          msgSize = 0;
          error(String.format("Arduino->MRL error %d rx sz errors", ++error_arduino_to_mrl_rx_cnt));
          return newByte;
        }
        msgSize = (byte) newByte.intValue();
        // dump.append(String.format("MSG|SZ %d", msgSize));
      } else if (byteCount > 2) {
        // remove header - fill msg data - (2) headbytes -1
        // (offset)
        // dump.append(String.format("|P%d %d", byteCount,
        // newByte));
        msg[byteCount - 3] = (byte) newByte.intValue();
      }
      if (byteCount == 2 + msgSize) {
        // we've received a full message
        // process valid message
        // TODO: deserialize this byte array as an mrl message object to
        // help clean up the code.
        // int[] payload = Arrays.copyOfRange(msg, 2, msgSize);
        // MrlCommMessage mrlMsg = new MrlCommMessage(msg[0], payload);
        processMessage(msg);
        // clean up memory/buffers
        msgSize = 0;
        byteCount = 0;
        Arrays.fill(msg, 0); // optimize remove
      }
    } catch (Exception e) {
      ++error_mrl_to_arduino_rx_cnt;
      error("msg structure violation %d", error_mrl_to_arduino_rx_cnt);
      // try again (clean up memory buffer)
      msgSize = 0;
      byteCount = 0;
      Logging.logError(e);
    }
    return newByte;
  }

  // This is called when a valid message is received from the serial port.
  private void processMessage(int[] message) {


    // MSG CONTENTS = FN | D0 | D1 | ...
    int function = message[0];
    log.info("Process Message Called: {}", ArduinoMsgCodec.functionToString(function));
    //    if (log.isDebugEnabled()) {
    //      log.debug("Process Message Called: {}", ArduinoMsgCodec.functionToString(function));
    //    }
    switch (function) {
      case PUBLISH_MRLCOMM_ERROR: {
        ++error_mrl_to_arduino_rx_cnt;
        error("MRL->Arduino rx %d type %d", error_mrl_to_arduino_rx_cnt, message[1]);
        break;
      }
      case PUBLISH_VERSION: {
        // TODO - get vendor version
        // String version = String.format("%d", message[1]);
        // versionQueue.add(message[1] & 0xff);
        mrlCommVersion = message[1] & 0xff;
        log.info("PUBLISH_VERSION {}", mrlCommVersion);
        invoke("publishVersion", mrlCommVersion);
        break;
      }
      // DEPRECATED - handled by PUBLISH_SENSOR_DATA
      case PUBLISH_PIN: {
        Pin pin = pinList.get(message[1]);
        pin.value = ((message[2] & 0xFF) << 8) + (message[3] & 0xFF);
        // TODO ? local callback - no thread single invoke -
        // publishLocalCallbacks("publishPin", )
        invoke("publishPin", pin);
        break;
      }
      case PUBLISH_LOAD_TIMING_EVENT: {
        long microsPerLoop = Serial.bytesToInt(message, 1, 4);
        info("load %d us", microsPerLoop);
        break;
      }
      case PUBLISH_SERVO_EVENT: {
        int index = message[1];
        int eventType = message[2];
        int currentPos = message[3];
        int targetPos = message[4];
        log.info(String.format(" index %d type %d cur %d target %d", index, eventType, currentPos & 0xff, targetPos & 0xff));
        // uber good -
        // TODO - deprecate ServoControl interface - not
        // needed Servo is abstraction enough
        Servo servo = (Servo) servoIndex.get(index).servo;
        servo.invoke("publishServoEvent", currentPos & 0xff);
        break;
      }
      case PUBLISH_SENSOR_DATA: {
        // get the sensor callback index
        int sensorIndex = (int) message[1];
        // get the sensor
        SensorData sensorData = indexToSensor.get(sensorIndex);
        if (sensorData == null) {
          // uh oh?  bad data form somewhere?
          log.warn("Unknown sensor data received...");
        } else {
          SensorDataSink sensor = sensorData.sensor;
          // find its needed datatype
          Object data = resolveSensorData(sensor, message);
          sensor.update(data);
        }
        break;
      } // PUBLISH_SENSOR_DATA
      // could be deprecated
      /*
       * case PUBLISH_PULSE: { int index = (int) message[1]; // FIXME -
       * assumption its a encoder pin on a Motor NO !!! Motor motor =
       * encoderPins.get(index);
       * 
       * Long data = Serial.bytesToLong(message, 2, 4);
       * 
       * sensor.update(data); break; }
       */
      // OOB
      case PUBLISH_PULSE_STOP: {
        int index = (int) message[1];
        // FIXME - assumption its a encoder pin on a Motor NO !!!
        SensorDataSink sensor = indexToSensor.get(index).sensor;
        Integer data = Serial.bytesToInt(message, 2, 4);
        sensor.update(data);
        break;
      }
      case PUBLISH_CUSTOM_MSG: {
        // msg or data is of size byteCount
        Object[] params = resolveCustomMessage(message);
        invoke("publishCustomMsg", new Object[] { params });
        break;
      }
      case PUBLISH_MESSAGE_ACK: {
        // log.info("Message Ack received.");
        ackRecieved = true;

        numAck++;
        // TODO: do i need to call notify?
        // notify(); 
        // TODO: something more async for notification on this mutex.
        break;
      }
      case PUBLISH_DEBUG: {
        // convert the int array to a string.
        // TODO is there an easier / better way to do this?
        StringBuilder payload = new StringBuilder();
        for (int i = 1; i < msgSize; i++) {
          payload.append((char)message[i]);
        }
        log.info("MRLComm Debug Message {}",payload);
        break;
      }
      default: {
        // FIXME - use formatter for message
        error("unknown serial event %d", function);
        break;
      }
    } // end switch
  }

  // given an int array that is a custom message, return the object that
  // represents that message.
  private Object[] resolveCustomMessage(int[] message) {
    int paramCnt = message[1];
    int paramIndex = 2; // current index in buffer
    // decode parameters
    Object[] params = new Object[paramCnt];
    int paramType = 0;
    for (int i = 0; i < paramCnt; ++i) {
      // get parameter type
      // paramType = message[];
      paramType = message[paramIndex];
      Integer x = 0;
      // convert
      if (paramType == ARDUINO_TYPE_INT) {
        // params[i] =
        x = ((message[++paramIndex] & 0xFF) << 8) + (message[++paramIndex] & 0xFF);
        if (x > 32767) {
          x = x - 65536;
        }
        params[i] = x;
        log.info(String.format("parameter %d is type ARDUINO_TYPE_INT value %d", i, x));
        ++paramIndex;
      } else {
        error("CUSTOM_MSG - unhandled type %d", paramType);
      }
    }
    // how to reflectively invoke multi-param method
    // (Python?)
    // FIXME - if local call directly? - this is an optimization
    if (customEventListener != null) {
      // send(customEventListener.getName(), "onCustomMsg",
      // params);
      customEventListener.onCustomMsg(params);
    }
    // FIXME more effecient to only allow subscribers which have
    // used the addCustomMsgListener?
    return params;
  }

  private Object resolveSensorData(SensorDataSink sensor, int[] message) {
    int clazz = sensor.getDataSinkType();
    Object data = null;
    // convert all return types from
    // asynchronous callbacks of sensors
    switch (clazz) {
      case DATA_SINK_TYPE_INTEGER: {
        data = Serial.bytesToInt(message, 3, 2); // 16 bit - 2 byte
        // int
      }
      break;
      case DATA_SINK_TYPE_PIN: {
        Pin pin = pinList.get(message[2]);
        pin.value = ((message[3] & 0xFF) << 8) + (message[4] & 0xFF);
        data = pin;
      }
      break;
      default: {
        error("unknown return type %d", clazz);
      }
      break;
    }
    return data;
  }

  @Override
  public String onConnect(String portName) {
    info("%s connected to %s", getName(), portName);
    // Get version should alread have been called.  don't call it again!
    // getVersion();
    return portName;
  }

  public String getPortName() {
    return serial.getPortName();
  }

  public void onCustomMsg(Integer ax, Integer ay, Integer az) {
    log.info("onCustomMsg");
  }

  @Override
  public String onDisconnect(String portName) {
    info("%s disconnected from %s", getName(), portName);
    return portName;
  }

  public void pinMode(int address, String mode) {
    if (mode != null && mode.equalsIgnoreCase("INPUT")) {
      pinMode(address, INPUT);
    } else {
      pinMode(address, OUTPUT);
    }
  }

  public void pinMode(Integer address, Integer value) {
    log.info("pinMode({},{}) to {}", address, value, serial.getName());
    sendMsg(PIN_MODE, address, value);
  }

  public Object[] publishCustomMsg(Object[] data) {
    return data;
  }

  // ----------- motor controller api end ----------------

  public Long publishLoadTimingEvent(Long us) {
    log.info(String.format("publishLoadTimingEvent - %d", us));
    return us;
  }

  public Integer publishMRLCommError(Integer code) {
    return code;
  }

  /**
   * This method is called with Pin data whene a pin value is changed on the
   * Arduino board the Arduino must be told to poll the desired pin(s). This is
   * done with a analogReadPollingStart(pin) or digitalReadPollingStart()
   */
  @Override
  public Pin publishPin(Pin p) {
    // log.info("Publish Pin: {}", p);
    pinList.get(p.pin).value = p.value;
    return p;
  }

  // ----------- MotorController API End ----------------

  public int publishServoEvent(Integer pos) {
    return pos;
  }

  public Pin publishTrigger(Pin pin) {
    return pin;
  }

  public Integer publishVersion(Integer version) {
    info("publishVersion %d", version);
    return version;
  }

  // ========== pulsePin begin =============
  public void pulse(int pin) {
    pulse(pin, -1);
  }

  public void pulse(int pin, int count) {
    pulse(pin, count, 1);
  }

  public void pulse(int pin, int count, int rate) {
    pulse(pin, count, rate, 1);
  }

  public void pulse(int pin, int count, int rate, int feedbackRate) {
    sendMsg(PULSE, pin, rate, feedbackRate);
  }

  /**
   * forced stop of a pulse series this will stop the pulses and send back a
   * publishPulsPinStop
   */
  public void pulseStop() {
    // sendMsg(PULSE_PIN_STOP);
  }

  public Long publishPulse(Long pulseCount) {
    return pulseCount;
  }

  /**
   * published stop of a pulse series this occurs when count # of pulses has
   * been reached or user intervention
   * 
   * @param currentCount
   * @return
   */
  public Integer publishPulseStop(Integer currentCount) {
    return currentCount;
  }

  // ========== pulsePin begin =============

  @Override
  public void releaseService() {
    super.releaseService();
    // soft reset - detaches servos & resets polling & pinmodes
    softReset();
    sleep(300);
    disconnect();
  }

  /**
   * MRL protocol method
   * 
   * @param function
   * @param param1
   * @param param2
   * 
   * TODO - take the cheese out of this method .. it shold be
   * sendMsg(byte[]...data)
   */
  public synchronized void sendMsg(int function, int... params) {
    log.info("Sending Message  : {}", ArduinoMsgCodec.functionToString(function));
    //    if (log.isDebugEnabled()) {
    //      log.debug("Sending Arduino message funciton {}", ArduinoMsgCodec.functionToString(function));
    //    }
    // some sanity checking.
    if (!serial.isConnected()) {
      log.warn("Serial port is not connected, unable to send message.");
      return;
    }
    // don't even attempt to send it if we know it's a bogus message.
    // TODO: we need to account for the magic byte & length bytes. max message size is 64-2 (potentially) 
    if (params.length > MAX_MSG_SIZE) {
      log.error("Arduino Message size was large! Function {} Size {}", function, params.length);
      return;
    }
    // System.out.println("Sending Message " + function );
    try {
      // Minimum MRLComm message is 3 bytes(int).
      // MAGIC_NUMBER|LENGTH|FUNCTION|PARAM0|PARAM1 would be valid
      int[] msgToSend = new int[3+params.length];
      msgToSend[0] = MAGIC_NUMBER;
      msgToSend[1] = 1 + params.length;
      msgToSend[2] = function;
      for (int i = 0 ; i < params.length; i++) {
        // What if the int is > 127 ?
        msgToSend[3+i] = params[i];
      }
      // send the message as an array. (serial port actually writes 1 byte at a time anyway.. oh well.)

      // set a flag that a message is in flight.
      ackRecieved = false;
      // notify();
      
      // Technically this is the only thing that needs to be synchronized i think.
      synchronized (msgToSend) {
        serial.write(msgToSend);        
      }
      // TODO: wait for an ack of the message to arrive!
      long start = System.currentTimeMillis();
      // wait a max of 100 for the ack.
      int limit = 1000;
      // log.info("Waiting on ack. {}", function);
      while (!ackRecieved) {
        // TODO: Avoid excessive cpu usage, and limit the amount of time we wait on this "ackRecieved" 
        // variable.  There's some java thread/notify stuff that might be better to use?
        // sleep 1 nanosecond?!
        // Thread.sleep(0,1);
        // sleep 1 millisecond
        Thread.sleep(1);
        // log.info("Waiting on ack {}", numAck);
        long delta = System.currentTimeMillis() - start;
        if (delta > limit) {
          // log.warn(, limit, function);
          warn(String.format("No ack received.. timing out after %d ms and continuing for function %s", limit, ArduinoMsgCodec.functionToString(function)));
          // eek?
          // ackRecieved = false;
          break;
        }
        // break;
      }      

      //if (log.isDebugEnabled()) {
      if (ackRecieved) {
        log.info("Arduino responded: {} {}", ArduinoMsgCodec.functionToString(function), numAck);
      } else {
        log.info("Ack not received : {} {}", ArduinoMsgCodec.functionToString(function), numAck);        
      }
      //}
      // putting delay at the end so we give the message and allow the arduino to process
      // this decreases the latency between when mrl sends the message
      // and the message is picked up by the arduino.
      // This helps avoid the arduino dropping messages and getting lost/disconnected.
      if (delay > 0) {
        Thread.sleep(delay);
      }
    } catch (Exception e) {
      error("sendMsg " + e.getMessage());
    }

  }

  // FIXME !! - implement sensorDetach !!!
  public synchronized boolean sensorAttach(SensorDataSink sensor) {
    String sensorName = sensor.getName();
    log.info(String.format("%s/sensorAttach/%s", getName(), sensorName));

    // simple count = index mapping
    int sensorIndex = sensors.size();

    // assume all other SensorSinks need connectivity (besides) Arduino ...
    if (serial == null && sensorIndex != 0) {
      error("could not attach sensor - no serial device!");
      return false;
    }

    if (sensors.containsKey(sensorName)) {
      log.warn("sensor already attach - detach first");
      return false;
    }

    // attach sensor index
    int[] config = sensor.getSensorConfig();
    int[] payload = new int[config.length + 3];

    payload[0] = sensorIndex;
    payload[1] = sensor.getSensorType();
    payload[2] = config.length;
    for (int i = 0; i < config.length; ++i) {
      payload[i + 3] = config[i];
    }

    // SENSOR_ATTACH Format
    // SENSOR_ATTACH | sensorIndex | sensorType | pinCount | pins ...

    log.info(String.format("sensor index %d type %d config %s", payload[0], payload[1], Arrays.toString(config)));

    // FIXME - interface should have - requiresConnection to attach vs if
    // (sensorIndex == 0)
    if (getName()
        .equals(sensorName) /* || !sensor.requiresConnectionToAttach() */) {
      log.info("{} attach self as SensorSink with sensorIndex {}", getName(), sensorIndex);
    } else {
      sendMsg(SENSOR_ATTACH, payload);
    }

    // sendMsg(SENSOR_ATTACH, index, sensor.getSensorType(),
    // sensor.getTriggerPin(), sensor.getEchoPin());

    SensorData sd = new SensorData(sensorIndex, sensor);

    sensors.put(sensorName, sd);
    indexToSensor.put(sensorIndex, sd);

    info(String.format("sensor index %d type %d config payload %s ", sensorIndex, sensor.getSensorType(), Arrays.toString(payload)));

    return true;
  }

  public boolean sensorPollingStart(String name, int timeoutMS) {
    info("sensorPollingStart %s", name);
    if (!sensors.containsKey(name)) {
      error("can not poll sensor %s - not defined", name);
      return false;
    }
    int index = sensors.get(name).index;
    sendMsg(SENSOR_POLLING_START, index, timeoutMS);
    return true;
  }

  public boolean sensorPollingStop(String name) {
    info("sensorPollingStop %s", name);
    if (!sensors.containsKey(name)) {
      error("can not poll sensor %s - not defined", name);
      return false;
    }
    int index = sensors.get(name).index;
    sendMsg(SENSOR_POLLING_STOP, index);
    return true;
  }

  /**
   * the complex pin to servo index attaching & detaching - then re-attaching to
   * a different servo DOES NOT WORK - because the Arduino servo library does
   * not cleanly detach
   * 
   * So Servo's and their pins need a constant mapping so that when one is
   * detached its always re-attached to the same one.
   * 
   * @param pin
   * @return
   */
  private int getServoIndex(int pin) {
    return pin - 2;
  }

  // FIXME - need interface for this
  public synchronized boolean servoAttach(Servo servo, Integer pin) {
    String servoName = servo.getName();
    log.info(String.format("servoAttach %s pin %d", servoName, pin));

    if (serial == null) {
      error("could not attach servo to pin %d serial is null - not initialized?", pin);
      return false;
    }

    // complex formula to calculate servo index
    // this "could" be complicated - even so compicated
    // as asking MRLComm.ino to find the "next available index
    // and send it back - but I've tried that scheme and
    // because the Servo's don't fully "detach" using the standard library
    // it proved very "bad"
    // simplistic mapping where Java is in control seems best
    // index is a mapping of pin - so Servo needs a wrapper class,
    // because servo does not contain this data !
    int index = getServoIndex(servo.getPin());

    if (servoIndex.containsKey(index)) {
      log.info("servo already attach - detach first");
      // important to return true - because we are "attached" !
      return true;
    }

    // simple re-map - to guarantee the same MRL Servo gets the same
    // MRLComm.ino servo
    if (pin < 2 || pin > MAX_SERVOS + 2) {
      error("pin out of range 2 < %d < %d", pin, MAX_SERVOS + 2);
      return false;
    }

    // we need to send the servo ascii name - format of SERVO_ATTCH is
    // SERVO_ATTACH (1 byte) | servo index (1 byte) | servo pin (1 byte) |
    // size of name (1 byte) | ASCII name of servo (N - bytes)
    // The name is not needed in MRLComm.ino - but it is needed in
    // virtualized Blender servo
    int payloadSize = 1 + 1 + 1 + servoName.length();

    int[] payload = new int[payloadSize];

    // payload[0] = SERVO_ATTACH;
    payload[0] = index;
    payload[1] = pin;
    payload[2] = servoName.length();

    byte ascii[] = servoName.getBytes();
    for (int i = 0; i < servoName.length(); ++i) {
      payload[i + 3] = 0xFF & ascii[i];
    }

    sendMsg(SERVO_ATTACH, payload);

    ServoData sd = new ServoData();
    sd.pin = pin;
    sd.servoIndex = index;
    sd.servo = servo;
    // servos.put(servo.getName(), sd);
    servoIndex.put(index, sd);
    servo.setController(this);
    servo.setPin(pin);
    log.info("servo index {} pin {} attached ", index, pin);
    return true;
  }

  @Override
  public boolean servoAttach(Servo servo) {
    if (servo == null) {
      error("servoAttach can not attach %s no service exists");
      return false;
    }
    return servoAttach(servo, servo.getPin());
  }

  @Override
  public synchronized boolean servoDetach(Servo servo) {
    String servoName = servo.getName();
    int index = getServoIndex(servo.getPin());
    log.info(String.format("servoDetach(%s) index %d", servoName, index));

    if (servoIndex.containsKey(index)) {
      sendMsg(SERVO_DETACH, index, 0);
      servoIndex.remove(index);
      return true;
    }

    error("servo %s detach failed - not found", servoName);
    return false;
  }

  // FIXME - do sweep single method call from ServoControl
  @Override
  public void servoSweepStart(Servo servo) {
    String servoName = servo.getName();
    if (!servoIndex.containsKey(servoName)) {
      warn("Servo %s not attached to %s", servoName, getName());
      return;
    }
    int index = getServoIndex(servo.getPin());
    log.info(String.format("servoSweep %s index %d min %d max %d step %d", servoName, index, servo.sweepMin, servo.sweepMax, servo.sweepStep));
    sendMsg(SERVO_SWEEP_START, index, servo.sweepMin, servo.sweepMax, servo.sweepStep);
  }

  @Override
  public void servoSweepStop(Servo servo) {
    sendMsg(SERVO_SWEEP_STOP, getServoIndex(servo.getPin()));
  }

  @Override
  public void servoWrite(Servo servo) {
    int index = getServoIndex(servo.getPin());
    log.info(String.format("servoWrite %s %d index %d", servo.getName(), servo.targetOutput, index));
    sendMsg(SERVO_WRITE, index, servo.targetOutput.intValue());
  }

  @Override
  public void servoWriteMicroseconds(Servo servo) {
    int index = getServoIndex(servo.getPin());
    log.info(String.format("writeMicroseconds %s %d index %d", servo.getName(), servo.uS, index));
    sendMsg(SERVO_WRITE_MICROSECONDS, index, servo.uS);
  }

  public String setBoard(String board) {
    this.board = board;
    createPinList();
    broadcastState();
    return board;
  }

  /**
   * easy way to set to a 54 pin arduino
   * 
   * @return
   */
  public String setBoardMega() {
    board = BOARD_TYPE_MEGA;
    createPinList();
    broadcastState();
    return board;
  }

  public String setBoardUno() {
    board = BOARD_TYPE_UNO;
    createPinList();
    broadcastState();
    return board;
  }

  /**
   * Debounce ensures that only a single signal will be acted upon for a single
   * opening or closing of a contact. the delay is the min number of pc cycles
   * must occur before a reading is taken
   * 
   * Affects all reading of pins setting to 0 sets it off
   * 
   * @param delay
   */
  public void setDebounce(int delay) {
    if (delay < 0 || delay > 32767) {
      error(String.format("%d debounce delay must be 0 < delay < 32767", delay));
    }
    int lsb = delay & 0xff;
    int msb = (delay >> 8) & 0xff;
    sendMsg(SET_DEBOUNCE, msb, lsb);

  }

  public void setDigitalTriggerOnly(Boolean b) {
    if (!b)
      sendMsg(SET_DIGITAL_TRIGGER_ONLY, FALSE);
    else
      sendMsg(SET_DIGITAL_TRIGGER_ONLY, TRUE);

  }

  public boolean setLoadTimingEnabled(boolean enable) {
    log.info(String.format("setLoadTimingEnabled %b", enable));

    if (enable) {
      sendMsg(SET_LOAD_TIMING_ENABLED, TRUE);
    } else {
      sendMsg(SET_LOAD_TIMING_ENABLED, FALSE);
    }

    return enable;
  }

  public void setPWMFrequency(Integer address, Integer freq) {

    int prescalarValue = 0;

    switch (freq) {
      case 31:
      case 62:
        prescalarValue = 0x05;
        break;
      case 125:
      case 250:
        prescalarValue = 0x04;
        break;
      case 500:
      case 1000:
        prescalarValue = 0x03;
        break;
      case 4000:
      case 8000:
        prescalarValue = 0x02;
        break;
      case 32000:
      case 64000:
        prescalarValue = 0x01;
        break;
      default:
        prescalarValue = 0x03;
    }

    sendMsg(SET_PWMFREQUENCY, address, prescalarValue);
  }

  /**
   * this sets the sample rate of polling reads both digital and analog it is a
   * loop count modulus - default is 1 which seems to be a bit high of a rate to
   * be broadcasting across the internet to several webclients :) valid ranges
   * are 1 to 32,767 (for Arduino's 2 byte signed integer)
   * 
   * @param rate
   */
  public int setSampleRate(int rate) {
    if (rate < 1 || rate > 32767) {
      error(String.format("%d sample rate can not be < 1", rate));
    }
    int lsb = rate & 0xff;
    int msb = (rate >> 8) & 0xff;
    sendMsg(SET_SAMPLE_RATE, msb, lsb);

    return rate;
  }

  public void setSerialRate(int rate) {
    sendMsg(SET_SERIAL_RATE, rate);
  }

  @Override
  public boolean servoEventsEnabled(Servo servo) {
    log.info(String.format("setServoEventsEnabled %s %b", servo.getName(), servo.isEventsEnabled));
    int index = getServoIndex(servo.getPin());
    if (servo.isEventsEnabled) {
      sendMsg(SERVO_EVENTS_ENABLED, index, TRUE);
    } else {
      sendMsg(SERVO_EVENTS_ENABLED, index, FALSE);
    }

    return true;

  }

  @Override
  public void setServoSpeed(Servo servo) {
    Double speed = servo.speed;
    if (speed == null || speed < 0.0f || speed > 1.0f) {
      error("speed %f out of bounds", speed);
      return;
    }

    int index = getServoIndex(servo.getPin());
    sendMsg(SET_SERVO_SPEED, index, (int) (speed * 100));
  }

  public void setSketch(Sketch sketch) {
    this.sketch = sketch;
    broadcastState();
  }

  /**
   * set a pin trigger where a value will be sampled and an event will be signal
   * when the pin turns into a different state.
   * 
   * @param pin
   * @param value
   * @return
   */
  public int setTrigger(int pin, int value) {
    return setTrigger(pin, value, 1);
  }

  /**
   * set a pin trigger where a value will be sampled and an event will be signal
   * when the pin turns into a different state.
   * 
   * @param pin
   * @param value
   * @param type
   * @return
   */
  public int setTrigger(int pin, int value, int type) {
    sendMsg(SET_TRIGGER, pin, type);
    return pin;
  }

  /**
   * send a reset to Arduino - all polling is stopped and all other counters are
   * reset
   * 
   * TODO - reset servos ? motors ? etc. ?
   */
  public void softReset() {
    sendMsg(ArduinoMsgCodec.SOFT_RESET, 0, 0);
  }

  @Override
  public void startService() {
    super.startService();
    try {
      serial = (Serial) startPeer("serial");
      // FIXME - dynamically additive - if codec key has never been used -
      // add key
      // serial.getOutbox().setBlocking(true);
      // inbox.setBlocking(true);
      serial.setCodec("arduino");
      serial.addByteListener(this);
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Object publishSensorData(Object data) {
    return data;
  }

  @Override
  public void motorReset(MotorControl motor) {
    // perhaps this should be in the motor control
    // motor.reset();
    // opportunity to reset variables on the controller
    // sendMsg(MOTOR_RESET, motor.getind);
  }

  @Override
  public void stopService() {
    super.stopService();
    disconnect();
  }

  @Override
  public void update(Object data) {
    invoke("publishPin", data);
  }

  @Override
  public int getDataSinkType() {
    return DATA_SINK_TYPE_PIN;
  }

  @Override
  public int getSensorType() {
    return SENSOR_TYPE_PIN;
  }

  @Override
  public int[] getSensorConfig() {
    // is a Pin sensor
    return new int[] {};
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

    ServiceType meta = new ServiceType(Arduino.class.getCanonicalName());
    meta.addDescription("This service interfaces with an Arduino micro-controller");
    meta.addCategory("microcontroller");
    meta.addPeer("serial", "Serial", "serial device for this Arduino");
    meta.addPeer("virtual", "VirtualDevice", "used to create virtual arduino");
    return meta;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.getInstance().configure();
      LoggingFactory.getInstance().setLevel(Level.INFO);

      //
      Arduino arduino = (Arduino) Runtime.createAndStart("arduino", "Arduino");
      //Serial serial = (Serial) arduino.getSerial();

      arduino.connect("COM30");

      // digitial PWM pins for hbrdige/motor control.
      int leftPwm = 6;
      int rightPwm = 7;
      // analog feedback pin A0 on the Uno
      int potPin = 14;
      boolean testMotor = true;
      boolean startAnalogPolling = true;

      if (startAnalogPolling) {
        arduino.analogReadPollingStart(potPin);
      }
      
      if (testMotor) {
        Motor motor = (Motor)Runtime.createAndStart("motor", "Motor");
        motor.setType2Pwm(leftPwm, rightPwm);
        // motor.attach(arduino);
        arduino.motorAttach(motor);
        while (true) {
          // try to overrun?
          // rand between -1 and 1.
          double rand = (Math.random() - 0.5)*2;
          motor.move(rand);
        }
      } else {
        Servo servo = (Servo)Runtime.createAndStart("servo", "Servo");
        servo.setPin(13);
        arduino.servoAttach(servo);
        servo.attach();
        int angle = 0;
        int max = 5000;
        while (true) {
          // System.out.println(angle);
          angle++;
          servo.moveTo(angle %180);
          if (angle > max) {
            break;
          }
        }
        System.out.println("done with loop..");;

      }

      System.in.read();
      //serial.connectTcp("192.168.0.99", 80);
      // arduino.setLoadTimingEnabled(true);
      //Runtime.start("python", "Python");
      //Runtime.start("webgui", "WebGui");
      // Runtime.start("servo", "Servo");
      // Runtime.start("clock", "Clock");
      // Runtime.start("serial", "Serial");
      // Arduino.createVirtual("COM9");
      // arduino.connect("COM18");

      /*
       * arduino.setLoadTimingEnabled(true); long ts =
       * System.currentTimeMillis();
       * 
       * for (int i = 0; i < 10000; ++i){
       * arduino.sendMsg(ArduinoMsgCodec.GET_VERSION); // log.info("{}", i); }
       * 
       * log.error("time {} ms", System.currentTimeMillis() - ts );
       * 
       * for (int i = 0; i < 10000; ++i){
       * arduino.sendMsg(ArduinoMsgCodec.GET_VERSION); log.info("{}", i); }
       */

      // arduino.broadcastState();

      // arduino.createVirtual("COM77");
      // arduino.createVirtual("COM18");
      // arduino.setBoardUno();
      // arduino.connect("COM18");
      // Runtime.start("webgui", "WebGui");
      // Runtime.start("gui", "GUIService");
      // Runtime.start("webgui", "WebGui");

      // arduino.analogReadPollingStart(14);
      // Runtime.start("gui", "GUIService");
      // Runtime.start("python", "Python");
      // arduino.connect("COM18");
      /*
       * 
       * 
       * arduino.getVersion(); Servo servo = (Servo) Runtime.start("servo",
       * "Servo"); servo.attach(arduino, 10);
       * 
       * servo.moveTo(10); servo.moveTo(90); servo.moveTo(180);
       * servo.moveTo(90); servo.moveTo(10);
       */

      /*
       * VirtualDevice virtual = (VirtualDevice) Runtime.start("virtual",
       * "VirtualDevice"); virtual.createVirtualArduino("vport"); Python logic =
       * virtual.getLogic();
       */

      // catcher.subscribe(arduino.getName(), "publishError");

      // Serial uart = virtual.getUART();

      /*
       * VirtualDevice virtual = (VirtualDevice) Runtime.start("virtual",
       * "VirtualDevice"); virtual.createVirtualArduino("vport");
       * arduino.connect("vport");
       */

      // Runtime.start("serial", "Serial");

      // arduino.setBoardMega();
      // arduino.connect("COM15");
      // Runtime.start("python", "Python");
      // Runtime.start("raspi", "Runtime");
      // Runtime.start("raspi","Runtime");
      // RemoteAdapter remote = (RemoteAdapter)Runtime.start("rasremote",
      // "RemoteAdapter");
      // remote.setDefaultPrefix("mac-");
      // remote.setDefaultPrefix("");
      // Runtime.start("gui", "GUIService");
      // remote.startListening();
      // Runtime.start("cli", "Cli");
      // Runtime.start("servo", "Servo");
      // Runtime.start("gui", "GUIService");
      // Runtime.start("python", "Python");
      // Runtime.broadcastStates();

      /*
       * WebGui webgui = (WebGui)Runtime.create("webgui", "WebGui");
       * webgui.setPort(8989); webgui.startService();
       */

      // arduino.analogReadPollingStart(68);

      /*
       * Serial serial = arduino.getSerial(); serial.connectTCP("localhost",
       * 9191); arduino.connect(serial.getPortName());
       * 
       * 
       * arduino.digitalWrite(13, 0); arduino.digitalWrite(13, 1);
       * arduino.digitalWrite(13, 0);
       * 
       * arduino.analogReadPollingStart(15);
       * 
       * // arduino.test("COM15");
       * 
       * arduino.setSampleRate(500); arduino.setSampleRate(1000);
       * arduino.setSampleRate(5000); arduino.setSampleRate(10000);
       * 
       * arduino.analogReadPollingStop(15);
       */

      log.info("here");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @Override
  public void motorAttach(MotorControl motor, int portNumber) {
    log.warn("Motor attach not implemented. use motorAttach(motorControl) instead");
  }

  @Override
  public void motorAttach(String name, int portNumber) {
    log.warn("Motor attach not implemented. use motorAttach(motorControl) instead");
  }

	@Override
	public void createDevice(int busAddress, int deviceAddress, String type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void releaseDevice(int busAddress, int deviceAddress) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void i2cWrite(int busAddress, int deviceAddress, byte[] buffer, int size) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int i2cRead(int busAddress, int deviceAddress, byte[] buffer, int size) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int i2cWriteRead(int busAddress, int deviceAddress, byte[] writeBuffer, int writeSize, byte[] readBuffer, int readSize) {
		// TODO Auto-generated method stub
		return 0;
	}

}
