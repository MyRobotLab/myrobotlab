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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
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
	private boolean locked = false; // for locking the motor in a stopped
									// position

	/**
	 * controller index for events & controllers to dumb to have dynamic string
	 * based containers
	 */
	private Integer index;

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

	BlockingQueue<Object> blockingData = new LinkedBlockingQueue<Object>();

	/**
	 * number of steps for this stepper - common is 200
	 */
	private Integer steps;

	private String type;

	private Integer currentPos = 0;

	private StepperController controller = null; // board name

	static final public String STEPPER_TYPE_POLOLU = "STEPPER_TYPE_POLOLU";

	/**
	 * step pins this can vary in size 2 for Pololu (dir & step) , 3, 4, 5, ...
	 * if more than one - they must be in the correct stepping order
	 */
	private Integer[] pins; // this data is "shared" with the controller

	/**
	 * direction pin is used for Pololu stype stepper drivers with a direction
	 * input
	 */

	// TODO - generalize
	private transient Arduino arduino;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		// TODO - generalize
		peers.put("arduino", "Arduino", "arduino");
		return peers;
	}

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
		if (controller == null) {
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

	public void move(int newPos) {
		this.arduino.stepperMove(getName(), newPos);
	}

	@Override
	public void stop() {
		this.arduino.stepperStop(getName());
	}

	public void setNumSteps(Integer steps) {
		this.steps = steps;
	}

	public void reset() {
		controller.stepperReset(getName());
	}

	public boolean attach(String port, Integer... pins) {
		return attach((String) null, port, pins);
	}

	public boolean attach(String arduino, String port, Integer... pins) {
		if (arduino == null && this.arduino == null) {
			this.arduino = (Arduino) startPeer("arduino");
		}
		return attach(this.arduino, port, STEPPER_TYPE_POLOLU, pins);
	}

	public boolean attach(Arduino arduino, String port, String type, Integer... pins) {
		this.arduino = arduino;
		this.type = type;
		this.pins = pins;

		if (!this.arduino.connect(port)) {
			error("could not connect port %s", port);
			return false;
		}

		return arduino.stepperAttach(this);
	}

	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public void startService() {
		super.startService();
		arduino = (Arduino) startPeer("arduino");
	}

	boolean connect(String port) {
		return arduino.connect(port);
	}

	public Integer[] getPins() {
		return pins;
	}

	public String getStepperType() {
		return type;
	}

	// excellent pattern - put in interface
	public Integer publishStepperEvent(Integer currentPos) {
		log.info(String.format("publishStepperEvent %s %d", getName(), currentPos));
		this.currentPos = currentPos;
		if (isBlockingOnStop) {
			blockingData.add(currentPos);
		}
		return currentPos;
	}

	@Override
	public int getSteps() {
		return steps;
	}

	private boolean isBlockingOnStop = false;

	public Integer moveToBlocking(Integer newPos) {
		try {

			isBlockingOnStop = true;
			blockingData.clear();

			move(newPos);
			Integer gotTo = (Integer) blockingData.poll(10000, TimeUnit.MILLISECONDS);
			return gotTo;
		} catch (Exception e) {
			Logging.logException(e);
			return null;
		}
	}

	public Status test() {
		Status status = Status.info("starting %s %s test", getName(), getType());
		// FIXME - there has to be a properties method to configure localized
		// testing
		boolean useGUI = true;

		Stepper stepper = (Stepper) Runtime.start(getName(), "Stepper");
		// Python python = (Python) Runtime.start("python", "Python");

		// && depending on headless
		if (useGUI) {
			Runtime.start("gui", "GUIService");
		}

		int dirPin = 34;
		int stepPin = 38;
		// nice simple interface
		// stepper.connect("COM15");
		stepper.attach("COM12", dirPin, stepPin);

		//stepper.moveToBlocking(77777);

		stepper.move(81100);

		stepper.stop();
		// stepper.reset();

		stepper.move(100);

		// TODO - blocking call

		log.info("here");

		stepper.move(1);
		stepper.reset();
		stepper.move(2);

		log.info("here");
		stepper.move(-1);
		log.info("here");
		stepper.move(-300);
		return status;
	}
	
	// Uber good - .. although this is "chained" versus star routing
	// Star routing would be routing from the Arduino directly to the Listener
	// The "chained" version takes 2 thread contexts :( .. but it has the benefit
	// of the "publishRange" method being affected by the Sensor service e.g.
	// change units, sample rate, etc
	public void addPublishStepperEventListener(Service service) {
		addListener("publishStepperEvent", service.getName(), "publishStepperEvent", Integer.class);
	}


	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Stepper stepper = (Stepper) Runtime.start("stepper", "Stepper");
		stepper.test();

	}
	
	@Override
	public String[] getCategories() {
		return new String[] {"motor", "control"};
	}

}
