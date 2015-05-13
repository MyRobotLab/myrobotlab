	package org.myrobotlab.service;

import static org.myrobotlab.codec.ArduinoMsgCodec.ANALOG_READ_POLLING_START;
import static org.myrobotlab.codec.ArduinoMsgCodec.ANALOG_READ_POLLING_STOP;
import static org.myrobotlab.codec.ArduinoMsgCodec.ANALOG_WRITE;
import static org.myrobotlab.codec.ArduinoMsgCodec.DIGITAL_READ_POLLING_START;
import static org.myrobotlab.codec.ArduinoMsgCodec.DIGITAL_READ_POLLING_STOP;
import static org.myrobotlab.codec.ArduinoMsgCodec.DIGITAL_WRITE;
import static org.myrobotlab.codec.ArduinoMsgCodec.GET_VERSION;
import static org.myrobotlab.codec.ArduinoMsgCodec.MAGIC_NUMBER;
import static org.myrobotlab.codec.ArduinoMsgCodec.MAX_MSG_SIZE;
import static org.myrobotlab.codec.ArduinoMsgCodec.MRLCOMM_VERSION;
import static org.myrobotlab.codec.ArduinoMsgCodec.PIN_MODE;
import static org.myrobotlab.codec.ArduinoMsgCodec.PUBLISH_CUSTOM_MSG;
import static org.myrobotlab.codec.ArduinoMsgCodec.PUBLISH_LOAD_TIMING_EVENT;
import static org.myrobotlab.codec.ArduinoMsgCodec.PUBLISH_MRLCOMM_ERROR;
import static org.myrobotlab.codec.ArduinoMsgCodec.PUBLISH_PIN;
import static org.myrobotlab.codec.ArduinoMsgCodec.PUBLISH_PULSE;
import static org.myrobotlab.codec.ArduinoMsgCodec.PUBLISH_SERVO_EVENT;
import static org.myrobotlab.codec.ArduinoMsgCodec.PUBLISH_STEPPER_EVENT;
import static org.myrobotlab.codec.ArduinoMsgCodec.PUBLISH_VERSION;
import static org.myrobotlab.codec.ArduinoMsgCodec.PULSE_IN;
import static org.myrobotlab.codec.ArduinoMsgCodec.SENSOR_ATTACH;
import static org.myrobotlab.codec.ArduinoMsgCodec.SENSOR_POLLING_START;
import static org.myrobotlab.codec.ArduinoMsgCodec.SENSOR_POLLING_STOP;
import static org.myrobotlab.codec.ArduinoMsgCodec.SERVO_ATTACH;
import static org.myrobotlab.codec.ArduinoMsgCodec.SERVO_DETACH;
import static org.myrobotlab.codec.ArduinoMsgCodec.SERVO_SWEEP_START;
import static org.myrobotlab.codec.ArduinoMsgCodec.SERVO_SWEEP_STOP;
import static org.myrobotlab.codec.ArduinoMsgCodec.SERVO_WRITE;
import static org.myrobotlab.codec.ArduinoMsgCodec.SERVO_WRITE_MICROSECONDS;
import static org.myrobotlab.codec.ArduinoMsgCodec.SET_DEBOUNCE;
import static org.myrobotlab.codec.ArduinoMsgCodec.SET_DIGITAL_TRIGGER_ONLY;
import static org.myrobotlab.codec.ArduinoMsgCodec.SET_LOAD_TIMING_ENABLED;
import static org.myrobotlab.codec.ArduinoMsgCodec.SET_PWMFREQUENCY;
import static org.myrobotlab.codec.ArduinoMsgCodec.SET_SAMPLE_RATE;
import static org.myrobotlab.codec.ArduinoMsgCodec.SET_SERIAL_RATE;
import static org.myrobotlab.codec.ArduinoMsgCodec.SET_SERVO_EVENTS_ENABLED;
import static org.myrobotlab.codec.ArduinoMsgCodec.SET_SERVO_SPEED;
import static org.myrobotlab.codec.ArduinoMsgCodec.SET_TRIGGER;
import static org.myrobotlab.codec.ArduinoMsgCodec.STEPPER_ATTACH;
import static org.myrobotlab.codec.ArduinoMsgCodec.STEPPER_MOVE_TO;
import static org.myrobotlab.codec.ArduinoMsgCodec.STEPPER_RESET;
import static org.myrobotlab.codec.ArduinoMsgCodec.STEPPER_STOP;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Stepper.StepperEvent;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.CustomMsgListener;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SensorDataPublisher;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.StepperController;
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

public class Arduino extends Service implements SensorDataPublisher, SerialDataListener, ServoController, MotorController, StepperController {

