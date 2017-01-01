/*
 * 
 *   Adafruit16CServoDriver
 *   
 *   TODO - test with Steppers & Motors - switches on board - interface accepts motor control
 *
 */

package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.motor.MotorConfig;
import org.myrobotlab.motor.MotorConfigDualPwm;
import org.myrobotlab.motor.MotorConfigSimpleH;
import org.myrobotlab.motor.MotorConfigPulse;
import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.slf4j.Logger;

/**
 * AdaFruit 16-Channel PWM / Servo Driver
 * 
 * @author GroG and Mats
 * 
 *         References : http://www.ladyada.net/make/mshield/use.html
 *         https://learn.adafruit.com/16-channel-pwm-servo-driver
 */

public class Adafruit16CServoDriver extends Service implements I2CControl, ServoController, MotorController {

	/**
	 * SpeedControl, calculates the next position at regular intervals to make
	 * the servo move at the desired speed
	 * 
	 */
	public class SpeedControl extends Thread {

		ServoData servoData;
		String name;
		
		public SpeedControl(String name) {
			super(String.format("%s.SpeedControl", name));
			servoData = servoMap.get(name);
			servoData.isMoving = true;
			this.name = name;
		}

		@Override
		public void run() {

			try {
				while (servoData.isMoving = true) {
					servoData = servoMap.get(name);
					if (servoData.targetOutput > servoData.currentOutput) {
						servoData.currentOutput += 1;
					} else if (servoData.targetOutput < servoData.currentOutput) {
						servoData.currentOutput -= 1;
					} else {
						// We have reached the position so shutdown the thread
						servoData.isMoving = false;
					}
					int pulseWidthOff = SERVOMIN + (int) (servoData.currentOutput * (int) ((float) SERVOMAX - (float) SERVOMIN) / (float) (180));
					setServo(servoData.pin, pulseWidthOff);
					// Calculate next step for the the new value for the motor
					Thread.sleep((int)(1000 / servoData.velocity));
				}

			} catch (Exception e) {
				servoData.isMoving = false;
				if (e instanceof InterruptedException) {
					info("Shutting down MotorUpdater");
				} else {
					logException(e);
				}
			}
		}

	}

	/** version of the library */
	static public final String VERSION = "0.9";

	private static final long serialVersionUID = 1L;

	// Depending on your servo make, the pulse width min and max may vary, you
	// want these to be as small/large as possible without hitting the hard stop
	// for max range. You'll have to tweak them as necessary to match the servos
	// you have!
	//
	public final static int SERVOMIN = 150; // this
											// is
											// the
											// 'minimum'
											// pulse
	// length count (out of 4096)
	public final static int SERVOMAX = 600; // this
											// is
											// the
											// 'maximum'
											// pulse
	// length count (out of 4096)

	transient public I2CController controller;

	// Constant for default PWM freqency
	private static int pwmFreq = 60;
	final static int minPwmFreq = 24;
	final static int maxPwmFreq = 1526;

	// List of possible addresses. Used by the GUI.
	public List<String> deviceAddressList = Arrays.asList("0x40", "0x41", "0x42", "0x43", "0x44", "0x45", "0x46",
			"0x47", "0x48", "0x49", "0x4A", "0x4B", "0x4C", "0x4D", "0x4E", "0x4F", "0x50", "0x51", "0x52", "0x53",
			"0x54", "0x55", "0x56", "0x57", "0x58", "0x59", "0x5A", "0x5B", "0x5C", "0x5D", "0x5E", "0x5F");
	// Default address
	public String deviceAddress = "0x40";
	/**
	 * This address is to address all Adafruit16CServoDrivers on the i2c bus
	 * Don't use this address for any other device on the i2c bus since it will
	 * cause collisions.
	 */
	public String broadcastAddress = "0x70";

	public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");
	public String deviceBus = "1";

