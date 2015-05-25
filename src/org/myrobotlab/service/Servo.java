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

import java.util.ArrayList;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         Servos have both input and output. Input is usually of the range of
 *         integers between 0 - 180, and output can relay those values directly
 *         to the servo's firmware (Arduino ServoLib, I2C controller, etc)
 * 
 *         However there can be the occasion that the input comes from a system
 *         which does not have the same range. Such that input can vary from 0.0
 *         to 1.0. For example, OpenCV coordinates are often returned in this
 *         range. When a mapping is needed Servo.map can be used. For this
 *         mapping Servo.map(0.0, 1.0, 0, 180) might be desired. Reversing input
 *         would be done with Servo.map(180, 0, 0, 180)
 * 
 *         outputY - is the values sent to the firmware, and should not
 *         necessarily be confused with the inputX which is the input values
 *         sent to the servo
 * 
 */

public class Servo extends Service implements ServoControl {

	/**
	 * Sweeper - TODO - should be implemented in the arduino code for smoother
	 * function
	 * 
	 */
	public class Sweeper extends Thread {

		int min;
		int max;
		int delay; // depending on type - this is 2 different things COMPUTER
		// its ms delay - CONTROLLER its modulus loop count
		int step;
		boolean oneWay;

		public Sweeper(String name, int min, int max, int delay, int step) {
			super(String.format("%s.sweeper", name));
			this.min = min;
			this.max = max;
			this.delay = delay;
			this.step = step;
			this.oneWay = false;
		}

		public Sweeper(String name, int min, int max, int delay, int step, boolean oneWay) {
			super(String.format("%s.sweeper", name));
			this.min = min;
			this.max = max;
			this.delay = delay;
			this.step = step;
			this.oneWay = oneWay;
		}

		// for non-controller based sweeping,
		// this is the delay for the sweeper thread.
		public int getDelay() {
			return delay;
		}

		@Override
		public void run() {

			if (inputX == null) {
				inputX = (float) min;
			}

			isSweeping = true;

			try {
				while (isSweeping) {
					// increment position that we should go to.
					if (inputX < max && step >= 0) {
						inputX += step;
					} else if (inputX > min && step < 0) {
						inputX += step;
					}

					// switch directions or exit if we are sweeping 1 way
					if ((inputX <= min && step < 0) || (inputX >= max && step > 0)) {
						if (oneWay) {
							isSweeping = false;
							break;
						}
						step = step * -1;
					}
					moveTo(inputX.intValue());
					Thread.sleep(delay);
				}

			} catch (Exception e) {
				isSweeping = false;
				if (e instanceof InterruptedException) {
					info("shutting down sweeper");
				} else {
					logException(e);
				}
			}
		}

		public void setDelay(int delay) {
			this.delay = delay;
		}
	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Servo.class);

	transient ServoController controller;

	// clipping

	private Float inputX;

	private float outputYMin = 0;

	// range mapping

	private float outputYMax = 180;

	private float minX = 0;

	private float maxX = 180;

	private float minY = 0;

	private float maxY = 180;

	private int rest = 90;

	private long lastActivityTime = 0;
	/**
	 * the pin is a necessary part of servo - even though this is really
	 * controller's information a pin is a integral part of a "servo" - so it is
	 * beneficial to store it allowing a re-attach during runtime
	 * 
	 * FIXME - not true - attach on the controller puts in the data - it should
	 * leave it even on a detach - a attach with pin should only replace it -
	 * that way pin does not need to be stored on the Servo
	 */
	private Integer pin;
	ArrayList<String> controllers;

	// computer thread based sweeping
	boolean isSweeping = false;

	// sweep types
	// TODO - computer implemented speed control (non-sweep)
	boolean speedControlOnUC = false;
	transient Thread sweeper = null;
	
	/**
	 * isAttached represents if the 
	 * 
	 * usually isAttached would be determined by whether the controller != null
	 * however detach() is a meaningful method on the micro-controller side
	 * and many services want to be able to detach() and re-attach() without
	 * a reference to the controller - that is why this boolean variable must
	 * be kept in sync
	 */
	private boolean isAttached = false;

	private boolean inverted = false;

