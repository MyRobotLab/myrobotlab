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

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.NameProvider;
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

		/*
		 * int min; int max; int delay; // depending on type - this is 2
		 * different things COMPUTER // its ms delay - CONTROLLER its modulus
		 * loop count int step; boolean sweepOneWay;
		 */

		public Sweeper(String name) {
			super(String.format("%s.sweeper", name));
		}

		@Override
		public void run() {

			if (targetPos == null) {
				targetPos = sweepMin;
			}

			try {
				while (isSweeping) {
					// increment position that we should go to.
					if (targetPos < sweepMax && sweepStep >= 0) {
						targetPos += sweepStep;
					} else if (targetPos > sweepMin && sweepStep < 0) {
						targetPos += sweepStep;
					}

					// switch directions or exit if we are sweeping 1 way
					if ((targetPos <= sweepMin && sweepStep < 0) || (targetPos >= sweepMax && sweepStep > 0)) {
						if (sweepOneWay) {
							isSweeping = false;
							break;
						}
						sweepStep = sweepStep * -1;
					}
					moveTo(targetPos.intValue());
					Thread.sleep(sweepDelay);
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

	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Servo.class);

	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(Servo.class.getCanonicalName());
		meta.addDescription("Controls a servo");
		meta.addCategory("motor", "control");

		return meta;
	}

	transient ServoController controller;

	String controllerName = null;

	private Mapper mapper = new Mapper(0, 180, 0, 180);

	private Integer rest = 90;

	private long lastActivityTime = 0;

	private Integer pin;

	/**
	 * the requested INPUT position of the servo
	 */
	Integer targetPos;

	/**
	 * Pin is "gone" - it only is needed at the time of attaching the device -
	 * Arduino and/or MRLComm will remember the pin for future reference.. its
	 * not a property of the "Servo"
	 * 
	 * the pin is a necessary part of servo - even though this is really
	 * controller's information a pin is a integral part of a "servo" - so it is
	 * beneficial to store it allowing a re-attach during runtime
	 * 
	 * FIXME - not true - attach on the controller puts in the data - it should
	 * leave it even on a detach - a attach with pin should only replace it -
	 * that way pin does not need to be stored on the Servo
	 */
	// private Integer pin;

	/**
	 * the calculated output for the servo
	 */
	Integer targetOutput;

	/**
	 * list of names of possible controllers
	 */
	ArrayList<String> controllers;
	/**
	 * current speed of the servo
	 */
	Double speed = 1.0;
	// FIXME - currently is only computer control - needs to be either
	// microcontroller or computer
	boolean isSweeping = false;
	int sweepMin = 0;
	int sweepMax = 180;
	int sweepDelay = 1;

	int sweepStep = 1;
	boolean sweepOneWay = false;

	// sweep types
	// TODO - computer implemented speed control (non-sweep)
	boolean speedControlOnUC = false;

	transient Thread sweeper = null;

	/**
	 * feedback of both incremental position and stops. would allow blocking
	 * moveTo if desired
	 */
	boolean isEventsEnabled = false;

	public Servo(String n) {
		super(n);
		lastActivityTime = System.currentTimeMillis();
	}

	public void addServoEventListener(NameProvider service) {
		addListener("publishServoEvent", service.getName(), "onServoEvent");
	}

	/**
	 * Re-attach to servo's current pin. The pin must have be set previously.
	 * Equivalent to Arduino's Servo.attach(currentPin)
	 */
	@Override
	public void attach() {
		attach(pin);
	}

	/**
	 * Equivalent to Arduino's Servo.attach(pin). It energizes the servo sending
	 * pulses to maintain its current position.
	 */
	@Override
	public void attach(int pin) {
		lastActivityTime = System.currentTimeMillis();
		getController().servoAttach(this, pin);
		this.pin = pin;
		broadcastState();
	}

	/**
	 * Equivalent to Arduino's Servo.detach() it de-energizes the servo
	 */
	@Override
	public void detach() {
		getController().servoDetach(this);
		broadcastState();
	}

	public boolean eventsEnabled(boolean b) {
		isEventsEnabled = b;
		getController().servoEventsEnabled(this, b);
		return b;
	}

	@Override
	public ServoController getController() {
		if (controller == null) {
			//error("%s's controller is null, perhaps you need to %s.attach(controller, pin, pos) ?", getName(), getName());
		}
		return controller;
	}

	public long getLastActivityTime() {
		return lastActivityTime;
	}

	public Double getMax() {
		return mapper.getMaxY();
	}

	public Double getMaxInput() {
		return mapper.getMaxX();
	}

	public Double getMaxOutput() {
		return mapper.getMaxOutput();
	}

	/*
	 * @Override public Integer getPin() {
	 * 
	 * return pin; }
	 * 
	 */

	public Double getMin() {
		return mapper.getMinY();
	}

	public Double getMinInput() {
		return mapper.getMinX();
	}

	public Double getMinOutput() {
		return mapper.getMinOutput();
	}

	public Integer getPos() {
		return targetPos;
	}

	public int getRest() {
		return rest;
	}

	public boolean isAttached() {
		return controller != null;
	}

	public boolean isInverted() {
		return mapper.isInverted();
	}

	// only if the sweep control is controled by computer and not arduino
	public boolean isSweeping() {
		return isSweeping;
	}

	public void map(double minX, double maxX, double minY, double maxY) {
		mapper = new Mapper(minX, maxX, minY, maxY);
		broadcastState();
	}

	public void moveTo(int pos) {

		if (controller == null) {
			error(String.format("%s's controller is not set", getName()));
			return;
		}

		targetPos = pos;
		targetOutput = mapper.calcInt(targetPos);

		getController().servoWrite(this);
		lastActivityTime = System.currentTimeMillis();

		// update the web gui that we've moved..
		// broadcastState();
		invoke("publishServoEvent", targetOutput);
	}

	private void controllerError() {
		error(String.format("%s's controller is not set - you probably need to servo.attach(controller, pin, pos)", getName()));
	}

	/**
	 * basic move command of the servo - usually is 0 - 180 valid range but can
	 * be adjusted and / or re-mapped with min / max and map commands
	 * 
	 * TODO - moveToBlocking - blocks until servo sends "ARRIVED_TO_POSITION"
	 * response
	 */

	// uber good
	public Integer publishServoEvent(Integer position) {
		return position;
	}

	/**
	 * FIXME - Hmmm good canidate for Microcontroller Peripheral
	 * 
	 * @return
	 */
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
	public void setController(DeviceController controller) {
		if (controller == null) {
			error("setting null as controller");
			return;
		}

		log.info(String.format("%s setController %s", getName(), controller.getName()));

		// FIXME - remove - the controller or MRLComm will take care of this
		// check
		/*
		 * if (isAttached()) { warn(
		 * "can not set controller %s when servo %s is attached", controller,
		 * getName()); return false; }
		 */

		this.controller = (ServoController) controller;
		broadcastState();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.interfaces.ServoControl#setPin(int)
	 */
	/*
	 * @Override public boolean setPin(int pin) { log.info(String.format(
	 * "setting %s pin to %d", getName(), pin)); if (isAttached()) { warn(
	 * "%s can not set pin %d when servo is attached", getName(), pin); return
	 * false; } this.pin = pin; broadcastState(); return true; }
	 */

	public void setInverted(boolean invert) {
		mapper.setInverted(invert);
	}

	@Override
	public void setMinMax(int min, int max) {
		mapper.setMin(min);
		mapper.setMax(max);
		broadcastState();
	}

	public void setRest(int rest) {
		this.rest = rest;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
		getController().servoSetSpeed(this);
	}

	// choose to handle sweep on arduino or in MRL on host computer thread.
	public void setSpeedControlOnUC(boolean b) {
		speedControlOnUC = b;
	}

	public void setSweepDelay(int delay) {
		sweepDelay = delay;
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
		getController().servoSweepStop(this);
		broadcastState();
	}

	public void sweep() {
		int min = mapper.getMinX().intValue();
		int max = mapper.getMaxX().intValue();
		sweep(min, max, 1, 1);
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

		this.sweepMin = min;
		this.sweepMax = max;
		this.sweepDelay = delay;
		this.sweepStep = step;
		this.sweepOneWay = oneWay;

		// FIXME - CONTROLLER TYPE SWITCH
		if (speedControlOnUC) {
			getController().servoSweepStart(this); // delay &
			// step
			// implemented
		} else {
			if (isSweeping) {
				stop();
			}

			sweeper = new Sweeper(getName());
			sweeper.start();
		}

		isSweeping = true;
		broadcastState();
	}

	/**
	 * Writes a value in microseconds (uS) to the servo, controlling the shaft
	 * accordingly. On a standard servo, this will set the angle of the shaft.
	 * On standard servos a parameter value of 1000 is fully counter-clockwise,
	 * 2000 is fully clockwise, and 1500 is in the middle.
	 * 
	 * Note that some manufactures do not follow this standard very closely so
	 * that servos often respond to values between 700 and 2300. Feel free to
	 * increase these endpoints until the servo no longer continues to increase
	 * its range. Note however that attempting to drive a servo past its
	 * endpoints (often indicated by a growling sound) is a high-current state,
	 * and should be avoided.
	 * 
	 * Continuous-rotation servos will respond to the writeMicrosecond function
	 * in an analogous manner to the write function.
	 * 
	 * @param pos
	 */
	public void writeMicroseconds(Integer uS) {
		log.info("writeMicroseconds({})", uS);
		getController().servoWriteMicroseconds(this, uS);
		lastActivityTime = System.currentTimeMillis();
		broadcastState();
	}

	/*
	 * @Override public void setPin(int pin) { this.pin = pin; }
	 */

	@Override
	public Integer getPin() {
		return pin;
	}

	public static void main(String[] args) throws InterruptedException {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		try {
			Runtime.start("webgui", "WebGui");
			Runtime.start("gui", "GUIService");
			Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			arduino.connect("COM5");
			Servo servo = (Servo) Runtime.start("servo", "Servo");
			servo.attach(arduino, 8);
			// servo.attach(arduino, 8);
			// servo.attach(

			servo.moveTo(90);
			servo.setRest(30);
			servo.moveTo(10);
			servo.moveTo(90);
			servo.moveTo(180);
			servo.rest();

			servo.setMinMax(30, 160);

			servo.moveTo(40);
			servo.moveTo(140);

			servo.moveTo(180);

			servo.setSpeed(0.5);
			servo.moveTo(31);
			servo.setSpeed(0.2);
			servo.moveTo(90);
			servo.moveTo(180);

			// servo.test();
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	@Override
	public int getSweepMin() {
		return sweepMin;
	}

	@Override
	public int getSweepMax() {
		return sweepMax;
	}

	@Override
	public int getSweepStep() {
		return sweepStep;
	}

	@Override
	public Integer getTargetOutput() {
		return targetOutput;
	}

	@Override
	public double getSpeed() {
		return speed;
	}

	@Override
	public void attach(ServoController controller, int pin) throws Exception {
		attach(controller, pin, null);
	}

	@Override
	public void attach(ServoController controller, int pin, Integer pos) throws Exception {
		
		if (this.controller == controller){
			log.info("already attached to controller - nothing to do");
			return;
		} else if (this.controller != null && this.controller != controller ){
			log.warn("already attached to controller %s - please detach before attaching to controller %s", this.controller.getName(), controller.getName());
			return;
		}
		
		
		// ORDER IS IMPORTANT !!!
		// attach the Control to the Controller first
		if (pos != null) {
			targetPos = pos;
			if (rest == null) {
				rest = pos;
			}
			controller.deviceAttach(this, pin, pos);
		} else {
			if (rest == null) {
				rest = 90;
			}
			controller.deviceAttach(this, pin);
		}
		
		// SET THE DATA 
		this.pin = pin;
		this.controller = controller;
	}

	@Override
	public void detach(ServoController controller) {
		if (this.controller == controller) {
			// detach the this device from the controller
			controller.deviceDetach(this);
			// remove the this controller's reference
			this.controller = null;
		}
	}

}