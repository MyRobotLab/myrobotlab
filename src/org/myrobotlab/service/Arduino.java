package org.myrobotlab.service;

import static org.myrobotlab.codec.serial.ArduinoMsgCodec.ANALOG_WRITE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.CONTROLLER_ATTACH;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DEVICE_ATTACH;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DEVICE_DETACH;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DEVICE_TYPE_ARDUINO;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DEVICE_TYPE_I2C;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DEVICE_TYPE_MOTOR;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DEVICE_TYPE_NEOPIXEL;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DEVICE_TYPE_SERVO;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DEVICE_TYPE_ULTRASONIC;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DIGITAL_WRITE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DISABLE_BOARD_STATUS;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DISABLE_PIN;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.DISABLE_PINS;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.ENABLE_BOARD_STATUS;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.ENABLE_PIN;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.GET_VERSION;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.HEARTBEAT;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.I2C_READ;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.I2C_WRITE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.I2C_WRITE_READ;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.MAGIC_NUMBER;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.MAX_MSG_SIZE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.MRLCOMM_VERSION;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.MSG_ROUTE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.NEO_PIXEL_SET_ANIMATION;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.NEO_PIXEL_WRITE_MATRIX;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PIN_MODE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_ATTACHED_DEVICE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_BOARD_INFO;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_BOARD_STATUS;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_DEBUG;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_MESSAGE_ACK;
///// java static import definition - DO NOT MODIFY - Begin //////
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_MRLCOMM_ERROR;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_PULSE_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_SENSOR_DATA;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_SERVO_EVENT;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PUBLISH_VERSION;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PULSE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.PULSE_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SENSOR_POLLING_START;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SENSOR_POLLING_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_ATTACH;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_DETACH;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_SET_MAX_VELOCITY;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_SET_SPEED;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_SWEEP_START;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_SWEEP_STOP;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_WRITE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SERVO_WRITE_MICROSECONDS;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_DEBOUNCE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_DEBUG;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_DIGITAL_TRIGGER_ONLY;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_PWMFREQUENCY;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_SAMPLE_RATE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_SERIAL_RATE;
import static org.myrobotlab.codec.serial.ArduinoMsgCodec.SET_TRIGGER;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.arduino.ArduinoUtils;
import org.myrobotlab.arduino.MrlMsg;
import org.myrobotlab.codec.serial.ArduinoMsgCodec;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.i2c.I2CBus;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.motor.MotorConfig;
import org.myrobotlab.motor.MotorConfigDualPwm;
import org.myrobotlab.motor.MotorConfigPulse;
import org.myrobotlab.motor.MotorConfigSimpleH;
import org.myrobotlab.service.data.DeviceMapping;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.data.SensorData;
import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.I2CBusControl;
import org.myrobotlab.service.interfaces.I2CBusController;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.Microcontroller;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.NeoPixelControl;
import org.myrobotlab.service.interfaces.NeoPixelController;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PinListener;
import org.myrobotlab.service.interfaces.SensorControl;
import org.myrobotlab.service.interfaces.SensorController;
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