	public static void main(String[] args) throws InterruptedException {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		try {

			Servo servo = (Servo) Runtime.start("servo", "Servo");
			// servo.test();
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	public Servo(String n) {
		super(n);
		lastActivityTime = System.currentTimeMillis();
	}

	// uber good
	public void addServoEventListener(Service service) {
		addListener("publishServoEvent", service.getName(), "onServoEvent", Integer.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.interfaces.ServoControl#attach()
	 */
	@Override
	public boolean attach() {
		lastActivityTime = System.currentTimeMillis();
		if (isAttached) {
			log.info(String.format("%s.attach() - already attached - detach first", getName()));
			return false;
		}

		if (controller == null) {
			error("no valid controller can not attach %s", getName());
			return false;
		}

		isAttached = controller.servoAttach(getName(), pin);

		if (isAttached) {
			// changed state
			broadcastState();
		}

		return isAttached;
	}

	public boolean attach(ServoController controller, Integer pin) {
		setPin(pin);

		if (setController(controller)) {
			return attach();
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.myrobotlab.service.interfaces.ServoControl#attach(java.lang.String,
	 * int)
	 */
	@Override
	public boolean attach(String controller, Integer pin) {
		return attach((ServoController) Runtime.getService(controller), pin);
	}

	public int calc(float s) {
		return Math.round(minY + ((s - minX) * (maxY - minY)) / (maxX - minX));
	}

	@Override
	public boolean detach() {
		if (!isAttached) {
			log.info(String.format("%s.detach() - already detach - attach first", getName()));
			return false;
		}

		if (controller != null) {
			if (controller.servoDetach(getName())) {
				isAttached = false;
				// changed state
				broadcastState();
				return true;
			}
		}

		return false;
	}

	@Override
	public String[] getCategories() {
		return new String[] { "motor", "control" };
	}

	@Override
	public String getControllerName() {
		if (controller == null) {
			return null;
		}

		return controller.getName();
	}

	@Override
	public String getDescription() {
		return "basic servo service";
	}

	public long getLastActivityTime() {
		return lastActivityTime;
	}

	public Integer getMax() {
		return (int) outputYMax;
	}

	public float getMaxInput() {
		return maxX;
	}

	public Integer getMin() {
		return (int) outputYMin;
	}

	public float getMinInput() {
		return minX;
	}

	// FIXME - really this could be normalized...
	// attach / detach - the detach would not remove the ServoData from
	// the controller only another attach with a different pin would
	// change it
	@Override
	public Integer getPin() {
		/*
		 * valiant attempt of normalizing - but Servo needs to know its own pin
		 * to support attach()
		 * 
		 * if (controller == null) { return null; }
		 * 
		 * return controller.getServoPin(getName());
		 */
		return pin;
	}

	public int getPos() {
		return Math.round(inputX);
	}

	@Override
	public Float getPosFloat() {
		return inputX;
	}

	public int getRest() {
		return rest;
	}

	public boolean isAttached() {
		return isAttached;
	}

	public boolean isInverted() {
		return inverted;
	}

	// only if the sweep control is controled by computer and not arduino
	public boolean isSweeping() {
		return isSweeping;
	}

	public void map(float minX, float maxX, float minY, float maxY) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
		broadcastState();
	}

	public void map(int minX, int maxX, int minY, int maxY) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
		broadcastState();
	}

	// FIXME - bs from gson encoding :P - or should it be Double :P
	public void moveTo(Double pos) {
		moveTo(pos.floatValue());
	}

	public void moveTo(Float pos) {
		if (controller == null) {
			error(String.format("%s's controller is not set", getName()));
			return;
		}

		inputX = pos;

		// the magic mapping
		int outputY = calc(inputX);

		if (outputY > outputYMax || outputY < outputYMin) {
			warn(String.format("%s.moveTo(%d) out of range", getName(), outputY));
			return;
		}

		// FIXME - currently their is no timerPosition
		// this could be gotten with 100 * outputY for some valid range
		log.info("servoWrite({})", outputY);
		controller.servoWrite(getName(), outputY);
		lastActivityTime = System.currentTimeMillis();

	}

	/**
	 * basic move command of the servo - usually is 0 - 180 valid range but can
	 * be adjusted and / or re-mapped with min / max and map commands
	 * 
	 * TODO - moveToBlocking - blocks until servo sends "ARRIVED_TO_POSITION"
	 * response
	 */

	@Override
	public void moveTo(Integer pos) {
		moveTo((float) pos);
	}

	// uber good
	public Integer publishServoEvent(Integer position) {
		return position;
	}

	public ArrayList<String> refreshControllers() {
		controllers = Runtime.getServiceNamesFromInterface(ServoController.class);
		return controllers;
	}

	@Override
	public void releaseService() {
		detach();
		super.releaseService();
	}

	public void rest() {
		moveTo(rest);
	}

	@Override
	public boolean setController(ServoController controller) {
		if (controller == null) {
			error("setting null as controller");
			return false;
		}

		log.info(String.format("%s setController %s", getName(), controller.getName()));

		if (isAttached()) {
			warn("can not set controller %s when servo %s is attached", controller, getName());
			return false;
		}

		this.controller = controller;
		broadcastState();
		return true;
	}

	@Override
	public boolean setController(String controller) {

		ServoController sc = (ServoController) Runtime.getService(controller);
		if (sc == null) {
			return false;
		}

		return setController(sc);
	}

	public boolean setEventsEnabled(boolean b) {
		controller.setServoEventsEnabled(getName(), b);
		return b;
	}

	public void setInverted(boolean invert) {
		if (!inverted && invert) {
			map(maxX, minX, minY, maxY);
			inverted = true;
		} else {
			inverted = false;
		}

	}

	@Override
	public void setMinMax(int min, int max) {
		outputYMin = min;
		outputYMax = max;
		broadcastState();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.interfaces.ServoControl#setPin(int)
	 */
	@Override
	public boolean setPin(int pin) {
		log.info(String.format("setting %s pin to %d", getName(), pin));
		if (isAttached()) {
			warn("%s can not set pin %d when servo is attached", getName(), pin);
			return false;
		}
		this.pin = pin;
		broadcastState();
		return true;
	}

	public int setRest(int i) {
		rest = i;
		return rest;
	}

	@Override
	public void setSpeed(Float speed) {
		if (speed == null) {
			return;
		}

		if (controller == null) {
			error("setSpeed - controller not set");
			return;
		}
		controller.setServoSpeed(getName(), speed);
	}

	// choose to handle sweep on arduino or in MRL on host computer thread.
	public void setSpeedControlOnUC(boolean b) {
		speedControlOnUC = b;
	}

	public void setSweeperDelay(int delay) {
		((Sweeper) sweeper).setDelay(delay);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.interfaces.ServoControl#stopServo()
	 */
	@Override
	public void stop() {
		isSweeping = false;
		sweeper = null;
		controller.servoSweepStop(getName());
	}

	public void sweep() {
		sweep(Math.round(minX), Math.round(maxX), 1, 1);
	}

	public void sweep(int min, int max) {
		sweep(min, max, 1, 1);
	}

	// FIXME - is it really speed control - you don't currently thread for
	// factional speed values
	public void sweep(int min, int max, int delay, int step) {
		sweep(min, max, delay, step, false);
	}

	public void sweep(int min, int max, int delay, int step, boolean oneWay) {
		// CONTROLLER TYPE SWITCH
		if (speedControlOnUC) {
			controller.servoSweepStart(getName(), min, max, step); // delay &
																	// step
																	// implemented
		} else {
			if (isSweeping) {
				stop();
			}

			sweeper = new Sweeper(getName(), min, max, delay, step, oneWay);
			sweeper.start();
		}
	}

	public void writeMicroseconds(Integer pos) {
		if (controller == null) {
			error(String.format("%s's controller is not set", getName()));
			return;
		}

		inputX = pos.floatValue();

		// the magic mapping
		int outputY = calc(inputX);

		if (outputY > outputYMax || outputY < outputYMin) {
			warn(String.format("%s.moveTo(%d) out of range", getName(), outputY));
			return;
		}

		// FIXME - currently their is no timerPosition
		// this could be gotten with 100 * outputY for some valid range
		log.info("servoWrite({})", outputY);
		controller.servoWriteMicroseconds(getName(), outputY);
		lastActivityTime = System.currentTimeMillis();

		// trying out broadcast after
		// position change
		broadcastState();
	}

}