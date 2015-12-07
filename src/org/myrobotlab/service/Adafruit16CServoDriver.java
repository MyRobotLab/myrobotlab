/*
 * 
 *   AdafruitMotorShield
 *   
 *   TODO - test with Steppers & Motors - switches on board - interface accepts motor control
 *
 */

package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.MRLException;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino.Sketch;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.ArduinoShield;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

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
	// you
	// have!
	public final static int SERVOMIN = 150; // this is the 'minimum' pulse
	// length count (out of 4096)
	public final static int SERVOMAX = 600; // this is the 'maximum' pulse
											// length count (out of 4096)

	transient public Arduino arduino = null;
	HashMap<String, Integer> servoMap = new HashMap<String, Integer>();

	public final int AF_BEGIN = 50;
	public final int AF_SET_PWM_FREQ = 51;
	public final int AF_SET_PWM = 52;
	public final int AF_SET_SERVO = 53;

	private boolean pwmFreqSet = false;

	public transient final static Logger log = LoggerFactory.getLogger(Adafruit16CServoDriver.class.getCanonicalName());

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

	HashMap<String, Integer> servoNameToPinMap = new HashMap<String, Integer>();

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		// peers.suggestAs("tracking.x", "pan", "Servo", "shared x");
		// peers.put("keyboard", "Keyboard", "Keyboard service");
		peers.put("arduino", "Arduino", "our Arduino");
		return peers;
	}

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Adafruit16CServoDriver driver = (Adafruit16CServoDriver) Runtime.start("pwm", "Adafruit16CServoDriver");

	}

	public Adafruit16CServoDriver(String n) {
		super(n);
		arduino = (Arduino) createPeer("arduino");
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

	// VENDOR SPECIFIC LIBRARY METHODS BEGIN /////
	// ----------- AF16C API Begin --------------
	public void begin() {
		arduino.sendMsg(AF_BEGIN, 0, 0);
	}

	public boolean connect(String comPort) {
		return arduino.connect(comPort);
	}

	public Arduino getArduino() {
		return arduino;
	}

	@Override
	public String[] getCategories() {
		return new String[] { "shield", "motor" };
	}

	@Override
	public String getDescription() {
		return "Adafruit Motor Shield Service";
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

	
	// drive the servo
	public void setPWM(Integer servoNum, Integer pulseWidthOn, Integer pulseWidthOff) {
		if (!pwmFreqSet) {
			setPWMFreq(60);
		}
		arduino.sendMsg(AF_SET_PWM, servoNum, pulseWidthOn, pulseWidthOff);
	}

	public void setPWMFreq(Integer hz) { // Analog servos run at ~60 Hz updates
		arduino.sendMsg(AF_SET_PWM_FREQ, hz, 0);
	}

	public void setServo(Integer servoNum, Integer pulseWidthOff) {
		if (!pwmFreqSet) {
			setPWMFreq(60);
		}
		// since pulseWidthOff can be larger than > 256 it needs to be
		// sent as 2 bytes
		arduino.sendMsg(AF_SET_SERVO, servoNum, pulseWidthOff >> 8, pulseWidthOff & 0xFF);
	}

	@Override
	public void startService() {
		super.startService();
		attach(arduino);
		arduino.startService();
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

	@Override
	public boolean servoAttach(Servo servo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean servoDetach(Servo servo) {
		// TODO Auto-generated method stub
		return false;
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void servoWriteMicroseconds(Servo servo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean setServoEventsEnabled(Servo servo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setServoSpeed(Servo servo) {
		// TODO Auto-generated method stub
		
	}


}
