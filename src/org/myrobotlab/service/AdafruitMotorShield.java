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

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino.Sketch;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.ArduinoShield;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.StepperController;
import org.slf4j.Logger;

/**
 * AdaFruit Motor Shield Controller Service
 * 
 * @author GroG
 * 
 *         References : http://www.ladyada.net/make/mshield/use.html
 */

public class AdafruitMotorShield extends Service implements MotorController, StepperController, ArduinoShield {
	/** version of the library */
	static public final String VERSION = "0.9";

	private static final long serialVersionUID = 1L;

	/*
	 * TODO - make step calls NON BLOCKING 1. make step calls non-blocking -
	 * they don't block MRL - but they block the processing of the Arduino which
	 * is not needed (or desired) 2. nice GUIService for steppers 3. release
	 * functionality 4. style set <-- easy
	 */

	// AF Shield controls these 2 servos
	// makes "attaching" impossible
	// Servo servo10;

	final public int FORWARD = 1;
	final public int BACKWARD = 2;
	final public int BRAKE = 3;
	final public int RELEASE = 4;

	final public int SINGLE = 1;
	final public int DOUBLE = 2;
	final public int INTERLEAVE = 3;
	final public int MICROSTEP = 4;

	HashMap<String, Integer> deviceNameToNumber = new HashMap<String, Integer>();

	transient public HashMap<String, Motor> motors = new HashMap<String, Motor>();
	transient public HashMap<String, Stepper> steppers = new HashMap<String, Stepper>();
	transient public HashMap<String, Servo> servos = new HashMap<String, Servo>();

	transient public Arduino arduino = null;

	final int AF_DCMOTOR_ATTACH = 51;
	final int AF_DCMOTOR_DETACH = 52;
	final int AF_DCMOTOR_RELEASE = 53;
	final int AF_DCMOTOR_SET_SPEED = 54;
	final int AF_DCMOTOR_RUN_COMMAND = 55;

	// FIXME - COMPLETE IMPLEMENTATION BEGIN --
	final int AF_STEPPER_ATTACH = 56;
	final int AF_STEPPER_DETACH = 57; // release
	final int AF_STEPPER_RELEASE = 58; // release
	final int AF_STEPPER_STEP = 59;
	final int AF_STEPPER_SET_SPEED = 60;
	// FIXME - COMPLETE IMPLEMENTATION END --

	public static final String ADAFRUIT_DEFINES =

	"\n\n" + "#include <AFMotor.h>\n\n" + " #define AF_DCMOTOR_ATTACH 51\n" + " #define AF_DCMOTOR_DETACH 52\n" + " #define AF_DCMOTOR_RELEASE 53\n"
			+ " #define AF_DCMOTOR_SET_SPEED 54\n" + " #define AF_DCMOTOR_RUN_COMMAND 55\n" + "\n" + " #define AF_STEPPER_ATTACH 56\n" + " #define AF_STEPPER_DETACH 57\n"
			+ " #define AF_STEPPER_RELEASE 58\n" + " #define AF_STEPPER_STEP 59\n" + " #define AF_STEPPER_SET_SPEED 60\n" +

			" AF_DCMotor* motorMap[4];\n" + " AF_Stepper* stepperMap[2];\n" + "\n\n";

	public int direction = FORWARD;

	// the last amount (abs) step command
	private int step = 0;

	public transient final static Logger log = LoggerFactory.getLogger(AdafruitMotorShield.class.getCanonicalName());

	public static final String ADAFRUIT_SETUP = "";

	public static final String ADAFRUIT_CODE = "\n\n" + "            case AF_DCMOTOR_ATTACH:{ \n" + "              motorMap[ioCommand[1] - 1] =  new AF_DCMotor(ioCommand[1]);\n "
			+ "            }\n" + "            break; \n" +

			"            case AF_DCMOTOR_DETACH:{ \n" + "             motorMap[ioCommand[2]]->run(RELEASE); \n" + "             delete motorMap[ioCommand[2]];\n"
			+ "            }\n" + "            break; \n" +

			"            case AF_DCMOTOR_RELEASE:{ \n" + "             motorMap[ioCommand[2]]->run(RELEASE); \n" + "            }\n" + "            break; \n" +

			"            case AF_DCMOTOR_RUN_COMMAND:{ \n" + "             motorMap[ioCommand[1]]->run(ioCommand[2]); \n" + "            }\n" + "            break; \n" +

			"            case AF_DCMOTOR_SET_SPEED:{ \n" + "             motorMap[ioCommand[1]]->setSpeed(ioCommand[2]); \n" + "            }\n" + "            break; \n" +

