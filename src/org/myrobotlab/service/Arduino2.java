package org.myrobotlab.service;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.SimpleTimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.MRLError;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.SerialDeviceFactory;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SensorDataPublisher;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.StepperControl;
import org.myrobotlab.service.interfaces.StepperController;
import org.slf4j.Logger;

/**
 * Implementation of a Arduino2 Service connected to MRL through a serial port.
 * The protocol is basically a pass through of system calls to the Arduino2
 * board. Data can be passed back from the digital or analog ports by request to
 * start polling. The serial port can be wireless (bluetooth), rf, or wired. The
 * communication protocol supported is in MRLComm.ino
 * 
 * Should support nearly all Arduino2 board types
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
 */

public class Arduino2 extends Service implements SensorDataPublisher, SerialDataListener, ServoController, MotorController, StepperController {

	private static final long serialVersionUID = 1L;
	public transient final static Logger log = LoggerFactory.getLogger(Arduino2.class);

	// ---------- MRLCOMM FUNCTION INTERFACE BEGIN -----------

	public static final int MRLCOMM_VERSION = 18;

	// serial protocol functions
	public static final int MAGIC_NUMBER = 170; // 10101010

	// MRL ---> Arduino2 methods
	public static final int DIGITAL_WRITE = 0;
	public static final int DIGITAL_VALUE = 1; // normalized with PinData
	public static final int ANALOG_WRITE = 2;
	public static final int ANALOG_VALUE = 3; // normalized with PinData
	public static final int PINMODE = 4;
	public static final int PULSE_IN = 5;
	public static final int SERVO_ATTACH = 6;
	public static final int SERVO_WRITE = 7;
	public static final int SERVO_SET_MAX_PULSE = 8;
	public static final int SERVO_DETACH = 9;
	public static final int SET_PWM_FREQUENCY = 11;
	public static final int SET_SERVO_SPEED = 12;
	public static final int ANALOG_READ_POLLING_START = 13;
	public static final int ANALOG_READ_POLLING_STOP = 14;
	public static final int DIGITAL_READ_POLLING_START = 15;
	public static final int DIGITAL_READ_POLLING_STOP = 16;
	public static final int SET_ANALOG_TRIGGER = 17;
	public static final int REMOVE_ANALOG_TRIGGER = 18;
	public static final int SET_DIGITAL_TRIGGER = 19;
	public static final int REMOVE_DIGITAL_TRIGGER = 20;
	public static final int DIGITAL_DEBOUNCE_ON = 21;
	public static final int DIGITAL_DEBOUNCE_OFF = 22;
	public static final int DIGITAL_TRIGGER_ONLY_ON = 23;
	public static final int DIGITAL_TRIGGER_ONLY_OFF = 24;
	public static final int SET_SERIAL_RATE = 25;
	public static final int GET_MRLCOMM_VERSION = 26;
	public static final int SET_SAMPLE_RATE = 27;
	public static final int SERVO_WRITE_MICROSECONDS = 28;
	public static final int MRLCOMM_ERROR = 29;

	public static final int PINGDAR_ATTACH = 30;
	public static final int PINGDAR_START = 31;
	public static final int PINGDAR_STOP = 32;
	public static final int PINGDAR_DATA = 33;

	public static final int SENSOR_ATTACH = 34;
	public static final int SENSOR_POLLING_START = 35;
	public static final int SENSOR_POLLING_STOP = 36;
	public static final int SENSOR_DATA = 37;

	public static final int SERVO_SWEEP_START = 38;
	public static final int SERVO_SWEEP_STOP = 39;

	// callback event - e.g. position arrived
	// MSG MAGIC | SZ | SERVO-INDEX | POSITION
	public static final int SERVO_EVENTS_ENABLE = 40;
	public static final int SERVO_EVENT = 41;

	public static final int LOAD_TIMING_ENABLE = 42;
	public static final int LOAD_TIMING_EVENT = 43;

	public static final int STEPPER_ATTACH = 44;
	public static final int STEPPER_MOVE = 45;
	public static final int STEPPER_STOP = 46;
	public static final int STEPPER_RESET = 47;

	public static final int STEPPER_EVENT = 48;
	public static final int STEPPER_EVENT_STOP = 1;

	public static final int STEPPER_TYPE_POLOLU = 1;

	public static final int CUSTOM_MSG = 50;

	public static final int ARDUINO_TYPE_INT = 16;

	// servo event types
	public static final int SERVO_EVENT_STOPPED = 1;
	public static final int SERVO_EVENT_POSITION_UPDATE = 2;

	// error types
	public static final int ERROR_SERIAL = 1;
	public static final int ERROR_UNKOWN_CMD = 2;

	// sensor types
	public static final int SENSOR_ULTRASONIC = 1;

	// need a method to identify type of board
	// http://forum.arduino.cc/index.php?topic=100557.0

	public static final int COMMUNICATION_RESET = 252;
	public static final int SOFT_RESET = 253;
	public static final int NOP = 255;

