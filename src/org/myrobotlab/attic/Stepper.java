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

package org.myrobotlab.attic;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

// TODO - implements Motor interface
// This mimics a DPDT Motor
public class Stepper extends Service {
	/*
	 * TODO - there are only 2 ways to control a simple DC motor - SMOKE and
	 * H-Bridge/DPDT Where DPDT - one digital line is off/on - the other is CW
	 * CCW
	 * 
	 * Pwr Dir D0 D1 0 0 STOP (CCW) 0 1 STOP (CW) 1 0 GO (CCW) 1 1 GO (CW)
	 * 
	 * POWER - PWM is controlled only on the Pwr Line only - 1 PWM line
	 * 
	 * The other is 1 digital line each for each direction and power (SMOKE) if
	 * both are high
	 * 
	 * Pwr Pwr D0 D1 0 0 STOP 0 1 CW 1 0 CCW 1 1 SHORT & BURN - !!!! NA !!!
	 * 
	 * POWER - PWM must be put on both lines - 2 PWM lines
	 */

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Stepper.class.toString());

	boolean isAttached = false;

	int PWRPin;
	int DIRPin;
	int powerMultiplier = 255; // default to Arduino analogWrite max

	int FORWARD = 1;
	int BACKWARD = 0;

	float power = 0;
	float maxPower = 1;

	boolean locked = false; // for locking the motor in a stopped position
	String controllerName = null; // board name

	public Stepper(String n) {
		super(n);
	}

	public void attach(String controllerName, int PWRPin, int DIRPin) {
		this.controllerName = controllerName;
		this.PWRPin = PWRPin;
		this.DIRPin = DIRPin;
		// WRONG ! send(controllerName, "motorAttach", this.getName(), PWRPin,
		// DIRPin);
	}

	public void invertDirection() {
		FORWARD = 0;
		BACKWARD = 1;
	}

	public void incrementPower(float increment) {
		if (power + increment > maxPower || power + increment < -maxPower) {
			log.error("power " + power + " out of bounds with increment " + increment);
			return;
		}
		power += increment;
		move(power);
	}

	// motor primitives begin ------------------------------------
	public void move(float power) {
		if (locked)
			return;

		// check if the direction has changed - send command if necessary
		if (power == 0) {
			send(controllerName, "analogWrite", PWRPin, 0);
		} else if (power > 0 && this.power <= 0) {
			send(controllerName, "digitalWrite", DIRPin, FORWARD);
		} else if (power < 0) {
			send(controllerName, "digitalWrite", DIRPin, BACKWARD);
		}

		// log.error("direction " + ((power > 0) ? "FORWARD" : "BACKWARD"));
		log.error(getName() + " power " + (int) (power * 100) + "% actual " + (int) (power * powerMultiplier));
		send(controllerName, "analogWrite", PWRPin, Math.abs((int) (power * powerMultiplier)));

		this.power = power;

	}

	public void stop() {
		move(0);
	}

	public void unLock() {
		log.info("unLock");
		locked = false;
	}

	public void lock() {
		log.info("lock");
		locked = true;
	}

	public void stopAndLock() {
		log.info("stopAndLock");
		move(0);
		lock();
	}

	public void setMaxPower(float max) {
		maxPower = max;
	}

	@Override
	public String getDescription() {
		return "<html>stepper motor service (not implemented)</html>";
	}


}

