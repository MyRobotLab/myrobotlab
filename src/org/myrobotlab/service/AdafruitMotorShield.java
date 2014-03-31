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
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.ArduinoShield;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.StepperControl;
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

	/* TODO - make step calls NON BLOCKING
	 1. make step calls non-blocking - they don't block MRL - but they block the processing of the Arduino which is not needed (or desired)
	 2. nice GUIService for steppers
	 3. release functionality
	 4. style set <-- easy
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
	
	public static Peers getPeers(String name)
	{
		Peers peers = new Peers(name);
		//peers.suggestAs("tracking.x", "pan", "Servo", "shared x");
		//peers.put("keyboard", "Keyboard", "Keyboard service");
		peers.put("arduino", "Arduino", "our Arduino");
		return peers;
	}
	
	public static final String ADAFRUIT_DEFINES = 

			"\n\n" + 
					"#include <AFMotor.h>\n\n" + 
					" #define AF_DCMOTOR_ATTACH 51\n" + 
					" #define AF_DCMOTOR_DETACH 52\n" +
					" #define AF_DCMOTOR_RELEASE 53\n" +
					" #define AF_DCMOTOR_SET_SPEED 54\n" + 
					" #define AF_DCMOTOR_RUN_COMMAND 55\n"
					+ "\n" +
					" #define AF_STEPPER_ATTACH 56\n" + 
					" #define AF_STEPPER_DETACH 57\n" +
					" #define AF_STEPPER_RELEASE 58\n" +
					" #define AF_STEPPER_STEP 59\n" + 
					" #define AF_STEPPER_SET_SPEED 60\n" +
		
					" AF_DCMotor* motorMap[4];\n" + 
					" AF_Stepper* stepperMap[2];\n" + 
					"\n\n";
	

	public int direction = FORWARD;
	
	// the last amount (abs) step command
	private int step = 0;

	public transient final static Logger log = LoggerFactory.getLogger(AdafruitMotorShield.class.getCanonicalName());

	public AdafruitMotorShield(String n) {
		super(n);
		arduino = (Arduino) createPeer("arduino");
	}

	public void startService() {
		super.startService();
		arduino.startService();
		attach(arduino);
	}

	/**
	 * creates a DC Motor on port 1,2,3, or 4
	 * 
	 * @param motorNum
	 */
	public Motor createDCMotor(Integer motorNum) {
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

	public void releaseDCMotor(String motorName) {
		if (!motors.containsKey(motorName)) {
			error("motor %s does not exist", motorName);
		}

		Motor m = motors.remove(motorName);
		m.releaseService();
		deviceNameToNumber.remove(motorName);
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
		
		if (steppers.containsKey(stepperName))
		{
			warn("%s alreaady exists", stepperName);
			return steppers.get(stepperName);
		}

		Stepper s = (Stepper)Runtime.createAndStart(stepperName, "Stepper");
		if (!stepperAttach(s, steps, stepperPort))
		{
			return null;
		}
		
		return s;
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

	// VENDOR SPECIFIC LIBRARY METHODS BEGIN /////
	// DC Motors
	// ----------- AFMotor API Begin --------------
	public void setSpeed(String name, Integer speed) { // FIXME - sloppy
		setSpeed(deviceNameToNumber.get(name) - 1, speed);
	}

	public void setSpeed(Integer motorPortNumber, Integer speed) {
		arduino.sendMsg(AF_DCMOTOR_SET_SPEED, motorPortNumber - 1, speed);
	}

	public void run(Integer motorPortNumber, Integer command) {
		arduino.sendMsg(AF_DCMOTOR_RUN_COMMAND, motorPortNumber - 1, command);
	}

	public void runForward(Integer motorPortNumber, Integer speed) {
		setSpeed(motorPortNumber, speed);
		run(motorPortNumber, FORWARD);
	}

	public void runBackward(Integer motorPortNumber, Integer speed) {
		setSpeed(motorPortNumber, speed);
		run(motorPortNumber, BACKWARD);
	}

	public void stop(Integer motorPortNumber) {
		// setSpeed(motorNumber, speed);
		run(motorPortNumber, RELEASE);
	}

	// Stepper Motors
	public void step(int count, int direction, int type) {

	}

	// ----------- AFMotor API End --------------

	@Override
	public String getDescription() {
		return "Adafruit Motor Shield Service";
	}

	// public static final String ADAFRUIT_SCRIPT_TYPE = "#define SCRIPT_TYPE "
	// TODO
	
	public static final String ADAFRUIT_SETUP = "";
	
		
	public static final String ADAFRUIT_CODE = "\n\n" +
			"            case AF_DCMOTOR_ATTACH:{ \n" +
			"              motorMap[ioCommand[1] - 1] =  new AF_DCMotor(ioCommand[1]);\n " +
			"            }\n" +
			"            break; \n" +

			"            case AF_DCMOTOR_DETACH:{ \n" +
			"             motorMap[ioCommand[2]]->run(RELEASE); \n" +
			"             delete motorMap[ioCommand[2]];\n" +
			"            }\n" +
			"            break; \n" +

			"            case AF_DCMOTOR_RELEASE:{ \n" +
			"             motorMap[ioCommand[2]]->run(RELEASE); \n" +
			"            }\n" +
			"            break; \n" +
			
			"            case AF_DCMOTOR_RUN_COMMAND:{ \n" +
			"             motorMap[ioCommand[1]]->run(ioCommand[2]); \n" +
			"            }\n" +
			"            break; \n" +
						
			"            case AF_DCMOTOR_SET_SPEED:{ \n" +
			"             motorMap[ioCommand[1]]->setSpeed(ioCommand[2]); \n" +
			"            }\n" +
			"            break; \n" +

			"\n\n" +
			"            case AF_STEPPER_ATTACH:{ \n" +
			"              stepperMap[ioCommand[2] - 1] = new AF_Stepper (ioCommand[1], ioCommand[2]);\n " +
			"            }\n" +
			"            break; \n" +

			"            case AF_STEPPER_DETACH:{ \n" +
			"             stepperMap[ioCommand[1]-1]->release(); \n" +
			"             delete stepperMap[ioCommand[1]-1]; \n" +
			"            }\n" +
			"            break; \n" +

			"            case AF_STEPPER_RELEASE:{ \n" +
			"             stepperMap[ioCommand[1]-1]->release(); \n" +
			"            }\n" +
			"            break; \n" +
			
			
			"            case AF_STEPPER_STEP:{ \n" +
			"             stepperMap[ioCommand[1]-1]->step(((ioCommand[2] << 8) + ioCommand[3]), ioCommand[4], ioCommand[5]); \n" +
			"            }\n" +
			"            break; \n" +
						
			"            case AF_STEPPER_SET_SPEED:{ \n" +
			"             stepperMap[ioCommand[1]-1]->setSpeed(ioCommand[2]); \n" +
			"            }\n" +
			"            break; \n";




	/**
	 * an Arduino does not need to know about a shield but a shield must know
	 * about a Arduino Arduino owns the script, but a Shield needs additional
	 * support Shields are specific - but plug into a generalized Arduino
	 * Arduino shields can not be plugged into other uCs
	 * 
	 * TODO - Program Version & Type injection - with feedback + query to load
	 */
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
		arduino.setSketch(newProgram.toString());
		// broadcast the arduino state - ArduinoGUI should subscribe to
		// setProgram
		broadcastState(); // state has changed let everyone know

		// servo9.attach(arduinoName, 9); // FIXME ??? - createServo(Integer i)
		// servo10.attach(arduinoName, 10);

		// error(String.format("couldn't find %s", arduinoName));
		return true;
	}

	@Override
	public void motorMoveTo(String name, Integer position) {
		// TODO Auto-generated method stub

	}

	@Override
	public void motorMove(String name) {

		// a bit weird indirection - but this would support
		// adafruit to be attached to motors defined outside of
		// initialization
		MotorControl mc = (MotorControl) Runtime.getService(name);
		Float pwr = mc.getPowerLevel();
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
	public boolean motorDetach(String data) {
		return false;

	}

	@Override
	public boolean motorAttach(String motorName, Object... motorData) {
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
	public ArrayList<Pin> getPinList() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isAttached() {
		return arduino != null;
	}

	@Override
	public Object[] getMotorData(String motorName) {
		String ret = String.format("m%d", deviceNameToNumber.get(motorName));
		Object[] data = new Object[] { ret };
		return data;
	}
	
	// MotorController end ----
	// StepperController begin ----

	@Override
	public boolean stepperAttach(String stepperName, Integer steps, Object...data) {
		Stepper stepper = (Stepper)Runtime.createAndStart(stepperName, "Stepper");
		return stepperAttach(stepper, steps, data);
	}

	@Override // FIXME DEPRECATE !!!! - use method with style 
	public void stepperStep(String name, Integer steps) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stepperStep(String name, Integer inSteps, Integer style) {
		
		if (inSteps < 0)
		{
			direction = BACKWARD;
		} else {
			direction = FORWARD;
		}
		step = Math.abs(inSteps);
		arduino.sendMsg(AF_STEPPER_STEP, deviceNameToNumber.get(name), step >> 8 & 0xFF, step & 0xFF, direction, style);
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

	@Override
	public boolean stepperAttach(StepperControl stepperControl, Integer steps, Object...data) {
		if (data.length != 1 || data[0].getClass() != Integer.class)
		{
			error("Adafruit stepper needs 1 Integers to specify port");
			return false;
		}
		
		Stepper stepper = (Stepper) stepperControl; // FIXME - only support stepper at the moment .. so not a big deal ... yet :P
		Integer stepperPort = (Integer)data[0];
		if (arduino == null || !arduino.isConnected())
		{
			error(String.format("can not attach servo %s before Arduino %s is connected", stepper.getName(), getName()));
			return false;
		}
		stepper.setController(this);
		stepper.broadcastState();
		
		steppers.put(stepper.getName(), stepper);
		arduino.sendMsg(AF_STEPPER_ATTACH, steps, stepperPort);
		deviceNameToNumber.put(stepper.getName(), stepperPort);
		arduino.sendMsg(AF_STEPPER_SET_SPEED, stepperPort, 10); // default to 10 rpm 
		
		return true;
	}
	// StepperController end ----
	
	public  boolean connect(String port) {
		if (arduino == null)
		{
			error("arduino %s is null", arduino.getName());
			return false;
		}
		
		return arduino.connect(port);
	}
	
	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		AdafruitMotorShield fruity = (AdafruitMotorShield) Runtime.createAndStart("fruity", "AdafruitMotorShield");
		Runtime.createAndStart("gui01", "GUIService");
		
		fruity.connect("COM3");
		
		Motor motor1 = fruity.createDCMotor(4);
		motor1.move(0.4f);
		
		// create a 200 step stepper on adafruitsheild port 1
		Stepper stepper1 = fruity.createStepper(200, 1); 
		
		// step 100 in one direction
		stepper1.step(100);
		// step 100 in the other
		stepper1.step(-100);
		
		// FIXME - needs to be cleaned up - tear down
		fruity.releaseStepper(stepper1.getName());

		//Runtime.createAndStart("python", "Python");
	}


}
