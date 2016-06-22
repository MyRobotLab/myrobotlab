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
import org.myrobotlab.service.interfaces.Device;
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

public class Servo extends Service implements ServoControl, Device {

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

	transient ServoController controller;

	String controllerName = null;

	private Mapper mapper = new Mapper(0, 180, 0, 180);

	private int rest = 90;

	private long lastActivityTime = 0;

	/**
	 * the requested INPUT position of the servo
	 */
	Integer targetPos;

	/**
	 * the calculated output for the servo
	 */
	Integer targetOutput;

	/**
	 * writeMicroseconds value
	 */
	Integer uS;

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
	 * isAttached represents if the
	 * 
	 * usually isAttached would be determined by whether the controller != null
	 * however detach() is a meaningful method on the micro-controller side and
	 * many services want to be able to detach() and re-attach() without a
	 * reference to the controller - that is why this boolean variable must be
	 * kept in sync
	 */
	private boolean isAttached = false;

	/**
	 * feedback of both incremental position and stops. would allow blocking
	 * moveTo if desired
	 */
	boolean isEventsEnabled = false;

	public Servo(String n) {
		super(n);
		lastActivityTime = System.currentTimeMillis();
		// don't allow buffer overruns?
		// outbox.setBlocking(true);
	}

	// uber good
	public void addServoEventListener(NameProvider service) {
		addListener("publishServoEvent", service.getName(), "onServoEvent");
	}

	/**
	 * FIXME - this is Servo.attach NOT Device.attach (GroG)
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

		// controller.attach(this);
		isAttached = true;
		broadcastState();
		return isAttached;
	}

	public boolean attach(ServoController controller, Integer pin) throws Exception {
		setPin(pin);

		if (setController(controller)) {
			// THIS IS ATTACHING THE DEVICE !!!
			controller.attachDevice(this);
			// THIS IS calling Arduino's Servo.attach(pin) !!
			return attach();
		}

		return false;
	}

	@Override
	public boolean attach(String controllerName, Integer pin) throws Exception {
		return attach((ServoController) Runtime.getService(controllerName), pin);
	}

	@Override
	public boolean detach() {
		if (!isAttached) {
			log.info(String.format("%s.detach() - already detach - attach first", getName()));
			return false;
		}

		if (controller != null) {
			controller.detachDevice(this);
			isAttached = false;
			// changed state
			broadcastState();
			return true;
		}

		return false;
	}

	@Override
	public String getControllerName() {
		if (controller == null) {
			return null;
		}

		return controller.getName();
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

	public Double getMin() {
		return mapper.getMinY();
	}

	public Double getMinInput() {
		return mapper.getMinX();
	}

	public Double getMinOutput() {
		return mapper.getMinOutput();
	}

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

	public Integer getPos() {
		return targetPos;
	}

	public int getRest() {
		return rest;
	}

	public boolean isAttached() {
		return isAttached;
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

		if (!isAttached) {
			warn("servo not attached");
			return;
		}

		targetPos = pos;
		targetOutput = mapper.calcInt(targetPos);

		controller.servoWrite(this);
		lastActivityTime = System.currentTimeMillis();

		// update the web gui that we've moved..
		// broadcastState();

		invoke("publishServoEvent", targetOutput);
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

	public boolean eventsEnabled(boolean b) {
		isEventsEnabled = b;
		controller.servoEventsEnabled(this, b);
		return b;
	}

	public void setInverted(boolean invert) {
		mapper.setInverted(invert);
	}

	@Override
	public void setMinMax(int min, int max) {
		mapper.setMin(min);
		mapper.setMax(max);
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

	public void setSpeed(double speed) {

		this.speed = speed;

		if (controller == null) {
			error("setSpeed - controller not set");
			return;
		}
		controller.setServoSpeed(this);
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
		this.uS = uS;

		if (controller == null) {
			error(String.format("%s's controller is not set", getName()));
			return;
		}

		// TODO -

		// FIXME - currently their is no timerPosition
		// this could be gotten with 100 * outputY for some valid range

		controller.servoWriteMicroseconds(this);
		lastActivityTime = System.currentTimeMillis();

		// trying out broadcast after
		// position change
		broadcastState();
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

	@Override
	public Integer getDeviceType() {
		return Device.DEVICE_TYPE_SERVO;
	}

	@Override
	public int[] getDeviceConfig() {
		return new int[] { pin };
	}

}