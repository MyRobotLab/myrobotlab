package org.myrobotlab.service;

import static org.myrobotlab.codec.serial.ArduinoMsgCodec.MAX_MSG_SIZE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.MAGIC_NUMBER;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.MRLCOMM_VERSION;

	///// java static import definition - DO NOT MODIFY - Begin //////
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_MRLCOMM_ERROR;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.GET_VERSION;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_VERSION;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.ADD_SENSOR_DATA_LISTENER;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.ANALOG_READ_POLLING_START;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.ANALOG_READ_POLLING_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.ANALOG_WRITE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.ATTACH_DEVICE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.CREATE_I2C_DEVICE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DETACH_DEVICE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DIGITAL_READ_POLLING_START;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DIGITAL_READ_POLLING_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DIGITAL_WRITE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.FIX_PIN_OFFSET;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.GET_BOARD_INFO;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.I2C_READ;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.I2C_WRITE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.I2C_WRITE_READ;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.INTS_TO_STRING;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.MOTOR_MOVE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.MOTOR_MOVE_TO;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.MOTOR_RESET;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.MOTOR_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PIN_MODE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_ATTACHED_DEVICE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_DEBUG;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_MESSAGE_ACK;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_PIN;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_PULSE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_PULSE_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_SENSOR_DATA;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_SERVO_EVENT;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_STATUS;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_TRIGGER;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PULSE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PULSE_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.RELEASE_I2C_DEVICE;
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
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_DEBUG;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_DIGITAL_TRIGGER_ONLY;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_LOAD_TIMING_ENABLED;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_PWMFREQUENCY;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_SAMPLE_RATE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_SERIAL_RATE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_SERVO_SPEED;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_TRIGGER;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SOFT_RESET;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.myrobotlab.codec.serial.ArduinoMsgCodec;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.DeviceMapping;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.data.SensorData;
import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.Microcontroller;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SensorDataListener;
import org.myrobotlab.service.interfaces.SensorDataPublisher;
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

public class Arduino extends Service implements Microcontroller, I2CControl, SerialDataListener, ServoController, MotorController, SensorDataPublisher, DeviceController {

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
	
	/**
	 * 
	 *
	 */
	public static class MrlCommStatus {
		public Long us;
		public Integer freeMemory;
		
		public MrlCommStatus(Long us, Integer freeMemory){
			this.us = us;
			this.freeMemory = freeMemory;
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
	public static final int ARDUINO_TYPE_INT            = 16;

	// servo event types
	public static final int SERVO_EVENT_STOPPED         = 1;

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
	 * FIXME ! - these processor types ! - something we are not interested in
	 * and do not have to deal with - we are far more interested in
	 * NUM_DIGITAL_PINS and "board pin layouts" -
	 * 
	 * As far as I can tell board types are in variants 1.0.5 Arduino IDE
	 * includes
	 * 
	 * This is the best reference I have found regarding actual pin capabilities
	 * https://learn.sparkfun.com/tutorials/arduino-comparison-guide#totally-
	 * tabular Uno & Duemilanove have 14 digital pins (6 PWM) & 6 analog - total
	 * 20 Mini & Pro have 14 digital pins (8 PWM) & 6 analog - total 20
	 * 
	 * ATmega328 Boards 32kB Program Space // 1 UART // 6 PWM // 4-8 Analog
	 * Inputs // 9-14 Digital I/O ATmega2560 Arduino Mega's 256kB Program Space
	 * // 4 UARTs // 14 PWM // 16 Analog Inputs // 54 Digital I/O -
	 * 
	 * So at the moment .. there is only Uno & Mega !!!
	 * 
	 */
	public transient static final String BOARD_TYPE_UNO = "Uno";
	public transient static final String BOARD_TYPE_MEGA = "Mega";
	public transient static final int BOARD_TYPE_ID_UNKNOWN = 0;
	public transient static final int BOARD_TYPE_ID_MEGA = 1;
	public transient static final int BOARD_TYPE_ID_UNO = 2;
	//temporary
	public final static int GET_BOARD_INFO = 70;
	public final static int PUBLISH_BOARD_INFO = 71;
	/**
	 * board type - UNO Mega etc..
	 */
	public String board;

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
	 * Devices - string name index of device we need 2 indexes for sensors because they
	 * will be referenced by name OR by index
	 */
	HashMap<String, DeviceMapping> deviceList = new HashMap<String, DeviceMapping>();

	/**
	 * id reference of sensor, key is the MRLComm device id
	 */
	HashMap<Integer, DeviceMapping> deviceIndex = new HashMap<Integer, DeviceMapping>();

	/**
	 * Serial service - the Arduino's serial connection
	 */
	Serial serial;

	int error_arduino_to_mrl_rx_cnt;
	int error_mrl_to_arduino_rx_cnt;

	int byteCount;

	int msgSize;

	transient int[] msg = new int[MAX_MSG_SIZE];
	
  // i2c This needs to be volatile because it can be updated in a different thread  
	volatile boolean i2cDataReturned = false;

	// parameters for testing the getVersion retry stuff.
	// TODO: some way to do this more synchronously
	// perhaps when we connect to the serial port, MRLComm can just have the
	// version waiting?
	public int retryMax = 3;
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
		// I don't like this..
		// sensorAttach(this);
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

		// TODO: the pin should be the pin number on the arduino.
		// instead if it's an analog pin, we need to subtract some number
		// depending if it's
		// an uno or a mega.. etc.. very very bad.
		// int actualPin = fixPinOffset(pin);
		// create an analog pin sensor and attach it to the arduino controller.
		// AnalogPinSensor s = new AnalogPinSensor(actualPin, sampleRate);
		// sensorAttach(s); - no longer applicable - sensor attach was done with
		// the PinArray !
		// send read polling start! (but actually. attaching the sensor should
		// probably already do this.
		// sendMsg(ANALOG_READ_POLLING_START, actualPin, (sampleRate >> 8) &
		// 0xff, sampleRate & 0xff);
	}

