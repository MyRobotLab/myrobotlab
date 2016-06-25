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

	HashMap<String, Integer> servoMap = new HashMap<String, Integer>();

	// Variable to ensure that a PWM freqency has been set before starting PWM
	private int pwmFreq = 60;
	private boolean pwmFreqSet = false;

	public List<String> deviceAddressList = Arrays.asList("0x60", "0x61", "0x62", "0x63", "0x64", "0x65", "0x66", "0x67", "0x68", "0x69", "0x6A", "0x6B", "0x6C", "0x6D", "0x6E",
			"0x6F", "0x70", "0x71", "0x72", "0x73", "0x74", "0x75", "0x76", "0x77", "0x78", "0x79", "0x7A", "0x7B", "0x7C", "0x7D", "0x7E", "0x7F");

	public String deviceAddress = "0x60";

	public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8");
	public String deviceBus = "1";

	public String type = "PCA9685";

	public transient final static Logger log = LoggerFactory.getLogger(Adafruit16CServoDriver.class.getCanonicalName());

	HashMap<String, Integer> servoNameToPinMap = new HashMap<String, Integer>();

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

	public static final int PWM_FREQ = 60; // default frequency for servos
	public static final int osc_clock = 25000000; // clock frequency of the
													// internal clock
	public static final int precision = 4096; // pwm_precision

	public ArrayList<String> controllers;
	public String controllerName;
	private boolean isAttached = false;
	
	/**
	 * @Mats - added by GroG - was wondering if this would help,
	 * probably you need a reverse index too ?
	 */
	HashMap<String, DeviceMapping> servoToPin = new HashMap<String, DeviceMapping>();
	HashMap<Integer, DeviceMapping> pinToServo = new HashMap<Integer, DeviceMapping>();

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

	public ArrayList<String> refreshControllers() {
		controllers = Runtime.getServiceNamesFromInterface(I2CControl.class);
		return controllers;
	}

	// ----------- AFMotor API End --------------
	// TODO 
	// Implement MotorController
	//
	
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
		isAttached = true;

		broadcastState();
		return true;
	}

	public void unsetController() {
		controller = null;
		this.deviceBus = null;
		this.deviceAddress = null;
		isAttached = false;
		broadcastState();
	}

	// FIXME - put in ServoController interface !!!
	public boolean attach(Servo servo, Integer pinNumber) {
		if (servo == null) {
			error("trying to attach null servo");
			return false;
		}

		servo.setController(this);
		servoNameToPinMap.put(servo.getName(), pinNumber);
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

	// The I2CControler methods should be used for both Arduino and RasPi
	// The buffer values and the offset and size needs to be changed to correct
	// values.
	public void begin() {

			byte[] buffer = { PCA9685_MODE1, 0x0 };
			controller.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);

	}

	// motor controller api

	// @Override
	public boolean isAttached() {
		return isAttached;
	}

	public void setDeviceAddress(String DeviceAddress) {
		this.deviceAddress = DeviceAddress;
	}

	// drive the true PWM. I
	public void setPWM(Integer servoNum, Integer pulseWidthOn, Integer pulseWidthOff) {

			byte[] buffer = { (byte) (PCA9685_LED0_ON_L + (servoNum * 4)), (byte) (pulseWidthOn & 0xff), (byte) (pulseWidthOn >> 8), (byte) (pulseWidthOff & 0xff),
					(byte) (pulseWidthOff >> 8) };
			controller.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
		
	}

	public void setPWMFreq(Integer hz) { // Analog servos run at ~60 Hz updates
		log.info(String.format("servoPWMFreq %s hz", hz));

			int prescale_value = Math.round(osc_clock / precision / PWM_FREQ) - 1;
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
	}

	public void setServo(Integer servoNum, Integer pulseWidthOff) {
		// since pulseWidthOff can be larger than > 256 it needs to be
		// sent as 2 bytes
		log.info(String.format("setServo %s deviceAddress %S pin %s pulse %s", servoNum, deviceAddress, servoNum, pulseWidthOff));
			byte[] buffer = { (byte) (PCA9685_LED0_OFF_L + (servoNum * 4)), (byte) (pulseWidthOff & 0xff), (byte) (pulseWidthOff >> 8) };
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
		servoNameToPinMap.put(servo.getName(), pinNumber);

			controller.createI2cDevice(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), type);
	
		return true;
	}

	@Override
	public void attachDevice(Device device, int[] config) {
		// TODO - any more setup required
		// Commented out. Can't have any Ardino specific methods here. /Mats 
		// arduino.attachDevice(device, config);
	}

	@Override
	public void detachDevice(Device servo) {
		// Commented out. Can't have any Ardino specific methods here. /Mats 
		// arduino.detachDevice(servo);
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
		setServo(servoToPin.get(servo.getName()).getIndex(), pulseWidthOff);
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
		log.info(String.format("servoWriteMicroseconds %s deviceAddress x%02X pin %s pulse %d", servo.getName(), deviceAddress, servoToPin.get(servo.getName()), pulseWidthOff));

			byte[] buffer = { (byte) (PCA9685_LED0_OFF_L + (servoToPin.get(servo.getName()).getIndex() * 4)), (byte) (pulseWidthOff & 0xff), (byte) (pulseWidthOff >> 8) };
			controller.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
	}

	@Override
	public boolean servoEventsEnabled(Servo servo, boolean enabled) {
		// Commented out. Cant have any Arduino specific code here /Mats
		// return arduino.servoEventsEnabled(servo, enabled);
	}

	@Override
	public void setServoSpeed(Servo servo) {
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

	/**
	 * this should exist for the I2C but do not know the correct I2C write
	 * command
	 */
	@Override
	public void servoDetach(Servo servo) {
		int pin = servoToPin.get(servo.getName()).getIndex();
		// FIXME send i2c command to detach
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
	 * "first" servo if not already created - Since this does not use the
	 * Arduino <Servo.h> servos - it DOES NOT need to create "Servo" devices in
	 * MRLComm. It will need to keep track of the "pin" to I2C address, and
	 * whenever a ServoControl.moveTo(79) - the Servo will tell this controller
	 * its name & location to move.
	 * 
	 * This service will translate the name & location to an I2C address & value
	 * write request to the MRLComm device.
	 */
	@Override
	public void attach(Servo servo, int pin) {
		// just guessing here what needs to be done
		// if its our first servo - we need to create an I2C device on MRLComm
		if (servoToPin.size() == 0){
			// @Mats - is this re-entrant ?
			// does it add the MRL I2C device ?
			SetDeviceAddress(deviceBus);
		}
		
		// potentially you could do your own speed control
		// on MRLComm similar to how speed control is currently done
		// with <Servo.h> servos - where MRLComm incrmentally moves them
		// on updateDevice 
		DeviceMapping mapping = new DeviceMapping(servo, new int[]{pin});
		servoToPin.put(servo.getName(), mapping);
		pinToServo.put(pin, mapping);
		
		// TODO - do the I2C equivalent of <Servo.h> Servo.attach		

	}

	/**
	 * probably just call servoDetach, if desired on the last servo removed it
	 * "could" free the actual I2C device in the MRLComm deviceList
	 */
	@Override
	public void detach(Servo servo) {
		int temp = servoToPin.get(servo.getName()).getIndex();
		servoToPin.remove(servo.getName());
		pinToServo.remove(temp);
		
		if (servoToPin.size() == 0){
			// TODO - free I2C device
		}

	}

	@Override
	public Integer getPin(Servo servo) {
		// TODO Auto-generated method stub
		return null;
	}

}
