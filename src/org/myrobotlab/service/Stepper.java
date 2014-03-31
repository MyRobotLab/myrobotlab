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
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.StepperControl;
import org.myrobotlab.service.interfaces.StepperController;
import org.slf4j.Logger;

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
public class Stepper extends Service implements StepperControl {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Stepper.class.toString());

	private boolean isAttached = false;
	private int rpm = 0;
	private boolean locked = false; // for locking the motor in a stopped position
	
	// Constants that the user passes in to the motor calls
	public static final Integer FORWARD = 1;
	public static final Integer BACKWARD = 2;
	public static final Integer BRAKE = 3;
	public static final Integer RELEASE = 4;

	// Constants that the user passes in to the stepper calls
	public static final Integer SINGLE = 1;
	public static final Integer DOUBLE = 2;
	public static final Integer INTERLEAVE = 3;
	public static final Integer MICROSTEP = 4;
	
	private Integer stepperingStyle = SINGLE;
	
	private StepperController controller = null; // board name
	
	public Stepper(String n) {
		super(n);
	}

	@Override
	public void unlock() {
		log.info("unLock");
		locked = false;
	}

	@Override
	public void lock() {
		log.info("lock");
		locked = true;
	}

	@Override
	public void stopAndLock() {
		log.info("stopAndLock");
		lock();
	}

	@Override
	public boolean setController(StepperController controller) {
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

	@Override
	public boolean isAttached() {
		return isAttached;
	}

	@Override
	public boolean detach() {
		if (controller == null)
		{
			return false;
		}
		controller.stepperDetach(getName());
		return true;
	}

	@Override
	public void setSpeed(Integer rpm) {
		controller.setSpeed(rpm);
	}
	
	@Override
	public void step(Integer steps) {
		step(steps, stepperingStyle);
	}

	@Override
	public void step(Integer steps, Integer style) {
		stepperingStyle = style;
		controller.stepperStep(getName(), steps, style);
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Arduino arduino = (Arduino) Runtime.createAndStart("arduino", "Arduino");
		Runtime.createAndStart("python", "Python");
		// Runtime.createAndStart("adafruit", "AdafruitMotorShield");
		Stepper stepper = (Stepper)Runtime.createAndStart("stepper", "Stepper");
		//stepper.attach(arduino, 5, 6, 7, 8);
		Runtime.createAndStart("gui", "GUIService");

	}


}
