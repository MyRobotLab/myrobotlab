/**
 *                    
 * @author GroG
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

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.StepperController;

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
public class Stepper extends Service {
	

	public final static int STEPPER_EVENT_STOP = 1;
	public final static int STEPPER_EVENT_STEP = 2;
	
	// TODO - control publishing of events @ the controller source
	// bit flags of types of events to publish
	public static class StepperEvent {
		public StepperEvent(int eventType, int pos) {
			this.eventType = eventType;
			this.pos = pos;
		}
		public int pos;
		public int eventType;
	}

	private static final long serialVersionUID = 1L;

	private boolean isAttached = false;
	private boolean locked = false; // for locking the motor in a stopped

	// needs to be int for dumb controller
	public final static int STEPPER_TYPE_SIMPLE = 1;

	public final Integer STEP_STYLE_SINGLE = 1;
	public final Integer STEP_STYLE_DOUBLE = 2;
	public final Integer STEP_STYLE_INTERLEAVE = 3;
	public final Integer STEP_STYLE_MICROSTEP = 4;

	/**
	 * controller index for events & controllers to dumb to have dynamic string
	 * based containers
	 */
	private Integer index;

	private Integer style = STEP_STYLE_SINGLE;

	transient BlockingQueue<Object> blockingData = new LinkedBlockingQueue<Object>();

	/**
	 * number of steps for this stepper - common is 200
	 */
	private Integer steps;

	private int type = STEPPER_TYPE_SIMPLE;

	private int currentPos = 0;

	private StepperController controller = null;

	/**
	 * based on stepper type - pin configuration may vary
	 */
	int stepPin;
	int dirPin;

	// support up to 5 wire steppers
	int pin0;
	int pin1;
	int pin2;
	int pin3;
	int pin4;

	private boolean isBlockingOnStop = false;

	public Stepper(String n) {
		super(n);
	}

	// Uber good - .. although this is "chained" versus star routing
	// Star routing would be routing from the Arduino directly to the Listener
	// The "chained" version takes 2 thread contexts :( .. but it has the
	// benefit
	// of the "publishRange" method being affected by the Sensor service e.g.
	// change units, sample rate, etc
	// TODO - isLocal() - determine local callback or pub/sub
	public void addPublishStepperEventListener(Service service) {
		addListener("publishStepperEvent", service.getName(), "publishStepperEvent", Integer.class);
	}
	
	public StepperEvent publishStepperEvent(StepperEvent data) {
		return data;
	}

	public boolean attach(StepperController controller, int dirPin, int stepPin) throws IOException {
		this.type = STEPPER_TYPE_SIMPLE;
		this.controller = controller;
		this.dirPin = dirPin;
		this.stepPin = stepPin;
		isAttached = controller.stepperAttach(this);
		broadcastState();
		if (isAttached){
			info("attached stepper %s index %d to %s with dir pin %d step pin %d", getName(), index, controller.getName(), dirPin, stepPin);
		} else {
			error("could not attach stepper");
		}
		return isAttached;
	}

	public boolean detach() {
		if (controller == null) {
			return false;
		}
		controller.stepperDetach(getName());
		return true;
	}

	public String[] getCategories() {
		return new String[] { "motor", "control" };
	}

	public String getControllerName() {
		if (controller != null) {
			return controller.getName();
		}

		return null;
	}

	public String getDescription() {
		return "general motor service";
	}

	public Integer getIndex() {
		return index;
	}

	public int getStepperType() {
		return type;
	}

	public int getSteps() {
		return steps;
	}

	public boolean isAttached() {
		return isAttached;
	}

	public void lock() {
		log.info("lock");
		locked = true;
	}

	public void moveTo(int newPos) {
		if (!isBlockingOnStop) {
			controller.stepperMoveTo(getName(), newPos, style);
		} else {
			blockingData.clear();
			moveTo(newPos);
			try {
				// Integer gotTo = (Integer) blockingData.poll(10000,
				// TimeUnit.MILLISECONDS);
				currentPos = (Integer) blockingData.take();
			} catch (Exception e) { // don't care
			}
		}
	}

	/**
	 * sets move to block if desired - this will block current thread on a
	 * moveTo command until the position is reached
	 * 
	 * @param block
	 */
	public void setBlocking(boolean block) {
		isBlockingOnStop = block;
	}

	/*
	public Integer publishStepperEvent(Integer currentPos) {
		log.info(String.format("publishStepperEvent %s %d", getName(), currentPos));
		this.currentPos = currentPos;
		return currentPos;
	}
	
	public Integer publishStepperStop(Integer currentPos){
		log.info(String.format("publishStepperStop %s %d", getName(), currentPos));
		this.currentPos = currentPos;
		if (isBlockingOnStop) {
			blockingData.add(currentPos);
		}
		return currentPos;
	}
	*/
	

	
	public int getPos(){
		return currentPos;
	}

	public void reset() {
		stop();
		currentPos = 0;
		controller.stepperReset(getName());
	}

	public boolean setController(StepperController controller) {
		this.controller = controller;
		return true;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public void setNumSteps(Integer steps) {
		this.steps = steps;
	}

	public void setSpeed(Integer rpm) {
		controller.setStepperSpeed(rpm);
	}

	public void stop() {
		controller.stepperStop(getName());
	}

	public void stopAndLock() {
		log.info("stopAndLock");
		stop();
		lock();
	}

	public void unlock() {
		log.info("unLock");
		locked = false;
	}


	public int getDirPin() {
		return dirPin;
	}

	public int getStepPin() {
		return stepPin;
	}

	public int setPos(int currentPos) {
		this.currentPos = currentPos;
		return currentPos;
	}

	public Status test() {
		Status status = Status.info("starting %s %s test", getName(), getType());
		try {

			Runtime.start("gui", "GUIService");
			String vport = "COM15";

			Stepper stepper = (Stepper) Runtime.start(getName(), "Stepper");
			Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			arduino.connect(vport);

			int dirPin = 34;
			int stepPin = 38;

			stepper.attach(arduino, dirPin, stepPin);

			// stepper.moveToBlocking(77777);
			stepper.reset();
			
			stepper.moveTo(10);
			stepper.stop();
			stepper.moveTo(-10);
			stepper.reset();

			stepper.moveTo(100);

		} catch (Exception e) {
			Logging.logError(e);
		}
		return status;
	}


	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Stepper stepper = (Stepper) Runtime.start("stepper", "Stepper");
		stepper.test();

	}

}
