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
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.DeviceMapping;
import org.myrobotlab.service.interfaces.Device;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * AdaFruit 16-Channel PWM / Servo Driver
 * 
 * @author GroG
 * 
 *         References : http://www.ladyada.net/make/mshield/use.html
 *         https://learn.adafruit.com/16-channel-pwm-servo-driver
 */

public class Adafruit16CServoDriver extends Service implements ServoController {
	/** version of the library */
	static public final String VERSION = "0.9";

	private static final long serialVersionUID = 1L;

	// Depending on your servo make, the pulse width min and max may vary, you
	// want these to be as small/large as possible without hitting the hard stop
	// for max range. You'll have to tweak them as necessary to match the servos
	// you have!
	//
	public final static int SERVOMIN = 150; // this is the 'minimum' pulse
	// length count (out of 4096)
	public final static int SERVOMAX = 600; // this is the 'maximum' pulse
	// length count (out of 4096)

	transient public I2CControl controller;

	// Variable to ensure that a PWM freqency has been set before starting PWM
	private int pwmFreq = 60;
	private boolean pwmFreqSet = false;

	public List<String> deviceAddressList = Arrays.asList("0x40", "0x41", "0x42", "0x43", "0x44", "0x45", "0x46", "0x47", "0x48", "0x49", "0x4A", "0x4B", "0x4C", "0x4D", "0x4E",
			"0x4F", "0x50", "0x51", "0x52", "0x53", "0x54", "0x55", "0x56", "0x57", "0x58", "0x59", "0x5A", "0x5B", "0x5C", "0x5D", "0x5E", "0x5F");

	public String deviceAddress = "0x40";

	/**
	 * This address is to address all Adafruit16CServoDrivers on the i2c bus Don't
	 * use this address for any other device on the i2c bus since it will cause
	 * collisions.
	 */
	public String broadcastAddress = "0x70";

	public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");
	public String deviceBus = "1";

	public String type = "PCA9685";

	public transient final static Logger log = LoggerFactory.getLogger(Adafruit16CServoDriver.class.getCanonicalName());

	public static final int PCA9685_MODE1 = 0x00; // Mode 1 register
	public static final byte PCA9685_SLEEP = 0x10; // Set sleep mode, before
	// changing prescale value
	public static final byte PCA9685_AUTOINCREMENT = 0x20; // Set autoincrement
	// to
	// be able to write
	// more than one
	// byte
	// in sequence

	public static final byte PCA9685_PRESCALE = (byte) 0xFE; // PreScale
	// register

	// Pin PWM addresses 4 bytes repeats for each pin so I only define pin 0
	// The rest of the addresses are calculated based on pin numbers
	public static final int PCA9685_LED0_ON_L = 0x06; // First LED address Low
	public static final int PCA9685_LED0_ON_H = 0x07; // First LED address High
	public static final int PCA9685_LED0_OFF_L = 0x08; // First LED address Low
	public static final int PCA9685_LED0_OFF_H = 0x08; // First LED address High

	// public static final int PWM_FREQ = 60; // default frequency for servos
	public static final int osc_clock = 25000000; // clock frequency of the
	// internal clock
	public static final int precision = 4096; // pwm_precision

	// i2c controller
	public ArrayList<String> controllers;
	public String controllerName;
	public boolean isControllerSet = false;

	// Servo
	private boolean isAttached = false;

