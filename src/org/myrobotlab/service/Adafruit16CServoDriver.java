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

import org.myrobotlab.framework.MRLException;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

import com.pi4j.io.i2c.I2CBus;

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

	// TODO - Only one I2CControler should be started
	transient public I2CControl controller;
	transient public Arduino arduino = null;
	transient public RasPi raspi = null;
	// Used during development to switch between Arduino and RasPi specific code
	// Not needed when both use I2CControl interface
	public String controler;

	HashMap<String, Integer> servoMap = new HashMap<String, Integer>();

	// TODO - The commands below should be removed, when a generic i2c interface
	// has been created
	public final int AF_BEGIN = 50;
	public final int AF_SET_PWM_FREQ = 51;
	public final int AF_SET_PWM = 52;
	public final int AF_SET_SERVO = 53;

	// Variable to ensure that a PWM freqency has been set before starting PWM
	private int pwmFreq = 60;
	private boolean pwmFreqSet = false;

	public List<String> deviceAddressList = Arrays.asList(
			 "0x60","0x61","0x62","0x63","0x64","0x65","0x66","0x67",
		   "0x68","0x69","0x6A","0x6B","0x6C","0x6D","0x6E","0x6F",
	     "0x70","0x71","0x72","0x73","0x74","0x75","0x76","0x77",
       "0x78","0x79","0x7A","0x7B","0x7C","0x7D","0x7E","0x7F");
			
	public String deviceAddress = "0x60";
	
	public List<String> deviceBusList = Arrays.asList(
			"0","1","2","3","4","5","6","7","8");	
	public String deviceBus = "1";
	
	public String type = "PCA9685";

	public transient final static Logger log = LoggerFactory.getLogger(Adafruit16CServoDriver.class.getCanonicalName());

	HashMap<String, Integer> servoNameToPinMap = new HashMap<String, Integer>();

	public static final int PCA9685_MODE1 = 0x00; // Mode 1 register
	public static final byte PCA9685_SLEEP = 0x10; // Set sleep mode, before
																									// changing prescale value
	public static final byte PCA9685_AUTOINCREMENT = 0x20; // Set autoincrement to
																													// be able to write
																													// more than one byte
																													// in sequence

	public static final byte PCA9685_PRESCALE = (byte) 0xFE; // PreScale register

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

	// attachControllerBoard ??? FIXME FIXME FIXME - should "attach" call
	// another's attach?
	/**
	 * an Arduino does not need to know about a shield but a shield must know
	 * about a Arduino Arduino owns the script, but a Shield needs additional
	 * support Shields are specific - but plug into a generalized Arduino Arduino
	 * shields can not be plugged into other uCs
	 * 
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
		isAttached = true;

		if (controller instanceof Arduino) {
			this.arduino = (Arduino) controller;
			controler = "Arduino";
		}

		if (controller instanceof RasPi) {
			this.raspi = (RasPi) controller;
			controler = "RasPi";
		}
		
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
	
	public void SetDeviceBus(String deviceBus){
		this.deviceBus = deviceBus;
		broadcastState();
  }
	
	boolean SetDeviceAddress(String deviceAddress){
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
	
	// VENDOR SPECIFIC LIBRARY METHODS BEGIN /////
	// ----------- AF16C API Begin --------------
	// TODO The Arduino specific parts should be move to the Arduino service
	// The I2CControler methods should be used for both Arduino and RasPi
	// The buffer values and the offset and size needs to be changed to correct
	// values.
	public void begin() {
		if (controler == "Arduino") {
			arduino.sendMsg(AF_BEGIN, Integer.decode(deviceAddress), 0, 0);
		} else {
			byte[] buffer = { PCA9685_MODE1, 0x0 };
			raspi.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
		}
	}

	public void connect(String comPort) {
		arduino.connect(comPort);
	}

	// motor controller api

	@Override
	public ArrayList<Pin> getPinList() {
		ArrayList<Pin> ret = new ArrayList<Pin>();
		for (int i = 0; i < 16; ++i) {
			Pin p = new Pin();
			p.pin = i;
			p.type = Pin.PWM_VALUE;
			ret.add(p);
		}
		return ret;
	}

	// @Override
	public boolean isAttached() {
		return isAttached;
	}

	public void setDeviceAddress(String DeviceAddress) {
		this.deviceAddress = DeviceAddress;
	}

	// drive the true PWM. I
	public void setPWM(Integer servoNum, Integer pulseWidthOn, Integer pulseWidthOff) {
		if (controler == "Arduino") {
			arduino.sendMsg(AF_SET_PWM, Integer.decode(deviceAddress), servoNum, pulseWidthOn, pulseWidthOff);
		} else {
			byte[] buffer = { (byte) (PCA9685_LED0_ON_L + (servoNum * 4)), (byte) (pulseWidthOn & 0xff), (byte) (pulseWidthOn >> 8), (byte) (pulseWidthOff & 0xff),
					(byte) (pulseWidthOff >> 8) };
			raspi.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
		}

	}

	public void setPWMFreq(Integer hz) { // Analog servos run at ~60 Hz updates
		log.info(String.format("servoPWMFreq %s hz", hz));
		if (controler == "Arduino") {
			arduino.sendMsg(AF_SET_PWM_FREQ, Integer.decode(deviceAddress), hz, 0);
			pwmFreqSet = true;
		} else {
			int prescale_value = Math.round(osc_clock / precision / PWM_FREQ) - 1;
			// Set sleep mode before changing PWM freqency
			byte[] buffer1 = { PCA9685_MODE1, PCA9685_SLEEP };
			raspi.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer1, buffer1.length);

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
			raspi.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer2, buffer2.length);

			// Leave sleep mode, set autoincrement to be able to write several bytes
			// in sequence
			byte[] buffer3 = { PCA9685_MODE1, PCA9685_AUTOINCREMENT };
			raspi.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer3, buffer3.length);

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
		if (controler == "Arduino") {
			arduino.sendMsg(AF_SET_SERVO, Integer.decode(deviceAddress), servoNum, pulseWidthOff >> 8, pulseWidthOff & 0xFF);
		} else {
			byte[] buffer = { (byte) (PCA9685_LED0_OFF_L + (servoNum * 4)), (byte) (pulseWidthOff & 0xff), (byte) (pulseWidthOff >> 8) };
			raspi.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
		}
	}

	@Override
	public void startService() {
		super.startService();
	}

	@Override
	public void attach(String name) throws MRLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void detach(String name) {
		// TODO Auto-generated method stub
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
		if (controler == "Arduino") {
			begin();
		} else {
			raspi.createI2cDevice(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), type);
		}

		return true;
	}

	@Override
	public boolean servoAttach(Servo servo) {

		return servoAttach(servo, servo.getPin());
	}

	@Override
	public boolean servoDetach(Servo servo) {

		servoNameToPinMap.remove(servo.getName());
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
		log.info(String.format("servoWrite %s deviceAddress %s pin %s targetOutput %d", servo.getName(), deviceAddress, servo.getPin(), servo.targetOutput));
		int pulseWidthOff = SERVOMIN + (int) (servo.targetOutput * (int) ((float) SERVOMAX - (float) SERVOMIN) / (float) (180));
		setServo(servo.getPin(), pulseWidthOff);
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
		log.info(String.format("servoWriteMicroseconds %s deviceAddress x%02X pin %s pulse %d", servo.getName(), deviceAddress, servo.getPin(), pulseWidthOff));

		if (controler == "Arduino") {
			arduino.sendMsg(AF_SET_SERVO, Integer.decode(deviceAddress), servo.getPin(), pulseWidthOff >> 8, pulseWidthOff & 0xFF);
		} else {
			byte[] buffer = { (byte) (PCA9685_LED0_OFF_L + (servo.getPin() * 4)), (byte) (pulseWidthOff & 0xff), (byte) (pulseWidthOff >> 8) };
			raspi.i2cWrite(Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
		}
	}

	@Override
	public boolean servoEventsEnabled(Servo servo) {
		// TODO Auto-generated method stub
		return false;
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
		 * meta.addPeer("arduino", "Arduino", "our Arduino"); meta.addPeer("raspi",
		 * "RasPi", "our RasPi");
		 */
		return meta;
	}
}
