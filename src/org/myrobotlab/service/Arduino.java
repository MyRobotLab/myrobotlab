/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.arduino.PApplet;
import org.myrobotlab.arduino.compiler.AvrdudeUploader;
import org.myrobotlab.arduino.compiler.Compiler;
import org.myrobotlab.arduino.compiler.MessageConsumer;
import org.myrobotlab.arduino.compiler.Preferences;
import org.myrobotlab.arduino.compiler.RunnerException;
import org.myrobotlab.arduino.compiler.Target;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.ToolTip;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceEvent;
import org.myrobotlab.serial.SerialDeviceEventListener;
import org.myrobotlab.serial.SerialDeviceException;
import org.myrobotlab.serial.SerialDeviceFactory;
import org.myrobotlab.serial.SerialDeviceService;
import org.myrobotlab.serial.VirtualSerialPort;
import org.myrobotlab.serial.VirtualSerialPort.VirtualNullModemCable;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SensorDataPublisher;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.StepperControl;
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
 */

public class Arduino extends Service implements SerialDeviceEventListener, SensorDataPublisher, ServoController, MotorController, StepperController, SerialDeviceService,
		MessageConsumer {

	private static final long serialVersionUID = 1L;
	public transient final static Logger log = LoggerFactory.getLogger(Arduino.class);

	// ---------- MRLCOMM FUNCTION INTERFACE BEGIN -----------

	static HashMap<Integer, String> rest = new HashMap<Integer, String>();
	
	public static final int MRLCOMM_VERSION = 20;

	// serial protocol functions
	public static final int MAGIC_NUMBER = 170; // 10101010

	// MRL ---> Arduino methods
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

	public static final int STEPPER_ATTACH	= 44;
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

	// vendor specific pins start at 50
	public static final String VENDOR_DEFINES_BEGIN = "// --VENDOR DEFINE SECTION BEGIN--";
	public static final String VENDOR_SETUP_BEGIN = "// --VENDOR SETUP BEGIN--";
	public static final String VENDOR_CODE_BEGIN = "// --VENDOR CODE BEGIN--";

	/**
	 * pin description of board
	 */
	ArrayList<Pin> pinList = null;
	
	
	// data and mapping for data going from MRL ---to---> Arduino
	HashMap<String, StepperControl> steppers = new HashMap<String, StepperControl>();
	// index for data mapping going from Arduino ---to---> MRL
	HashMap<Integer, StepperControl> stepperIndex = new HashMap<Integer, StepperControl>();

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

	// serial device info
	private transient SerialDevice serialDevice;

	// from Arduino IDE (yuk)
	static HashSet<File> libraries;

	static boolean commandLine;
	public HashMap<String, Target> targetsTable = new HashMap<String, Target>();

	static File buildFolder;
	static public HashMap<String, File> importToLibraryTable;

	// imported Arduino constants
	public static final int HIGH = 0x1;
	public static final int LOW = 0x0;

	public static final int INPUT = 0x0;
	public static final int OUTPUT = 0x1;

	public static final int MOTOR_FORWARD = 1;
	public static final int MOTOR_BACKWARD = 0;

	private boolean connected = false;
	private String boardType;

	BlockingQueue<Object> blockingData = new LinkedBlockingQueue<Object>();
	
	StringBuilder debugTX = new StringBuilder();
	StringBuilder debugRX = new StringBuilder();

	HashMap<String, Motor> motors = new HashMap<String, Motor>();
	HashMap<Integer, String> encoderPins = new HashMap<Integer, String>();
	
	transient Service customEventListener = null;
	
	String filenameRX;
	boolean isRXRecording = true;
	transient FileWriter fileWriterRX = null;
	transient BufferedWriter bufferedWriterRX = null;
	String rxFileFormat;


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

	// from the Arduino IDE :P
	public Preferences preferences;
	transient Compiler compiler;
	transient AvrdudeUploader uploader;

	// compile / upload
	private String buildPath = "";
	private String sketchName = "MRLComm";
	private String sketch = "";

	/**
	 * list of serial port names from the system which the Arduino service is
	 * running - this list is refreshed on querySerialDevices
	 */
	public ArrayList<String> portNames = new ArrayList<String>();

	public Arduino(String n) {
		super(n);
		//load(); put in Service

		// target arduino
		// board atmenga328
		preferences = new Preferences(String.format("%s.preferences.txt", getName()), null);
		preferences.set("sketchbook.path", ".myrobotlab");

		preferences.setInteger("serial.debug_rate", 57600);
		preferences.set("serial.parity", "N"); // f'ing stupid,
		preferences.setInteger("serial.databits", 8);
		preferences.setInteger("serial.stopbits", 1); // f'ing weird 1,1.5,2
		preferences.setBoolean("upload.verbose", true);

		File librariesFolder = getContentFile("libraries");

		// FIXME - all below should be done inside Compiler2
		try {

			targetsTable = new HashMap<String, Target>();
			loadHardware(getHardwareFolder());
			loadHardware(getSketchbookHardwareFolder());
			addLibraries(librariesFolder);
			File sketchbookLibraries = getSketchbookLibrariesFolder();
			addLibraries(sketchbookLibraries);
		} catch (IOException e) {
			Logging.logException(e);
		}

		compiler = new Compiler(this);
		uploader = new AvrdudeUploader(this);

		getPortNames();

		// FIXME - hilacious long wait - need to incorporate
		// .waitTillServiceReady
		// especially if there are multiple initialization threads
		// SWEEEET ! - Service already provides an isReady - just need to
		// overload it with a Thread.sleep check -> broadcast setState

		createPinList();

		String filename = "MRLComm.ino";
		String resourcePath = String.format("Arduino/%s", filename);
		log.info(String.format("loadResourceFile %s", resourcePath));
		String defaultSketch = FileIO.resourceToString(resourcePath);
		this.sketch = defaultSketch;
	}

	public void setBoard(String board) {
		boardType = board;
		preferences.set("board", board);
		createPinList();
		preferences.save();
		broadcastState();
	}

	protected void loadHardware(File folder) {
		if (!folder.isDirectory())
			return;

		String list[] = folder.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				// skip .DS_Store files, .svn folders, etc
				if (name.charAt(0) == '.')
					return false;
				if (name.equals("CVS"))
					return false;
				return (new File(dir, name).isDirectory());
			}
		});
		// if a bad folder or something like that, this might come back null
		if (list == null)
			return;

		// alphabetize list, since it's not always alpha order
		// replaced hella slow bubble sort with this feller for 0093
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);
		// after that lovely searching of dirs - will come back with
		// [arduino, tools]

		for (String target : list) {
			File subfolder = new File(folder, target);
			targetsTable.put(target, new Target(target, subfolder, this));
		}
	}

	public void setPreference(String name, String value) {
		preferences.set(name, value);
		if ("board".equals(name)) {
			broadcastState();
		}
	}

	public String getSerialDeviceName() {
		if (serialDevice != null) {
			return serialDevice.getName();
		}

		return null;
	}

	// FIXME - this should be in SerialService interface - get rid of query!!!
	/*
	 * public ArrayList<String> getPortNames() { return
	 * SerialDeviceFactory.getSerialDeviceNames(); }
	 */
	// FIXME - this should be in SerialService interface !!!
	/*
	 * public ArrayList<String> querySerialDeviceNames() {
	 * 
	 * log.info("queryPortNames");
	 * 
	 * serialDeviceNames = SerialDeviceFactory.getSerialDeviceNames();
	 * 
	 * // adding connected serial port if connected if (serialDevice != null) {
	 * if (serialDevice.getName() != null)
	 * serialDeviceNames.add(serialDevice.getName()); }
	 * 
	 * return serialDeviceNames; }
	 */

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

			// FIXME - determin bus speed e.g. 50Khz - and limit the messages
			// (buffer them?) until they
			// are under that max message send value :P

			// not CRC16 - but cheesy error correction of bytestream
			// http://www.java2s.com/Open-Source/Java/6.0-JDK-Modules-sun/misc/sun/misc/CRC16.java.htm
			// #include <util/crc16.h>
			// _crc16_update (test, testdata);

			serialDevice.write(MAGIC_NUMBER);

			// msg size = function byte + x param bytes
			// msg size does not include MAGIC_NUMBER & size
			// MAGIC_NUMBER|3|FUNCTION|PARAM0|PARAM2 would be valid
			serialDevice.write(1 + params.length);

			serialDevice.write(function);

			for (int i = 0; i < params.length; ++i) {
				serialDevice.write(params[i]);
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

	@ToolTip("sends an array of data to the serial port which an Arduino is attached to")
	@Override
	public void write(String data) throws IOException {
		write(data.getBytes());
	}

	public synchronized void write(byte[] data) throws IOException {
		for (int i = 0; i < data.length; ++i) {
			serialDevice.write(data[i]);
		}
	}

	@Override
	public void write(int data) throws IOException {
		serialDevice.write(data);
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

	public boolean servoAttach(String servoName) {
		Servo servo = (Servo) Runtime.getService(servoName);
		if (servo == null) {
			error("servoAttach can not attach %s no service exists", servoName);
			return false;
		}
		return servoAttach(servoName, servo.getPin());
	}

	// FIXME - put in interface
	public boolean servoAttach(Servo servo) {
		if (servo == null) {
			error("servoAttach servo is null");
			return false;
		}

		if (servo.getPin() == null) {
			error("%s servo pin not set", servo.getName());
			return false;
		}
		return servoAttach(servo.getName(), servo.getPin());
	}

	@Override
	public synchronized boolean servoAttach(String servoName, Integer pin) {
		log.info(String.format("servoAttach %s pin %d", servoName, pin));

		if (serialDevice == null) {
			error("could not attach servo to pin " + pin + " serial port is null - not initialized?");
			return false;
		}

		if (servos.containsKey(servoName)) {
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
		ServoControl sc = (ServoControl)Runtime.getService(servoName);
		sd.servo = sc;
		servos.put(servoName, sd);
		servoIndex.put(index, sd);

		log.info("servo index {} pin {} attached ", index, pin);
		return true;
	}

	@Override
	public synchronized boolean servoDetach(String servoName) {
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
		if (serialDevice == null) {
			error("serialDevice is NULL !");
			return;
		}

		if (!servos.containsKey(servoName)) {
			warn("Servo %s not attached to %s", servoName, getName());
			return;
		}

		int index = servos.get(servoName).servoIndex;

		log.info(String.format("servoWrite %s %d index %d", servoName, newPos, index));

		sendMsg(SERVO_WRITE, index, newPos);

	}
	
	public void servoWriteMicroseconds(String servoName, Integer newPos) {
		if (serialDevice == null) {
			error("serialDevice is NULL !");
			return;
		}

		if (!servos.containsKey(servoName)) {
			warn("Servo %s not attached to %s", servoName, getName());
			return;
		}

		int index = servos.get(servoName).servoIndex;

		log.info(String.format("writeMicroseconds %s %d index %d", servoName, newPos, index));

		sendMsg(SERVO_WRITE_MICROSECONDS, index, newPos);

	}

	@Override
	public void servoSweep(String servoName, int min, int max, int step) { // delay
																			// /
																			// speed
																			// ?
																			// -
																			// or
																			// is
																			// speed
																			// same
																			// as
																			// delay
		if (serialDevice == null) {
			error("serialDevice is NULL !");
			return;
		}

		if (!servos.containsKey(servoName)) {
			warn("Servo %s not attached to %s", servoName, getName());
			return;
		}

		int index = servos.get(servoName).servoIndex;

		log.info(String.format("servoSweep %s index %d min %d max %d step %d", servoName, index, min, max, step));

		sendMsg(SERVO_SWEEP_START, index, min, max, step);

	}

	/*
	 * @Override public Integer getServoPin(String servoName) { if
	 * (servos.containsKey(servoName)) { return servos.get(servoName).pin; }
	 * return null; }
	 */

	// ---------------------------- ServoController End -----------------------
	// ---------------------- Protocol Methods Begin ------------------

	public void digitalReadPollStart(Integer address) {

		log.info("digitalRead (" + address + ") to " + serialDevice.getName());
		sendMsg(DIGITAL_READ_POLLING_START, address, 0);

	}

	public void digitalReadPollStop(Integer address) {

		log.info("digitalRead (" + address + ") to " + serialDevice.getName());
		sendMsg(DIGITAL_READ_POLLING_STOP, address, 0);

	}

	public void digitalWrite(Integer address, Integer value) {
		if (serialDevice == null){
			error("serial device is null");
			return;
		}
		log.info("digitalWrite (" + address + "," + value + ") to " + serialDevice.getName() + " function number " + DIGITAL_WRITE);
		sendMsg(DIGITAL_WRITE, address, value);
		pinList.get(address).value = value;
	}

	public Integer getVersion() {
		try {
			if (serialDevice != null) {
				blockingData.clear();
				sendMsg(GET_MRLCOMM_VERSION, 0, 0);

				mrlcommVersion = (Integer) blockingData.poll(1000, TimeUnit.MILLISECONDS);

				if (mrlcommVersion == null) {
					error("did not get response from arduino....");
				} else if (!mrlcommVersion.equals(MRLCOMM_VERSION)) {
					error(String.format("MRLComm.ino responded with version %s expected version is %s", mrlcommVersion, MRLCOMM_VERSION));
				} else {
					info(String.format("connected %s responded version %s ... goodtimes...", serialDevice.getName(), mrlcommVersion));
				}
				
				// int version = Integer.parseInt();
				return mrlcommVersion;
			} else {
				return null;
			}

		} catch (Exception e) {
			Logging.logException(e);
			return null;
		}
	}

	public Integer publishVersion(Integer version) {
		return version;
	}

	public void pinMode(Integer address, Integer value) {
		log.info("pinMode (" + address + "," + value + ") to " + serialDevice.getName() + " function number " + PINMODE);
		sendMsg(PINMODE, address, value);
	}

	public void analogWrite(Integer address, Integer value) {
		log.info("analogWrite (" + address + "," + value + ") to " + serialDevice.getName() + " function number " + ANALOG_WRITE);
		sendMsg(ANALOG_WRITE, address, value);
	}

	/**
	 * This method is called with Pin data whene a pin value is changed on the
	 * Arduino board the Arduino must be told to poll the desired pin(s). This
	 * is done with a analogReadPollingStart(pin) or digitalReadPollingStart()
	 */
	public Pin publishPin(Pin p) {
		// log.debug(p);
		pinList.get(p.pin).value = p.value;
		if (encoderPins.containsKey(p.pin)){
			motors.get(encoderPins.get(p.pin)).setCurrentPos((double)p.value);
		}
		return p;
	}

	public String readSerialMessage(String s) {
		return s;
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
		if (serialDevice != null) {
			serialDevice.close();
		}
	}

	static final public int MAX_MSG_LEN = 64;
	//StringBuilder rxDebug = new StringBuilder ();

	// TODO - define as int[] because Java bytes suck !
	byte[] msg = new byte[64]; // TODO define outside
	int newByte;
	int byteCount = 0;
	int msgSize = 0;

	//StringBuffer dump = new StringBuffer();

	
	private String portName = "";
	
	private int rate = 57600;
	
	private int databits = 8;
	
	private int stopbits = 1;
	
	private int parity = 0;
	private int error_arduino_to_mrl_rx_cnt;
	private int error_mrl_to_arduino_rx_cnt;

	@Override
	public void serialEvent(SerialDeviceEvent event) {
		switch (event.getEventType()) {
		case SerialDeviceEvent.BI:
		case SerialDeviceEvent.OE:
		case SerialDeviceEvent.FE:
		case SerialDeviceEvent.PE:
		case SerialDeviceEvent.CD:
		case SerialDeviceEvent.CTS:
		case SerialDeviceEvent.DSR:
		case SerialDeviceEvent.RI:
		case SerialDeviceEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialDeviceEvent.DATA_AVAILABLE:

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
				while (serialDevice.isOpen() && (newByte = serialDevice.read()) > -1) {

					/*
					if (isRXRecording) {
						bufferedWriterRX.write(newByte);
					}
					*/
					
					++byteCount;

					if (byteCount == 1) {
						if (newByte != MAGIC_NUMBER) {
							byteCount = 0;
							msgSize = 0;
							warn(String.format("Arduino->MRL error - bad magic number %d - %d rx errors", newByte, ++error_arduino_to_mrl_rx_cnt));
							//dump.setLength(0);
						}
						continue;
					} else if (byteCount == 2) {
						// get the size of message
						if (newByte > 64) {
							byteCount = 0;
							msgSize = 0;
							error(String.format("Arduino->MRL error %d rx sz errors", ++error_arduino_to_mrl_rx_cnt));
							continue;
						}
						msgSize = (byte) newByte;
						// dump.append(String.format("MSG|SZ %d", msgSize));
					} else if (byteCount > 2) {
						// remove header - fill msg data - (2) headbytes -1
						// (offset)
						// dump.append(String.format("|P%d %d", byteCount,
						// newByte));
						msg[byteCount - 3] = (byte) newByte;
					}

					// process valid message
					if (byteCount == 2 + msgSize) {
						// log.error("A {}", dump.toString());
						// dump.setLength(0);

						// MSG CONTENTS = FN | D0 | D1 | ...
						byte function = msg[0];
						//log.info(String.format("%d", msg[1]));
						switch (function) {

						case MRLCOMM_ERROR: {
							++error_mrl_to_arduino_rx_cnt;
							error("MRL->Arduino rx %d type %d", error_mrl_to_arduino_rx_cnt, msg[1]);
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
							long pulse = Serial.bytesToUnsignedInt(msg, 1, 4);
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

							long microsPerLoop = Serial.bytesToUnsignedInt(msg, 1, 4);
							info("load %d us", microsPerLoop);
							// log.info(String.format(" index %d type %d cur %d target %d", servoIndex, eventType, currentPos & 0xff, targetPos & 0xff));
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
							// TODO - deprecate ServoControl interface - not needed Servo is abstraction enough
							Servo servo = (Servo) servoIndex.get(index).servo;
							servo.invoke("publishServoEvent", currentPos  & 0xff);
							break;
						}
						

						case SENSOR_DATA: {
							int index = (int) msg[1];
							SensorData sd = sensorsIndex.get(index);
							sd.duration = Serial.bytesToUnsignedInt(msg, 2, 4);
							// HMM WAY TO GO - is NOT to invoke its own but
							// invoke publishSensorData on Sensor
							// since its its own service
							// invoke("publishSensorData", sd);
							// NICE !! - force sensor to have publishSensorData
							// or publishRange in interface !!!
							// sd.sensor.invoke("publishRange", sd);
//							if (sd.duration != 0){
								sd.sensor.invoke("publishRange", sd.duration);
//							}
							break;
						}

						case STEPPER_EVENT: {

							int eventType = msg[1];
							int index = msg[2];
							int currentPos = (msg[3] << 8) + (msg[4] & 0xff);

							log.info(String.format(" index %d type %d cur %d", index, eventType, currentPos));
							// uber good - 
							// TODO - stepper ServoControl interface - not needed Servo is abstraction enough
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
							
							for (int i = 0; i < paramCnt; ++i){
								
								// get parameter type
								//paramType = msg[];
								paramType = msg[paramIndex];
										
								// convert
								if (paramType == ARDUINO_TYPE_INT){
									//params[i] =
									int x = ((msg[++paramIndex] & 0xFF) << 8) + (msg[++paramIndex] & 0xFF);
									if (x > 32767){ x = x - 65536; }
									params[i] = x;
									if (log.isDebugEnabled()){
										log.debug(String.format("parameter %d is type ARDUINO_TYPE_INT value %d", i, x));
									}
									++paramIndex;
								} else {
									error("CUSTOM_MSG - unhandled type %d", paramType);
								}
								
								// load it on boxing array
								
							}
							
							// how to reflectively invoke multi-param method (Python?)
							if (customEventListener != null){
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
				} // while (serialDevice.isOpen() && (newByte =
					// serialDevice.read()) > -1

			} catch (Exception e) {
				++error_mrl_to_arduino_rx_cnt;
				error("msg structure violation %d", error_mrl_to_arduino_rx_cnt);
				// try again ?
				msgSize = 0;
				byteCount = 0;
				Logging.logException(e);
			}

		}

	}
	
		
	public String formatMRLCommMsg(String prefix, byte[] message, int size){
		debugRX.setLength(0);
		if (prefix != null){
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

	public Map<String, String> getBoardPreferences() {
		Target target = getTarget();
		if (target == null)
			return new LinkedHashMap();
		Map map = target.getBoards();
		if (map == null)
			return new LinkedHashMap();
		map = (Map) map.get(preferences.get("board"));
		if (map == null)
			return new LinkedHashMap();
		return map;
	}

	public Target getTarget() {
		return targetsTable.get(preferences.get("target"));
	}

	public HashMap<String, Target> getTargetsTable() {
		return targetsTable;
	}

	public String getSketchbookLibrariesPath() {
		return getSketchbookLibrariesFolder().getAbsolutePath();
	}

	public File getSketchbookHardwareFolder() {
		return new File(getSketchbookFolder(), "hardware");
	}

	protected File getDefaultSketchbookFolder() {
		File sketchbookFolder = null;
		try {
			sketchbookFolder = new File("./.myrobotlab");// platform.getDefaultSketchbookFolder();
		} catch (Exception e) {
		}

		// create the folder if it doesn't exist already
		boolean result = true;
		if (!sketchbookFolder.exists()) {
			result = sketchbookFolder.mkdirs();
		}

		if (!result) {
			showError("You forgot your sketchbook", "Arduino cannot run because it could not\n" + "create a folder to store your sketchbook.", null);
		}

		return sketchbookFolder;
	}

	public File getSketchbookLibrariesFolder() {
		return new File(getSketchbookFolder(), "libraries");
	}

	public File getSketchbookFolder() {
		return new File(preferences.get("sketchbook.path"));
	}

	public File getBuildFolder() {
		if (buildFolder == null) {
			String buildPath = preferences.get("build.path");
			if (buildPath != null) {
				buildFolder = new File(buildPath);

			} else {
				// File folder = new File(getTempFolder(), "build");
				// if (!folder.exists()) folder.mkdirs();
				buildFolder = createTempFolder("build");
				buildFolder.deleteOnExit();
			}
		}
		return buildFolder;
	}

	static public File createTempFolder(String name) {
		try {
			File folder = File.createTempFile(name, null);
			// String tempPath = ignored.getParent();
			// return new File(tempPath);
			folder.delete();
			folder.mkdirs();
			return folder;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void removeDescendants(File dir) {
		if (!dir.exists())
			return;

		String files[] = dir.list();
		for (int i = 0; i < files.length; i++) {
			if (files[i].equals(".") || files[i].equals(".."))
				continue;
			File dead = new File(dir, files[i]);
			if (!dead.isDirectory()) {
				if (!preferences.getBoolean("compiler.save_build_files")) {
					if (!dead.delete()) {
						// temporarily disabled
						System.err.println("Could not delete " + dead);
					}
				}
			} else {
				removeDir(dead);
				// dead.delete();
			}
		}
	}

	/**
	 * Remove all files in a directory and the directory itself.
	 */
	public void removeDir(File dir) {
		if (dir.exists()) {
			removeDescendants(dir);
			if (!dir.delete()) {
				System.err.println("Could not delete " + dir);
			}
		}
	}

	/**
	 * Return an InputStream for a file inside the Processing lib folder.
	 */
	static public InputStream getLibStream(String filename) throws IOException {
		return new FileInputStream(new File(getContentFile("lib"), filename));
	}

	static public void saveFile(String str, File file) throws IOException {
		File temp = File.createTempFile(file.getName(), null, file.getParentFile());
		PApplet.saveStrings(temp, new String[] { str });
		if (file.exists()) {
			boolean result = file.delete();
			if (!result) {
				throw new IOException("Could not remove old version of " + file.getAbsolutePath());
			}
		}
		boolean result = temp.renameTo(file);
		if (!result) {
			throw new IOException("Could not replace " + file.getAbsolutePath());
		}
	}

	public static boolean isCommandLine() {
		return commandLine;
	}

	protected boolean addLibraries(File folder) throws IOException {
		if (!folder.isDirectory())
			return false;

		String list[] = folder.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				// skip .DS_Store files, .svn folders, etc
				if (name.charAt(0) == '.')
					return false;
				if (name.equals("CVS"))
					return false;
				return (new File(dir, name).isDirectory());
			}
		});
		// if a bad folder or something like that, this might come back null
		if (list == null)
			return false;

		// alphabetize list, since it's not always alpha order
		// replaced hella slow bubble sort with this feller for 0093
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

		boolean ifound = false;

		// reset the set of libraries
		libraries = new HashSet<File>();
		// reset the table mapping imports to libraries
		importToLibraryTable = new HashMap<String, File>();

		for (String libraryName : list) {
			File subfolder = new File(folder, libraryName);

			libraries.add(subfolder);
			String packages[] = Compiler.headerListFromIncludePath(subfolder.getAbsolutePath());
			for (String pkg : packages) {
				importToLibraryTable.put(pkg, subfolder);
			}

			ifound = true;
		}
		return ifound;
	}

	public String showMessage(String msg, String desc) {
		log.info("showMessage " + msg);
		return msg;
	}

	public SerialDevice getSerialDevice() {
		return serialDevice;
	}

	@Override
	public ArrayList<String> getPortNames() {
		// FIXME - is this inclusive or ones which are left ?????
		portNames = SerialDeviceFactory.getSerialDeviceNames();
		return portNames;
	}

	@Override
	public boolean connect(String name, int rate, int databits, int stopbits, int parity) {
		if (name == null || name.length() == 0) {
			log.info("got emtpy connect name - disconnecting");
			return disconnect();
		}

		if (isConnected()) {
			if (portName != null && portName.equals(name)) {
				log.info(String.format("%s already connected", portName));
				return true;
			} else if (portName != null && !portName.equals(name)) {
				warn("requesting port change from %s to %s - disconnecting", portName, name);
				disconnect();
			}
		}

		try {
			info("attempting to connect %s %d %d %d %d", name, rate, databits, stopbits, parity);
			// LAME - this should be done in the constructor or don't get with
			// details ! - this data makes no diff
			serialDevice = SerialDeviceFactory.getSerialDevice(name, rate, databits, stopbits, parity);
			if (serialDevice != null) {
				this.portName = name; // serialDevice.getName();
				this.rate = rate;
				this.databits = databits;
				this.stopbits = stopbits;
				this.parity = parity;

				// connect();
				// MOVE ALL VALUES TO @ELEMENTS
				// ----------------begin connect ----------------------------
				message(String.format("\nconnecting to serial device %s\n", serialDevice.getName()));

				if (!serialDevice.isOpen()) {
					serialDevice.open();
					serialDevice.addEventListener(this);
					serialDevice.notifyOnDataAvailable(true);
					serialDevice.setParams(rate, databits, stopbits, parity);
					sleep(2000);

					// String version = null;

				} else {
					warn(String.format("\n%s is already open, close first before opening again\n", serialDevice.getName()));
				}

				connected = true;
				// -----------------end connect -------

				save(); // successfully bound to port - saving
				preferences.set("serial.port", portName);
				// FIXME - normalize
				preferences.save();
				broadcastState(); // state has changed let everyone know
				invoke("getVersion");
				return true;
			}
		} catch (Exception e) {
			logException(e);
		}

		error("could not connect %s to port %s", getName(), name);
		return false;
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

	public void compile(String sketchName, String sketch) {
		// FYI - not thread safe
		this.sketchName = sketchName;
		this.sketch = sketch;
		this.buildPath = createBuildPath(sketchName);

		try {
			compiler.compile(sketchName, sketch, buildPath, true);
		} catch (RunnerException e) {
			logException(e);
			invoke("compilerError", e.getMessage());
		}
		log.debug(sketch);
	}

	public void upload(String sketch) throws Throwable {
		compile("MRLComm", sketch); // FIXME throw push error();
		uploader.uploadUsingPreferences(buildPath, sketchName, false);
	}

	public void uploadFile(String filename) throws Throwable {
		upload(FileIO.fileToString(filename));
	}

	/**
	 * Get the number of lines in a file by counting the number of newline
	 * characters inside a String (and adding 1).
	 */
	static public int countLines(String what) {
		int count = 1;
		for (char c : what.toCharArray()) {
			if (c == '\n')
				count++;
		}
		return count;
	}

	/**
	 * Grab the contents of a file as a string.
	 */
	static public String loadFile(File file) throws IOException {
		String[] contents = PApplet.loadStrings(file);
		if (contents == null)
			return null;
		return PApplet.join(contents, "\n");
	}

	@Override
	public ArrayList<Pin> getPinList() {
		return pinList;
	}

	public ArrayList<Pin> createPinList() {
		pinList = new ArrayList<Pin>();
		boardType = preferences.get("board");
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

	@Override
	public void message(String msg) {
		log.info(msg);
		invoke("publishMessage", msg);
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
		return connect(portName, rate, databits, stopbits, parity);
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

	@Override
	public boolean disconnect() {
		connected = false;
		if (serialDevice == null) {
			return false;
		}

		serialDevice.close();
		portName = "";

		info("disconnected");
		broadcastState();
		return true;
	}

	public String getPortName() {
		return portName;
	}

	// ----------- motor controller api begin ----------------

	@Override
	public boolean motorAttach(String motorName, Integer pwrPin, Integer dirPin) {
		return motorAttach(motorName, pwrPin, dirPin, null);
	}

	public boolean motorAttach(String motorName, Integer pwmPin, Integer dirPin, Integer encoderPin) {
		Motor motor = (Motor)Runtime.getService(motorName);
		if (!motor.isLocal()) {
			error("motor is not in the same MRL instance as the motor controller");
			return false;
		}
		
		motor.pwmPin = pwmPin;
		motor.dirPin = dirPin;
		motors.put(motor.getName(), motor);
		motor.setController(this);
		
		if (encoderPin != null){
			motor.encoderPin = encoderPin;
			//sendMsg(PINMODE, md.encoderPin, INPUT);
			encoderPins.put(motor.encoderPin, motor.getName());
			analogReadPollingStart(motor.encoderPin);
		}
		
		sendMsg(PINMODE, motor.pwmPin, OUTPUT);
		sendMsg(PINMODE, motor.dirPin, OUTPUT);
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

	public void motorMove(String name) {

		Motor motor = motors.get(name);
		double powerLevel = motor.getPowerLevel();
		
		sendMsg(DIGITAL_WRITE, motor.dirPin, (powerLevel < 0)? MOTOR_BACKWARD : MOTOR_FORWARD);
		sendMsg(ANALOG_WRITE, motor.pwmPin, Math.abs((int)(powerLevel)));
	}
	
	@Override // add speed?
	public void motorMoveTo(String name, double position) {
		// default speed
	}

	// ----------- motor controller api end ----------------
	
	
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

	public String getSketch() {
		return this.sketch;
	}

	public String setSketch(String newSketch) {
		sketch = newSketch;
		return sketch;
	}

	public String loadSketchFromFile(String filename) throws FileNotFoundException {
		sketch = FileIO.fileToString(filename);
		return sketch;
	}


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

	public void setSerialRate(int rate) {
		try {
			log.info(String.format("setSerialRate %d", rate));
			sendMsg(SET_SERIAL_RATE, rate, 0);
			serialDevice.setParams(rate, 8, 1, 0);
		} catch (SerialDeviceException e) {
			logException(e);
		}
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

	/**
	 * connect to serial port with default parameters 57600 rate, 8 data bits, 1
	 * stop bit, 0 parity
	 */
	@Override
	public boolean connect(String port) {
		return connect(port, 57600, 8, 1, 0);
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

	@Override
	public void servoStop(String servoName) {
		// FIXME prolly should stop speed controlled movement as well as sweep
		sendMsg(SERVO_SWEEP_STOP, servos.get(servoName).servoIndex);
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
			if (serialDevice != null) {
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

	@Override
	public int read() throws IOException {
		return serialDevice.read();
	}

	@Override
	public int read(byte[] data) throws IOException {
		return serialDevice.read(data);
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

		if (serialDevice == null) {
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
	// FIXME - relatively clean interface BUT - ALL THIS LOGIC SHOULD BE IN STEPPER NOT ARDUINO !!!
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

		if (serialDevice == null) {
			error("could not attach stepper - no serial device!");
			return false;
		}

		if (steppers.containsKey(stepperName)) {
			log.warn("stepper already attach - detach first");
			return false;
		}

		stepper.setController(this);
		
		if (Stepper.STEPPER_TYPE_POLOLU.equals(stepper.getStepperType())) {
			// int type = Stepper.STEPPER_TYPE_POLOLU.hashCode(); heh, cool idea - but byte collision don't want to risk ;)
			int type = 1;
			
			// simple count = index mapping
			index = steppers.size();
			
			Integer [] pins = stepper.getPins();
			if (pins.length != 2){
				error("Pololu stepper needs 2 pins defined - direction pin & step pin");
				return false;
			}

			// attach index pin
			sendMsg(STEPPER_ATTACH, index, type, pins[0], pins[1]);
			
			stepper.setIndex(index);

			steppers.put(stepperName, stepper);
			stepperIndex.put(index, stepper);

			log.info(String.format("stepper STEPPER_TYPE_POLOLU index %d pin direction %d step %d attached ", index,  pins[0], pins[1]));
		} else {
			error("unkown type of stepper");
			return false;
		}

		return true;
	}
	

	public void stepperMove(String name, Integer newPos) {
		if (!steppers.containsKey(name)){
			error("%s stepper not found", name);
			return;
		}
		
		StepperControl stepper = steppers.get(name);
		if (Stepper.STEPPER_TYPE_POLOLU.equals(stepper.getStepperType())){
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

	public Status test() {

		Status status = Status.info("starting %s %s test", getName(), getType());

		// get running reference to self
		Arduino arduino = (Arduino)Runtime.start(getName(),"Arduino");
		
		boolean useGUI = false;	
		if (useGUI){
			Runtime.createAndStart("gui", "GUIService");
		}
		
		// create virtual null modem cable
		String port = "COM99";
		String uartPort = "UART99";
		
		VirtualNullModemCable nullModem = VirtualSerialPort.createNullModemCable(port, uartPort);
		Serial uart = (Serial)Runtime.start(uartPort, "Serial");
		uart.recordRX(String.format("%s.rx", uartPort));
		uart.recordTX(String.format("%s.tx", uartPort));
		
		// set board type
		// FIXME - this should be done by MRLComm.ino (compiled in)
		status.addInfo("setting board type to %s", BOARD_TYPE_ATMEGA2560);
		arduino.setBoard(BOARD_TYPE_ATMEGA2560);
	
		if (!arduino.connect(port)) {
			status.addError("could not conntect %s on %s", getName(), port);
		}
		
		ArrayList<Pin> pinList = getPinList();
		status.addInfo("found %d pins", pinList.size());
		
		for (int i = 0; i < pinList.size(); ++i){
			arduino.analogWrite(pinList.get(i).pin, 0);
			arduino.analogWrite(pinList.get(i).pin, 128);
			arduino.analogWrite(pinList.get(i).pin, 255);
		}
		
		for (int i = 0; i < pinList.size(); ++i){
			arduino.digitalWrite(pinList.get(i).pin, 1);
			arduino.digitalWrite(pinList.get(i).pin, 0);
		}
		
		arduino.disconnect();
		nullModem.close();
		

		for (int i = 0; i < 10; ++i) {
			long duration = arduino.pulseIn(7, 8);
			log.info("duration {} uS", duration);
		}

		UltrasonicSensor sr04 = (UltrasonicSensor) Runtime.start("sr04", "UltrasonicSensor");
		Runtime.start("gui", "GUIService");

		sr04.attach(arduino, port, 7, 8);
		sr04.startRanging();

		log.info("here");
		sr04.stopRanging();

		// TODO - test all functions

		// TODO - test digital pin
		// getDigitalPins

		// getAnalogPins
		
		return status;

	}
	
	public boolean setServoEventsEnabled(String servoName, boolean enable){
		log.info(String.format("setServoEventsEnabled %s %b", servoName, enable));
		if (servos.containsKey(servoName)) {
			ServoData sd = servos.get(servoName);
			
			if (enable){
				sendMsg(SERVO_EVENTS_ENABLE, sd.servoIndex, TRUE);
			} else {
				sendMsg(SERVO_EVENTS_ENABLE, sd.servoIndex, FALSE);				
			}
			
			return true;
		}
		
		return false;
	}

	public boolean setLoadTimingEnabled(boolean enable){
		log.info(String.format("setLoadTimingEnabled %b", enable));
		
			if (enable){
				sendMsg(LOAD_TIMING_ENABLE, TRUE);
			} else {
				sendMsg(LOAD_TIMING_ENABLE, FALSE);				
			}
			
		return enable;
	}
	
	
	// String functions to interface are important
	// Interfaces should support both - "real" references and "String" references
	// the String reference just "gets" the real reference - but this is important
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
	
	public void onCustomMsg(Integer ax, Integer ay, Integer az){
		log.info("here");
	}

	

	public void addCustomMsgListener(Service service) {
		customEventListener = service;
	}

	public boolean recordRX(String filename) {
		try {

			if (filename == null) {
				filenameRX = String.format("rx.%s.%d.data", getName(), System.currentTimeMillis());
			} else {
				//filenameTX = filename;
			}

			if (fileWriterRX == null) {
				fileWriterRX = new FileWriter(filenameRX);
				bufferedWriterRX = new BufferedWriter(fileWriterRX);
			}

			isRXRecording = true;
			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}
	
	public boolean stopRecording() {
		try {
			isRXRecording = false;
		

			if (fileWriterRX != null) {
				bufferedWriterRX.close();
				fileWriterRX.close();
				fileWriterRX = null;
				bufferedWriterRX = null;
			}


			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;

	}
	
	public void addByteListener(SerialDataListener service){
		
	}

	public String getBoardType() {		
		return boardType;
	}
	
	public static void main(String[] args) {
		try {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			Runtime.start("gui", "GUIService");
			//arduino.test();
			
			/*
			Python python = (Python) Runtime.start("python", "Python");
			Runtime.start("gui", "GUIService");
			*/
			//arduino.addCustomMsgListener(python);
			//arduino.customEventListener = python;
			//arduino.connect("COM15");
			
			//arduino.test("COM15");

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

	@Override
	public int available() {
		// TODO Auto-generated method stub
		return 0;
	}



}