	/**
	 * @Mats - added by GroG - was wondering if this would help, probably you need
	 *       a reverse index too ?
	 * @GroG - I only need servoNameToPin 
	 */
	HashMap<String, Integer> servoNameToPin = new HashMap<String, Integer>();

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Adafruit16CServoDriver driver = (Adafruit16CServoDriver) Runtime.start("pwm", "Adafruit16CServoDriver");
		log.info("Driver {}", driver);

	}

	public Adafruit16CServoDriver(String n) {
		super(n);
		subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
	}

	public void onRegistered(ServiceInterface s) {
		refreshControllers();
		broadcastState();

	}

	/*
	 * Refresh the list of running services that can be selected in the GUI
	 */
	public ArrayList<String> refreshControllers() {
		controllers = Runtime.getServiceNamesFromInterface(I2CControl.class);
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
	 *          = The name of the i2c controller
	 * @param deviceBus
	 *          = i2c bus Ignored by Arduino Should be "1" for the RasPi "0"-"7"
	 *          for I2CMux
	 * @param deviceAddress
	 *          = The i2c address of the PCA9685 ( "0x60" - "0x7F")
	 * @return
	 */
	// @Override
	public boolean setController(String controllerName, String deviceBus, String deviceAddress) {
		return setController((I2CControl) Runtime.getService(controllerName), deviceBus, deviceAddress);
	}

	public boolean setController(String controllerName) {
		return setController((I2CControl) Runtime.getService(controllerName), this.deviceBus, this.deviceAddress);
	}

	public boolean setController(I2CControl controller) {
		return setController(controller, this.deviceBus, this.deviceAddress);
	}

	public boolean setController(I2CControl controller, String deviceBus, String deviceAddress) {
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
		isControllerSet = true;

		broadcastState();
		return true;
	}

	public void unsetController() {
		controller = null;
		this.deviceBus = null;
		this.deviceAddress = null;
		isControllerSet = false;
		broadcastState();
	}

	// FIXME - put in ServoController interface !!!
	public boolean attach(Servo servo, Integer pinNumber) {
		if (servo == null) {
			error("trying to attach null servo");
			return false;
		}

		servo.setController(this);
		servoNameToPin.put(servo.getName(), pinNumber);
		return true;
	}

	public void SetDeviceBus(String deviceBus) {
		this.deviceBus = deviceBus;
		broadcastState();
	}

	boolean SetDeviceAddress(String deviceAddress) {
		if (controller != null) {
			if (this.deviceAddress != deviceAddress) {
				controller.releaseI2cDevice(Integer.parseInt(deviceBus), Integer.decode(deviceAddress));
				controller.createI2cDevice(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), type);
			}
		}
		log.info(String.format("Setting device address to %s", deviceAddress));
		this.deviceAddress = deviceAddress;
		return true;
	}

	public void begin() {

		byte[] buffer = { PCA9685_MODE1, 0x0 };
		controller.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
	}

	// @Override
	public boolean isAttached() {
		return isAttached;
	}

	public void setDeviceAddress(String DeviceAddress) {
		this.deviceAddress = DeviceAddress;
	}

	/**
	 * Set the PWM pulsewidth
	 * 
	 * @param pin
	 * @param pulseWidthOn
	 * @param pulseWidthOff
	 */
	public void setPWM(Integer pin, Integer pulseWidthOn, Integer pulseWidthOff) {

		byte[] buffer = { (byte) (PCA9685_LED0_ON_L + (pin * 4)), (byte) (pulseWidthOn & 0xff), (byte) (pulseWidthOn >> 8), (byte) (pulseWidthOff & 0xff), (byte) (pulseWidthOff >> 8) };
		controller.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
	}

	/**
	 * Set the PWM frequency i.e. the frequency between positive pulses.
	 * 
	 * @param hz
	 */
	public void setPWMFreq(Integer hz) { // Analog servos run at ~60 Hz updates
		log.info(String.format("servoPWMFreq %s hz", hz));

		int prescale_value = Math.round(osc_clock / precision / hz) - 1;
		// Set sleep mode before changing PWM freqency
		byte[] buffer1 = { PCA9685_MODE1, PCA9685_SLEEP };
		controller.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer1, buffer1.length);

		// Wait 1 millisecond until the oscillator has stabilized
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			if (Thread.interrupted()) { // Clears interrupted status!
			}
		}

		// Write the PWM frequency value
		byte[] buffer2 = { PCA9685_PRESCALE, (byte) prescale_value };
		controller.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer2, buffer2.length);

		// Leave sleep mode, set autoincrement to be able to write several
		// bytes
		// in sequence
		byte[] buffer3 = { PCA9685_MODE1, PCA9685_AUTOINCREMENT };
		controller.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer3, buffer3.length);

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
		log.info(String.format("setServo %s deviceAddress %S pin %s pulse %s", pin, deviceAddress, pin, pulseWidthOff));
		byte[] buffer = { (byte) (PCA9685_LED0_OFF_L + (pin * 4)), (byte) (pulseWidthOff & 0xff), (byte) (pulseWidthOff >> 8) };
		controller.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
	}

	public String getControllerName() {

		String controlerName = null;

		if (controller != null) {
			controlerName = controller.getName();
		}

		return controlerName;
	}

	public I2CControl getController() {
		return controller;
	}

	public synchronized boolean servoAttach(Servo servo, Integer pinNumber) {
		if (servo == null) {
			error("trying to attach null servo");
			return false;
		}

		if (controller == null) {
			error("trying to attach a pin before attaching to an i2c controller");
			return false;
		}

		servo.setController(this);
		servoNameToPin.put(servo.getName(), pinNumber);

		return true;
	}

	@Override
	public void servoSweepStart(Servo servo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void servoSweepStop(Servo servo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void servoWrite(Servo servo) {
		if (!pwmFreqSet) {
			setPWMFreq(pwmFreq);
		}
		log.info(String.format("servoWrite %s deviceAddress %s targetOutput %d", servo.getName(), deviceAddress, servo.targetOutput));
		int pulseWidthOff = SERVOMIN + (int) (servo.targetOutput * (int) ((float) SERVOMAX - (float) SERVOMIN) / (float) (180));
		setServo(getPin(servo), pulseWidthOff);
	}

	@Override
	public void servoWriteMicroseconds(Servo servo) {
		if (!pwmFreqSet) {
			setPWMFreq(pwmFreq);
		}
		// 1000 ms => 150, 2000 ms => 600
		int pulseWidthOff = (int) (servo.uS * 0.45) - 300;
		// since pulseWidthOff can be larger than > 256 it needs to be
		// sent as 2 bytes
		log.info(String.format("servoWriteMicroseconds %s deviceAddress x%02X pin %s pulse %d", servo.getName(), deviceAddress, getPin(servo), pulseWidthOff));

		byte[] buffer = { (byte) (PCA9685_LED0_OFF_L + (getPin(servo) * 4)), (byte) (pulseWidthOff & 0xff), (byte) (pulseWidthOff >> 8) };
		controller.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
	}

	@Override
	public boolean servoEventsEnabled(Servo servo, boolean enabled) {
		// @GroG. What is this method supposed to do ?
		// return arduino.servoEventsEnabled(servo, enabled);
		return enabled;
	}

	@Override
	public void setServoSpeed(Servo servo) {
		// TODO Auto-generated method stub.

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
		 * meta.addPeer("arduino", "Arduino", "our Arduino"); meta.addPeer("raspi",
		 * "RasPi", "our RasPi");
		 */
		return meta;
	}

	/**
	 * Is this method intended to do a complete detach so that the pin can be
	 * reassigned to a different servo ?
	 */
	@Override
	public void servoDetach(Servo servo) {
		int pin = getPin(servo);
		setPWM(pin, 4096, 0);
	}

	@Override
	public void attachDevice(Device device, Object... config) {
		// TODO - any more setup required
		// Commented out. Can't have any Ardino specific methods here. /Mats
		// arduino.attachDevice(device, config);
		// @Grog. What is this methods expected to do ?
	}

	@Override
	public void detachDevice(Device servo) {
		// Commented out. Can't have any Ardino specific methods here. /Mats
		// arduino.detachDevice(servo);
		// @Grog. What is this methods expected to do ?
	}

	/**
	 * I do not remember if the I2C Servo driver has a concept of
	 * Servo.attach/detach like Arduino <Servo.h> does. I would think it does,
	 * this really means go and stay at a position, vs power off to the servo
	 */
	@Override
	public void servoAttach(Servo servo) {
		// TODO Auto-generated method stub

	}

	/**
	 * Device attach - this should be creating the I2C device on MRLComm for the
	 * "first" servo if not already created - Since this does not use the Arduino
	 * <Servo.h> servos - it DOES NOT need to create "Servo" devices in MRLComm.
	 * It will need to keep track of the "pin" to I2C address, and whenever a
	 * ServoControl.moveTo(79) - the Servo will tell this controller its name &
	 * location to move.
	 * 
	 * This service will translate the name & location to an I2C address & value
	 * write request to the MRLComm device.
	 * 
	 * Mats comments on the above MRLComm should not know anything about the
	 * servos in this case. This service keeps track of the servos. MRLComm should
	 * not know anything about what addresses are used on the i2c bus MRLComm
	 * should initiate the i2c bus when it receives the first i2c write or read
	 * This service knows nothing about other i2c devices that can be on the same
	 * bus. And most important. This service knows nothing about MRLComm at all.
	 * I.e except for this bunch of comments :-)
	 * 
	 * It implements the methods defined in the ServoController and translates the
	 * servo requests to i2c writes using the I2CControl interface
	 * 
	 */
	@Override
	public void attach(Servo servo, int pin) {
		// potentially you could do your own speed control
		// on MRLComm similar to how speed control is currently done
		// with <Servo.h> servos - where MRLComm incrmentally moves them
		// on updateDevice
		servoNameToPin.put(servo.getName(), pin);
	}

	/**
	 * probably just call servoDetach, if desired on the last servo removed it
	 * "could" free the actual I2C device in the MRLComm deviceList
	 */
	@Override
	public void detach(Servo servo) {
		servoNameToPin.remove(servo.getName());
	}

	@Override
	public Integer getPin(Servo servo) {
		return servoNameToPin.get(servo.getName());
	}
}