	/**
	 * MotorData is the combination of a Motor and any controller data needed to
	 * implement all of MotorController API
	 * 
	 */
	class MotorData implements Serializable {
		private static final long serialVersionUID = 1L;
		transient MotorControl motor = null;
		String type = null;
		int PWMPin = -1;
		int dirPin0 = -1;
		int dirPin1 = -1;
	}

	class SensorData implements Serializable {
		private static final long serialVersionUID = 1L;
		// -- FIXME - make Sensor(controller?) interface - when we get a new
		// sensor
		public transient UltrasonicSensor sensor = null;
		public int sensorIndex = -1;
		public long duration; // for ultrasonic
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

	public static class Sketch {
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
	public static final int SENSOR_ULTRASONIC = 1;
	public static final int COMMUNICATION_RESET = 252;
	public static final int SOFT_RESET = 253;
	public static final int NOP = 255;

	public static final int TRUE = 1;
	public static final int FALSE = 0;
	public Integer mrlCommVersion = null;

	public transient static final int REVISION = 100;

	public transient static final String BOARD_TYPE_UNO = "uno";
	public transient static final String BOARD_TYPE_ATMEGA168 = "atmega168";

	public transient static final String BOARD_TYPE_ATMEGA328P = "atmega328p";
	public transient static final String BOARD_TYPE_ATMEGA2560 = "atmega2560";

	public transient static final String BOARD_TYPE_ATMEGA1280 = "atmega1280";
	public transient static final String BOARD_TYPE_ATMEGA32U4 = "atmega32u4";
	// vendor specific pins start at 50
	public static final String VENDOR_DEFINES_BEGIN = "// --VENDOR DEFINE SECTION BEGIN--";

	public static final String VENDOR_SETUP_BEGIN = "// --VENDOR SETUP BEGIN--";

	public static final String VENDOR_CODE_BEGIN = "// --VENDOR CODE BEGIN--";
	/**
	 * pin description of board
	 */
	ArrayList<Pin> pinList = null;

	// data and mapping for data going from MRL ---to---> Arduino
	HashMap<String, Stepper> steppers = new HashMap<String, Stepper>();
	// index for data mapping going from Arduino ---to---> MRL
	HashMap<Integer, Stepper> stepperIndex = new HashMap<Integer, Stepper>();

	// data and mapping for data going from MRL ---to---> Arduino
	HashMap<String, SensorData> sensors = new HashMap<String, SensorData>();
	// index for data mapping going from Arduino ---to---> MRL
	HashMap<Integer, SensorData> sensorsIndex = new HashMap<Integer, SensorData>();

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

	
	String board;

	/**
	 * blocking queues to support blocking methods
	 */
	// Member field vs local define for single entry ?
	transient BlockingQueue<Integer> pulseQueue = new LinkedBlockingQueue<Integer>();
	transient BlockingQueue<Integer> versionQueue = new LinkedBlockingQueue<Integer>();

	HashMap<String, Motor> motors = new HashMap<String, Motor>();

	HashMap<Integer, String> encoderPins = new HashMap<Integer, String>();

	transient CustomMsgListener customEventListener = null;

	/**
	 * servos - name index of servo we need 2 indexes for servos because they
	 * will be referenced by name OR by index
	 */
	HashMap<String, ServoData> servos = new HashMap<String, ServoData>();
	/**
	 * index reference of servo
	 */
	HashMap<Integer, ServoData> servoIndex = new HashMap<Integer, ServoData>();

	/**
	 * Serial service - the Arduino's serial connection
	 */
	transient Serial serial;

	int error_arduino_to_mrl_rx_cnt;
	int error_mrl_to_arduino_rx_cnt;

	int byteCount;

	int msgSize;

	int[] msg = new int[MAX_MSG_SIZE];

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		peers.put("serial", "Serial", "serial device for this Arduino");
		return peers;
	}

	// ---------------------------- ServoController End -----------------------
	// ---------------------- Protocol Methods Begin ------------------

	public Arduino(String n) {
		super(n);
		serial = (Serial) createPeer("serial");
		createPinList();
		String mrlcomm = FileIO.resourceToString("Arduino/MRLComm2.ino");
		setSketch(new Sketch("MRLComm", mrlcomm));
	}

	public void addCustomMsgListener(CustomMsgListener service) {
		customEventListener = service;
	}

