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
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.motor.MotorConfig;
import org.myrobotlab.motor.MotorConfigDualPwm;
import org.myrobotlab.motor.MotorConfigSimpleH;
import org.myrobotlab.sensor.Encoder;
import org.myrobotlab.sensor.EncoderListener;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.MotorEncoder;
import org.myrobotlab.service.interfaces.ServiceInterface;
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
public class Motor extends Service implements MotorControl, EncoderListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Motor.class);

	protected transient MotorController controller = null;
	/**
	 * list of names of possible controllers
	 */
	public List<String> controllers;

	String types[] = MotorConfig.getTypes();

	boolean locked = false;

	/**
	 * the power level requested - varies between -1.0 <--> 1.0
	 */

	double powerLevel = 0;
	double maxPower = 1.0;
	double minPower = -1.0;

	Mapper powerMap = new Mapper(-1.0, 1.0, -255.0, 255.0);

	// position
	double currentPos = 0;
	double targetPos = 0;

	Mapper encoderMap = new Mapper(-800.0, 800.0, -800.0, 800.0);

	transient MotorEncoder encoder = null;

	MotorConfig config;

	// FIXME - implements an Encoder interface
	// get a named instance - stopping and tarting should not be creating &
	// destroying
	transient Object lock = new Object();

	String controllerName;

	public Motor(String n) {
		super(n);
		subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
	}

	public void onRegistered(ServiceInterface s) {
		refreshControllers();
		broadcastState();
	}

	public List<String> refreshControllers() {
		controllers = Runtime.getServiceNamesFromInterface(MotorController.class);
		return controllers;
	}

	public DeviceController getController() {
		return controller;
	}

	@Override
	public double getPowerLevel() {
		return powerLevel;
	}

	@Override
	public double getPowerOutput() {
		return powerMap.calcOutput(powerLevel);
	}

	public Mapper getPowerMap() {
		return powerMap;
	}

	@Override
	public boolean isAttached(MotorController controller) {
		return this.controller == controller;
	}

	@Override
	public boolean isInverted() {
		return powerMap.isInverted();
	}

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
		log.debug("{}.move({})", getName(), power);
		if (power > maxPower) {
			power = maxPower;
		}
		if (power < minPower) {
			power = minPower;
		}
		powerLevel = power;

		if (locked) {
			log.warn("motor locked");
			return;
		}
		controller.motorMove(this);
		broadcastState();
	}

	@Override
	public void moveTo(double newPos, Double powerLevel) {
		this.powerLevel = powerLevel;
		if (controller == null) {
			error(String.format("%s's controller is not set", getName()));
			return;
		}

		// targetPos = encoderMap.calc(newPos);
		targetPos = newPos;
		controller.motorMoveTo(this);
		broadcastState();
	}

	@Override
	public void moveTo(double newPos) {
		moveTo(newPos, null);
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
		log.info("{}.stop()", getName());
		powerLevel = 0.0;
		controller.motorStop(this);
		broadcastState();
	}

	@Override
	public void stopAndLock() {
		log.info("stopAndLock");
		move(0.0);
		lock();
		broadcastState();
	}

	@Override
	public void unlock() {
		log.info("unLock");
		locked = false;
		broadcastState();
	}

	// FIXME - related to update(SensorData) no ?
	public Integer updatePosition(Integer position) {
		currentPos = position;
		return position;
	}

	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(Motor.class.getCanonicalName());
		meta.addDescription("General Motor Service");
		meta.addCategory("motor");

		return meta;
	}

	@Override
	public double getTargetPos() {
		return targetPos;
	}

	@Override
	public void pulse() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setEncoder(Encoder encoder) {
		// TODO Auto-generated method stub

	}

	public void detach(MotorController controller) {
		controller.detach(this);
		controller = null;
		controllerName = null;
		broadcastState();
	}

	@Override
	public void attach(MotorController controller) throws Exception {
		this.controller = controller;
		if (controller != null) {
			controllerName = controller.getName();
		}
		broadcastState();
	}

	/////// config start ////////////////////////
	public void setPwmPins(int leftPin, int rightPin) {
		config = new MotorConfigDualPwm(leftPin, rightPin);
		broadcastState();
	}

	public void setPwrDirPins(int pwrPin, int dirPin) {
		config = new MotorConfigSimpleH(pwrPin, dirPin);
		broadcastState();
	}

	@Override
	public MotorConfig getConfig() {
		return config;
	}

	@Override
	public boolean isAttached() {
		return controller != null;
	}

	// TODO - this could be Java 8 default interface implementation
	@Override
	public void detach(String controllerName) {
		if (controller == null || !controllerName.equals(controller.getName())) {
			return;
		}
		controller.detach(this);
		controller = null;
	}

	@Override
	public boolean isAttached(String name) {
		return (controller != null && controller.getName().equals(name));
	}

	@Override
	public Set<String> getAttached() {
		HashSet<String> ret = new HashSet<String>();
		if (controller != null) {
			ret.add(controller.getName());
		}
		return ret;
	}

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			Runtime.start("gui", "SwingGui");
			Runtime.start("webgui", "WebGui");
			Runtime.start("motor", "Motor");
			Runtime.start("arduino", "Arduino");
			boolean done = true;
			if (done) {
				return;
			}

			// FIXME - all testing or replacing of main code should be new JUnit
			// tests - with virtual arduino !!!)
			String port = "COM15";

			// Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			// Runtime.createAndStart("gui", "SwingGui");
			// Runtime.createAndStart("webgui", "WebGui");
			// arduino.setBoard(Arduino.BOARD_TYPE_ATMEGA2560);
			// arduino.connect(port);
			// arduino.broadcastState();

			// Runtime.createAndStart("python", "Python");

			int pwmPin = 6;
			int dirPin = 7;

			// int leftPwm = 6;
			// int rightPwm = 7;

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

			/*
			 * m1.setType2Pwm(leftPwm, rightPwm); m1.setTypeStepper();
			 * m1.setTypePulseStep(pwmPin, dirPin);
			 */
			// Runtime.start("webgui", "WebGui");
			// m1.attach(arduino, Motor.TYPE_PULSE_STEP, pwmPin, dirPin);
			// m1.attach(arduino, Motor.TYPE_2_PWM, pwmPin, dirPin);
			// m1.attach(arduino, Motor.TYPE_SIMPLE, pwmPin, dirPin);
			m1.attach((MotorController) arduino);

			m1.move(1.0);
			m1.move(-1.0);

			// TODO - overload with speed?
			m1.moveTo(250);
			m1.moveTo(700);
			m1.moveTo(250);
			m1.moveTo(250);

			arduino.enableBoardInfo(true);
			arduino.enableBoardInfo(false);
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

}