	// ---------- MRLCOMM FUNCTION INTERFACE END -----------

	public static final int TRUE = 1;
	public static final int FALSE = 0;

	public Integer mrlcommVersion = null;

	public transient static final int REVISION = 100;

	public transient static final String BOARD_TYPE_UNO = "uno";
	public transient static final String BOARD_TYPE_ATMEGA168 = "atmega168";
	public transient static final String BOARD_TYPE_ATMEGA328P = "atmega328p";
	public transient static final String BOARD_TYPE_ATMEGA2560 = "atmega2560";
	public transient static final String BOARD_TYPE_ATMEGA1280 = "atmega1280";
	public transient static final String BOARD_TYPE_ATMEGA32U4 = "atmega32u4";

	/**
	 * pin description of board
	 */
	ArrayList<Pin> pinList = null;

	// data and mapping for data going from MRL ---to---> Arduino2
	HashMap<String, StepperControl> steppers = new HashMap<String, StepperControl>();
	// index for data mapping going from Arduino2 ---to---> MRL
	HashMap<Integer, StepperControl> stepperIndex = new HashMap<Integer, StepperControl>();

	// data and mapping for data going from MRL ---to---> Arduino2
	HashMap<String, SensorData> sensors = new HashMap<String, SensorData>();
	// index for data mapping going from Arduino2 ---to---> MRL
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

	// serial device info
	// private transient SerialDevice serial;

	// imported Arduino2 constants
	public static final int HIGH = 0x1;
	public static final int LOW = 0x0;

	public static final int INPUT = 0x0;
	public static final int OUTPUT = 0x1;

	public static final int MOTOR_FORWARD = 1;
	public static final int MOTOR_BACKWARD = 0;

	private boolean connected = false;
	private String boardType;

	transient BlockingQueue<Object> blockingData = new LinkedBlockingQueue<Object>();

	StringBuilder debugTX = new StringBuilder();
	StringBuilder debugRX = new StringBuilder();

	String portName = null;

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

	HashMap<String, MotorData> motors = new HashMap<String, MotorData>();

	transient Service customEventListener = null;

	class SensorData implements Serializable {
		private static final long serialVersionUID = 1L;
		// -- FIXME - make Sensor(controller?) interface - when we get a new
		// sensor
		public transient UltrasonicSensor sensor = null;
		public int sensorIndex = -1;
		public long duration; // for ultrasonic
	}

	// servos
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

	/**
	 * the local name to servo info
	 */
	HashMap<String, ServoData> servos = new HashMap<String, ServoData>();
	HashMap<Integer, ServoData> servoIndex = new HashMap<Integer, ServoData>();

	public ArrayList<String> portNames = new ArrayList<String>();

