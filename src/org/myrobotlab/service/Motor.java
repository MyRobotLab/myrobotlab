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

import java.util.Arrays;
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
public class Motor extends Service implements MotorControl, SensorDataSink {
	
	/*
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("controller", "Arduino", "controller");
		return peers;
	}
	*/

	/**
	 * FIXME - REMOVE - do low level counting and encoder triggers on the
	 * micro-controller !!!
	 */
	class EncoderTimer extends Thread {
		public double power = 0.0;
		public double duration = 0;

		EncoderTimer(String name, double power, double duration) {
			super(name + "_duration");
			this.power = power;
			this.duration = duration;

		}

		@Override
		public void run() {
			while (isRunning()) // this is from Service - is OK?
			{
				synchronized (lock) {
					try {
						lock.wait();

						move(this.power);
						/*
						 * inMotion = true;
						 * 
						 * Thread.sleep((double) (this.duration * 1000));
						 * 
						 * instance.stop(); inMotion = false;
						 */

					} catch (InterruptedException e) {
						log.warn("duration thread interrupted");
					}
				}
			}
		}
	}

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
	 * motor with psuedo (fake) encoder. Pulses are done by
	 * the micro-controller with something like a Arduino digitalWrite 0/1 the
	 * pulses are sent both to the pin & back through the serial line as a
	 * "control not feedback" encoder
	 */
	public final static String TYPE_PULSE_STEP = "pulse step";
	
	
	/**
	 * Ya - we'll be supporting stepper soon
	 */
	public final static String TYPE_STEPPER = "stepper";
	
	
	// pin "types" - to used in different motor "types"
	public final static String PIN_TYPE_PWM = "pwm";
	public final static String PIN_TYPE_DIR = "dir";
	public final static String PIN_TYPE_PWM_LEFT = "pwm left";
	public final static String PIN_TYPE_PWM_RIGHT = "pwm right";
	
	
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

	Map<String,Integer> pinMap = new TreeMap<String,Integer>();

	Integer encoderPin;
	// power
	double powerLevel = 1;
	double powerOutput = 0;

	Mapper powerMap = new Mapper(-1.0, 1.0, -255.0, 255.0);
	// position
	int currentPos = 0;
	int targetPos = 0;
	
	Mapper encoderMap = new Mapper(-800.0, 800.0, -800.0, 800.0);
	
	transient MotorEncoder encoder = null;