	public transient final static Logger log = LoggerFactory.getLogger(Adafruit16CServoDriver.class.getCanonicalName());

	public static final int PCA9685_MODE1 = 0x00; // Mod
													// 1
													// register
	public static final byte PCA9685_SLEEP = 0x10; // Set
													// sleep
													// mode,
													// before
													// changing
													// prescale
													// value
	public static final byte PCA9685_AUTOINCREMENT = 0x20; // Set
															// autoincrement
															// to
															// be
															// able
															// to
															// write
															// more
															// than
															// one
															// byte
															// in
															// sequence

	public static final byte PCA9685_PRESCALE = (byte) 0xFE; // PreScale
																// register

	// Pin PWM addresses 4 bytes repeats for each pin so I only define pin 0
	// The rest of the addresses are calculated based on pin numbers
	public static final int PCA9685_LED0_ON_L = 0x06; // First
														// LED
														// address
														// Low
	public static final int PCA9685_LED0_ON_H = 0x07; // First
														// LED
														// address
														// High
	public static final int PCA9685_LED0_OFF_L = 0x08; // First
														// LED
														// address
														// Low
	public static final int PCA9685_LED0_OFF_H = 0x08; // First
														// LED
														// addressHigh

	// public static final int PWM_FREQ = 60; // default frequency for servos
	public static final float osc_clock = 25000000; // clock
													// frequency
													// of
													// the
													// internal
													// clock
	public static final float precision = 4096; // pwm_precision

	// i2c controller
	public List<String> controllers;
	public String controllerName;
	public boolean isControllerSet = false;

	/**
	 * @Mats - added by GroG - was wondering if this would help, probably you
	 *       need a reverse index too ?
	 * @GroG - I only need servoNameToPin yet. To be able to move at a set speed
	 *       a few extra values are needed
	 */
	class ServoData {
		int pin;
		boolean pwmFreqSet = false;
		int pwmFreq;
		SpeedControl speedcontrol;
		double velocity = 0;
		boolean isMoving = false;
		double targetOutput;
		double currentOutput;
	}

	transient HashMap<String, ServoData> servoMap = new HashMap<String, ServoData>();

	// Motor related constants
	public static final int MOTOR_FORWARD = 1;
	public static final int MOTOR_BACKWARD = 0;
	public static final int defaultMotorPwmFreq = 1000;