	// peers
	transient Serial serial;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		peers.put("serial", "Serial", "serial device for this Arduino");
		return peers;
	}

	public Arduino2(String n) {
		super(n);
		// FIXME - load last serial port (if exists) THIS WILL NEED TO BE Serial
		// serice data !!!
		// FIXME - load board type - allow user to force - (possible to retrieve
		// from connection? - good idea? or bad?)
		//load(); // config - last com port connected too - last rate

		serial = (Serial) Runtime.create("serial", "Serial");
		serial.getPortNames();
		createPinList();
		// invoke("getPortNames"); // FIXME - this could happen before gui
		// initialized - don't do it - waste of time
	}

	public String setBoard(String board) {
		boardType = board;
		return board;
	}

	public String getPortName() {
		if (serial != null) {
			return serial.getPortName();
		}
		return null;
	}

	public void startService() {
		super.startService();
		serial.startService();
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

			if (log.isDebugEnabled()) {
				debugTX.append("sendMsg -> MAGIC_NUMBER|");
				debugTX.append("SZ ").append(1 + params.length);
				debugTX.append(String.format("|FN %d", function));
				for (int i = 0; i < params.length; ++i) {
					if (log.isDebugEnabled()) {
						debugTX.append(String.format("|P%d %d", i, params[i]));
					}
				}
				log.debug(debugTX.toString());
				debugTX.setLength(0);
			}

		} catch (Exception e) {
			error("sendMsg " + e.getMessage());
		}

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

		sendMsg(SET_PWM_FREQUENCY, address, prescalarValue);
	}

	// ----------ServoController begin-------------
	// FIXME - is this re-entrant ???

	public boolean servoAttach(String servoName, Integer pin) {
		Servo servo = (Servo) Runtime.getService(servoName);
		if (servo == null) {
			error("servoAttach can not attach %s no service exists", servoName);
			return false;
		}
		return servoAttach(servo, servo.getPin());
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
			log.warn("servo already attach - detach first");
			return false;
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
		// SERVO_ATTACH (1 byte) | servo index (1 byte) | servo pin (1 byte) | size of name (1 byte) | ASCII name of servo (N - bytes)
		// The name is not needed in MRLComm.ino - but it is needed in virtualized Blender servo
		int payloadSize = 1 + 1 + 1 + servoName.length();
		
		int[] payload = new int[payloadSize];
		
		//payload[0] = SERVO_ATTACH;
		payload[0] = index;
		payload[1] = pin;
		payload[2] = servoName.length();
		
		byte ascii[] = servoName.getBytes();
		for (int i = 0; i < servoName.length(); ++i){
			payload[i + 3] = 0xFF & ascii[i];
		}

		// attach index pin
		//sendMsg(SERVO_ATTACH, index, pin, servoName.length(), servoName.get);
		//sendMsg(function, params)
		sendMsg(SERVO_ATTACH, payload);

		ServoData sd = new ServoData();
		sd.pin = pin;
		sd.servoIndex = index;
		sd.servo = servo;
		servos.put(servo.getName(), sd);
		servoIndex.put(index, sd);

		log.info("servo index {} pin {} attached ", index, pin);
		return true;
	}

	@Override
	public boolean servoDetach(String servoName) {
		Servo servo = (Servo) Runtime.getService(servoName);
		return servoDetach(servo);
	}

	// FIXME make & merge interface for this
	public synchronized boolean servoDetach(Servo servo) {
		String servoName = servo.getName();
		log.info(String.format("servoDetach(%s)", servoName));

		if (servos.containsKey(servoName)) {
			ServoData sd = servos.get(servoName);
			sendMsg(SERVO_DETACH, sd.servoIndex, 0);
			// FIXME - simplify remove
			// sd.servo.setController((ServoController)null);
			// FIXME !!! - DON'T REMOVE !!!
			servos.remove(servoName);
			servoIndex.remove(sd.servoIndex);
			return true;
		}

		error("servo %s detach failed - not found", servoName);
		return false;
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

	@Override
	public void servoSweep(String servoName, int min, int max, int step) {
		if (!servos.containsKey(servoName)) {
			warn("Servo %s not attached to %s", servoName, getName());
			return;
		}
		int index = servos.get(servoName).servoIndex;
		log.info(String.format("servoSweep %s index %d min %d max %d step %d", servoName, index, min, max, step));
		sendMsg(SERVO_SWEEP_START, index, min, max, step);
	}

	// ---------------------------- ServoController End -----------------------
	// ---------------------- Protocol Methods Begin ------------------

	public void digitalWrite(Integer address, Integer value) {
		info("digitalWrite (%d,%d) to %s", address, value, serial.getName());
		sendMsg(DIGITAL_WRITE, address, value);
		pinList.get(address).value = value;
	}

	// FIXME - put in interface - use the last octet
	public Integer getVersion() {
		try {
			blockingData.clear();
			sendMsg(GET_MRLCOMM_VERSION, 0, 0);
			mrlcommVersion = (Integer) blockingData.poll(1000, TimeUnit.MILLISECONDS);

		} catch (Exception e) {
			Logging.logException(e);
		}

		invoke("publishVersion", mrlcommVersion);
		return mrlcommVersion;
	}

	public Integer publishVersion(Integer version) {
		return version;
	}

	public void pinMode(Integer address, Integer value) {
		log.info(String.format("pinMode(%d,%d) to %s", address, value, serial.getName()));
		sendMsg(PINMODE, address, value);
	}

	public void analogWrite(Integer address, Integer value) {
		log.info(String.format("analogWrite(%d,%d) to %s", address, value, serial.getName()));
		// FIXME
		// if (pin.mode == INPUT) {sendMsg(PIN_MODE, OUTPUT)}
		sendMsg(ANALOG_WRITE, address, value);
	}

	/**
	 * This method is called with Pin data whene a pin value is changed on the
	 * Arduino2 board the Arduino2 must be told to poll the desired pin(s). This
	 * is done with a analogReadPollingStart(pin) or digitalReadPollingStart()
	 */
	public Pin publishPin(Pin p) {
		// log.debug(p);
		pinList.get(p.pin).value = p.value;
		return p;
	}

	// force an digital read - data will be published in a call-back
	// TODO - make a serialSendBlocking
	public void digitalReadPollingStart(Integer pin) {
		sendMsg(PINMODE, pin, INPUT);
		sendMsg(DIGITAL_READ_POLLING_START, pin, 0); // last param is not
		// used in read
	}

	public void digitalReadPollingStop(Integer pin) {
		sendMsg(DIGITAL_READ_POLLING_STOP, pin, 0); // last param is not used
		// in read
	}

	// force an analog read - data will be published in a call-back
	// TODO - make a serialSendBlocking
	public void analogReadPollingStart(Integer pin) {
		sendMsg(PINMODE, pin, INPUT);
		sendMsg(ANALOG_READ_POLLING_START, pin, 0); // last param is not used
	}

	public void analogReadPollingStop(Integer pin) {
		sendMsg(ANALOG_READ_POLLING_STOP, pin, 0); // last param is not used
		// in read
	}

	@Override
	public String getDescription() {
		return "This service interfaces with an Arduino micro-controller.";
	}

	public void stopService() {
		super.stopService();
		serial.stopService();
	}

	int error_arduino_to_mrl_rx_cnt;
	int error_mrl_to_arduino_rx_cnt;
	int byteCount;
	int msgSize;
	int[] msg = new int[64]; // TODO define outside

	/**
	 * callback for Serial service - local (not remote)
	 * 
	 * FIXME - design problem of calling the function with a single byte
	 * repeatedly is slow - best to have an "event" then read as many as
	 * possible
	 * 
	 * POLL vs Block ?
	 */

	@Override
	public void onByte(Integer newByte) {
		/*
		 * switch (event.getEventType()) { case SerialDeviceEvent.BI: case
		 * SerialDeviceEvent.OE: case SerialDeviceEvent.FE: case
		 * SerialDeviceEvent.PE: case SerialDeviceEvent.CD: case
		 * SerialDeviceEvent.CTS: case SerialDeviceEvent.DSR: case
		 * SerialDeviceEvent.RI: case SerialDeviceEvent.OUTPUT_BUFFER_EMPTY:
		 * break; case SerialDeviceEvent.DATA_AVAILABLE:
		 */

		// at this point we should have a complete message
		// the msg contains only daat // METHOD | P0 | P1 ... | PN

		// msg buffer
		// msg[0] METHOD
		// msg[1] P0
		// msg[2] P1
		// ...
		// msg[N] PN+1
		try {

			/*
			 * DON'T EVER MAKE THIS NON-MONOLITHIC ! - the overhead of going
			 * into another method is high and will end up loss of data in
			 * serial communications !
			 * 
			 * Optimize this as much as possible !
			 */
			// FIXME - DO NOT USE THIS - USE PUB / SUB !! its more effecient anyway
			while (serial.isOpen() && (newByte = serial.read()) > -1) {

				++byteCount;

				if (byteCount == 1) {
					if (newByte != MAGIC_NUMBER) {
						byteCount = 0;
						msgSize = 0;
						warn(String.format("Arduino2->MRL error - bad magic number %d - %d rx errors", newByte, ++error_arduino_to_mrl_rx_cnt));
						// dump.setLength(0);
					}
					continue;
				} else if (byteCount == 2) {
					// get the size of message
					if (newByte > 64) {
						byteCount = 0;
						msgSize = 0;
						error(String.format("Arduino2->MRL error %d rx sz errors", ++error_arduino_to_mrl_rx_cnt));
						continue;
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

					case MRLCOMM_ERROR: {
						++error_mrl_to_arduino_rx_cnt;
						error("MRL->Arduino2 rx %d type %d", error_mrl_to_arduino_rx_cnt, msg[1]);
						break;
					}

					case GET_MRLCOMM_VERSION: {
						// TODO - get vendor version
						// String version = String.format("%d", msg[1]);
						blockingData.add((int) msg[1] & 0xff);
						invoke("publishVersion", (int) msg[1] & 0xff);
						break;
					}
					case PULSE_IN: {
						// extract signed Java long from byte array offset 1
						// - length 4 :P
						// FIXME dangerous - your re-using Version's
						// blockingData :P
						long pulse = Serial.bytesToLong(msg, 1, 4);
						blockingData.add(pulse);
						break;
					}
					case ANALOG_VALUE: {
						// Pin p = new Pin(msg[1], msg[0], (((msg[2] & 0xFF)
						// << 8) + (msg[3] & 0xFF)), getName());
						// FIXME
						Pin pin = pinList.get(msg[1]);
						pin.value = ((msg[2] & 0xFF) << 8) + (msg[3] & 0xFF);
						invoke("publishPin", pin);
						break;
					}
					case DIGITAL_VALUE: {
						Pin pin = pinList.get(msg[1]);
						pin.value = msg[2];
						invoke("publishPin", pin);
						break;
					}

					case LOAD_TIMING_EVENT: {

						long microsPerLoop = Serial.bytesToLong(msg, 1, 4);
						info("load %d us", microsPerLoop);
						// log.info(String.format(" index %d type %d cur %d target %d",
						// servoIndex, eventType, currentPos & 0xff,
						// targetPos & 0xff));
						// invoke("publishPin", pin);
						break;
					}

					case SERVO_EVENT: {

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

					case SENSOR_DATA: {
						int index = (int) msg[1];
						SensorData sd = sensorsIndex.get(index);
						sd.duration = Serial.bytesToLong(msg, 2, 4);
						// HMM WAY TO GO - is NOT to invoke its own but
						// invoke publishSensorData on Sensor
						// since its its own service
						// invoke("publishSensorData", sd);
						// NICE !! - force sensor to have publishSensorData
						// or publishRange in interface !!!
						// sd.sensor.invoke("publishRange", sd);
						sd.sensor.invoke("publishRange", sd.duration);
						break;
					}

					case STEPPER_EVENT: {

						int eventType = msg[1];
						int index = msg[2];
						int currentPos = (msg[3] << 8) + (msg[4] & 0xff);

						log.info(String.format(" index %d type %d cur %d", index, eventType, currentPos));
						// uber good -
						// TODO - stepper ServoControl interface - not
						// needed Servo is abstraction enough
						Stepper stepper = (Stepper) stepperIndex.get(index);
						stepper.invoke("publishStepperEvent", currentPos);
						break;
					}

					case CUSTOM_MSG: {

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

							// convert
							if (paramType == ARDUINO_TYPE_INT) {
								// params[i] =
								int x = ((msg[++paramIndex] & 0xFF) << 8) + (msg[++paramIndex] & 0xFF);
								if (x > 32767) {
									x = x - 65536;
								}
								params[i] = x;
								log.info(String.format("parameter %d is type ARDUINO_TYPE_INT value %d", i, x));
								++paramIndex;
							} else {
								error("CUSTOM_MSG - unhandled type %d", paramType);
							}

							// load it on boxing array

						}

						// how to reflectively invoke multi-param method
						// (Python?)
						if (customEventListener != null) {
							send(customEventListener.getName(), "onCustomMsg", params);
						}

						break;
					}

					default: {
						error(formatMRLCommMsg("unknown serial event <- ", msg, msgSize));
						break;
					}

					} // end switch

					if (log.isDebugEnabled()) {
						log.debug(formatMRLCommMsg("serialEvent <- ", msg, msgSize));
					}

					// processed msg
					// reset msg buffer
					msgSize = 0;
					byteCount = 0;
				}
			} // while (serial.isOpen() && (newByte =
				// serial.read()) > -1

		} catch (Exception e) {
			++error_mrl_to_arduino_rx_cnt;
			error("msg structure violation %d", error_mrl_to_arduino_rx_cnt);
			// try again ?
			msgSize = 0;
			byteCount = 0;
			Logging.logException(e);
		}

	}

	public String formatMRLCommMsg(String prefix, byte[] message, int size) {
		debugRX.setLength(0);
		if (prefix != null) {
			debugRX.append(prefix);
		}
		debugRX.append(String.format("MAGIC_NUMBER|SZ %d|FN %d", size, message[0]));
		for (int i = 1; i < size; ++i) {
			debugRX.append(String.format("|P%d %d", i, message[i]));
		}
		return debugRX.toString();
	}

	// FIXME !!! - REMOVE ALL BELOW - except compile(File) compile(String)
	// upload(File) upload(String)
	// supporting methods for Compiler & UPloader may be necessary

	static public String getAvrBasePath() {
		Platform platform = Platform.getLocalInstance();
		if (platform.isLinux()) {
			return ""; // avr tools are installed system-wide and in the path
		} else {
			return getHardwarePath() + File.separator + "tools" + File.separator + "avr" + File.separator + "bin" + File.separator;
		}
	}

	static public String getHardwarePath() {
		return getHardwareFolder().getAbsolutePath();
	}

	static public File getHardwareFolder() {
		// calculate on the fly because it's needed by Preferences.init() to
		// find
		// the boards.txt and programmers.txt preferences files (which happens
		// before the other folders / paths get cached).
		return getContentFile("hardware");
	}

	static public File getContentFile(String name) {
		String path = System.getProperty("user.dir");

		// Get a path to somewhere inside the .app folder
		Platform platform = Platform.getLocalInstance();
		if (platform.isMac()) {
			String javaroot = System.getProperty("javaroot");
			if (javaroot != null) {
				path = javaroot;
			}
		}

		path += File.separator + "arduino";

		File working = new File(path);
		return new File(working, name);
	}

	public Serial getSerial() {
		return serial;
	}

	/**
	 * getPortNames returns the current list of free serial ports on the machine
	 * 
	 * @return
	 */
	public ArrayList<String> getPortNames() {
		portNames = SerialDeviceFactory.getSerialDeviceNames();
		return portNames;
	}

	public boolean connect(String name, int rate, int databits, int stopbits, int parity) {
		return serial.connect(name, rate, databits, stopbits, parity);
	}

	public void setCompilingProgress(Integer progress) {
		log.info(String.format("progress %d ", progress));
		invoke("publishCompilingProgress", progress);
	}

	public Integer publishCompilingProgress(Integer progress) {
		return progress;
	}

	public String createBuildPath(String sketchName) {
		// make a work/tmp directory if one doesn't exist - TODO - new time
		// stamp?
		Date d = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
		formatter.setCalendar(cal);

		String tmpdir = String.format("obj%s%s.%s", File.separator, sketchName, formatter.format(d));
		new File(tmpdir).mkdirs();

		return tmpdir;

	}

	@Override
	public ArrayList<Pin> getPinList() {
		return pinList;
	}

	public ArrayList<Pin> createPinList() {
		pinList = new ArrayList<Pin>();
		int pinType = Pin.DIGITAL_VALUE;

		if (boardType != null && boardType.contains("mega")) {
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

	static public String showError(String error, String desc, Exception e) {
		return error;
	}

	public String compilerError(String error) {
		return error;
	}

	public String publishMessage(String msg) {
		return msg;
	}

	public boolean connect() {
		return connect();
	}

	/**
	 * valid means - connected and MRLComm was found at the correct version
	 * 
	 * @return
	 */
	public boolean isValid() {
		return MRLCOMM_VERSION == getVersion();
	}

	public boolean isConnected() {
		// I know not normalized
		// but we have to do this - since
		// the SerialDevice is transient
		return connected;
	}

	public boolean disconnect() {
		return serial.disconnect();
	}

	// ----------- Motor Controller API Begin ----------------

	@Override
	public boolean motorAttach(String motorName, Integer pwrPin, Integer dirPin) {
		return motorAttach(motorName, pwrPin, dirPin, null);
	}

	public boolean motorAttach(String motorName, Integer pwrPin, Integer dirPin, Integer encoderPin) {
		ServiceInterface sw = Runtime.getService(motorName);
		if (!sw.isLocal()) {
			error("motor is not in the same MRL instance as the motor controller");
			return false;
		}
		ServiceInterface service = sw;
		MotorControl motor = (MotorControl) service; 

		MotorData md = new MotorData();
		md.motor = motor;
		md.PWMPin = pwrPin;
		md.dirPin0 = dirPin;
		motors.put(motor.getName(), md);
		motor.setController(this);
		sendMsg(PINMODE, md.PWMPin, OUTPUT);
		sendMsg(PINMODE, md.dirPin0, OUTPUT);
		return true;

	}


	/**
	 * an implementation which supports service names is important there is no
	 * benefit in the object array parameter here all methods should have their
	 * own signature
	 */

	/**
	 * implementation of motorAttach(String motorName, Object... motorData) is
	 * private so that interfacing consistently uses service names to attach,
	 * even though service is local
	 * 
	 * @param motor
	 * @param motorData
	 * @return
	 */
	private boolean motorAttach(MotorControl motor, Object... motorData) {
		if (motor == null || motorData == null) {
			error("null data or motor - can't attach motor");
			return false;
		}

		if (motorData.length != 2 || motorData[0] == null || motorData[1] == null) {
			error("motor data must be of the folowing format - motorAttach(Integer PWMPin, Integer directionPin)");
			return false;
		}

		MotorData md = new MotorData();
		md.motor = motor;
		md.PWMPin = (Integer) motorData[0];
		md.dirPin0 = (Integer) motorData[1];
		motors.put(motor.getName(), md);
		motor.setController(this);
		sendMsg(PINMODE, md.PWMPin, OUTPUT);
		sendMsg(PINMODE, md.dirPin0, OUTPUT);
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

	public void motorMove(String name) {

		MotorData md = motors.get(name);
		MotorControl m = md.motor;
		double power = m.getPowerLevel();

		if (power < 0) {
			sendMsg(DIGITAL_WRITE, md.dirPin0, m.isInverted() ? MOTOR_FORWARD : MOTOR_BACKWARD);
			sendMsg(ANALOG_WRITE, md.PWMPin, Math.abs((int) (255 * m.getPowerLevel())));
		} else if (power > 0) {
			sendMsg(DIGITAL_WRITE, md.dirPin0, m.isInverted() ? MOTOR_BACKWARD : MOTOR_FORWARD);
			sendMsg(ANALOG_WRITE, md.PWMPin, (int) (255 * m.getPowerLevel()));
		} else {
			sendMsg(ANALOG_WRITE, md.PWMPin, 0);
		}
	}

	public void motorMoveTo(String name, Float position) {
		// TODO Auto-generated method stub

	}

	public void digitalDebounceOn() {
		digitalDebounceOn(50);
	}

	public void digitalDebounceOn(int delay) {
		if (delay < 0 || delay > 32767) {
			error(String.format("%d debounce delay must be 0 < delay < 32767", delay));
		}
		int lsb = delay & 0xff;
		int msb = (delay >> 8) & 0xff;
		sendMsg(DIGITAL_DEBOUNCE_ON, msb, lsb);

	}

	public void digitalDebounceOff() {
		sendMsg(DIGITAL_DEBOUNCE_OFF, 0, 0);
	}

	// ----------- MotorController API End ----------------
	
	public void softReset() {
		sendMsg(SOFT_RESET, 0, 0);
	}

	@Override
	public void setServoSpeed(String servoName, Float speed) {
		if (speed == null || speed < 0.0f || speed > 1.0f) {
			error("speed %f out of bounds", speed);
			return;
		}
		sendMsg(SET_SERVO_SPEED, servos.get(servoName).servoIndex, (int) (speed * 100));
	}

	@Override
	public void releaseService() {
		super.releaseService();
		// soft reset - detaches servos & resets polling & pinmodes
		softReset();
		sleep(300);
		disconnect();
	}

	public void setDigitalTriggerOnly(Boolean b) {
		if (b)
			sendMsg(DIGITAL_TRIGGER_ONLY_ON, 0, 0);
		else
			sendMsg(DIGITAL_TRIGGER_ONLY_OFF, 0, 0);

	}

	// -- StepperController begin ----

	@Override
	public void stepperStep(String name, Integer steps) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stepperStep(String name, Integer steps, Integer style) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSpeed(Integer speed) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean stepperDetach(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object[] getStepperData(String stepperName) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean connect(String port) {
		return connect(port, 57600, 8, 1, 0);
	}

	/**
	 * this sets the sample rate of polling reads both digital and analog it is
	 * a loop count modulus - default is 1 which seems to be a bit high of a
	 * rate to be broadcasting across the internet to several webclients :)
	 * valid ranges are 1 to 32,767 (for Arduino2's 2 byte signed integer)
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

	@Override
	public void servoStop(String servoName) {
		// FIXME DEPRECATE OR IMPLEMENT
		// sendMsg(SERVO_STOP_AND_REPORT, servos.get(servoName).servoIndex);
	}

	// often used as a ping echo pulse - timing is critical
	// so it has to be done on the uC .. therefore
	// a trigger pin has to be sent as well to the pulseIn
	// as well as the pulse/echo pin
	public long pulseIn(int trigPin, int echoPin) {
		return pulseIn(trigPin, echoPin, HIGH, 1000);
	}

	public long pulseIn(int trigPin, int echoPin, Integer timeout) {

		if (timeout != null) {
			timeout = 1000;
		}
		return pulseIn(trigPin, echoPin, 1000, null);
	}

	public long pulseIn(int trigPin, int echoPin, int timeout, String highLow) {
		int value = HIGH;
		if (highLow != null && highLow.equalsIgnoreCase("LOW")) {
			value = LOW;
		}
		return pulseIn(trigPin, echoPin, value, timeout);
	}

	// FIXME - rather application specific - possible to add variable delays on
	// trigger
	// and echo
	public long pulseIn(int trigPin, int echoPin, int value, int timeout) {
		try {
			if (serial != null) {
				blockingData.clear();
				sendMsg(PULSE_IN, trigPin, echoPin, value, timeout);
				// downstream longer timeout than upstream
				Long pulse = (Long) blockingData.poll(250 + timeout, TimeUnit.MILLISECONDS);
				if (pulse == null) {
					return 0;
				}
				return pulse;
			} else {
				return 0;
			}

		} catch (Exception e) {
			Logging.logException(e);
			return 0;
		}
	}

	public void pinMode(int address, String mode) {
		if (mode != null && mode.equalsIgnoreCase("INPUT")) {
			pinMode(address, INPUT);
		} else {
			pinMode(address, OUTPUT);
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

	// --- stepper begin ---
	// FIXME - relatively clean interface BUT - ALL THIS LOGIC SHOULD BE IN
	// STEPPER NOT ARDUINO !!!
	// SO STEPPER MUST NEED TO KNOW ABOUT CONTROLLER TYPE

	public boolean stepperAttach(String stepperName) {
		Stepper stepper = (Stepper) Runtime.getService(stepperName);
		if (stepper == null) {
			log.error("Stepper {} not valid", stepperName);
			return false;
		}
		return stepperAttach(stepper);
	}

	public boolean stepperAttach(StepperControl stepper) {
		String stepperName = stepper.getName();
		log.info(String.format("stepperAttach %s", stepperName));
		Integer index = 0;

		if (serial == null) {
			error("could not attach stepper - no serial device!");
			return false;
		}

		if (steppers.containsKey(stepperName)) {
			log.warn("stepper already attach - detach first");
			return false;
		}

		stepper.setController(this);

		if (Stepper.STEPPER_TYPE_POLOLU.equals(stepper.getStepperType())) {
			// int type = Stepper.STEPPER_TYPE_POLOLU.hashCode(); heh, cool idea
			// - but byte collision don't want to risk ;)
			int type = 1;

			// simple count = index mapping
			index = steppers.size();

			Integer[] pins = stepper.getPins();
			if (pins.length != 2) {
				error("Pololu stepper needs 2 pins defined - direction pin & step pin");
				return false;
			}

			// attach index pin
			sendMsg(STEPPER_ATTACH, index, type, pins[0], pins[1]);

			stepper.setIndex(index);

			steppers.put(stepperName, stepper);
			stepperIndex.put(index, stepper);

			log.info(String.format("stepper STEPPER_TYPE_POLOLU index %d pin direction %d step %d attached ", index, pins[0], pins[1]));
		} else {
			error("unkown type of stepper");
			return false;
		}

		return true;
	}

	public String formatMRLCommMsg(String prefix, int[] message, int size) {
		debugRX.setLength(0);
		if (prefix != null) {
			debugRX.append(prefix);
		}
		debugRX.append(String.format("MAGIC_NUMBER|SZ %d|FN %d", size, message[0]));
		for (int i = 1; i < size; ++i) {
			debugRX.append(String.format("|P%d %d", i, message[i]));
		}
		return debugRX.toString();
	}

	public void stepperMove(String name, Integer newPos) {
		if (!steppers.containsKey(name)) {
			error("%s stepper not found", name);
			return;
		}

		StepperControl stepper = steppers.get(name);
		if (Stepper.STEPPER_TYPE_POLOLU.equals(stepper.getStepperType())) {
		} else {
			error("unknown stepper type");
			return;
		}

		int lsb = newPos & 0xff;
		int msb = (newPos >> 8) & 0xff;

		sendMsg(STEPPER_MOVE, stepper.getIndex(), msb, lsb);

		// TODO - call back event - to say arrived ?

		// TODO - blocking method

	}

	// --- stepper end ---

	public void test(String port) throws Exception {

		// get running reference to self
		Arduino2 arduino = (Arduino2) Runtime.start(getName(), "Arduino2");

		boolean useGUI = true;

		if (useGUI) {
			Runtime.createAndStart("gui", "GUIService");
		}

		if (!arduino.connect(port)) {
			throw new MRLError("could not connect to port %s", port);
		}

		for (int i = 0; i < 1000; ++i) {

			long duration = arduino.pulseIn(7, 8);
			log.info("duration {} uS", duration);
			// sleep(100);
		}

		UltrasonicSensor sr04 = (UltrasonicSensor) Runtime.start("sr04", "UltrasonicSensor");
		Runtime.start("gui", "GUIService");

		// sr04.attach(arduino, port, 7, 8);
		sr04.startRanging();

		log.info("here");
		sr04.stopRanging();

		// TODO - test all functions

		// TODO - test digital pin
		// getDigitalPins

		// getAnalogPins

	}

	public boolean setServoEventsEnabled(String servoName, boolean enable) {
		log.info(String.format("setServoEventsEnabled %s %b", servoName, enable));
		if (servos.containsKey(servoName)) {
			ServoData sd = servos.get(servoName);

			if (enable) {
				sendMsg(SERVO_EVENTS_ENABLE, sd.servoIndex, TRUE);
			} else {
				sendMsg(SERVO_EVENTS_ENABLE, sd.servoIndex, FALSE);
			}

			return true;
		}

		return false;
	}

	public boolean setLoadTimingEnabled(boolean enable) {
		log.info(String.format("setLoadTimingEnabled %b", enable));

		if (enable) {
			sendMsg(LOAD_TIMING_ENABLE, TRUE);
		} else {
			sendMsg(LOAD_TIMING_ENABLE, FALSE);
		}

		return enable;
	}

	// String functions to interface are important
	// Interfaces should support both - "real" references and "String"
	// references
	// the String reference just "gets" the real reference - but this is
	// important
	// to support all protocols

	@Override
	public void stepperReset(String stepperName) {
		StepperControl stepper = steppers.get(stepperName);
		sendMsg(STEPPER_RESET, stepper.getIndex());
	}

	public void stepperStop(String name) {
		StepperControl stepper = steppers.get(name);
		sendMsg(STEPPER_STOP, stepper.getIndex());
	}

	public void onCustomMsg(Integer ax, Integer ay, Integer az) {
		log.info("here");
	}

	public static void main(String[] args) {
		try {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			Arduino2 arduino = (Arduino2) Runtime.start("arduino", "Arduino2");
			Python python = (Python) Runtime.start("python", "Python");
			Runtime.start("gui", "GUIService");
			// arduino.addCustomMsgListener(python);
			// arduino.customEventListener = python;
			// arduino.connect("COM15");

			// arduino.test("COM15");

			// blocking examples

			/*
			 * long duration = sr04.ping(); log.info("duration {}", duration);
			 * long range = sr04.range(); log.info("range {}", range);
			 */
			// non blocking - event example
			// sr04.publishRange(long duration);

			// arduino.pinMode(trigPin, "OUTPUT");
			// arduino.pinMode(echoPin, "INPUT");

			// arduino.digitalWrite(7, 0);
			// arduino.digitalWrite(7, 1);
			// arduino.digitalWrite(7, 0);

			// Runtime.createAndStart("python", "Python");
			// Runtime.createAndStart("gui01", "GUIService");
			// arduino.connect("COM15");

			// log.info("{}", arduino.pulseIn(5));

			// FIXME - null pointer error
			log.info("here");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public void addCustomMsgListener(Service service) {
		customEventListener = service;
	}

	@Override
	public void servoWriteMicroseconds(String name, Integer ms) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void motorMoveTo(String name, double position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String[] getCategories() {
		return new String[] {"microcontroller"};
	}
}
