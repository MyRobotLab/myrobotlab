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
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.interfaces.NameProvider;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         Servos have both input and output. Input is usually of the range of
 *         integers between 0.0 - 180.0, and output can relay those values
 *         directly to the servo's firmware (Arduino ServoLib, I2C controller,
 *         etc)
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

		public Sweeper(String name) {
			super(String.format("%s.sweeper", name));
		}

		/**
		 * Sweeping works on input, a thread is used as the "controller" (this
		 * is input) and input sweeps back and forth - the servo parameters know
		 * what to do for output
		 */

		@Override
		public void run() {

			double sweepMin = 0.0;
			double sweepMax = 0.0;
			// start in the middle
			double sweepPos = mapper.getMinX() + (mapper.getMaxX() - mapper.getMinX()) / 2;
			isSweeping = true;

			try {
				while (isSweeping) {

					// set our range to be inside 'real' min & max input
					sweepMin = mapper.getMinX() + 1;
					sweepMax = mapper.getMaxX() - 1;

					// if pos is too small or too big flip direction
					if (sweepPos >= sweepMax || sweepPos <= sweepMin) {
						sweepStep = sweepStep * -1;
					}

					sweepPos += sweepStep;
					moveTo(sweepPos);
					Thread.sleep(sweepDelay);
				}
			} catch (Exception e) {
				isSweeping = false;
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

	Mapper mapper = new Mapper(0, 180, 0, 180);

	/**
	 * default rest is 90 default target position will be 90 if not specified
	 */
	double rest = 90;

	/**
	 * last time the servo has moved
	 */
	long lastActivityTime = 0;

	/**
	 * the 'pin' for this Servo - it is Integer because it can be in a state of
	 * 'not set' or null.
	 * 
	 * pin is the ONLY value which cannot and will not be 'defaulted'
	 */
	Integer pin;

	/**
	 * the requested INPUT position of the servo
	 */
	Double targetPos;

	/**
	 * the calculated output for the servo
	 */
	Double targetOutput;

	/**
	 * list of names of possible controllers
	 */
	public List<String> controllers;

	boolean isSweeping = false;
	// double sweepMin = 0;
	// double sweepMax = 180;
	int sweepDelay = 100;

	double sweepStep = 1;
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
	boolean isIKEventEnabled = false;

	double maxVelocity = -1;

	boolean isPinAttached = false;

	double velocity = -1;

	double acceleration = -1;

	double lastPos;

	boolean autoEnable = false;

	public int defaultDisableDelay = 10000;
	private boolean moving;
	private double currentPosInput;
	private boolean autoDisable = false;
	private transient Timer autoDisableTimer;

	public transient static final int SERVO_EVENT_STOPPED = 1;
	public transient static final int SERVO_EVENT_POSITION_UPDATE = 2;

	public class IKData {
		public String name;
		public Double pos;
		public Integer state;
		public double velocity;
		public Double targetPos;
	}

	public Servo(String n) {
		super(n);
		refreshControllers();
		subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
		lastActivityTime = System.currentTimeMillis();
	}

	public void onRegistered(ServiceInterface s) {
		refreshControllers();
		broadcastState();

	}

	public void addServoEventListener(NameProvider service) {
		isEventsEnabled = true;
		addListener("publishServoEvent", service.getName(), "onServoEvent");
	}

	public void addIKServoEventListener(NameProvider service) {
		isIKEventEnabled = true;
		addListener("publishIKServoEvent", service.getName(), "onIKServoEvent");
	}

	/**
	 * Re-attach to servo current conroller and pin. The pin must have be set previously.
	 * And the controller must have been set prior to calling this.
	 * 
	 */
	@Override
	public void attach() {
		attach(pin);
	}

	/**
	 * Equivalent to the Arduino IDE Servo.attach(). It energizes the servo sending
	 * pulses to maintain its current position.
	 */
	public void enable() {
		enable(pin);
	}

	/**
	 * This method will attach to the currently set controller with the specified pin.
	 */
	@Override
	public void attach(int pin) {
		lastActivityTime = System.currentTimeMillis();
		controller.servoAttachPin(this, pin);
		this.pin = pin;
		isPinAttached = true;
		broadcastState();
		invoke("publishServoEnable", getName());
	}

	/**
	 * Equivalent to Arduino's Servo.attach(pin). It energizes the servo sending
	 * pulses to maintain its current position.
	 */
	public void enable(int pin) {
		lastActivityTime = System.currentTimeMillis();
		controller.servoAttachPin(this, pin);
		this.pin = pin;
		isPinAttached = true;
		broadcastState();
		invoke("publishServoEnable", getName());
	}

	/**
	 * This method will disconnect the servo from it's controller.
	 * see also enable() / disable()
	 */
	@Override
	public void detach() {
		warn("detach() is deprecated please use disable()");
		isPinAttached = false;
		if (controller != null) {
			controller.servoDetachPin(this);
			detach(controller);
		}
		broadcastState();
		// TODO: this doesn't seem to be consistent depending on how you invoke
		// "detach()" ...
		// invoke("publishServoDetach", getName());
	}

	/**
	 * This method will leave the servo connected to the controller, however it will stop
	 * sending pwm messages to the servo.
	 */
	public void disable() {
		isPinAttached = false;
		if (controller != null) {
			controller.servoDetachPin(this);
			// detach(controller);
		}
		broadcastState();
		// TODO: this doesn't seem to be consistent depending on how you invoke
		// "detach()" ...
		invoke("publishServoDisable", getName());
	}

	public boolean eventsEnabled(boolean b) {
		isEventsEnabled = b;
		broadcastState();
		return b;
	}

	@Override
	public ServoController getController() {
		return controller;
	}

	public long getLastActivityTime() {
		return lastActivityTime;
	}

	@Override
	public double getMax() {
		return mapper.getMaxX();
	}

	@Override
	public double getMin() {
		return mapper.getMinX();
	}

	public double getMaxInput() {
		return mapper.getMaxInput();
	}

	public double getMinInput() {
		return mapper.getMinInput();
	}

	@Override
	public double getRest() {
		return rest;
	}

	// FIXME - change name to isPinAttached()
	// python scripts might use this ? :(
	public boolean isAttached() {
		// this is not pin attach
		return controller != null;
	}

	public boolean isPinAttached() {
		return isPinAttached;
	}

	public boolean isEnabled() {
		return isPinAttached;
	}

	@Override
	public boolean isInverted() {
		return mapper.isInverted();
	}

	public void map(double minX, double maxX, double minY, double maxY) {
		mapper = new Mapper(minX, maxX, minY, maxY);
		broadcastState();
	}

	public void moveTo(double pos) {

		if (controller == null) {
			error(String.format("%s's controller is not set", getName()));
			return;
		}
		if (autoEnable && !isEnabled()) {
			enable();
		}
		if (pos == lastPos) {
			if (autoDisable && isPinAttached()) {
				disable();
			}
			return;
		}
		lastPos = targetPos;
		if (pos < mapper.getMinX()) {
			pos = mapper.getMinX();
		}
		if (pos > mapper.getMaxX()) {
			pos = mapper.getMaxX();
		}
		targetPos = pos;
		targetOutput = getTargetOutput();// mapper.calcOutput(targetPos); //
											// calculated degrees

		// calculated degrees
		controller.servoMoveTo(this);
		lastActivityTime = System.currentTimeMillis();

	}

	/**
	 * basic move command of the servo - usually is 0 - 180 valid range but can
	 * be adjusted and / or re-mapped with min / max and map commands
	 * 
	 * TODO - moveToBlocking - blocks until servo sends "ARRIVED_TO_POSITION"
	 * response
	 */

	public Double publishServoEvent(Double position) {
		return position;
	}

	public List<String> refreshControllers() {
		controllers = Runtime.getServiceNamesFromInterface(ServoController.class);
		return controllers;
	}

	@Override
	public void releaseService() {
		disable();
		detach(controller);
		super.releaseService();
	}

	public void rest() {
		moveTo(rest);
	}

	public void setInverted(boolean invert) {
		mapper.setInverted(invert);
	}

	@Override
	public void setMinMax(double min, double max) {
		map(min, max, min, max);
		// THIS IS A BUG - BUT CORRECTING IT BURNS OLD SCRIPT'S SERVOS !!
		// mapper.setMinMaxOutput(min, max);
		/*
		 * THIS IS A BUG default setMinMax setMin & setMax IS INPUT NOT OUTPUT
		 * !!!! mapper.setMin(min); mapper.setMax(max);
		 */
		// broadcastState();
	}

	@Override
	public void setRest(int rest) {
		// TODO:remove this interface
		this.rest = rest;
	}

	public void setRest(Double rest) {
		// TODO: update the interface with the double version to make all servos
		// implement
		// a double for their positions.
		this.rest = rest;
	}

	/**
	 * setSpeed is deprecated, new function for speed control is setVelocity()
	 */
	@Deprecated
	public void setSpeed(double speed) {

		// KWATTERS: The realtionship between the old set speed value and actual
		// angular velocity was exponential.
		// To create a model to map these, I took the natural log of the speed
		// values, computed a linear regression line
		// y=mx+b
		// And then convert it back to exponential space with the e^y
		// approximating this with the equation
		// val = e ^ (slope * x + intercept)
		// slope & intercept were fitted by taking a linear regression of the
		// log values
		// of the mapping.

		// Speed,NewFunction,OldMeasured
		// 0.1,3,6
		// 0.2,5,7
		// 0.3,7,9
		// 0.4,9,9
		// 0.5,13,11
		// 0.6,19,13
		// 0.7,26,18
		// 0.8,36,27
		// 0.9,50,54

		// These 2 values can be tweaked for a slightly different curve that
		// fits the observed data.
		double slope = 3.25;
		double intercept = 1;

		double vel = Math.exp(slope * speed + intercept);
		// set velocity to 0.0 if the speed = 1.0.. This skips the velocity
		// calculation logic.
		if (speed >= 1.0) {
			vel = maxVelocity;
		}
		setVelocity((int) vel);
		// Method from build 1670
		// if(speed <= 0.1d) setVelocity(6);
		// else if (speed <= 0.2d) setVelocity(7);
		// else if (speed <= 0.3d) setVelocity(8);
		// else if (speed <= 0.4d) setVelocity(9);
		// else if (speed <= 0.5d) setVelocity(11);
		// else if (speed <= 0.6d) setVelocity(13);
		// else if (speed <= 0.7d) setVelocity(18);
		// else if (speed <= 0.8d) setVelocity(27);
		// else if (speed <= 0.9d) setVelocity(54);
		// else setVelocity(0);
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
		controller.servoSweepStop(this);
		broadcastState();
	}

	public void sweep() {
		double min = mapper.getMinX();
		double max = mapper.getMaxX();
		sweep(min, max, 50, 1);
	}

	public void sweep(double min, double max) {
		sweep(min, max, 1, 1);
	}

	// FIXME - is it really speed control - you don't currently thread for
	// factional speed values
	public void sweep(double min, double max, int delay, double step) {
		sweep(min, max, delay, step, false);
	}

	public void sweep(double min, double max, int delay, double step, boolean oneWay) {
		mapper.setMinMaxInput(min, max);
		/*
		 * THIS IS A BUGG !!! mapper.setMin(min); mapper.setMax(max);
		 */

		this.sweepDelay = delay;
		this.sweepStep = step;
		this.sweepOneWay = oneWay;

		// FIXME - CONTROLLER TYPE SWITCH
		if (speedControlOnUC) {
			controller.servoSweepStart(this); // delay &
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
		controller.servoWriteMicroseconds(this, uS);
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

	@Override
	public double getPos() {
		if (targetPos == null) {
			return rest;
		} else {
			return targetPos;
		}
	}

	// This was originally named setController
	// and Tracking service depended on it to set
	// the servos to a controller where pins could
	// be assigned later...
	public void attach(ServoController controller) throws Exception {
		if (isAttached(controller)) {
			log.info("{} servo is already attached to controller {}", getName(), this.controller.getName());
			return;
		} else if (this.controller != controller) {
		  //we're switching controllers
		  detach();
		}

//			log.warn("already attached to controller {} - please detach before attaching to controller {}",
//					this.controller.getName(), controller.getName());
//			return;
//		}

		targetOutput = getTargetOutput();// mapper.calcOutput(targetPos);

		// set the controller
		this.controller = controller;
		this.controllerName = controller.getName();

		// now attach the controller
		// the controller better have
		// isAttach(ServoControl) to prevent infinit loop
		controller.attach(this);
		sleep(300);
		// the controller is attached now
		// its time to attach the pin
		enable(pin);

		broadcastState();
	}

	public void attach(String controllerName, int pin) throws Exception {
		this.pin = pin;
		attach((ServoController) Runtime.getService(controllerName));
	}

	public void attach(String controllerName, Integer pin, Double pos) throws Exception {
		this.pin = pin;
		this.targetPos = pos;
		attach((ServoController) Runtime.getService(controllerName));
	}

	/*
	 * GroG REMOVED THIS METHOD - because the way method caching works it
	 * potentially conflicted and prevents the above method from working ..
	 * attach(String controllerName, Integer pin, Double pos)
	 *
	 * public void attach(String controllerName, int pin, Integer pos, Integer
	 * velocity) throws Exception { attach((ServoController)
	 * Runtime.getService(controllerName), pin, pos, velocity); }
	 */

	/**
	 * attach will default the position to a default reset position since its
	 * not specified
	 */
	@Override
	public void attach(ServoController controller, int pin) throws Exception {
		this.pin = pin;
		attach(controller);
	}

	public void attach(ServoController controller, int pin, double pos) throws Exception {
		this.pin = pin;
		this.targetPos = pos;
		attach(controller);
	}

	// FIXME - setController is very deficit in its abilities - compared to the
	// complexity of this
	@Override
	public void attach(ServoController controller, int pin, double pos, double velocity) throws Exception {
		this.pin = pin;
		this.targetPos = pos;
		this.velocity = velocity;
		attach(controller);
	}

	public boolean isAttached(ServoController controller) {
		return this.controller == controller;
	}

	@Override
	public void detach(String controllerName) {
		detach((ServoController) Runtime.getService(controllerName));
	}

	@Override
	public void detach(ServoController controller) {
		if (this.controller == controller) {
			// detach the this device from the controller
			controller.detach(this);
			// remove the this controller's reference
			this.controller = null;
			this.controllerName = null;
			isPinAttached = false;
			broadcastState();
		}
	}

	public void setMaxVelocity(int velocity) {
		this.maxVelocity = velocity;
	}

	public void setVelocity(double velocity) {
		if (maxVelocity != -1 && velocity > maxVelocity) {
			velocity = maxVelocity;
			log.info("Trying to set velocity to a value greater than max velocity");
		}
		this.velocity = velocity;
		if (controller != null) {
			controller.servoSetVelocity(this);
		}
	}

	public void setAcceleration(double acceleration) {
		this.acceleration = acceleration;
		if (controller != null) {
			controller.servoSetAcceleration(this);
		}
	}

	@Override
	public double getMaxVelocity() {
		return maxVelocity;
	}

	@Override
	public double getVelocity() {
		return velocity;
	}

	public IKData publishIKServoEvent(IKData data) {
		return data;
	}

	@Override
	public void setPin(int pin) {
		this.pin = pin;
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

	@Override
	public double getAcceleration() {
		return acceleration;
	}

	/**
	 * getCurrentPos() - return the calculated position of the servo use
	 * lastActivityTime and velocity for the computation
	 * 
	 * @return the current position of the servo
	 */
	public Double getCurrentPos() {
		return currentPosInput;
	}

	/**
	 * return time to move the servo to the target position, in ms
	 * 
	 * @return
	 */
	int timeToMove() {
		if (velocity <= 0.0) {
			return 1;
		}
		double delta = Math.abs(mapper.calcOutput(targetPos) - mapper.calcOutput(lastPos));
		double time = delta / velocity * 1000;
		return (int) time;
	}

	/**
	 * enableAutoAttach will attach a servo when ask to move and  it when
	 * the move is complete
	 * 
	 * @param autoAttach
	 */
	@Deprecated
	public void enableAutoAttach(boolean autoAttach) {
		warn("enableAutoAttach is disabled please use enableAutoEnable");
		this.autoEnable = autoAttach;
	}

	public void enableAutoEnable(boolean autoEnable) {
		this.autoEnable = autoEnable;
	}

	@Deprecated
	public void enableAutoDetach(boolean autoDetach) {
		warn("enableAutoDetach is disabled please use enableAutoDisable");
		this.autoDisable = autoDetach;
		this.addServoEventListener(this);
	}

	public void enableAutoDisable(boolean autoDisable) {
		this.autoDisable = autoDisable;
		this.addServoEventListener(this);
	}

	public double microsecondsToDegree(int microseconds) {
		if (microseconds <= 180)
			return microseconds;
		return (double) (microseconds - 544) * 180 / (2400 - 544);
	}

	public String publishServoEnable(String name) {
		return name;
	}

	public String publishServoDisable(String name) {
		return name;
	}

	/**
	 * this output is 'always' in degrees !
	 */
	@Override
	public double getTargetOutput() {
		if (targetPos == null) {
			targetPos = rest;
		}
		targetOutput = mapper.calcOutput(targetPos);
		return targetOutput;
	}

	public void autoDisable() {
		if (getTasks().containsKey("EndMoving")) {
			purgeTask("EndMoving");
		}
		if (!isMoving() && autoEnable && isPinAttached()) {
			disable();
		}
		// moving = false;
	}

	public boolean isMoving() {
		return moving;
	}

	public void onServoEvent(Integer eventType, Integer currentPosUs, Integer targetPos) {
		double currentPos = microsecondsToDegree(currentPosUs);
		currentPosInput = mapper.calcInput(currentPos);
		if (isEventsEnabled) {
			invoke("publishServoEvent", currentPosInput);
		}
		if (isIKEventEnabled) {
			IKData data = new IKData();
			data.name = getName();
			data.pos = currentPosInput;
			data.state = eventType;
			data.velocity = velocity;
			data.targetPos = this.targetPos;
			invoke("publishIKServoEvent", data);
		}
		if (eventType == SERVO_EVENT_STOPPED) {
			moving = false;
		} else {
			moving = true;
		}
	}

	public void onIMAngles(Object[] data) {
		String name = (String) data[0];
		if (name.equals(this.getName())) {
			moveTo((double) data[1]);
		}
	}

	public void onServoEvent(Double position) {
		// log.info("{}.ServoEvent {}", getName(), position);
		if (!isMoving() && autoDisable && isPinAttached()) {
			if (velocity > -1) {
				disable();
			} else {
				if (autoDisableTimer != null) {
					autoDisableTimer.cancel();
					autoDisableTimer = null;
				}
				autoDisableTimer = new Timer();
				autoDisableTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						disable();
					}
				}, (long) defaultDisableDelay);
			}
		}
	}

	 /**
   * Set the controller for this servo but does not attach it.
   * see also attach()
   */
  public void setController(ServoController controller) {
    this.controller = controller;
  }

	
	public static void main(String[] args) throws InterruptedException {
		try {
			LoggingFactory.init(Level.INFO);

			VirtualArduino virtual = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
			virtual.connect("COM10");
			// Runtime.start("webgui", "WebGui");
			Runtime.start("gui", "SwingGui");
			Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			arduino.record();
			// arduino.getSerial().record();

			log.info("ports {}", Arrays.toString(arduino.getSerial().getPortNames().toArray()));
			arduino.connect("COM10");

			log.info("ready here");
			// arduino.ackEnabled = true;
			Servo servo = (Servo) Runtime.start("servo", "Servo");

			servo.attach(arduino, 7);
			servo.moveTo(90);
			servo.setRest(30);

			boolean done = true;
			if (done) {
				return;
			}

			servo.attach(8);
			servo.moveTo(90);
			servo.moveTo(30);

			servo.attach(9);
			servo.moveTo(90);
			servo.setRest(30);

			// FIXME - JUNIT - test attach - detach - re-attach
			// servo.detach(arduino);

			log.info("servo attach {}", servo.isAttached());

			arduino.disconnect();
			arduino.connect("COM4");

			arduino.reset();

			log.info("ready here 2");
			// servo.attach(arduino, 8);
			// servo.attach(
			servo.attach(arduino, 7);
			servo.moveTo(90);
			servo.moveTo(30);

			servo.attach(9);
			servo.moveTo(90);
			servo.setRest(30);

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
			servo.setSpeed(1.0);

			// servo.test();
		} catch (Exception e) {
			Logging.logError(e);
		}

	}
}