	/**
	 * start analog polling of selected pin
	 * 
	 * @param pin
	 */
	public void analogReadPollingStart(Integer pin) {
		sendMsg(PIN_MODE, pin, INPUT);
		sendMsg(ANALOG_READ_POLLING_START, pin);
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

	/**
	 * default params to connect to Arduino & MRLComm.ino
	 * 
	 * @param port
	 * @return
	 * @throws IOException
	 * @throws SerialDeviceException
	 */
	public boolean connect(String port) {
		// FIXME ! <<<-- REMOVE ,this) - patterns should be to add listener on
		// startService
		// return connect(port, 57600, 8, 1, 0); <- put this back ?
		return serial.connect(port); // <<<-- REMOVE ,this) - patterns
											// should be to add listener on
											// startService
	}

	// TODO - should be override .. ??
	public Serial connectVirtualUART() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException,
			IllegalArgumentException, InvocationTargetException {
		Serial uart = serial.createVirtualUART();
		uart.setCodec("arduino");
		connect(serial.getName());
		return uart;
	}

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
		sendMsg(PIN_MODE, pin, INPUT);
		sendMsg(DIGITAL_READ_POLLING_START, pin);
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
	public String[] getCategories() {
		return new String[] { "microcontroller" };
	}

	@Override
	public String getDescription() {
		return "This service interfaces with an Arduino micro-controller.";
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
	
	public Integer refreshVersion(){
		mrlCommVersion = null;
		return getVersion();
	}

	/**
	 * GOOD DESIGN !! - blocking version of getVersion - blocks on
	 * publishVersion method returns null if 1 second timeout is reached.
	 * 
	 * This is a good pattern for future blocking methods.
	 * 
	 * @return
	 */
	public Integer getVersion() {
		log.info("getVersion");
		
		// cached
		if (mrlCommVersion != null){
			invoke("publishVersion", mrlCommVersion);
			return mrlCommVersion;
		}
		
		try {
			versionQueue.clear();
			sendMsg(GET_VERSION);
			mrlCommVersion = versionQueue.poll(1000, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			Logging.logError(e);
		}
		if (mrlCommVersion == null) {
			error("did not get response from arduino....");
		} else if (!mrlCommVersion.equals(MRLCOMM_VERSION)) {
			error(String.format("MRLComm.ino responded with version %s expected version is %s", mrlCommVersion, MRLCOMM_VERSION));
		} else {
			info(String.format("connected %s responded version %s ... goodtimes...", serial.getName(), mrlCommVersion));
		}

		return mrlCommVersion;
	}

	public boolean isConnected() {
		return serial.isConnected();
	}

	@Override
	public boolean motorAttach(String motorName, Integer pwmPin, Integer dirPin) {
		return motorAttach(motorName, Motor.TYPE_PWM_DIR, pwmPin, dirPin, null);
	}

	@Override
	public boolean motorAttach(String motorName, String type, Integer pwmPin, Integer dirPin) {
		return motorAttach(motorName, type, pwmPin, dirPin, null);
	}

	// ----------- motor controller api begin ----------------
	// very old problem - how much logic in controller versus motor
	// the concept of a 2 pin controller is pretty ubiquitous and probably
	// should be in the motor

	@Override
	public boolean motorAttach(String motorName, String type, Integer pwmPin, Integer dirPin, Integer encoderPin) {
		Motor motor = (Motor) Runtime.getService(motorName);
		if (!motor.isLocal()) {
			error("motor is not in the same MRL instance as the motor controller");
			return false;
		}
		motor.type = type;

		motors.put(motor.getName(), motor);
		motor.setController(this);

		if (encoderPin != null) {
			motor.encoderPin = encoderPin;
			// sendMsg(PINMODE, md.encoderPin, INPUT);
			encoderPins.put(motor.encoderPin, motor.getName());
			analogReadPollingStart(motor.encoderPin);
		}

		if (Motor.TYPE_LPWM_RPWM.equals(motor.type)) {
			motor.pwmLeft = pwmPin;
			motor.pwmRight = dirPin;
			sendMsg(PIN_MODE, motor.pwmLeft, OUTPUT);
			sendMsg(PIN_MODE, motor.pwmRight, OUTPUT);
		} else {
			motor.pwmPin = pwmPin;
			motor.dirPin = dirPin;
			sendMsg(PIN_MODE, motor.pwmPin, OUTPUT);
			sendMsg(PIN_MODE, motor.dirPin, OUTPUT);
		}

		motor.broadcastState();
		return true;
	}

	@Override
	public boolean motorDetach(String motorName) {
		boolean ret = motors.containsKey(motorName);
		if (ret) {
			motors.remove(motorName);
		}
		return ret;
	}

	@Override
	public void motorMove(String name) {

		Motor motor = motors.get(name);
		double powerLevel = motor.getPowerLevel();

		if (Motor.TYPE_LPWM_RPWM.equals(motor.type)) {
			if (powerLevel < 0) {
				sendMsg(ANALOG_WRITE, motor.pwmLeft, 0);
				sendMsg(ANALOG_WRITE, motor.pwmRight, Math.abs((int) (powerLevel)));
			} else {
				sendMsg(ANALOG_WRITE, motor.pwmRight, 0);
				sendMsg(ANALOG_WRITE, motor.pwmLeft, Math.abs((int) (powerLevel)));
			}

		} else {
			sendMsg(DIGITAL_WRITE, motor.dirPin, (powerLevel < 0) ? MOTOR_BACKWARD : MOTOR_FORWARD);
			sendMsg(ANALOG_WRITE, motor.pwmPin, Math.abs((int) (powerLevel)));
		}

	}

	@Override
	public void motorMoveTo(String name, double position) {
		// TODO Auto-generated method stub

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

	@Override
	public Integer onByte(Integer newByte) {

		try {

			// log.info(String.format("onByte %d", newByte));

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
			// while (serial.isConnected() && (newByte = serial.read()) > -1) {

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

			// process valid message
			if (byteCount == 2 + msgSize) {
				// log.error("A {}", dump.toString());
				// dump.setLength(0);

				// MSG CONTENTS = FN | D0 | D1 | ...
				int function = msg[0];
				// log.info(String.format("%d", msg[1]));
				switch (function) {

				case PUBLISH_MRLCOMM_ERROR: {
					++error_mrl_to_arduino_rx_cnt;
					error("MRL->Arduino rx %d type %d", error_mrl_to_arduino_rx_cnt, msg[1]);
					break;
				}

				case PUBLISH_VERSION: {
					// TODO - get vendor version
					// String version = String.format("%d", msg[1]);
					versionQueue.add(msg[1] & 0xff);
					int v = msg[1] & 0xff;
					log.info(String.format("PUBLISH_VERSION %d", msg[1] & 0xff));
					invoke("publishVersion", v);
					break;
				}
				// FIXME PUBLISH_PULSE_IN
				case PUBLISH_PULSE: {
					// extract signed Java long from byte array offset 1
					// - length 4 :P
					Integer pulse = Serial.bytesToInt(msg, 1, 4);
					pulseQueue.add(pulse);
					break;
				}
				case PUBLISH_PIN: {
					// Pin p = new Pin(msg[1], msg[0], (((msg[2] & 0xFF)
					// << 8) + (msg[3] & 0xFF)), getName());
					// FIXME
					//Pin pin = pinList.get(msg[1]); BIG BUG - if a reference is sent and
					// the same reference whic his trying to be displayed is changed underneath
					Pin pin = new Pin(pinList.get(msg[1]));
					pin.value = ((msg[2] & 0xFF) << 8) + (msg[3] & 0xFF);
					invoke("publishPin", pin);
					break;
				}
				/*
				 * case PUBLISH_DIGITAL_VALUE: { Pin pin = pinList.get(msg[1]);
				 * pin.value = msg[2]; invoke("publishPin", pin); break; }
				 */

				case PUBLISH_LOAD_TIMING_EVENT: {

					long microsPerLoop = Serial.bytesToInt(msg, 1, 4);
					info("load %d us", microsPerLoop);
					// invoke("publishPin", pin);
					break;
				}

				case PUBLISH_SERVO_EVENT: {

					int index = msg[1];
					int eventType = msg[2];
					int currentPos = msg[3];
					int targetPos = msg[4];

					log.info(String.format(" index %d type %d cur %d target %d", index, eventType, currentPos & 0xff, targetPos & 0xff));
					// uber good -
					// TODO - deprecate ServoControl interface - not
					// needed Servo is abstraction enough
					Servo servo = (Servo) servoIndex.get(index).servo;
					servo.invoke("publishServoEvent", currentPos & 0xff);
					break;
				}

				/*
				 * case PUBLISH_SENSOR_DATA: { int index = (int) msg[1];
				 * SensorData sd = sensorsIndex.get(index); sd.duration =
				 * Serial.bytesToInt(msg, 2, 4); // HMM WAY TO GO - is NOT to
				 * invoke its own but // invoke publishSensorData on Sensor //
				 * since its its own service // invoke("publishSensorData", sd);
				 * // NICE !! - force sensor to have publishSensorData // or
				 * publishRange in interface !!! //
				 * sd.sensor.invoke("publishRange", sd);
				 * sd.sensor.invoke("publishRange", sd.duration); break; }
				 */

				case PUBLISH_STEPPER_EVENT: {

					int index = msg[1];
					int eventType = msg[2];
					int currentPos = (msg[3] << 8) + (msg[4] & 0xff);

					log.info(String.format(" index %d type %d cur pos %d", index, eventType, currentPos));
					// uber good -
					// TODO - stepper ServoControl interface - not
					// needed Servo is abstraction enough
					Stepper stepper = (Stepper) stepperIndex.get(index);
					//stepper.invoke("publishStepperEvent", currentPos);
					// LOCAL !!! - Remote from Arduino or Stepper ?!?!?
					// ?? stepper.publishStepperEvent(currentPos);
					// GOOD - model this pattern
					// set service data directly 
					// not having a local call back seems ridiculous ! ie. controller separated from controlled periphery 
					// should not be supported - after updating data directly invoke the event on the stepper
					stepper.setPos(currentPos);
					// based on config of stepper - invoke or don't
					stepper.invoke("publishStepperEvent", new StepperEvent(eventType, currentPos));
					//
					break;
				}

				case PUBLISH_CUSTOM_MSG: {

					// msg or data is of size byteCount
					int paramCnt = msg[1];
					int paramIndex = 2; // current index in buffer
					// decode parameters
					Object[] params = new Object[paramCnt];

					int paramType = 0;

					for (int i = 0; i < paramCnt; ++i) {

						// get parameter type
						// paramType = msg[];
						paramType = msg[paramIndex];

						Integer x = 0;
						// convert
						if (paramType == ARDUINO_TYPE_INT) {
							// params[i] =
							x = ((msg[++paramIndex] & 0xFF) << 8) + (msg[++paramIndex] & 0xFF);
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
						//send(customEventListener.getName(), "onCustomMsg", params);
						customEventListener.onCustomMsg(params);
					}
					// FIXME more effecient to only allow subscribers which have used the addCustomMsgListener?
					invoke("publishCustomMsg", new Object[]{params});

					break;
				}

				default: {
					// FIXME - use formatter for message
					error("unknown serial event <- ");
					break;
				}

				} // end switch

				if (log.isDebugEnabled()) {
					// FIXME - use formatter
					log.debug("serialEvent <- ");//
				}

				// processed msg
				// reset msg buffer
				msgSize = 0;
				byteCount = 0;
			}
			// } // while (serial.isOpen() && (newByte =
			// serial.read()) > -1

		} catch (Exception e) {
			++error_mrl_to_arduino_rx_cnt;
			error("msg structure violation %d", error_mrl_to_arduino_rx_cnt);
			// try again ?
			msgSize = 0;
			byteCount = 0;
			Logging.logError(e);
		}

		return newByte;
	}

	@Override
	public String onConnect(String portName) {
		info("%s connected to %s", getName(), portName);
		getVersion();
		return portName;
	}
	
	public String getPortName(){
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
		log.info(String.format("pinMode(%d,%d) to %s", address, value, serial.getName()));
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
	 * Arduino board the Arduino must be told to poll the desired pin(s). This
	 * is done with a analogReadPollingStart(pin) or digitalReadPollingStart()
	 */
	@Override
	public Pin publishPin(Pin p) {
		// log.debug(p);
		pinList.get(p.pin).value = p.value;
		return p;
	}

	/**
	 * GOOD ! - asynchronous call-back for a pulseIn, it can be subscribed to,
	 * or with the blocking queue a different thread can use it as a blocking
	 * call
	 * 
	 * Easy testing can be accomplished by using the blocking pulseIn which
	 * utilizes the queue
	 * 
	 * @param data
	 * @return
	 */
	public Integer publishPulse(Integer data) {
		pulseQueue.add(data);
		return data;
	}

	// ----------- MotorController API End ----------------

	public int publishServoEvent(Integer pos) {
		return pos;
	}

	public SensorData publishSesorData(SensorData data) {
		return data;
	}

	public Pin publishTrigger(Pin pin) {
		return pin;
	}

	// -- StepperController begin ----

	public Integer publishVersion(Integer version) {
		info("publishVersion %d", version);
		return version;
	}

	// often used as a ping echo pulse - timing is critical
	// so it has to be done on the uC .. therefore
	// a trigger pin has to be sent as well to the pulseIn
	// as well as the pulse/echo pin
	public long pulseIn(int trigPin, int echoPin) {
		return pulseIn(trigPin, echoPin, HIGH, 1000);
	}

	/**
	 * The pulseIn - does not use the Arduino language "pulseIn" because that
	 * method blocks. We don't want to be blocked inside of MRLComm ! But for
	 * convenience the elves have created a blocking pulseIn which works with
	 * the asynchronous event and non-blocking pulseIn in MRLComm
	 * 
	 * @param trigPin
	 * @param echoPin
	 * @param value
	 * @param timeout
	 * @return
	 */
	// FIXME - rather application specific - possible to add variable delays on
	// trigger
	// and echo
	public int pulseIn(int trigPin, int echoPin, int value, int timeout) {
		try {
			if (serial != null) {
				pulseQueue.clear();
				sendMsg(PULSE_IN, trigPin, echoPin, value, timeout);
				// downstream longer timeout than upstream
				Integer pulse = pulseQueue.poll(250 + timeout, TimeUnit.MILLISECONDS);
				if (pulse == null) {
					return 0;
				}
				return pulse;
			} else {
				return 0;
			}

		} catch (Exception e) {
			Logging.logError(e);
			return 0;
		}
	}

	public long pulseIn(int trigPin, int echoPin, int timeout, String highLow) {
		int value = HIGH;
		if (highLow != null && highLow.equalsIgnoreCase("LOW")) {
			value = LOW;
		}
		return pulseIn(trigPin, echoPin, value, timeout);
	}

	public long pulseIn(int trigPin, int echoPin, Integer timeout) {

		if (timeout != null) {
			timeout = 1000;
		}
		return pulseIn(trigPin, echoPin, 1000, null);
	}

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
	 *            TODO - take the cheese out of this method .. it shold be
	 *            sendMsg(byte[]...data)
	 */
	public synchronized void sendMsg(int function, int... params) {
		// log.debug("sendMsg magic | fn " + function + " p1 " + param1 + " p2 "
		// + param2);
		try {

			// not CRC16 - but cheesy error correction of bytestream
			// http://www.java2s.com/Open-Source/Java/6.0-JDK-Modules-sun/misc/sun/misc/CRC16.java.htm
			// #include <util/crc16.h>
			// _crc16_update (test, testdata);

			serial.write(MAGIC_NUMBER);

			// msg size = function byte + x param bytes
			// msg size does not include MAGIC_NUMBER & size
			// MAGIC_NUMBER|3|FUNCTION|PARAM0|PARAM2 would be valid
			serial.write(1 + params.length);

			serial.write(function);

			for (int i = 0; i < params.length; ++i) {
				serial.write(params[i]);
			}

		} catch (Exception e) {
			error("sendMsg " + e.getMessage());
		}

	}

	public synchronized int sensorAttach(String sensorName) {
		UltrasonicSensor sensor = (UltrasonicSensor) Runtime.getService(sensorName);
		if (sensor == null) {
			log.error("Sensor {} not valid", sensorName);
			return -1;
		}
		return sensorAttach(sensor);
	}

	public synchronized int sensorAttach(UltrasonicSensor sensor) {
		String sensorName = sensor.getName();
		log.info(String.format("sensorAttach %s", sensorName));
		int sensorIndex = -1;

		if (serial == null) {
			error("could not attach sensor - no serial device!");
			return -1;
		}

		if (sensors.containsKey(sensorName)) {
			log.warn("sensor already attach - detach first");
			return -1;
		}

		int type = -1;

		if (sensor instanceof UltrasonicSensor) {
			type = SENSOR_ULTRASONIC;
		}

		if (type == SENSOR_ULTRASONIC) {

			// simple count = index mapping
			sensorIndex = sensors.size();

			// attach index pin
			sendMsg(SENSOR_ATTACH, sensorIndex, SENSOR_ULTRASONIC, sensor.getTriggerPin(), sensor.getEchoPin());

			SensorData sd = new SensorData();
			sd.sensor = sensor;
			sd.sensorIndex = sensorIndex;

			sensors.put(sensorName, sd);
			sensorsIndex.put(sensorIndex, sd);

			log.info(String.format("sensor SR04 index %d pin trig %d echo %d attached ", sensorIndex, sensor.getTriggerPin(), sensor.getEchoPin()));
		}

		return sensorIndex;
	}

	public boolean sensorPollingStart(String name, int timeoutMS) {
		info("sensorPollingStart %s", name);
		if (!sensors.containsKey(name)) {
			error("can not poll sensor %s - not defined", name);
			return false;
		}
		int index = sensors.get(name).sensorIndex;
		sendMsg(SENSOR_POLLING_START, index, timeoutMS);
		return true;
	}

	public boolean sensorPollingStop(String name) {
		info("sensorPollingStop %s", name);
		if (!sensors.containsKey(name)) {
			error("can not poll sensor %s - not defined", name);
			return false;
		}
		int index = sensors.get(name).sensorIndex;
		sendMsg(SENSOR_POLLING_STOP, index);
		return true;
	}

	// FIXME - need interface for this
	public synchronized boolean servoAttach(Servo servo, Integer pin) {
		String servoName = servo.getName();
		log.info(String.format("servoAttach %s pin %d", servoName, pin));

		if (serial == null) {
			error("could not attach servo to pin %d serial is null - not initialized?", pin);
			return false;
		}

		if (servos.containsKey(servo.getName())) {
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

		// complex formula to calculate servo index
		// this "could" be complicated - even so compicated
		// as asking MRLComm.ino to find the "next available index
		// and send it back - but I've tried that scheme and
		// because the Servo's don't fully "detach" using the standard library
		// it proved very "bad"
		// simplistic mapping where Java is in control seems best
		int index = pin - 2;

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
		servos.put(servo.getName(), sd);
		servoIndex.put(index, sd);
		servo.setController(this);
		servo.setPin(pin);
		log.info("servo index {} pin {} attached ", index, pin);
		return true;
	}

	@Override
	public boolean servoAttach(String servoName, Integer pin) {
		Servo servo = (Servo) Runtime.getService(servoName);
		if (servo == null) {
			error("servoAttach can not attach %s no service exists", servoName);
			return false;
		}
		return servoAttach(servo, servo.getPin());
	}

	// FIXME make & merge interface for this
	public synchronized boolean servoDetach(Servo servo) {
		String servoName = servo.getName();
		log.info(String.format("servoDetach(%s)", servoName));

		if (servos.containsKey(servoName)) {
			ServoData sd = servos.get(servoName);
			sendMsg(SERVO_DETACH, sd.servoIndex, 0);
			servos.remove(servoName);
			servoIndex.remove(sd.servoIndex);
			return true;
		}

		error("servo %s detach failed - not found", servoName);
		return false;
	}

	@Override
	public boolean servoDetach(String servoName) {
		Servo servo = (Servo) Runtime.getService(servoName);
		return servoDetach(servo);
	}

	@Override
	public void servoSweepStart(String servoName, int min, int max, int step) {
		if (!servos.containsKey(servoName)) {
			warn("Servo %s not attached to %s", servoName, getName());
			return;
		}
		int index = servos.get(servoName).servoIndex;
		log.info(String.format("servoSweep %s index %d min %d max %d step %d", servoName, index, min, max, step));
		sendMsg(SERVO_SWEEP_START, index, min, max, step);
	}

	@Override
	public void servoSweepStop(String servoName) {
		int index = servos.get(servoName).servoIndex;
		sendMsg(SERVO_SWEEP_STOP, index);
	}

	@Override
	public void servoWrite(String servoName, Integer newPos) {
		if (!servos.containsKey(servoName)) {
			warn("Servo %s not attached to %s", servoName, getName());
			return;
		}

		int index = servos.get(servoName).servoIndex;
		log.info(String.format("servoWrite %s %d index %d", servoName, newPos, index));
		sendMsg(SERVO_WRITE, index, newPos);
	}

	// FIXME - not "servo" .. just writeMicroseconds
	// FIXME FIXME FIXME - start fixing up & creating interface of PIN WRITING
	// READING GETTING AND CONTROLL CUZ
	// THATS WHAT MICROCONTROLLERS DO !
	@Override
	public void servoWriteMicroseconds(String servoName, Integer newPos) {

		if (!servos.containsKey(servoName)) {
			warn("Servo %s not attached to %s", servoName, getName());
			return;
		}

		int index = servos.get(servoName).servoIndex;

		log.info(String.format("writeMicroseconds %s %d index %d", servoName, newPos, index));

		sendMsg(SERVO_WRITE_MICROSECONDS, index, newPos);

	}

	public String setBoard(String board) {
		this.board = board;
		createPinList();
		broadcastState();
		return board;
	}
	
	/**
	 * easy way to set to a 54 pin arduino
	 * @return
	 */
	public String setBoardMega(){
		board = BOARD_TYPE_ATMEGA2560;
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
	public boolean setServoEventsEnabled(String servoName, boolean enable) {
		log.info(String.format("setServoEventsEnabled %s %b", servoName, enable));
		if (servos.containsKey(servoName)) {
			ServoData sd = servos.get(servoName);

			if (enable) {
				sendMsg(SET_SERVO_EVENTS_ENABLED, sd.servoIndex, TRUE);
			} else {
				sendMsg(SET_SERVO_EVENTS_ENABLED, sd.servoIndex, FALSE);
			}

			return true;
		}

		return false;
	}

	@Override
	public void setServoSpeed(String servoName, Float speed) {
		if (speed == null || speed < 0.0f || speed > 1.0f) {
			error("speed %f out of bounds", speed);
			return;
		}
		sendMsg(SET_SERVO_SPEED, servos.get(servoName).servoIndex, (int) (speed * 100));
	}

	public void setSketch(Sketch sketch) {
		this.sketch = sketch;
		broadcastState();
	}

	public void setStepperSpeed(Integer speed) {
		// TODO Auto-generated method stub

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
		sendMsg(SOFT_RESET, 0, 0);
	}

	@Override
	public void startService() {
		super.startService();
		try {
			serial = (Serial) startPeer("serial");
			// FIXME - dynamically additive - if codec key has never been used -
			// add key
			serial.setCodec("arduino");
			serial.addByteListener(this);
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public boolean stepperAttach(Stepper stepper) {
		String stepperName = stepper.getName();
		log.info(String.format("stepperAttach %s", stepperName));
		
		if (!isConnected()){
			error("%s must be connected to serial port before attaching stepper", getName());
			return false;
		}
		
		int index = 0;

		if (steppers.containsKey(stepperName)) {
			warn("stepper already attach - detach first");
			return true;
		}

		stepper.setController(this);

		if (Stepper.STEPPER_TYPE_SIMPLE == stepper.getStepperType()) {
			
			// simple count = index mapping
			index = steppers.size();

			// attach index pin - FIXME - add number of steps and other paramters - initial speed - pause timings
			sendMsg(STEPPER_ATTACH, index, stepper.getStepperType(), stepper.getDirPin(), stepper.getStepPin());

			stepper.setIndex(index);

			steppers.put(stepperName, stepper);
			stepperIndex.put(index, stepper);

			log.info(String.format("stepper STEPPER_TYPE_SIMPLE index %d pin direction %d step %d attached ", index, stepper.getDirPin(), stepper.getStepPin()));
		} else {
			error("unkown type of stepper");
			return false;
		}

		return true;
	}

	@Override
	public boolean stepperAttach(String stepperName) {
		Stepper stepper = (Stepper) Runtime.getService(stepperName);
		if (stepper == null) {
			log.error("Stepper {} not valid", stepperName);
			return false;
		}
		return stepperAttach(stepper);
	}

	@Override
	public boolean stepperDetach(String stepperName) {
		Stepper stepper = null;
		if (steppers.containsKey(stepperName)){
			stepper = steppers.remove(stepperName);
			if (stepperIndex.containsKey(stepper.getIndex())){
				stepperIndex.remove(stepper.getIndex());
				return true;
			}
		}
		return false;
	}

	public void stepperMoveTo(String name, int newPos, int style) {
		if (!steppers.containsKey(name)) {
			error("%s stepper not found", name);
			return;
		}

		Stepper stepper = steppers.get(name);
		if (Stepper.STEPPER_TYPE_SIMPLE != stepper.getStepperType()) {
			error("unknown stepper type");
			return;
		}

		int lsb = newPos & 0xff;
		int msb = (newPos >> 8) & 0xff;

		sendMsg(STEPPER_MOVE_TO, stepper.getIndex(), msb, lsb, style);

		// TODO - call back event - to say arrived ?

		// TODO - blocking method

	}

	@Override
	public void stepperReset(String stepperName) {
		Stepper stepper = steppers.get(stepperName);
		sendMsg(STEPPER_RESET, stepper.getIndex());
	}

	public void stepperStop(String name) {
		Stepper stepper = steppers.get(name);
		sendMsg(STEPPER_STOP, stepper.getIndex());
	}

	@Override
	public void stopService() {
		super.stopService();
		disconnect();
	}

	@Override
	public Status test() {

		Status status = Status.info("starting %s %s test", getName(), getType());

		try {
			// get running reference to self
			Arduino arduino = (Arduino) Runtime.start(getName(), "Arduino");
			Serial serial = arduino.getSerial();
			Serial uart = serial.createVirtualUART();
			uart.record();

			// set board type
			// FIXME - this should be done by MRLComm.ino (compiled in)
			status.addInfo("setting board type to %s", BOARD_TYPE_ATMEGA2560);
			arduino.setBoard(BOARD_TYPE_ATMEGA2560);

			ArrayList<Pin> pinList = getPinList();
			status.addInfo("found %d pins", pinList.size());

			for (int i = 0; i < pinList.size(); ++i) {
				arduino.analogWrite(pinList.get(i).pin, 0);
				arduino.analogWrite(pinList.get(i).pin, 128);
				arduino.analogWrite(pinList.get(i).pin, 255);
			}

			for (int i = 0; i < pinList.size(); ++i) {
				arduino.digitalWrite(pinList.get(i).pin, 1);
				arduino.digitalWrite(pinList.get(i).pin, 0);
			}

			arduino.disconnect();

			// nullModem.close();

			for (int i = 0; i < 10; ++i) {
				long duration = arduino.pulseIn(7, 8);
				log.info("duration {} uS", duration);
			}

			UltrasonicSensor sr04 = (UltrasonicSensor) Runtime.start("sr04", "UltrasonicSensor");
			Runtime.start("gui", "GUIService");

			sr04.attach(serial.getPortName(), 7, 8);
			sr04.startRanging();

			sr04.stopRanging();
		} catch (Exception e) {
			Logging.logError(e);
		}

		// TODO - test all functions

		// TODO - test digital pin
		// getDigitalPins

		// getAnalogPins

		return status;

	}

	public static void main(String[] args) {
		try {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");

			/*
			VirtualDevice virtual = (VirtualDevice) Runtime.start("virtual", "VirtualDevice");
			virtual.createVirtualArduino("vport");
			arduino.connect("vport");
			*/
			

			//arduino.setBoardMega();
			//arduino.connect("COM15");
			Runtime.start("python", "Python");			
			Runtime.start("gui", "GUIService");
			//Runtime.start("python", "Python");
			//Runtime.broadcastStates();

			//arduino.analogReadPollingStart(68);
			boolean done = true;
			if (done) {
				return;
			}
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

}
