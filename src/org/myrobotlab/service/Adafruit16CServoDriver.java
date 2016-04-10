/*
 * 
 *   AdafruitMotorShield
 *   
 *   TODO - test with Steppers & Motors - switches on board - interface accepts motor control
 *
 */

package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.myrobotlab.framework.MRLException;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.repo.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino.ServoData;
import org.myrobotlab.service.Arduino.Sketch;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.ArduinoShield;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

import com.pi4j.io.i2c.I2CBus;

/**
 * AdaFruit Motor Shield Controller Service
 * 
 * @author GroG
 * 
 *         References : http://www.ladyada.net/make/mshield/use.html
 */

public class Adafruit16CServoDriver extends Service implements ArduinoShield, ServoController {
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
	transient public Arduino arduino = null;
	transient public RasPi raspi = null;
	// Used during development to switch between Arduino and RasPi specific code
	// Not needed when both use I2CControl interface
	public String controler = "RasPI"; 
	
	HashMap<String, Integer> servoMap = new HashMap<String, Integer>();

	// TODO - The commands below should be removed, when a generic i2c interface has been created
	public final int AF_BEGIN = 50;
	public final int AF_SET_PWM_FREQ = 51;
	public final int AF_SET_PWM = 52;
	public final int AF_SET_SERVO = 53;

	// Variable to ensure that a PWM freqency has been set before starting PWM
	private int pwmFreq = 60;
	private boolean pwmFreqSet = false;
	
    // Default i2cAddress
	public int busAddress = I2CBus.BUS_1;
	public int deviceAddress = 0x40;
	public String type = "PCA9685";

	public transient final static Logger log = LoggerFactory.getLogger(Adafruit16CServoDriver.class.getCanonicalName());

	// TODO - Remove this code. It's not needed for code injection any more
	/*
	public static final String ADAFRUIT_DEFINES = "\n#define AF_SET_PWM 52\n" + "#define AF_BEGIN 50\n" + "#define AF_SET_SERVO 53\n"
			+ "#define SERVOMIN  150 // this is the 'minimum' pulse length count (out of 4096)\n"
			+ "#define SERVOMAX  600 // this is the 'maximum' pulse length count (out of 4096)\n\n" + "\n\n#include <Wire.h>\n" + "#include <Adafruit_PWMServoDriver.h>\n"
			+ "// called this way, it uses the default address 0x40\n" + "Adafruit_PWMServoDriver pwm = Adafruit_PWMServoDriver();\n" +

			"int servoNum = 0; // servoNum is currently active servo";

	public static final String ADAFRUIT_SETUP = "\n\n" + "   pwm.begin();\n   pwm.setPWMFreq(60);  // Analog servos run at ~60 Hz updates \n";

	public static final String ADAFRUIT_CODE = "\n\n" +

	"            case AF_SET_PWM:{ \n" + "             pwm.setPWM(ioCmd[1], ioCmd[2], ioCmd[3]); \n" + "            break;} \n" + "            case AF_BEGIN:{ \n"
			+ "             pwm.begin(); \n" + "            break;} \n" + "            case AF_SET_SERVO:{ \n"
			+ "            pwm.setPWM(ioCmd[1], 0, (ioCmd[2] << 8) + ioCmd[3]); \n" + "            break; }\n";
    */
	HashMap<String, Integer> servoNameToPinMap = new HashMap<String, Integer>();

	public static final int  PCA9685_MODE1 = 0x00;          // Mode 1 register
	public static final byte PCA9685_SLEEP = 0x10;          // Set sleep mode, before changing prescale value
	public static final byte PCA9685_AUTOINCREMENT = 0x20;  // Set autoincrement to be able to write more than one byte in sequence

	public static final byte  PCA9685_PRESCALE = (byte)0xFE;  // PreScale register

	// Pin PWM addresses 4 bytes repeats for each pin so I only define pin 0
	// The rest of the addresses are calculated based on pin numbers
	public static final int  PCA9685_LED0_ON_L  = 0x06;  // First LED address Low
	public static final int  PCA9685_LED0_ON_H  = 0x07;  // First LED address High
	public static final int  PCA9685_LED0_OFF_L = 0x08;  // First LED address Low
	public static final int  PCA9685_LED0_OFF_H = 0x08;  // First LED address High


	public static final int PWM_FREQ = 60;        // default frequency for servos
	public static final int osc_clock = 25000000; // clock frequency of the internal clock
	public static final int precision = 4096;     // pwm_precision 

