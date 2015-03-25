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

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.MotorEncoder;
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
public class Motor extends Service implements MotorControl {

	/**
	 * FIXME - REMOVE - do low level counting and encoder triggers on the
	 * micro-controller !!!
	 */
	class EncoderTimer extends Thread {
		public double power = 0.0;
		public double duration = 0;

		Motor instance = null;

		EncoderTimer(double power, double duration, Motor instance) {
			super(instance.getName() + "_duration");
			this.power = power;
			this.duration = duration;
			this.instance = instance;
		}

		@Override
		public void run() {
			while (isRunning()) // this is from Service - is OK?
			{
				synchronized (lock) {
					try {
						lock.wait();

						instance.move(this.power);
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

	/**
	 * TYPE_PWM_DIR is the type of motor controller which has a digital
	 * direction pin. If this pin is high the motor travels in one direction if
	 * the pin is low the motor travels in the reverse direction
	 * 
	 */
	public final static String TYPE_PWM_DIR = "TYPE_PWM_DIR";

	/**
	 * TYPE_LPWM_RPWM - is a motor controller which one pin is pulsed to go one
	 * way and the other pin is pulsed to go the other way .. both low or both
	 * high invalid? Does one pin need to be held high or low while the other is
	 * pulsed.
	 */
	public final static String TYPE_LPWM_RPWM = "TYPE_LPWM_RPWM";
	// control
	/**
	 * FIXME - move motor - pin driver logic into Motor (it is common across all
	 * micro-controllers) for the 2 types of motor controller
	 * 
	 * Make "named" Peer versus direct controller reference
	 */
	transient private MotorController controller = null;
	boolean locked = false;
	private boolean isAttached = false;
	public Integer pwmPin;
	public Integer dirPin;
	public Integer pwmLeft;
	public Integer pwmRight;

	public Integer encoderPin;
	// power
	public double powerOutput = 0;

	Mapper powerMap = new Mapper(-1.0, 1.0, -255.0, 255.0);
	// position
	public double currentPos = 0.0;
	public double targetPos = 0.0;
	Mapper encoderMap = new Mapper(-800.0, 800.0, -800.0, 800.0);
	transient MotorEncoder encoder = null;

	// FIXME - REMOVE !!! DEPRECATE - just a "type" of encoder
	transient EncoderTimer durationThread = null;

	public String type = TYPE_PWM_DIR;

	// FIXME - implements an Encoder interface
	// get a named instance - stopping and starting should not be creating &
	// destroying
	transient Object lock = new Object();

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			String port = "COM15";

			Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			Runtime.createAndStart("gui", "GUIService");
			arduino.setBoard(Arduino.BOARD_TYPE_ATMEGA2560);
			arduino.connect(port);
			arduino.broadcastState();

			for (int i = 0; i < 100; ++i) {

			}
			// Runtime.createAndStart("python", "Python");

			Motor m1 = (Motor) Runtime.createAndStart("m1", "Motor");
			// arduino.motorAttach("m1", 8, 7, 54);
			arduino.motorAttach("m1", 7, 6);
			arduino.setSampleRate(8000);
			m1.setSpeed(0.95);

			// with encoder
			// m1.moveTo(600);

			m1.stop();
			m1.move(0.94);
			m1.stop();
			m1.move(-0.94);
			m1.stop();

			// arduino.motorAttach("m1", 8, 7, 54) ;

			m1.moveTo(600f);
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

	public Motor(String n) {
		super(n);
	}

	private void attached(boolean isAttached) {
		this.isAttached = isAttached;
		broadcastState();
	}

	@Override
	public boolean detach() {
		return controller.motorDetach(getName());
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
		return powerOutput;
	}

	public Mapper getPowerMap() {
		return powerMap;
	}

	@Override
	public boolean isAttached() {
		return isAttached;
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
		if (locked) {
			log.warn("motor locked");
			return;
		}
		powerOutput = powerMap.calc(power);
		controller.motorMove(getName());
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
				durationThread = new EncoderTimer(power, duration, this);
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

	@Override
	public void moveTo(double newPos) {
		if (controller == null) {
			error(String.format("%s's controller is not set", getName()));
			return;
		}

		targetPos = encoderMap.calc(newPos);
		controller.motorMoveTo(getName(), targetPos);
	}

	public double publishChangePos(Double newValue) {
		return newValue;
	}

	@Override
	public boolean setController(MotorController controller) {
		this.controller = controller;
		attached(true);
		return true;
	}

	public double setCurrentPos(double value) {
		if (currentPos != value) {
			currentPos = value;
			invoke("publishChangePos", value);
		}
		return value;
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

	public void setSpeed(double power) {
		powerOutput = powerMap.calc(power);
	}

	@Override
	public void stop() {
		move(0.0);
	}

	@Override
	public void stopAndLock() {
		log.info("stopAndLock");
		move(0.0);
		lock();
	}

	@Override
	public Status test() {
		Status status = super.test();
		try {

		} catch (Exception e) {
			status.addError(e);
			Logging.logError(e);
		}
		return status;
	}

	@Override
	public void unlock() {
		log.info("unLock");
		locked = false;
	}

}