	// FIXME - REMOVE !!! DEPRECATE - just a "type" of encoder
	transient EncoderTimer durationThread = null;

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
		powerOutput = powerMap.calc(powerLevel);
		controller.motorMove(this);
	}

	/**
	 * FIXME - deprecate - make a encoding type on the micro-controller - an
	 * alert, make it very general - can be clicks from a "real" encoder .. or
	 * increments of an internal timer !!!
	 */
	@Override
	public void moveFor(double power, double duration) {
		// default is not to block
		moveFor(power, duration, false);
	}

	/**
	 * FIXME - deprecate - make a encoding type on the micro-controller - an
	 * alert, make it very general - can be clicks from a "real" encoder .. or
	 * increments of an internal timer !!!
	 */
	@Override
	public void moveFor(double power, double duration, Boolean block) {
		// TODO - remove - Timer which implements SensorFeedback should be used
		if (!block) {
			// non-blocking call to move for a duration
			if (durationThread == null) {
				durationThread = new EncoderTimer(getName(), power, duration);
				durationThread.start();
			} else {
				/*
				 * if (inMotion) { error("duration is busy with another move" +
				 * durationThread.duration); } else { synchronized (lock) {
				 * durationThread.power = power; durationThread.duration =
				 * duration; lock.notifyAll(); } }
				 */
			}
		} else {
			// block the calling thread
			move(this.powerOutput);
			// inMotion = true;

			try {
				Thread.sleep((int) (duration * 1000));
			} catch (InterruptedException e) {
				logException(e);
			}

			stop();
			// inMotion = false;

		}

	}

	/**
	 * GOOD - future of complicated attaching - supply all data in one horrific function signature - overload and default appropriately
	 * this sets all necessary data in the Motor - at the end of this method the controller is called, and it uses this service
	 * to pull out any necessary data to complete the attachment 
	 * 
	 * @param controllerName
	 * @param motorType
	 * @param pwmPin
	 * @param dirPin
	 * @param encoderType
	 * @param encoderPin
	 * @throws MRLException
	 * 
	 * TODO - encoder perhaps should be handled different where an array of data is passed in Motor.setEncoder(int[] sensorConfig)
	 */
	public void attach(MotorController controller, String motorType, int... pins) throws Exception {
		log.info("{}.attach({},{})", getName(), motorType, Arrays.toString(pins));
		
		controllerName = controller.getName();

		if (!controller.isConnected()){
			throw new IllegalArgumentException(String.format("%s is not connected", controller.getName()));
		}
		
		if (motorType == null){
			this.type = Motor.TYPE_SIMPLE;
		} else {
			this.type = motorType;
		}
		
		if (!types.contains(motorType)) {
			throw new MRLException(String.format("invalid type %s", motorType));
		}
	
		// TODO - support steppers too
		// put array of pins into more intelligent container
		// check "typical" 2 pin variety motors
		if ((motorType.equals(Motor.TYPE_2_PWM) || motorType.equals(Motor.TYPE_SIMPLE) || motorType.equals(Motor.TYPE_PULSE_STEP))  && pins.length != 2){
			throw new MRLException("motor type {} requires exactly 2 pins", motorType);
		}			
			
		if (motorType.equals(Motor.TYPE_SIMPLE) || motorType.equals(Motor.TYPE_PULSE_STEP)) {
			pinMap.put(PIN_TYPE_PWM, pins[0]);
			pinMap.put(PIN_TYPE_DIR, pins[1]);
		} else if (motorType.equals(Motor.TYPE_2_PWM)) {
			pinMap.put(PIN_TYPE_PWM_LEFT, pins[0]);
			pinMap.put(PIN_TYPE_PWM_RIGHT, pins[1]);
		} else {
			throw new MRLException(String.format("motor type %s currently not supported", motorType));
		}

		// finally the call to the controller
		// with a fully loaded and verified motor
		controller.motorAttach(this);
		broadcastState();
	}
	
	public Integer getPin(String name){
		return pinMap.get(name);
	}

	@Override
	public void moveTo(int newPos) {
		if (controller == null) {
			error(String.format("%s's controller is not set", getName()));
			return;
		}

		// targetPos = encoderMap.calc(newPos);
		targetPos = newPos;
		controller.motorMoveTo(this);
	}

	// FIXME - DEPRECATE !!!
	@Override
	public boolean setController(MotorController controller) {
		this.controller = controller;
		this.controllerName = controller.getName();
		return true;
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
		powerOutput = powerMap.calc(powerLevel);
	}

	@Override
	public void stop() {
		//move(0.0);
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

	public void setType(String type) throws MRLException {
		if (!types.contains(type)){
			throw new MRLException("%s not valid", type);
		}
		this.type = type;
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
	
		
	public Integer updatePosition(Integer position){
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
		if (type != null && type.equals(TYPE_PULSE_STEP)){
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
		if (type.equals(TYPE_PULSE_STEP)){
			// pulse step only needs the pwm pin
			return new int[]{pinMap.get(PIN_TYPE_PWM)};
		}
		return new int[]{};
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
		for (String pin: pinMap.keySet()){
			Integer x = pinMap.get(pin);
			log.info(String.format("[%d] = %s (%d)", index, pin, x));
			ret[index] = x;
			++index;
		}
		return ret;
	}

	@Override
	public String[] getTypes() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			String port = "COM15";

			//Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			// Runtime.createAndStart("gui", "GUIService");
			//arduino.setBoard(Arduino.BOARD_TYPE_ATMEGA2560);
			//arduino.connect(port);
			// arduino.broadcastState();

			// Runtime.createAndStart("python", "Python");

			int pwmPin= 13;
			int dirPin = 8;
			
			//int encoderPin= 7;
			Arduino arduino = (Arduino)Runtime.start("arduino", "Arduino");
			arduino.connect(port);
			Motor m1 = (Motor)Runtime.start("m1", "Motor");
			Runtime.start("webgui", "WebGui");
			m1.attach(arduino, Motor.TYPE_PULSE_STEP, pwmPin, dirPin);//(port, Motor.TYPE_SIMPLE, pwmPin, dirPin);
			
			m1.moveTo(250);
			m1.moveTo(700);
			m1.moveTo(250);
			m1.moveTo(250);
	
			arduino.setLoadTimingEnabled(true);
			arduino.setLoadTimingEnabled(false);
			m1.stop();
			m1.moveTo(200);
			m1.stop();

			//Runtime.start("webgui", "WebGui");

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

		/*
		 * 
		 * leftHand.moveTo(thumb, index, majeure, ringFinger, pinky, wrist);
		 * 
		 * moveHand("left", 61, 49, 14, 38, 15, 64);
		 * 
		 * m1.move(1.0); m1.move(0.5f); m1.move(0.0); m1.move(-0.5f);
		 * 
		 * 
		 * m1.stopAndLock();
		 * 
		 * m1.move(0.5f);
		 * 
		 * m1.unlock(); m1.stop();
		 */

	}
	



}