	/**
	 * pin named map of all the pins on the board
	 */
	Map<String, PinDefinition> pinMap = null;
	/**
	 * the definitive sequence of pins - "true address"
	 */
	Map<Integer, PinDefinition> pinIndex = null;

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Adafruit16CServoDriver driver = (Adafruit16CServoDriver) Runtime.start("pwm", "Adafruit16CServoDriver");
		log.info("Driver {}", driver);

	}

	public Adafruit16CServoDriver(String n) {
		super(n);
		createPinList();
		refreshControllers();
		subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
	}

	public void onRegistered(ServiceInterface s) {
		refreshControllers();
		broadcastState();
	}

	/*
	 * Refresh the list of running services that can be selected in the GUI
	 */
	public List<String> refreshControllers() {
		controllers = Runtime.getServiceNamesFromInterface(I2CController.class);
		return controllers;
	}

	// ----------- AFMotor API End --------------
	// TODO
	// Implement MotorController
	//
	/**
	 * This set of methods is used to set i2c parameters
	 * 
	 * @param controllerName
	 *            = The name of the i2c controller
	 * @param deviceBus
	 *            = i2c bus Should be "1" for Arduino and RasPi "0"-"7" for
	 *            I2CMux
	 * @param deviceAddress
	 *            = The i2c address of the PCA9685 ( "0x40" - "0x5F")
	 * @return
	 */
	// @Override
	public boolean setController(String controllerName, String deviceBus, String deviceAddress) {
		return setController((I2CController) Runtime.getService(controllerName), deviceBus, deviceAddress);
	}

	public boolean setController(String controllerName) {
		return setController((I2CController) Runtime.getService(controllerName), this.deviceBus, this.deviceAddress);
	}

	@Override
	public boolean setController(I2CController controller) {
		return setController(controller, this.deviceBus, this.deviceAddress);
	}

	public boolean setController(I2CController controller, String deviceBus, String deviceAddress) {
		if (controller == null) {
			error("setting null as controller");
			return false;
		}

		controllerName = controller.getName();
		log.info(String.format("%s setController %s", getName(), controllerName));

		controllerName = controller.getName();
		this.controller = controller;
		this.deviceBus = deviceBus;
		this.deviceAddress = deviceAddress;

		createDevice();
		isControllerSet = true;
		broadcastState();
		return true;
	}

	@Override
	public void setDeviceBus(String deviceBus) {
		this.deviceBus = deviceBus;
		broadcastState();
	}

	@Override
	public void setDeviceAddress(String deviceAddress) {
		if (controller != null) {
			if (this.deviceAddress != deviceAddress) {
				controller.releaseI2cDevice(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress));
				controller.i2cAttach(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress));
			}
		}
		log.info(String.format("Setting device address to %s", deviceAddress));
		this.deviceAddress = deviceAddress;
	}

	/**
	 * This method creates the i2c device
	 */
	boolean createDevice() {
		if (controller != null) {
			// controller.releaseI2cDevice(this, Integer.parseInt(deviceBus),
			// Integer.decode(deviceAddress));
			controller.i2cAttach(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress));
		} else {
			log.error("Can't create device until the controller has been set");
			return false;
		}

		log.info(String.format("Creating device on bus: %s address %s", deviceBus, deviceAddress));
		return true;
	}

	// @Override
	// boolean DeviceControl.isAttached()
	public boolean isAttached() {
		return controller != null;
	}

	/**
	 * Set the PWM pulsewidth
	 * 
	 * @param pin
	 * @param pulseWidthOn
	 * @param pulseWidthOff
	 */
	public void setPWM(Integer pin, Integer pulseWidthOn, Integer pulseWidthOff) {

		byte[] buffer = { (byte) (PCA9685_LED0_ON_L + (pin * 4)), (byte) (pulseWidthOn & 0xff),
				(byte) (pulseWidthOn >> 8), (byte) (pulseWidthOff & 0xff), (byte) (pulseWidthOff >> 8) };
		controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
	}

	/**
	 * Set the PWM frequency i.e. the frequency between positive pulses.
	 * 
	 * @param hz
	 */
	public void setPWMFreq(int pin, Integer hz) { // Analog servos run at ~60 Hz

		float prescale_value;

		if (hz < minPwmFreq) {
			log.error(String.format("Minimum PWMFreq is %s Hz, requested freqency is %s Hz, clamping to minimum",
					minPwmFreq, hz));
			hz = minPwmFreq;
			prescale_value = 255;
		} else if (hz > maxPwmFreq) {
			log.error(String.format("Maximum PWMFreq is %s Hz, requested frequency is %s Hz, clamping to maximum",
					maxPwmFreq, hz));
			hz = maxPwmFreq;
			prescale_value = 3;
		} else {
			prescale_value = Math.round(osc_clock / precision / hz) - 1;
		}

		log.info(String.format("PWMFreq %s hz, prescale_value calculated to %s", hz, prescale_value));
		// Set sleep mode before changing PWM freqency
		byte[] writeBuffer = { PCA9685_MODE1, PCA9685_SLEEP };
		controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writeBuffer,
				writeBuffer.length);

		// Wait 1 millisecond until the oscillator has stabilized
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			if (Thread.interrupted()) { // Clears interrupted status!
			}
		}

		// Write the PWM frequency value
		byte[] buffer2 = { PCA9685_PRESCALE, (byte) prescale_value };
		controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer2, buffer2.length);

		// Leave sleep mode, set autoincrement to be able to write several
		// bytes
		// in sequence
		byte[] buffer3 = { PCA9685_MODE1, PCA9685_AUTOINCREMENT };
		controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer3, buffer3.length);

		// Wait 1 millisecond until the oscillator has stabilized
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			if (Thread.interrupted()) { // Clears interrupted status!
			}
		}
	}

	public void setServo(Integer pin, Integer pulseWidthOff) {
		// since pulseWidthOff can be larger than > 256 it needs to be
		// sent as 2 bytes
		log.debug(
				String.format("setServo %s deviceAddress %s pin %s pulse %s", pin, deviceAddress, pin, pulseWidthOff));
		byte[] buffer = { (byte) (PCA9685_LED0_OFF_L + (pin * 4)), (byte) (pulseWidthOff & 0xff),
				(byte) (pulseWidthOff >> 8) };
		controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
	}

	/**
	 * this would have been nice to have Java 8 and a default implementation in
	 * this interface which does Servo sweeping in the Servo (already
	 * implemented) and only if the controller can does it do sweeping on the
	 * "controller"
	 * 
	 * For example MrlComm can sweep internally (or it used to be implemented)
	 */
	@Override
	public void servoSweepStart(ServoControl servo) {
		log.info("Adafruit16C can not do sweeping on the controller - sweeping must be done in ServoControl");
	}

	@Override
	public void servoSweepStop(ServoControl servo) {
		log.info("Adafruit16C can not do sweeping on the controller - sweeping must be done in ServoControl");
	}

	@Override
	public void servoMoveTo(ServoControl servo) {
		ServoData servoData = servoMap.get(servo.getName());
		if (!servoData.pwmFreqSet) {
			setPWMFreq(servoData.pin, servoData.pwmFreq);
			servoData.pwmFreqSet = true;
		}
		// Move at max speed
		if (servoData.velocity == -1) {
			servoData.currentOutput = servo.getTargetOutput();
			servoData.targetOutput = servo.getTargetOutput();
			log.debug(String.format("servoWrite %s deviceAddress %s targetOutput %d", servo.getName(), deviceAddress,
					servo.getTargetOutput()));
			int pulseWidthOff = SERVOMIN
					+ (int) (servo.getTargetOutput() * (int) ((float) SERVOMAX - (float) SERVOMIN) / (float) (180));
			setServo(servo.getPin(), pulseWidthOff);
		} else {
			servoData.targetOutput = servo.getTargetOutput();
			// Start a thread to handle the speed for this servo
			if (servoData.isMoving == false) {
				servoData.speedcontrol = new SpeedControl(servo.getName());
			}
		}
	}

	@Override
	public void servoWriteMicroseconds(ServoControl servo, int uS) {
		ServoData servoData = servoMap.get(servo.getName());
		if (!servoData.pwmFreqSet) {
			setPWMFreq(servoData.pin, servoData.pwmFreq);
			servoData.pwmFreqSet = true;
		}

		int pin = servo.getPin();
		// 1000 ms => 150, 2000 ms => 600
		int pulseWidthOff = (int) (uS * 0.45) - 300;
		// since pulseWidthOff can be larger than > 256 it needs to be
		// sent as 2 bytes
		log.info(String.format("servoWriteMicroseconds %s deviceAddress x%02X pin %s pulse %d", servo.getName(),
				deviceAddress, pin, pulseWidthOff));

		byte[] buffer = { (byte) (PCA9685_LED0_OFF_L + (pin * 4)), (byte) (pulseWidthOff & 0xff),
				(byte) (pulseWidthOff >> 8) };
		controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
	}

	@Override
	public DeviceController getController() {
		return controller;
	}

	/**
	 * Device attach - this should be creating the I2C bus on MRLComm for the
	 * "first" servo if not already created - Since this does not use the
	 * Arduino <Servo.h> servos - it DOES NOT need to create "Servo" devices in
	 * MRLComm. It will need to keep track of the "pin" to I2C address, and
	 * whenever a ServoControl.moveTo(pos) - the Servo will tell this controller
	 * its name & location to move. Mats says. The board has a single i2c
	 * address that doesn't change. The Arduino only needs to keep track of the
	 * i2c bus, not all devices that can communicate thru it. I.e. This service
	 * should keep track of servos, not the Arduino or the Raspi.
	 * 
	 * 
	 * This service will translate the name & location to an I2C address & value
	 * write request to the MRLComm device.
	 * 
	 * Mats comments on the above. MRLComm should not know anything about the
	 * servos in this case. This service keeps track of the servos. MRLComm
	 * should not know anything about what addresses are used on the i2c bus
	 * MRLComm should initiate the i2c bus when it receives the first i2c write
	 * or read This service knows nothing about other i2c devices that can be on
	 * the same bus. And most important. This service knows nothing about
	 * MRLComm at all. I.e except for this bunch of comments :-)
	 * 
	 * It implements the methods defined in the ServoController and translates
	 * the servo requests to i2c writes defined in the I2CControl interface
	 * 
	 */

	/**
	 * if your device controller can provided several {Type}Controller
	 * interfaces, there might be commonality between all of them. e.g.
	 * initialization of data structures, preparing communication, sending
	 * control and config messages, etc.. - if there is commonality, it could be
	 * handled here - where Type specific methods call this method
	 * 
	 * This is a software representation of a board that uses the i2c protocol.
	 * It uses the methods defined in the I2CController interface to write
	 * servo-commands. The I2CController interface defines the common methods
	 * for all devices that use the i2c protocol. In most services I will define
	 * addition <device>Control methods, but this service is a "middle man" so
	 * it implements the ServoController methods and should not have any "own"
	 * methods.
	 *
	 * After our explanation of the roles of <device>Control and
	 * <device>Controller it's clear to me that any device that uses the i2c
	 * protocol needs to implement to <device>Control methods: I2CControl that
	 * is the generic interface for any i2c device <device>Control, that defines
	 * the specific methods for that device. For example the MPU6050 should
	 * implement both I2CControl and MPU6050Control or perhaps a AccGyroControl
	 * interface that would define the common methods that a
	 * Gyro/Accelerometer/Magnetometer device should implement.
	 */

	@Deprecated // use attach(ServoControl servo)
	void servoAttach(ServoControl device, Object... conf) {
		ServoControl servo = (ServoControl) device;
		// should initial pos be a requirement ?
		// This will fail because the pin data has not yet been set in Servo
		// servoNameToPin.put(servo.getName(), servo.getPin());
		String servoName = servo.getName();
		ServoData servoData = new ServoData();
		servoData.pin = (int) conf[0];
		servoData.pwmFreqSet = false;
		servoData.pwmFreq = pwmFreq;
		servoMap.put(servoName, servoData);
		invoke("publishAttachedDevice", servoName);
	}
	
	public void attach(ServoControl servo) {
		ServoData servoData = new ServoData();
		servoData.pin = servo.getPin();
		servoData.pwmFreqSet = false;
		servoData.pwmFreq = pwmFreq;
		servoMap.put(servo.getName(), servoData);
		invoke("publishAttachedDevice", servo.getName());
	}

	void motorAttach(MotorControl device, Object... conf) {
		/*
		 * This is where motor data could be initialized. So far all motor data
		 * this service needs can be requested from the motors config
		 */
		MotorControl motor = (MotorControl) device;
		invoke("publishAttachedDevice", motor.getName());
	}

	public void detach(DeviceControl servo) {
		servoDetachPin((ServoControl) servo);
		servoMap.remove(servo.getName());
	}

	public String publishAttachedDevice(String deviceName) {
		return deviceName;
	}

	/**
	 * Start sending pulses to the servo
	 * 
	 */
	@Override
	public void servoAttachPin(ServoControl servo, int pin) {
		servoMoveTo(servo);
	}

	/**
	 * Stop sending pulses to the servo, relax
	 */
	@Override
	public void servoDetachPin(ServoControl servo) {
		int pin = servo.getPin();
		setPWM(pin, 4096, 0);
	}

	@Override
	public void servoSetMaxVelocity(ServoControl servo) {
		// TODO Auto-generated method stub.
		// perhaps cannot do this with Adafruit16CServoDriver
		// Mats says: It can be done in this service. But not by the board.

	}

	@Override
	public void motorMove(MotorControl mc) {

		MotorConfig c = mc.getConfig();

		if (c == null) {
			error("motor config not set");
			return;
		}

		Class<?> type = mc.getConfig().getClass();

		double powerOutput = mc.getPowerOutput();

		if (MotorConfigSimpleH.class == type) {
			MotorConfigSimpleH config = (MotorConfigSimpleH) c;
			if (config.getPwmFreq() == null) {
				config.setPwmFreq(defaultMotorPwmFreq);
				setPWMFreq(config.getPwrPin(), config.getPwmFreq());
			}
			setPinValue(config.getDirPin(), (powerOutput < 0) ? MOTOR_BACKWARD : MOTOR_FORWARD);
			setPinValue(config.getPwrPin(), powerOutput);
		} else if (MotorConfigDualPwm.class == type) {
			MotorConfigDualPwm config = (MotorConfigDualPwm) c;
			log.info(String.format("Adafrutit16C Motor DualPwm motorMove, powerOutput = %s", powerOutput));
			if (config.getPwmFreq() == null) {
				config.setPwmFreq(defaultMotorPwmFreq);
				setPWMFreq(config.getLeftPin(), config.getPwmFreq());
				setPWMFreq(config.getRightPin(), config.getPwmFreq());
			}
			if (powerOutput < 0) {
				setPinValue(config.getLeftPin(), 0);
				setPinValue(config.getRightPin(), Math.abs(powerOutput / 255));
			} else if (powerOutput > 0) {
				setPinValue(config.getRightPin(), 0);
				setPinValue(config.getLeftPin(), Math.abs(powerOutput / 255));
			} else {
				setPinValue(config.getRightPin(), 0);
				setPinValue(config.getLeftPin(), 0);
			}
		} else if (MotorPulse.class == type) {
			MotorPulse motor = (MotorPulse) mc;
			// sendMsg(ANALOG_WRITE, motor.getPin(Motor.PIN_TYPE_PWM_RIGHT),
			// 0);
			// TODO implement with a -1 for "endless" pulses or a different
			// command parameter :P
			// TODO Change to setPwmFreq I guess
			// setPwmFreq(motor.getPulsePin(), (int) Math.abs(powerOutput));
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
	public void motorStop(MotorControl mc) {
		MotorConfig c = mc.getConfig();

		if (c == null) {
			error("motor config not set");
			return;
		}

		Class<?> type = mc.getConfig().getClass();

		if (MotorConfigPulse.class == type) {
			MotorConfigPulse config = (MotorConfigPulse) mc.getConfig();
			setPinValue(config.getPulsePin(), 0);
		} else if (MotorConfigSimpleH.class == type) {
			MotorConfigSimpleH config = (MotorConfigSimpleH) mc.getConfig();
			if (config.getPwmFreq() == null) {
				config.setPwmFreq(500);
				setPWMFreq(config.getPwrPin(), config.getPwmFreq());
			}
			setPinValue(config.getPwrPin(), 0);
		} else if (MotorConfigDualPwm.class == type) {
			MotorConfigDualPwm config = (MotorConfigDualPwm) mc.getConfig();
			setPinValue(config.getLeftPin(), 0);
			setPinValue(config.getRightPin(), 0);
		}

	}

	@Override
	public void motorReset(MotorControl motor) {
		// perhaps this should be in the motor control
		// motor.reset();
		// opportunity to reset variables on the controller
		// sendMsg(MOTOR_RESET, motor.getind);

	}

	public void setPinValue(int pin, double powerOutput) {
		log.info(String.format("Adafruit16C setPinValue, pin = %s, powerOutput = %s", pin, powerOutput));
		if (powerOutput < 0) {
			log.error(String.format("Adafruit16CServoDriver setPinValue. Value below zero (%s). Defaulting to 0.",
					powerOutput));
			powerOutput = 0;
		} else if (powerOutput > 1) {
			log.error(
					String.format("Adafruit16CServoDriver setPinValue. Value > 1 (%s). Defaulting to 1", powerOutput));
			powerOutput = 1;
		}

		int powerOn;
		int powerOff;
		// No phase shift. Simple calculation
		if (powerOutput == 0) {
			powerOn = 4096;
			powerOff = 0;
		} else if (powerOutput == 1) {
			powerOn = 0;
			powerOff = 1;
		} else {
			powerOn = (int) (powerOutput * 4096);
			powerOff = 4095;
		}
		log.info(String.format("powerOutput = %s, powerOn = %s, powerOff = %s", powerOutput, powerOn, powerOff));
		setPWM(pin, powerOn, powerOff);
	}

	public List<PinDefinition> getPinList() {
		List<PinDefinition> list = new ArrayList<PinDefinition>(pinIndex.values());
		return list;
	}

	public Map<String, PinDefinition> createPinList() {
		pinIndex = new HashMap<Integer, PinDefinition>();

		for (int i = 0; i < 16; ++i) {
			PinDefinition pindef = new PinDefinition();
			String name = null;
			name = String.format("D%d", i);
			pindef.setDigital(true);
			pindef.setName(name);
			pindef.setAddress(i);
			pinIndex.put(i, pindef);
		}
		return pinMap;
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

		ServiceType meta = new ServiceType(Adafruit16CServoDriver.class.getCanonicalName());
		meta.addDescription("Adafruit 16-Channel PWM/Servo Driver");
		meta.addCategory("shield", "servo & pwm");
		meta.setSponsor("Mats");
		/*
		 * meta.addPeer("arduino", "Arduino", "our Arduino");
		 * meta.addPeer("raspi", "RasPi", "our RasPi");
		 */
		return meta;
	}

	@Override
	public void servoSetVelocity(ServoControl servo) {
		ServoData servoData = servoMap.get(servo.getName());
		servoData.velocity = servo.getVelocity();
	}

	@Override
	public void attach(ServoControl servo, int pin) {
		servo.setPin(pin);
		attach(servo);
	}

	@Override
	public int getDeviceCount() {
		return servoMap.size();
	}

	@Override
	public Set<String> getDeviceNames() {
		return servoMap.keySet();
	}

  /**
   * we can only have one controller for this control
   * - so it's easy - just detach
   */
 
  // TODO - this could be Java 8 default interface implementation
  @Override
  public void detach(String controllerName) {
    if (controller == null || !controllerName.equals(controller.getName())) {
      return;
    }
    controller.detach(this);
    controller = null;
    this.deviceBus = null;
    this.deviceAddress = null;
    isControllerSet = false;
    broadcastState();
  }
  
  /**
   * GOOD DESIGN - this method is the same pretty much for all Services
   * could be a Java 8 default implementation to the interface
   */
  @Override
  public boolean isAttached(String name) {
    return (controller != null && controller.getName().equals(name));
  }

  @Override
  public Set<String> getAttached() {
    HashSet<String> ret = new HashSet<String>();
    if (controller != null){
      ret.add(controller.getName());
    }
    return ret;
  }


	@Override
	public void servoSetAcceleration(ServoControl servo) {
		// TODO Auto-generated method stub
		
	}

}