	/*
	public static final int pin   = 3;             // pin number
	public static final int left  = 300;           // left position
	public static final int right = 450;           // right position
   */ 
	
	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Adafruit16CServoDriver driver = (Adafruit16CServoDriver) Runtime.start("pwm", "Adafruit16CServoDriver");

	}

	public Adafruit16CServoDriver(String n) {
		super(n);
		// Only one should be created
		arduino = (Arduino) createPeer("arduino");
		raspi   = (RasPi) createPeer("raspi");
	}

	// ----------- AFMotor API End --------------

	// attachControllerBoard ??? FIXME FIXME FIXME - should "attach" call
	// another's attach?
	/**
	 * an Arduino does not need to know about a shield but a shield must know
	 * about a Arduino Arduino owns the script, but a Shield needs additional
	 * support Shields are specific - but plug into a generalized Arduino
	 * Arduino shields can not be plugged into other uCs
	 * 
	 * TODO - Program Version & Type injection - with feedback + query to load
	 */
	@Override
	public boolean attach(Arduino arduino) {

		if (arduino == null) {
			error("can't attach - arduino is invalid");
			return false;
		}

		this.arduino = arduino;

		// FIXME - better way to do this might be custom messaging within the
		// MRLComm protocol - or re-implementation of the library with MRLComm
		// :(
		// arduinoName; FIXME - get clear on diction Program Script or Sketch
		StringBuffer newProgram = new StringBuffer();
		newProgram.append(arduino.getSketch().data);

		/*
		// modify the program
		int insertPoint = newProgram.indexOf(Arduino.VENDOR_DEFINES_BEGIN);

		if (insertPoint > 0) {
			newProgram.insert(Arduino.VENDOR_DEFINES_BEGIN.length() + insertPoint, ADAFRUIT_DEFINES);
		} else {
			error("could not find insert point in MRLComm.ino");
			// get info back to user
			return false;
		}

		insertPoint = newProgram.indexOf(Arduino.VENDOR_SETUP_BEGIN);

		if (insertPoint > 0) {
			newProgram.insert(Arduino.VENDOR_SETUP_BEGIN.length() + insertPoint, ADAFRUIT_SETUP);
		} else {
			error("could not find insert point in MRLComm.ino");
			// get info back to user
			return false;
		}

		insertPoint = newProgram.indexOf(Arduino.VENDOR_CODE_BEGIN);

		if (insertPoint > 0) {
			newProgram.insert(Arduino.VENDOR_CODE_BEGIN.length() + insertPoint, ADAFRUIT_CODE);
		} else {
			error("could not find insert point in MRLComm.ino");
			// get info back to user
			return false;
		}
		*/
		
		// set the program
		Sketch sketch = new Sketch("Adafruit16CServoDriver", newProgram.toString());
		arduino.setSketch(sketch);
		// broadcast the arduino state - ArduinoGUI should subscribe to
		// setProgram
		broadcastState(); // state has changed let everyone know
		arduino.broadcastState();
		// servo9.attach(arduinoName, 9); // FIXME ??? - createServo(Integer i)
		// servo10.attach(arduinoName, 10);

		// error(String.format("couldn't find %s", arduinoName));
		return true;
	}

	// public static final String ADAFRUIT_SCRIPT_TYPE = "#define SCRIPT_TYPE "
	// TODO

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
	
	public boolean attach(RasPi raspi) {

		if (raspi == null) {
			error("can't attach - RasPi is invalid");
			return false;
		}

		this.raspi = raspi;
		return true;
	}
	// VENDOR SPECIFIC LIBRARY METHODS BEGIN /////
	// ----------- AF16C API Begin --------------
	// TODO The Arduino specific parts should be move to the Arduino service
	//      The I2CControler methods should be used for both Arduino and RasPi
	//      The buffer valuse and the offset and size needs to be changed to correct values.
	public void begin() {
        if (controler == "Arduino"){
        	arduino.sendMsg(AF_BEGIN, deviceAddress, 0, 0);
        }
        else {
        	byte[] buffer = {PCA9685_MODE1,0x0};
			raspi.i2cWrite(busAddress, deviceAddress, buffer, buffer.length);
        }
	}

	public boolean connect(String comPort) {
		return arduino.connect(comPort);
	}

	public Arduino getArduino() {
		return arduino;
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

	@Override
	public boolean isAttached() {
		return arduino != null;
	}

	
	public void setDeviceAddress(Integer DeviceAddress) {
		deviceAddress = DeviceAddress;
	}
	
	// drive the true PWM. I
	public void setPWM(Integer servoNum, Integer pulseWidthOn, Integer pulseWidthOff) {
        if (controler == "Arduino"){
        	arduino.sendMsg(AF_SET_PWM, deviceAddress, servoNum, pulseWidthOn, pulseWidthOff);
        }
        else {
        	byte[] buffer = {(byte) (PCA9685_LED0_ON_L + (servoNum * 4)),
        			        (byte)(pulseWidthOn&0xff),(byte)(pulseWidthOn>>8),
        			        (byte)(pulseWidthOff&0xff),(byte)(pulseWidthOff>>8)};
        	raspi.i2cWrite(busAddress,deviceAddress, buffer, buffer.length);
        }
        
	}
	
	public void setPWMFreq(Integer hz) { // Analog servos run at ~60 Hz updates
        if (controler == "Arduino"){
		  arduino.sendMsg(AF_SET_PWM_FREQ, deviceAddress, hz, 0);
        }
        else
        {
        	int prescale_value = Math.round(osc_clock / precision / PWM_FREQ) -1;
            // Set sleep mode before changing PWM freqency
        	byte[] buffer1 = {PCA9685_MODE1, PCA9685_SLEEP};
        	raspi.i2cWrite(busAddress, deviceAddress, buffer1, buffer1.length);
            
        	// Wait 1 millisecond until the oscillator has stabilized
        	try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				  if (Thread.interrupted()) { // Clears interrupted status!
				  }
			}
            
            // Write the PWM frequency value
        	byte[] buffer2 = {PCA9685_PRESCALE, (byte) prescale_value};
            raspi.i2cWrite(busAddress, deviceAddress, buffer2, buffer2.length );

            // Leave sleep mode, set autoincrement to be able to write several bytes in sequence
        	byte[] buffer3 = {PCA9685_MODE1, PCA9685_AUTOINCREMENT};
            raspi.i2cWrite(busAddress, deviceAddress, buffer3, buffer3.length );
            
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
		log.info(String.format("setServo %s deviceAddress x%02X pin %s pulse %s", servoNum, deviceAddress, servoNum, pulseWidthOff));
        if (controler == "Arduino"){
        	arduino.sendMsg(AF_SET_SERVO, deviceAddress, servoNum, pulseWidthOff >> 8, pulseWidthOff & 0xFF);
        }
        else {
        	byte[] buffer = {(byte) (PCA9685_LED0_OFF_L + (servoNum * 4)),
			        (byte)(pulseWidthOff&0xff),(byte)(pulseWidthOff>>8)};
        	raspi.i2cWrite(busAddress, deviceAddress, buffer, buffer.length);
        }
	}
	
	
	@Override
	public void startService() {
		super.startService();
		attach(arduino);
		arduino.startService();
		attach(raspi);
		raspi.startService();
		// TODO - request myArduino - re connect
	}

	@Override
	public void attach(String name) throws MRLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean detach(String name) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public synchronized boolean servoAttach(Servo servo, Integer pinNumber) {
		if (servo == null) {
			error("trying to attach null servo");
			return false;
		}

		servo.setController(this);
		servoNameToPinMap.put(servo.getName(), pinNumber);
		
		raspi.createDevice(busAddress, deviceAddress, type);
		begin();
		
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
		log.info(String.format("servoWrite %s deviceAddress x%02X pin %s targetOutput %d", servo.getName(), deviceAddress, servo.getPin(), servo.targetOutput));
		int pulseWidthOff = SERVOMIN + (int)(servo.targetOutput * (int)((float)SERVOMAX - (float)SERVOMIN) / (float)(180));
		setServo(servo.getPin(), pulseWidthOff);
	}

	@Override
	public void servoWriteMicroseconds(Servo servo) {
		if (!pwmFreqSet) {
			setPWMFreq(pwmFreq);
		}
		// 1000 ms => 150, 2000 ms => 600
		int pulseWidthOff = (int)(servo.uS * 0.45) - 300;
		// since pulseWidthOff can be larger than > 256 it needs to be
		// sent as 2 bytes
		log.info(String.format("servoWriteMicroseconds %s deviceAddress x%02X pin %s pulse %d", servo.getName(), deviceAddress, servo.getPin(), pulseWidthOff));

        if (controler == "Arduino"){		
        	arduino.sendMsg(AF_SET_SERVO, deviceAddress, servo.getPin(), pulseWidthOff >> 8, pulseWidthOff & 0xFF);
        }
        else {
        	byte[] buffer = {(byte) (PCA9685_LED0_OFF_L + (servo.getPin() * 4)),
			        (byte)(pulseWidthOff&0xff),(byte)(pulseWidthOff>>8)};
        	raspi.i2cWrite(busAddress, deviceAddress, buffer, buffer.length);
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
	 * This static method returns all the details of the class without
	 * it having to be constructed.  It has description, categories,
	 * dependencies, and peer definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData(){
		
		ServiceType meta = new ServiceType(Adafruit16CServoDriver.class.getCanonicalName());
		meta.addDescription("Adafruit Motor Shield Service");
		meta.addCategory("shield", "motor");		
		meta.addPeer("arduino", "Arduino", "our Arduino");
		meta.addPeer("raspi", "RasPi", "our RasPi");
		return meta;		
	}
}
