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

import org.myrobotlab.logging.Level;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;

/**
 * @author GroG
 * 
 *         represents a common continuous direct current electric motor. The
 *         motor will need a MotorController which can be one of many different
 *         types of electronics. A simple H-bridge typically has 2 bits which
 *         controll the motor. A direction bit which changes the polarity of the
 *         H-bridges output
 * 
 */
public class Motor extends Service implements MotorControl {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Motor.class.toString());
	
	
	public final static String TYPE_2WIRE_HBRIDGE = "TYPE_2WIRE_HBRIDGE";
	public final static String TYPE_3WIRE_HBRIDGE = "TYPE_3WIRE_HBRIDGE";

	/**
	 * state of Motor being attached to a motor controller
	 */
	private boolean isAttached = false;

	/**
	 * determines if the motor should spin CW or CCW relative to the positive or
	 * negative signed power
	 */
	private boolean directionInverted = false;

	/**
	 * current power level of the motor - valid range between -1.0 and 1.0
	 */
	private float powerLevel = 0;

	/**
	 * limit on power level - valid range is between 0 and 1.0
	 */
	private float maxPower = 1;

	public boolean inMotion = false;

	boolean locked = false; // for locking the motor in a stopped position
	private MotorController controller = null; // board name

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.X#getPowerLevel()
	 */
	@Override
	public float getPowerLevel() {
		return powerLevel;
	}

	public void invertDirection(boolean invert) {
		this.directionInverted = invert;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.X#isDirectionInverted()
	 */
	@Override
	public boolean isDirectionInverted() {
		return directionInverted;
	}

	transient EncoderTimer durationThread = null;

	/**
	 * Motor constructor takes a single unique name for identification. e.g.
	 * Motor left = new Motor("left");
	 * 
	 * @param name
	 */
	public Motor(String n) {
		super(n);
	}



	// --------- Motor (front end) API Begin ----------------------------
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.X#move(float)
	 */
	@Override
	public void move(Float newPowerLevel) {
		// check for locked or invalid level
		if (locked) {
			log.warn("motor locked");
			return;
		}
		
		if (Math.abs(newPowerLevel) > maxPower) {
			error(String.format("invalid power level %d", newPowerLevel));
			return;
		}

		// set the new power level
		powerLevel = newPowerLevel;
		// request controller to move
		controller.motorMove(getName());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.X#moveTo(java.lang.Integer)
	 */
	@Override
	public void moveTo(Integer newPos) {
		// FIXME - implement - needs encoder
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.X#setMaxPower(float)
	 */
	@Override
	public void setMaxPower(float max) {
		if (maxPower > 1 || maxPower < 0) {
			error("max power must be between 0.0 and 0.1");
			return;
		}
		maxPower = max;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.X#stop()
	 */
	@Override
	public void stop() {
		move(0.0f);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.X#unLock()
	 */
	@Override
	public void unlock() {
		log.info("unLock");
		locked = false;
	}

	public void lock() {
		log.info("lock");
		locked = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.X#stopAndLock()
	 */
	@Override
	public void stopAndLock() {
		log.info("stopAndLock");
		move(0.0f);
		lock();
	}

	// --------- Motor (front end) API End ----------------------------

	// FIXME - implements an Encoder interface
	// get a named instance - stopping and starting should not be creating &
	// destroying
	transient Object lock = new Object();

	class EncoderTimer extends Thread {
		public float power = 0.0f;
		public float duration = 0;

		Motor instance = null;

		EncoderTimer(float power, float duration, Motor instance) {
			super(instance.getName() + "_duration");
			this.power = power;
			this.duration = duration;
			this.instance = instance;
		}

		public void run() {
			while (isRunning()) // this is from Service - is OK?
			{
				synchronized (lock) {
					try {
						lock.wait();

						instance.move(this.power);
						inMotion = true;

						Thread.sleep((int) (this.duration * 1000));

						instance.stop();
						inMotion = false;

					} catch (InterruptedException e) {
						log.warn("duration thread interrupted");
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.X#moveFor(float, int)
	 */
	@Override
	public void moveFor(Float power, Float duration) {
		// default is not to block
		moveFor(power, duration, false);
	}

	// TODO - operate from thread pool
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.X#moveFor(float, int, boolean)
	 */
	@Override
	public void moveFor(Float power, Float duration, Boolean block) {
		// TODO - remove - Timer which implements SensorFeedback should be used
		if (!block) {
			// non-blocking call to move for a duration
			if (durationThread == null) {
				durationThread = new EncoderTimer(power, duration, this);
				durationThread.start();
			} else {
				if (inMotion) {
					error("duration is busy with another move" + durationThread.duration);
				} else {
					synchronized (lock) {
						durationThread.power = power;
						durationThread.duration = duration;
						lock.notifyAll();
					}
				}
			}
		} else {
			// block the calling thread
			move(this.powerLevel);
			inMotion = true;

			try {
				Thread.sleep((int) (duration * 1000));
			} catch (InterruptedException e) {
				logException(e);
			}

			stop();
			inMotion = false;

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.X#attach(org.myrobotlab.service.interfaces.
	 * MotorController)
	 */
	@Override
	public boolean setController(MotorController controller) {
		this.controller = controller;
		attached(true);
		return true;
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

	private void attached(boolean isAttached) {
		this.isAttached = isAttached;
		broadcastState();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.X#isAttached()
	 */
	@Override
	public boolean isAttached() {
		return isAttached;
	}

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		String port = "COM12";
		Arduino arduino = (Arduino)Runtime.createAndStart("arduino", "Arduino");
		arduino.connect(port);

		Motor m1 = (Motor)Runtime.createAndStart("m1","Motor");
		arduino.motorAttach("m1", 3, 4) ;

		m1.move(1.0f);
		m1.move(0.5f);
		m1.move(0.0f);
		m1.move(-0.5f);
		

		m1.stopAndLock();

		m1.move(0.5f);

		m1.unlock();
		m1.stop();


	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.X#detach()
	 */
	@Override
	public boolean detach() {
		return controller.motorDetach(getName());
	}
}
