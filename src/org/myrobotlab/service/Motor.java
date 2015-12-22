/**
 *                    
 * @author greg (at) myrobotlab.org
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.framework.MRLException;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.sensor.Encoder;
import org.myrobotlab.sensor.EncoderListener;
import org.myrobotlab.sensor.EncoderTimer;
import org.myrobotlab.service.interfaces.Microcontroller;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.MotorEncoder;
import org.myrobotlab.service.interfaces.SensorDataSink;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         represents a common continuous direct current electric motor. The
 *         motor will need a MotorController which can be one of many different
 *         types of electronics. A simple H-bridge typically has 2 bits which
 *         control the motor. A direction bit which changes the polarity of the
 *         H-bridges output
 * 
 */
public class Motor extends Service implements MotorControl, SensorDataSink, EncoderListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Motor.class.toString());

	// //////////////// Motor Types Begin
	// ////////////////////////////////////////

	/**
	 * TYPE_PWM_DIR is the type of motor controller which has a digital
	 * direction pin. If this pin is high the motor travels in one direction if
	 * the pin is low the motor travels in the reverse direction
	 * 
	 */
	public final static String TYPE_SIMPLE = "simple pwm dir";

	/**
	 * TYPE_LPWM_RPWM - is a motor controller which one pin is pulsed to go one
	 * way and the other pin is pulsed to go the other way .. both low or both
	 * high invalid? Does one pin need to be held high or low while the other is
	 * pulsed.
	 */
	public final static String TYPE_2_PWM = "2 pwm";

	/**
	 * motor with psuedo (fake) encoder. Pulses are done by the micro-controller
	 * with something like a Arduino digitalWrite 0/1 the pulses are sent both
	 * to the pin & back through the serial line as a "control not feedback"
	 * encoder
	 */
	public final static String TYPE_PULSE_STEP = "pulse step";

	/**
	 * Ya - we'll be supporting stepper soon
	 */
	public final static String TYPE_STEPPER = "stepper";

	// pin "types" - to used in different motor "types"
	public final static String PIN_TYPE_PWM = "pwm pin";
	public final static String PIN_TYPE_DIR = "dir pin";
	public final static String PIN_TYPE_PWM_LEFT = "pwm left pin";
	public final static String PIN_TYPE_PWM_RIGHT = "pwm right pin";
	public final static String PIN_TYPE_PULSE = "pulse pin";

	// SENSOR INFO BEGI

	public final static String ENCODER_TYPE_NONE = "none";
	public final static String ENCODER_TYPE_SIMPLE = "simple";

	// control
	/**
	 * FIXME - move motor - pin driver logic into Motor (it is common across all
	 * micro-controllers) for the 2 types of motor controller
	 * 
	 * Make "named" Peer versus direct controller reference
	 */

	// //////////////// Motor Types End ////////////////////////////////////////

	transient private MotorController controller = null;
	String controllerName = null;

	boolean locked = false;

	Map<String, Integer> pinMap = new TreeMap<String, Integer>();

	/**
	 * the power level requested - varies between -1.0 <--> 1.0
	 */
	double powerLevel = 0;

	Mapper powerMap = new Mapper(-1.0, 1.0, -255.0, 255.0);
	
	// position
	int currentPos = 0;
	int targetPos = 0;

	Mapper encoderMap = new Mapper(-800.0, 800.0, -800.0, 800.0);

	transient MotorEncoder encoder = null;

	String type = TYPE_SIMPLE;
	String encoderType = ENCODER_TYPE_NONE;

	Set<String> types = new HashSet<String>();
	Set<String> encoderTypes = new HashSet<String>();

	// FIXME - implements an Encoder interface
	// get a named instance - stopping and tarting should not be creating &
	// destroying
	transient Object lock = new Object();

	public Motor(String n) {
		super(n);
		types.add(TYPE_SIMPLE);
		types.add(TYPE_2_PWM);
		types.add(TYPE_PULSE_STEP);
		encoderTypes.add(ENCODER_TYPE_NONE);
		encoderTypes.add(ENCODER_TYPE_SIMPLE);
	}

	@Override
	public boolean detach() {
		boolean ret = controller.motorDetach(this);
		controllerName = null; // FIXME - should only have controllerName and
								// not isAttached - test for null
		controller = null;
		broadcastState();
		return ret;

	}

	@Override
	public String[] getCategories() {
		return new String[] { "motor" };
	}

	public String getControllerName() {
		if (controller != null) {
			return controller.getName();
		}
		return null;
	}

	@Override
	public String getDescription() {
		return "general motor service";
	}

	@Override
	public double getPowerLevel() {
		return powerLevel;
	}
	
	@Override
	public double getPowerOutput() {
		return powerMap.calc(powerLevel);
	}

	public Mapper getPowerMap() {
		return powerMap;
	}

	@Override
	public boolean isAttached() {
		return controllerName != null;
	}

	@Override
	public boolean isInverted() {
		return powerMap.isInverted();
	}

	// --------- Motor (front end) API End ----------------------------

	@Override
	public void lock() {
		log.info("lock");
		locked = true;
	}

	public void mapEncoder(double minX, double maxX, double minY, double maxY) {
		encoderMap = new Mapper(minX, maxX, minY, maxY);
		broadcastState();
	}

	public void mapPower(double minX, double maxX, double minY, double maxY) {
		powerMap = new Mapper(minX, maxX, minY, maxY);
		broadcastState();
	}

	@Override
	// not relative ! - see moveStep
	public void move(double power) {
		powerLevel = power;
		if (locked) {
			log.warn("motor locked");
			return;
		}
		controller.motorMove(this);
	}
	
	public void attach(String controllerName) throws Exception {
		attach((MotorController)Runtime.getService(controllerName));
	}

	/**
	 * GOOD - future of complicated attaching - supply all data in one horrific
	 * function signature - overload and default appropriately this sets all
	 * necessary data in the Motor - at the end of this method the controller is
	 * called, and it uses this service to pull out any necessary data to
	 * complete the attachment
	 * 
	 * @param controllerName
	 * @param motorType
	 * @param pwmPin
	 * @param dirPin
	 * @param encoderType
	 * @param encoderPin
	 * @throws MRLException
	 * 
	 *             TODO - encoder perhaps should be handled different where an
	 *             array of data is passed in Motor.setEncoder(int[]
	 *             sensorConfig)
	 */
	public void attach(MotorController controller) throws Exception {
		controllerName = controller.getName();
		// GOOD DESIGN !! - this is the extent of what our attach should be !!!
		// just call the controller's motorAttach & broadcast our state
		controller.motorAttach(this);
		broadcastState();
	}

	public Integer getPin(String name) {
		return pinMap.get(name);
	}

	@Override
	public void moveTo(int newPos, Double powerLevel) {
		this.powerLevel = powerLevel; 
		if (controller == null) {
			error(String.format("%s's controller is not set", getName()));
			return;
		}

		// targetPos = encoderMap.calc(newPos);
		targetPos = newPos;
		controller.motorMoveTo(this);
	}
	
	@Override
	public void moveTo(int newPos){
		moveTo(newPos, null);
	}

	// FIXME - DEPRECATE !!!
	@Override
	public void setController(MotorController controller) {
		this.controller = controller;
		this.controllerName = controller.getName();
	}

	@Override
	public void setInverted(boolean invert) {
		powerMap.setInverted(invert);
	}

	// ---- Servo begin ---------
	public void setMinMax(double min, double max) {
		powerMap.setMin(min);
		powerMap.setMax(max);
		broadcastState();
	}

	public void setPowerLevel(double power) {
		powerLevel = power;
	}

	@Override
	public void stop() {
		// move(0.0);
		powerLevel = 0.0;
		controller.motorStop(this);
	}

	@Override
	public void stopAndLock() {
		log.info("stopAndLock");
		move(0.0);
		lock();
	}

	@Override
	public void unlock() {
		log.info("unLock");
		locked = false;
	}

	public String getType() {
		return type;
	}

	public String getEncoderType() {
		return encoderType;
	}

	public void setEncoderType(String type) {
		encoderType = type;
	}

	public Integer updatePosition(Integer position) {
		currentPos = position;
		return position;
	}

	// Perhaps shoulb be updateSensor
	@Override
	public void update(Object data) {
		invoke("updatePosition", data);
	}

	@Override
	public String getDataSinkType() {
		// other data types available if needed
		return "java.lang.Integer";
	}

	@Override
	public int getSensorType() {
		if (type != null && type.equals(TYPE_PULSE_STEP)) {
			return SENSOR_PULSE;
		} else {
			return SENSOR_PIN;
		}
	}

	public MotorController getController() {
		return controller;
	}

	@Override
	public int[] getSensorConfig() {
		if (type.equals(TYPE_PULSE_STEP)) {
			// pulse step only needs the pwm pin
			return new int[] { pinMap.get(PIN_TYPE_PWM) };
		}
		return new int[] {};
	}

	@Override
	public boolean hasSensor() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int[] getControlPins() {
		log.info("getControlPins");
		int[] ret = new int[pinMap.size()];
		int index = 0;
		for (String pin : pinMap.keySet()) {
			Integer x = pinMap.get(pin);
			log.info(String.format("[%d] = %s (%d)", index, pin, x));
			ret[index] = x;
			++index;
		}
		return ret;
	}

	@Override
	public String[] getTypes() {
		return types.toArray(new String[types.size()]);
	}

	public void setTypePulseStep(Integer pulsePin, Integer dirPin) throws MRLException {
		this.type = TYPE_PULSE_STEP;
		pinMap.clear();
		pinMap.put(PIN_TYPE_PULSE, pulsePin);
		pinMap.put(PIN_TYPE_DIR, dirPin);
	}

	public void setTypeStepper() {
		// TODO Auto-generated method stub

	}

	public void setType2Pwm(Integer leftPwm, Integer rightPwm) throws MRLException {
		this.type = TYPE_2_PWM;
		pinMap.clear();
		pinMap.put(PIN_TYPE_PWM_LEFT, leftPwm);
		pinMap.put(PIN_TYPE_PWM_RIGHT, rightPwm);
	}

	public void setTypeSimple(Integer pwmPin, Integer dirPin) throws MRLException {
		this.type = TYPE_SIMPLE;
		pinMap.clear();
		pinMap.put(PIN_TYPE_PWM, pwmPin);
		pinMap.put(PIN_TYPE_DIR, dirPin);
	}

	/**
	 * all logic defining the configuration points of a motor type are in this
	 * one method
	 * 
	 * @param type
	 * @throws MRLException
	 */
	/*
	 * public void setType(String type) throws MRLException { if
	 * (!types.contains(type)){ throw new MRLException("%s not valid", type); }
	 * 
	 * 
	 * 
	 * this.type = type; }
	 */

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			// FIXME - all testing or replacing of main code should be new JUnit
			// tests - with virtual arduino !!!)
			String port = "COM15";

			// Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			// Runtime.createAndStart("gui", "GUIService");
			Runtime.createAndStart("webgui", "WebGui");
			// arduino.setBoard(Arduino.BOARD_TYPE_ATMEGA2560);
			// arduino.connect(port);
			// arduino.broadcastState();

			// Runtime.createAndStart("python", "Python");

			int pwmPin = 6;
			int dirPin = 7;

			int leftPwm = 6;
			int rightPwm = 7;

			// virtual hardware
			/*
			 * VirtualDevice virtual = (VirtualDevice)Runtime.start("virtual",
			 * "VirtualDevice"); virtual.createVirtualArduino(port);
			 */

			// int encoderPin= 7;
			Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			arduino.connect(port);

			arduino.pinMode(6, Arduino.OUTPUT);
			arduino.pinMode(7, Arduino.OUTPUT);

			arduino.digitalWrite(7, 1);
			// arduino.digitalWrite(6, 1);

			arduino.analogWrite(6, 255);
			arduino.analogWrite(6, 200);
			arduino.analogWrite(6, 100);
			arduino.analogWrite(6, 0);

			Motor m1 = (Motor) Runtime.start("m1", "Motor");
			m1.setTypeSimple(pwmPin, dirPin);
			
			/*
			m1.setType2Pwm(leftPwm, rightPwm);
			m1.setTypeStepper();
			m1.setTypePulseStep(pwmPin, dirPin);
			*/
			// Runtime.start("webgui", "WebGui");
			// m1.attach(arduino, Motor.TYPE_PULSE_STEP, pwmPin, dirPin);
			// m1.attach(arduino, Motor.TYPE_2_PWM, pwmPin, dirPin);
			//m1.attach(arduino, Motor.TYPE_SIMPLE, pwmPin, dirPin);
			m1.attach(arduino);
			
			m1.move(1.0);
			m1.move(-1.0);

			// TODO - overload with speed?
			m1.moveTo(250);
			m1.moveTo(700);
			m1.moveTo(250);
			m1.moveTo(250);

			arduino.setLoadTimingEnabled(true);
			arduino.setLoadTimingEnabled(false);
			m1.stop();
			m1.move(0.5);
			m1.moveTo(200);
			m1.stop();

			// Runtime.start("webgui", "WebGui");

			// arduino.motorAttach("m1", 8, 7, 54);
			// m1.setType(Motor.TYPE_PWM_DIR_FE);
			// arduino.setSampleRate(8000);
			// m1.setSpeed(0.95);
			/*
			 * arduino.motorAttach("m1", Motor.TYPE_FALSE_ENCODER, 8, 7);
			 * m1.moveTo(30); m1.moveTo(230); m1.moveTo(430); m1.moveTo(530);
			 * m1.moveTo(130); m1.moveTo(330);
			 */
			// with encoder
			// m1.moveTo(600);

			/*
			 * m1.stop(); m1.move(0.94); m1.stop(); m1.move(-0.94); m1.stop();
			 * 
			 * // arduino.motorAttach("m1", 8, 7, 54) ;
			 * 
			 * m1.moveTo(600f);
			 */
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	@Override
	public void pulse() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setEncoder(Encoder encoder) {
		// TODO Auto-generated method stub
		
	}

	
}