public class Arduino extends Service implements Microcontroller, PinArrayControl, I2CBusController, I2CController, SerialDataListener, ServoController, MotorController,
		NeoPixelController, SensorDataPublisher, DeviceController, SensorController, SensorDataListener {

	/**
	 * BoardInfo is all info which needs to be published once after connection
	 */
	public static class BoardInfo {
		public Integer type;

		public BoardInfo(Integer type) {
			this.type = type;
		}
	}

	/**
	 * Status data for the running MRLComm sketch. This data will be returned
	 * from the sketch to Java-land to report on the speed and current free
	 * memory of the Microcontroller
	 */
	public static class BoardStatus {
		public Long us;
		public Integer sram;
		public Integer deviceCount; // deviceList with types
		// FIXME - list of current devices ids & their types ?
		// ie the deviceList
		// List<Integer, Integer>

		public BoardStatus(Long us, Integer sram, Integer deviceCount) {
			this.us = us;
			this.sram = sram;
			this.deviceCount = deviceCount;
		}
	}

	public static class I2CDeviceMap {
		public int busAddress;
		public int deviceAddress;
		public I2CControl control;
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

	private static final long serialVersionUID = 1L;

	public transient final static Logger log = LoggerFactory.getLogger(Arduino.class);

	public static final int TRUE = 1;

	public static final int FALSE = 0;

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
	 * With the new upload method, Gael need to have support for ADK Mega
	 *
	 */

	// Java-land defintion
	public transient static final String BOARD_TYPE_UNO = "uno";
	public transient static final String BOARD_TYPE_MEGA = "mega";
	public transient static final String BOARD_TYPE_MEGA_ADK = "megaADK";

	// MrlComm definition
	public transient static final int BOARD_TYPE_ID_UNKNOWN = 0;
	public transient static final int BOARD_TYPE_ID_MEGA = 1;
	public transient static final int BOARD_TYPE_ID_UNO = 2;
	public transient static final int BOARD_TYPE_ID_ADK_MEGA = 3;

	public static final int HIGH = 0x1;
	public static final int LOW = 0x0;
	public static final int INPUT = 0x0;
	public static final int OUTPUT = 0x1;

	public static final int MOTOR_FORWARD = 1;

	public static final int MOTOR_BACKWARD = 0;

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
				arduino.enablePin(7);
			}

			if (testMotor) {
				Motor motor = (Motor) Runtime.createAndStart("motor", "Motor");
				// motor.setType2Pwm(leftPwm, rightPwm);
				// motor.attach(arduino);
				// arduino.attachDevice(motor, null);
				while (true) {
					// try to overrun?
					// rand between -1 and 1.
					double rand = (Math.random() - 0.5) * 2;
					motor.move(rand);
				}
			} else {
				Servo servo = (Servo) Runtime.createAndStart("servo", "Servo");
				servo.attach(arduino, 10);
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
	 * path of the Arduino IDE must be set by user
	 */
	public String arduinoPath;

	public Sketch sketch;
	public String uploadSketchResult;

	Integer mrlCommVersion = null;

	/**
	 * number of ms to pause after sending a message to the Arduino
	 */
	public int delay = 0;

	/**
	 * board type - UNO Mega etc..
	 * 
	 * if the user 'connects' first then the info could come from the board ..
	 * but if the user wants to upload first a npe will be thrown so we default
	 * it here to Uno
	 */
	public String boardType = "Uno";

	/**
	 * pin named map of all the pins on the board
	 */
	Map<String, PinDefinition> pinMap = null;
	/**
	 * the definitive sequence of pins - "true address"
	 */
	Map<Integer, PinDefinition> pinIndex = null;

	transient Map<String, PinArrayListener> pinArrayListeners = new HashMap<String, PinArrayListener>();

	/**
	 * map of pin listeners
	 */
	transient Map<Integer, List<PinListener>> pinListeners = new HashMap<Integer, List<PinListener>>();

	/**
	 * the map of pins which the pin listeners are listening too - if the set is
	 * null they are listening to "any" published pin
	 */
	Map<String, Set<Integer>> pinSets = new HashMap<String, Set<Integer>>();

	/**
	 * Devices - string name index of device we need 2 indexes for sensors
	 * because they will be referenced by name OR by index
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
	public int msgSize;

	transient int[] msg = new int[MAX_MSG_SIZE];
	// i2c This needs to be volatile because it will be updated in a different
	// threads
	volatile boolean i2cDataReturned = false;
	volatile byte[] i2cData = new byte[64];
	volatile int i2cDataSize;

	/**
	 * <pre>
	 * Hi Mats : I would recommend using the deviceList instead of creating another
	 * map here .. the deviceList contains DeviceMapping & DeviceMappings have Object[] config
	 * you can add your I2CDeviceMap to config - because it would be "config" for your
	 * DeviceControl.DEVICE_TYPE_I2C
	 * 
	 * I think it might be could to pull as much I2C definitions as possible - what I mean to say
	 * is I2C is its "own" thing - for example I2CDeviceMap exists currently in both Arduino & Raspi and
	 * its the same definition - it deserves to exist on its own (perhaps in its own package name?) and
	 * Arduino & RasPi services should share that definition..
	 * 
	 * A I2CDeviceController interface should be defined too ..
	 * the signatures would follow the same pattern as all DeviceController & DeviceControl interfaces..
	 * 
	 * You already have a I2CControl which should be derived from DeviceControl..
	 * 
	 * You needs an I2CController (derived from DeviceController) -
	 * 		the controllers purpose is to implement the low level details so I2Control methods can be done
	 * 
	 * I2Control methods are :
	 *  	void createI2cDevice(int busAddress, int deviceAddress); // pure i2c no serviceName
	 *  	void releaseI2cDevice(int busAddress, int deviceAddress);
	 *  	void i2cWrite(int busAddress, int deviceAddress, byte[] buffer, int size);
	 *  	int i2cRead(int busAddress, int deviceAddress, byte[] buffer, int size);
	 * 
	 * I2CController methods could be
	 *  	void createI2cDevice(I2Control control, int busAddress, int deviceAddress); // don't need serviceName - you have the whole service in "control" parameter
	 *  	void releaseI2cDevice(I2Control control, int busAddress, int deviceAddress);
	 *  	void i2cWrite(I2Control control, int busAddress, int deviceAddress, byte[] buffer, int size);
	 *  	int i2cRead(I2Control control, int busAddress, int deviceAddress, byte[] buffer, int size);
	 * 
	 * The I2CControl method implementation would be potentially very simple - they would just call the controller's method with
	 * "this" as a first parameter... e.g.
	 * 
	 * 		void createI2cDevice(int busAddress, int deviceAddress, String serviceName){
	 * 					controller.createI2cDevice(this, busAddress, deviceAddress)
	 * 		}
	 * 
	 * This pattern follows MotorController / MotorControl & ServoController / ServoControl .. & Soon to be
	 * PinArrayController / PinArrayControl, SerialController / SerialControl
	 * 
	 * This abstraction is structured enough to follow, yet necessary to provide the implementation differences between say
	 * RasPi & Arduino for I2C ... or Serial control .. e.g. using a Serial service to read & write over USB vs Using Arduino
	 * to create and relay serial reads & writes over a different set of pins (Mega pins 14-19)
	 * 
	 * The plans I have for SerialControl & SerialController (not yet defined - but similar to I2cControll & I2cController) is
	 * SerialControl provides basic reads & writes - the SerialController provides the same methods, but with the SerialControl
	 * as the first parameter - Arduino would be a service which implemented both SerialControl & SerialController
	 * 
	 * A service which "needed" Serial control .. say a NeoPixel Ring would allow attachment to a Serial service directly through
	 * FTDI (https://github.com/tdicola/Adafruit_NeoPixel_FTDI)  or attachment to an Arduino Mega on pin 14 & 15.
	 * The business logic of driving the NeoPixel with serial commands would be in the NeoPixel service.
	 * 
	 *  GroG
	 * 
	 *  Comments on the above from Mats
	 *  Hi GroG.
	 * 
	 *  Thanks for writing and explaining about the responsibilities for the different interfaces.
	 *  I understand now that I have made a mistake when implementing I2CControl in RasPi, Ardino and I2cMux.
	 *  They all should implement I2CController
	 *  The services for i2c devices like Adafruit16CServoDriver, AdafruitIna219, I2cMux and Mpu6050 should implement I2CControl.
	 *  I will rework that so that everything follows the same pattern.
	 * 
	 *  About using the deviceList.
	 *  My understanding was/is that adding a device to the devicelist also would create a corresponding device in MRLComm.
	 *  Since a single i2c bus may contain as many as 127 addressable devices that would potentially use a lot of memory in MRLComm.
	 *  So I want to create a new device that represents a single I2CBus and that device should be added to the devicelist and also be created in
	 *  MRLComm.
	 *  That is the reason that I also created a new i2cDevices list to keep track of the different i2c devices.
	 * 
	 *  If we can add the i2c devices to the devicelist without creating a device in MRLComm, then that's perhaps a better way.
	 *  In that case we still need a i2cbus device that will be a MrlI2CDevice object.
	 *  To make it clear what it actually represents, I would like to rename it to MrlI2cBus.
	 * 
	 *  About I2CDeviceMap.
	 *  If we can add the i2c devices to the devicelist without creating a device in MRLComm then I2CDeviceMap isn't needed at all
	 *  in this Arduino service. So that's one option that needs to be explored / discussed.
	 * 
	 *  If that's not possible, then an other option is to keep the i2cDevices list using I2CDeviceMap.
	 *  I tried to use the same definition of it in both RasPi and Arduino.
	 *  However the I2CDeviceMap in RasPi is based on definitions in pi4j.
	 *  In the RasPi service both I2CBus and I2CDevice are objects defined in pi4j.
	 *  So I redefined I2CDeviceMap in this Arduino service to only use Strings, not objects.
	 *  They are not the same in this service and RasPi even if they share the same names.
	 * 
	 *  The third option is to keep the i2cDevices list but use DeviceMapping.
	 * 
	 *  I hope that I have been able to explain what I have done and the reasons for it.
	 *  So what way do you think is the best? Other people are also welcome to express their opinions.
	 * 
	 *  About I2CControl.
	 *  Since I2CControl should be the methods on the high level, I don't think that
	 *  the low level I2CController methods should be replicated.
	 *  It's one thing to use the I2CController method calls, something completley
	 *  different to implement them.
	 *  For example Arduino and RasPi implements the methods defined in the I2CController interface.
	 *  Adafruit16CServoController and Mpu6050 use the methods in the I2CController interface.
	 *  So I agree on the methods in I2CController, but not on the I2CControl methods that you suggested.
	 *  For example, I don't want a Python script create an Adafruit16CServoDriver and then
	 *  use Adafruit16CServoDriver.i2cWrite(...) to directly make i2c reads and writes.
	 *  That would be really strange.
	 *  I'm fine with a Python script using Arduino or RasPi to do i2c reads and writes since it
	 *  is defined in the I2CController interface.
	 *
	 * </pre>
	 */

	I2CBus i2cBus;

	HashMap<String, I2CDeviceMap> i2cDevices = new HashMap<String, I2CDeviceMap>();

	// ---------------------------- ServoController End -----------------------
	// ---------------------- Protocol Methods Begin ------------------

	// parameters for testing the getVersion retry stuff.
	// TODO: some way to do this more synchronously
	// perhaps when we connect to the serial port, MRLComm can just have the
	// version waiting?
	public int retryMax = 3;
	public int retryConnectDelay = 1500;

	// make sure this is sync'd across threads,
	private volatile boolean ackRecieved = false;

	private int numAck = 0;

	int pinEventsDefaultRate = 8000;

	int publishBoardStatusModulus = 1000;

	transient Arduino rootController = null;
	public static final int MRL_IO_NOT_DEFINED = 0;
	public static final int MRL_IO_SERIAL_0 = 1;
	public static final int MRL_IO_SERIAL_1 = 2;
	public static final int MRL_IO_SERIAL_2 = 3;
	public static final int MRL_IO_SERIAL_3 = 4;
	public transient int controllerAttachAs = MRL_IO_NOT_DEFINED;
	transient HashMap<Integer, Arduino> attachedController = new HashMap<Integer, Arduino>();
	
	boolean heartbeat = false;

	public Arduino(String n) {
		super(n);
		serial = (Serial) createPeer("serial");
		createPinList();
		String mrlcomm = FileIO.resourceToString("Arduino/MrlComm/MRLComm.ino");
		setSketch(new Sketch("MRLComm", mrlcomm));

		// add self as an attached device
		// to handle pin events and other base
		// Arduino methods
		DeviceMapping map = new DeviceMapping(this, (Object[]) null);
		map.setId(0);
		deviceList.put(getName(), map);
		deviceIndex.put(0, map);

	}

	/**
	 * stop the selected pin from polling analog reads
	 *
	 * @param pin
	 */
	/*
	 * public void analogReadPollingStop(int pin) { // int actualPin =
	 * fixPinOffset(pin); // TODO: look up which sensor it is. and tell tell the
	 * sensor to stop // instead. // that should call a sensor.stop on the
	 * controller maybe?! // sendMsg(ANALOG_READ_POLLING_STOP, actualPin); }
	 */

	public void analogWrite(int address, int value) {
		log.info(String.format("analogWrite(%d,%d)", address, value));
		// FIXME
		// if (pin.mode == INPUT) {sendMsg(PIN_MODE, OUTPUT)}
		sendMsg(ANALOG_WRITE, address, value);
	}

	/**
	 * attach a pin listener who listens to a specific pin FIXME - implement the
	 * 'specific' pin
	 */
	@Override
	public void attach(PinListener listener, int address) {
		String name = listener.getName();

		if (listener.isLocal()) {
			List<PinListener> list = null;
			if (pinListeners.containsKey(address)) {
				list = pinListeners.get(address);
			} else {
				list = new ArrayList<PinListener>();
			}
			list.add(listener);
			pinListeners.put(address, list);

		} else {
			// setup for pub sub
			// FIXME - there is an architectual problem here
			// locally it works - but remotely - outbox would need to know
			// specifics of
			// the data its sending
			addListener("publishPin", name, "onPin");
		}
	}

	/**
	 * attach a pin listener which listens for an array of all active pins
	 */
	@Override
	public void attach(PinArrayListener listener) {
		pinArrayListeners.put(listener.getName(), listener);
	}

	/**
	 * String interface - this allows you to easily use url api requests like
	 * /attach/nameOfListener/3
	 */
	@Override
	public void attach(String listener, int address) {
		attach((PinListener) Runtime.getService(listener), address);
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
	 */
	@Override
	public void connect(String port, int rate, int databits, int stopbits, int parity) {

		try {

			serial.open(port, rate, databits, stopbits, parity);

			Integer version = getVersion();
			if (version == null || version != MRLCOMM_VERSION) {
				error("MRLComm expected version %d actual is %d", MRLCOMM_VERSION, version);
				return;
			}

		} catch (Exception e) {
			log.error("serial open threw", e);
			error(e.getMessage());
		}

		broadcastState();
	}

	// this allow to connect a controller to another controller with Serial1,
	// Serial2, Serial3 on a mega board
	public void connect(Arduino controller, String serialPort) throws IOException {
		if (controller == null) {
			error("setting null as controller");
			return;
		}
		if (controller == this) {
			error("controller can't attach to itself");
			return;
		}
		if (!controller.boardType.toLowerCase().contains("mega")) {
			error("You must connect to a Mega controller");
			return;
		}
		rootController = controller;
		// connect("COM15");
		serial = rootController.serial;
		switch (serialPort) {
		case "Serial1":
			controllerAttachAs = MRL_IO_SERIAL_1;
			break;
		case "Serial2":
			controllerAttachAs = MRL_IO_SERIAL_2;
			break;
		case "Serial3":
			controllerAttachAs = MRL_IO_SERIAL_3;
			break;
		default:
			error("Unknow serial port");
			return;
		}
		controller.controllerAttach(this, controllerAttachAs);
		softReset();
		Integer version = getVersion();
		if (version == null || version != MRLCOMM_VERSION) {
			error("MRLComm expected version %d actual is %d", MRLCOMM_VERSION, version);
		}
		broadcastState();
	}

	public void controllerAttach(Arduino controller, int serialPort) {
		attachedController.put(serialPort, controller);
		MrlMsg msg = new MrlMsg(CONTROLLER_ATTACH);
		msg.addData(serialPort);
		sendMsg(msg);
	}

	@Override
	public void createI2cDevice(I2CControl control, int busAddress, int deviceAddress) {
		// TODO Auto-generated method stub - I2C
		// Create the i2c bus device in MRLComm the first time this method is
		// invoked.
		// Add the i2c device to the list of i2cDevices
		// Pattern: deviceAttach(device, Object... config)
		// To add the i2c bus to the deviceList I need an device that represents
		// the i2c bus here and in MRLComm
		// This will only handle the creation of i2cBus.
		if (i2cBus == null) {
			i2cBus = new I2CBus(String.format("I2CBus%s", busAddress));
		}
		deviceAttach(i2cBus, getMrlDeviceType(i2cBus), busAddress);

		// This part adds the service to the mapping between
		// busAddress||DeviceAddress
		// and the service name to be able to send data back to the invoker
		String key = String.format("%d.%d", busAddress, deviceAddress);
		I2CDeviceMap devicedata = new I2CDeviceMap();
		if (i2cDevices.containsKey(key)) {
			log.error(String.format("Device %s %s %s already exists.", busAddress, deviceAddress, control.getName()));
		} else {
			devicedata.busAddress = busAddress;
			devicedata.deviceAddress = deviceAddress;
			devicedata.control = control;
			i2cDevices.put(key, devicedata);
		}
	}

	@Override
	public void releaseI2cDevice(I2CControl control, int busAddress, int deviceAddress) {
		// TODO Auto-generated method stub
		// This method should delete the i2c device entry from the list of
		// I2CDevices
		String key = String.format("%d.%d", busAddress, deviceAddress);
		if (i2cDevices.containsKey(key)) {
			i2cDevices.remove(key);
		}
	}

	public Map<String, PinDefinition> createPinList() {
		pinMap = new HashMap<String, PinDefinition>();
		pinIndex = new HashMap<Integer, PinDefinition>();

		if (boardType != null && boardType.toLowerCase().contains("mega")) {
			for (int i = 0; i < 70; ++i) {
				PinDefinition pindef = new PinDefinition();
				String name = null;
				if (i == 0) {
					pindef.setRx(true);
				}
				if (i == 1) {
					pindef.setTx(true);
				}
				if (i < 1 || (i > 13 && i < 54)) {
					name = String.format("D%d", i);
					pindef.setDigital(true);
				} else if (i > 53) {
					name = String.format("A%d", i - 54);
					pindef.setAnalog(true);
				} else {
					name = String.format("D%d", i);
					pindef.setPwm(true);
				}
				pindef.setName(name);
				pindef.setAddress(i);
				pinMap.put(name, pindef);
				pinIndex.put(i, pindef);
			}
		} else {
			for (int i = 0; i < 20; ++i) {
				PinDefinition pindef = new PinDefinition();
				String name = null;
				if (i == 0) {
					pindef.setRx(true);
				}
				if (i == 1) {
					pindef.setTx(true);
				}
				if (i < 14) {
					name = String.format("D%d", i);
					pindef.setDigital(true);
				} else {
					pindef.setAnalog(true);
					name = String.format("A%d", i - 14);
				}
				if (i == 3 || i == 5 || i == 6 || i == 9 || i == 10 || i == 11) {
					pindef.setPwm(true);
					name = String.format("D%d", i);
				}
				pindef.setName(name);
				pindef.setAddress(i);
				pinMap.put(String.format("A%d", i), pindef);
				pinIndex.put(i, pindef);
			}
		}
		return pinMap;
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
	 * the micro controller message format for ATTACH_DEVICE will be:
	 *
	 * <pre>
	 *
	 * MSG STRUCTURE
	 *                    |<-- ioCmd starts here                                        |<-- config starts here
	 * MAGIC_NUMBER|LENGTH|ATTACH_DEVICE|DEVICE_TYPE|NAME_SIZE|NAME .... (N)|CONFIG_SIZE|DATA0|DATA1 ...|DATA(N)
	 * 
	 * 
	 * ATTACH_DEVICE - this method id
	 * DEVICE_TYPE - the mrlcomm device type we are attaching
	 * NAME_SIZE - the size of the name of the service of the device we are attaching
	 * NAME .... (N) - the name data
	 * CONFIG_SIZE - the size of the folloing config
	 * DATA0|DATA1 ...|DATA(N) - config data
	 *
	 * </pre>
	 *
	 * ATTACH_DEVICE - this method id DEVICE_TYPE - the mrlcomm device type we
	 * are attaching NAME_SIZE - the size of the name of the service of the
	 * device we are attaching NAME .... (N) - the name data CONFIG_SIZE - the
	 * size of the folloing config DATA0|DATA1 ...|DATA(N) - config data
	 *
	 * @param device
	 */
	@Override
	public synchronized void deviceAttach(DeviceControl device, Object... config) {

		String name = device.getName();

		// check to see if we are already attached as the device controller
		// btw - this potentially will be a problem if its operating in a
		// different
		// process - the controller will probably be transient
		DeviceController dc = device.getController();
		if (dc == this) {
			log.info(String.format("%s already attached at device level - nothing to do", device.getName()));
			return;
		}

		// check to see if this device is already attached
		if (this != device.getController()) {
			device.setController(this);
		}

		// ??
		if (deviceList.containsKey(name)) {
			DeviceMapping map = deviceList.get(name);
			if (map.getId() == null) {
				log.error("oh no !  the device %s record is in place, but we do not have a device id - something wrong in mrlcomm?", name);
				log.error("mrlcomm is in an unknown state");
				throw new IllegalArgumentException("half attached device - name already defined, no id");
			}
		}

		// get the device type int so mrl knows what type of device
		// to create
		int mrlDeviceType = getMrlDeviceType(device);
		int nameSize = name.length();

		// FIXME - total length test - if name overruns - throw

		info("attaching device %s of type %d", name, mrlDeviceType);

		int deviceConfigSize = 0;
		if (config != null) {
			deviceConfigSize = config.length;
		}

		// ArrayList<Integer> msgParms = new int[deviceConfigSize + 2];
		List<Integer> msgBody = new ArrayList<Integer>();
		// ATTACH_DEVICE|DEVICE_TYPE|NAME_SIZE|NAME ....
		// (N)|CONFIG_MSG_SIZE|DATA0|DATA1 ...|DATA(N)

		// create msg payload for the specific device in MRLComm
		msgBody.add(mrlDeviceType); // DEVICE_TYPE

		msgBody.add(nameSize); // NAME_SIZE
		for (int i = 0; i < nameSize; ++i) {
			msgBody.add((int) name.charAt(i));
		}

		msgBody.add(deviceConfigSize); // CONFIG_MSG_SIZE

		// move the device config into the msg
		// DATA0|DATA1 ...|DATA(N)
		if (config != null) {
			for (int i = 0; i < config.length; ++i) {
				msgBody.add((int) config[i]);
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

		sendMsg(DEVICE_ATTACH, msgBody);

	}

	@Override
	public void deviceDetach(DeviceControl device) {
		sendMsg(DEVICE_DETACH, getDeviceId(device));
	}

	/**
	 * silly Arduino implementation - but keeping it since its familiar
	 * 
	 * @param address
	 * @param value
	 */
	public void digitalWrite(int address, int value) {
		sendMsg(DIGITAL_WRITE, address, value);
		PinDefinition pinDef = pinIndex.get(address);
		invoke("publishPinDefinition", pinDef);
	}

	public void disablePin(int address) {
		if (!isConnected()) {
			error("must be connected to disable pins");
			return;
		}
		MrlMsg msg = new MrlMsg(DISABLE_PIN);
		msg.addData(address);
		sendMsg(msg);
		PinDefinition pinDef = pinIndex.get(address);
		invoke("publishPinDefinition", pinDef);
	}

	public void disablePins() {
		MrlMsg msg = new MrlMsg(DISABLE_PINS);
		sendMsg(msg);
	}

	public void disconnect() {
		mrlCommVersion = null;
		for (Arduino controller : attachedController.values()) {
			controller.disconnect();
		}
		attachedController.clear();
		if (controllerAttachAs != MRL_IO_NOT_DEFINED) {
			controllerAttachAs = MRL_IO_NOT_DEFINED;
			serial = (Serial) createPeer("serial");
		} else {
			serial.disconnect();
		}
		broadcastState();
	}

	public void enableBoardStatus() {
		enableBoardStatus(publishBoardStatusModulus);
	}

	public void enableBoardStatus(int rate) {
		this.publishBoardStatusModulus = rate;
		MrlMsg msg = new MrlMsg(ENABLE_BOARD_STATUS);
		msg.addData16(rate);
		sendMsg(msg);
	}

	public void sendMsg(MrlMsg msg) {
		sendMsg(msg.getMethod(), msg.getList());
	}

	public void disableBoardStatus() {
		sendMsg(DISABLE_BOARD_STATUS);
	}

	/**
	 * start polling reads of selected pin
	 *
	 * @param pin
	 */
	public void enablePin(int address) {
		if (!isConnected()) {
			error("must be connected to enable pins");
			return;
		}
		MrlMsg msg = new MrlMsg(ENABLE_PIN);
		msg.addData(address); // ANALOG 1 DIGITAL 0
		PinDefinition pin = pinIndex.get(address);
		
		msg.addData(getMrlPinType(pin)); // pinType
		// TODO - make this Hz so everyone is happy :)
		// msg.addData16(rate); //
		sendMsg(msg);
		
		pin.setEnabled(true);
		invoke("publishPinDefinition", pin);
	}

	/**
	 * int type to describe the pin defintion to Pin.h 0 digital 1 analog
	 * 
	 * @param pin
	 * @return
	 */
	public Integer getMrlPinType(PinDefinition pin) {
		if (boardType == null) {
			error("must have pin board type to determin pin definition");
			return null;
		}

		if (pin.isAnalog()) {
			return 1;
		}

		return 0;
	}

	@Override
	public String getBoardType() {
		return boardType;
	}

	@Override
	public DeviceController getController() {
		return this;
	}

	Integer getDeviceId(DeviceControl device) {
		return getDeviceId(device.getName());
	}

	Integer getDeviceId(String name) {
		if (deviceList.containsKey(name)) {
			Integer id = deviceList.get(name).getId();
			if (id == null) {
				error("cannot get device id for %s - device attempetd to attach - but I suspect something went wrong", name);
			}
			return id;
		}
		log.error("getDeviceId could not find device {}", name);
		return null;
	}

	/**
	 * int identifier for MrlTypeDevice - this has to be in sync with MRLComm's
	 * type ids
	 *
	 * @param device
	 * @return
	 */
	private Integer getMrlDeviceType(DeviceControl device) {

		// FIXME - this will be need to be more type specific
		if (device instanceof MotorControl) {
			return DEVICE_TYPE_MOTOR;
		}

		if (device instanceof Arduino) {
			return DEVICE_TYPE_ARDUINO;
		}

		// FixMe this does not follow spec..
		// of Control Controller
		if (device instanceof UltrasonicSensor) {
			return DEVICE_TYPE_ULTRASONIC;
		}

		if (device instanceof Servo) {
			return DEVICE_TYPE_SERVO;
		}

		if (device instanceof I2CBusControl) {
			return DEVICE_TYPE_I2C;
		}

		if (device instanceof NeoPixelControl) {
			return DEVICE_TYPE_NEOPIXEL;
		}

		throw new IllegalArgumentException(String.format("a mrl device type for %s of type %s could not be found ", device.getName(), device.getClass().getCanonicalName()));
	}

	@Override
	public List<PinDefinition> getPinList() {
		List<PinDefinition> list = new ArrayList<PinDefinition>(pinIndex.values());
		return list;
	}

	public String getPortName() {
		return serial.getPortName();
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
			log.error("could not get version", e);
		}
		if (mrlCommVersion == null) {
			error(String.format("%s did not get response from arduino....", serial.getPortName()));
		} else if (!mrlCommVersion.equals(MRLCOMM_VERSION)) {
			error(String.format("MRLComm.ino responded with version %s expected version is %s", mrlCommVersion, MRLCOMM_VERSION));
		} else {
			info(String.format("%s connected on %s responded version %s ... goodtimes...", serial.getName(), serial.getPortName(), mrlCommVersion));
		}

		invoke("publishVersion", mrlCommVersion);

		return mrlCommVersion;
	}

	@Override
	public int i2cRead(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {
		i2cDataReturned = false;
		// Get the device index to the MRL i2c bus
		String i2cBus = String.format("I2CBus%s", busAddress);
		DeviceMapping map;
		map = deviceList.get(i2cBus);
		int id = map.getId(); // Device index to the I2CBus
		int msgBuffer[] = new int[3];
		msgBuffer[0] = id;
		msgBuffer[1] = deviceAddress;
		msgBuffer[2] = size;
		sendMsg(I2C_READ, msgBuffer);
		int retry = 0;
		int retryMax = 1000; // ( About 1000ms = s)
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
			log.debug(String.format("i2cReturnData returned %s bytes to caller %s.", i2cDataSize, control.getName()));
			for (int i = 0; i < i2cDataSize; i++) {
				buffer[i] = i2cData[i];
				log.debug(String.format("i2cReturnData returned ix %s value %s", i, buffer[i]));
			}
			return i2cDataSize;
		}
		// Time out, no data returned
		return -1;
	}

	@Override
	public void i2cWrite(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {
		String i2cBus = String.format("I2CBus%s", busAddress);
		DeviceMapping deviceMapping = deviceList.get(i2cBus);
		int id = deviceMapping.getId();
		int msgBuffer[] = new int[size + 3];
		msgBuffer[0] = id; // Device index to the I2CBus
		msgBuffer[1] = deviceAddress;
		msgBuffer[2] = size;
		for (int i = 0; i < size; i++) {
			msgBuffer[i + 3] = (int) buffer[i] & 0xff;
		}
		sendMsg(I2C_WRITE, msgBuffer);
	}

	@Override
	public int i2cWriteRead(I2CControl control, int busAddress, int deviceAddress, byte[] writeBuffer, int writeSize, byte[] readBuffer, int readSize) {
		if (writeSize != 1) {
			i2cWrite(control, busAddress, deviceAddress, writeBuffer, writeSize);
			return i2cRead(control, busAddress, deviceAddress, readBuffer, readSize);
		} else {
			i2cDataReturned = false;
			// Get the device index to the MRL i2c bus
			String i2cBus = String.format("I2CBus%s", busAddress);
			DeviceMapping map;
			map = deviceList.get(i2cBus);
			int id = map.getId(); // Device index to the I2CBus
			int msgBuffer[] = new int[3];
			msgBuffer[0] = id;
			msgBuffer[1] = deviceAddress;
			msgBuffer[2] = readSize;
			msgBuffer[3] = writeBuffer[0];
			sendMsg(I2C_WRITE_READ, msgBuffer);
			int retry = 0;
			int retryMax = 1000; // ( About 1000ms = s)
			try {
				/**
				 * We will wait up to retryMax times to get the i2c data back
				 * from MRLComm.c and wait 1 ms between each try. A blocking
				 * queue is not needed, as this is only a single data element -
				 * and blocking is not necessary.
				 */
				while ((retry < retryMax) && (!i2cDataReturned)) {
					sleep(1);
					++retry;
				}
			} catch (Exception e) {
				Logging.logError(e);
			}
			if (i2cDataReturned) {
				log.debug(String.format("i2cReturnData returned %s bytes to caller %s.", i2cDataSize, control.getName()));
				for (int i = 0; i < i2cDataSize; i++) {
					readBuffer[i] = i2cData[i];
					log.debug(String.format("i2cReturnData returned ix %s value %s", i, readBuffer[i]));
				}
				return i2cDataSize;
			}
			// Time out, no data returned
			return -1;
		}
	}

	@Override
	public void i2cReturnData(SensorData data) {
		int[] rawData = (int[]) data.getData();
		i2cDataSize = rawData.length;
		for (int i = 0; i < i2cDataSize; i++) {
			i2cData[i] = (byte) (rawData[i] & 0xff);
		}
		log.debug("i2cReturnData invoked");
		i2cDataReturned = true;
	}

	@Override
	public boolean isAttached() {
		return true;
	}

	public boolean isConnected() {
		// include that we must have gotten a valid MRLComm version number.
		if (serial != null && serial.isConnected() && mrlCommVersion != null) {
			return true;
		}
		if (rootController != null && rootController.isConnected() && mrlCommVersion != null) {
			return true;
		}
		return false;
	}

	/**
	 * FIXME DEPRECATED - / - REMOVE - handle in attach(Device)
	 */
	/*
	 * public void motorAttach(MotorControl motor) throws MRLException { if
	 * (!motor.isLocal()) { throw new MRLException(
	 * "motor is not in the same MRL instance as the motor controller"); }
	 * 
	 * int[] controlPins = motor.getControlPins(); for (int i = 0; i <
	 * controlPins.length; ++i) { pinMode(controlPins[i], OUTPUT); }
	 * 
	 * String type = motor.getType();
	 * 
	 * if (type == null) { throw new IllegalArgumentException(""); }
	 * 
	 * // if we have a pulse step - we can do a form // of false encoding :P if
	 * (motor.getType().equals(Motor.TYPE_PULSE_STEP)) { // TODO - add other //
	 * "real" // encoders // the pwm pin in a pulse step motor "is" the encoder
	 * // sensorAttach(motor); TODO attachDevice }
	 * 
	 * motor.setController(this); motor.broadcastState(); }
	 */

	// ================= new interface end =========================

	@Override
	public void motorMove(MotorControl mc) {
		
		MotorConfig c = mc.getConfig();
		
		if (c == null){
			error("motor config not set");
			return;
		}
		
		Class<?> type = mc.getConfig().getClass();

		double powerOutput = mc.getPowerOutput();

		if (MotorConfigSimpleH.class == type) {
			MotorConfigSimpleH config = (MotorConfigSimpleH)c;
			sendMsg(DIGITAL_WRITE, config.getDirPin(), (powerOutput < 0) ? MOTOR_BACKWARD : MOTOR_FORWARD);
			sendMsg(ANALOG_WRITE, config.getPwrPin(), (int) Math.abs(powerOutput));
		} else if (MotorConfigDualPwm.class == type) {
			MotorConfigDualPwm config = (MotorConfigDualPwm)c;
			if (powerOutput < 0) {
				sendMsg(ANALOG_WRITE, config.getLeftPin(), 0);
				sendMsg(ANALOG_WRITE, config.getRightPin(), (int) Math.abs(powerOutput));
			} else if (powerOutput > 0) {
				sendMsg(ANALOG_WRITE, config.getRightPin(), 0);
				sendMsg(ANALOG_WRITE, config.getLeftPin(), (int) Math.abs(powerOutput));
			} else {
				sendMsg(ANALOG_WRITE, config.getRightPin(), 0);
				sendMsg(ANALOG_WRITE, config.getLeftPin(), 0);
			}
		} else if (MotorPulse.class == type) {
			MotorPulse motor = (MotorPulse) mc;
			// sdsendMsg(ANALOG_WRITE, motor.getPin(Motor.PIN_TYPE_PWM_RIGHT),
			// 0);
			// TODO implement with a -1 for "endless" pulses or a different
			// command parameter :P
			sendMsg(PULSE, motor.getPulsePin(), (int) Math.abs(powerOutput));
		} else {
			error("motorMove for motor type %s not supported", type);
		}

	}

	@Override
	public void motorMoveTo(MotorControl mc) {
		// speed parameter?
		// modulo - if < 1
		// speed = 1 else
		log.info("motorMoveTo targetPos {} powerLevel {}", mc.getTargetPos(), mc.getPowerLevel());

		Class<?> type = mc.getClass();

		// if pulser (with or without fake encoder
		// send a series of pulses !
		// with current direction
		if (MotorPulse.class == type) {
			MotorPulse motor = (MotorPulse) mc;
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
	public void motorReset(MotorControl motor) {
		// perhaps this should be in the motor control
		// motor.reset();
		// opportunity to reset variables on the controller
		// sendMsg(MOTOR_RESET, motor.getind);
	}

	@Override
	public void motorStop(MotorControl mc) {
		MotorConfig c = mc.getConfig();
		
		if (c == null){
			error("motor config not set");
			return;
		}
		
		Class<?> type = mc.getConfig().getClass();

		if (MotorConfigPulse.class == type) {
			MotorConfigPulse config = (MotorConfigPulse) mc.getConfig();
			sendMsg(PULSE_STOP, config.getPulsePin());
		} else if (MotorConfigSimpleH.class == type) {
			MotorConfigSimpleH config = (MotorConfigSimpleH) mc.getConfig();
			sendMsg(ANALOG_WRITE, config.getPwrPin(), 0);
		} else if (MotorConfigDualPwm.class == type) {
			MotorConfigDualPwm config = (MotorConfigDualPwm) mc.getConfig();
			sendMsg(ANALOG_WRITE, config.getLeftPin(), 0);
			sendMsg(ANALOG_WRITE, config.getRightPin(), 0);
		}

	}

	@Override
	public void neoPixelWriteMatrix(NeoPixel neopixel, List<Integer> msg) {
		int id = getDeviceId(neopixel);
		int[] buffer = new int[msg.size() + 2];
		buffer[0] = id;
		buffer[1] = msg.size();
		for (int i = 0; i < msg.size(); i++) {
			buffer[i + 2] = msg.get(i);
		}
		sendMsg(NEO_PIXEL_WRITE_MATRIX, buffer);
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

	@Override
	public String onConnect(String portName) {
		info("%s connected to %s", getName(), portName);
		// Get version should already have been called. don't call it again!
		// getVersion();
		return portName;
	}

	// ========== pulsePin begin =============

	public void onCustomMsg(Integer ax, Integer ay, Integer az) {
		log.info("onCustomMsg");
	}

	@Override
	public String onDisconnect(String portName) {
		info("%s disconnected from %s", getName(), portName);
		return portName;
	}

	public void pinMode(int address, int mode) {
		sendMsg(PIN_MODE, address, mode);
		PinDefinition pinDef = pinIndex.get(address);
		invoke("publishPinDefinition", pinDef);
	}

	@Override
	public void pinMode(int address, String mode) {
		if (mode != null && mode.equalsIgnoreCase("INPUT")) {
			pinMode(address, INPUT);
		} else {
			pinMode(address, OUTPUT);
		}
	}

	/**
	 * With Arduino we want to be able to do pinMode("D7", "INPUT"), but it
	 * should not be part of the PinArrayControl interface - because when it
	 * comes down to it .. a pin MUST ALWAYS have an address regardless what you
	 * label or name it...
	 * 
	 * @param pinName
	 * @param mode
	 */
	public void pinMode(String pinName, String mode) {
		if (mode != null && mode.equalsIgnoreCase("INPUT")) {
			pinMode(pinNameToAddress(pinName), mode);
		} else {
			pinMode(pinNameToAddress(pinName), mode);
		}
	}

	public Integer pinNameToAddress(String pinName) {
		if (!pinMap.containsKey(pinName)) {
			error("no pin %s exists", pinName);
			return null;
		}
		return pinMap.get(pinName).getAddress();
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
			StringBuilder payload = new StringBuilder();
			for (int i = 2; i < msgSize; i++) {
				payload.append((char) message[i]);
			}
			error("MRL->Arduino rx %d type %d: %s", error_mrl_to_arduino_rx_cnt, message[1], payload);
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
		case PUBLISH_BOARD_STATUS: {
			long microsPerLoop = Serial.bytesToInt(message, 1, 2);
			int sram = (int) Serial.bytesToInt(message, 3, 2);
			int deviceCount = (int) Serial.bytesToInt(message, 5, 2);
			info("load %d us - sram %d bytes  device count %d", microsPerLoop, sram, deviceCount);
			invoke("publishBoardStatus", new BoardStatus(microsPerLoop, sram, deviceCount));
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
			Servo servo = (Servo) deviceIndex.get(id).getDevice();
			servo.invoke("publishServoEvent", currentPos & 0xff);
			break;
		}

		/**
		 * PUBLISH_DEVICE_ATTACHED - is the callback from MRLComm to bind a
		 * service with its id
		 *
		 * <pre>
		 * MSG STRUCTURE 0 					1 					2 			  3+
		 * 		PUBLISH_ATTACHED_DEVICE | NEW_DEVICE_ID | NAME_STR_SIZE | NAME
		 * </pre>
		 *
		 */
		case PUBLISH_ATTACHED_DEVICE: {

			int newDeviceId = message[1];
			int nameStrSize = message[2];
			String deviceName = intsToString(message, 3, nameStrSize);

			if (!deviceList.containsKey(deviceName)) {
				error("PUBLISH_ATTACHED_DEVICE deviceName %s not found !", deviceName);
				break;
			}

			DeviceMapping deviceMapping = deviceList.get(deviceName);
			deviceMapping.setId(newDeviceId);
			deviceIndex.put(newDeviceId, deviceList.get(deviceName));
			invoke("publishAttachedDevice", deviceName);

			info("==== ATTACHED DEVICE %s WITH MRLDEVICE %d ====", deviceName, newDeviceId);

			break;
		}

		/**
		 * FIXME - this needs to be publishing SensorEvent (s) since the Arduino
		 * is a PinArrayControl & Controller - if the "pin" is active the events
		 * will become PinEvents
		 * 
		 * SensorEvent is the most "raw" form.. PinEvents is typically just Pin
		 * state change
		 * 
		 * Some sensors will need the "raw" form so that they can
		 * re-interpret/decode the data
		 */
		case PUBLISH_SENSOR_DATA: {
			// PUBLISH_ATTACHED_SENSOR | DEVICE_INDEX | DATA_SIZE | DATA ....

			int id = message[1];

			// get the size of the data payload
			int size = (int) message[2];
			if (size > message.length - 2) {
				error("PUBLISH_SENSOR_DATA invalid size %d", size);
				break;
			}

			// get the device mapping from the returning id
			DeviceMapping map = deviceIndex.get(id);
			// get the device - in this case it "should" be a sensor listener
			// since mrl is trying to publish data back to it...
			SensorDataListener sensor = (SensorDataListener) map.getDevice();

			// unload the data
			int[] rawDate = new int[size];
			for (int i = 0; i < size; ++i) {
				rawDate[i] = message[i + 2 + 1];
			}

			SensorData event = new SensorData(rawDate);

			// an optimization - bypass queues if local
			if (sensor.isLocal()) {
				sensor.onSensorData(event);
			}

			// publish for everything else..
			// standard pub / sub
			invoke("publishSensorData", event);

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
			heartbeat = true;
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
		case PUBLISH_BOARD_INFO: {
			int boardId = message[1];
			String boardName = "";
			switch (boardId) {
			case BOARD_TYPE_ID_MEGA:
				boardName = BOARD_TYPE_MEGA;
				break;
			case BOARD_TYPE_ID_UNO:
				boardName = BOARD_TYPE_UNO;
				break;
			default:
				boardName = "Unknown";
				break;
			}
			log.info("Board type returned by Arduino: {}", boardName);
			log.info("Board type currently set: {}", boardType);
			if ((boardType == "" || boardType == null) && boardId != BOARD_TYPE_ID_UNKNOWN) {
				setBoard(boardName);
				log.info("Board type set to: {}", boardType);
			} else {
				log.info("No change in board type");
			}
			break;
		}
		case MSG_ROUTE: {
			int[] newMsg = new int[MAX_MSG_SIZE];
			for (int i = 2; i < msgSize; i++) {
				newMsg[i - 2] = message[i];
			}
			attachedController.get(message[1]).msgSize = msgSize - 2;
			attachedController.get(message[1]).processMessage(newMsg);
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
	public String publishAttachedDevice(String deviceName) {
		return deviceName;
	}

	public BoardInfo publishBoardInfo(BoardInfo status) {
		return status;
	}

	public BoardStatus publishBoardStatus(BoardStatus status) {
		return status;
	}

	public String publishDebug(String msg) {
		return msg;
	}

	public void publishMessageAck() {
	}

	public Integer publishMRLCommError(Integer code) {
		return code;
	}

	/**
	 * This method is called with Pin data whene a pin value is changed on the
	 * Arduino board the Arduino must be told to poll the desired pin(s). This
	 * is done with a analogReadPollingStart(pin) or digitalReadPollingStart()
	 */

	public PinData publishPin(PinData pinData) {
		// caching last value
		pinIndex.get(pinData.getAddress()).setValue(pinData.getValue());
		return pinData;
	}

	/**
	 * publish all read pin data in one array at once
	 */
	public PinData[] publishPinArray(PinData[] pinData) {
		// FIXME - update all cache
		// pinIndex.get(pinEvent.getAddress()).setValue(pinEvent.getValue());
		return pinData;
	}
	
	/**
	 * method to communicate changes in pinmode or state changes
	 * @param pinDef
	 * @return
	 */
	public PinDefinition publishPinDefinition(PinDefinition pinDef){
		return pinDef;
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

	/**
	 * SensorDataPublisher.publishSensorData - publishes SensorData
	 */
	@Override
	public SensorData publishSensorData(SensorData data) {
		return data;
	}

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

	// PIN CONTROL //////////////////////////////////
	// FIXME implement
	// digitalRead for digital pins
	// analogRead for analog pins
	// block for callback
	@Override
	public int read(int address) {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public int read(String pinName) {
		return read(pinNameToAddress(pinName));
	}

	public void refresh() {
		serial.refresh();
		broadcastState();
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
	 *            MAGIC_NUMBER|LENGTH|FUNCTION|PARAM0|PARAM1 ... |PARAM(N)
	 *
	 */
	public synchronized void sendMsg(int function, int... params) {
		if (rootController != null) {
			MrlMsg msg = new MrlMsg(MSG_ROUTE);
			msg.addData(controllerAttachAs);
			msg.addData(function);
			for (int i = 0; i < params.length; i++) {
				msg.addData(params[i]);
			}
			rootController.sendMsg(msg);
		} else {
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
				// send the message as an array. (serial port actually writes 1
				// byte
				// at a time anyway.. oh well.)

				// set a flag that a message is in flight.
				ackRecieved = false;
				// notify();

				// Technically this is the only thing that needs to be
				// synchronized
				// i think.
				synchronized (msgToSend) {
					serial.write(msgToSend);
				}
			} catch (Exception e) {
				error("sendMsg " + e.getMessage());
			}
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
			// Thread.sleep(1);
			sleep(1);
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
			// Thread.sleep(delay);
			sleep(delay);
		}

	}

	public synchronized void sendMsg(int function, List<Integer> params) {
		int[] p = new int[params.size()];
		for (int i = 0; i < params.size(); ++i) {
			p[i] = params.get(i);
		}

		sendMsg(function, p);
	}

	@Override
	public void sensorActivate(SensorControl sensor, Object... conf) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sensorDeactivate(SensorControl sensor) {
		// TODO Auto-generated method stub

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

	@Override
	public void servoAttach(ServoControl servo, int pin) {
		sendMsg(SERVO_ATTACH, getDeviceId(servo), pin);
	}

	/**
	 * THESE ARE SERVO COMMANDS ! NOT REQUEST TO ATTACH OR DETACH THE SERVO AS A
	 * DEVICE !!! This is Servo.attach(10) .. not Arduino.attach(Device) !!
	 */
	@Override
	public void servoDetach(ServoControl servo) {
		int id = getDeviceId(servo);
		sendMsg(SERVO_DETACH, getDeviceId(servo), id);
	}

	@Override
	public void servoSetSpeed(ServoControl servo) {
		Double speed = servo.getSpeed();
		if (speed == null || speed < 0.0f || speed > 1.0f) {
			error("speed %f out of bounds", speed);
			return;
		}

		int id = getDeviceId(servo);
		sendMsg(SERVO_SET_SPEED, id, (int) (speed * 100));
	}

	// FIXME - do sweep single method call from ServoControl
	@Override
	public void servoSweepStart(ServoControl servo) {
		int id = getDeviceId(servo);
		log.info(String.format("servoSweep %s id %d min %d max %d step %d", servo.getName(), id, servo.getSweepMin(), servo.getSweepMax(), servo.getSweepStep()));
		sendMsg(SERVO_SWEEP_START, id, servo.getSweepMin(), servo.getSweepMax(), servo.getSweepStep());
	}

	@Override
	public void servoSweepStop(ServoControl servo) {
		sendMsg(SERVO_SWEEP_STOP, getDeviceId(servo));
	}

	@Override
	public void servoWrite(ServoControl servo) {
		int id = getDeviceId(servo);
		log.info("servoWrite {} {} id {}", servo.getName(), servo.getTargetOutput(), id);
		sendMsg(SERVO_WRITE, id, servo.getTargetOutput().intValue());
	}

	@Override
	public void servoWriteMicroseconds(ServoControl servo, int uS) {
		int id = getDeviceId(servo);
		log.info(String.format("writeMicroseconds %s %d id %d", servo.getName(), uS, id));
		MrlMsg msg = new MrlMsg(SERVO_WRITE_MICROSECONDS, id);
		msg.addData16(uS);
		sendMsg(msg);
	}

	public String setBoard(String board) {
		this.boardType = board;
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
		boardType = BOARD_TYPE_MEGA;
		createPinList();
		broadcastState();
		return boardType;
	}

	public String setBoardUno() {
		boardType = BOARD_TYPE_UNO;
		createPinList();
		broadcastState();
		return boardType;
	}

  public String setBoardMegaADK() {
    boardType = BOARD_TYPE_MEGA_ADK;
    createPinList();
    broadcastState();
    return boardType;
  }

  /**
	 * DeviceControl methods. In this case they represents the I2CBusControl Not
	 * sure if this is good to use the Arduino as an I2CBusControl Exploring
	 * different alternatives. I may have to rethink. Alternate solutions are
	 * welcome. /Mats.
	 */

	@Override
	public void setController(DeviceController controller) {
		// TODO Auto-generated method stub
		// Not sure what to do here. I don't want to create an infinite loop
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

	public void setDebug(boolean b) {
		if (b)
			sendMsg(SET_DEBUG, 1);
		else
			sendMsg(SET_DEBUG, 0);
	}

	public void setDigitalTriggerOnly(Boolean b) {
		if (!b)
			sendMsg(SET_DIGITAL_TRIGGER_ONLY, FALSE);
		else
			sendMsg(SET_DIGITAL_TRIGGER_ONLY, TRUE);

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

	/*
	 * This would never work as a jar public void openMrlComm() throws Exception
	 * { File f = new File("src\\resource\\Arduino\\MRLComm\\MRLComm.ino"); if
	 * (f.exists()) { if (Desktop.isDesktopSupported()) {
	 * Desktop.getDesktop().open(f); } } }
	 */

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

	@Override
	public void stopService() {
		super.stopService();
		disconnect();
	}

	public void uploadSketch(String arduinoIdePath, String port, String type) throws IOException {
		arduinoIdePath = arduinoIdePath.replace("\\", "/");
		arduinoIdePath = arduinoIdePath.trim();
		if (!arduinoIdePath.endsWith("/")) {
			arduinoIdePath += "/";
		}

		log.info(String.format("arduino IDE Path=%s", arduinoIdePath));
		log.info(String.format("Port=%s", port));
		log.info(String.format("type=%s", type));
		if (arduinoIdePath != null && !arduinoIdePath.equals(ArduinoUtils.arduinoPath)) {
			this.arduinoPath = arduinoIdePath;
			ArduinoUtils.arduinoPath = arduinoIdePath;
			save();
		}

		uploadSketchResult = String.format("Uploaded %s ", new Date());
		
		boolean connectedState = isConnected();
		try {

			if (connectedState) {
				log.info("disconnecting...");
				disconnect();
			}
			ArduinoUtils.uploadSketch(port, type);

		} catch (Exception e) {
			log.info("ArduinoUtils threw trying to upload", e);
		}

		if (connectedState) {
			log.info("reconnecting...");
			serial.connect();
		}

		// perhaps you can reduce the inter-process information
		// to succeed | fail .. perhaps you can't
		// I would prefer transparency - send all output to the ui
		uploadSketchResult += ArduinoUtils.getOutput();

		log.info(uploadSketchResult);
		broadcastState();
	}

	/**
	 * PinArrayControl method
	 */
	@Override
	public void write(int address, int value) {
		info("write (%d,%d) to %s", address, value, serial.getName());

		PinDefinition pinDef = pinIndex.get(address);

		if (pinDef.isPwm()) {
			analogWrite(address, value);
		} else {
			digitalWrite(address, value);
		}
		// cache value
		pinDef.setValue(value);
	}

	/**
	 * Arduino is a sensor listener - it listens to its own pins and enabled Pin
	 * events
	 */
	@Override
	public void onSensorData(SensorData data) {

		// at the moment we do not need a 'type'
		// all sensor array data will be sent here ..

		// this is MRLComms raw data handled in the context of
		// a Sensor's onSensorData - so this method is specific for
		// transforming 'Arduino' specific data to a useful form
		// Since Arduino's useful data are pins we convert the pin values
		// read into an array of pins

		// we publish both pins and arrays of pins
		// it depends what other services have registered for

		// regardless we need to convert it all to PinData

		// it better be an array[] of ints with tuples in the form
		// | address | msb | lsb | ...
		int[] rawPinData = (int[]) data.getData();

		int pinDataCnt = rawPinData.length
				/ 3; /* length / (1 address + 1 msg + 1 lsb) */
		if (rawPinData.length % 3 != 0) {
			log.error("something is wrong - expecting 3 bytes per pin data");
		}

		PinData[] pinArray = new PinData[pinDataCnt];

		// parse sort reduce ...
		for (int i = 0; i < pinArray.length; ++i) {
			PinData pinData = new PinData(rawPinData[3 * i], Serial.bytesToInt(rawPinData, (3 * i) + 1, 2));
			pinArray[i] = pinData;
			int address = pinData.getAddress();

			// handle individual pins
			if (pinListeners.containsKey(address)) {
				List<PinListener> list = pinListeners.get(address);
				for (int j = 0; j < list.size(); ++j) {
					PinListener pinListner = list.get(j);
					if (pinListner.isLocal()) {
						pinListner.onPin(pinData);
					} else {
						invoke("publishPin", pinData);
					}
				}
			}
		}

		// publish array
		invoke("publishPinArray", new Object[] { pinArray });
	}

	public void publishBoardInfo() {

	}

	public void msgRoute() {

	}

	@Override
	public void neoPixelSetAnimation(NeoPixel neopixel, int animation, int red, int green, int blue, int speed) {
		MrlMsg msg = new MrlMsg(NEO_PIXEL_SET_ANIMATION);
		msg.addData(getDeviceId(neopixel));
		msg.addData(6); // size of the config
		msg.addData(animation);
		msg.addData(red);
		msg.addData(green);
		msg.addData(blue);
		msg.addData16(speed);
		sendMsg(msg);
	}

	@Override
	public void servoSetMaxVelocity(ServoControl servo) {
		MrlMsg msg = new MrlMsg(SERVO_SET_MAX_VELOCITY);
		msg.addData(getDeviceId(servo));
		msg.addData(2);
		msg.addData16(servo.getMaxVelocity());
		sendMsg(msg);
	}
	
	public void enabledHeartbeat() {
	  heartbeat = true;
	  addTask("heartbeat",1000,"heartbeat");
	}
	
	public void heartbeat() {
	  if (!heartbeat) {
	    log.info("No answer from controller:{}. Disconnecting...",this.getName());
	    purgeTask("heartbeat");
	    if (isConnected()){
	      disconnect();
	    }
	  }
	  heartbeat = false;
	  sendMsg(HEARTBEAT);
	}
}