	private int fixPinOffset(Integer pin) {
		int actualPin = 0;
		if (board.toLowerCase().contains("mega")) {
			actualPin = pin - 53;
		} else {
			actualPin = pin - 14;
		}
		return actualPin;
	}

	public void analogReadPollingStart(int pin) {
		analogReadPollingStart(pin, 1);
	}

	/**
	 * stop the selected pin from polling analog reads
	 * 
	 * @param pin
	 */
	public void analogReadPollingStop(int pin) {
		int actualPin = fixPinOffset(pin);
		// TODO: look up which sensor it is. and tell tell the sensor to stop
		// instead.
		// that should call a sensor.stop on the controller maybe?!
		sendMsg(ANALOG_READ_POLLING_STOP, actualPin);
	}

	public void analogWrite(int address, int value) {
		log.info(String.format("analogWrite(%d,%d) to %s", address, value, serial.getName()));
		// FIXME
		// if (pin.mode == INPUT) {sendMsg(PIN_MODE, OUTPUT)}
		sendMsg(ANALOG_WRITE, address, value);
	}

	public void connect(String port) throws IOException {
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
	public void connect(String port, int rate, int databits, int stopbits, int parity) throws IOException {

		// FIXME ! <<<-- REMOVE ,this) - patterns should be to add listener on
		// startService
		// return connect(port, 57600, 8, 1, 0); <- put this back ?
		// return serial.connect(port); // <<<-- REMOVE ,this) - patterns
		// should be to add listener on
		// startService
		serial.open(port, rate, databits, stopbits, parity);
		
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
	 * static public VirtualDevice createVirtual(String port) throws
	 * IOException{ // Once device to rule them all ? - I think that would
	 * work.. VirtualDevice virtual = (VirtualDevice) Runtime.start("virtual",
	 * "VirtualDevice"); // this call would generate the instance of virtual
	 * device needed virtual.createVirtualArduino(port); return virtual; }
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
	public void digitalReadPollingStart(int pin) throws IOException {
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
	public void digitalReadPollingStop(int pin) {
		sendMsg(DIGITAL_READ_POLLING_STOP, pin);
	}

	public void digitalWrite(int address, int value) {
		info("digitalWrite (%d,%d) to %s", address, value, serial.getName());
		sendMsg(DIGITAL_WRITE, address, value);
		pinList.get(address).value = value;
	}

	public void disconnect() {
		serial.disconnect();
	}
	
	public void getBoardInfo(){
	  sendMsg(GET_BOARD_INFO);
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
			 * We will try up to retryMax times to get a version out of
			 * MRLComm.c and wait 333 ms between each try. A blocking queue is
			 * not needed, as this is only a single data element - and blocking
			 * is not necessary. mrlCommVersion will be set by our port listener
			 * in PUBLISH_VERSION if the result comes back.
			 */
			while ((retry < retryMax) && (mrlCommVersion == null)) {
				// versionQueue.clear();
				sendMsg(GET_VERSION);
				// mrlCommVersion = versionQueue.poll(1000,
				// TimeUnit.MILLISECONDS);
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

	/**
	 * FIXME DEPRECATED - / - REMOVE - handle in attach(Device)
	 */
	/*
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
			// sensorAttach(motor); TODO attachDevice
		}

		motor.setController(this);
		motor.broadcastState();
	}
	*/

	// ================= new interface end =========================


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

			// TODO FIXME
			// sendMsg(PULSE, deviceList.get(motor.getName()).id, b3, b2, b1,
			// b0, (int) motor.getPowerLevel(), feedbackRate);
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
			/**
			 * Archtype InputStream read - rxtxLib does not have this
			 * straightforward design, but the details of how it behaves is is
			 * handled in the Serial service and we are given a unified
			 * interface
			 * 
			 * The "read()" is data taken from a blocking queue in the Serial
			 * service. If we want to support blocking functions in Arduino then
			 * we'll "publish" to our local queues
			 */
			// TODO: consider reading more than 1 byte at a time ,and make this
			// callback onBytes or something like that.

			++byteCount;
			if (log.isDebugEnabled()) {
				log.info("onByte {} \tbyteCount \t{}", newByte, byteCount);
			}
			if (byteCount == 1) {
				if (newByte != MAGIC_NUMBER) {
					byteCount = 0;
					msgSize = 0;
					Arrays.fill(msg, MAGIC_NUMBER);
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
			} else {
				// the case where byteCount is negative?! not got.
				error(String.format("Arduino->MRL error %d rx negsz errors", ++error_arduino_to_mrl_rx_cnt));
				return newByte;
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
			log.warn("msg_structure violation byteCount {} buffer {}", byteCount, Arrays.copyOf(msg, byteCount));
			// try again (clean up memory buffer)
			msgSize = 0;
			byteCount = 0;
			Logging.logError(e);
		}
		return newByte;
	}

	static String intsToString(int[] ints) {
		return intsToString(ints, 0, ints.length);
	}

	static String intsToString(int[] ints, int begin) {
		return intsToString(ints, begin, ints.length - begin);
	}

	static String intsToString(int[] ints, int begin, int length) {
		byte[] b = new byte[length];
		for (int i = 0; i < length; ++i) {
			b[i] = (byte) ints[begin + i];
		}
		return new String(b);
	}

	// This is called when a valid message is received from the serial port.
	private void processMessage(int[] message) {

		// MSG CONTENTS = FN | D0 | D1 | ...
		int function = message[0];
		// log.info("Process Message Called: {}",
		// ArduinoMsgCodec.functionToString(function));
		// if (log.isDebugEnabled()) {
		// log.debug("Process Message Called: {}",
		// ArduinoMsgCodec.functionToString(function));
		// }
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
		case PUBLISH_STATUS: {
			long microsPerLoop = Serial.bytesToInt(message, 1, 4);
			int freeMemory = (int)Serial.bytesToInt(message, 5, 2);
			info("load %d us - memory %d bytes", microsPerLoop, freeMemory);
			invoke("publishStatus", new MrlCommStatus(microsPerLoop, freeMemory));
			break;
		}
		
		// TODO - REMOVE - this needs to be routed through
		// PUBLISH_SENSOR_DATA and processed in Servo service !
		case PUBLISH_SERVO_EVENT: {
			int id = message[1];
			int eventType = message[2];
			int currentPos = message[3];
			int targetPos = message[4];
			log.info(String.format(" id %d type %d cur %d target %d", id, eventType, currentPos & 0xff, targetPos & 0xff));
			// uber good -
			// TODO - deprecate ServoControl interface - not
			// needed Servo is abstraction enough
			Servo servo = (Servo) deviceList.get(id).getDevice();
			servo.invoke("publishServoEvent", currentPos & 0xff);
			break;
		}

		/**
		 * PUBLISH_DEVICE_ATTACHED - is the callback from MRLComm to bind a
		 * service with its id
		 * 
		 * MSG STRUCTURE
		 *    0							1					2			3+
		 * PUBLISH_ATTACHED_DEVICE | NEW_DEVICE_INDEX | NAME_STR_SIZE | NAME
		 * 
		 */
		case PUBLISH_ATTACHED_DEVICE: {
			
			int newDeviceIndex = message[1];
			int nameStrSize = message[2];
			String deviceName = intsToString(message, 3, nameStrSize);
			
			if (!deviceList.containsKey(deviceName)){
				error("PUBLISH_ATTACHED_DEVICE deviceName %s not found !", deviceName);
				break;
			}
			
			DeviceMapping deviceMapping = deviceList.get(deviceName);			
			deviceIndex.put(newDeviceIndex, deviceList.get(deviceName));
			
			invoke("publishAttachedDevice", deviceMapping.getDevice());

			info("connected service %s with mrlcomm device %d", deviceName, newDeviceIndex);
			
			break;
		}

		case PUBLISH_SENSOR_DATA: {
			// PUBLISH_ATTACHED_SENSOR | DEVICE_INDEX | DATA_SIZE | DATA ....

			int id = message[1];

			// get the size of the data payload
			int size = (int) message[2];
			if (size > message.length - 2) {
				error("PUBLISH_SENSOR_DATA invalid size %d", size);
				break;
			}

			// get the sensor
			SensorDataListener sensor = (SensorDataListener) deviceIndex.get(id);

			// unload the data
			SensorData sensorData = new SensorData(new int[size]);
			for (int i = 0; i < size; ++i) {
				sensorData.data[i] = message[2 + 1];
			}

			// try to callback with the new data
			// if our listener has registered directly
			// then we can optimize - because its in process
			sensor.update(sensorData); // TODO !! IMPORTANT if this is pinData
										// then we update our pins

			// potentially, there could be a } else { here
			// which would switch between optimized .update and regular(network
			// capable) publishSensorData
			// we will attempt to do both at the moment .. and see how the
			// consumers like that

			invoke("publishSensorData", sensorData);

			break;
		}

		case PUBLISH_PULSE_STOP: {
			int id = (int) message[1];
			// FIXME - assumption its a encoder pin on a Motor NO !!!
			// SensorDataPublisher sensor = deviceIndex.get(id).sensor;
			// Integer data = Serial.bytesToInt(message, 2, 4);
			// sensor.update(data);
			break;
		}

		case PUBLISH_MESSAGE_ACK: {
			log.info("Message Ack received: {}", ArduinoMsgCodec.functionToString(message[1]));
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
				payload.append((char) message[i]);
			}
			log.info("MRLComm Debug Message {}", payload);
			break;
		}
    case PUBLISH_BOARD_INFO:{
      int boardId=message[1];
      String boardName="";
      switch(boardId){
        case BOARD_TYPE_ID_MEGA:
          boardName=BOARD_TYPE_MEGA;
          break;
        case BOARD_TYPE_ID_UNO:
          boardName=BOARD_TYPE_UNO;
          break;
        default:
          boardName="Unknown";
          break;
      }
      log.info("Board type returned by Arduino: {}",boardName);
      log.info("Board type currently set: {}",board);
      if(board=="" && boardId != BOARD_TYPE_ID_UNKNOWN){
        setBoard(boardName);
        log.info("Board type set to: {}",board);
      }
      else{
        log.info("No change in board type");
      }
      break;
    }
		default: {
			// FIXME - use formatter for message
			error("unknown serial event %d", function);
			break;
		}
		} // end switch
	}
	
	/**
	 * when a device becomes attached to MRLComm
	 * 
	 * @param device
	 * @return
	 */
	public DeviceControl publishAttachedDevice(DeviceControl device){
		return device;
	}

	public void publishMessageAck() {
	}

	public String publishDebug(String msg) {
		return msg;
	}

	@Override
	public String onConnect(String portName) {
		info("%s connected to %s", getName(), portName);
		// Get version should already have been called. don't call it again!
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

	// ----------- motor controller api end ----------------

	public MrlCommStatus publishStatus(Long us, Integer freeMemory) {
		log.info(String.format("publishStatus - %d %d", us, freeMemory));
		MrlCommStatus ret = new MrlCommStatus(us, freeMemory);
		ret.us = us;
		ret.freeMemory = freeMemory;
		return ret;
	}

	public Integer publishMRLCommError(Integer code) {
		return code;
	}

	/**
	 * This method is called with Pin data whene a pin value is changed on the
	 * Arduino board the Arduino must be told to poll the desired pin(s). This
	 * is done with a analogReadPollingStart(pin) or digitalReadPollingStart()
	 */

	public Pin publishPin(Pin p) {
		// TODO: this is being replaced with PublishSensorData !
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
	
	public synchronized void sendMsg(int function, List<Integer> params) {
		int[] p = new int[params.size()];
		for (int i = 0; i < params.size(); ++i){
			p[i] = params.get(i);
		}
		
		sendMsg(function, p);
	}

	/**
	 * MRL protocol method
	 * 
	 * @param function
	 * @param param1
	 * @param param2
	 * 
	 *            MAGIC_NUMBER|LENGTH|FUNCTION|PARAM0|PARAM1 ... |PARAM(N)
	 * 
	 */
	public synchronized void sendMsg(int function, int... params) {
		// log.info("Sending Message : {}",
		// ArduinoMsgCodec.functionToString(function));
		// if (log.isDebugEnabled()) {
		// log.debug("Sending Arduino message funciton {}",
		// ArduinoMsgCodec.functionToString(function));
		// }
		// some sanity checking.
		if (!serial.isConnected()) {
			log.warn("Serial port is not connected, unable to send message.");
			return;
		}
		// don't even attempt to send it if we know it's a bogus message.
		// TODO: we need to account for the magic byte & length bytes. max
		// message size is 64-2 (potentially)
		if (params.length > MAX_MSG_SIZE) {
			log.error("Arduino Message size was large! Function {} Size {}", function, params.length);
			return;
		}
		// System.out.println("Sending Message " + function );
		try {
			// Minimum MRLComm message is 3 bytes(int).
			// MAGIC_NUMBER|LENGTH|FUNCTION|PARAM0|PARAM1 would be valid
			int[] msgToSend = new int[3 + params.length];
			msgToSend[0] = MAGIC_NUMBER;
			msgToSend[1] = 1 + params.length;
			msgToSend[2] = function;
			for (int i = 0; i < params.length; i++) {
				// What if the int is > 127 ?
				msgToSend[3 + i] = params[i];
			}
			// send the message as an array. (serial port actually writes 1 byte
			// at a time anyway.. oh well.)

			// set a flag that a message is in flight.
			ackRecieved = false;
			// notify();

			// Technically this is the only thing that needs to be synchronized
			// i think.
			synchronized (msgToSend) {
				serial.write(msgToSend);
			}
			// TODO: wait for an ack of the message to arrive!
			long start = System.currentTimeMillis();
			// wait a max of 100 for the ack.
			int limit = 2000;
			// log.info("Waiting on ack. {}", function);
			while (!ackRecieved) {
				// TODO: Avoid excessive cpu usage, and limit the amount of time
				// we wait on this "ackRecieved"
				// variable. There's some java thread/notify stuff that might be
				// better to use?
				// sleep 1 nanosecond?!
				// Thread.sleep(0,1);
				// sleep 1 millisecond
				Thread.sleep(1);
				// log.info("Waiting on ack {}", numAck);
				long delta = System.currentTimeMillis() - start;
				// timeout waiting for the ack.
				if (delta > limit) {
					// log.warn(, limit, function);
					// warn(String.format("No ack received.. timing out after %d
					// ms and continuing for function %s", limit,
					// ArduinoMsgCodec.functionToString(function)));
					// ackRecieved = true;
					// Try again!
					// sendMsg(function, params);
					// eek?
					// ackRecieved = false;
					break;
				}
				// break;
			}

			// if (log.isDebugEnabled()) {
			if (!ackRecieved) {
				log.info("Ack not received : {} {}", ArduinoMsgCodec.functionToString(function), numAck);
			}
			// }
			// putting delay at the end so we give the message and allow the
			// arduino to process
			// this decreases the latency between when mrl sends the message
			// and the message is picked up by the arduino.
			// This helps avoid the arduino dropping messages and getting
			// lost/disconnected.
			if (delay > 0) {
				Thread.sleep(delay);
			}
		} catch (Exception e) {
			error("sendMsg " + e.getMessage());
		}

	}

	/**
	 * attachDevice is the core of attaching peripheries to a micro controller
	 * 
	 * attachDevice sends a message within a message (passthrough) so that the
	 * microcontroller code can initialize and create a new Device which is
	 * applicable for the service requesting.
	 * 
	 * Arduino will attach itself this way too as a Analog & Digital Pin array
	 * 
	 * Comment from Mats: Arduino should probably attach itself as an DEVICE_TYPE_I2C 
	 * the first to createI2cDevice is invoked. This service probably needs to keep track of
	 * the differnt devices connected on the i2c bus, but not MRLComm.
	 * 
	 * the micro controller message format for ATTACH_DEVICE will be:
	 * 
	 * MAGIC_NUMBER|LENGTH|FUNCTION|PARAM0|PARAM1
	 * 
	 * MAGIC_NUMBER|LENGTH|ATTACH_DEVICE|DEVICE_TYPE|NAME_SIZE|NAME .... (N)|CONFIG_SIZE|DATA0|DATA1 ...|DATA(N)
	 * 
	 * ATTACH_DEVICE - this method id
	 * DEVICE_TYPE - the mrlcomm device type we are attaching
	 * NAME_SIZE - the size of the name of the service of the device we are attaching 
	 * NAME .... (N) - the name data
	 * CONFIG_SIZE - the size of the folloing config
	 * DATA0|DATA1 ...|DATA(N) - config data
	 * 
	 * @param device
	 */
	@Override
	public synchronized void attachDevice(DeviceControl device, Object... config) {

		int deviceType = device.getDeviceType();
		String name = device.getName();
		int nameSize = name.length();
		
		// FIXME - total length test - if name overruns - throw

		info("attaching device %s of type %d", name, deviceType);

		int deviceConfigSize = 0;
		if (config != null) {
			deviceConfigSize = config.length;
		}

		// ArrayList<Integer> msgParms = new int[deviceConfigSize + 2];
		List<Integer> msgBody = new ArrayList<Integer>();
		// ATTACH_DEVICE|DEVICE_TYPE|NAME_SIZE|NAME .... (N)|CONFIG_MSG_SIZE|DATA0|DATA1 ...|DATA(N)

		// create msg payload for the specific device in MRLComm
		msgBody.add(deviceType); 		// DEVICE_TYPE
		
		msgBody.add(nameSize); // NAME_SIZE
		for (int i = 0; i < nameSize; ++i) {
			msgBody.add((int)name.charAt(i));
		}
		
		msgBody.add(deviceConfigSize); 	// CONFIG_MSG_SIZE

		// move the device config into the msg
		// DATA0|DATA1 ...|DATA(N)
		if (config != null) {
			for (int i = 0; i < config.length; ++i) {
				msgBody.add((int)config[i]);
			}
		}
		
		
		// we put the device on the name lst - this allows
		// references to work from
		// Java-land Service -----name---> deviceList
		// Java-land Service <----name---- deviceList
		deviceList.put(device.getName(), new DeviceMapping(device, config));

		// to allow full duplex communication we need the device id
		// from MRLComm to the appropriate service (identified by name)
		// so we send the service name to MRLComm and it echos back the name
		// and a deviceIndex if it was successfully attached
		//
		// deviceList ------id----> MRLComm
		// deviceList <------id---- MRLComm
		//
		// only then is there full duplex connectivity
		//
		// Java-land Service -----name---> deviceList ------id----> MRLComm
		// Java-land Service <----name---- deviceList <-----id----- MRLComm
		//
		// The Java-land Service owns the name, the MRLComm owns the id and
		// Arduino owns
		// the mapping of the two.

 		sendMsg(ATTACH_DEVICE, msgBody);

	}
	
	Integer getDeviceId(DeviceControl device){
		return getDeviceId(device.getName());
	}
	
	Integer getDeviceId(String name){	
		if (deviceList.containsKey(name)){
			return deviceList.get(name).getId();
		}
		log.error("getDeviceId could not find device {}", name);
		return null;
	}

	/**
	 * sensorPollingStart begins general device read "polling". It puts the
	 * device in a reading state. Its not really applicable to poll a Pin this
	 * way as it refers to the "Device" level not the Pin.
	 * 
	 * Putting a PinArray device into reading state might be a global setting to
	 * turn on all polling pins
	 * 
	 * @param deviceIndex
	 */
	public void sensorPollingStart(String name) {
		sendMsg(SENSOR_POLLING_START, getDeviceId(name));
		}

	/**
	 * Stops the "Device" from polling - puts the device into a non-reading
	 * state
	 * 
	 * @param deviceIndex
	 */

	public void sensorPollingStop(String name) {
		sendMsg(SENSOR_POLLING_STOP, getDeviceId(name));
	}

	// FIXME - do sweep single method call from ServoControl
	@Override
	public void servoSweepStart(Servo servo) {
		int id = getDeviceId(servo);
		log.info(String.format("servoSweep %s id %d min %d max %d step %d", servo.getName(), id, servo.sweepMin, servo.sweepMax, servo.sweepStep));
		sendMsg(SERVO_SWEEP_START, id, servo.sweepMin, servo.sweepMax, servo.sweepStep);
	}

	@Override
	public void servoSweepStop(Servo servo) {
		sendMsg(SERVO_SWEEP_STOP, getDeviceId(servo));
	}

	@Override
	public void servoWrite(Servo servo) {
		int id = getDeviceId(servo);
		log.info("servoWrite {} {} id {}", servo.getName(), servo.targetOutput, id);
		sendMsg(SERVO_WRITE, id, servo.targetOutput.intValue());
	}

	@Override
	public void servoWriteMicroseconds(Servo servo) {
		int id = getDeviceId(servo);
		log.info(String.format("writeMicroseconds %s %d id %d", servo.getName(), servo.uS, id));
		sendMsg(SERVO_WRITE_MICROSECONDS, id, servo.uS);
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
	 * Debounce ensures that only a single signal will be acted upon for a
	 * single opening or closing of a contact. the delay is the min number of pc
	 * cycles must occur before a reading is taken
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
	 * this sets the sample rate of polling reads both digital and analog it is
	 * a loop count modulus - default is 1 which seems to be a bit high of a
	 * rate to be broadcasting across the internet to several webclients :)
	 * valid ranges are 1 to 32,767 (for Arduino's 2 byte signed integer)
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
	public boolean servoEventsEnabled(Servo servo, boolean enabled) {
		log.info(String.format("setServoEventsEnabled %s %b", servo.getName(), servo.isEventsEnabled));
		int id = getDeviceId(servo);
		if (enabled) {
			sendMsg(SERVO_EVENTS_ENABLED, id, TRUE);
		} else {
			sendMsg(SERVO_EVENTS_ENABLED, id, FALSE);
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

		int id = getDeviceId(servo);
		sendMsg(SET_SERVO_SPEED, id, (int) (speed * 100));
	}

	public void setSketch(Sketch sketch) {
		this.sketch = sketch;
		broadcastState();
	}

	/**
	 * set a pin trigger where a value will be sampled and an event will be
	 * signal when the pin turns into a different state.
	 * 
	 * @param pin
	 * @param value
	 * @return
	 */
	public int setTrigger(int pin, int value) {
		return setTrigger(pin, value, 1);
	}

	/**
	 * set a pin trigger where a value will be sampled and an event will be
	 * signal when the pin turns into a different state.
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
	 * send a reset to Arduino - all polling is stopped and all other counters
	 * are reset
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

	// @Override
	// public void update(Object data) {
	// invoke("publishPin", data);
	// }

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
			// Serial serial = (Serial) arduino.getSerial();

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
				Motor motor = (Motor) Runtime.createAndStart("motor", "Motor");
				motor.setType2Pwm(leftPwm, rightPwm);
				// motor.attach(arduino);
				arduino.attachDevice(motor, null);
				while (true) {
					// try to overrun?
					// rand between -1 and 1.
					double rand = (Math.random() - 0.5) * 2;
					motor.move(rand);
				}
			} else {
				Servo servo = (Servo) Runtime.createAndStart("servo", "Servo");
				arduino.attach(servo, 10);
				// arduino.attachDevice(servo, null);
				servo.attach();
				int angle = 0;
				int max = 5000;
				while (true) {
					// System.out.println(angle);
					angle++;
					servo.moveTo(angle % 180);
					if (angle > max) {
						break;
					}
				}
				System.out.println("done with loop..");
				;

			}

			System.in.read();
			// serial.connectTcp("192.168.0.99", 80);
			// arduino.setLoadTimingEnabled(true);
			// Runtime.start("python", "Python");
			// Runtime.start("webgui", "WebGui");
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
			 * arduino.sendMsg(ArduinoMsgCodec.GET_VERSION); // log.info("{}",
			 * i); }
			 * 
			 * log.error("time {} ms", System.currentTimeMillis() - ts );
			 * 
			 * for (int i = 0; i < 10000; ++i){
			 * arduino.sendMsg(ArduinoMsgCodec.GET_VERSION); log.info("{}", i);
			 * }
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
			 * arduino.getVersion(); Servo servo = (Servo)
			 * Runtime.start("servo", "Servo"); servo.attach(arduino, 10);
			 * 
			 * servo.moveTo(10); servo.moveTo(90); servo.moveTo(180);
			 * servo.moveTo(90); servo.moveTo(10);
			 */

			/*
			 * VirtualDevice virtual = (VirtualDevice) Runtime.start("virtual",
			 * "VirtualDevice"); virtual.createVirtualArduino("vport"); Python
			 * logic = virtual.getLogic();
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
			 * Serial serial = arduino.getSerial();
			 * serial.connectTCP("localhost", 9191);
			 * arduino.connect(serial.getPortName());
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

	/**
	 * high level method for attaching the device of a 
	 * Servo - not to be confused with Servo.attach - this method
	 * attaches the "device" and sends the necessary config to the
	 * method which handles all devices attachDevice(Device, int[] config).
	 * 
	 * Its purpose is to provide a standardized way of attaching a Servo to 
	 * this microcontrollers - the details of how this microcontroller prepares
	 * its message and sends it to the MRLComm is hidden
	 * 
	 * @param servo
	 * @param pin
	 */
	@Override
	public void attach(Servo servo, int pin) {
		servo.setController(this);
		attachDevice(servo, pin);
	}
	
	/**
	 * high level method for detaching the device of a 
	 * Servo - not to be confused with Servo.detach.
	 * 
	 * This method tells MRLComm to release resources 
	 * for this device which will probably include calling the 
	 * method Servo.detach on MRLComm
	 * 
	 */
	public void detach(Servo servo){
		detachDevice(servo);
	}

	@Override
	public void createI2cDevice(int busAddress, int deviceAddress, String type) {
		// TODO Auto-generated method stub - I2C
		// This method should add an entry to the list of I2CDevices that it can do a callback to
		// It should create and initiate one I2C bus in MRLComm and add this arduino as its callback

	}

	@Override
	public void releaseI2cDevice(int busAddress, int deviceAddress) {
		// TODO Auto-generated method stub
		// This method should delete the i2c device entry from the list of I2CDevices
	}

	@Override
	public void i2cWrite(int busAddress, int deviceAddress, byte[] buffer, int size) {
    int msgBuffer[] = new int[size+1];
    msgBuffer[0] = deviceAddress;
    for (int i=0 ; i < size ; i++){
    	msgBuffer[i+1] = (int)buffer[i] & 0xFF;
    }
		sendMsg(I2C_WRITE,msgBuffer);
	}

	@Override
	public int i2cRead(int busAddress, int deviceAddress, byte[] buffer, int size) {
		// Get the device index to the MRL i2c bus so that it can be added to the datastream
		int deviceIndex = 42; // Change this to get the the deviceinex using busAddress
		sendMsg(I2C_READ, deviceIndex, deviceAddress, size);
		int retry = 0;
		int retryMax = 1000;
		try {
			/**
			 * We will wait up to retryMax times to get the i2c data back from
			 * MRLComm.c and wait 1 ms between each try. A blocking queue is not
			 * needed, as this is only a single data element - and blocking is
			 * not necessary.
			 */
			while ((retry < retryMax) && (!i2cDataReturned)) {
				sleep(1);
				++retry;
			}
		} catch (Exception e) {
			Logging.logError(e);
		}
		if (i2cDataReturned) {
			// TODO Implement me
		  // Return the data in buffer[]
			// Return the size of the data or -1 in case of an error
			return size;
		}
		// Time out, no data returned
		return -1;
	}

	@Override
	public int i2cWriteRead(int busAddress, int deviceAddress, byte[] writeBuffer, int writeSize, byte[] readBuffer, int readSize) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setDebug(boolean b) {
		if (b)
			sendMsg(SET_DEBUG, 1);
		else
			sendMsg(SET_DEBUG, 0);
	}

	/**
	 * SensorDataPublisher.publishSensorData - publishes SensorData ! int[] can
	 * be anything the consuming Sensor Service needs
	 */
	@Override
	public SensorData publishSensorData(SensorData data) {
		return data;
	}
	
	@Override
	public void addSensorDataListener(SensorDataListener listener, int[] config) {
		attachDevice(listener, config);
	}


	@Override
	public void detachDevice(DeviceControl device) {
		// TODO validity checks
	}

	/**
	 * THESE ARE SERVO COMMANDS !  NOT REQUEST
	 * TO ATTACH OR DETACH THE SERVO AS A DEVICE !!!
	 * This is Servo.attach(10) .. not Arduino.attach(Device) !!
	 */
	@Override
	public void servoAttach(Servo servo) {
		int id = getDeviceId(servo);
		sendMsg(SERVO_ATTACH, getDeviceId(servo), id);		
	}

	/**
	 * THESE ARE SERVO COMMANDS !  NOT REQUEST
	 * TO ATTACH OR DETACH THE SERVO AS A DEVICE !!!
	 * This is Servo.attach(10) .. not Arduino.attach(Device) !!
	 */
	@Override
	public void servoDetach(Servo servo) {
		int id = getDeviceId(servo);
		sendMsg(SERVO_DETACH, getDeviceId(servo), id);		
	}

	@Override
	public void attach(MotorControl motor, int port) {
		error("port attach not supported for motors - did you want 2 pin control - attach(motor, powerPin, dirPin) ?");
	}

	@Override
	public void attach(MotorControl motor, int powerPin, int dirPin) {
		attachDevice(motor, new int[]{powerPin, dirPin});
	}

	@Override
	public Integer getPin(Servo servo) {
		// TODO Auto-generated method stub
		return null;
	}


}