			"\n\n" + "            case AF_STEPPER_ATTACH:{ \n" + "              stepperMap[ioCommand[2] - 1] = new AF_Stepper (ioCommand[1], ioCommand[2]);\n " + "            }\n"
			+ "            break; \n" +

			"            case AF_STEPPER_DETACH:{ \n" + "             stepperMap[ioCommand[1]-1]->release(); \n" + "             delete stepperMap[ioCommand[1]-1]; \n"
			+ "            }\n" + "            break; \n" +

			"            case AF_STEPPER_RELEASE:{ \n" + "             stepperMap[ioCommand[1]-1]->release(); \n" + "            }\n" + "            break; \n" +

			"            case AF_STEPPER_STEP:{ \n" + "             stepperMap[ioCommand[1]-1]->step(((ioCommand[2] << 8) + ioCommand[3]), ioCommand[4], ioCommand[5]); \n"
			+ "            }\n" + "            break; \n" +

			"            case AF_STEPPER_SET_SPEED:{ \n" + "             stepperMap[ioCommand[1]-1]->setSpeed(ioCommand[2]); \n" + "            }\n" + "            break; \n";

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

		try {
			// FIXME !!! - don't use Adafruit's library - do your own stepper control through "pure" MRLComm.ino
			AdafruitMotorShield fruity = (AdafruitMotorShield) Runtime.createAndStart("fruity", "AdafruitMotorShield");
			Runtime.createAndStart("gui01", "GUIService");

			fruity.connect("COM3");

			Motor motor1 = fruity.createDCMotor(4);
			motor1.move(0.4f);

			// create a 200 step stepper on adafruitsheild port 1
			Stepper stepper1 = fruity.createStepper(200, 1);

			// FIXME - needs to be cleaned up - tear down
			fruity.releaseStepper(stepper1.getName());

			// Runtime.createAndStart("python", "Python");
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public AdafruitMotorShield(String n) {
		super(n);
		arduino = (Arduino) createPeer("arduino");
	}

	/**
	 * an Arduino does not need to know about a shield but a shield must know
	 * about a Arduino Arduino owns the script, but a Shield needs additional
	 * support Shields are specific - but plug into a generalized Arduino
	 * Arduino shields can not be plugged into other uCs
	 * 
	 * TODO - Program Version & Type injection - with feedback + query to load
	 */
	@Override
	public boolean attach(Arduino inArduino) {

		if (inArduino == null) {
			error("can't attach - arduino is invalid");
			return false;
		}

		this.arduino = inArduino;

		// arduinoName; FIXME - get clear on diction Program Script or Sketch
		StringBuffer newProgram = new StringBuffer();
		newProgram.append(arduino.getSketch());

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

		// set the program
		Sketch sketch = new Sketch("AdafruitMotorShield", newProgram.toString());
		arduino.setSketch(sketch);
		// broadcast the arduino state - ArduinoGUI should subscribe to
		// setProgram
		broadcastState(); // state has changed let everyone know

		// servo9.attach(arduinoName, 9); // FIXME ??? - createServo(Integer i)
		// servo10.attach(arduinoName, 10);

		// error(String.format("couldn't find %s", arduinoName));
		return true;
	}

	public boolean connect(String port) {
		return arduino.connect(port);
	}

	/**
	 * creates a DC Motor on port 1,2,3, or 4
	 * 
	 * @param motorNum
	 * @throws Exception
	 */
	public Motor createDCMotor(Integer motorNum) throws Exception {
		if (motorNum == null || motorNum < 1 || motorNum > 4) {
			error(String.format("motor number should be 1,2,3,4 not %d", motorNum));
			return null;
		}
		String motorName = String.format("%s_m%d", getName(), motorNum);
		deviceNameToNumber.put(motorName, motorNum);
		Motor m = new Motor(motorName);
		m.startService();
		motors.put(motorName, m);
		m.broadcastState();
		m.setController(this);
		return m;
	}

	/**
	 * creates a stepper on stepper port 1 or 2
	 * 
	 * @param stepperPort
	 */
	public Stepper createStepper(Integer steps, Integer stepperPort) {
		if (stepperPort == null || stepperPort < 1 || stepperPort > 2) {
			error(String.format("stepper number should 1 or 2 not %d", stepperPort));
			return null;
		}

		String stepperName = String.format("%s_stepper%d", getName(), stepperPort);

		if (steppers.containsKey(stepperName)) {
			warn("%s alreaady exists", stepperName);
			return steppers.get(stepperName);
		}

		Stepper s = (Stepper) Runtime.createAndStart(stepperName, "Stepper");
		if (!stepperAttach(s)) {
			return null;
		}

		return s;
	}

	@Override
	public String[] getCategories() {
		return new String[] { "shield", "motor" };
	}

	@Override
	public String getDescription() {
		return "Adafruit Motor Shield Service";
	}

	@Override
	public ArrayList<Pin> getPinList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAttached() {
		return arduino != null;
	}

	@Override
	public boolean motorAttach(String motorName, Integer pwrPin, Integer dirPin) {
		return motorAttach(motorName, Motor.TYPE_PWM_DIR, pwrPin, dirPin, null);
	}

	// ----------- AFMotor API End --------------

	@Override
	public boolean motorAttach(String motorName, String type, Integer pwrPin, Integer dirPin) {
		// TODO Auto-generated method stub
		return false;
	}

	// public static final String ADAFRUIT_SCRIPT_TYPE = "#define SCRIPT_TYPE "
	// TODO

	@Override
	public boolean motorAttach(String motorName, String type, Integer pwrPin, Integer dirPin, Integer encoderPin) {
		ServiceInterface sw = Runtime.getService(motorName);
		if (!sw.isLocal()) {
			error("motor needs to be in same instance of mrl as controller");
			return false;
		}

		Motor m = (Motor) sw;
		m.setController(this);
		m.broadcastState();
		return true;
	}

	@Override
	public boolean motorDetach(String data) {
		return false;

	}

	@Override
	public void motorMove(String name) {

		// a bit weird indirection - but this would support
		// adafruit to be attached to motors defined outside of
		// initialization
		MotorControl mc = (MotorControl) Runtime.getService(name);
		double pwr = mc.getPowerLevel();
		int pwm = (int) (pwr * 255);
		int motorPortNumber = deviceNameToNumber.get(name);

		if (pwr > 0) {
			runForward(motorPortNumber, pwm);
		} else if (pwr < 0) {
			runBackward(motorPortNumber, -1 * pwm);
		} else {
			stop(motorPortNumber);
		}

	}

	@Override
	public void motorMoveTo(String name, double position) {
		// TODO Auto-generated method stub

	}

	public void releaseDCMotor(String motorName) {
		if (!motors.containsKey(motorName)) {
			error("motor %s does not exist", motorName);
		}

		Motor m = motors.remove(motorName);
		m.releaseService();
		deviceNameToNumber.remove(motorName);
	}

	// FIXME - release releases electrical connection
	// detach remove object from memory and "releases" service
	public void releaseStepper(String stepperName) {
		if (!steppers.containsKey(stepperName)) {
			error("stepper %s does not exist", stepperName);
		}

		Stepper m = steppers.remove(stepperName);
		m.releaseService();
		deviceNameToNumber.remove(stepperName);
	}

	public void run(Integer motorPortNumber, Integer command) {
		arduino.sendMsg(AF_DCMOTOR_RUN_COMMAND, motorPortNumber - 1, command);
	}

	public void runBackward(Integer motorPortNumber, Integer speed) {
		setSpeed(motorPortNumber, speed);
		run(motorPortNumber, BACKWARD);
	}

	public void runForward(Integer motorPortNumber, Integer speed) {
		setSpeed(motorPortNumber, speed);
		run(motorPortNumber, FORWARD);
	}

	// MotorController end ----
	// StepperController begin ----

	public void setSpeed(Integer motorPortNumber, Integer speed) {
		arduino.sendMsg(AF_DCMOTOR_SET_SPEED, motorPortNumber - 1, speed);
	}

	// VENDOR SPECIFIC LIBRARY METHODS BEGIN /////
	// DC Motors
	// ----------- AFMotor API Begin --------------
	public void setSpeed(String name, Integer speed) { // FIXME - sloppy
		setSpeed(deviceNameToNumber.get(name) - 1, speed);
	}

	@Override
	public void setStepperSpeed(Integer speed) {
		// TODO Auto-generated method stub

	}

	@Override
	public void startService() {
		super.startService();
		arduino.startService();
		attach(arduino);
	}

	// Stepper Motors
	public void step(int count, int direction, int type) {

	}

	@Override
	public boolean stepperAttach(Stepper stepper) {
		// TODO - USE "PURE" MRLCOMM.INO - nothing gained with Adafruit's library

		return true;
	}

	// StepperController end ----

	@Override
	public boolean stepperAttach(String stepperName) {
		Stepper stepper = (Stepper) Runtime.createAndStart(stepperName, "Stepper");
		return stepperAttach(stepper);
	}

	@Override
	public boolean stepperDetach(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void stepperReset(String stepper) {
		// TODO Auto-generated method stub

	}

	public void stop(Integer motorPortNumber) {
		// setSpeed(motorNumber, speed);
		run(motorPortNumber, RELEASE);
	}

	@Override
	public void stepperMoveTo(String name, int pos, int style) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stepperStop(String name) {
		// TODO Auto-generated method stub
		
	}

}