/*
 * TODO - implement in Arduino
 * 
 * int targetPosition = 0; boolean movingToPosition = false;
 * 
 * 
 * public void attachEncoder(String encoderName, int pin) // TODO Encoder
 * Interface { this.encoderName = encoderName; PinData pd = new PinData();
 * pd.pin = pin; encoderPin = pin; // TODO - have encoder own pin - send name
 * for event
 * 
 * // TODO - Make Encoder Interface
 * 
 * MRLListener MRLListener = new MRLListener();
 * 
 * MRLListener.getName() = name; MRLListener.outMethod = "publishPin";
 * MRLListener.inMethod = "incrementPosition"; MRLListener.paramType =
 * PinData.class.getCanonicalName(); send(encoderName, "addListener",
 * MRLListener);
 * 
 * }
 * 
 * 
 * public int getPosition() { return position; }
 * 
 * // feedback // public static final String FEEDBACK_TIMER = "FEEDBACK_TIMER";
 * enum FeedbackType { Timer, Digital }
 * 
 * Timer timer = new Timer(); FeedbackType poistionFeedbackType =
 * FeedbackType.Timer;
 * 
 * enum BlockingMode { Blocking, Staggered, Overlap }
 * 
 * BlockingMode blockingMode = BlockingMode.Blocking;
 * 
 * 
 * TODO - motors should not have any idea as to what their "pins" are - this
 * should be maintained by the controller
 * 
 * 
 * String encoderName = null; // feedback device int encoderPin = 0; // TODO -
 * put in Encoder class
 * 
 * 
 * public int incrementPosition(PinData p) { if (p.pin != encoderPin) // TODO
 * TODO TODO - this is wrong - should be // filtering on the Arduino publish
 * !!!! return 0;
 * 
 * if (direction == FORWARD) { position += 1; if (movingToPosition && position
 * >= targetPosition) { stopMotor(); movingToPosition = false; }
 * 
 * } else { position -= 1; if (movingToPosition && position <= targetPosition) {
 * stopMotor(); movingToPosition = false; } }
 * 
 * return position;
 * 
 * }
 * 
 * 
 * // move to relative amount - needs position feedback public void move(int
 * amount) // TODO - "amount" should be moveTo { // setPower(lastPower); if
 * (amount == 0) { return; } else if (direction == FORWARD) { direction =
 * FORWARD; position += amount; } else if (direction == BACKWARD) { direction =
 * BACKWARD; position -= amount; }
 * 
 * move(); amount = Math.abs(amount); if (poistionFeedbackType ==
 * FeedbackType.Timer && blockingMode == BlockingMode.Blocking) { try {
 * Thread.sleep(amount * positionMultiplier); } catch (InterruptedException e) {
 * e.printStackTrace(); } // TODO - this is overlapp mode (least useful) //
 * timer.schedule(new FeedbackTask(FeedbackTask.stopMotor, amount * //
 * positionMultiplier), amount * positionMultiplier); }
 * 
 * stopMotor(); }
 * 
 * // TODO - enums pinMode & OUTPUT // TODO - abstract "controller"
 * Controller.OUTPUT
 * 
 * send(controllerName, "pinMode", PWRPin, Arduino.OUTPUT); // TODO THIS IS
 * NOT!!! A FUNCTION OF THE MOTOR - THIS NEEDS TO BE TAKEN CARE OF BY THE BOARD
 * send(controllerName, "pinMode", DIRPin, Arduino.OUTPUT);
 * 
 * public void move(int direction, float power, int amount) { setDir(direction);
 * setPower(power); move(amount); }
 * 
 * 
 * public void setDir(int direction) { if (locked) return;
 * 
 * this.direction = direction; }
 * 
 * public void move(int direction, float power) { if (locked) return;
 * 
 * setDir(direction); setPower(power); move(); }
 * 
 * public void moveTo(Integer newPos) { targetPosition = newPos;
 * movingToPosition = true; if (position - newPos < 0) { setDir(FORWARD); //
 * move(Math.abs(position - newPos)); setPower(0.5f); move(); } else if
 * (position - newPos > 0) { setDir(BACKWARD); // move(Math.abs(position -
 * newPos)); setPower(0.5f); move(); } else return; }
 * 
 * public void moveCW() { setDir(FORWARD); move(); }
 * 
 * public void moveCCW() { setDir(BACKWARD); move(); }
 * 
 * public void setPower(float power) { if (locked) return;
 * 
 * if (power > maxPower || power < -maxPower) { log.error(power +
 * " power out of bounds - max power is "+ maxPower); return; }
 * 
 * this.power = power; move(power); }
 * 
 * int positionMultiplier = 1000; boolean useRamping = false; public void
 * setUseRamping(boolean ramping) { useRamping = ramping; }
 * 
 * // motor primitives end ------------------------------------ /* Power and
 * Direction parameters work on the principle that they are values of a motor,
 * but are not operated upon until a "move" command is issued. "Move" will
 * direct the motor to move to use the targeted power and direction.
 * 
 * All of the following functions use primitives and are basically composite
 * functions
 */

